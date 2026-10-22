package ru.bgtu.schedule.data

import android.content.Context
import org.json.JSONObject
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
    val id: Long,
    val overall: Int,
    val delivery: Int,
    val strictness: Int,
    val loyalty: Int,
    val text: String,
    val updatedAt: String,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val myVote: Int = 0,
    /** My review left from this device without an account. */
    val claimable: Boolean = false,
    val hidden: Boolean = false,
)

data class TeacherRatings(
    val summary: RatingSummary?,
    val reviews: List<TeacherReview>,
    val mine: TeacherReview?,
    val legacyUntil: String? = null,
)

data class VoteResult(val likes: Int, val dislikes: Int, val myVote: Int)

data class Claimable(val count: Int, val until: String?)

sealed class RatingResult {
    data object Ok : RatingResult()
    data class Failed(val failure: ApiFailure) : RatingResult() {
        val code: String get() = failure.code
    }
}

object RatingApi {
    const val BASE_URL = Http.BASE_URL
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
        val reply = Http.call("GET", "/api/ratings")?.takeIf { it.ok } ?: return summaries
        val next = reply.json()?.optJSONArray("items").mapObjects { summaryOf(it) }.associateBy { it.teacher }
        summaries = next
        return next
    }

    suspend fun teacher(context: Context, name: String): TeacherRatings? {
        val query = URLEncoder.encode(name.trim(), "UTF-8")
        val headers = Account.authHeaders() + ("X-Device-Id" to deviceId(context))
        val reply = Http.call("GET", "/api/ratings/teacher?name=$query", headers = headers)
        val root = reply?.takeIf { it.ok }?.json() ?: return null
        val list = root.optJSONArray("reviews").mapObjects { reviewOf(it) }
        val summary = root.optJSONObject("summary")?.let { summaryOf(it) }
        summary?.let { summaries = cachedSummaries() + (it.teacher to it) }
        return TeacherRatings(summary, list, root.optJSONObject("mine")?.let { reviewOf(it) }, root.stringOrNull("legacyUntil"))
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

    /** [value] is 1 (like), -1 (dislike) or 0 (take the vote back). */
    suspend fun vote(context: Context, reviewId: Long, value: Int): VoteResult {
        val body = JSONObject().put("deviceId", deviceId(context)).put("reviewId", reviewId).put("value", value)
        val root = Http.api("POST", "/api/ratings/vote", body, Account.authHeaders())
        return VoteResult(root.optInt("likes"), root.optInt("dislikes"), root.optInt("myVote"))
    }

    suspend fun report(context: Context, kind: String, target: Long, reason: String) {
        val body = JSONObject()
            .put("deviceId", deviceId(context))
            .put("kind", kind)
            .put("target", target)
            .put("reason", reason.trim().take(200))
        Http.api("POST", "/api/reports", body, Account.authHeaders())
    }

    /** Old reviews left from this device without an account. */
    suspend fun claimable(context: Context): Claimable? {
        val reply = Http.call("GET", "/api/ratings/claim", headers = mapOf("X-Device-Id" to deviceId(context)))
        val root = reply?.takeIf { it.ok }?.json() ?: return null
        return Claimable(root.optInt("count"), root.stringOrNull("until"))
    }

    /** Moves this device's old reviews, votes and reports to the signed-in account. */
    suspend fun claim(context: Context): Int {
        val root = Http.api("POST", "/api/ratings/claim", JSONObject().put("deviceId", deviceId(context)), Account.authHeaders())
        summaries = null
        return root.optInt("claimed")
    }

    private suspend fun send(method: String, body: JSONObject): RatingResult {
        val response = Http.call(method, "/api/ratings", body.toString(), Account.authHeaders())
            ?: return RatingResult.Failed(ApiFailure("offline"))
        return if (response.ok) {
            summaries = null
            RatingResult.Ok
        } else {
            RatingResult.Failed(response.failure())
        }
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
        id = item.optLong("id"),
        overall = item.optInt("overall"),
        delivery = item.optInt("delivery"),
        strictness = item.optInt("strictness"),
        loyalty = item.optInt("loyalty"),
        text = item.optString("text"),
        updatedAt = item.optString("updatedAt"),
        likes = item.optInt("likes"),
        dislikes = item.optInt("dislikes"),
        myVote = item.optInt("myVote"),
        claimable = item.optBoolean("claimable"),
        hidden = item.optBoolean("hidden"),
    )
}
