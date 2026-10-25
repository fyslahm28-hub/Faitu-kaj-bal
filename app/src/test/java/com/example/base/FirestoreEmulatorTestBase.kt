package com.example.base

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.io.File
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
abstract class FirestoreEmulatorTestBase {

    protected lateinit var firestore: FirebaseFirestore
    protected lateinit var auth: FirebaseAuth

    private fun isPortOpen(host: String, port: Int, timeoutMs: Int = 500): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    @Before
    open fun setUpFirebase() {
        assumeTrue(
            "Firebase Auth/Firestore emulators are not running on $EMULATOR_HOST:$FIRESTORE_PORT / $AUTH_PORT; skipping emulator-backed test.",
            isPortOpen(EMULATOR_HOST, FIRESTORE_PORT) && isPortOpen(EMULATOR_HOST, AUTH_PORT)
        )
        syncRulesWithRunningEmulator()

        val context = ApplicationProvider.getApplicationContext<Context>()

        val app = if (FirebaseApp.getApps(context).isEmpty()) {
            val newApp = FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.qawmikormo.khedmt")
                    .setProjectId(PROJECT_ID)
                    .setApiKey("fake-api-key-for-emulator")
                    .build()
            )
            val newFirestore = FirebaseFirestore.getInstance(newApp)
            newFirestore.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
            newFirestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()

            val newAuth = FirebaseAuth.getInstance(newApp)
            newAuth.useEmulator(EMULATOR_HOST, AUTH_PORT)
            newApp
        } else {
            FirebaseApp.getInstance()
        }

        firestore = FirebaseFirestore.getInstance(app)
        auth = FirebaseAuth.getInstance(app)
    }

    private fun syncRulesWithRunningEmulator() {
        if (rulesSynced) return
        val candidates = listOf(
            File("../firestore.rules"),
            File("firestore.rules"),
            File("/app/applet/firestore.rules")
        )
        val rulesFile = candidates.firstOrNull { it.exists() && it.isFile } ?: return
        val rulesContent = rulesFile.readText(Charsets.UTF_8)

        val bodyJson = JSONObject().apply {
            put("ignore_errors", true)
            put(
                "rules",
                JSONObject().apply {
                    put(
                        "files",
                        JSONArray().put(
                            JSONObject().apply {
                                put("name", "firestore.rules")
                                put("content", rulesContent)
                            }
                        )
                    )
                }
            )
        }.toString()

        try {
            val url = URL("http://$EMULATOR_HOST:$FIRESTORE_PORT/emulator/v1/projects/$PROJECT_ID:securityRules")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 3000
                readTimeout = 3000
            }
            conn.outputStream.use { it.write(bodyJson.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code in 200..299) {
                rulesSynced = true
            }
            conn.disconnect()
        } catch (_: Exception) {
            // Ignore if emulator endpoint differs
        }
    }

    @After
    open fun tearDownFirebase() {
        if (::auth.isInitialized) {
            auth.signOut()
        }
    }

    protected suspend fun signInTestUser(email: String): String = withContext(Dispatchers.IO) {
        withTimeout(AUTH_TIMEOUT_MS) {
            val result = try {
                auth.signInWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
            } catch (unused: FirebaseAuthInvalidUserException) {
                auth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
            }
            checkNotNull(result.user?.uid) { "User auth failed" }
        }
    }

    private companion object {
        const val EMULATOR_HOST = "127.0.0.1"
        const val FIRESTORE_PORT = 8088
        const val AUTH_PORT = 9098
        const val PROJECT_ID = "demo-no-project"
        const val DEFAULT_PASSWORD = "password123"
        const val AUTH_TIMEOUT_MS = 5000L
        var rulesSynced = false
    }
}
