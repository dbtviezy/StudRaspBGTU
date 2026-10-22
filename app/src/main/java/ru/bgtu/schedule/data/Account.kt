package ru.bgtu.schedule.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import ru.bgtu.schedule.BuildConfig
import java.time.Instant

data class User(
    val id: String,
    val name: String,
    val login: String?,
    val telegram: Boolean,
    val google: Boolean,
    val createdAt: String? = null,
) {
    /** Short public ID other students can use to start a direct chat. */
    val shortId: String get() = id.replace("-", "").take(8).uppercase()

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("login", login ?: JSONObject.NULL)
        .put("telegram", telegram)
        .put("google", google)
        .put("createdAt", createdAt ?: JSONObject.NULL)

    companion object {
        fun of(json: JSONObject?): User? {
            if (json == null) return null
            val id = json.optString("id")
            if (id.isBlank()) return null
            return User(
                id = id,
                name = json.optString("name"),
                login = json.stringOrNull("login"),
                telegram = json.optBoolean("telegram"),
                google = json.optBoolean("google"),
                createdAt = json.stringOrNull("createdAt"),
            )
        }
    }
}

/** A partial or full ban: `scope` is all, reviews, votes, reports, chat or chat_create. */
data class Restriction(val scope: String, val until: String?, val reason: String) {
    companion object {
        fun of(json: JSONObject) = Restriction(json.optString("scope", "all"), json.stringOrNull("until"), json.optString("reason"))
    }
}

data class RepRequest(
    val id: Long,
    val groupId: String,
    val groupName: String,
    val comment: String,
    val status: String,
    val createdAt: String,
) {
    companion object {
        fun of(json: JSONObject?): RepRequest? {
            if (json == null) return null
            return RepRequest(
                id = json.optLong("id"),
                groupId = json.optString("groupId"),
                groupName = json.optString("groupName"),
                comment = json.optString("comment"),
                status = json.optString("status"),
                createdAt = json.optString("createdAt"),
            )
        }
    }
}

data class DeviceSession(val id: Long, val device: String, val createdAt: String, val usedAt: String, val current: Boolean)

data class MyReview(
    val id: Long,
    val teacher: String,
    val overall: Int,
    val text: String,
    val updatedAt: String,
    val hidden: Boolean,
    val likes: Int,
    val dislikes: Int,
)

/** Why the account can't review yet. */
sealed class ReviewBlock {
    data object NoAccount : ReviewBlock()
    data object NeedSocial : ReviewBlock()
    data class TooNew(val until: String) : ReviewBlock()
}

data class AccountState(
    val token: String?,
    val user: User?,
    val restrictions: List<Restriction> = emptyList(),
    val rep: RepRequest? = null,
) {
    val signedIn: Boolean get() = token != null && user != null

    fun restriction(vararg scopes: String): Restriction? = restrictions.firstOrNull { it.scope == "all" || it.scope in scopes }

    /** null when the account may leave reviews. */
    fun reviewBlock(): ReviewBlock? {
        val account = user ?: return ReviewBlock.NoAccount
        if (token == null) return ReviewBlock.NoAccount
        if (!account.telegram && !account.google) return ReviewBlock.NeedSocial
        val created = account.createdAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
        val ready = created.plusSeconds(86_400)
        return if (ready.isAfter(Instant.now())) ReviewBlock.TooNew(ready.toString()) else null
    }
}

data class AuthConfig(val password: Boolean, val telegramBot: String?, val googleClientId: String?, val chat: Boolean)

object Account {
    private const val FILE = "bgtu_account"
    const val AUTH_SCHEME_STUDENT = "studraspbgtu"
    const val AUTH_SCHEME_ADMIN = "studraspbgtu-admin"

    private var prefs: SharedPreferences? = null
    private val flow = MutableStateFlow(AccountState(null, null))
    val state: StateFlow<AccountState> = flow.asStateFlow()

    @Volatile
    private var config: AuthConfig? = null

    /** One-time code from the `studraspbgtu://auth?code=` link, waiting to be exchanged. */
    val pendingCode = MutableStateFlow<String?>(null)

    fun init(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs = sp
        val token = sp.getString("token", null)
        val user = sp.getString("user", null)?.let { runCatching { User.of(JSONObject(it)) }.getOrNull() }
        flow.value = if (token != null && user != null) AccountState(token, user) else AccountState(null, null)
    }

    val current: AccountState get() = flow.value

    private fun publish(next: AccountState) {
        flow.value = next
        prefs?.edit()?.apply {
            if (next.token != null) putString("token", next.token) else remove("token")
            if (next.user != null) putString("user", next.user.toJson().toString()) else remove("user")
            apply()
        }
    }

    private fun signedIn(root: JSONObject) {
        val user = User.of(root.optJSONObject("user")) ?: throw ApiFailure("server_error")
        val token = root.stringOrNull("token") ?: current.token ?: throw ApiFailure("server_error")
        val same = current.user?.id == user.id
        val restrictions = if (root.has("restrictions")) {
            root.optJSONArray("restrictions").mapObjects { Restriction.of(it) }
        } else if (same) current.restrictions else emptyList()
        val rep = if (root.has("rep")) RepRequest.of(root.optJSONObject("rep")) else if (same) current.rep else null
        publish(AccountState(token, user, restrictions, rep))
    }

    /** Our API with the session token attached; a 401 on an authorised call signs the device out. */
    suspend fun api(method: String, path: String, body: JSONObject? = null, auth: Boolean = true): JSONObject {
        val token = current.token
        val headers = if (auth && token != null) mapOf("Authorization" to "Bearer $token") else emptyMap()
        val reply = Http.call(method, path, body?.toString(), headers) ?: throw ApiFailure("offline")
        if (reply.status == 401 && auth && token != null) publish(AccountState(null, null))
        if (!reply.ok) throw reply.failure()
        return reply.json() ?: JSONObject()
    }

    /** Authorization header for calls made outside [api]. */
    fun authHeaders(): Map<String, String> = current.token?.let { mapOf("Authorization" to "Bearer $it") }.orEmpty()

    suspend fun config(): AuthConfig {
        config?.let { return it }
        val root = api("GET", "/api/auth/config", auth = false)
        return AuthConfig(
            password = root.optBoolean("password"),
            telegramBot = root.stringOrNull("telegramBot"),
            googleClientId = root.stringOrNull("googleClientId"),
            chat = root.optBoolean("chat"),
        ).also { config = it }
    }

    suspend fun register(login: String, password: String, name: String) {
        val body = JSONObject().put("login", login.trim()).put("password", password).put("name", name.trim())
        signedIn(api("POST", "/api/auth/register", body, auth = false))
    }

    suspend fun signIn(login: String, password: String) {
        val body = JSONObject().put("login", login.trim()).put("password", password)
        signedIn(api("POST", "/api/auth/login", body, auth = false))
    }

    suspend fun exchange(code: String) {
        signedIn(api("POST", "/api/auth/exchange", JSONObject().put("code", code), auth = false))
    }

    suspend fun update(patch: JSONObject) {
        signedIn(api("POST", "/api/auth/me", patch))
    }

    suspend fun refresh() {
        if (current.token == null) return
        runCatching { signedIn(api("GET", "/api/auth/me")) }
    }

    suspend fun sessions(): List<DeviceSession> =
        api("GET", "/api/auth/sessions").optJSONArray("items").mapObjects {
            DeviceSession(it.optLong("id"), it.optString("device"), it.optString("createdAt"), it.optString("usedAt"), it.optBoolean("current"))
        }

    suspend fun endSession(id: Long) {
        api("DELETE", "/api/auth/sessions", JSONObject().put("id", id))
    }

    suspend fun requestRep(groupId: String, groupName: String, comment: String) {
        val body = JSONObject().put("groupId", groupId).put("groupName", groupName).put("comment", comment.trim().take(300))
        val rep = RepRequest.of(api("POST", "/api/reps", body).optJSONObject("rep"))
        flow.value = current.copy(rep = rep)
    }

    suspend fun cancelRep() {
        val rep = RepRequest.of(api("DELETE", "/api/reps").optJSONObject("rep"))
        flow.value = current.copy(rep = rep)
    }

    suspend fun myReviews(): List<MyReview> =
        api("GET", "/api/ratings/mine").optJSONArray("items").mapObjects {
            MyReview(
                id = it.optLong("id"),
                teacher = it.optString("teacher"),
                overall = it.optInt("overall"),
                text = it.optString("text"),
                updatedAt = it.optString("updatedAt"),
                hidden = it.optBoolean("hidden"),
                likes = it.optInt("likes"),
                dislikes = it.optInt("dislikes"),
            )
        }

    suspend fun signOut(everywhere: Boolean = false, deleteAccount: Boolean = false, keepReviews: Boolean = false) {
        val body = JSONObject()
        if (everywhere) body.put("everywhere", true)
        if (deleteAccount) body.put("deleteAccount", true)
        if (keepReviews) body.put("keepReviews", true)
        try {
            api("DELETE", "/api/auth/me", body)
        } catch (error: ApiFailure) {
            if (deleteAccount) throw error
        }
        publish(AccountState(null, null))
    }

    /** Local sign-out without a server call, used by the data reset. */
    fun forget() {
        publish(AccountState(null, null))
    }

    fun authScheme(): String = if (BuildConfig.ADMIN) AUTH_SCHEME_ADMIN else AUTH_SCHEME_STUDENT

    /** Telegram and Google sign-in happen on the website, which sends a one-time code back to the app. */
    fun browserSignInUrl(provider: String, lang: String): String =
        "${Http.BASE_URL}/auth/app?provider=$provider&return=${authScheme()}&lang=$lang"

    fun acceptLink(uri: Uri?) {
        if (uri == null || uri.scheme != authScheme() || uri.host != "auth") return
        uri.getQueryParameter("code")?.takeIf { it.isNotBlank() }?.let { pendingCode.value = it }
    }
}
