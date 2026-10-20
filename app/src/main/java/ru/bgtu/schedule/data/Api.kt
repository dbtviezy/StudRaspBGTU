package ru.bgtu.schedule.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Error code from the server (`{"error": "..."}`) or `offline` when the request did not reach it.
 * [extra] holds the other fields of the error body, e.g. `scope`, `until`, `reason` of a restriction.
 */
class ApiFailure(val code: String, val extra: JSONObject = JSONObject()) : Exception(code)

data class HttpReply(val status: Int, val body: String) {
    val ok: Boolean get() = status in 200..299

    fun json(): JSONObject? = runCatching { JSONObject(body) }.getOrNull()

    fun array(): JSONArray? = runCatching { JSONArray(body) }.getOrNull()

    fun errorCode(): String {
        val code = json()?.optString("error").orEmpty()
        return code.ifBlank { if (status >= 500) "server_error" else "bad_request" }
    }

    fun failure(): ApiFailure = ApiFailure(errorCode(), json() ?: JSONObject())
}

object Http {
    const val BASE_URL = "https://studraspbgtu-web.vercel.app"

    /** Shown in «Мои устройства». */
    private val deviceName: String by lazy {
        val maker = android.os.Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val model = android.os.Build.MODEL.orEmpty()
        val name = if (model.startsWith(maker, ignoreCase = true)) model else "$maker $model"
        "${name.trim()} · Android".take(80)
    }

    /** Returns null when the server could not be reached at all. */
    suspend fun call(
        method: String,
        url: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): HttpReply? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(if (url.startsWith("http")) url else BASE_URL + url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = method
                connection.connectTimeout = 8000
                connection.readTimeout = 12000
                connection.useCaches = false
                connection.setRequestProperty("Accept", "application/json")
                if (!url.startsWith("http")) {
                    connection.setRequestProperty("X-Device-Name", java.net.URLEncoder.encode(deviceName, "UTF-8"))
                }
                headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                HttpReply(status, stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty())
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    /** Calls our own API and returns the JSON body, or throws [ApiFailure]. */
    suspend fun api(
        method: String,
        path: String,
        body: JSONObject? = null,
        headers: Map<String, String> = emptyMap(),
    ): JSONObject {
        val reply = call(method, path, body?.toString(), headers) ?: throw ApiFailure("offline")
        if (!reply.ok) throw reply.failure()
        return reply.json() ?: JSONObject()
    }
}

fun JSONObject.stringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T?): List<T> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            transform(item)?.let { add(it) }
        }
    }
}
