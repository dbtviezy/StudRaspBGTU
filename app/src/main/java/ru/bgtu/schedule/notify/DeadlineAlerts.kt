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
import ru.bgtu.schedule.data.TodoItem
import ru.bgtu.schedule.data.TodoStore
import ru.bgtu.schedule.ui.copyOf
import ru.bgtu.schedule.data.Prefs
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object DeadlineAlerts {
    private const val CHANNEL = "deadlines"
    private const val SLOTS = 24
    private const val BASE = 400

    fun reschedule(context: Context) {
        val app = context.applicationContext
        val manager = app.getSystemService(AlarmManager::class.java) ?: return
        cancel(app, manager)
        val now = LocalDateTime.now()
        val upcoming = TodoStore(app).all().mapNotNull { item ->
            val at = fireAt(item) ?: return@mapNotNull null
            if (!at.isAfter(now)) return@mapNotNull null
            item to at
        }.sortedBy { it.second }.take(SLOTS)
        upcoming.forEachIndexed { index, (item, at) ->
            val intent = Intent(app, DeadlineAlarmReceiver::class.java).apply {
                putExtra("id", BASE + index)
                putExtra("title", item.title)
                putExtra("kind", item.remind)
            }
            val pending = PendingIntent.getBroadcast(
                app,
                BASE + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val whenMs = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
            runCatching {
                if (exactAllowed) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
                else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
            }
        }
    }

    fun show(context: Context, intent: Intent) {
        val copy = copyOf(Prefs(context).language())
        val title = intent.getStringExtra("title").orEmpty().ifBlank { copy.tasks }
        val kind = intent.getStringExtra("kind").orEmpty()
        val text = if (kind == "morning") copy.remindMorning else copy.remindEve
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, copy.deadlineChannel, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = copy.deadlineChannelHint
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
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(intent.getIntExtra("id", BASE), notification)
    }

    private fun cancel(context: Context, manager: AlarmManager) {
        repeat(SLOTS) { index ->
            val intent = Intent(context, DeadlineAlarmReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                BASE + index,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: return@repeat
            manager.cancel(pending)
            pending.cancel()
        }
    }

    private fun fireAt(item: TodoItem): LocalDateTime? {
        if (item.done) return null
        val date = item.deadlineDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        return when (item.remind) {
            "eve" -> date.minusDays(1).atTime(19, 0)
            "morning" -> date.atTime(LocalTime.of(8, 0))
            else -> null
        }
    }
}

class DeadlineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DeadlineAlerts.show(context, intent)
        val pending = goAsync()
        Thread {
            try {
                DeadlineAlerts.reschedule(context)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
