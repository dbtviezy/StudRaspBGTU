package ru.bgtu.schedule.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

data class ChatInfo(
    val id: String,
    val kind: String,
    val title: String,
    val groupId: String?,
    val inviteCode: String?,
    val role: String,
    val accepted: Boolean,
    val peerName: String?,
    val lastId: Long?,
    val lastBody: String?,
    val lastAuthor: String?,
    val lastAt: String?,
    val unread: Int,
    val pinnedBody: String? = null,
    val pinnedAuthor: String? = null,
    /** I am the confirmed class rep of this chat's group. */
    val rep: Boolean = false,
) {
    val isDirect: Boolean get() = kind == "dm"
    val isOwner: Boolean get() = role == "owner"

    /** The creator, their helpers and the confirmed class rep can pin messages, run polls and post group tasks. */
    val isStaff: Boolean get() = !isDirect && accepted && (role == "owner" || role == "helper" || rep)
}

data class ChatMember(val userId: String, val name: String, val role: String, val rep: Boolean = false)

data class ChatPoll(
    val id: Long,
    val question: String,
    val options: List<String>,
    val multiple: Boolean,
    val closed: Boolean,
    val author: String,
    val counts: List<Int>,
    val mine: List<Int>,
    val voters: Int,
)

data class GroupTask(
    val id: Long,
    val chatId: String,
    val title: String,
    val note: String,
    val dueDate: String?,
    val dueTime: String?,
    val author: String,
) {
    val todoId: String get() = "group-$id"
}

data class ChatMessage(
    val id: Long?,
    val chatId: String,
    val userId: String,
    val author: String,
    val body: String,
    val createdAt: String,
    val clientId: String,
    val status: String,
) {
    val pending: Boolean get() = status == "pending"
    val failed: Boolean get() = status == "failed"
}

private data class ChatTicket(val url: String, val anonKey: String, val token: String, val expiresAt: Long, val userId: String)

/**
 * Chats live in Supabase; the app talks to its REST API with a short-lived token from our server.
 * Messages are cached on the phone, and messages written offline wait in an outbox.
 */
object ChatStore {
    const val MAX_MESSAGE = 1000
    private const val FILE = "bgtu_chat"
    private const val KEEP = 300
    private val TRANSIENT = setOf("offline", "server_error", "unauthorized", "rate_limited", "chat_not_configured", "chat_unavailable")

    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val flushLock = Mutex()
    private val ticketLock = Mutex()

    @Volatile
    private var ticket: ChatTicket? = null

    private val versionFlow = MutableStateFlow(0)
    val version: StateFlow<Int> = versionFlow.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    private fun emit() {
        versionFlow.value = versionFlow.value + 1
    }

    private fun readArray(key: String): JSONArray =
        prefs?.getString(key, null)?.let { runCatching { JSONArray(it) }.getOrNull() } ?: JSONArray()

    private fun writeArray(key: String, array: JSONArray) {
        prefs?.edit()?.putString(key, array.toString())?.apply()
    }

    private fun chatKey(id: String) = "chat.$id"

    fun cachedChats(): List<ChatInfo> = readArray("chats").mapObjects { chatOf(it) }

    private fun outbox(): List<ChatMessage> = readArray("outbox").mapObjects { messageOf(it, it.optString("status", "pending")) }

    private fun writeOutbox(items: List<ChatMessage>) {
        writeArray("outbox", JSONArray().apply { items.forEach { put(messageJson(it)) } })
    }

    fun cachedMessages(chatId: String): List<ChatMessage> {
        val sent = readArray(chatKey(chatId)).mapObjects { messageOf(it, "sent") }
        return sent + outbox().filter { it.chatId == chatId }
    }

    fun unreadTotal(): Int = cachedChats().sumOf { if (it.accepted) it.unread else 1 }

    private fun storeMessages(chatId: String, incoming: List<ChatMessage>, replace: Boolean = false) {
        val current = if (replace) emptyList() else readArray(chatKey(chatId)).mapObjects { messageOf(it, "sent") }
        val merged = (current + incoming).filter { it.id != null }.associateBy { it.id }.values
            .sortedBy { it.id }
            .takeLast(KEEP)
        writeArray(chatKey(chatId), JSONArray().apply { merged.forEach { put(messageJson(it)) } })
    }

    /** Drops everything cached on this phone; used on sign-out and data reset. */
    fun clear() {
        prefs?.edit()?.clear()?.apply()
        ticket = null
        emit()
    }

    private suspend fun ticket(force: Boolean = false): ChatTicket = ticketLock.withLock {
        val userId = Account.current.user?.id ?: throw ApiFailure("unauthorized")
        val cached = ticket
        if (!force && cached != null && cached.userId == userId && cached.expiresAt - System.currentTimeMillis() > 120_000) {
            return cached
        }
        val root = Account.api("POST", "/api/chat/token")
        ChatTicket(
            url = root.optString("url").trimEnd('/'),
            anonKey = root.optString("anonKey"),
            token = root.optString("token"),
            expiresAt = root.optLong("expiresAt"),
            userId = root.optString("userId"),
        ).also { ticket = it }
    }

    private fun supabaseError(status: Int, body: String): String {
        val message = runCatching { JSONObject(body).optString("message") }.getOrNull().orEmpty()
        listOf("rate_limited", "invalid_code", "muted", "no_create", "forbidden", "bad_request").firstOrNull { message.contains(it) }?.let { return it }
        if (status == 401 || status == 403) return "forbidden"
        return if (status >= 500) "server_error" else "bad_request"
    }

    private suspend fun rest(
        method: String,
        path: String,
        body: String? = null,
        prefer: String? = null,
        retry: Boolean = true,
    ): HttpReply {
        val current = ticket()
        val headers = buildMap {
            put("apikey", current.anonKey)
            put("Authorization", "Bearer ${current.token}")
            prefer?.let { put("Prefer", it) }
        }
        val reply = Http.call(method, "${current.url}/rest/v1/$path", body, headers) ?: throw ApiFailure("offline")
        if (reply.status == 401 && retry) {
            ticket(force = true)
            return rest(method, path, body, prefer, retry = false)
        }
        if (!reply.ok) throw ApiFailure(supabaseError(reply.status, reply.body))
        return reply
    }

    private suspend fun rpc(name: String, args: JSONObject = JSONObject()): HttpReply =
        rest("POST", "rpc/$name", args.toString())

    suspend fun refreshChats(): List<ChatInfo> {
        val rows = rpc("my_chats").array().mapObjects { chatOf(it) }
        writeArray("chats", JSONArray().apply { rows.forEach { put(chatJson(it)) } })
        emit()
        return rows
    }

    suspend fun refreshMessages(chatId: String) {
        val known = readArray(chatKey(chatId)).mapObjects { messageOf(it, "sent") }
        val lastId = known.lastOrNull()?.id ?: 0L
        val path = if (lastId > 0) {
            "messages?chat_id=eq.$chatId&id=gt.$lastId&order=id.asc&limit=200"
        } else {
            "messages?chat_id=eq.$chatId&order=id.desc&limit=100"
        }
        val rows = rest("GET", path).array().mapObjects { messageOf(it, "sent") }
        if (rows.isNotEmpty()) storeMessages(chatId, rows)
        emit()
    }

    /** Re-reads the last 100 messages, dropping ones deleted on the server (by the author or a moderator). */
    suspend fun resyncMessages(chatId: String) {
        val rows = rest("GET", "messages?chat_id=eq.$chatId&order=id.desc&limit=100").array().mapObjects { messageOf(it, "sent") }
        storeMessages(chatId, rows, replace = true)
        emit()
    }

    fun send(chatId: String, text: String) {
        val body = text.trim().take(MAX_MESSAGE)
        if (body.isEmpty()) return
        val user = Account.current.user
        val message = ChatMessage(
            id = null,
            chatId = chatId,
            userId = user?.id.orEmpty(),
            author = user?.name.orEmpty(),
            body = body,
            createdAt = Instant.now().toString(),
            clientId = UUID.randomUUID().toString(),
            status = "pending",
        )
        writeOutbox(outbox() + message)
        emit()
        flushSoon()
    }

    fun flushSoon() {
        scope.launch { flush() }
    }

    /** Sends queued messages; anything that fails because of the network stays queued for the next try. */
    suspend fun flush() {
        if (!Account.current.signedIn) return
        if (!flushLock.tryLock()) return
        try {
            for (message in outbox()) {
                if (message.failed) continue
                try {
                    val payload = JSONObject()
                        .put("chat_id", message.chatId)
                        .put("body", message.body)
                        .put("client_id", message.clientId)
                    val reply = rest(
                        "POST",
                        "messages?on_conflict=client_id",
                        payload.toString(),
                        prefer = "return=representation,resolution=ignore-duplicates",
                    )
                    writeOutbox(outbox().filterNot { it.clientId == message.clientId })
                    val rows = reply.array().mapObjects { messageOf(it, "sent") }
                    if (rows.isNotEmpty()) storeMessages(message.chatId, rows)
                    emit()
                } catch (error: ApiFailure) {
                    if (error.code in TRANSIENT) break
                    writeOutbox(outbox().map { if (it.clientId == message.clientId) it.copy(status = "failed") else it })
                    emit()
                }
            }
        } finally {
            flushLock.unlock()
        }
    }

    fun retry(clientId: String) {
        writeOutbox(outbox().map { if (it.clientId == clientId) it.copy(status = "pending") else it })
        emit()
        flushSoon()
    }

    fun discard(clientId: String) {
        writeOutbox(outbox().filterNot { it.clientId == clientId })
        emit()
    }

    suspend fun deleteOwn(chatId: String, id: Long) {
        rest("DELETE", "messages?id=eq.$id")
        forgetMessage(chatId, id)
    }

    fun forgetMessage(chatId: String, id: Long) {
        val left = readArray(chatKey(chatId)).mapObjects { messageOf(it, "sent") }.filterNot { it.id == id }
        writeArray(chatKey(chatId), JSONArray().apply { left.forEach { put(messageJson(it)) } })
        emit()
    }

    suspend fun createGroupChat(groupId: String, title: String): ChatInfo {
        val row = rpc("create_group_chat", JSONObject().put("p_group", groupId).put("p_title", title.trim())).json()
        refreshChats()
        return chatOf(row ?: throw ApiFailure("server_error")) ?: throw ApiFailure("server_error")
    }

    suspend fun join(code: String): ChatInfo {
        val row = rpc("join_chat", JSONObject().put("p_code", code.trim().uppercase())).json()
        refreshChats()
        return chatOf(row ?: throw ApiFailure("server_error")) ?: throw ApiFailure("server_error")
    }

    suspend fun startDirect(peerId: String, peerName: String): ChatInfo {
        val row = rpc("start_dm", JSONObject().put("p_peer", peerId).put("p_peer_name", peerName)).json()
        refreshChats()
        return chatOf(row ?: throw ApiFailure("server_error")) ?: throw ApiFailure("server_error")
    }

    suspend fun accept(chatId: String) {
        rpc("accept_chat", JSONObject().put("p_chat", chatId))
        refreshChats()
    }

    suspend fun leave(chatId: String) {
        rpc("leave_chat", JSONObject().put("p_chat", chatId))
        prefs?.edit()?.remove(chatKey(chatId))?.apply()
        refreshChats()
    }

    suspend fun regenerateInvite(chatId: String): String {
        val code = rpc("regenerate_invite", JSONObject().put("p_chat", chatId)).body.trim().trim('"')
        refreshChats()
        return code
    }

    suspend fun markRead(chatId: String, id: Long) {
        runCatching { rpc("mark_read", JSONObject().put("p_chat", chatId).put("p_id", id)) }
        val next = cachedChats().map { if (it.id == chatId) it.copy(unread = 0) else it }
        writeArray("chats", JSONArray().apply { next.forEach { put(chatJson(it)) } })
        emit()
    }

    suspend fun pin(chatId: String, messageId: Long?) {
        rpc("pin_message", JSONObject().put("p_chat", chatId).put("p_message", messageId ?: JSONObject.NULL))
        refreshChats()
    }

    private val reps = java.util.concurrent.ConcurrentHashMap<String, Set<String>>()

    /** User ids of the confirmed class reps of a group (cached until the app restarts). */
    suspend fun classReps(groupId: String): Set<String> {
        reps[groupId]?.let { return it }
        val query = java.net.URLEncoder.encode(groupId, "UTF-8")
        val ids = rest("GET", "class_reps?group_id=eq.$query&select=user_id").array().mapObjects { it.optString("user_id") }.toSet()
        reps[groupId] = ids
        return ids
    }

    suspend fun members(chatId: String, groupId: String? = null): List<ChatMember> {
        val repIds = groupId?.let { runCatching { classReps(it) }.getOrNull() }.orEmpty()
        return rest("GET", "chat_members?chat_id=eq.$chatId&accepted=eq.true&select=user_id,name,role&order=joined_at.asc")
            .array().mapObjects { row ->
                val id = row.optString("user_id")
                ChatMember(id, row.optString("name"), roleOf(row.optString("role")), id in repIds)
            }
    }

    suspend fun setRole(chatId: String, userId: String, role: String) {
        rpc("set_member_role", JSONObject().put("p_chat", chatId).put("p_user", userId).put("p_role", role))
    }

    fun cachedPolls(chatId: String): List<ChatPoll> = readArray("polls.$chatId").mapObjects { pollOf(it) }

    suspend fun refreshPolls(chatId: String): List<ChatPoll> {
        val array = rpc("chat_polls", JSONObject().put("p_chat", chatId)).array() ?: JSONArray()
        writeArray("polls.$chatId", array)
        emit()
        return array.mapObjects { pollOf(it) }
    }

    suspend fun createPoll(chatId: String, question: String, options: List<String>, multiple: Boolean) {
        rpc(
            "create_poll",
            JSONObject()
                .put("p_chat", chatId)
                .put("p_question", question)
                .put("p_options", JSONArray(options))
                .put("p_multiple", multiple),
        )
        refreshPolls(chatId)
    }

    suspend fun vote(chatId: String, pollId: Long, choices: List<Int>) {
        rpc("vote_poll", JSONObject().put("p_poll", pollId).put("p_choices", JSONArray(choices)))
        refreshPolls(chatId)
    }

    suspend fun closePoll(chatId: String, pollId: Long, delete: Boolean) {
        rpc("close_poll", JSONObject().put("p_poll", pollId).put("p_delete", delete))
        refreshPolls(chatId)
    }

    fun cachedGroupTasks(): List<GroupTask> = readArray("tasks").mapObjects { taskOf(it) }

    suspend fun refreshGroupTasks(): List<GroupTask> {
        val array = rest("GET", "chat_tasks?select=id,chat_id,title,note,due_date,due_time,author&order=due_date.asc.nullslast,id.desc&limit=200").array() ?: JSONArray()
        writeArray("tasks", array)
        emit()
        return array.mapObjects { taskOf(it) }
    }

    suspend fun addGroupTask(chatId: String, title: String, note: String, dueDate: String?, dueTime: String?) {
        rpc(
            "add_chat_task",
            JSONObject()
                .put("p_chat", chatId)
                .put("p_title", title)
                .put("p_note", note)
                .put("p_due_date", dueDate ?: JSONObject.NULL)
                .put("p_due_time", if (dueDate != null && dueTime != null) dueTime else JSONObject.NULL),
        )
        refreshGroupTasks()
    }

    suspend fun deleteGroupTask(taskId: Long) {
        rpc("delete_chat_task", JSONObject().put("p_task", taskId))
        refreshGroupTasks()
    }

    private fun roleOf(value: String) = when (value) {
        "owner" -> "owner"
        "helper" -> "helper"
        else -> "member"
    }

    private fun JSONArray?.ints(): List<Int> = this?.let { a -> (0 until a.length()).map { a.optInt(it) } } ?: emptyList()

    private fun pollOf(row: JSONObject): ChatPoll? {
        if (!row.has("id")) return null
        val options = row.optJSONArray("options")
        return ChatPoll(
            id = row.optLong("id"),
            question = row.optString("question"),
            options = options?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
            multiple = row.optBoolean("multiple"),
            closed = row.optBoolean("closed"),
            author = row.optString("author"),
            counts = row.optJSONArray("counts").ints(),
            mine = row.optJSONArray("mine").ints(),
            voters = row.optInt("voters"),
        )
    }

    private fun taskOf(row: JSONObject): GroupTask? {
        if (!row.has("id")) return null
        return GroupTask(
            id = row.optLong("id"),
            chatId = row.optString("chat_id"),
            title = row.optString("title"),
            note = row.optString("note"),
            dueDate = row.stringOrNull("due_date"),
            dueTime = row.stringOrNull("due_time"),
            author = row.optString("author"),
        )
    }

    suspend fun lookupUser(query: String): Pair<String, String> {
        val root = Account.api("GET", "/api/users/lookup?q=${java.net.URLEncoder.encode(query.trim(), "UTF-8")}")
        return root.optString("id") to root.optString("name")
    }

    private fun chatOf(row: JSONObject): ChatInfo? {
        val id = row.optString("id")
        if (id.isBlank()) return null
        return ChatInfo(
            id = id,
            kind = if (row.optString("kind") == "dm") "dm" else "group",
            title = row.optString("title"),
            groupId = row.stringOrNull("group_id"),
            inviteCode = row.stringOrNull("invite_code"),
            role = roleOf(row.optString("role")),
            accepted = row.optBoolean("accepted", true),
            peerName = row.stringOrNull("peer_name"),
            lastId = if (row.isNull("last_id")) null else row.optLong("last_id"),
            lastBody = row.stringOrNull("last_body"),
            lastAuthor = row.stringOrNull("last_author"),
            lastAt = row.stringOrNull("last_at"),
            unread = row.optInt("unread"),
            pinnedBody = row.stringOrNull("pinned_body"),
            pinnedAuthor = row.stringOrNull("pinned_author"),
            rep = row.optBoolean("rep"),
        )
    }

    private fun chatJson(chat: ChatInfo): JSONObject = JSONObject()
        .put("id", chat.id)
        .put("kind", chat.kind)
        .put("title", chat.title)
        .put("group_id", chat.groupId ?: JSONObject.NULL)
        .put("invite_code", chat.inviteCode ?: JSONObject.NULL)
        .put("role", chat.role)
        .put("accepted", chat.accepted)
        .put("peer_name", chat.peerName ?: JSONObject.NULL)
        .put("last_id", chat.lastId ?: JSONObject.NULL)
        .put("last_body", chat.lastBody ?: JSONObject.NULL)
        .put("last_author", chat.lastAuthor ?: JSONObject.NULL)
        .put("last_at", chat.lastAt ?: JSONObject.NULL)
        .put("unread", chat.unread)
        .put("pinned_body", chat.pinnedBody ?: JSONObject.NULL)
        .put("pinned_author", chat.pinnedAuthor ?: JSONObject.NULL)
        .put("rep", chat.rep)

    private fun messageOf(row: JSONObject, status: String): ChatMessage? {
        val chatId = row.optString("chat_id")
        if (chatId.isBlank()) return null
        return ChatMessage(
            id = if (row.isNull("id") || !row.has("id")) null else row.optLong("id"),
            chatId = chatId,
            userId = row.optString("user_id"),
            author = row.optString("author"),
            body = row.optString("body"),
            createdAt = row.optString("created_at"),
            clientId = row.optString("client_id"),
            status = status,
        )
    }

    private fun messageJson(message: ChatMessage): JSONObject = JSONObject()
        .put("id", message.id ?: JSONObject.NULL)
        .put("chat_id", message.chatId)
        .put("user_id", message.userId)
        .put("author", message.author)
        .put("body", message.body)
        .put("created_at", message.createdAt)
        .put("client_id", message.clientId)
        .put("status", message.status)
}
