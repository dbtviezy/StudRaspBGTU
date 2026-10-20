package ru.bgtu.schedule.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import ru.bgtu.schedule.BuildConfig

data class User(
    val id: String,
    val name: String,
    val login: String?,
    val telegram: Boolean,
    val google: Boolean,
) {
    /** Short public ID other students can use to start a direct chat. */
    val shortId: String get() = id.replace("-", "").take(8).uppercase()

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("login", login ?: JSONObject.NULL)
        .put("telegram", telegram)
        .put("google", google)

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
            )
        }
    }
}

data class AccountState(val token: String?, val user: User?) {
    val signedIn: Boolean get() = token != null && user != null
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
        publish(AccountState(token, user))
    }

    /** Our API with the session token attached; a 401 on an authorised call signs the device out. */
    suspend fun api(method: String, path: String, body: JSONObject? = null, auth: Boolean = true): JSONObject {
        val token = current.token
        val headers = if (auth && token != null) mapOf("Authorization" to "Bearer $token") else emptyMap()
        val reply = Http.call(method, path, body?.toString(), headers) ?: throw ApiFailure("offline")
        if (reply.status == 401 && auth && token != null) publish(AccountState(null, null))
        if (!reply.ok) throw ApiFailure(reply.errorCode())
        return reply.json() ?: JSONObject()
    }

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

    suspend fun signOut(everywhere: Boolean = false, deleteAccount: Boolean = false) {
        val body = JSONObject()
        if (everywhere) body.put("everywhere", true)
        if (deleteAccount) body.put("deleteAccount", true)
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
