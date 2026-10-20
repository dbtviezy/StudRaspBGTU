package ru.bgtu.schedule.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

data class RatingSummary(
    val teacher: String,
    val count: Int,
    val overall: Double,
    val delivery: Double,
    val strictness: Double,
    val loyalty: Double,
)

data class TeacherReview(
    val overall: Int,
    val delivery: Int,
    val strictness: Int,
    val loyalty: Int,
    val text: String,
    val updatedAt: String,
)

data class TeacherRatings(val summary: RatingSummary?, val reviews: List<TeacherReview>, val mine: TeacherReview?)

sealed class RatingResult {
    data object Ok : RatingResult()
    data class Failed(val code: String) : RatingResult()
}

object RatingApi {
    const val BASE_URL = "https://studraspbgtu.vercel.app"
    const val MAX_TEXT = 300

    @Volatile
    private var summaries: Map<String, RatingSummary>? = null

    fun deviceId(context: Context): String {
        val sp = context.applicationContext.getSharedPreferences("bgtu_device", Context.MODE_PRIVATE)
        sp.getString("id", null)?.let { return it }
        val id = UUID.randomUUID().toString()
        sp.edit().putString("id", id).apply()
        return id
    }

    fun cachedSummaries(): Map<String, RatingSummary> = summaries.orEmpty()

    suspend fun loadSummaries(force: Boolean = false): Map<String, RatingSummary>? {
        if (!force) summaries?.let { return it }
        val root = request("GET", "/api/ratings") ?: return summaries
        val items = root.optJSONArray("items") ?: return summaries
        val next = buildMap {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val summary = summaryOf(item) ?: continue
                put(summary.teacher, summary)
            }
        }
        summaries = next
        return next
    }

    suspend fun teacher(context: Context, name: String): TeacherRatings? {
        val query = URLEncoder.encode(name.trim(), "UTF-8")
        val root = request("GET", "/api/ratings/teacher?name=$query", deviceId = deviceId(context)) ?: return null
        val reviews = root.optJSONArray("reviews")
        val list = buildList {
            if (reviews != null) {
                for (index in 0 until reviews.length()) reviews.optJSONObject(index)?.let { add(reviewOf(it)) }
            }
        }
        val summary = root.optJSONObject("summary")?.let { summaryOf(it) }
        summary?.let { summaries = cachedSummaries() + (it.teacher to it) }
        return TeacherRatings(summary, list, root.optJSONObject("mine")?.let { reviewOf(it) })
    }

    suspend fun submit(
        context: Context,
        teacher: String,
        overall: Int,
        delivery: Int,
        strictness: Int,
        loyalty: Int,
        text: String,
    ): RatingResult {
        val body = JSONObject()
            .put("deviceId", deviceId(context))
            .put("teacher", teacher.trim())
            .put("overall", overall)
            .put("delivery", delivery)
            .put("strictness", strictness)
            .put("loyalty", loyalty)
            .put("text", text.trim().take(MAX_TEXT))
        return send("POST", body)
    }

    suspend fun delete(context: Context, teacher: String): RatingResult {
        val body = JSONObject().put("deviceId", deviceId(context)).put("teacher", teacher.trim())
        return send("DELETE", body)
    }

    private suspend fun send(method: String, body: JSONObject): RatingResult {
        val response = exchange(method, "/api/ratings", body, null) ?: return RatingResult.Failed("offline")
        return if (response.first in 200..299) {
            summaries = null
            RatingResult.Ok
        } else {
            val code = runCatching { JSONObject(response.second).optString("error") }.getOrNull().orEmpty()
            RatingResult.Failed(code.ifBlank { "http_${response.first}" })
        }
    }

    private suspend fun request(method: String, path: String, deviceId: String? = null): JSONObject? {
        val response = exchange(method, path, null, deviceId) ?: return null
        if (response.first !in 200..299) return null
        return runCatching { JSONObject(response.second) }.getOrNull()
    }

    private suspend fun exchange(method: String, path: String, body: JSONObject?, deviceId: String?): Pair<Int, String>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(BASE_URL + path).openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = method
                    connection.connectTimeout = 8000
                    connection.readTimeout = 10000
                    connection.setRequestProperty("Accept", "application/json")
                    deviceId?.let { connection.setRequestProperty("X-Device-Id", it) }
                    if (body != null) {
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    }
                    val code = connection.responseCode
                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                    code to (stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty())
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }

    private fun summaryOf(item: JSONObject): RatingSummary? {
        val teacher = item.optString("teacher").trim()
        if (teacher.isBlank()) return null
        return RatingSummary(
            teacher = teacher,
            count = item.optInt("count"),
            overall = item.optDouble("overall", 0.0),
            delivery = item.optDouble("delivery", 0.0),
            strictness = item.optDouble("strictness", 0.0),
            loyalty = item.optDouble("loyalty", 0.0),
        )
    }

    private fun reviewOf(item: JSONObject) = TeacherReview(
        overall = item.optInt("overall"),
        delivery = item.optInt("delivery"),
        strictness = item.optInt("strictness"),
        loyalty = item.optInt("loyalty"),
        text = item.optString("text"),
        updatedAt = item.optString("updatedAt"),
    )
}
