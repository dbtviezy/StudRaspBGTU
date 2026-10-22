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
import ru.bgtu.schedule.data.HiddenStore
import ru.bgtu.schedule.data.Holidays
import ru.bgtu.schedule.data.NotifyProfile
import ru.bgtu.schedule.data.Prefs
import ru.bgtu.schedule.data.ScheduleRepository
import ru.bgtu.schedule.data.lessonsOn
import ru.bgtu.schedule.ui.copyOf
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** One evening push: tomorrow's class count and the first room, or silence if the day is empty. */
object DigestAlerts {
    private const val CHANNEL = "digest"
    private const val REQ = 9001

    fun reschedule(context: Context) {
        val app = context.applicationContext
        val manager = app.getSystemService(AlarmManager::class.java) ?: return
        cancel(app, manager)
        val prefs = Prefs(app)
        if (!prefs.digestEnabled() || !NotifyProfile.allowsDigest(prefs.notifyProfile())) return
        if (prefs.groupId() == null) return
        val hour = prefs.digestHour()
        var fire = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour, 0))
        if (!fire.isAfter(LocalDateTime.now())) fire = fire.plusDays(1)
        val intent = Intent(app, DigestAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            app,
            REQ,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val whenMs = fire.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        schedule(manager, whenMs, pending)
    }

    fun cancelAll(context: Context) {
        val manager = context.applicationContext.getSystemService(AlarmManager::class.java) ?: return
        cancel(context.applicationContext, manager)
    }

    fun show(context: Context) {
        val app = context.applicationContext
        val prefs = Prefs(app)
        val copy = copyOf(prefs.language())
        val groupId = prefs.groupId() ?: return
        val bundle = runCatching { ScheduleRepository.load(app) }.getOrNull() ?: return
        val group = bundle.find(groupId)?.second ?: return
        val tomorrow = LocalDate.now().plusDays(1)
        if (Holidays.on(tomorrow) != null) return
        val hidden = HiddenStore(app).keys()
        val overrides = ru.bgtu.schedule.data.DayOverrides(app)
        val lessons = lessonsOn(group, tomorrow.dayOfWeek.value, tomorrow, bundle.semesterStart, "auto")
            .filter { it.key !in hidden }
            .mapNotNull { overrides.apply(tomorrow, it) }
        if (lessons.isEmpty()) return
        val first = lessons.first()
        val time = first.time.ifBlank { first.start }
        val text = copy.more.digestBody(lessons.size, time, first.room)
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, copy.more.channelDigest, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = copy.more.channelDigestHint
                },
            )
        }
        val open = PendingIntent.getActivity(
            app,
            0,
            Intent(app, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            REQ,
            NotificationCompat.Builder(app, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_lesson)
                .setContentTitle(copy.more.digestTitle)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun cancel(context: Context, manager: AlarmManager) {
        val pending = PendingIntent.getBroadcast(
            context,
            REQ,
            Intent(context, DigestAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        manager.cancel(pending)
        pending.cancel()
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
}

class DigestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        DigestAlerts.show(context)
        val pending = goAsync()
        Thread {
            try {
                DigestAlerts.reschedule(context)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
