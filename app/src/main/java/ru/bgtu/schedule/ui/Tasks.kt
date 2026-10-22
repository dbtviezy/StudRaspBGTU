package ru.bgtu.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.bgtu.schedule.data.Group
import ru.bgtu.schedule.data.Lesson
import ru.bgtu.schedule.data.TodoItem
import ru.bgtu.schedule.data.TodoStore
import ru.bgtu.schedule.data.isOverdue
import ru.bgtu.schedule.data.sortedTodos
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Composable
fun TasksOverlay(group: Group, onClose: () -> Unit, onChanged: () -> Unit) {
    val s = S
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { TodoStore(context) }
    var tick by remember { mutableIntStateOf(0) }
    val items = remember(tick) { sortedTodos(store.all()) }
    var editor by remember { mutableStateOf<TodoItem?>(null) }
    var creating by remember { mutableStateOf(false) }
    val open = items.filterNot { it.done }
    val done = items.filter { it.done }
    val account by ru.bgtu.schedule.data.Account.state.collectAsState()
    val chatVersion by ru.bgtu.schedule.data.ChatStore.version.collectAsState()
    val today = LocalDate.now().toString()
    val groupTasks = remember(chatVersion, account.signedIn) {
        if (!account.signedIn) emptyList()
        else ru.bgtu.schedule.data.ChatStore.cachedGroupTasks().filter { it.dueDate == null || it.dueDate >= today }
    }
    val chatTitles = remember(chatVersion) { ru.bgtu.schedule.data.ChatStore.cachedChats().associate { it.id to it.title } }
    LaunchedEffect(account.signedIn) {
        if (account.signedIn) runCatching { ru.bgtu.schedule.data.ChatStore.refreshGroupTasks() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = LocalBottomInset.current + 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.allTasks, modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = s.close, tint = Ink)
            }
        }
        Text(s.tasksHint, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        PrimaryButton(s.addTask, onClick = { creating = true })
        Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) {
            Text(
                s.tasksEmpty,
                color = Muted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        } else {
            if (open.isNotEmpty()) {
                Text(s.openTasks, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                open.forEach { item ->
                    TaskRow(item, onToggle = {
                        store.upsert(item.copy(done = !item.done))
                        tick++
                        onChanged()
                    }, onOpen = { editor = item })
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (done.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(s.completed, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                done.forEach { item ->
                    TaskRow(item, onToggle = {
                        store.upsert(item.copy(done = !item.done))
                        tick++
                        onChanged()
                    }, onOpen = { editor = item })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        if (groupTasks.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(s.more.groupTasks, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            groupTasks.forEach { task ->
                GroupTaskCard(task, chatTitle = chatTitles[task.chatId]?.ifBlank { null }) {
                    tick++
                    onChanged()
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    if (creating || editor != null) {
        TodoEditorDialog(
            group = group,
            initial = editor,
            presetLesson = null,
            onDismiss = {
                creating = false
                editor = null
            },
            onSave = { item ->
                store.upsert(item)
                tick++
                onChanged()
                creating = false
                editor = null
            },
            onDelete = editor?.let { current ->
                {
                    store.remove(current.id)
                    tick++
                    onChanged()
                    editor = null
                }
            },
        )
    }
}

@Composable
private fun TaskRow(item: TodoItem, onToggle: () -> Unit, onOpen: () -> Unit) {
    val s = S
    val late = item.isOverdue()
    TaskCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = item.done,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = Blue, uncheckedColor = Muted, checkmarkColor = Color.White),
            )
            Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
                Text(
                    item.title,
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = if (item.done) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                )
                Text(
                    if (late) "${s.overdue} · ${deadlineText(s, item)}" else deadlineText(s, item),
                    color = if (late) Red else Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
                item.lessonLabel?.let { label ->
                    Text(label, color = Blue, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoEditorDialog(
    group: Group,
    initial: TodoItem?,
    presetLesson: Lesson?,
    presetDate: String? = null,
    presetTime: String? = null,
    onDismiss: () -> Unit,
    onSave: (TodoItem) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val s = S
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var date by remember { mutableStateOf(initial?.deadlineDate ?: presetDate) }
    var time by remember { mutableStateOf(initial?.deadlineTime ?: presetTime.orEmpty()) }
    var remind by remember { mutableStateOf(initial?.remind.orEmpty()) }
    var lessonKey by remember { mutableStateOf(initial?.lessonKey ?: presetLesson?.key) }
    var lessonLabel by remember {
        mutableStateOf(initial?.lessonLabel ?: presetLesson?.let { lessonLabelOf(s, it) })
    }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingLesson by remember { mutableStateOf(false) }
    var timeError by remember { mutableStateOf(false) }
    var lessonQuery by remember { mutableStateOf("") }
    val dateState = rememberDatePickerState()
    val remindScroll = rememberScrollState()
    val lessons = remember(group.id) {
        group.lessons.distinctBy { it.key }.sortedWith(compareBy({ it.day }, { it.start }))
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Card)
                .padding(16.dp),
        ) {
            Text(
                if (initial == null) s.newTask else s.editTask,
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(s.taskTitle) },
                placeholder = { Text(s.taskTitleHint) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(s.deadline, color = Muted, fontSize = 12.sp)
            Text(
                date?.let { raw ->
                    val parsed = runCatching { LocalDate.parse(raw) }.getOrNull()
                    if (parsed == null) s.noDeadline else s.formatLongDateCap(parsed)
                } ?: s.noDeadline,
                color = Ink,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row {
                TextButton(onClick = {
                    date?.let { raw ->
                        runCatching { LocalDate.parse(raw).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
                            .getOrNull()
                            ?.let { dateState.selectedDateMillis = it }
                    }
                    pickingDate = true
                }) { Text(s.pickDate, color = Blue) }
                if (date != null) {
                    TextButton(onClick = { date = null }) { Text(s.clearDeadline, color = Muted) }
                }
            }
            if (date != null) {
                Row(Modifier.horizontalScroll(remindScroll), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("" to s.remindOff, "eve" to s.remindEve, "morning" to s.remindMorning).forEach { (value, label) ->
                        Text(
                            label,
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (remind == value) Blue else Bg)
                                .clickable { remind = value }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (remind == value) Color.White else Ink,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = time,
                onValueChange = {
                    time = it
                    timeError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(s.timeOptional) },
                placeholder = { Text(s.timeHint) },
                singleLine = true,
                isError = timeError,
                supportingText = if (timeError) {
                    { Text(s.badTime) }
                } else {
                    null
                },
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(s.attachLessonOptional, color = Muted, fontSize = 12.sp)
            Text(
                lessonLabel ?: s.notAttached,
                color = if (lessonLabel == null) Muted else Ink,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row {
                TextButton(onClick = { pickingLesson = true }) { Text(s.chooseLesson, color = Blue) }
                if (lessonKey != null) {
                    TextButton(onClick = {
                        lessonKey = null
                        lessonLabel = null
                    }) { Text(s.removeLesson, color = Muted) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(s.delete, color = Red) }
                }
                TextButton(onClick = onDismiss) { Text(s.cancel, color = Muted) }
                TextButton(enabled = title.isNotBlank(), onClick = {
                    val cleanTime = normalizeTime(time)
                    if (time.isNotBlank() && cleanTime == null) {
                        timeError = true
                        return@TextButton
                    }
                    if (title.isBlank()) return@TextButton
                    onSave(
                        TodoItem(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            deadlineDate = date,
                            deadlineTime = cleanTime,
                            lessonKey = lessonKey,
                            lessonLabel = lessonLabel,
                            done = initial?.done ?: false,
                            remind = if (date == null) "" else remind,
                        ),
                    )
                }) { Text(s.save, color = if (title.isNotBlank()) Blue else Muted) }
            }
        }
    }

    if (pickingDate) {
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    pickingDate = false
                }) { Text(s.done, color = Blue) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) { Text(s.cancel, color = Muted) }
            },
        ) { DatePicker(state = dateState) }
    }

    if (pickingLesson) {
        Dialog(onDismissRequest = { pickingLesson = false }) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(Card)
                    .padding(16.dp),
            ) {
                Text(s.chooseLesson, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = lessonQuery,
                    onValueChange = { lessonQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(s.subjectOrDay) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(8.dp))
                val filtered = lessons.filter { lesson ->
                    val q = lessonQuery.trim()
                    q.isBlank() || lesson.subject.contains(q, true) || lesson.dayName.contains(q, true) || lesson.start.contains(q)
                }
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    if (filtered.isEmpty()) Text(s.nothingFound, color = Muted)
                    filtered.forEach { lesson ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    lessonKey = lesson.key
                                    lessonLabel = lessonLabelOf(s, lesson)
                                    pickingLesson = false
                                }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(lesson.subject, color = Ink, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${s.dayName(lesson.dayName)} · ${lesson.start}",
                                color = Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
                TextButton(onClick = { pickingLesson = false }, modifier = Modifier.align(Alignment.End)) {
                    Text(s.cancel, color = Muted)
                }
            }
        }
    }
}

@Composable
private fun TaskCard(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x1420336B), spotColor = Color(0x1020336B))
            .clip(RoundedCornerShape(22.dp))
            .background(Card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
    ) { content() }
}

internal fun lessonLabelOf(copy: Copy, lesson: Lesson): String {
    return listOf(copy.dayName(lesson.dayName), lesson.start, lesson.subject)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
}

internal fun deadlineText(copy: Copy, item: TodoItem): String {
    val raw = item.deadlineDate ?: return copy.noDeadline
    val date = runCatching { LocalDate.parse(raw) }.getOrNull() ?: return copy.noDeadline
    val base = copy.formatLongDateCap(date)
    return if (item.deadlineTime.isNullOrBlank()) base else "$base · ${item.deadlineTime}"
}

internal fun normalizeTime(raw: String): String? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    val parts = text.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return "%02d:%02d".format(hour, minute)
}
