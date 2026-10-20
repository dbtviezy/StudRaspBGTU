package ru.bgtu.schedule.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.bgtu.schedule.MainActivity
import ru.bgtu.schedule.R
import ru.bgtu.schedule.data.Account
import ru.bgtu.schedule.data.ChatStore
import ru.bgtu.schedule.data.Prefs
import ru.bgtu.schedule.data.Profanity

/**
 * Message notifications without a push server: Android wakes [ChatCheckJob] about every 15 minutes
 * (when there is internet), it asks Supabase for the chat list and notifies about new messages.
 */
object ChatNotify {
    private const val JOB_ID = 4201
    private const val CHANNEL = "messages"
    private const val SEEN = "bgtu_chat_seen"
    const val EXTRA_CHAT = "open_chat"

    /** True while the app is on screen: the open chat already shows new messages. */
    @Volatile
    var foreground = false

    fun schedule(context: Context) {
        val app = context.applicationContext
        val scheduler = app.getSystemService(JobScheduler::class.java) ?: return
        if (!Prefs(app).chatNotify()) {
            scheduler.cancel(JOB_ID)
            return
        }
        if (scheduler.getPendingJob(JOB_ID) != null) return
        val job = JobInfo.Builder(JOB_ID, ComponentName(app, ChatCheckJob::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(15 * 60 * 1000L)
            .setPersisted(true)
            .build()
        runCatching { scheduler.schedule(job) }
    }

    /** Remembers the newest message ids so only later ones raise a notification. */
    fun markSeen(context: Context) {
        val seen = context.applicationContext.getSharedPreferences(SEEN, Context.MODE_PRIVATE).edit()
        ChatStore.cachedChats().forEach { chat -> chat.lastId?.let { seen.putLong(chat.id, it) } }
        seen.apply()
    }

    suspend fun check(context: Context) {
        val app = context.applicationContext
        val prefs = Prefs(app)
        if (!prefs.chatNotify()) return
        Account.init(app)
        ChatStore.init(app)
        if (!Account.current.signedIn) return
        val chats = runCatching { ChatStore.refreshChats() }.getOrNull() ?: return
        if (foreground) {
            markSeen(app)
            return
        }
        val seen = app.getSharedPreferences(SEEN, Context.MODE_PRIVATE)
        val edit = seen.edit()
        val copy = ru.bgtu.schedule.ui.copyOf(prefs.language())
        val hide = prefs.hideProfanity()
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, copy.more.channelMessages, NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        for (chat in chats) {
            val last = chat.lastId ?: continue
            val known = seen.getLong(chat.id, -1L)
            edit.putLong(chat.id, last)
            if (known < 0 || last <= known || chat.unread <= 0) continue
            val title = when {
                chat.isDirect -> chat.peerName ?: copy.more.directChat
                chat.title.isNotBlank() -> chat.title
                else -> copy.more.groupChat
            }
            val body = Profanity.maskIf(hide, chat.lastBody.orEmpty())
            val line = if (!chat.isDirect && chat.lastAuthor != null) "${chat.lastAuthor}: $body" else body
            val text = if (chat.unread > 1) "$line\n${copy.more.newMessages(chat.unread)}" else line
            val open = PendingIntent.getActivity(
                app,
                chat.id.hashCode(),
                Intent(app, MainActivity::class.java)
                    .putExtra(EXTRA_CHAT, chat.id)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(app, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_lesson)
                .setContentTitle(title)
                .setContentText(line)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()
            runCatching { manager.notify("chat", chat.id.hashCode(), notification) }
        }
        edit.apply()
    }

    fun cancelFor(context: Context, chatId: String) {
        context.getSystemService(NotificationManager::class.java)?.cancel("chat", chatId.hashCode())
    }
}

class ChatCheckJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        running = scope.launch {
            try {
                ChatNotify.check(applicationContext)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running?.cancel()
        return true
    }
}
