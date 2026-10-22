package ru.bgtu.schedule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import java.time.LocalDate
import java.util.Locale

data class Copy(
    val code: String,
    val tabHome: String,
    val tabSchedule: String,
    val tabGroups: String,
    val tabCalendar: String,
    val tabProfile: String,
    val brand: String,
    val fullTime: String,
    val back: String,
    val chooseFaculty: String,
    val facultyThenGroup: String,
    val chooseGroup: String,
    val name: String,
    val nameHint: String,
    val findGroup: String,
    val groupMissing: String,
    val openSchedule: String,
    val toFaculties: String,
    val groupsTitle: String,
    val groupsHint: String,
    val findGroupOrFaculty: String,
    val nothingFound: String,
    val theme: String,
    val reminders: String,
    val studentFallback: String,
    val today: String,
    val tomorrow: String,
    val upcomingLessons: String,
    val freeWindow: String,
    val freeWindowText: String,
    val schedule: String,
    val filterAuto: String,
    val filterAll: String,
    val numerator: String,
    val denominator: String,
    val calendar: String,
    val previousMonth: String,
    val nextMonth: String,
    val noLessonsThatDay: String,
    val profile: String,
    val changePhoto: String,
    val removePhoto: String,
    val photo: String,
    val group: String,
    val faculty: String,
    val level: String,
    val cohort: String,
    val period: String,
    val form: String,
    val fullTimeForm: String,
    val lessonReminder: String,
    val lessonReminderHint: String,
    val atStart: String,
    val lead5: String,
    val lead10: String,
    val lead15: String,
    val lead30: String,
    val allowExact: String,
    val files: String,
    val filesHint: String,
    val weekHelpTitle: String,
    val weekHelp: String,
    val fetchedLine: String,
    val openSite: String,
    val contact: String,
    val changeGroup: String,
    val resetData: String,
    val resetTitle: String,
    val resetText: String,
    val reset: String,
    val cancel: String,
    val close: String,
    val remindersHint: String,
    val noUpcoming: String,
    val filesBody: String,
    val copying: String,
    val addFiles: String,
    val copyingFiles: String,
    val empty: String,
    val open: String,
    val toLesson: String,
    val delete: String,
    val attachToLesson: String,
    val subjectOrDay: String,
    val attached: String,
    val done: String,
    val copyFailed: String,
    val ok: String,
    val time: String,
    val pair: String,
    val type: String,
    val room: String,
    val teacher: String,
    val day: String,
    val nothingAttached: String,
    val attachFile: String,
    val note: String,
    val noteHint: String,
    val noLessons: String,
    val noLessonsHint: String,
    val hasFile: String,
    val cropTitle: String,
    val cropHint: String,
    val loadFailedTitle: String,
    val loadFailedText: String,
    val language: String,
    val tasks: String,
    val tasksHint: String,
    val tasksEmpty: String,
    val expandTasks: String,
    val collapseTasks: String,
    val addTask: String,
    val newTask: String,
    val editTask: String,
    val taskTitle: String,
    val taskTitleHint: String,
    val deadline: String,
    val noDeadline: String,
    val pickDate: String,
    val clearDeadline: String,
    val timeOptional: String,
    val timeHint: String,
    val badTime: String,
    val attachLessonOptional: String,
    val notAttached: String,
    val removeLesson: String,
    val chooseLesson: String,
    val save: String,
    val overdue: String,
    val completed: String,
    val openTasks: String,
    val tasksForLesson: String,
    val noTasksForLesson: String,
    val hasTask: String,
    val allTasks: String,
    val widgetPickGroup: String,
    val widgetNoLessons: String,
    val lessonFallback: String,
    val lessonStarts: String,
    val channelLessons: String,
    val channelLessonsHint: String,
    val liveInClass: String,
    val liveSoon: String,
    val liveWindow: String,
    val liveDone: String,
    val liveNone: String,
    val liveNext: String,
    val remindOff: String,
    val remindEve: String,
    val remindMorning: String,
    val deadlineChannel: String,
    val deadlineChannelHint: String,
    val exportWeek: String,
    val exportEmpty: String,
    val exportNoCalendar: String,
    val shareDay: String,
    val teachersMode: String,
    val groupsMode: String,
    val findTeacher: String,
    val teacherHint: String,
    val favoriteTeachers: String,
    val noTeacherSoon: String,
    val teacherSoon: String,
    val addFavorite: String,
    val removeFavorite: String,
    val wasAbsent: String,
    val clearAbsence: String,
    val absences: String,
    val absencesEmpty: String,
    val hideLesson: String,
    val attendLesson: String,
    val hiddenTitle: String,
    val hiddenEmpty: String,
    val subjectNote: String,
    val subjectNoteHint: String,
    val bothWeeks: String,
    val numeratorColumn: String,
    val denominatorColumn: String,
    val backupSave: String,
    val backupLoad: String,
    val backupHint: String,
    val backupDone: String,
    val backupRestored: String,
    val backupFailed: String,
    val holidayQuiet: String,
    val telegram: String,
    val secondGroup: String,
    val secondGroupHint: String,
    val pickSecond: String,
    val clearSecond: String,
    val bothFreeTitle: String,
    val noSharedWindow: String,
    val todayChange: String,
    val todayOnly: String,
    val todayRoom: String,
    val roomExample: String,
    val saveRoom: String,
    val cancelToday: String,
    val goneToday: String,
    val officialAgain: String,
    val setLimit: String,
    val clearLimit: String,
    val limitUsed: String,
    val welcomeTitle: String,
    val welcomeText: String,
    val notifyPageTitle: String,
    val notifyPageText: String,
    val notifyAsk: String,
    val notifyYes: String,
    val notifyNo: String,
    val searchPageTitle: String,
    val searchPageText: String,
    val nextPage: String,
    val begin: String,
    val themeLight: String,
    val themeDark: String,
    val langRu: String,
    val langEn: String,
    val localData: String,
    val updatesInTelegram: String,
    val missedBadge: String,
    val restore: String,
    val ratingTitle: String,
    val ratingNone: String,
    val ratingOpen: String,
    val ratingOverall: String,
    val ratingDelivery: String,
    val ratingStrictness: String,
    val ratingLoyalty: String,
    val ratingAnonymous: String,
    val ratingOffline: String,
    val rateTeacher: String,
    val editReview: String,
    val noReviews: String,
    val reviewText: String,
    val reviewHint: String,
    val sendReview: String,
    val deleteReview: String,
    val retry: String,
    val loading: String,
    val weekdays: List<String>,
    val monthsGenitive: List<String>,
    val monthsNominative: List<String>,
    val dayShort: List<String>,
) {
    fun formatLongDate(date: LocalDate): String {
        val weekday = weekdays[date.dayOfWeek.value - 1]
        return "$weekday, ${date.dayOfMonth} ${monthsGenitive[date.monthValue - 1]}"
    }

    fun formatLongDateCap(date: LocalDate): String {
        val locale = if (code == "en") Locale.ENGLISH else Locale("ru")
        return formatLongDate(date).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }
    }

    fun formatMonth(date: LocalDate): String = "${monthsNominative[date.monthValue - 1]} ${date.year}"

    fun greeting(hour: Int): String = when (code) {
        "en" -> when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            in 18..22 -> "Good evening"
            else -> "Good night"
        }
        else -> when (hour) {
            in 5..11 -> "Доброе утро"
            in 12..17 -> "Добрый день"
            in 18..22 -> "Добрый вечер"
            else -> "Доброй ночи"
        }
    }

    fun pairsWord(count: Int): String = if (code == "en") {
        if (count == 1) "class" else "classes"
    } else {
        val mod100 = count % 100
        val mod10 = count % 10
        when {
            mod100 in 11..14 -> "пар"
            mod10 == 1 -> "пара"
            mod10 in 2..4 -> "пары"
            else -> "пар"
        }
    }

    fun todayPill(count: Int): String = if (code == "en") {
        "$count ${pairsWord(count)} today"
    } else {
        "$count ${pairsWord(count)} сегодня"
    }

    fun groupsCount(count: Int): String = if (code == "en") {
        if (count == 1) "1 group" else "$count groups"
    } else {
        val mod100 = count % 100
        val mod10 = count % 10
        val word = when {
            mod100 in 11..14 -> "групп"
            mod10 == 1 -> "группа"
            mod10 in 2..4 -> "группы"
            else -> "групп"
        }
        "$count $word"
    }

    fun evenWeekTitle(even: Boolean): String = if (code == "en") {
        if (even) "Even week" else "Odd week"
    } else {
        if (even) "чётная неделя" else "нечётная неделя"
    }

    fun kindTitle(even: Boolean): String = if (even) denominator else numerator

    fun studyWeekLine(number: Int, even: Boolean): String = if (code == "en") {
        "Academic week $number · ${kindTitle(even)}"
    } else {
        "$number-я учебная неделя · ${kindTitle(even)}"
    }

    fun weekNow(number: Int, even: Boolean): String = if (code == "en") {
        "Now ${kindTitle(even)}. Semester week $number."
    } else {
        "Сейчас ${kindTitle(even)}. $number-я неделя семестра."
    }

    fun widgetWeek(number: Int, even: Boolean): String = if (code == "en") {
        "Week $number · ${kindTitle(even)}"
    } else {
        "$number-я неделя · ${kindTitle(even)}"
    }

    fun weekBadge(week: String): String = when (week) {
        "num" -> numerator
        "den" -> denominator
        else -> if (code == "en") "Every week" else "каждую неделю"
    }

    fun pairLabel(number: Int): String = if (code == "en") "Class $number" else "$number пара"

    fun cohortValue(year: Int): String = if (code == "en") "Class of $year" else "набор $year"

    fun whenLabel(day: LocalDate, today: LocalDate): String = when {
        day == today -> this.today.lowercase()
        day == today.plusDays(1) -> tomorrow.lowercase()
        else -> formatLongDate(day)
    }

    fun lessonIn(lead: Int): String = if (code == "en") "Class in $lead min" else "Пара через $lead мин"

    fun bothFreeLine(day: LocalDate, today: LocalDate, time: String): String = when {
        code == "en" && day == today -> "Both free at $time"
        code == "en" && day == today.plusDays(1) -> "Both free tomorrow at $time"
        code == "en" -> "Both free on ${formatLongDate(day)} at $time"
        day == today -> "Оба свободны в $time"
        day == today.plusDays(1) -> "Оба свободны завтра в $time"
        else -> "Оба свободны ${formatLongDate(day)} в $time"
    }

    fun skipsLeft(left: Int): String = if (code == "en") {
        if (left == 1) "1 left" else "$left left"
    } else {
        "осталось $left"
    }

    fun openTasksLine(count: Int): String = if (code == "en") {
        if (count == 1) "1 open task" else "$count open tasks"
    } else {
        val mod100 = count % 100
        val mod10 = count % 10
        val word = when {
            mod100 in 11..14 -> "дел"
            mod10 == 1 -> "дело"
            mod10 in 2..4 -> "дела"
            else -> "дел"
        }
        "$count $word"
    }

    fun filesCopied(names: String): String = if (code == "en") {
        "Copied into the app: $names. This is a separate copy on the phone. The original can be deleted."
    } else {
        "Скопировано в память приложения: $names. Это отдельная копия на телефоне, оригинал можно удалить."
    }

    val more: MoreCopy get() = if (code == "en") MoreEn else MoreRu

    val acc: AccountCopy get() = if (code == "en") AccountEn else AccountRu

    /** Like [errorText], but spells out restrictions (what, until when, why) from the error details. */
    fun failureText(failure: ru.bgtu.schedule.data.ApiFailure): String {
        val extra = failure.extra
        if ((failure.code == "restricted" || failure.code == "banned") && extra.has("scope")) {
            val until = if (extra.isNull("until")) null else extra.optString("until").ifBlank { null }
            return acc.restrictionText(extra.optString("scope"), until, extra.optString("reason"), more.banned)
        }
        if (failure.code == "account_too_new" && extra.optString("until").isNotBlank()) return acc.accountTooNew(extra.optString("until"))
        return errorText(failure.code)
    }

    fun restrictionText(restriction: ru.bgtu.schedule.data.Restriction): String =
        acc.restrictionText(restriction.scope, restriction.until, restriction.reason, more.banned)

    fun periodLabel(raw: String): String =
        if (code == "en") raw.replace(Regex("""(\d+)\s*семестр"""), "semester $1").replace("семестр", "semester") else raw

    fun facultyName(raw: String): String = if (code == "en") FACULTIES_EN[raw.trim()] ?: raw else raw

    fun roomName(raw: String): String = if (code != "en") raw else raw
        .replace("Читальный зал", "Reading room")
        .replace("Спортивный зал", "Gym")
        .replace("Лаборатория", "Lab")

    fun teacherGroups(count: Int): String = if (code == "en") {
        if (count == 1) "teaches 1 group" else "teaches $count groups"
    } else {
        val mod100 = count % 100
        "ведёт у $count ${if (count % 10 == 1 && mod100 != 11) "группы" else "групп"}"
    }

    fun unreadCount(count: Int): String = if (count > 99) "99+" else count.toString()

    fun formatBytes(size: Long): String {
        val en = code == "en"
        return when {
            size < 1024 -> "$size ${if (en) "B" else "Б"}"
            size < 1024 * 1024 -> "${size / 1024} ${if (en) "KB" else "КБ"}"
            else -> String.format(if (en) Locale.US else Locale("ru"), "%.1f %s", size / 1048576.0, if (en) "MB" else "МБ")
        }
    }

    /** Human text for an error code returned by our API or the chat server. */
    fun errorText(code: String): String = when (code) {
        "offline", "db_unavailable", "not_configured", "server_error", "chat_unavailable", "provider_unavailable" -> more.serverOff
        "chat_not_configured" -> more.chatsOff
        "provider_disabled" -> more.providerOff
        "rate_limited" -> more.tooMany
        "wrong_credentials" -> more.wrongCredentials
        "login_taken" -> more.loginTaken
        "invalid_login" -> more.badLogin
        "invalid_password" -> more.badPassword
        "invalid_name" -> more.badName
        "already_linked" -> more.alreadyLinked
        "last_method" -> more.lastMethod
        "banned" -> more.banned
        "restricted" -> more.forbidden
        "muted" -> acc.muted
        "no_create" -> acc.noCreate
        "social_required", "account_too_new" -> acc.socialRequired
        "device_used" -> acc.deviceUsed
        "already_rep" -> acc.alreadyRep
        "own_review" -> more.ownReview
        "invalid_code" -> more.invalidCode
        "user_not_found" -> more.userNotFound
        "forbidden", "unauthorized" -> more.forbidden
        "text_too_long" -> ratingError(code)
        else -> more.somethingWrong
    }

    fun levelName(raw: String): String {
        val key = raw.trim().lowercase()
        val named = when (key) {
            "бакалавр" -> if (code == "en") "Bachelor" else "Бакалавр"
            "магистр" -> if (code == "en") "Master" else "Магистр"
            "специалист" -> if (code == "en") "Specialist" else "Специалист"
            else -> null
        }
        return named ?: raw.replaceFirstChar { it.uppercase() }.ifBlank { "—" }
    }

    fun dayName(raw: String): String {
        if (code != "en") return raw
        return when (raw) {
            "Понедельник" -> "Monday"
            "Вторник" -> "Tuesday"
            "Среда" -> "Wednesday"
            "Четверг" -> "Thursday"
            "Пятница" -> "Friday"
            "Суббота" -> "Saturday"
            "Воскресенье" -> "Sunday"
            else -> raw
        }
    }

    fun lessonType(raw: String): String {
        if (code != "en" || raw.isBlank()) return raw
        var text = raw
        listOf(
            "Электронные лекции" to "Online lectures",
            "Лабораторные работы" to "Lab work",
            "Практические занятия" to "Practice",
            "Лекции" to "Lectures",
        ).forEach { (ru, en) -> text = text.replace(ru, en) }
        return text
    }

    fun span(minutes: Int): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return if (code == "en") {
            val hourText = when (hours) {
                0 -> ""
                1 -> "1 hour"
                else -> "$hours hours"
            }
            val minuteText = when (rest) {
                0 -> ""
                1 -> "1 minute"
                else -> "$rest minutes"
            }
            listOf(hourText, minuteText).filter { it.isNotEmpty() }.joinToString(" ").ifBlank { "0 minutes" }
        } else {
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
            listOf(hourText, minuteText).filter { it.isNotEmpty() }.joinToString(" ").ifBlank { "0 минут" }
        }
    }

    fun leftPhrase(minutes: Int): String = if (code == "en") "${span(minutes)} left" else "ещё ${span(minutes)}"

    fun untilPhrase(minutes: Int): String = if (code == "en") "in ${span(minutes)}" else "через ${span(minutes)}"

    fun exported(count: Int): String = if (code == "en") {
        if (count == 1) "1 class added to the calendar" else "$count classes added to the calendar"
    } else {
        "В календарь добавлено: $count"
    }

    fun absenceCount(count: Int): String = if (code == "en") {
        if (count == 1) "1 miss" else "$count misses"
    } else {
        val mod100 = count % 100
        val mod10 = count % 10
        val word = when {
            mod100 in 11..14 -> "пропусков"
            mod10 == 1 -> "пропуск"
            mod10 in 2..4 -> "пропуска"
            else -> "пропусков"
        }
        "$count $word"
    }

    fun hiddenSection(count: Int): String = if (code == "en") "Hidden ($count)" else "Скрыто ($count)"

    fun resetFiles(count: Int, size: String): String = if (code == "en") {
        "Also delete files copied into the app ($count · $size)"
    } else {
        "Также удалить файлы, скопированные в приложение ($count · $size)"
    }

    fun reviewsCount(count: Int): String = if (code == "en") {
        if (count == 1) "1 rating" else "$count ratings"
    } else {
        val mod100 = count % 100
        val mod10 = count % 10
        val word = when {
            mod100 in 11..14 -> "оценок"
            mod10 == 1 -> "оценка"
            mod10 in 2..4 -> "оценки"
            else -> "оценок"
        }
        "$count $word"
    }

    fun ratingError(reason: String): String = when (reason) {
        "offline" -> ratingOffline
        "rate_limited" -> if (code == "en") "Too many changes. Try again in an hour." else "Слишком много изменений. Попробуйте через час."
        "text_too_long" -> if (code == "en") "The review is longer than 300 characters" else "Отзыв длиннее 300 символов"
        "unknown_teacher" -> if (code == "en") "This teacher is not in the schedule" else "Этого преподавателя нет в расписании"
        "banned" -> more.banned
        else -> more.ratingFailed
    }

    fun leadOptions(): List<Pair<Int, String>> = listOf(
        0 to atStart,
        5 to lead5,
        10 to lead10,
        15 to lead15,
        30 to lead30,
    )
}

private val FACULTIES_EN = mapOf(
    "Механико-технологический факультет" to "Faculty of Mechanical Engineering and Technology",
    "Учебно-научный институт транспорта" to "Institute of Transport",
    "Учебно-научный технологический институт" to "Institute of Technology",
    "Факультет информационных технологий" to "Faculty of Information Technology",
    "Факультет отраслевой и цифровой экономики" to "Faculty of Industrial and Digital Economics",
    "Факультет энергетики и электроники" to "Faculty of Power Engineering and Electronics",
)

fun copyOf(code: String?): Copy = if (code == "en") CopyEn else CopyRu

val LocalCopy = staticCompositionLocalOf { CopyRu }

val S: Copy
    @Composable
    get() = LocalCopy.current
