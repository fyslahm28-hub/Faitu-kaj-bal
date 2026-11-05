package com.example.data.repository

import android.os.Build
import com.example.data.model.DirectMessage
import com.example.data.model.FeedPost
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Direct HTTPS Cloud Firestore v1 REST API client (`https://firestore.googleapis.com/v1/...`)
 * for the project's named database (`ai-studio-android-811ea734-e9e8-4ff4-8e56-ca97ec8a429c`
 * in `gen-lang-client-0048059944`).
 *
 * Used alongside the Firestore Android gRPC SDK so that:
 * 1. Message writes (`sendDirectMessage`) and Feed post writes (`createFeedPost`, `updateFeedPost`)
 *    commit directly to the real Firestore server over standard HTTPS even if the gRPC HTTP/2
 *    stream or local SQLite queue is congested on low-bandwidth mobile connections.
 * 2. Live message and feed post reads (`observeMessages`, `observePosts`) fetch fresh server
 *    documents directly over HTTPS whenever the gRPC stream takes more than a couple of seconds.
 */
object FirestoreRestFallbackClient {

    const val DEFAULT_PROJECT_ID = "gen-lang-client-0048059944"
    const val DEFAULT_DATABASE_ID = "ai-studio-android-811ea734-e9e8-4ff4-8e56-ca97ec8a429c"

    fun isRobolectricRuntime(): Boolean {
        return Build.FINGERPRINT.equals("robolectric", ignoreCase = true) ||
            System.getProperty("robolectric.logging") != null
    }

    suspend fun resolveIdToken(auth: FirebaseAuth?): String? {
        val user = runCatching { auth?.currentUser }.getOrNull() ?: return null
        val cachedToken = withTimeoutOrNull(3_500L) {
            runCatching { user.getIdToken(false).await()?.token?.trim() }.getOrNull()
        }
        if (!cachedToken.isNullOrBlank()) return cachedToken
        return withTimeoutOrNull(4_500L) {
            runCatching { user.getIdToken(true).await()?.token?.trim() }.getOrNull()
        }
    }

    private fun isServerTimestampSentinel(value: Any?): Boolean {
        if (value == null) return false
        if (value == FieldValue.serverTimestamp()) return true
        val className = value.javaClass.name
        return className.contains("ServerTimestamp", ignoreCase = true)
    }

    private fun formatIsoTimestamp(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val base = sdf.format(Date(timestamp.seconds * 1000L))
        val nanos = timestamp.nanoseconds.coerceIn(0, 999_999_999)
        return String.format(Locale.US, "%s.%09dZ", base, nanos)
    }

    fun parseIsoTimestamp(raw: String?): Timestamp? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        return try {
            val clean = trimmed.removeSuffix("Z")
            val mainAndFraction = clean.split(".", limit = 2)
            val datePart = mainAndFraction[0]
            val fracPart = mainAndFraction.getOrNull(1).orEmpty()
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsedDate = sdf.parse(datePart) ?: return null
            val seconds = parsedDate.time / 1000L
            val nanos = if (fracPart.isEmpty()) {
                0
            } else {
                fracPart.take(9).padEnd(9, '0').toIntOrNull() ?: 0
            }
            Timestamp(seconds, nanos)
        } catch (_: Throwable) {
            null
        }
    }

    private fun toFirestoreValueJson(value: Any?): JSONObject {
        val obj = JSONObject()
        when (value) {
            null -> obj.put("nullValue", JSONObject.NULL)
            is Boolean -> obj.put("booleanValue", value)
            is Int -> obj.put("integerValue", value.toString())
            is Long -> obj.put("integerValue", value.toString())
            is Double -> obj.put("doubleValue", value)
            is Float -> obj.put("doubleValue", value.toDouble())
            is Timestamp -> obj.put("timestampValue", formatIsoTimestamp(value))
            is String -> obj.put("stringValue", value)
            is Iterable<*> -> {
                val valuesArr = JSONArray()
                for (item in value) {
                    if (!isServerTimestampSentinel(item)) {
                        valuesArr.put(toFirestoreValueJson(item))
                    }
                }
                val arrayObj = JSONObject()
                if (valuesArr.length() > 0) {
                    arrayObj.put("values", valuesArr)
                }
                obj.put("arrayValue", arrayObj)
            }
            is Map<*, *> -> {
                val fieldsObj = JSONObject()
                for ((k, v) in value) {
                    val keyStr = k?.toString()?.trim().orEmpty()
                    if (keyStr.isNotBlank() && !isServerTimestampSentinel(v)) {
                        fieldsObj.put(keyStr, toFirestoreValueJson(v))
                    }
                }
                val mapObj = JSONObject()
                if (fieldsObj.length() > 0) {
                    mapObj.put("fields", fieldsObj)
                }
                obj.put("mapValue", mapObj)
            }
            else -> obj.put("stringValue", value.toString())
        }
        return obj
    }

    /**
     * Builds the JSON body for `POST .../documents:commit` from a Kotlin payload map,
     * converting `FieldValue.serverTimestamp()` fields into `updateTransforms` with
     * `setToServerValue: "REQUEST_TIME"`.
     */
    fun buildCommitRequestBody(
        projectId: String = DEFAULT_PROJECT_ID,
        databaseId: String = DEFAULT_DATABASE_ID,
        collection: String,
        documentId: String,
        payload: Map<String, Any?>,
        updateMaskFields: List<String>? = null
    ): JSONObject {
        val resolvedProject = projectId.trim().ifBlank { DEFAULT_PROJECT_ID }
        val resolvedDb = databaseId.trim().ifBlank { DEFAULT_DATABASE_ID }
        val docName = "projects/$resolvedProject/databases/$resolvedDb/documents/$collection/$documentId"

        val fieldsJson = JSONObject()
        val transformsArr = JSONArray()

        for ((key, value) in payload) {
            if (isServerTimestampSentinel(value)) {
                transformsArr.put(
                    JSONObject()
                        .put("fieldPath", key)
                        .put("setToServerValue", "REQUEST_TIME")
                )
            } else {
                fieldsJson.put(key, toFirestoreValueJson(value))
            }
        }

        val writeObj = JSONObject()
        val updateObj = JSONObject()
            .put("name", docName)
            .put("fields", fieldsJson)
        writeObj.put("update", updateObj)

        if (!updateMaskFields.isNullOrEmpty()) {
            val maskPaths = JSONArray()
            updateMaskFields.filterNot { field ->
                isServerTimestampSentinel(payload[field])
            }.forEach { maskPaths.put(it) }
            writeObj.put("updateMask", JSONObject().put("fieldPaths", maskPaths))
        }

        if (transformsArr.length() > 0) {
            writeObj.put("updateTransforms", transformsArr)
        }

        return JSONObject().put("writes", JSONArray().put(writeObj))
    }

    /**
     * Commits a document create/set or partial update directly to Cloud Firestore via HTTPS REST API.
     */
    suspend fun commitDocumentWrite(
        collection: String,
        documentId: String,
        payload: Map<String, Any?>,
        auth: FirebaseAuth?,
        databaseId: String = DEFAULT_DATABASE_ID,
        projectId: String = DEFAULT_PROJECT_ID,
        updateMaskFields: List<String>? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (isRobolectricRuntime()) {
            return@withContext Result.failure(IllegalStateException("Skipped in Robolectric"))
        }
        val idToken = resolveIdToken(auth)
            ?: return@withContext Result.failure(
                FirebaseFirestoreException(
                    "Unauthenticated session token",
                    FirebaseFirestoreException.Code.UNAUTHENTICATED
                )
            )
        val resolvedProject = projectId.trim().ifBlank { DEFAULT_PROJECT_ID }
        val resolvedDb = databaseId.trim().ifBlank { DEFAULT_DATABASE_ID }
        val endpoint = "https://firestore.googleapis.com/v1/projects/$resolvedProject/databases/$resolvedDb/documents:commit"
        val bodyJson = buildCommitRequestBody(
            projectId = resolvedProject,
            databaseId = resolvedDb,
            collection = collection,
            documentId = documentId,
            payload = payload,
            updateMaskFields = updateMaskFields
        ).toString()

        var conn: HttpURLConnection? = null
        try {
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 7_000
                readTimeout = 8_000
                setRequestProperty("Authorization", "Bearer $idToken")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
            }
            conn.outputStream.use { out ->
                out.write(bodyJson.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            val code = conn.responseCode
            if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
                Result.success(Unit)
            } else {
                val errBody = runCatching {
                    conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }.getOrDefault("")
                val mappedCode = when (code) {
                    401 -> FirebaseFirestoreException.Code.UNAUTHENTICATED
                    403 -> FirebaseFirestoreException.Code.PERMISSION_DENIED
                    404 -> FirebaseFirestoreException.Code.NOT_FOUND
                    400 -> FirebaseFirestoreException.Code.INVALID_ARGUMENT
                    429 -> FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED
                    else -> FirebaseFirestoreException.Code.UNAVAILABLE
                }
                Result.failure(
                    FirebaseFirestoreException(
                        "HTTP $code: ${errBody.take(300)}",
                        mappedCode
                    )
                )
            }
        } catch (e: Throwable) {
            Result.failure(e)
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private fun extractStringField(fields: JSONObject?, name: String, default: String = ""): String {
        val fieldObj = fields?.optJSONObject(name) ?: return default
        return if (fieldObj.has("stringValue")) fieldObj.optString("stringValue", default) else default
    }

    private fun extractBooleanField(fields: JSONObject?, name: String, default: Boolean = false): Boolean {
        val fieldObj = fields?.optJSONObject(name) ?: return default
        return if (fieldObj.has("booleanValue")) fieldObj.optBoolean("booleanValue", default) else default
    }

    private fun extractIntField(fields: JSONObject?, name: String, default: Int = 0): Int {
        val fieldObj = fields?.optJSONObject(name) ?: return default
        if (fieldObj.has("integerValue")) {
            return fieldObj.optString("integerValue", default.toString()).toIntOrNull() ?: default
        }
        if (fieldObj.has("doubleValue")) {
            return fieldObj.optDouble("doubleValue", default.toDouble()).toInt()
        }
        return default
    }

    private fun extractTimestampField(fields: JSONObject?, name: String): Timestamp? {
        val fieldObj = fields?.optJSONObject(name) ?: return null
        if (fieldObj.has("timestampValue")) {
            return parseIsoTimestamp(fieldObj.optString("timestampValue", ""))
        }
        return null
    }

    private fun extractStringListField(fields: JSONObject?, name: String): List<String> {
        val valuesArr = fields?.optJSONObject(name)
            ?.optJSONObject("arrayValue")
            ?.optJSONArray("values")
            ?: return emptyList()
        val result = ArrayList<String>(valuesArr.length())
        for (i in 0 until valuesArr.length()) {
            val item = valuesArr.optJSONObject(i) ?: continue
            if (item.has("stringValue")) {
                result.add(item.optString("stringValue", ""))
            }
        }
        return result
    }

    private fun extractStringMapField(fields: JSONObject?, name: String): Map<String, String> {
        val mapFields = fields?.optJSONObject(name)
            ?.optJSONObject("mapValue")
            ?.optJSONObject("fields")
            ?: return emptyMap()
        val result = LinkedHashMap<String, String>()
        val keys = mapFields.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val vObj = mapFields.optJSONObject(k) ?: continue
            if (vObj.has("stringValue")) {
                result[k] = vObj.optString("stringValue", "")
            }
        }
        return result
    }

    private fun extractMapListField(fields: JSONObject?, name: String): List<Map<String, String>> {
        val valuesArr = fields?.optJSONObject(name)
            ?.optJSONObject("arrayValue")
            ?.optJSONArray("values")
            ?: return emptyList()
        val result = ArrayList<Map<String, String>>(valuesArr.length())
        for (i in 0 until valuesArr.length()) {
            val mapFields = valuesArr.optJSONObject(i)
                ?.optJSONObject("mapValue")
                ?.optJSONObject("fields")
                ?: continue
            val map = LinkedHashMap<String, String>()
            val keys = mapFields.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val vObj = mapFields.optJSONObject(k) ?: continue
                if (vObj.has("stringValue")) {
                    map[k] = vObj.optString("stringValue", "")
                }
            }
            result.add(map)
        }
        return result
    }

    fun parseDirectMessageDocument(docObj: JSONObject): DirectMessage? {
        val fullName = docObj.optString("name", "")
        val docId = fullName.substringAfterLast("/").trim()
        if (docId.isBlank()) return null
        val fields = docObj.optJSONObject("fields") ?: return null
        val createTimeFallback = parseIsoTimestamp(docObj.optString("createTime", ""))
        val createdAt = extractTimestampField(fields, "createdAt") ?: createTimeFallback

        val senderId = extractStringField(fields, "senderId")
        val receiverId = extractStringField(fields, "receiverId")
        val groupId = extractStringField(fields, "groupId")
        val isRead = extractBooleanField(fields, "isRead", false)
        val readByUserIds = extractStringListField(fields, "readByUserIds")
        val isGroupMsg = groupId.isNotBlank() || receiverId.startsWith("GROUP:", ignoreCase = true)
        val isReadByRecipient = isRead || if (isGroupMsg) {
            readByUserIds.any { token ->
                token.isNotBlank() &&
                    token != senderId &&
                    !token.startsWith("DELIVERED:", ignoreCase = true)
            }
        } else {
            receiverId.isNotBlank() && readByUserIds.contains(receiverId)
        }
        val isDeliveredToRecipient = if (isGroupMsg) {
            readByUserIds.any { token ->
                token.startsWith("DELIVERED:", ignoreCase = true) &&
                    !token.equals("DELIVERED:$senderId", ignoreCase = true)
            }
        } else {
            receiverId.isNotBlank() && readByUserIds.contains("DELIVERED:$receiverId")
        }
        val resolvedDeliveryState = when {
            isReadByRecipient -> "READ"
            isDeliveredToRecipient -> "DELIVERED"
            else -> "SENT"
        }

        return DirectMessage(
            id = docId,
            senderId = senderId,
            senderName = extractStringField(fields, "senderName"),
            senderPhotoUrl = extractStringField(fields, "senderPhotoUrl"),
            receiverId = receiverId,
            receiverName = extractStringField(fields, "receiverName"),
            groupId = groupId,
            groupName = extractStringField(fields, "groupName"),
            groupMemberIds = extractStringListField(fields, "groupMemberIds"),
            subject = extractStringField(fields, "subject"),
            body = extractStringField(fields, "body"),
            imageUrls = extractStringListField(fields, "imageUrls"),
            isRead = isRead,
            readByUserIds = readByUserIds,
            senderReaction = extractStringField(fields, "senderReaction"),
            receiverReaction = extractStringField(fields, "receiverReaction"),
            userReactions = extractStringMapField(fields, "userReactions"),
            isEdited = extractBooleanField(fields, "isEdited", false),
            editedAt = extractTimestampField(fields, "editedAt"),
            deletedForSender = extractBooleanField(fields, "deletedForSender", false),
            deletedForReceiver = extractBooleanField(fields, "deletedForReceiver", false),
            deletedForUserIds = extractStringListField(fields, "deletedForUserIds"),
            isDeletedForEveryone = extractBooleanField(fields, "isDeletedForEveryone", false),
            createdAt = createdAt
        ).apply {
            localDeliveryState = resolvedDeliveryState
        }
    }

    fun parseFeedPostDocument(docObj: JSONObject): FeedPost? {
        val fullName = docObj.optString("name", "")
        val docId = fullName.substringAfterLast("/").trim()
        if (docId.isBlank()) return null
        val fields = docObj.optJSONObject("fields") ?: return null
        val createTimeFallback = parseIsoTimestamp(docObj.optString("createTime", ""))
        val updateTimeFallback = parseIsoTimestamp(docObj.optString("updateTime", ""))
        val createdAt = extractTimestampField(fields, "createdAt") ?: createTimeFallback
        val updatedAt = extractTimestampField(fields, "updatedAt") ?: updateTimeFallback ?: createdAt

        val imageUrls = extractStringListField(fields, "imageUrls")
        val mediaMetadata = extractMapListField(fields, "mediaMetadata")
        val attachmentDocId = extractStringField(fields, "attachmentDocId")

        mediaMetadata.forEachIndexed { idx, meta ->
            val preview = meta["previewDataUrl"]?.takeIf { it.startsWith("data:image", ignoreCase = true) }
                ?: meta["mediaDataUrl"]?.takeIf { it.startsWith("data:image", ignoreCase = true) }
            if (!preview.isNullOrBlank()) {
                meta["attachmentDocId"]?.takeIf { it.isNotBlank() }?.let {
                    com.example.ui.util.ProfilePhotoHelper.cacheAttachmentDataUrl(it, preview)
                }
                com.example.ui.util.ProfilePhotoHelper.extractAttachmentDocId(meta["url"].orEmpty())?.let {
                    com.example.ui.util.ProfilePhotoHelper.cacheAttachmentDataUrl(it, preview)
                }
                imageUrls.getOrNull(idx)?.let { imgUrl ->
                    com.example.ui.util.ProfilePhotoHelper.extractAttachmentDocId(imgUrl)?.let {
                        com.example.ui.util.ProfilePhotoHelper.cacheAttachmentDataUrl(it, preview)
                    }
                }
                com.example.ui.util.ProfilePhotoHelper.cacheAttachmentDataUrl("${docId}_media_$idx", preview)
                if (idx == 0 && attachmentDocId.isNotBlank() && !attachmentDocId.endsWith("_ext_text")) {
                    com.example.ui.util.ProfilePhotoHelper.cacheAttachmentDataUrl(attachmentDocId, preview)
                }
            }
        }

        return FeedPost(
            id = docId,
            authorId = extractStringField(fields, "authorId"),
            authorName = extractStringField(fields, "authorName"),
            authorPhotoUrl = extractStringField(fields, "authorPhotoUrl"),
            postType = extractStringField(fields, "postType", "JOB_CIRCULAR"),
            title = extractStringField(fields, "title"),
            district = extractStringField(fields, "district"),
            upazilaOrLocation = extractStringField(fields, "upazilaOrLocation"),
            designation = extractStringField(fields, "designation"),
            qualification = extractStringField(fields, "qualification"),
            salaryOrExperience = extractStringField(fields, "salaryOrExperience"),
            deadlineOrPreferredDistricts = extractStringField(fields, "deadlineOrPreferredDistricts"),
            description = extractStringField(fields, "description"),
            contactNumber = extractStringField(fields, "contactNumber"),
            madrasaId = extractStringField(fields, "madrasaId"),
            isVerifiedMadrasa = extractBooleanField(fields, "isVerifiedMadrasa", false),
            isBoosted = extractBooleanField(fields, "isBoosted", false),
            khedmotCategories = extractStringListField(fields, "khedmotCategories"),
            customCategory = extractStringField(fields, "customCategory"),
            imageUrls = imageUrls,
            mediaTypes = extractStringListField(fields, "mediaTypes"),
            mediaMetadata = mediaMetadata,
            attachmentDocId = attachmentDocId,
            userReactions = extractStringMapField(fields, "userReactions"),
            userReactionNames = extractStringMapField(fields, "userReactionNames"),
            commentCount = extractIntField(fields, "commentCount", 0),
            shareCount = extractIntField(fields, "shareCount", 0),
            sharedPostId = extractStringField(fields, "sharedPostId"),
            sharedAuthorId = extractStringField(fields, "sharedAuthorId"),
            sharedAuthorName = extractStringField(fields, "sharedAuthorName"),
            sharedTitle = extractStringField(fields, "sharedTitle"),
            sharedDescription = extractStringField(fields, "sharedDescription"),
            sharedPostType = extractStringField(fields, "sharedPostType"),
            isEdited = extractBooleanField(fields, "isEdited", false),
            visibility = extractStringField(fields, "visibility", "PUBLIC"),
            approvalStatus = extractStringField(fields, "approvalStatus", "APPROVED"),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun executeRunQueryHttp(
        idToken: String,
        structuredQuery: JSONObject,
        databaseId: String = DEFAULT_DATABASE_ID,
        projectId: String = DEFAULT_PROJECT_ID
    ): JSONArray? {
        val resolvedProject = projectId.trim().ifBlank { DEFAULT_PROJECT_ID }
        val resolvedDb = databaseId.trim().ifBlank { DEFAULT_DATABASE_ID }
        val endpoint = "https://firestore.googleapis.com/v1/projects/$resolvedProject/databases/$resolvedDb/documents:runQuery"
        val bodyStr = JSONObject().put("structuredQuery", structuredQuery).toString()

        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 6_000
                readTimeout = 8_000
                setRequestProperty("Authorization", "Bearer $idToken")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
            }
            conn.outputStream.use { out ->
                out.write(bodyStr.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            if (conn.responseCode !in 200..299) return null
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            if (responseText.isBlank()) return null
            JSONArray(responseText)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    /**
     * Queries the latest `feed_posts` documents directly from the Firestore server over HTTPS REST.
     */
    suspend fun queryFeedPosts(
        auth: FirebaseAuth?,
        databaseId: String = DEFAULT_DATABASE_ID,
        projectId: String = DEFAULT_PROJECT_ID,
        limit: Int = 60
    ): List<FeedPost>? = withContext(Dispatchers.IO) {
        if (isRobolectricRuntime()) return@withContext null
        val idToken = resolveIdToken(auth) ?: return@withContext null

        val orderedQuery = JSONObject()
            .put("from", JSONArray().put(JSONObject().put("collectionId", "feed_posts")))
            .put(
                "orderBy",
                JSONArray().put(
                    JSONObject()
                        .put("field", JSONObject().put("fieldPath", "createdAt"))
                        .put("direction", "DESCENDING")
                )
            )
            .put("limit", limit)

        val arr = executeRunQueryHttp(idToken, orderedQuery, databaseId, projectId) ?: run {
            val fallbackQuery = JSONObject()
                .put("from", JSONArray().put(JSONObject().put("collectionId", "feed_posts")))
                .put("limit", limit)
            executeRunQueryHttp(idToken, fallbackQuery, databaseId, projectId)
        } ?: return@withContext null

        val posts = ArrayList<FeedPost>()
        for (i in 0 until arr.length()) {
            val docObj = arr.optJSONObject(i)?.optJSONObject("document") ?: continue
            parseFeedPostDocument(docObj)?.let { posts.add(it) }
        }
        posts.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
    }

    /**
     * Queries all messages for [userId] (`receiverId == userId`, `senderId == userId`, and
     * `groupMemberIds array-contains userId`) directly from the Firestore server over HTTPS REST.
     */
    suspend fun queryUserMessages(
        auth: FirebaseAuth?,
        userId: String,
        databaseId: String = DEFAULT_DATABASE_ID,
        projectId: String = DEFAULT_PROJECT_ID,
        limitPerQuery: Int = 80
    ): List<DirectMessage>? = withContext(Dispatchers.IO) {
        val trimmedUid = userId.trim()
        if (trimmedUid.isBlank() || isRobolectricRuntime()) return@withContext null
        val idToken = resolveIdToken(auth) ?: return@withContext null

        fun buildMessageFilterQuery(fieldPath: String, op: String): JSONObject {
            return JSONObject()
                .put("from", JSONArray().put(JSONObject().put("collectionId", "messages")))
                .put(
                    "where",
                    JSONObject().put(
                        "fieldFilter",
                        JSONObject()
                            .put("field", JSONObject().put("fieldPath", fieldPath))
                            .put("op", op)
                            .put("value", JSONObject().put("stringValue", trimmedUid))
                    )
                )
                .put("limit", limitPerQuery)
        }

        coroutineScope {
            val receivedDef = async {
                executeRunQueryHttp(
                    idToken = idToken,
                    structuredQuery = buildMessageFilterQuery("receiverId", "EQUAL"),
                    databaseId = databaseId,
                    projectId = projectId
                )
            }
            val sentDef = async {
                executeRunQueryHttp(
                    idToken = idToken,
                    structuredQuery = buildMessageFilterQuery("senderId", "EQUAL"),
                    databaseId = databaseId,
                    projectId = projectId
                )
            }
            val groupDef = async {
                executeRunQueryHttp(
                    idToken = idToken,
                    structuredQuery = buildMessageFilterQuery("groupMemberIds", "ARRAY_CONTAINS"),
                    databaseId = databaseId,
                    projectId = projectId
                )
            }

            val rArr = receivedDef.await()
            val sArr = sentDef.await()
            val gArr = groupDef.await()

            if (rArr == null && sArr == null && gArr == null) {
                return@coroutineScope null
            }

            val merged = LinkedHashMap<String, DirectMessage>()
            for (arr in listOfNotNull(rArr, sArr, gArr)) {
                for (i in 0 until arr.length()) {
                    val docObj = arr.optJSONObject(i)?.optJSONObject("document") ?: continue
                    val parsed = parseDirectMessageDocument(docObj) ?: continue
                    if (parsed.id.isNotBlank()) {
                        merged[parsed.id] = parsed
                    }
                }
            }
            merged.values.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    private fun executeGetDocumentHttp(
        idToken: String?,
        collection: String,
        documentId: String,
        databaseId: String = DEFAULT_DATABASE_ID,
        projectId: String = DEFAULT_PROJECT_ID
    ): JSONObject? {
        val resolvedProject = projectId.trim().ifBlank { DEFAULT_PROJECT_ID }
        val resolvedDb = databaseId.trim().ifBlank { DEFAULT_DATABASE_ID }
        val endpoint = "https://firestore.googleapis.com/v1/projects/$resolvedProject/databases/$resolvedDb/documents/$collection/$documentId"
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 14_000
                if (!idToken.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $idToken")
                }
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
            }
            if (conn.responseCode !in 200..299) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            if (body.isBlank()) return null
            JSONObject(body)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    /**
     * Fetches the offloaded `mediaDataUrl` from `/feed_post_attachments/{attachmentDocId}` directly via HTTPS REST.
     */
    suspend fun fetchAttachmentMediaDataUrl(
        attachmentDocId: String,
        auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull(),
        databaseId: String = DEFAULT_DATABASE_ID,
        projectId: String = DEFAULT_PROJECT_ID
    ): String? = withContext(Dispatchers.IO) {
        val trimmedId = attachmentDocId.trim()
        if (trimmedId.isBlank() || isRobolectricRuntime()) return@withContext null
        val idToken = resolveIdToken(auth)
        val docObj = executeGetDocumentHttp(
            idToken = idToken,
            collection = "feed_post_attachments",
            documentId = trimmedId,
            databaseId = databaseId,
            projectId = projectId
        ) ?: return@withContext null
        val fields = docObj.optJSONObject("fields") ?: return@withContext null
        extractStringField(fields, "mediaDataUrl").takeIf { it.startsWith("data:image", ignoreCase = true) }
            ?: extractStringField(fields, "downloadUrl").takeIf { it.startsWith("data:image", ignoreCase = true) }
            ?: extractStringField(fields, "extendedText").takeIf { it.startsWith("data:image", ignoreCase = true) }
    }

    /**
     * Fetches a `/users/{userId}` document directly via HTTPS REST so partner names, avatars,
     * and last-seen timestamps resolve even if gRPC document listeners are delayed.
     */
    suspend fun fetchUserProfile(
        auth: FirebaseAuth?,
        userId: String,
        databaseId: String = DEFAULT_DATABASE_ID,
        projectId: String = DEFAULT_PROJECT_ID
    ): com.example.data.model.UserProfile? = withContext(Dispatchers.IO) {
        val trimmedUid = userId.trim()
        if (trimmedUid.isBlank() || isRobolectricRuntime()) return@withContext null
        val idToken = resolveIdToken(auth) ?: return@withContext null
        val docObj = executeGetDocumentHttp(
            idToken = idToken,
            collection = "users",
            documentId = trimmedUid,
            databaseId = databaseId,
            projectId = projectId
        ) ?: return@withContext null
        val fields = docObj.optJSONObject("fields") ?: return@withContext null
        val createTimeFallback = parseIsoTimestamp(docObj.optString("createTime", ""))
        val updateTimeFallback = parseIsoTimestamp(docObj.optString("updateTime", ""))
        com.example.data.model.UserProfile(
            id = trimmedUid,
            userId = extractStringField(fields, "userId", trimmedUid).ifBlank { trimmedUid },
            fullName = extractStringField(fields, "fullName"),
            accountType = extractStringField(fields, "accountType", "khadim"),
            district = extractStringField(fields, "district"),
            designation = extractStringField(fields, "designation"),
            qualification = extractStringField(fields, "qualification"),
            experience = extractStringField(fields, "experience"),
            preferredDistricts = extractStringField(fields, "preferredDistricts"),
            bio = extractStringField(fields, "bio"),
            contactInfo = extractStringField(fields, "contactInfo"),
            contactEmail = extractStringField(fields, "contactEmail"),
            phonePrivacy = com.example.data.model.UserProfile.normalizePrivacyMode(extractStringField(fields, "phonePrivacy")),
            emailPrivacy = com.example.data.model.UserProfile.normalizePrivacyMode(extractStringField(fields, "emailPrivacy")),
            madrasaName = extractStringField(fields, "madrasaName"),
            photoUrl = extractStringField(fields, "photoUrl"),
            isOnline = extractBooleanField(fields, "isOnline", false),
            lastSeenAt = extractTimestampField(fields, "lastSeenAt") ?: updateTimeFallback,
            typingToUserId = extractStringField(fields, "typingToUserId"),
            typingUpdatedAt = extractTimestampField(fields, "typingUpdatedAt"),
            blockedUserIds = extractStringListField(fields, "blockedUserIds"),
            chatThemes = extractStringMapField(fields, "chatThemes"),
            isVerified = extractBooleanField(fields, "isVerified", false),
            createdAt = extractTimestampField(fields, "createdAt") ?: createTimeFallback,
            updatedAt = extractTimestampField(fields, "updatedAt") ?: updateTimeFallback
        )
    }
}

