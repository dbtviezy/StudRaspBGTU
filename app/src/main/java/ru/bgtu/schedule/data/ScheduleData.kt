package ru.bgtu.schedule.data

import android.content.Context
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class Lesson(
    val day: Int,
    val dayName: String,
    val time: String,
    val start: String,
    val end: String,
    val subject: String,
    val type: String,
    val teacher: String,
    val room: String,
    val week: String,
    val baseKey: String? = null,
) {
    val key: String get() = baseKey ?: "$day|$time|$subject|$teacher|$room|$week"
}

data class Group(
    val id: String,
    val level: String,
    val year: Int?,
    val lessons: List<Lesson>,
)

data class Faculty(
    val name: String,
    val short: String,
    val groups: List<Group>,
)

data class ScheduleBundle(
    val source: String,
    val periodLabel: String,
    val fetchedAt: String,
    val semesterStart: LocalDate,
    val faculties: List<Faculty>,
) {
    val groups: List<Pair<Faculty, Group>> = faculties.flatMap { faculty ->
        faculty.groups.map { faculty to it }
    }

    fun find(id: String?): Pair<Faculty, Group>? {
        if (id.isNullOrBlank()) return null
        return groups.firstOrNull { it.second.id == id }
    }
}

data class WeekInfo(
    val number: Int,
    val even: Boolean,
    val kind: String,
    val title: String,
    val kindTitle: String,
)

object ScheduleRepository {
    fun load(context: Context): ScheduleBundle {
        val text = context.assets.open("schedule.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)
        val facultiesJson = root.getJSONArray("faculties")
        val faculties = buildList {
            for (i in 0 until facultiesJson.length()) {
                val faculty = facultiesJson.getJSONObject(i)
                val groupsJson = faculty.getJSONArray("groups")
                val groups = buildList {
                    for (j in 0 until groupsJson.length()) {
                        val group = groupsJson.getJSONObject(j)
                        val lessonsJson = group.getJSONArray("lessons")
                        val lessons = buildList {
                            for (k in 0 until lessonsJson.length()) {
                                val lesson = lessonsJson.getJSONObject(k)
                                add(
                                    Lesson(
                                        day = lesson.optInt("day"),
                                        dayName = lesson.optString("dayName"),
                                        time = lesson.optString("time"),
                                        start = lesson.optString("start"),
                                        end = lesson.optString("end"),
                                        subject = lesson.optString("subject"),
                                        type = lesson.optString("type"),
                                        teacher = lesson.optString("teacher"),
                                        room = lesson.optString("room"),
                                        week = lesson.optString("week", "all"),
                                    ),
                                )
                            }
                        }
                        val year = if (group.has("year") && !group.isNull("year")) group.getInt("year") else null
                        add(
                            Group(
                                id = group.getString("id"),
                                level = group.optString("level"),
                                year = year,
                                lessons = lessons,
                            ),
                        )
                    }
                }
                add(
                    Faculty(
                        name = faculty.getString("name"),
                        short = faculty.getString("short"),
                        groups = groups,
                    ),
                )
            }
        }
        val start = runCatching { LocalDate.parse(root.optString("semesterStart")) }
            .getOrDefault(LocalDate.of(2026, 9, 1))
        return ScheduleBundle(
            source = root.optString("source"),
            periodLabel = root.optString("periodLabel"),
            fetchedAt = root.optString("fetchedAt"),
            semesterStart = start,
            faculties = faculties,
        )
    }
}

class Prefs(context: Context) {
    private val app = context.applicationContext
    private val sp = context.getSharedPreferences("bgtu", Context.MODE_PRIVATE)

    fun groupId(): String? = sp.getString(KEY_GROUP, null)

    fun setGroup(id: String?) {
        sp.edit().putString(KEY_GROUP, id).apply()
    }

    fun name(): String = sp.getString(KEY_NAME, "").orEmpty()

    fun setName(value: String) {
        sp.edit().putString(KEY_NAME, value.trim()).apply()
    }

    /** The theme in effect: the system one when "follow system" is on, otherwise the saved choice. */
    fun darkTheme(): Boolean = if (followSystemTheme()) systemDark() else sp.getBoolean(KEY_DARK, false)

    fun setDarkTheme(value: Boolean) {
        sp.edit().putBoolean(KEY_DARK, value).apply()
    }

    fun systemDark(): Boolean =
        (app.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    fun followSystemTheme(): Boolean = sp.getBoolean(KEY_THEME_SYSTEM, false)

    fun setFollowSystemTheme(value: Boolean) {
        sp.edit().putBoolean(KEY_THEME_SYSTEM, value).apply()
    }

    fun hideProfanity(): Boolean = sp.getBoolean(KEY_PROFANITY, false)

    fun setHideProfanity(value: Boolean) {
        sp.edit().putBoolean(KEY_PROFANITY, value).apply()
    }

    fun chatNotify(): Boolean =
        NotifyProfile.allowsChats(notifyProfile()) && sp.getBoolean(KEY_CHAT_NOTIFY, false)

    fun setChatNotify(value: Boolean) {
        sp.edit().putBoolean(KEY_CHAT_NOTIFY, value && NotifyProfile.allowsChats(notifyProfile())).apply()
    }

    fun ongoingLesson(): Boolean =
        NotifyProfile.allowsOngoing(notifyProfile()) && sp.getBoolean(KEY_ONGOING, false)

    fun setOngoingLesson(value: Boolean) {
        sp.edit().putBoolean(KEY_ONGOING, value && NotifyProfile.allowsOngoing(notifyProfile())).apply()
    }

    /** normal | quiet | session | off. Migrates once from the old lesson/chat toggles. */
    fun notifyProfile(): String {
        migrateNotifyProfile()
        return NotifyProfile.clean(sp.getString(KEY_NOTIFY_PROFILE, NotifyProfile.NORMAL))
    }

    fun setNotifyProfile(value: String) {
        val profile = NotifyProfile.clean(value)
        val edit = sp.edit().putString(KEY_NOTIFY_PROFILE, profile)
        if (!NotifyProfile.allowsLessons(profile)) edit.putBoolean(KEY_NOTIFY, false)
        else if (!sp.getBoolean(KEY_NOTIFY, false)) edit.putBoolean(KEY_NOTIFY, true)
        if (!NotifyProfile.allowsChats(profile)) edit.putBoolean(KEY_CHAT_NOTIFY, false)
        if (!NotifyProfile.allowsOngoing(profile)) edit.putBoolean(KEY_ONGOING, false)
        if (!NotifyProfile.allowsDigest(profile)) edit.putBoolean(KEY_DIGEST, false)
        edit.apply()
    }

    fun digestEnabled(): Boolean =
        NotifyProfile.allowsDigest(notifyProfile()) && sp.getBoolean(KEY_DIGEST, false)

    fun setDigestEnabled(value: Boolean) {
        sp.edit().putBoolean(KEY_DIGEST, value && NotifyProfile.allowsDigest(notifyProfile())).apply()
    }

    /** Local hour 18..23 for the evening digest; default 21. */
    fun digestHour(): Int = sp.getInt(KEY_DIGEST_HOUR, 21).coerceIn(18, 23)

    fun setDigestHour(hour: Int) {
        sp.edit().putInt(KEY_DIGEST_HOUR, hour.coerceIn(18, 23)).apply()
    }

    private fun migrateNotifyProfile() {
        if (sp.contains(KEY_NOTIFY_PROFILE)) return
        val lessons = sp.getBoolean(KEY_NOTIFY, false)
        val chats = sp.getBoolean(KEY_CHAT_NOTIFY, false)
        val profile = when {
            !lessons && !chats -> NotifyProfile.OFF
            !lessons && chats -> NotifyProfile.QUIET
            else -> NotifyProfile.NORMAL
        }
        sp.edit().putString(KEY_NOTIFY_PROFILE, profile).apply()
    }

    fun seenVersion(): String? = sp.getString(KEY_SEEN_VERSION, null)

    fun setSeenVersion(value: String) {
        sp.edit().putString(KEY_SEEN_VERSION, value).apply()
    }

    /** The one-time «Войдите в аккаунт» offer was shown. */
    fun loginOffered(): Boolean = sp.getBoolean(KEY_LOGIN_OFFERED, false)

    fun setLoginOffered() {
        sp.edit().putBoolean(KEY_LOGIN_OFFERED, true).apply()
    }

    fun clearProfile() {
        sp.edit().remove(KEY_GROUP).remove(KEY_NAME).apply()
    }

    fun wipeUserData() {
        val keep = setOf(KEY_DARK, KEY_LANG, KEY_ONBOARDED, KEY_THEME_SYSTEM, KEY_SEEN_VERSION)
        val edit = sp.edit()
        sp.all.keys.filterNot { it in keep }.forEach { edit.remove(it) }
        edit.apply()
    }

    fun note(date: String, lessonKey: String): String = sp.getString(noteKey(date, lessonKey), "").orEmpty()

    fun setNote(date: String, lessonKey: String, value: String) {
        val key = noteKey(date, lessonKey)
        val edit = sp.edit()
        if (value.isBlank()) edit.remove(key) else edit.putString(key, value.trim())
        edit.apply()
    }

    fun exportNotes(): Map<String, String> {
        return sp.all.mapNotNull { (key, value) ->
            if (key.startsWith("note|") && value is String && value.isNotBlank()) {
                key.removePrefix("note|") to value
            } else {
                null
            }
        }.toMap()
    }

    fun replaceNotes(notes: Map<String, String>) {
        val edit = sp.edit()
        sp.all.keys.filter { it.startsWith("note|") }.forEach { edit.remove(it) }
        notes.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) edit.putString("note|$key", value)
        }
        edit.apply()
    }

    /** Lesson reminders are on only in the normal profile (and the stored toggle). */
    fun notifyEnabled(): Boolean =
        NotifyProfile.allowsLessons(notifyProfile()) && sp.getBoolean(KEY_NOTIFY, false)

    fun setNotifyEnabled(value: Boolean) {
        val edit = sp.edit().putBoolean(KEY_NOTIFY, value)
        if (value && notifyProfile() != NotifyProfile.NORMAL) {
            edit.putString(KEY_NOTIFY_PROFILE, NotifyProfile.NORMAL)
        }
        edit.apply()
    }

    fun notifyLead(): Int = sp.getInt(KEY_LEAD, 10)

    fun setNotifyLead(minutes: Int) {
        sp.edit().putInt(KEY_LEAD, minutes).apply()
    }

    fun widgetDate(): String? = sp.getString(KEY_WIDGET_DATE, null)

    fun widgetPickedOn(): String? = sp.getString(KEY_WIDGET_PICKED_ON, null)

    fun setWidgetDate(iso: String, pickedOn: String) {
        sp.edit().putString(KEY_WIDGET_DATE, iso).putString(KEY_WIDGET_PICKED_ON, pickedOn).apply()
    }

    fun language(): String? = if (sp.contains(KEY_LANG)) sp.getString(KEY_LANG, "ru") else null

    fun setLanguage(value: String) {
        sp.edit().putString(KEY_LANG, value).apply()
    }

    fun onboarded(): Boolean = sp.getBoolean(KEY_ONBOARDED, false)

    fun setOnboarded(value: Boolean) {
        sp.edit().putBoolean(KEY_ONBOARDED, value).apply()
    }

    fun friendGroup(): String? = sp.getString(KEY_FRIEND, null)

    fun setFriendGroup(id: String?) {
        val edit = sp.edit()
        if (id.isNullOrBlank()) edit.remove(KEY_FRIEND) else edit.putString(KEY_FRIEND, id)
        edit.apply()
    }

    companion object {
        private const val KEY_GROUP = "group"
        private const val KEY_NAME = "name"
        private const val KEY_DARK = "dark"
        private const val KEY_NOTIFY = "notify"
        private const val KEY_LEAD = "notify_lead"
        private const val KEY_NOTIFY_PROFILE = "notify_profile"
        private const val KEY_DIGEST = "digest"
        private const val KEY_DIGEST_HOUR = "digest_hour"
        private const val KEY_WIDGET_DATE = "widget_date"
        private const val KEY_WIDGET_PICKED_ON = "widget_picked_on"
        private const val KEY_LANG = "lang"
        private const val KEY_ONBOARDED = "onboarded"
        private const val KEY_FRIEND = "friend"
        private const val KEY_THEME_SYSTEM = "theme_system"
        private const val KEY_PROFANITY = "hide_profanity"
        private const val KEY_CHAT_NOTIFY = "chat_notify"
        private const val KEY_ONGOING = "ongoing_lesson"
        private const val KEY_SEEN_VERSION = "seen_version"
        private const val KEY_LOGIN_OFFERED = "login_offered"

        private fun noteKey(date: String, lessonKey: String) = "note|$date|$lessonKey"
    }
}

fun weekInfo(date: LocalDate, semesterStart: LocalDate): WeekInfo {
    val startMonday = semesterStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val thisMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val number = (ChronoUnit.WEEKS.between(startMonday, thisMonday) + 1).toInt()
    val even = number % 2 == 0
    return WeekInfo(
        number = number,
        even = even,
        kind = if (even) "den" else "num",
        title = if (even) "чётная неделя" else "нечётная неделя",
        kindTitle = if (even) "знаменатель" else "числитель",
    )
}

fun lessonVisible(lesson: Lesson, kind: String?): Boolean {
    if (kind == null) return true
    return lesson.week == "all" || lesson.week == kind
}

fun activeKind(date: LocalDate, semesterStart: LocalDate, filter: String): String? {
    return when (filter) {
        "all" -> null
        "num" -> "num"
        "den" -> "den"
        else -> weekInfo(date, semesterStart).kind
    }
}

fun lessonsOn(
    group: Group,
    day: Int,
    date: LocalDate,
    semesterStart: LocalDate,
    filter: String,
): List<Lesson> {
    val kind = activeKind(date, semesterStart, filter)
    return group.lessons
        .filter { it.day == day && lessonVisible(it, kind) }
        .sortedWith(compareBy({ it.start.ifBlank { "99:99" } }, { it.week }))
}

fun pairNumber(start: String): Int? = when (start) {
    "08:00" -> 1
    "09:45" -> 2
    "11:30" -> 3
    "13:20" -> 4
    "15:05" -> 5
    "16:50" -> 6
    "18:35" -> 7
    "20:00", "20:10" -> 8
    else -> null
}

fun parseTime(value: String): LocalTime? = runCatching { LocalTime.parse(value) }.getOrNull()

fun durationPhrase(start: String, end: String): String {
    val from = parseTime(start) ?: return ""
    val to = parseTime(end) ?: return ""
    val mins = ChronoUnit.MINUTES.between(from, to).toInt()
    if (mins <= 0) return ""
    val hours = mins / 60
    val rest = mins % 60
    val hourText = when (hours) {
        0 -> ""
        1 -> "1 час"
        in 2..4 -> "$hours часа"
        else -> "$hours часов"
    }
    val minuteText = when {
        rest == 0 -> ""
        rest % 10 == 1 && rest % 100 != 11 -> "$rest минута"
        rest % 10 in 2..4 && rest % 100 !in 12..14 -> "$rest минуты"
        else -> "$rest минут"
    }
    return listOf(hourText, minuteText).filter { it.isNotEmpty() }.joinToString(" ")
}

fun pairsWord(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> "пар"
        mod10 == 1 -> "пара"
        mod10 in 2..4 -> "пары"
        else -> "пар"
    }
}

private val MONTHS = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)
private val MONTHS_NOM = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
)
private val WEEKDAYS = listOf(
    "понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье",
)
val DAY_SHORT = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

fun formatLongDate(date: LocalDate): String {
    val weekday = WEEKDAYS[date.dayOfWeek.value - 1]
    return "$weekday, ${date.dayOfMonth} ${MONTHS[date.monthValue - 1]}"
}

fun formatMonth(date: LocalDate): String = "${MONTHS_NOM[date.monthValue - 1]} ${date.year}"

fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Доброе утро"
    in 12..17 -> "Добрый день"
    in 18..22 -> "Добрый вечер"
    else -> "Доброй ночи"
}

fun weekBadge(week: String): String = when (week) {
    "num" -> "числитель"
    "den" -> "знаменатель"
    else -> "каждую неделю"
}

fun typeColorKey(type: String): String = when {
    type.contains("лаб", ignoreCase = true) -> "lab"
    type.contains("практ", ignoreCase = true) -> "practice"
    type.contains("электрон", ignoreCase = true) -> "online"
    type.contains("лек", ignoreCase = true) -> "lecture"
    else -> "other"
}
