package com.example.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

fun translateFirestoreExceptionToBangla(exception: Exception): Exception {
    val msg = exception.message.orEmpty()
    if (msg.contains("exceeds", ignoreCase = true) ||
        msg.contains("1 MiB", ignoreCase = true) ||
        msg.contains("1048576", ignoreCase = true) ||
        msg.contains("maximum size", ignoreCase = true) ||
        (msg.contains("INVALID_ARGUMENT", ignoreCase = true) && msg.contains("feed_posts", ignoreCase = true))
    ) {
        return IllegalArgumentException(
            com.example.ui.util.ProfilePhotoHelper.OVERSIZED_POST_ERROR_BANGLA,
            exception
        )
    }
    return exception
}
