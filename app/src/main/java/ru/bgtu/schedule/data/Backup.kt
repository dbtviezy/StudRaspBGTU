package ru.bgtu.schedule.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object Backup {
    fun exportJson(context: Context): String {
        val prefs = Prefs(context)
        val root = JSONObject()
        root.put("version", 2)
        root.put("name", prefs.name())
        root.put("group", prefs.groupId().orEmpty())
        root.put("friend", prefs.friendGroup().orEmpty())
        val notes = JSONObject()
        prefs.exportNotes().forEach { (key, value) -> notes.put(key, value) }
        root.put("notes", notes)
        val subjects = JSONObject()
        SubjectNotes(context).all().forEach { (key, value) -> subjects.put(key, value) }
        root.put("subjectNotes", subjects)
        root.put("hidden", JSONArray(HiddenStore(context).keys().toList()))
        val absences = JSONArray()
        AbsenceStore(context).all().forEach { item ->
            absences.put(JSONObject().put("date", item.date).put("key", item.lessonKey).put("subject", item.subject))
        }
        root.put("absences", absences)
        val changes = JSONArray()
        DayOverrides(context).exportItems().forEach { item ->
            changes.put(
                JSONObject()
                    .put("date", item.date)
                    .put("key", item.lessonKey)
                    .put("cancelled", item.cancelled)
                    .put("room", item.room ?: ""),
            )
        }
        root.put("dayChanges", changes)
        val limits = JSONObject()
        AbsenceLimits(context).exportItems().forEach { (subject, value) -> limits.put(subject, value) }
        root.put("absenceLimits", limits)
        root.put("favoriteTeachers", JSONArray(FavoriteTeachers(context).keys().toList()))
        val todos = JSONArray()
        TodoStore(context).all().forEach { item ->
            todos.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("deadlineDate", item.deadlineDate ?: "")
                    .put("deadlineTime", item.deadlineTime ?: "")
                    .put("lessonKey", item.lessonKey ?: "")
                    .put("lessonLabel", item.lessonLabel ?: "")
                    .put("done", item.done)
                    .put("remind", item.remind),
            )
        }
        root.put("todos", todos)
        root.put("files", JSONArray(listLibrary(context).map { it.file.name }))
        root.put(
            "settings",
            JSONObject()
                .put("notifyProfile", prefs.notifyProfile())
                .put("notifyLead", prefs.notifyLead())
                .put("digest", prefs.digestEnabled())
                .put("digestHour", prefs.digestHour())
                .put("hideProfanity", prefs.hideProfanity())
                .put("systemTheme", prefs.followSystemTheme())
                .put("dark", prefs.darkTheme())
                .put("lang", prefs.language() ?: ""),
        )
        return root.toString(2)
    }

    fun importJson(context: Context, text: String): Pair<String, String> {
        val root = JSONObject(text)
        val prefs = Prefs(context)
        val name = root.optString("name")
        val group = root.optString("group")
        prefs.setName(name)
        if (group.isNotBlank()) prefs.setGroup(group)
        val friend = root.optString("friend")
        prefs.setFriendGroup(friend.ifBlank { null })
        val notesJson = root.optJSONObject("notes") ?: JSONObject()
        val notes = buildMap {
            val keys = notesJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, notesJson.optString(key))
            }
        }
        prefs.replaceNotes(notes)
        val subjectJson = root.optJSONObject("subjectNotes") ?: JSONObject()
        val subjects = buildMap {
            val keys = subjectJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, subjectJson.optString(key))
            }
        }
        SubjectNotes(context).replaceAll(subjects)
        val hidden = root.optJSONArray("hidden") ?: JSONArray()
        val hiddenStore = HiddenStore(context)
        hiddenStore.keys().forEach { hiddenStore.set(it, false) }
        for (index in 0 until hidden.length()) hiddenStore.set(hidden.optString(index), true)
        val absenceJson = root.optJSONArray("absences") ?: JSONArray()
        val absences = buildList {
            for (index in 0 until absenceJson.length()) {
                val item = absenceJson.optJSONObject(index) ?: continue
                val date = item.optString("date")
                val key = item.optString("key")
                if (date.isBlank() || key.isBlank()) continue
                add(Absence(date, key, item.optString("subject")))
            }
        }
        AbsenceStore(context).replaceAll(absences)
        val changeJson = root.optJSONArray("dayChanges") ?: JSONArray()
        val changes = buildList {
            for (index in 0 until changeJson.length()) {
                val item = changeJson.optJSONObject(index) ?: continue
                val date = item.optString("date")
                val key = item.optString("key")
                if (date.isBlank() || key.isBlank()) continue
                add(DayChange(date, key, item.optBoolean("cancelled"), item.optString("room").ifBlank { null }))
            }
        }
        DayOverrides(context).replaceAll(changes)
        val limitJson = root.optJSONObject("absenceLimits") ?: JSONObject()
        val limits = buildMap {
            val keys = limitJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, limitJson.optInt(key))
            }
        }
        AbsenceLimits(context).replaceAll(limits)
        if (root.has("favoriteTeachers")) {
            val favoriteJson = root.optJSONArray("favoriteTeachers") ?: JSONArray()
            val favorites = buildList {
                for (index in 0 until favoriteJson.length()) {
                    val name = favoriteJson.optString(index)
                    if (name.isNotBlank()) add(name)
                }
            }
            FavoriteTeachers(context).replaceAll(favorites)
        }
        val todoJson = root.optJSONArray("todos") ?: JSONArray()
        val store = TodoStore(context)
        store.all().forEach { store.remove(it.id) }
        for (index in 0 until todoJson.length()) {
            val item = todoJson.optJSONObject(index) ?: continue
            val title = item.optString("title")
            if (title.isBlank()) continue
            store.upsert(
                TodoItem(
                    id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                    title = title,
                    deadlineDate = item.optString("deadlineDate").ifBlank { null },
                    deadlineTime = item.optString("deadlineTime").ifBlank { null },
                    lessonKey = item.optString("lessonKey").ifBlank { null },
                    lessonLabel = item.optString("lessonLabel").ifBlank { null },
                    done = item.optBoolean("done"),
                    remind = item.optString("remind"),
                ),
            )
        }
        return name to group
    }
}
