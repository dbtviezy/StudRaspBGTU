package ru.bgtu.schedule.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import ru.bgtu.schedule.BuildConfig
import java.net.URLEncoder

data class AdminStats(
    val users: Int,
    val activeUsers: Int,
    val reviews: Int,
    val hiddenReviews: Int,
    val newReviews: Int,
    val ratedTeachers: Int,
    val votes: Int,
    val openReports: Int,
    val activeBans: Int,
    val announcements: Int,
    val chats: Int?,
    val messages: Int?,
    val chatEnabled: Boolean,
)

data class AdminReport(
    val kind: String,
    val target: Long,
    val count: Int,
    val lastAt: String?,
    val snapshot: String,
    val reasons: List<String>,
)

data class AdminReview(
    val id: Long,
    val teacher: String,
    val overall: Int,
    val text: String,
    val hidden: Boolean,
    val updatedAt: String?,
    val likes: Int,
    val dislikes: Int,
    val reports: Int,
)

data class AdminMessage(
    val id: Long,
    val chatId: String,
    val userId: String,
    val author: String,
    val body: String,
    val createdAt: String?,
    val chat: String,
)

data class AdminBan(
    val id: Long,
    val userId: String?,
    val device: Boolean,
    val name: String?,
    val login: String?,
    val reason: String,
    val createdAt: String?,
    val until: String?,
)

data class AdminLogEntry(val id: Long, val action: String, val target: String, val detail: String, val createdAt: String?)

data class Announcement(val id: Long, val text: String, val createdAt: String?, val until: String? = null, val active: Boolean = true)

data class AdminUser(
    val id: String,
    val name: String,
    val login: String?,
    val telegram: Boolean,
    val google: Boolean,
    val createdAt: String?,
    val seenAt: String?,
    val banned: Boolean,
)

/** Moderation API; only the admin build signs in here, with the password from ADMIN_PASSWORD on the server. */
object AdminApi {
    private const val FILE = "bgtu_admin"
    private var prefs: SharedPreferences? = null
    private val tokenFlow = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = tokenFlow.asStateFlow()

    val enabled: Boolean get() = BuildConfig.ADMIN

    fun init(context: Context) {
        if (!enabled || prefs != null) return
        val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs = sp
        val saved = sp.getString("token", null)
        val expires = sp.getLong("expiresAt", 0L)
        tokenFlow.value = saved?.takeIf { expires > System.currentTimeMillis() }
    }

    val signedIn: Boolean get() = tokenFlow.value != null

    suspend fun signIn(password: String) {
        val root = Http.api("POST", "/api/admin/login", JSONObject().put("password", password))
        val value = root.optString("token")
        if (value.isBlank()) throw ApiFailure("server_error")
        prefs?.edit()?.putString("token", value)?.putLong("expiresAt", root.optLong("expiresAt"))?.apply()
        tokenFlow.value = value
    }

    fun signOut() {
        prefs?.edit()?.clear()?.apply()
        tokenFlow.value = null
    }

    private suspend fun call(method: String, path: String, body: JSONObject? = null): JSONObject {
        val current = tokenFlow.value ?: throw ApiFailure("unauthorized")
        val reply = Http.call(method, path, body?.toString(), mapOf("X-Admin-Token" to current)) ?: throw ApiFailure("offline")
        if (reply.status == 401) signOut()
        if (!reply.ok) throw ApiFailure(reply.errorCode())
        return reply.json() ?: JSONObject()
    }

    private suspend fun view(name: String, query: String = ""): JSONObject {
        val q = if (query.isBlank()) "" else "&q=${URLEncoder.encode(query.trim(), "UTF-8")}"
        return call("GET", "/api/admin?view=$name$q")
    }

    suspend fun action(name: String, fields: Map<String, Any?> = emptyMap()) {
        val body = JSONObject().put("action", name)
        fields.forEach { (key, value) -> body.put(key, value ?: JSONObject.NULL) }
        call("POST", "/api/admin", body)
    }

    suspend fun stats(): AdminStats {
        val root = view("stats")
        return AdminStats(
            users = root.optInt("users"),
            activeUsers = root.optInt("activeUsers"),
            reviews = root.optInt("reviews"),
            hiddenReviews = root.optInt("hiddenReviews"),
            newReviews = root.optInt("newReviews"),
            ratedTeachers = root.optInt("ratedTeachers"),
            votes = root.optInt("votes"),
            openReports = root.optInt("openReports"),
            activeBans = root.optInt("activeBans"),
            announcements = root.optInt("announcements"),
            chats = if (root.isNull("chats")) null else root.optInt("chats"),
            messages = if (root.isNull("messages")) null else root.optInt("messages"),
            chatEnabled = root.optBoolean("chatEnabled"),
        )
    }

    suspend fun reports(): List<AdminReport> = view("reports").optJSONArray("items").mapObjects { item ->
        val reasons = item.optJSONArray("reasons")
        AdminReport(
            kind = item.optString("kind"),
            target = item.optString("target").toLongOrNull() ?: return@mapObjects null,
            count = item.optInt("count"),
            lastAt = item.stringOrNull("lastAt"),
            snapshot = item.optString("snapshot"),
            reasons = List(reasons?.length() ?: 0) { reasons!!.optString(it) }.filter { it.isNotBlank() },
        )
    }

    suspend fun reviews(query: String): List<AdminReview> = view("reviews", query).optJSONArray("items").mapObjects { item ->
        AdminReview(
            id = item.optLong("id"),
            teacher = item.optString("teacher"),
            overall = item.optInt("overall"),
            text = item.optString("text"),
            hidden = item.optBoolean("hidden"),
            updatedAt = item.stringOrNull("updatedAt"),
            likes = item.optInt("likes"),
            dislikes = item.optInt("dislikes"),
            reports = item.optInt("reports"),
        )
    }

    suspend fun messages(): List<AdminMessage> = view("messages").optJSONArray("items").mapObjects { item ->
        AdminMessage(
            id = item.optLong("id"),
            chatId = item.optString("chatId"),
            userId = item.optString("userId"),
            author = item.optString("author"),
            body = item.optString("body"),
            createdAt = item.stringOrNull("createdAt"),
            chat = item.optString("chat"),
        )
    }

    suspend fun bans(): List<AdminBan> = view("bans").optJSONArray("items").mapObjects { item ->
        AdminBan(
            id = item.optLong("id"),
            userId = item.stringOrNull("userId"),
            device = item.optBoolean("device"),
            name = item.stringOrNull("name"),
            login = item.stringOrNull("login"),
            reason = item.optString("reason"),
            createdAt = item.stringOrNull("createdAt"),
            until = item.stringOrNull("until"),
        )
    }

    suspend fun announcements(): List<Announcement> = view("announcements").optJSONArray("items").mapObjects { item ->
        Announcement(
            id = item.optLong("id"),
            text = item.optString("text"),
            createdAt = item.stringOrNull("createdAt"),
            until = item.stringOrNull("until"),
            active = item.optBoolean("active"),
        )
    }

    suspend fun users(query: String): List<AdminUser> = view("users", query).optJSONArray("items").mapObjects { item ->
        AdminUser(
            id = item.optString("id"),
            name = item.optString("name"),
            login = item.stringOrNull("login"),
            telegram = item.optBoolean("telegram"),
            google = item.optBoolean("google"),
            createdAt = item.stringOrNull("createdAt"),
            seenAt = item.stringOrNull("seenAt"),
            banned = item.optBoolean("banned"),
        )
    }

    suspend fun log(): List<AdminLogEntry> = view("log").optJSONArray("items").mapObjects { item ->
        AdminLogEntry(
            id = item.optLong("id"),
            action = item.optString("action"),
            target = item.optString("target"),
            detail = item.optString("detail"),
            createdAt = item.stringOrNull("createdAt"),
        )
    }
}

/** Public announcements from the team, shown on the home screen. */
object Announcements {
    private const val FILE = "bgtu_announcements"

    suspend fun load(): List<Announcement> {
        val reply = Http.call("GET", "/api/announcements")?.takeIf { it.ok } ?: return emptyList()
        return reply.json()?.optJSONArray("items").mapObjects { item ->
            Announcement(item.optLong("id"), item.optString("text"), item.stringOrNull("createdAt"))
        }
    }

    fun dismissed(context: Context): Set<String> =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getStringSet("dismissed", emptySet()).orEmpty()

    fun dismiss(context: Context, id: Long) {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val next = dismissed(context) + id.toString()
        sp.edit().putStringSet("dismissed", next).apply()
    }
}
