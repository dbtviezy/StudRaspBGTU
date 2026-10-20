package ru.bgtu.schedule.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ru.bgtu.schedule.MainActivity
import ru.bgtu.schedule.R
import ru.bgtu.schedule.data.DayOverrides
import ru.bgtu.schedule.data.HiddenStore
import ru.bgtu.schedule.data.Holidays
import ru.bgtu.schedule.data.Lesson
import ru.bgtu.schedule.data.Prefs
import ru.bgtu.schedule.data.ScheduleRepository
import ru.bgtu.schedule.data.lessonsOn
import ru.bgtu.schedule.data.parseTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * "Class in progress" notification: posted when a class starts, counts down to its end by itself
 * and disappears when the class is over. One alarm for the next class start.
 */
object OngoingLesson {
    private const val CHANNEL = "ongoing"
    private const val NOTIFICATION_ID = 7301
    private const val REQUEST = 7300

    fun reschedule(context: Context) {
        val app = context.applicationContext
        val manager = app.getSystemService(AlarmManager::class.java) ?: return
        val pending = alarmIntent(app)
        manager.cancel(pending)
        val prefs = Prefs(app)
        if (!prefs.ongoingLesson()) {
            app.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
            return
        }
        val lessons = upcoming(app, prefs) ?: return
        val now = LocalDateTime.now()
        lessons.firstOrNull { (start, end, _) -> !now.isBefore(start) && now.isBefore(end) }?.let { (_, end, lesson) ->
            show(app, lesson, end)
        }
        val next = lessons.firstOrNull { (start, _, _) -> start.isAfter(now) } ?: return
        val whenMs = next.first.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        runCatching {
            if (exactAllowed) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
            }
        }
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST,
        Intent(context, OngoingLessonReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Start, end and lesson for today and the next few days, merged with hidden lessons and day changes. */
    private fun upcoming(context: Context, prefs: Prefs): List<Triple<LocalDateTime, LocalDateTime, Lesson>>? {
        val groupId = prefs.groupId() ?: return null
        val bundle = runCatching { ScheduleRepository.load(context) }.getOrNull() ?: return null
        val group = bundle.find(groupId)?.second ?: return null
        val hidden = HiddenStore(context).keys()
        val overrides = DayOverrides(context)
        val result = mutableListOf<Triple<LocalDateTime, LocalDateTime, Lesson>>()
        var date = LocalDate.now()
        val last = date.plusDays(14)
        while (!date.isAfter(last)) {
            if (Holidays.on(date) == null) {
                lessonsOn(group, date.dayOfWeek.value, date, bundle.semesterStart, "auto")
                    .filter { it.key !in hidden }
                    .mapNotNull { overrides.apply(date, it) }
                    .forEach { lesson ->
                        val start = parseTime(lesson.start) ?: return@forEach
                        val end = parseTime(lesson.end) ?: start.plusMinutes(95)
                        result += Triple(LocalDateTime.of(date, start), LocalDateTime.of(date, end), lesson)
                    }
            }
            if (result.any { it.first.isAfter(LocalDateTime.now()) }) break
            date = date.plusDays(1)
        }
        return result.sortedBy { it.first }
    }

    private fun show(context: Context, lesson: Lesson, end: LocalDateTime) {
        val prefs = Prefs(context)
        val copy = ru.bgtu.schedule.ui.copyOf(prefs.language())
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, copy.more.channelOngoing, NotificationManager.IMPORTANCE_LOW),
            )
        }
        val endMs = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val left = endMs - System.currentTimeMillis()
        if (left <= 0) return
        val open = PendingIntent.getActivity(
            context,
            REQUEST,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val endText = "%02d:%02d".format(end.hour, end.minute)
        val text = listOf(copy.more.untilTime(endText), lesson.room).filter { it.isNotBlank() }.joinToString(" · ")
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_lesson)
            .setContentTitle(lesson.subject.ifBlank { copy.lessonFallback })
            .setContentText(text)
            .setWhen(endMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setTimeoutAfter(left)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }
}

class OngoingLessonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                OngoingLesson.reschedule(context)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
