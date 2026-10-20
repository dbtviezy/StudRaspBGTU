package ru.bgtu.schedule.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class Holiday(val nameRu: String, val nameEn: String)

object Holidays {
    private val days: Map<LocalDate, Holiday> = buildMap {
        put(LocalDate.of(2026, 11, 4), Holiday("День народного единства", "Unity Day"))
        var day = LocalDate.of(2026, 12, 29)
        val end = LocalDate.of(2027, 1, 11)
        while (!day.isAfter(end)) {
            put(day, Holiday("Каникулы", "Holidays"))
            day = day.plusDays(1)
        }
    }

    fun on(date: LocalDate): Holiday? = days[date]
}

class HiddenStore(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun keys(): Set<String> = sp.getStringSet(KEY, emptySet()).orEmpty()

    fun has(key: String): Boolean = key in keys()

    fun set(key: String, hidden: Boolean) {
        val next = keys().toMutableSet()
        if (hidden) next += key else next -= key
        sp.edit().putStringSet(KEY, next).apply()
    }

    companion object {
        private const val FILE = "bgtu_hidden"
        private const val KEY = "keys"
    }
}

class SubjectNotes(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun get(subject: String): String = sp.getString(subject, "").orEmpty()

    fun set(subject: String, value: String) {
        val edit = sp.edit()
        if (value.isBlank()) edit.remove(subject) else edit.putString(subject, value.trim())
        edit.apply()
    }

    fun all(): Map<String, String> {
        return sp.all.mapNotNull { (key, value) ->
            if (value is String && value.isNotBlank()) key to value else null
        }.toMap()
    }

    fun replaceAll(notes: Map<String, String>) {
        val edit = sp.edit().clear()
        notes.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) edit.putString(key, value)
        }
        edit.apply()
    }

    companion object {
        private const val FILE = "bgtu_subject_notes"
    }
}

data class Absence(val date: String, val lessonKey: String, val subject: String)

class AbsenceStore(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun all(): List<Absence> {
        val raw = sp.getString(KEY, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val key = item.optString("key")
                val date = item.optString("date")
                if (key.isBlank() || date.isBlank()) continue
                add(Absence(date, key, item.optString("subject")))
            }
        }
    }

    fun has(date: String, lessonKey: String): Boolean = all().any { it.date == date && it.lessonKey == lessonKey }

    fun marks(): Set<String> = all().map { "${it.date}|${it.lessonKey}" }.toSet()

    fun toggle(date: String, lessonKey: String, subject: String) {
        val next = all().filterNot { it.date == date && it.lessonKey == lessonKey }.toMutableList()
        if (!has(date, lessonKey)) next += Absence(date, lessonKey, subject)
        write(next)
    }

    fun replaceAll(items: List<Absence>) {
        write(items)
    }

    private fun write(items: List<Absence>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("date", item.date)
                    .put("key", item.lessonKey)
                    .put("subject", item.subject),
            )
        }
        sp.edit().putString(KEY, array.toString()).apply()
    }

    companion object {
        private const val FILE = "bgtu_absences"
        private const val KEY = "items"
    }
}

private val TEACHER_NAME = Regex("""([А-ЯЁA-Z][а-яёa-z\-]+)((?:\s+[А-ЯЁA-Z]?\.){1,2})""")

// Ratings are keyed by these names; keep identical to splitTeachers() in web/src/lib/schedule.ts.
fun splitTeachers(raw: String): List<String> {
    val found = TEACHER_NAME.findAll(raw).map { match ->
        val initials = match.groupValues[2].split('.').map { it.trim() }.filter { it.isNotEmpty() }
        (listOf(match.groupValues[1]) + initials.map { "$it." }).joinToString(" ")
    }.distinct().toList()
    return found.ifEmpty { listOfNotNull(raw.trim().ifBlank { null }) }
}

data class TeacherHit(
    val teacher: String,
    val groupId: String,
    val facultyShort: String,
    val lesson: Lesson,
)

fun searchTeachers(bundle: ScheduleBundle, query: String): List<TeacherHit> {
    val text = query.trim()
    if (text.length < 2) return emptyList()
    return bundle.groups.flatMap { (faculty, group) ->
        group.lessons.filter { it.teacher.contains(text, true) }.map { lesson ->
            TeacherHit(lesson.teacher, group.id, faculty.short, lesson)
        }
    }.distinctBy { "${it.teacher}|${it.groupId}|${it.lesson.key}" }
        .sortedWith(compareBy({ it.teacher }, { it.lesson.day }, { it.lesson.start }, { it.groupId }))
        .take(60)
}

class FavoriteTeachers(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun keys(): Set<String> = sp.getStringSet(KEY, emptySet()).orEmpty().toSet()

    fun has(name: String): Boolean = name.trim() in keys()

    fun set(name: String, favorite: Boolean) {
        val key = name.trim()
        if (key.isBlank()) return
        val next = keys().toMutableSet()
        if (favorite) next += key else next -= key
        sp.edit().putStringSet(KEY, next).apply()
    }

    fun replaceAll(names: Collection<String>) {
        val next = names.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        sp.edit().putStringSet(KEY, next).apply()
    }

    companion object {
        private const val FILE = "bgtu_favorite_teachers"
        private const val KEY = "names"
    }
}

data class TeacherNext(val day: LocalDate, val groupId: String, val facultyShort: String, val lesson: Lesson)

fun nextTeacherLessons(
    bundle: ScheduleBundle,
    teacher: String,
    today: LocalDate,
    now: LocalTime,
    semesterStart: LocalDate,
    limit: Int = 3,
): List<TeacherNext> {
    val name = teacher.trim()
    if (name.isBlank()) return emptyList()
    val hits = bundle.groups.flatMap { (faculty, group) ->
        group.lessons.filter { it.teacher.trim().equals(name, true) }.map { Triple(faculty.short, group, it) }
    }
    if (hits.isEmpty()) return emptyList()
    val result = mutableListOf<TeacherNext>()
    for (offset in 0..13) {
        val day = today.plusDays(offset.toLong())
        if (day.dayOfWeek.value == 7 || Holidays.on(day) != null) continue
        val kind = activeKind(day, semesterStart, "auto")
        hits.filter { (_, _, lesson) -> lesson.day == day.dayOfWeek.value && lessonVisible(lesson, kind) }
            .sortedBy { it.third.start.ifBlank { "99:99" } }
            .forEach { (faculty, group, lesson) ->
                if (offset == 0) {
                    val end = parseTime(lesson.end) ?: parseTime(lesson.start)
                    if (end != null && !end.isAfter(now)) return@forEach
                }
                result += TeacherNext(day, group.id, faculty, lesson)
                if (result.size >= limit) return result
            }
    }
    return result
}

sealed class LiveStatus {
    data class Off(val holiday: Holiday) : LiveStatus()
    data class InClass(val lesson: Lesson, val minutesLeft: Int) : LiveStatus()
    data class Soon(val lesson: Lesson, val minutesUntil: Int) : LiveStatus()
    data class Window(val minutes: Int, val next: Lesson) : LiveStatus()
    data class Done(val next: Pair<LocalDate, Lesson>?) : LiveStatus()
    data object None : LiveStatus()
}

fun liveStatus(
    group: Group,
    today: LocalDate,
    now: LocalTime,
    semesterStart: LocalDate,
    hidden: Set<String>,
    overrides: DayOverrides? = null,
): LiveStatus {
    Holidays.on(today)?.let { return LiveStatus.Off(it) }
    val lessons = shownLessons(group, today.dayOfWeek.value, today, semesterStart, "auto", hidden, overrides)
    val current = lessons.firstOrNull { lesson ->
        val start = parseTime(lesson.start) ?: return@firstOrNull false
        val end = parseTime(lesson.end) ?: return@firstOrNull false
        !now.isBefore(start) && now.isBefore(end)
    }
    if (current != null) {
        val end = parseTime(current.end) ?: return LiveStatus.InClass(current, 0)
        val left = java.time.temporal.ChronoUnit.MINUTES.between(now, end).toInt().coerceAtLeast(1)
        return LiveStatus.InClass(current, left)
    }
    val next = lessons.firstOrNull { lesson ->
        val start = parseTime(lesson.start) ?: return@firstOrNull false
        start.isAfter(now)
    }
    if (next != null) {
        val start = parseTime(next.start) ?: return LiveStatus.Soon(next, 0)
        val until = java.time.temporal.ChronoUnit.MINUTES.between(now, start).toInt().coerceAtLeast(0)
        val previous = lessons.lastOrNull { lesson ->
            val end = parseTime(lesson.end) ?: return@lastOrNull false
            !end.isAfter(now)
        }
        val previousEnd = previous?.end?.let { parseTime(it) }
        if (previousEnd != null) {
            val gap = java.time.temporal.ChronoUnit.MINUTES.between(previousEnd, start).toInt()
            if (gap >= 40) return LiveStatus.Window(gap, next)
        }
        return LiveStatus.Soon(next, until)
    }
    if (lessons.isEmpty()) {
        val tomorrow = today.plusDays(1)
        val upcoming = if (Holidays.on(tomorrow) != null) {
            null
        } else {
            shownLessons(group, tomorrow.dayOfWeek.value, tomorrow, semesterStart, "auto", hidden, overrides).firstOrNull()
                ?.let { tomorrow to it }
        }
        return if (upcoming == null) LiveStatus.None else LiveStatus.Done(upcoming)
    }
    val tomorrow = today.plusDays(1)
    val upcoming = if (Holidays.on(tomorrow) != null) {
        null
    } else {
        shownLessons(group, tomorrow.dayOfWeek.value, tomorrow, semesterStart, "auto", hidden, overrides).firstOrNull()
            ?.let { tomorrow to it }
    }
    return LiveStatus.Done(upcoming)
}

fun shownLessons(
    group: Group,
    day: Int,
    date: LocalDate,
    semesterStart: LocalDate,
    filter: String,
    hidden: Set<String>,
    overrides: DayOverrides? = null,
): List<Lesson> {
    if (Holidays.on(date) != null) return emptyList()
    return lessonsOn(group, day, date, semesterStart, filter)
        .filter { it.key !in hidden }
        .mapNotNull { overrides?.apply(date, it) ?: it }
}

class DayOverrides(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun find(date: String, lessonKey: String): DayChange? {
        return all().firstOrNull { it.date == date && it.lessonKey == lessonKey }
    }

    fun apply(date: LocalDate, lesson: Lesson): Lesson? {
        val change = find(date.toString(), lesson.key) ?: return lesson
        if (change.cancelled) return null
        val room = change.room?.takeIf { it.isNotBlank() } ?: return lesson
        return lesson.copy(room = room, baseKey = lesson.key)
    }

    fun cancelledOn(date: String): Set<String> =
        all().filter { it.date == date && it.cancelled }.map { it.lessonKey }.toSet()

    fun set(date: String, lessonKey: String, cancelled: Boolean, room: String?) {
        val next = all().filterNot { it.date == date && it.lessonKey == lessonKey } +
            DayChange(date, lessonKey, cancelled, room?.trim()?.ifBlank { null })
        write(next)
    }

    fun clear(date: String, lessonKey: String) {
        write(all().filterNot { it.date == date && it.lessonKey == lessonKey })
    }

    fun exportItems(): List<DayChange> = all()

    fun replaceAll(items: List<DayChange>) {
        write(items)
    }

    private fun all(): List<DayChange> {
        val raw = sp.getString(KEY, null) ?: return emptyList()
        val array = runCatching { org.json.JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val date = item.optString("date")
                val key = item.optString("key")
                if (date.isBlank() || key.isBlank()) continue
                add(DayChange(date, key, item.optBoolean("cancelled"), item.optString("room").ifBlank { null }))
            }
        }
    }

    private fun write(items: List<DayChange>) {
        val array = org.json.JSONArray()
        items.forEach { item ->
            array.put(
                org.json.JSONObject()
                    .put("date", item.date)
                    .put("key", item.lessonKey)
                    .put("cancelled", item.cancelled)
                    .put("room", item.room ?: ""),
            )
        }
        sp.edit().putString(KEY, array.toString()).apply()
    }

    companion object {
        private const val FILE = "bgtu_day_overrides"
        private const val KEY = "items"
    }
}

data class DayChange(val date: String, val lessonKey: String, val cancelled: Boolean, val room: String?)

class AbsenceLimits(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun get(subject: String): Int? = if (sp.contains(subject)) sp.getInt(subject, 0) else null

    fun set(subject: String, value: Int?) {
        val edit = sp.edit()
        if (value == null || value < 0) edit.remove(subject) else edit.putInt(subject, value)
        edit.apply()
    }

    fun exportItems(): Map<String, Int> {
        return sp.all.mapNotNull { (key, value) ->
            val number = value as? Int ?: return@mapNotNull null
            key to number
        }.toMap()
    }

    fun replaceAll(items: Map<String, Int>) {
        val edit = sp.edit().clear()
        items.forEach { (key, value) -> if (value >= 0) edit.putInt(key, value) }
        edit.apply()
    }

    companion object {
        private const val FILE = "bgtu_absence_limits"
    }
}

private val PAIR_STARTS = listOf("08:00", "09:45", "11:30", "13:20", "15:05", "16:50", "18:35", "20:00")

fun sharedFree(
    mine: Group,
    friend: Group,
    today: LocalDate,
    now: LocalTime,
    semesterStart: LocalDate,
    hidden: Set<String>,
    overrides: DayOverrides,
): List<Pair<LocalDate, String>> {
    val result = mutableListOf<Pair<LocalDate, String>>()
    for (offset in 0..2) {
        val day = today.plusDays(offset.toLong())
        if (day.dayOfWeek.value == 7 || Holidays.on(day) != null) continue
        val mineBusy = shownLessons(mine, day.dayOfWeek.value, day, semesterStart, "auto", hidden, overrides)
            .map { it.start }
            .toSet()
        val friendBusy = shownLessons(friend, day.dayOfWeek.value, day, semesterStart, "auto", emptySet(), null)
            .map { it.start }
            .toSet()
        PAIR_STARTS.forEach { start ->
            if (offset == 0) {
                val time = parseTime(start) ?: return@forEach
                if (!time.isAfter(now)) return@forEach
            }
            if (start !in mineBusy && start !in friendBusy) result += day to start
        }
        if (result.size >= 3) break
    }
    return result.take(3)
}

fun lessonsOfKind(group: Group, day: Int, kind: String, hidden: Set<String>): List<Lesson> {
    return group.lessons
        .filter { it.day == day && it.key !in hidden && (it.week == "all" || it.week == kind) }
        .sortedWith(compareBy({ it.start.ifBlank { "99:99" } }, { it.week }))
}

data class FreeGap(val minutes: Int, val startsAt: String)

sealed class DayRow {
    data class Class(val lesson: Lesson) : DayRow()
    data class Gap(val gap: FreeGap) : DayRow()
}

fun dayRows(lessons: List<Lesson>): List<DayRow> {
    if (lessons.isEmpty()) return emptyList()
    val rows = mutableListOf<DayRow>()
    lessons.forEachIndexed { index, lesson ->
        rows += DayRow.Class(lesson)
        val next = lessons.getOrNull(index + 1) ?: return@forEachIndexed
        val from = parseTime(lesson.end) ?: return@forEachIndexed
        val to = parseTime(next.start) ?: return@forEachIndexed
        val minutes = java.time.temporal.ChronoUnit.MINUTES.between(from, to).toInt()
        if (minutes >= 40) rows += DayRow.Gap(FreeGap(minutes, lesson.end))
    }
    return rows
}

fun LocalDateTime.toMillis(): Long = atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
