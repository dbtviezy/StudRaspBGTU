package ru.bgtu.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.bgtu.schedule.data.Account
import ru.bgtu.schedule.data.ApiFailure
import ru.bgtu.schedule.data.ChatInfo
import ru.bgtu.schedule.data.ChatMember
import ru.bgtu.schedule.data.ChatPoll
import ru.bgtu.schedule.data.ChatStore
import ru.bgtu.schedule.data.GroupTask
import ru.bgtu.schedule.data.Profanity
import ru.bgtu.schedule.data.TodoItem
import ru.bgtu.schedule.data.TodoStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** App-wide switches read from many screens. */
object AppSignals {
    var hideProfanity by mutableStateOf(false)

    /** Chat to open after a tap on a message notification. */
    var openChat by mutableStateOf<String?>(null)

    /** Opens the sign-in sheet from anywhere, e.g. when an action needs an account. */
    var signIn by mutableStateOf(false)
}

/** A small «староста» pill next to a name. */
@Composable
internal fun RepBadge() {
    Text(
        S.more.roleRep,
        color = Blue,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(BlueSoft)
            .padding(horizontal = 7.dp, vertical = 1.dp),
    )
}

/** Masks swear words in other people's text when the user turned the filter on. */
fun censor(text: String): String = Profanity.maskIf(AppSignals.hideProfanity, text)

@Composable
internal fun PinnedBar(chat: ChatInfo, onUnpin: (() -> Unit)?) {
    val body = chat.pinnedBody ?: return
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(Card)
            .clickable { open = !open }
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.PushPin, contentDescription = null, tint = Blue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                S.more.pinned + (chat.pinnedAuthor?.let { " · $it" } ?: ""),
                color = Blue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                censor(body),
                color = Ink,
                fontSize = 13.sp,
                maxLines = if (open) 12 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onUnpin != null) {
            IconButton(onClick = onUnpin) {
                Icon(Icons.Filled.Close, contentDescription = S.more.unpin, tint = Muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private suspend fun guardedCall(s: Copy, body: suspend () -> Unit) {
    try {
        body()
    } catch (failure: ApiFailure) {
        Toasts.show(s.failureText(failure))
    }
}

@Composable
internal fun MembersSheet(chat: ChatInfo, onClose: () -> Unit) {
    val s = S
    val scope = rememberCoroutineScope()
    val me = Account.current.user?.id
    var members by remember { mutableStateOf<List<ChatMember>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        try {
            members = ChatStore.members(chat.id, chat.groupId)
        } catch (failure: ApiFailure) {
            error = s.failureText(failure)
        }
    }

    BottomSheet(onDismiss = onClose) {
        Text(s.more.members, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(s.more.helpersHint, color = Muted, fontSize = 13.sp)
        ErrorLine(error)
        members?.forEach { member ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(BlueSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(member.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = Blue, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (member.userId == me) "${member.name} (${s.more.you})" else member.name,
                            color = Ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (member.rep) RepBadge()
                    }
                    if (member.role != "member") {
                        Text(if (member.role == "owner") s.more.roleOwner else s.more.roleHelper, color = Blue, fontSize = 12.sp)
                    }
                }
                if (chat.isOwner && member.role != "owner") {
                    val helper = member.role == "helper"
                    TextButton(onClick = {
                        scope.launch {
                            guardedCall(s) {
                                ChatStore.setRole(chat.id, member.userId, if (helper) "member" else "helper")
                                attempt++
                            }
                        }
                    }) {
                        Text(if (helper) s.more.removeHelper else s.more.makeHelper, color = if (helper) Muted else Blue, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PollCard(chat: ChatInfo, poll: ChatPoll) {
    val s = S
    val scope = rememberCoroutineScope()
    val picked = remember(poll.id, poll.mine) { mutableStateListOf<Int>().apply { addAll(poll.mine) } }
    var busy by remember { mutableStateOf(false) }
    val total = poll.counts.sum()
    val showResults = poll.mine.isNotEmpty() || poll.closed

    fun run(body: suspend () -> Unit) {
        busy = true
        scope.launch {
            guardedCall(s, body)
            busy = false
        }
    }

    SoftCard {
        Text(censor(poll.question), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(
            listOf(poll.author, s.more.pollVotes(poll.voters), if (poll.closed) s.more.pollClosed else if (poll.multiple) s.more.pollMultiple else "")
                .filter { it.isNotBlank() }
                .joinToString(" · "),
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        poll.options.forEachIndexed { index, option ->
            val count = poll.counts.getOrElse(index) { 0 }
            val share = if (total > 0) count.toFloat() / total else 0f
            val on = index in picked
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Bg)
                    .then(if (on) Modifier.border(1.5.dp, Blue, RoundedCornerShape(12.dp)) else Modifier)
                    .clickable(enabled = !poll.closed && !busy) {
                        if (poll.multiple) {
                            if (on) picked.remove(index) else picked.add(index)
                        } else {
                            val next = if (index in poll.mine) emptyList() else listOf(index)
                            run { ChatStore.vote(chat.id, poll.id, next) }
                        }
                    },
            ) {
                if (showResults) {
                    Box(Modifier.matchParentSize()) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(share.coerceIn(0f, 1f)).background(BlueSoft))
                    }
                }
                Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (on) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Blue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(censor(option), color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (showResults) Text("${(share * 100).toInt()}%", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            val changed = poll.multiple && picked.sorted() != poll.mine.sorted()
            if (changed && !poll.closed) {
                TextButton(enabled = !busy, onClick = { run { ChatStore.vote(chat.id, poll.id, picked.toList()) } }) {
                    Text(s.more.vote, color = Blue, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.weight(1f))
            if (chat.isStaff && !poll.closed) {
                TextButton(enabled = !busy, onClick = { run { ChatStore.closePoll(chat.id, poll.id, false) } }) {
                    Text(s.more.pollClose, color = Muted, fontSize = 13.sp)
                }
            }
            if (chat.isStaff) {
                TextButton(enabled = !busy, onClick = { run { ChatStore.closePoll(chat.id, poll.id, true) } }) {
                    Text(s.more.pollDelete, color = Red, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun NewPollForm(chat: ChatInfo, onDone: () -> Unit) {
    val s = S
    val scope = rememberCoroutineScope()
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    var multiple by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    SoftCard {
        InputField(question, { question = it.take(200) }, s.more.pollQuestion)
        options.forEachIndexed { index, option ->
            InputField(option, { options[index] = it.take(100) }, s.more.pollOption(index + 1), modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        if (options.size < 8) {
            TextButton(onClick = { options.add("") }) { Text(s.more.addOption, color = Blue) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { multiple = !multiple }) {
            Checkbox(
                checked = multiple,
                onCheckedChange = { multiple = it },
                colors = CheckboxDefaults.colors(checkedColor = Blue, uncheckedColor = Muted, checkmarkColor = Color.White),
            )
            Text(s.more.pollMultiple, color = Ink, fontSize = 14.sp)
        }
        ErrorLine(error)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) { Text(s.cancel, color = Muted) }
            TextButton(enabled = !busy, onClick = {
                val filled = options.map { it.trim() }.filter { it.isNotEmpty() }
                if (question.isBlank() || filled.size < 2) {
                    error = s.more.pollNeedOptions
                    return@TextButton
                }
                busy = true
                error = null
                scope.launch {
                    try {
                        ChatStore.createPoll(chat.id, question.trim(), filled, multiple)
                        onDone()
                    } catch (failure: ApiFailure) {
                        error = s.failureText(failure)
                    } finally {
                        busy = false
                    }
                }
            }) { Text(s.more.pollCreate, color = Blue, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
internal fun PollsSheet(chat: ChatInfo, onClose: () -> Unit) {
    val s = S
    val version by ChatStore.version.collectAsState()
    val polls = remember(version) { ChatStore.cachedPolls(chat.id) }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chat.id) {
        error = try {
            ChatStore.refreshPolls(chat.id)
            null
        } catch (failure: ApiFailure) {
            s.failureText(failure)
        }
    }

    BottomSheet(onDismiss = onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.more.polls, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
            if (chat.isStaff && !creating) {
                TextButton(onClick = { creating = true }) { Text(s.more.newPoll, color = Blue) }
            }
        }
        ErrorLine(error)
        if (creating) NewPollForm(chat) { creating = false }
        if (polls.isEmpty() && !creating) Text(s.more.noPolls, color = Muted, fontSize = 14.sp)
        polls.forEach { poll -> PollCard(chat, poll) }
    }
}

private fun dueText(copy: Copy, task: GroupTask): String {
    val date = task.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return copy.more.noDate
    val base = copy.formatLongDateCap(date)
    return if (task.dueTime.isNullOrBlank()) base else "$base · ${task.dueTime}"
}

@Composable
internal fun GroupTaskCard(task: GroupTask, chatTitle: String? = null, onAdded: () -> Unit = {}) {
    val s = S
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { TodoStore(context) }
    var added by remember(task.id) { mutableStateOf(store.all().any { it.id == task.todoId }) }
    val chat = remember(task.chatId) { ChatStore.cachedChats().firstOrNull { it.id == task.chatId } }
    val overdue = task.dueDate != null && task.dueDate < LocalDate.now().toString()

    SoftCard {
        Text(censor(task.title), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(dueText(s, task), color = if (overdue) Red else Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        if (task.note.isNotBlank()) {
            Text(censor(task.note), color = Ink, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Text(
            listOfNotNull(task.author.ifBlank { null }, chatTitle?.let { s.more.fromChat(it) }).joinToString(" · "),
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (added) {
                Text(s.more.inMine, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
            } else {
                TextButton(onClick = {
                    store.upsert(
                        TodoItem(
                            id = task.todoId,
                            title = task.title,
                            deadlineDate = task.dueDate,
                            deadlineTime = if (task.dueDate != null) task.dueTime else null,
                            lessonKey = null,
                            lessonLabel = null,
                            done = false,
                        ),
                    )
                    added = true
                    Toasts.show(s.more.addedToMine)
                    onAdded()
                }) { Text(s.more.addToMine, color = Blue, fontWeight = FontWeight.SemiBold) }
            }
            Spacer(Modifier.weight(1f))
            if (chat?.isStaff == true) {
                IconButton(onClick = { scope.launch { guardedCall(s) { ChatStore.deleteGroupTask(task.id) } } }) {
                    Icon(Icons.Filled.Delete, contentDescription = s.delete, tint = Muted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewTaskForm(chat: ChatInfo, onDone: () -> Unit) {
    val s = S
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<String?>(null) }
    var time by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val dateState = rememberDatePickerState()

    SoftCard {
        InputField(title, { title = it.take(200) }, s.more.groupTaskTitle)
        InputField(note, { note = it.take(500) }, s.more.groupTaskNote, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), singleLine = false, minLines = 2)
        Text(s.more.pickDate, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        Text(
            date?.let { s.formatLongDateCap(LocalDate.parse(it)) } ?: s.more.noDate,
            color = Ink,
            fontWeight = FontWeight.Medium,
        )
        Row {
            TextButton(onClick = { picking = true }) { Text(s.pickDate, color = Blue) }
            if (date != null) TextButton(onClick = { date = null }) { Text(s.clearDeadline, color = Muted) }
        }
        if (date != null) {
            InputField(time, { time = it.take(5) }, s.timeOptional, hint = s.timeHint)
        }
        ErrorLine(error)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) { Text(s.cancel, color = Muted) }
            TextButton(enabled = !busy && title.isNotBlank(), onClick = {
                val cleanTime = if (time.isBlank()) null else normalizeTime(time)
                if (time.isNotBlank() && cleanTime == null) {
                    error = s.badTime
                    return@TextButton
                }
                busy = true
                error = null
                scope.launch {
                    try {
                        ChatStore.addGroupTask(chat.id, title.trim(), note.trim(), date, cleanTime)
                        onDone()
                    } catch (failure: ApiFailure) {
                        error = s.failureText(failure)
                    } finally {
                        busy = false
                    }
                }
            }) { Text(s.more.add, color = Blue, fontWeight = FontWeight.SemiBold) }
        }
    }

    if (picking) {
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    picking = false
                }) { Text(s.done, color = Blue) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(s.cancel, color = Muted) } },
        ) { DatePicker(state = dateState) }
    }
}

@Composable
internal fun GroupTasksSheet(chat: ChatInfo, onClose: () -> Unit) {
    val s = S
    val version by ChatStore.version.collectAsState()
    val tasks = remember(version) { ChatStore.cachedGroupTasks().filter { it.chatId == chat.id } }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        error = try {
            ChatStore.refreshGroupTasks()
            null
        } catch (failure: ApiFailure) {
            s.failureText(failure)
        }
    }

    BottomSheet(onDismiss = onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.more.groupTasks, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
            if (chat.isStaff && !creating) {
                TextButton(onClick = { creating = true }) { Text(s.more.newGroupTask, color = Blue) }
            }
        }
        Text(s.more.groupTasksHint, color = Muted, fontSize = 13.sp)
        ErrorLine(error)
        if (creating) NewTaskForm(chat) { creating = false }
        if (tasks.isEmpty() && !creating) Text(s.more.noGroupTasks, color = Muted, fontSize = 14.sp)
        tasks.forEach { task -> GroupTaskCard(task) }
        Spacer(Modifier.height(4.dp))
    }
}
