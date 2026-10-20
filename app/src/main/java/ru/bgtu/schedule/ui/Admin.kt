package ru.bgtu.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.bgtu.schedule.data.AdminApi
import ru.bgtu.schedule.data.Announcement
import ru.bgtu.schedule.data.Announcements
import ru.bgtu.schedule.data.ApiFailure

private fun stamp(value: String?): String = value?.take(16)?.replace('T', ' ').orEmpty()

/** Ban form: days (0 = forever), reason, and an optional extra checkbox. */
@Composable
fun BanSheet(
    withHideAll: Boolean = false,
    withDelete: Boolean = false,
    onDismiss: () -> Unit,
    onBan: suspend (days: Int?, reason: String, extra: Boolean) -> Unit,
) {
    val s = S
    val scope = rememberCoroutineScope()
    var days by remember { mutableStateOf("7") }
    var reason by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    BottomSheet(onDismiss) {
        Text(s.more.ban, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        InputField(days, { value -> days = value.filter { it.isDigit() }.take(4) }, s.more.banDays, keyboard = KeyboardType.Number)
        InputField(reason, { reason = it.take(200) }, s.more.banReason)
        if (withHideAll || withDelete) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = extra, onCheckedChange = { extra = it }, colors = CheckboxDefaults.colors(checkedColor = Red))
                Text(if (withHideAll) s.more.hideAllReviews else s.more.deleteMessageToo, color = Ink, fontSize = 14.sp)
            }
        }
        ErrorLine(error)
        Row {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text(s.cancel, color = Muted) }
            TextButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            onBan(days.toIntOrNull()?.takeIf { it > 0 }, reason.trim(), extra)
                            Toasts.show(s.more.adminDone)
                            onDismiss()
                        } catch (failure: ApiFailure) {
                            error = s.errorText(failure.code)
                        } finally {
                            busy = false
                        }
                    }
                },
            ) { Text(s.more.ban, color = Red, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** Team announcements on the home screen; each can be closed and stays closed. */
@Composable
fun AnnouncementsBanner() {
    val context = LocalContext.current
    val s = S
    var items by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    LaunchedEffect(Unit) {
        val dismissed = Announcements.dismissed(context)
        items = Announcements.load().filter { it.id.toString() !in dismissed }
    }
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 12.dp)) {
        items.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BlueSoft)
                    .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Filled.Campaign, contentDescription = null, tint = Blue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.more.announcement, color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(item.text, color = Ink, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 2.dp))
                }
                IconButton(onClick = {
                    Announcements.dismiss(context, item.id)
                    items = items - item
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = s.close, tint = Muted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private enum class AdminTab { STATS, REPORTS, REVIEWS, MESSAGES, BANS, ANNOUNCEMENTS, USERS }

/** Runs a moderation call, shows "done" or the error, then reloads the list. */
@Composable
private fun rememberModerate(onDone: () -> Unit): (suspend () -> Unit) -> Unit {
    val scope = rememberCoroutineScope()
    val s = S
    return { body ->
        scope.launch {
            try {
                body()
                Toasts.show(s.more.adminDone)
                onDone()
            } catch (failure: ApiFailure) {
                Toasts.show(s.errorText(failure.code))
            }
        }
    }
}

/** Loads a list for the current tab; shows a spinner, the error or the content. */
@Composable
private fun <T> Loaded(
    key: Any,
    load: suspend () -> T,
    content: @Composable (T) -> Unit,
) {
    val s = S
    var value by remember(key) { mutableStateOf<T?>(null) }
    var error by remember(key) { mutableStateOf<String?>(null) }
    LaunchedEffect(key) {
        try {
            value = load()
        } catch (failure: ApiFailure) {
            error = s.errorText(failure.code)
        }
    }
    val current = value
    when {
        error != null -> Text(error.orEmpty(), color = Red, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
        current == null -> Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator(color = Blue, modifier = Modifier.size(28.dp))
        }
        else -> content(current)
    }
}

@Composable
fun AdminScreen(onClose: () -> Unit) {
    val s = S
    val token by AdminApi.token.collectAsState()
    var tab by remember { mutableStateOf(AdminTab.STATS) }
    var reload by remember { mutableIntStateOf(0) }
    FullPage(
        onClose = onClose,
        title = s.more.adminPanel,
        actions = {
            if (token != null) {
                IconButton(onClick = { reload++ }) { Icon(Icons.Filled.Refresh, contentDescription = null, tint = Ink) }
            }
        },
    ) {
        if (token == null) {
            AdminSignIn()
            return@FullPage
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AdminTab.entries.forEach { item ->
                val label = when (item) {
                    AdminTab.STATS -> s.more.adminStats
                    AdminTab.REPORTS -> s.more.adminReports
                    AdminTab.REVIEWS -> s.more.adminReviews
                    AdminTab.MESSAGES -> s.more.adminMessages
                    AdminTab.BANS -> s.more.adminBans
                    AdminTab.ANNOUNCEMENTS -> s.more.adminAnnouncements
                    AdminTab.USERS -> s.more.adminUsers
                }
                FilterChip(label, tab == item) { tab = item }
            }
        }
        val moderate = rememberModerate { reload++ }
        Column(Modifier.fillMaxSize()) {
            when (tab) {
                AdminTab.STATS -> StatsTab(reload)
                AdminTab.REPORTS -> ReportsTab(reload, moderate)
                AdminTab.REVIEWS -> ReviewsTab(reload, moderate)
                AdminTab.MESSAGES -> MessagesTab(reload, moderate)
                AdminTab.BANS -> BansTab(reload, moderate)
                AdminTab.ANNOUNCEMENTS -> AnnouncementsTab(reload, moderate)
                AdminTab.USERS -> UsersTab(reload, moderate)
            }
        }
    }
}

@Composable
private fun AdminSignIn() {
    val s = S
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(s.more.adminHint, color = Muted, fontSize = 14.sp)
        InputField(password, { password = it.take(200) }, s.more.adminPassword, password = true)
        ErrorLine(error)
        PrimaryButton(
            s.more.signIn,
            enabled = !busy && password.isNotEmpty(),
            onClick = {
                busy = true
                error = null
                scope.launch {
                    try {
                        AdminApi.signIn(password)
                        password = ""
                    } catch (failure: ApiFailure) {
                        error = s.errorText(failure.code)
                    } finally {
                        busy = false
                    }
                }
            },
        )
    }
}

@Composable
private fun AdminList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp + LocalBottomInset.current),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun EmptyLine() {
    Text(S.more.adminEmpty, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
private fun StatsTab(reload: Int) {
    val s = S
    Loaded(reload, { AdminApi.stats() }) { stats ->
        AdminList {
            item {
                SoftCard {
                    DetailRow(s.more.statUsers, stats.users.toString())
                    DetailRow(s.more.statActive, stats.activeUsers.toString())
                    DetailRow(s.more.statReviews, stats.reviews.toString())
                    DetailRow(s.more.statHidden, stats.hiddenReviews.toString())
                    DetailRow(s.more.statNewReviews, stats.newReviews.toString())
                    DetailRow(s.more.statTeachers, stats.ratedTeachers.toString())
                    DetailRow(s.more.statVotes, stats.votes.toString())
                    DetailRow(s.more.statReports, stats.openReports.toString())
                    DetailRow(s.more.statBans, stats.activeBans.toString())
                    DetailRow(s.more.statAnnouncements, stats.announcements.toString())
                    if (stats.chatEnabled) {
                        DetailRow(s.more.statChats, stats.chats?.toString() ?: "—")
                        DetailRow(s.more.statMessages, stats.messages?.toString() ?: "—")
                    } else {
                        Text(s.more.chatsNotConnected, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsTab(reload: Int, moderate: (suspend () -> Unit) -> Unit) {
    val s = S
    Loaded(reload, { AdminApi.reports() }) { items ->
        AdminList {
            if (items.isEmpty()) item { EmptyLine() }
            items(items, key = { "${it.kind}:${it.target}" }) { report ->
                SoftCard {
                    Text(
                        "${if (report.kind == "review") s.more.reportReview else s.more.reportMessage} #${report.target} · ${report.count} ${s.more.reportsLabel}",
                        color = Muted,
                        fontSize = 12.sp,
                    )
                    Text(report.snapshot.ifBlank { "—" }, color = Ink, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    report.reasons.forEach { reason ->
                        Text("• $reason", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    Text(stamp(report.lastAt), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    Row {
                        val fields = mapOf("kind" to report.kind, "target" to report.target)
                        TextButton(onClick = { moderate { AdminApi.action("report.accept", fields) } }) {
                            Text(s.more.acceptReport, color = Red)
                        }
                        TextButton(onClick = { moderate { AdminApi.action("report.dismiss", fields) } }) {
                            Text(s.more.dismissReport, color = Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewsTab(reload: Int, moderate: (suspend () -> Unit) -> Unit) {
    val s = S
    var query by remember { mutableStateOf("") }
    var applied by remember { mutableStateOf("") }
    var banning by remember { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<Long?>(null) }
    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        InputField(query, { query = it.take(80) }, s.more.adminSearch, modifier = Modifier.weight(1f))
        TextButton(onClick = { applied = query.trim() }) { Text(s.more.find, color = Blue) }
    }
    Loaded(reload to applied, { AdminApi.reviews(applied) }) { items ->
        AdminList {
            if (items.isEmpty()) item { EmptyLine() }
            items(items, key = { it.id }) { review ->
                SoftCard {
                    Text(
                        "${review.teacher} · ${review.overall}★" + if (review.hidden) " · ${s.more.hiddenMark}" else "",
                        color = Muted,
                        fontSize = 12.sp,
                    )
                    Text(review.text.ifBlank { "—" }, color = Ink, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(
                        "👍 ${review.likes}  👎 ${review.dislikes}  ⚑ ${review.reports} · ${stamp(review.updatedAt)}",
                        color = Muted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row {
                        if (review.hidden) {
                            TextButton(onClick = { moderate { AdminApi.action("review.unhide", mapOf("id" to review.id)) } }) {
                                Text(s.more.unhide, color = Blue)
                            }
                        } else {
                            TextButton(onClick = { moderate { AdminApi.action("review.hide", mapOf("id" to review.id)) } }) {
                                Text(s.more.hide, color = Blue)
                            }
                        }
                        TextButton(onClick = { deleting = review.id }) { Text(s.more.deleteForever, color = Red) }
                        TextButton(onClick = { banning = review.id }) { Text(s.more.ban, color = Red) }
                    }
                }
            }
        }
    }
    banning?.let { id ->
        BanSheet(withHideAll = true, onDismiss = { banning = null }) { days, reason, extra ->
            AdminApi.action("ban.review", mapOf("id" to id, "days" to days, "reason" to reason, "hideAll" to extra))
        }
    }
    deleting?.let { id ->
        ConfirmDelete(onDismiss = { deleting = null }) {
            moderate { AdminApi.action("review.delete", mapOf("id" to id)) }
        }
    }
}

@Composable
private fun MessagesTab(reload: Int, moderate: (suspend () -> Unit) -> Unit) {
    val s = S
    var banning by remember { mutableStateOf<Long?>(null) }
    Loaded(reload, { AdminApi.messages() }) { items ->
        AdminList {
            if (items.isEmpty()) item { EmptyLine() }
            items(items, key = { it.id }) { message ->
                SoftCard {
                    Text("${message.author} · ${message.chat}", color = Muted, fontSize = 12.sp)
                    Text(message.body, color = Ink, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(stamp(message.createdAt), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    Row {
                        TextButton(onClick = { moderate { AdminApi.action("message.delete", mapOf("id" to message.id)) } }) {
                            Text(s.more.deleteForever, color = Red)
                        }
                        TextButton(onClick = { banning = message.id }) { Text(s.more.ban, color = Red) }
                    }
                }
            }
        }
    }
    banning?.let { id ->
        BanSheet(withDelete = true, onDismiss = { banning = null }) { days, reason, extra ->
            AdminApi.action("ban.message", mapOf("id" to id, "days" to days, "reason" to reason, "deleteMessage" to extra))
        }
    }
}

@Composable
private fun BansTab(reload: Int, moderate: (suspend () -> Unit) -> Unit) {
    val s = S
    Loaded(reload, { AdminApi.bans() }) { items ->
        AdminList {
            if (items.isEmpty()) item { EmptyLine() }
            items(items, key = { it.id }) { ban ->
                SoftCard {
                    val who = when {
                        ban.device -> s.more.deviceBan
                        ban.login != null -> "${ban.name.orEmpty()} @${ban.login}"
                        else -> ban.name ?: ban.userId.orEmpty()
                    }
                    Text(who, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    if (ban.reason.isNotBlank()) Text(ban.reason, color = Ink, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                    Text(
                        "${stamp(ban.createdAt)} · ${ban.until?.let { "${s.more.until} ${stamp(it)}" } ?: s.more.forever}",
                        color = Muted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    TextButton(onClick = { moderate { AdminApi.action("ban.remove", mapOf("id" to ban.id)) } }) {
                        Text(s.more.unban, color = Blue)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementsTab(reload: Int, moderate: (suspend () -> Unit) -> Unit) {
    val s = S
    var text by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("7") }
    Loaded(reload, { AdminApi.announcements() }) { items ->
        AdminList {
            item {
                SoftCard {
                    InputField(text, { text = it.take(500) }, s.more.announceText, singleLine = false, minLines = 3)
                    InputField(days, { value -> days = value.filter { it.isDigit() }.take(4) }, s.more.announceDays, keyboard = KeyboardType.Number)
                    PrimaryButton(
                        s.more.publish,
                        enabled = text.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        onClick = {
                            val body = text.trim()
                            val length = days.toIntOrNull()?.takeIf { it > 0 }
                            moderate {
                                AdminApi.action("announce.create", mapOf("text" to body, "days" to length))
                                text = ""
                            }
                        },
                    )
                }
            }
            if (items.isEmpty()) item { EmptyLine() }
            items(items, key = { it.id }) { item ->
                SoftCard {
                    Text(
                        "${stamp(item.createdAt)} · ${if (item.active) s.more.activeMark else s.more.hiddenMark}" +
                            (item.until?.let { " · ${s.more.until} ${stamp(it)}" } ?: ""),
                        color = Muted,
                        fontSize = 12.sp,
                    )
                    Text(item.text, color = Ink, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    if (item.active) {
                        TextButton(onClick = { moderate { AdminApi.action("announce.remove", mapOf("id" to item.id)) } }) {
                            Text(s.more.takeDown, color = Red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsersTab(reload: Int, moderate: (suspend () -> Unit) -> Unit) {
    val s = S
    var query by remember { mutableStateOf("") }
    var applied by remember { mutableStateOf("") }
    var banning by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<String?>(null) }
    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        InputField(query, { query = it.take(80) }, s.more.adminSearch, modifier = Modifier.weight(1f))
        TextButton(onClick = { applied = query.trim() }) { Text(s.more.find, color = Blue) }
    }
    Loaded(reload to applied, { AdminApi.users(applied) }) { items ->
        AdminList {
            if (items.isEmpty()) item { EmptyLine() }
            items(items, key = { it.id }) { user ->
                SoftCard {
                    Text(
                        user.name + (user.login?.let { " @$it" } ?: ""),
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    val ways = listOfNotNull(
                        user.login?.let { s.more.passwordField },
                        "Telegram".takeIf { user.telegram },
                        "Google".takeIf { user.google },
                    ).joinToString(", ")
                    Text(
                        "ID ${user.id.replace("-", "").take(8).uppercase()} · $ways" + if (user.banned) " · ${s.more.ban}" else "",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text("${stamp(user.createdAt)} → ${stamp(user.seenAt)}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                    Row {
                        TextButton(onClick = { banning = user.id }) { Text(s.more.ban, color = Red) }
                        TextButton(onClick = { deleting = user.id }) { Text(s.more.deleteForever, color = Red) }
                    }
                }
            }
        }
    }
    banning?.let { id ->
        BanSheet(onDismiss = { banning = null }) { days, reason, _ ->
            AdminApi.action("ban.user", mapOf("userId" to id, "days" to days, "reason" to reason))
        }
    }
    deleting?.let { id ->
        ConfirmDelete(onDismiss = { deleting = null }) {
            moderate { AdminApi.action("user.delete", mapOf("userId" to id)) }
        }
    }
}

@Composable
private fun ConfirmDelete(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val s = S
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        titleContentColor = Ink,
        textContentColor = Muted,
        title = { Text(s.more.confirmDelete) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Muted) } },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) { Text(s.more.deleteForever, color = Red) }
        },
    )
}
