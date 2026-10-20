package ru.bgtu.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.bgtu.schedule.ui.BgtuApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0xFF2A56F6.toInt()),
            navigationBarStyle = SystemBarStyle.dark(0xFF2A56F6.toInt()),
        )
        setContent {
            BgtuApp()
        }
    }
}
