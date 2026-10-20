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
import ru.bgtu.schedule.data.Lesson
import ru.bgtu.schedule.data.Prefs
import ru.bgtu.schedule.data.ScheduleRepository
import ru.bgtu.schedule.data.lessonsOn
import ru.bgtu.schedule.data.parseTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object LessonAlerts {
    private const val CHANNEL = "lessons"
    private const val SLOTS = 16

    fun reschedule(context: Context) {
        val app = context.applicationContext
        val manager = app.getSystemService(AlarmManager::class.java) ?: return
        cancel(app, manager)
        val prefs = Prefs(app)
        if (!prefs.notifyEnabled()) return
        val groupId = prefs.groupId() ?: return
        val bundle = runCatching { ScheduleRepository.load(app) }.getOrNull() ?: return
        val group = bundle.find(groupId)?.second ?: return
        val hidden = ru.bgtu.schedule.data.HiddenStore(app).keys()
        val lead = prefs.notifyLead()
        val now = LocalDateTime.now()
        val upcoming = nextLessons(app, group, bundle.semesterStart, now, hidden)
        upcoming.take(SLOTS).forEachIndexed { index, (date, lesson) ->
            val start = parseTime(lesson.start) ?: return@forEachIndexed
            val fireAt = LocalDateTime.of(date, start).minusMinutes(lead.toLong())
            if (!fireAt.isAfter(now)) return@forEachIndexed
            val intent = Intent(app, LessonAlarmReceiver::class.java).apply {
                putExtra("id", index + 1)
                putExtra("subject", lesson.subject)
                putExtra("time", lesson.time.ifBlank { lesson.start })
                putExtra("room", lesson.room)
                putExtra("lead", lead)
            }
            val pending = PendingIntent.getBroadcast(
                app,
                index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val whenMs = fireAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            schedule(manager, whenMs, pending)
        }
    }

    fun cancelAll(context: Context) {
        val manager = context.applicationContext.getSystemService(AlarmManager::class.java) ?: return
        cancel(context.applicationContext, manager)
    }

    fun show(context: Context, intent: Intent) {
        val copy = ru.bgtu.schedule.ui.copyOf(Prefs(context).language())
        val subject = intent.getStringExtra("subject").orEmpty().ifBlank { copy.lessonFallback }
        val time = intent.getStringExtra("time").orEmpty()
        val room = intent.getStringExtra("room").orEmpty()
        val lead = intent.getIntExtra("lead", 0)
        val title = if (lead <= 0) copy.lessonStarts else copy.lessonIn(lead)
        val text = listOf(subject, time, room).filter { it.isNotBlank() }.joinToString(" · ")
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, copy.channelLessons, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = copy.channelLessonsHint
                },
            )
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_lesson)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(intent.getIntExtra("id", 1), notification)
    }

    private fun cancel(context: Context, manager: AlarmManager) {
        repeat(SLOTS) { index ->
            val intent = Intent(context, LessonAlarmReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                index,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: return@repeat
            manager.cancel(pending)
            pending.cancel()
        }
    }

    private fun schedule(manager: AlarmManager, whenMs: Long, pending: PendingIntent) {
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        runCatching {
            if (exactAllowed) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
            }
        }.onFailure {
            runCatching { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending) }
        }
    }

    private fun nextLessons(
        context: Context,
        group: ru.bgtu.schedule.data.Group,
        semesterStart: LocalDate,
        now: LocalDateTime,
        hidden: Set<String>,
    ): List<Pair<LocalDate, Lesson>> {
        val result = mutableListOf<Pair<LocalDate, Lesson>>()
        val overrides = ru.bgtu.schedule.data.DayOverrides(context)
        var date = now.toLocalDate()
        val end = date.plusDays(21)
        while (!date.isAfter(end)) {
            if (ru.bgtu.schedule.data.Holidays.on(date) != null) {
                date = date.plusDays(1)
                continue
            }
            lessonsOn(group, date.dayOfWeek.value, date, semesterStart, "auto")
                .filter { it.key !in hidden }
                .mapNotNull { overrides.apply(date, it) }
                .forEach { lesson ->
                val start = parseTime(lesson.start) ?: return@forEach
                if (LocalDateTime.of(date, start).isAfter(now)) result += date to lesson
            }
            date = date.plusDays(1)
        }
        return result
    }
}

class LessonAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LessonAlerts.show(context, intent)
        val pending = goAsync()
        Thread {
            try {
                LessonAlerts.reschedule(context)
            } finally {
                pending.finish()
            }
        }.start()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        Thread {
            try {
                LessonAlerts.reschedule(context)
                DeadlineAlerts.reschedule(context)
                OngoingLesson.reschedule(context)
                ChatNotify.schedule(context)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
