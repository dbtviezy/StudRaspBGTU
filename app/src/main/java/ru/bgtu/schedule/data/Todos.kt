package ru.bgtu.schedule.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TodoItem(
    val id: String,
    val title: String,
    val deadlineDate: String?,
    val deadlineTime: String?,
    val lessonKey: String?,
    val lessonLabel: String?,
    val done: Boolean,
    val remind: String = "",
)

class TodoStore(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("bgtu_todos", Context.MODE_PRIVATE)

    fun all(): List<TodoItem> {
        val raw = sp.getString(KEY, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val title = item.optString("title")
                if (title.isBlank()) continue
                add(
                    TodoItem(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
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
        }
    }

    fun forLesson(key: String): List<TodoItem> = all().filter { it.lessonKey == key }

    fun hasOpen(key: String): Boolean = all().any { it.lessonKey == key && !it.done }

    fun upsert(item: TodoItem) {
        val next = all().filterNot { it.id == item.id } + item
        write(next)
    }

    fun remove(id: String) {
        write(all().filterNot { it.id == id })
    }

    private fun write(items: List<TodoItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
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
        sp.edit().putString(KEY, array.toString()).apply()
    }

    companion object {
        private const val KEY = "items"
    }
}

fun TodoItem.deadlineAt(): LocalDateTime? {
    val date = deadlineDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    val time = deadlineTime?.let { parseTime(it) } ?: return date.atTime(23, 59)
    return LocalDateTime.of(date, time)
}

fun TodoItem.isOverdue(now: LocalDateTime = LocalDateTime.now()): Boolean {
    if (done) return false
    val at = deadlineAt() ?: return false
    return at.isBefore(now)
}

fun sortedTodos(items: List<TodoItem>): List<TodoItem> {
    return items.sortedWith(
        compareBy<TodoItem> { it.done }
            .thenBy { it.deadlineDate == null }
            .thenBy { it.deadlineDate ?: "9999-99-99" }
            .thenBy { it.deadlineTime ?: "99:99" }
            .thenBy { it.title },
    )
}
