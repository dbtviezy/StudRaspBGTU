package ru.bgtu.schedule.data

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.Instant

/**
 * Account sync of user data. The wire format (sections, keys, values) must match web/src/lib/sync.ts:
 * fav: name → true · profile: group | friend | name → string · notes: "date|lessonKey" → text ·
 * subjectNotes: subject → text · todos: id → todo · absences: "date|lessonKey" → subject · limits: subject → number ·
 * hidden: lessonKey → true · dayChanges: "date|lessonKey" → { cancelled, room } · settings: key → value.
 * A null value deletes the item. The newest change of an item wins on the server.
 */
object Sync {
    enum class Choice { MERGE, ACCOUNT, DEVICE }

    data class Status(val busy: Boolean = false, val asking: Boolean = false, val at: String? = null, val failed: Boolean = false)

    private const val FILE = "bgtu_sync"
    private const val SEP = "\u0001"
    private val DATA_SECTIONS = setOf("fav", "notes", "subjectNotes", "todos", "absences", "limits", "hidden", "dayChanges")

    /** Settings this platform has; others (web-only) are kept on the server untouched. */
    private val OWN_SETTINGS = setOf(
        "dark", "systemTheme", "lang", "hideProfanity",
        "notify", "notifyLead", "ongoing",
        "notifyProfile", "digest", "digestHour",
    )

    private val statusFlow = MutableStateFlow(Status())
    val status: StateFlow<Status> = statusFlow.asStateFlow()

    /** Bumped after server data was written into the local stores, so the screens re-read them. */
    private val appliedFlow = MutableStateFlow(0)
    val applied: StateFlow<Int> = appliedFlow.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private var ask: CompletableDeferred<Choice>? = null

    private class Item(val s: String, val k: String, val v: String?, val t: Long)

    private class Saved(val userId: String, val cursor: Long, val snapshot: Map<String, String>, val at: String?)

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun loadSaved(context: Context, userId: String): Saved? {
        val raw = prefs(context).getString("state", null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            if (root.optString("userId") != userId) return null
            val snap = root.optJSONObject("snapshot") ?: JSONObject()
            val map = buildMap { snap.keys().forEach { put(it, snap.optString(it)) } }
            Saved(userId, root.optLong("cursor"), map, root.stringOrNull("at"))
        }.getOrNull()
    }

    private fun save(context: Context, saved: Saved) {
        val snap = JSONObject()
        saved.snapshot.forEach { (key, value) -> snap.put(key, value) }
        val root = JSONObject()
            .put("userId", saved.userId)
            .put("cursor", saved.cursor)
            .put("snapshot", snap)
            .put("at", saved.at ?: JSONObject.NULL)
        prefs(context).edit().putString("state", root.toString()).apply()
    }

    /** Forgets what was synced, so the next sign-in starts with the merge question. */
    fun forget(context: Context) {
        prefs(context).edit().remove("state").apply()
        statusFlow.value = statusFlow.value.copy(at = null, failed = false)
    }

    fun lastAt(context: Context): String? {
        val user = Account.current.user ?: return null
        return statusFlow.value.at ?: loadSaved(context, user.id)?.at
    }

    fun answer(choice: Choice) {
        ask?.complete(choice)
        ask = null
    }

    /** Claims this device's old reviews once per account. */
    suspend fun claimOnce(context: Context) {
        val user = Account.current.user ?: return
        val sp = prefs(context)
        val key = "claimed.${user.id}"
        if (sp.getBoolean(key, false)) return
        runCatching { RatingApi.claim(context) }.onSuccess { sp.edit().putBoolean(key, true).apply() }
    }

    fun syncSoon(context: Context) {
        val app = context.applicationContext
        if (!Account.current.signedIn) return
        scope.launch { run(app) }
    }

    /** Syncs now unless a sync is already running. Errors only set the status. */
    suspend fun run(context: Context) {
        val user = Account.current.user ?: return
        if (!Account.current.signedIn) return
        if (!lock.tryLock()) return
        statusFlow.value = statusFlow.value.copy(busy = true)
        try {
            val saved = loadSaved(context, user.id)
            if (saved != null) regular(context, saved) else first(context, user.id)
            statusFlow.value = statusFlow.value.copy(failed = false)
        } catch (_: ApiFailure) {
            statusFlow.value = statusFlow.value.copy(failed = true)
        } catch (_: org.json.JSONException) {
            statusFlow.value = statusFlow.value.copy(failed = true)
        } finally {
            statusFlow.value = statusFlow.value.copy(busy = false, asking = false)
            lock.unlock()
        }
    }

    private fun keyOf(s: String, k: String) = "$s$SEP$k"

    private fun split(key: String): Pair<String, String> {
        val at = key.indexOf(SEP)
        return key.substring(0, at) to key.substring(at + 1)
    }

    private fun ownsKey(key: String): Boolean {
        val (s, k) = split(key)
        return s != "settings" || k in OWN_SETTINGS
    }

    /** JSON with sorted keys and whole numbers without a fraction, so equal values compare equal. */
    private fun canon(value: Any?): String = when (value) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> value.keys().asSequence().sorted().joinToString(",", "{", "}") { JSONObject.quote(it) + ":" + canon(value.opt(it)) }
        is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") { canon(value.opt(it)) }
        is String -> JSONObject.quote(value)
        is Boolean -> value.toString()
        is Double -> if (value % 1.0 == 0.0 && !value.isInfinite()) value.toLong().toString() else value.toString()
        is Float -> canon(value.toDouble())
        is Number -> value.toLong().toString()
        else -> JSONObject.quote(value.toString())
    }

    private fun parse(text: String): Any? = JSONTokener(text).nextValue().takeUnless { it == JSONObject.NULL }

    private fun collect(context: Context): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        fun put(s: String, k: String, v: Any) {
            if (k.isNotBlank()) out[keyOf(s, k)] = canon(v)
        }
        val prefs = Prefs(context)
        FavoriteTeachers(context).keys().forEach { put("fav", it, true) }
        prefs.groupId()?.takeIf { it.isNotBlank() }?.let { put("profile", "group", it) }
        prefs.friendGroup()?.takeIf { it.isNotBlank() }?.let { put("profile", "friend", it) }
        prefs.name().takeIf { it.isNotBlank() }?.let { put("profile", "name", it) }
        prefs.exportNotes().forEach { (k, v) -> if (v.isNotBlank()) put("notes", k, v) }
        SubjectNotes(context).all().forEach { (k, v) -> if (v.isNotBlank()) put("subjectNotes", k, v) }
        TodoStore(context).all().forEach { item ->
            put(
                "todos",
                item.id,
                JSONObject()
                    .put("title", item.title)
                    .put("deadlineDate", item.deadlineDate ?: JSONObject.NULL)
                    .put("deadlineTime", item.deadlineTime ?: JSONObject.NULL)
                    .put("lessonKey", item.lessonKey ?: JSONObject.NULL)
                    .put("lessonLabel", item.lessonLabel ?: JSONObject.NULL)
                    .put("done", item.done)
                    .put("remind", item.remind),
            )
        }
        AbsenceStore(context).all().forEach { put("absences", "${it.date}|${it.lessonKey}", it.subject) }
        AbsenceLimits(context).exportItems().forEach { (k, v) -> put("limits", k, v) }
        HiddenStore(context).keys().forEach { put("hidden", it, true) }
        DayOverrides(context).exportItems().forEach {
            put("dayChanges", "${it.date}|${it.lessonKey}", JSONObject().put("cancelled", it.cancelled).put("room", it.room ?: ""))
        }
        val system = prefs.followSystemTheme()
        if (!system) put("settings", "dark", prefs.darkTheme())
        put("settings", "systemTheme", system)
        prefs.language()?.let { put("settings", "lang", it) }
        put("settings", "hideProfanity", prefs.hideProfanity())
        put("settings", "notify", prefs.notifyEnabled())
        put("settings", "notifyLead", prefs.notifyLead())
        put("settings", "ongoing", prefs.ongoingLesson())
        put("settings", "notifyProfile", prefs.notifyProfile())
        put("settings", "digest", prefs.digestEnabled())
        put("settings", "digestHour", prefs.digestHour())
        return out
    }

    private fun splitDateKey(k: String): Pair<String, String> {
        val at = k.indexOf('|')
        return if (at < 0) k to "" else k.substring(0, at) to k.substring(at + 1)
    }

    private fun JSONObject.text(key: String): String? = if (isNull(key)) null else optString(key).ifBlank { null }

    /** Writes server items into the local stores. [replace] first clears the synced data sections. */
    private fun apply(context: Context, items: List<Item>, replace: Boolean = false) {
        if (items.isEmpty() && !replace) return
        val prefs = Prefs(context)
        val favStore = FavoriteTeachers(context)
        val subjectStore = SubjectNotes(context)
        val todoStore = TodoStore(context)
        val absenceStore = AbsenceStore(context)
        val limitStore = AbsenceLimits(context)
        val hiddenStore = HiddenStore(context)
        val dayStore = DayOverrides(context)

        val fav = if (replace) mutableSetOf() else favStore.keys().toMutableSet()
        val notes = if (replace) mutableMapOf() else prefs.exportNotes().toMutableMap()
        val subjects = if (replace) mutableMapOf() else subjectStore.all().toMutableMap()
        val todos = LinkedHashMap((if (replace) emptyList() else todoStore.all()).associateBy { it.id })
        val absences = LinkedHashMap((if (replace) emptyList() else absenceStore.all()).associateBy { "${it.date}|${it.lessonKey}" })
        val limits = if (replace) mutableMapOf() else limitStore.exportItems().toMutableMap()
        val hidden = if (replace) mutableSetOf() else hiddenStore.keys().toMutableSet()
        val changes = LinkedHashMap((if (replace) emptyList() else dayStore.exportItems()).associateBy { "${it.date}|${it.lessonKey}" })

        for (item in items) {
            val value = item.v?.let { parse(it) }
            val gone = value == null
            val k = item.k
            when (item.s) {
                "fav" -> if (gone) fav -= k else fav += k
                "profile" -> when (k) {
                    "group" -> if (!gone) prefs.setGroup(value.toString())
                    "friend" -> prefs.setFriendGroup(if (gone) null else value.toString())
                    "name" -> prefs.setName(if (gone) "" else value.toString())
                }
                "notes" -> if (gone) notes -= k else notes[k] = value.toString()
                "subjectNotes" -> if (gone) subjects -= k else subjects[k] = value.toString()
                "todos" -> {
                    val todo = value as? JSONObject
                    if (todo == null) {
                        todos -= k
                    } else {
                        todos[k] = TodoItem(
                            id = k,
                            title = todo.optString("title"),
                            deadlineDate = todo.text("deadlineDate"),
                            deadlineTime = todo.text("deadlineTime"),
                            lessonKey = todo.text("lessonKey"),
                            lessonLabel = todo.text("lessonLabel"),
                            done = todo.optBoolean("done"),
                            remind = todo.text("remind").orEmpty(),
                        )
                    }
                }
                "absences" -> if (gone) {
                    absences -= k
                } else {
                    val (date, key) = splitDateKey(k)
                    absences[k] = Absence(date, key, value.toString())
                }
                "limits" -> if (gone) limits -= k else (value as? Number)?.let { limits[k] = it.toInt() }
                "hidden" -> if (gone) hidden -= k else hidden += k
                "dayChanges" -> {
                    val change = value as? JSONObject
                    if (change == null) {
                        changes -= k
                    } else {
                        val (date, key) = splitDateKey(k)
                        changes[k] = DayChange(date, key, change.optBoolean("cancelled"), change.text("room"))
                    }
                }
                "settings" -> if (!gone) when (k) {
                    "dark" -> (value as? Boolean)?.let { prefs.setDarkTheme(it) }
                    "systemTheme" -> (value as? Boolean)?.let { prefs.setFollowSystemTheme(it) }
                    "lang" -> if (value == "ru" || value == "en") prefs.setLanguage(value as String)
                    "hideProfanity" -> (value as? Boolean)?.let { prefs.setHideProfanity(it) }
                    "notify" -> (value as? Boolean)?.let { prefs.setNotifyEnabled(it) }
                    "notifyLead" -> (value as? Number)?.let { prefs.setNotifyLead(it.toInt()) }
                    "ongoing" -> (value as? Boolean)?.let { prefs.setOngoingLesson(it) }
                    "notifyProfile" -> prefs.setNotifyProfile(value.toString())
                    "digest" -> (value as? Boolean)?.let { prefs.setDigestEnabled(it) }
                    "digestHour" -> (value as? Number)?.let { prefs.setDigestHour(it.toInt()) }
                }
            }
        }
        favStore.replaceAll(fav)
        prefs.replaceNotes(notes)
        subjectStore.replaceAll(subjects)
        todoStore.replaceAll(todos.values.toList())
        absenceStore.replaceAll(absences.values.toList())
        limitStore.replaceAll(limits)
        hiddenStore.replaceAll(hidden)
        dayStore.replaceAll(changes.values.toList())
        appliedFlow.value = appliedFlow.value + 1
    }

    private class Reply(val items: List<Item>, val cursor: Long)

    private suspend fun post(since: Long, changes: List<Item>): Reply {
        val array = JSONArray()
        changes.forEach { item ->
            array.put(
                JSONObject()
                    .put("s", item.s)
                    .put("k", item.k)
                    .put("v", item.v?.let { parse(it) } ?: JSONObject.NULL)
                    .put("t", item.t),
            )
        }
        val root = Account.api("POST", "/api/sync", JSONObject().put("since", since).put("changes", array))
        val items = root.optJSONArray("items").mapObjects { row ->
            val s = row.optString("s")
            val k = row.optString("k")
            if (s.isBlank() || k.isBlank()) return@mapObjects null
            val v = if (row.isNull("v")) null else canon(row.opt("v"))
            Item(s, k, v, row.optLong("t"))
        }
        return Reply(items, root.optLong("cursor", since))
    }

    private fun snapshotOf(base: Map<String, String>, items: List<Item>): Map<String, String> {
        val next = base.toMutableMap()
        items.forEach { item ->
            if (item.v == null) next -= keyOf(item.s, item.k) else next[keyOf(item.s, item.k)] = item.v
        }
        return next
    }

    private fun hasData(keys: Collection<String>) = keys.any { split(it).first in DATA_SECTIONS }

    private suspend fun first(context: Context, userId: String) {
        val firstReply = post(0, emptyList())
        val server = firstReply.items.filter { it.v != null }
        val serverKeys = server.associate { keyOf(it.s, it.k) to it.v!! }
        val local = collect(context)
        val differs = local.any { (key, value) -> split(key).first in DATA_SECTIONS && serverKeys[key] != value } ||
            serverKeys.any { (key, value) -> split(key).first in DATA_SECTIONS && local[key] != value }
        var choice = Choice.MERGE
        if (hasData(local.keys) && hasData(serverKeys.keys) && differs) {
            val waiting = CompletableDeferred<Choice>()
            ask = waiting
            statusFlow.value = statusFlow.value.copy(asking = true)
            choice = waiting.await()
            statusFlow.value = statusFlow.value.copy(asking = false)
        }
        val now = System.currentTimeMillis()
        val changes = mutableListOf<Item>()
        if (choice == Choice.ACCOUNT) {
            apply(context, server, replace = true)
        } else {
            local.forEach { (key, value) ->
                val (s, k) = split(key)
                changes += Item(s, k, value, now)
            }
            if (choice == Choice.DEVICE) {
                serverKeys.keys.filter { it !in local && ownsKey(it) }.forEach { key ->
                    val (s, k) = split(key)
                    changes += Item(s, k, null, now)
                }
            }
        }
        val reply = post(firstReply.cursor, changes)
        val fromServer = if (choice == Choice.MERGE) {
            server.filter { keyOf(it.s, it.k) !in local } + reply.items
        } else {
            reply.items
        }
        apply(context, fromServer)
        val at = Instant.now().toString()
        save(context, Saved(userId, reply.cursor, snapshotOf(serverKeys, changes + reply.items), at))
        statusFlow.value = statusFlow.value.copy(at = at)
    }

    private suspend fun regular(context: Context, saved: Saved) {
        val current = collect(context)
        val now = System.currentTimeMillis()
        val changes = mutableListOf<Item>()
        current.forEach { (key, value) ->
            if (saved.snapshot[key] != value) {
                val (s, k) = split(key)
                changes += Item(s, k, value, now)
            }
        }
        saved.snapshot.keys.filter { it !in current && ownsKey(it) }.forEach { key ->
            val (s, k) = split(key)
            changes += Item(s, k, null, now)
        }
        val reply = post(saved.cursor, changes)
        val mine = changes.associateBy { keyOf(it.s, it.k) }
        apply(context, reply.items.filter { item -> mine[keyOf(item.s, item.k)]?.v != item.v || keyOf(item.s, item.k) !in mine })
        val at = Instant.now().toString()
        save(context, Saved(saved.userId, reply.cursor, snapshotOf(saved.snapshot, changes + reply.items), at))
        statusFlow.value = statusFlow.value.copy(at = at)
    }
}
