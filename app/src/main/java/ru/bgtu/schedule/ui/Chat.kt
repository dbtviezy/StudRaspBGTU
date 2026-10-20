package ru.bgtu.schedule.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.bgtu.schedule.data.Account
import ru.bgtu.schedule.data.AdminApi
import ru.bgtu.schedule.data.ApiFailure
import ru.bgtu.schedule.data.ChatInfo
import ru.bgtu.schedule.data.ChatMessage
import ru.bgtu.schedule.data.ChatStore
import ru.bgtu.schedule.data.NearbyChat
import ru.bgtu.schedule.data.RatingApi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val OFFLINE_CODES = setOf("offline", "server_error")

internal fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText("text", text))
}

private fun Copy.chatTitle(chat: ChatInfo): String = when {
    chat.isDirect -> chat.peerName ?: more.directChat
    chat.title.isNotBlank() -> chat.title
    chat.groupId != null -> "${more.groupChat} ${chat.groupId}"
    else -> more.groupChat
}

private fun parseInstant(iso: String?): java.time.ZonedDateTime? =
    iso?.let { runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()) }.getOrNull() }
        ?: iso?.let { runCatching { java.time.OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()) }.getOrNull() }

private fun timeOf(iso: String?): String = parseInstant(iso)?.let { "%02d:%02d".format(it.hour, it.minute) }.orEmpty()

private fun Copy.locale(): Locale = if (code == "en") Locale.ENGLISH else Locale("ru")

private fun Copy.shortWhen(iso: String?): String {
    val date = parseInstant(iso) ?: return ""
    if (date.toLocalDate() == LocalDate.now()) return timeOf(iso)
    return date.format(DateTimeFormatter.ofPattern("d MMM", locale())).trimEnd('.')
}

private fun Copy.dayLabel(iso: String?): String {
    val date = parseInstant(iso)?.toLocalDate() ?: return ""
    val today = LocalDate.now()
    return when (date) {
        today -> this.today
        today.minusDays(1) -> if (code == "en") "Yesterday" else "Вчера"
        else -> date.format(DateTimeFormatter.ofPattern("d MMMM", locale()))
    }
}

/** Chats tab: online chats (Supabase) and the nearby chat without internet. */
@Composable
fun ChatScreen(groupId: String, studentName: String) {
    val s = S
    var nearby by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Text(s.more.chatsTitle, color = Ink, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Row(Modifier.padding(top = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(s.more.chatOnline, !nearby) { nearby = false }
                FilterChip(s.more.chatNearby, nearby) { nearby = true }
            }
        }
        if (nearby) NearbyPane(studentName) else OnlineChats(groupId)
    }
}

private enum class ChatForm { CREATE, JOIN, DIRECT }

@Composable
private fun OnlineChats(groupId: String) {
    val s = S
    val account by Account.state.collectAsState()
    val version by ChatStore.version.collectAsState()
    val chats = remember(version) { ChatStore.cachedChats() }
    var enabled by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var signing by remember { mutableStateOf(false) }
    var form by remember { mutableStateOf<ChatForm?>(null) }
    var openedId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(AppSignals.openChat) {
        AppSignals.openChat?.let {
            openedId = it
            AppSignals.openChat = null
        }
    }
    LaunchedEffect(Unit) {
        enabled = try {
            Account.config().chat
        } catch (_: ApiFailure) {
            null
        }
    }
    LaunchedEffect(account.signedIn, enabled) {
        if (!account.signedIn || enabled == false) return@LaunchedEffect
        while (true) {
            error = try {
                ChatStore.refreshChats()
                ChatStore.flush()
                null
            } catch (failure: ApiFailure) {
                failure.code
            }
            delay(20_000)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            !account.signedIn -> item {
                SoftCard {
                    Text(s.more.chatsSignIn, color = Ink, fontSize = 14.sp)
                    PrimaryButton(s.more.signIn, onClick = { signing = true }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                }
            }
            enabled == false || error == "chat_not_configured" -> item { InfoBox(s.more.chatsOff) }
            else -> {
                if (error in OFFLINE_CODES) item { OfflineBar(rounded = true) }
                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(s.more.createGroupChat, false) { form = ChatForm.CREATE }
                        FilterChip(s.more.joinByCode, false) { form = ChatForm.JOIN }
                        FilterChip(s.more.newDirect, false) { form = ChatForm.DIRECT }
                    }
                }
                val shown = error
                if (shown != null && shown !in OFFLINE_CODES) item { ErrorLine(s.errorText(shown)) }
                if (chats.isEmpty()) item { Text(s.more.noChats, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(8.dp)) }
                items(chats, key = { it.id }) { chat -> ChatRow(chat) { openedId = chat.id } }
            }
        }
        item {
            Text(s.more.chatRetention, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }

    if (signing) SignInSheet(onClose = { signing = false })
    form?.let { kind ->
        ChatFormSheet(kind, groupId, onClose = { form = null }) { chat -> openedId = chat.id }
    }
    openedId?.let { id ->
        ChatView(id, onClose = { openedId = null }, onOpenChat = { openedId = it.id })
    }
}

@Composable
private fun InfoBox(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BlueSoft)
            .padding(14.dp),
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = Blue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Ink, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun OfflineBar(rounded: Boolean = false) {
    Text(
        S.more.offlineBanner,
        color = Ink,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (rounded) 14.dp else 0.dp))
            .background(Amber.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun ChatRow(chat: ChatInfo, onOpen: () -> Unit) {
    val s = S
    val title = s.chatTitle(chat)
    val preview = when {
        !chat.accepted -> "${chat.peerName.orEmpty()} ${s.more.chatRequest}"
        chat.lastBody != null -> (if (!chat.isDirect && chat.lastAuthor != null) "${chat.lastAuthor}: " else "") + censor(chat.lastBody)
        else -> s.more.noMessages
    }
    SoftCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BlueSoft),
                contentAlignment = Alignment.Center,
            ) {
                if (chat.isDirect) {
                    Text(title.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = Blue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                } else {
                    Icon(Icons.Filled.Groups, contentDescription = null, tint = Blue, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(s.shortWhen(chat.lastAt), color = Muted, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        preview,
                        color = if (chat.accepted) Muted else Blue,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (chat.unread > 0) {
                        Text(
                            s.unreadCount(chat.unread),
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Blue)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatFormSheet(kind: ChatForm, groupId: String, onClose: () -> Unit, onOpened: (ChatInfo) -> Unit) {
    val s = S
    val scope = rememberCoroutineScope()
    var value by remember { mutableStateOf(if (kind == ChatForm.CREATE) "${s.more.groupChat} $groupId" else "") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val title = when (kind) {
        ChatForm.CREATE -> s.more.createGroupChat
        ChatForm.JOIN -> s.more.joinByCode
        ChatForm.DIRECT -> s.more.newDirect
    }
    val hint = when (kind) {
        ChatForm.CREATE -> s.more.inviteShare
        ChatForm.JOIN -> s.more.inviteCodeHint
        ChatForm.DIRECT -> null
    }
    val label = when (kind) {
        ChatForm.CREATE -> s.more.chatTitleField
        ChatForm.JOIN -> s.more.inviteCode
        ChatForm.DIRECT -> s.more.directHint
    }
    val limit = when (kind) {
        ChatForm.CREATE -> 60
        ChatForm.JOIN -> 8
        ChatForm.DIRECT -> 24
    }
    BottomSheet(onDismiss = onClose) {
        Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (hint != null) Text(hint, color = Muted, fontSize = 13.sp)
        OutlinedTextField(
            value = value,
            onValueChange = { next -> value = (if (kind == ChatForm.JOIN) next.uppercase() else next).take(limit) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = if (kind == ChatForm.JOIN) KeyboardCapitalization.Characters else KeyboardCapitalization.Sentences,
            ),
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors(),
        )
        ErrorLine(error)
        Row {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) { Text(s.cancel, color = Muted) }
            TextButton(
                enabled = !busy && value.isNotBlank(),
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            val chat = when (kind) {
                                ChatForm.CREATE -> ChatStore.createGroupChat(groupId, value)
                                ChatForm.JOIN -> ChatStore.join(value)
                                ChatForm.DIRECT -> {
                                    val (id, name) = ChatStore.lookupUser(value)
                                    ChatStore.startDirect(id, name).also { Toasts.show(s.more.waitingAccept) }
                                }
                            }
                            onClose()
                            onOpened(chat)
                        } catch (failure: ApiFailure) {
                            error = s.errorText(failure.code)
                        } finally {
                            busy = false
                        }
                    }
                },
            ) {
                Text(if (kind == ChatForm.DIRECT) s.more.find else title, color = Blue, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ChatView(chatId: String, onClose: () -> Unit, onOpenChat: (ChatInfo) -> Unit) {
    val s = S
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account by Account.state.collectAsState()
    val adminToken by AdminApi.token.collectAsState()
    val version by ChatStore.version.collectAsState()
    val chat = remember(version) { ChatStore.cachedChats().firstOrNull { it.id == chatId } }
    val messages = remember(version, chatId) { ChatStore.cachedMessages(chatId) }
    val me = account.user?.id
    var text by rememberSaveable(chatId) { mutableStateOf("") }
    var offline by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf<ChatMessage?>(null) }
    var reporting by remember { mutableStateOf<ChatMessage?>(null) }
    var banning by remember { mutableStateOf<ChatMessage?>(null) }
    var leaving by remember { mutableStateOf(false) }
    var invite by remember { mutableStateOf(false) }
    var actions by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val lastId = messages.maxOfOrNull { it.id ?: 0L } ?: 0L

    LaunchedEffect(chatId) {
        ru.bgtu.schedule.notify.ChatNotify.cancelFor(context, chatId)
        try {
            ChatStore.resyncMessages(chatId)
            offline = false
        } catch (failure: ApiFailure) {
            offline = failure.code in OFFLINE_CODES
        }
        while (true) {
            delay(15_000)
            try {
                ChatStore.refreshMessages(chatId)
                ChatStore.flush()
                offline = false
                error = null
            } catch (failure: ApiFailure) {
                offline = failure.code in OFFLINE_CODES
                if (!offline) error = s.errorText(failure.code)
            }
        }
    }
    LaunchedEffect(lastId) {
        if (lastId > 0) ChatStore.markRead(chatId, lastId)
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size)
    }

    fun guarded(body: suspend () -> Unit) {
        scope.launch {
            try {
                body()
            } catch (failure: ApiFailure) {
                Toasts.show(s.errorText(failure.code))
            }
        }
    }

    val subtitle = when {
        chat == null -> ""
        chat.isDirect -> s.more.directChat
        else -> s.more.groupChat + (chat.groupId?.let { " · $it" } ?: "")
    }
    BareDialog(onDismiss = onClose, fullScreen = true) {
      Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Bg)
                .statusBarsPadding()
                .imePadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp)) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, tint = Ink)
                }
                Column(Modifier.weight(1f)) {
                    Text(chat?.let { s.chatTitle(it) }.orEmpty(), color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, color = Muted, fontSize = 12.sp, maxLines = 1)
                }
                if (chat != null && !chat.isDirect) {
                    IconButton(onClick = { sheet = "polls" }) {
                        Icon(Icons.Filled.Poll, contentDescription = s.more.polls, tint = Ink)
                    }
                    IconButton(onClick = { sheet = "tasks" }) {
                        Icon(Icons.Filled.Checklist, contentDescription = s.more.groupTasks, tint = Ink)
                    }
                }
                IconButton(onClick = { actions = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = s.more.moreActions, tint = Ink)
                }
            }
            if (chat != null && !chat.isDirect) {
                PinnedBar(chat, onUnpin = if (chat.isStaff) ({ guarded { ChatStore.pin(chatId, null) } }) else null)
            }
            if (offline) OfflineBar()
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            s.more.noMessages,
                            color = Muted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        )
                    }
                }
                items(messages.size, key = { index -> messages[index].clientId.ifBlank { messages[index].id.toString() } }) { index ->
                    val message = messages[index]
                    val day = s.dayLabel(message.createdAt)
                    if (index == 0 || s.dayLabel(messages[index - 1].createdAt) != day) {
                        Text(
                            day,
                            color = Muted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        )
                    }
                    Bubble(
                        message = message,
                        mine = message.userId == me || message.id == null,
                        showAuthor = chat?.isDirect == false,
                        onMenu = { menu = message },
                    )
                }
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        error?.let { ErrorLine(it) }
                        Text(s.more.chatRetention, color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            if (chat != null && chat.isDirect && !chat.accepted) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Card)
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp + LocalBottomInset.current),
                ) {
                    Text("${chat.peerName.orEmpty()} ${s.more.chatRequest}", color = Ink, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { guarded { ChatStore.leave(chatId); onClose() } }) { Text(s.more.decline, color = Red) }
                        TextButton(onClick = { guarded { ChatStore.accept(chatId) } }) { Text(s.more.accept, color = Blue) }
                    }
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Card)
                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp + LocalBottomInset.current),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(ChatStore.MAX_MESSAGE) },
                        modifier = Modifier.weight(1f).heightIn(max = 160.dp),
                        placeholder = { Text(s.more.messagePlaceholder) },
                        shape = RoundedCornerShape(20.dp),
                        colors = fieldColors(),
                        maxLines = 6,
                    )
                    IconButton(
                        enabled = text.isNotBlank(),
                        onClick = {
                            ChatStore.send(chatId, text)
                            text = ""
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = s.more.sendMessage, tint = if (text.isNotBlank()) Blue else Muted)
                    }
                }
            }
        }
        ToastHost(bottom = 96.dp)
      }
    }

    menu?.let { message ->
        val sent = message.id != null && !message.pending && !message.failed
        val own = message.userId == me
        BottomSheet(onDismiss = { menu = null }) {
            Text(message.body, color = Muted, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (message.failed) {
                MenuItem(s.retry) { ChatStore.retry(message.clientId); menu = null }
            }
            if (!sent) {
                MenuItem(s.delete, danger = true) { ChatStore.discard(message.clientId); menu = null }
            }
            if (sent) {
                MenuItem(s.more.copyText) {
                    copyToClipboard(context, message.body)
                    Toasts.show(s.more.copied)
                    menu = null
                }
            }
            if (sent && chat?.isStaff == true) {
                MenuItem(s.more.pin) {
                    menu = null
                    guarded { ChatStore.pin(chatId, message.id) }
                }
            }
            if (sent && own) {
                MenuItem(s.delete, danger = true) {
                    menu = null
                    guarded { ChatStore.deleteOwn(chatId, message.id!!) }
                }
            }
            if (sent && !own && chat?.isDirect == false) {
                MenuItem(s.more.newDirect) {
                    menu = null
                    guarded {
                        val direct = ChatStore.startDirect(message.userId, message.author)
                        Toasts.show(s.more.waitingAccept)
                        onOpenChat(direct)
                    }
                }
            }
            if (sent && !own) {
                MenuItem(s.more.report, danger = true) { reporting = message; menu = null }
            }
            if (sent && adminToken != null) {
                MenuItem("${s.more.moderation}: ${s.more.deleteForever}", danger = true) {
                    menu = null
                    guarded {
                        AdminApi.action("message.delete", mapOf("id" to message.id))
                        ChatStore.forgetMessage(chatId, message.id!!)
                        Toasts.show(s.more.adminDone)
                    }
                }
                if (!own) {
                    MenuItem("${s.more.moderation}: ${s.more.ban}", danger = true) { banning = message; menu = null }
                }
            }
        }
    }
    reporting?.let { message ->
        ReportSheet(
            onDismiss = { reporting = null },
            onSend = { reason -> RatingApi.report(context, "message", message.id ?: 0L, reason, Account.current.token) },
        )
    }
    banning?.let { message ->
        BanSheet(withDelete = true, onDismiss = { banning = null }) { days, reason, extra ->
            AdminApi.action("ban.message", mapOf("id" to message.id, "days" to days, "reason" to reason, "deleteMessage" to extra))
            if (extra) message.id?.let { ChatStore.forgetMessage(chatId, it) }
        }
    }
    if (actions) {
        BottomSheet(onDismiss = { actions = false }) {
            if (chat != null && !chat.isDirect) {
                MenuItem(s.more.members) { sheet = "members"; actions = false }
                if (chat.inviteCode != null) MenuItem(s.more.inviteCode) { invite = true; actions = false }
            }
            MenuItem(s.more.leaveChat, danger = true) { leaving = true; actions = false }
        }
    }
    if (chat != null) {
        when (sheet) {
            "members" -> MembersSheet(chat) { sheet = null }
            "polls" -> PollsSheet(chat) { sheet = null }
            "tasks" -> GroupTasksSheet(chat) { sheet = null }
        }
    }
    if (invite && chat != null) InviteSheet(chat) { invite = false }
    if (leaving) {
        AlertDialog(
            onDismissRequest = { leaving = false },
            containerColor = Card,
            titleContentColor = Ink,
            textContentColor = Muted,
            title = { Text(s.more.leaveChat) },
            text = { Text(s.more.leaveChatText) },
            dismissButton = { TextButton(onClick = { leaving = false }) { Text(s.cancel, color = Muted) } },
            confirmButton = {
                TextButton(onClick = {
                    leaving = false
                    guarded {
                        ChatStore.leave(chatId)
                        onClose()
                    }
                }) { Text(s.more.leaveChat, color = Red) }
            },
        )
    }
}

@Composable
private fun MenuItem(text: String, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        color = if (danger) Red else Ink,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Bubble(message: ChatMessage, mine: Boolean, showAuthor: Boolean, onMenu: () -> Unit) {
    val s = S
    val meta = when {
        message.pending -> s.more.pendingMessage
        message.failed -> s.more.failedMessage
        else -> timeOf(message.createdAt)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (mine) 18.dp else 6.dp,
                        bottomEnd = if (mine) 6.dp else 18.dp,
                    ),
                )
                .background(
                    when {
                        message.failed -> RedSoft
                        mine -> Blue
                        else -> Card
                    },
                )
                .combinedClickable(onClick = { if (!message.pending || message.failed) onMenu() }, onLongClick = onMenu)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            val ink = if (mine && !message.failed) androidx.compose.ui.graphics.Color.White else Ink
            if (showAuthor && !mine) {
                Text(message.author, color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
            Text(if (mine) message.body else censor(message.body), color = ink, fontSize = 15.sp, lineHeight = 20.sp)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.End).padding(top = 2.dp)) {
                if (message.pending) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = ink.copy(alpha = 0.7f), modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(3.dp))
                }
                Text(meta, color = if (message.failed) Red else ink.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun InviteSheet(chat: ChatInfo, onClose: () -> Unit) {
    val s = S
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf(chat.inviteCode.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    BottomSheet(onDismiss = onClose) {
        Text(s.more.inviteCode, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            code.ifBlank { "—" },
            color = Blue,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Text(s.more.inviteShare, color = Muted, fontSize = 13.sp)
        ErrorLine(error)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (chat.isOwner) {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            code = ChatStore.regenerateInvite(chat.id)
                        } catch (failure: ApiFailure) {
                            error = s.errorText(failure.code)
                        }
                    }
                }) { Text(s.more.newInviteCode, color = Muted) }
            }
            TextButton(onClick = {
                copyToClipboard(context, code)
                Toasts.show(s.more.codeCopied)
            }) { Text(s.more.copyCode, color = Blue) }
        }
    }
}

@Composable
private fun NearbyPane(studentName: String) {
    val s = S
    val context = LocalContext.current
    val state by NearbyChat.state.collectAsState()
    var text by rememberSaveable { mutableStateOf("") }
    var denied by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val name = Account.current.user?.name ?: studentName.ifBlank { s.studentFallback }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) {
            denied = false
            NearbyChat.start(context, name)
        } else {
            denied = true
        }
    }

    fun start() {
        val missing = NearbyChat.permissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) NearbyChat.start(context, name) else launcher.launch(missing.toTypedArray())
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.size)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                SoftCard {
                    Text(s.more.nearbyTitle, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text(s.more.nearbyHint, color = Muted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp))
                    if (state.running) {
                        Text(
                            if (state.peers.isEmpty()) s.more.nearbySearching else s.more.nearbyPeers(state.peers.size),
                            color = Blue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        if (state.peers.isNotEmpty()) {
                            Text(state.peers.values.joinToString(", "), color = Muted, fontSize = 12.sp)
                        }
                        OutlineButton(s.more.nearbyStop, onClick = { NearbyChat.stop() }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    } else {
                        if (denied) ErrorLine(s.more.nearbyNoPermission)
                        if (state.error != null) ErrorLine(s.more.nearbyFailed)
                        PrimaryButton(s.more.nearbyStart, onClick = { start() }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    }
                }
            }
            if (state.messages.isEmpty()) {
                item {
                    Text(s.more.nearbyEmpty, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                }
            }
            items(state.messages, key = { it.id }) { message ->
                Bubble(
                    message = ChatMessage(
                        id = 0L,
                        chatId = "nearby",
                        userId = if (message.mine) "me" else message.author,
                        author = message.author,
                        body = message.text,
                        createdAt = Instant.ofEpochMilli(message.at).toString(),
                        clientId = message.id,
                        status = "sent",
                    ),
                    mine = message.mine,
                    showAuthor = true,
                    onMenu = {
                        copyToClipboard(context, message.text)
                        Toasts.show(s.more.copied)
                    },
                )
            }
        }
        if (state.running) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Card)
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(NearbyChat.MAX_TEXT) },
                    modifier = Modifier.weight(1f).heightIn(max = 140.dp),
                    placeholder = { Text(s.more.messagePlaceholder) },
                    shape = RoundedCornerShape(20.dp),
                    colors = fieldColors(),
                    maxLines = 5,
                )
                IconButton(
                    enabled = text.isNotBlank() && state.peers.isNotEmpty(),
                    onClick = {
                        NearbyChat.send(text)
                        text = ""
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = s.more.sendMessage,
                        tint = if (text.isNotBlank() && state.peers.isNotEmpty()) Blue else Muted,
                    )
                }
            }
        }
    }
}
