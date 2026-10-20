package ru.bgtu.schedule

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.bgtu.schedule.data.Account
import ru.bgtu.schedule.data.AdminApi
import ru.bgtu.schedule.data.ChatStore
import ru.bgtu.schedule.data.Sync
import ru.bgtu.schedule.notify.ChatNotify
import ru.bgtu.schedule.ui.AppSignals
import ru.bgtu.schedule.ui.BgtuApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Account.init(this)
        ChatStore.init(this)
        AdminApi.init(this)
        if (savedInstanceState == null) {
            Account.acceptLink(intent?.data)
            openChatFrom(intent)
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0xFF2A56F6.toInt()),
            navigationBarStyle = SystemBarStyle.dark(0xFF2A56F6.toInt()),
        )
        setContent {
            BgtuApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Account.acceptLink(intent.data)
        openChatFrom(intent)
    }

    override fun onStart() {
        super.onStart()
        ChatNotify.foreground = true
        Sync.syncSoon(this)
    }

    override fun onStop() {
        super.onStop()
        ChatNotify.foreground = false
        ChatNotify.markSeen(this)
        Sync.syncSoon(this)
    }

    private fun openChatFrom(intent: Intent?) {
        intent?.getStringExtra(ChatNotify.EXTRA_CHAT)?.let { AppSignals.openChat = it }
    }
}
