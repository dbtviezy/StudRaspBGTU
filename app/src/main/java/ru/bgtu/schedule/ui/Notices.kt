package ru.bgtu.schedule.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.bgtu.schedule.data.Updates

@Composable
fun WhatsNewDialog(onClose: () -> Unit) {
    val s = S
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Card,
        titleContentColor = Ink,
        textContentColor = Ink,
        title = { Text(s.more.whatsNewTitle(AppVersion.LABEL)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                s.more.whatsNewItems.forEach { (title, text) ->
                    Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(top = 10.dp))
                    Text(text, color = Muted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(s.more.whatsNewOk, color = Blue, fontWeight = FontWeight.SemiBold) } },
    )
}

@Composable
fun UpdateDialog(tag: String, onClose: () -> Unit) {
    val s = S
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Card,
        titleContentColor = Ink,
        textContentColor = Muted,
        title = { Text(s.more.updateTitle(tag)) },
        text = { Text(s.more.updateText) },
        dismissButton = {
            TextButton(onClick = {
                Updates.dismiss(context, tag)
                onClose()
            }) { Text(s.more.updateLater, color = Muted) }
        },
        confirmButton = {
            TextButton(onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Updates.DOWNLOAD_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                onClose()
            }) { Text(s.more.updateDownload, color = Blue, fontWeight = FontWeight.SemiBold) }
        },
    )
}
