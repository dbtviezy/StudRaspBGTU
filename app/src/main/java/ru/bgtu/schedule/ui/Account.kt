package ru.bgtu.schedule.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject
import ru.bgtu.schedule.data.Account
import ru.bgtu.schedule.data.ApiFailure
import ru.bgtu.schedule.data.AuthConfig
import ru.bgtu.schedule.data.ChatStore
import ru.bgtu.schedule.data.DeviceSession
import ru.bgtu.schedule.data.MyReview
import ru.bgtu.schedule.data.Prefs
import ru.bgtu.schedule.data.RatingApi
import ru.bgtu.schedule.data.Sync

/** Loads the server's sign-in options; null while loading, failure code when the server is unreachable. */
@Composable
private fun rememberAuthConfig(): Pair<AuthConfig?, String?> {
    var config by remember { mutableStateOf<AuthConfig?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            config = Account.config()
        } catch (failure: ApiFailure) {
            error = failure.code
        }
    }
    return config to error
}

/** Runs a server call with a busy flag and a readable error. */
private class AccountAction {
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
}

@Composable
private fun rememberAction(): Pair<AccountAction, (suspend () -> Unit, () -> Unit) -> Unit> {
    val action = remember { AccountAction() }
    val scope = rememberCoroutineScope()
    val s = S
    val run: (suspend () -> Unit, () -> Unit) -> Unit = { body, after ->
        if (!action.busy) {
            action.busy = true
            action.error = null
            scope.launch {
                try {
                    body()
                    after()
                } catch (failure: ApiFailure) {
                    action.error = s.failureText(failure)
                } finally {
                    action.busy = false
                }
            }
        }
    }
    return action to run
}

@Composable
fun SignInSheet(onClose: () -> Unit) {
    val s = S
    val context = LocalContext.current
    val (config, configError) = rememberAuthConfig()
    val (action, run) = rememberAction()
    var signUp by remember { mutableStateOf(false) }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(Prefs(context.applicationContext).name().trim()) }
    val lang = if (s.code == "en") "en" else "ru"

    fun openBrowser(provider: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Account.browserSignInUrl(provider, lang))))
        onClose()
    }

    BottomSheet(onDismiss = onClose) {
        Text(if (signUp) s.more.signUp else s.more.signIn, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(s.more.accountHint, color = Muted, fontSize = 13.sp)
        if (configError != null) ErrorLine(s.errorText(configError))
        if (config != null) {
            if (config.telegramBot != null || config.googleClientId != null) {
                if (config.telegramBot != null) {
                    OutlineButton(s.more.withTelegram, onClick = { openBrowser("telegram") })
                }
                if (config.googleClientId != null) {
                    OutlineButton(s.more.withGoogle, onClick = { openBrowser("google") })
                }
                Text(s.more.browserHint, color = Muted, fontSize = 12.sp)
            } else {
                Text(s.more.providerOff, color = Muted, fontSize = 12.sp)
            }
            Text(s.more.orWord, color = Muted, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        InputField(login, { login = it.take(24) }, s.more.loginField, hint = if (signUp) s.more.loginRule else null)
        InputField(
            password,
            { password = it.take(100) },
            s.more.passwordField,
            password = true,
            hint = if (signUp) s.more.passwordRule else null,
        )
        if (signUp) {
            InputField(name, { name = it.take(40) }, s.more.chatName, hint = s.more.chatNameRule)
        }
        ErrorLine(action.error)
        PrimaryButton(
            if (signUp) s.more.signUp else s.more.signIn,
            onClick = {
                run({
                    if (signUp) Account.register(login, password, name) else Account.signIn(login, password)
                }, {
                    Toasts.show(s.more.signedInToast)
                    onClose()
                })
            },
            enabled = !action.busy && login.isNotBlank() && password.isNotEmpty(),
        )
        TextButton(onClick = { signUp = !signUp }) {
            Text(if (signUp) s.more.haveAccount else s.more.noAccount, color = Blue)
        }
    }
}

private enum class EditKind { NAME, PASSWORD, CREDENTIALS }

@Composable
private fun EditSheet(kind: EditKind, onClose: () -> Unit) {
    val s = S
    val user by Account.state.collectAsState()
    val (action, run) = rememberAction()
    var name by remember { mutableStateOf(user.user?.name.orEmpty()) }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    val title = when (kind) {
        EditKind.NAME -> s.more.chatName
        EditKind.PASSWORD -> s.more.changePassword
        EditKind.CREDENTIALS -> s.more.setPassword
    }
    BottomSheet(onDismiss = onClose) {
        Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        when (kind) {
            EditKind.NAME -> InputField(name, { name = it.take(40) }, s.more.chatName, hint = s.more.chatNameRule)
            EditKind.CREDENTIALS -> {
                InputField(login, { login = it.take(24) }, s.more.loginField, hint = s.more.loginRule)
                InputField(password, { password = it.take(100) }, s.more.passwordField, password = true, hint = s.more.passwordRule)
            }
            EditKind.PASSWORD -> {
                InputField(password, { password = it.take(100) }, s.more.passwordField, password = true)
                InputField(next, { next = it.take(100) }, s.more.newPassword, password = true, hint = s.more.passwordRule)
            }
        }
        ErrorLine(action.error)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClose) { Text(s.cancel, color = Muted) }
            TextButton(
                enabled = !action.busy,
                onClick = {
                    val patch = when (kind) {
                        EditKind.NAME -> JSONObject().put("name", name.trim())
                        EditKind.PASSWORD -> JSONObject().put("password", password).put("newPassword", next)
                        EditKind.CREDENTIALS -> JSONObject().put("login", login.trim()).put("password", password)
                    }
                    run({ Account.update(patch) }, onClose)
                },
            ) { Text(s.save, color = Blue) }
        }
    }
}

/** Account card on the profile screen, same layout as the web one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountCard(groupId: String? = null, onOpenChats: () -> Unit = {}) {
    val s = S
    val context = LocalContext.current
    val state by Account.state.collectAsState()
    val user = state.user
    var signing by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EditKind?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var askReviews by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<String?>(null) }
    val (action, run) = rememberAction()

    if (user == null || !state.signedIn) {
        var warning by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            val claimable = RatingApi.claimable(context)
            if (claimable != null && claimable.count > 0) warning = s.acc.legacyWarn(claimable.until)
        }
        SoftCard {
            Text(s.more.account, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(s.more.accountHint, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            warning?.let { Text(it, color = StarYellow, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            PrimaryButton(s.more.signIn, onClick = { signing = true }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        }
        if (signing) SignInSheet(onClose = { signing = false })
        return
    }

    fun leave(everywhere: Boolean = false, deleteAccount: Boolean = false, keepReviews: Boolean = false) {
        run({
            Account.signOut(everywhere, deleteAccount, keepReviews)
            ChatStore.clear()
        }, {
            if (everywhere) Toasts.show(s.more.signOut)
        })
    }

    SoftCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .clickable { editing = EditKind.NAME },
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BlueSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(user.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = Blue, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user.name, color = Ink, fontWeight = FontWeight.SemiBold)
                Text(user.login?.let { "@$it" } ?: s.more.account, color = Muted, fontSize = 12.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = s.more.chatName, tint = Muted)
        }
        DetailRow(s.more.yourId, user.shortId)
        Text(s.more.yourIdHint, color = Muted, fontSize = 12.sp)

        ProviderLine("Telegram", user.telegram) { run({ Account.update(JSONObject().put("unlink", "telegram")) }, {}) }
        ProviderLine("Google", user.google) { run({ Account.update(JSONObject().put("unlink", "google")) }, {}) }
        if (!user.telegram || !user.google) {
            Text(s.more.linkOnSite, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        ErrorLine(action.error)

        if (state.restrictions.isNotEmpty()) {
            Text(s.acc.restrictionsTitle, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
            state.restrictions.forEach { Text(s.restrictionText(it), color = Red, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
        }
        SyncBlock()
        RepBlock(groupId)

        FlowRow(Modifier.padding(top = 8.dp)) {
            TextButton(onClick = { sheet = "reviews" }) { Text(s.acc.myReviews, color = Blue) }
            TextButton(onClick = { sheet = "devices" }) { Text(s.acc.devices, color = Blue) }
        }

        FlowRow {
            if (user.login != null) {
                TextButton(onClick = { editing = EditKind.PASSWORD }) { Text(s.more.changePassword, color = Blue) }
            } else {
                TextButton(onClick = { editing = EditKind.CREDENTIALS }) { Text(s.more.setPassword, color = Blue) }
            }
            TextButton(onClick = onOpenChats) { Text(s.more.chatsTitle, color = Blue) }
            TextButton(onClick = { leave() }) { Text(s.more.signOut, color = Blue) }
            TextButton(onClick = { leave(everywhere = true) }) { Text(s.more.signOutEverywhere, color = Muted) }
            TextButton(onClick = { deleting = true }) { Text(s.more.deleteAccount, color = Red) }
        }
    }

    editing?.let { kind -> EditSheet(kind) { editing = null } }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            containerColor = Card,
            titleContentColor = Ink,
            textContentColor = Muted,
            title = { Text(s.more.deleteAccount) },
            text = { Text(s.more.deleteAccountText) },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text(s.cancel, color = Muted) } },
            confirmButton = {
                TextButton(onClick = {
                    deleting = false
                    askReviews = true
                }) { Text(s.delete, color = Red) }
            },
        )
    }
    if (askReviews) {
        BottomSheet(onDismiss = { askReviews = false }) {
            Text(s.more.deleteAccount, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(s.acc.deleteReviewsQuestion, color = Muted, fontSize = 13.sp)
            OutlineButton(s.acc.keepReviewsAnon, onClick = {
                askReviews = false
                leave(deleteAccount = true, keepReviews = true)
            })
            OutlineButton(s.acc.deleteWithReviews, color = Red, onClick = {
                askReviews = false
                leave(deleteAccount = true, keepReviews = false)
            })
            TextButton(onClick = { askReviews = false }, modifier = Modifier.align(Alignment.End)) { Text(s.cancel, color = Muted) }
        }
    }
    when (sheet) {
        "reviews" -> MyReviewsSheet { sheet = null }
        "devices" -> DevicesSheet { sheet = null }
    }
}

@Composable
private fun SyncBlock() {
    val s = S
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val status by Sync.status.collectAsState()
    val at = status.at ?: remember(status.busy) { Sync.lastAt(context) }
    Text(s.acc.syncTitle, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
    Text(s.acc.syncHint, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            when {
                status.failed -> s.acc.syncFailed
                at != null -> s.acc.syncDone(at)
                else -> ""
            },
            color = if (status.failed) Red else Muted,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        TextButton(enabled = !status.busy, onClick = { scope.launch { Sync.run(context.applicationContext) } }) {
            Text(s.acc.syncNow, color = Blue)
        }
    }
}

@Composable
private fun RepBlock(groupId: String?) {
    val s = S
    val state by Account.state.collectAsState()
    val (action, run) = rememberAction()
    var writing by remember { mutableStateOf(false) }
    var comment by remember { mutableStateOf("") }
    val rep = state.rep
    val current = rep?.takeIf { it.status == "pending" || it.status == "approved" }
    Text(s.acc.repTitle, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
    Text(s.acc.repHint, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
    if (current != null && current.status == "approved") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Text(s.acc.repApproved(current.groupId), color = Ink, fontSize = 13.sp)
            RepBadge()
        }
    } else if (current != null) {
        Text(s.acc.repPending(current.groupId), color = Ink, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
    } else if (rep?.status == "rejected") {
        Text(s.acc.repRejected, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
    }
    when {
        current != null -> TextButton(enabled = !action.busy, onClick = { run({ Account.cancelRep() }, {}) }) {
            Text(if (current.status == "approved") s.acc.repStepDown else s.acc.repCancel, color = Muted)
        }
        groupId.isNullOrBlank() -> Text(s.acc.repNoGroup, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        writing -> {
            InputField(comment, { comment = it.take(300) }, s.acc.repComment, singleLine = false, minLines = 3, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { writing = false }) { Text(s.cancel, color = Muted) }
                TextButton(enabled = !action.busy, onClick = {
                    run({ Account.requestRep(groupId, groupId, comment) }, {
                        writing = false
                        comment = ""
                        Toasts.show(s.acc.repPending(groupId))
                    })
                }) { Text(s.acc.repSend, color = Blue, fontWeight = FontWeight.SemiBold) }
            }
        }
        else -> TextButton(onClick = { writing = true }) { Text("${s.acc.repRequest} · $groupId", color = Blue) }
    }
    ErrorLine(action.error)
}

@Composable
private fun MyReviewsSheet(onClose: () -> Unit) {
    val s = S
    val bundle = LocalBundle.current
    var items by remember { mutableStateOf<List<MyReview>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var opened by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            items = Account.myReviews()
        } catch (failure: ApiFailure) {
            error = s.failureText(failure)
        }
    }
    BottomSheet(onDismiss = onClose) {
        Text(s.acc.myReviews, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        ErrorLine(error)
        if (items?.isEmpty() == true) Text(s.acc.noMyReviews, color = Muted, fontSize = 13.sp)
        items?.forEach { review ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Bg)
                    .clickable(enabled = bundle != null) { opened = review.teacher }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (review.hidden) "${review.teacher} · ${s.acc.hiddenReview}" else review.teacher,
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Text(
                        "★ ${review.overall} · ${s.acc.moment(review.updatedAt)} · 👍 ${review.likes} · 👎 ${review.dislikes}",
                        color = Muted,
                        fontSize = 12.sp,
                    )
                    if (review.text.isNotBlank()) Text(review.text, color = Ink, fontSize = 13.sp, maxLines = 2)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Muted)
            }
        }
    }
    val name = opened
    if (name != null && bundle != null) TeacherDialog(bundle, name, onClose = { opened = null })
}

@Composable
private fun DevicesSheet(onClose: () -> Unit) {
    val s = S
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<DeviceSession>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        try {
            items = Account.sessions()
        } catch (failure: ApiFailure) {
            error = s.failureText(failure)
        }
    }
    BottomSheet(onDismiss = onClose) {
        Text(s.acc.devices, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        ErrorLine(error)
        items?.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.device.ifBlank { s.acc.unknownDevice } + if (item.current) " · ${s.acc.thisDevice}" else "",
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Text(s.acc.deviceSeen(item.usedAt), color = Muted, fontSize = 12.sp)
                }
                if (!item.current) {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                Account.endSession(item.id)
                                attempt++
                            } catch (failure: ApiFailure) {
                                Toasts.show(s.failureText(failure))
                            }
                        }
                    }) { Text(s.more.signOut, color = Muted) }
                }
            }
        }
    }
}

@Composable
private fun ProviderLine(label: String, linked: Boolean, onUnlink: () -> Unit) {
    val s = S
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(
            if (linked) "$label · ${s.more.linked}" else label,
            color = if (linked) Ink else Muted,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        if (linked) {
            TextButton(onClick = onUnlink) { Text(s.more.unlink, color = Muted) }
        }
    }
}
