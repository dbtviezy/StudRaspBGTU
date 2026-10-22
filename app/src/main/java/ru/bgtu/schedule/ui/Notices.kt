package ru.bgtu.schedule.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.bgtu.schedule.data.RatingApi
import ru.bgtu.schedule.data.Sync
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

/** One-time offer to sign in; warns when old reviews from this phone would be hidden. */
@Composable
fun LoginOfferDialog(onSignIn: () -> Unit, onClose: () -> Unit) {
    val s = S
    val context = LocalContext.current
    var warning by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val claimable = RatingApi.claimable(context)
        if (claimable != null && claimable.count > 0) warning = s.acc.legacyWarn(claimable.until)
    }
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Card,
        titleContentColor = Ink,
        textContentColor = Muted,
        title = { Text(s.acc.loginOfferTitle) },
        text = {
            Column {
                Text(s.acc.loginOfferText, fontSize = 14.sp)
                warning?.let { Text(it, color = StarYellow, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp)) }
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(s.acc.later, color = Muted) } },
        confirmButton = {
            TextButton(onClick = onSignIn) { Text(s.more.signIn, color = Blue, fontWeight = FontWeight.SemiBold) }
        },
    )
}

/** Asked on the first sync of a device when it and the account hold different data. */
@Composable
fun SyncQuestionDialog() {
    val s = S
    AlertDialog(
        onDismissRequest = {},
        containerColor = Card,
        titleContentColor = Ink,
        textContentColor = Muted,
        title = { Text(s.acc.syncAskTitle) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(s.acc.syncAskText, fontSize = 14.sp)
                SyncChoiceRow(s.acc.syncMerge, s.acc.syncMergeHint) { Sync.answer(Sync.Choice.MERGE) }
                SyncChoiceRow(s.acc.syncTakeAccount, s.acc.syncTakeAccountHint) { Sync.answer(Sync.Choice.ACCOUNT) }
                SyncChoiceRow(s.acc.syncKeepDevice, s.acc.syncKeepDeviceHint) { Sync.answer(Sync.Choice.DEVICE) }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun SyncChoiceRow(title: String, hint: String, onClick: () -> Unit) {
    Column(
        Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(hint, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
    }
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
