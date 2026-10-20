package ru.bgtu.schedule.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import ru.bgtu.schedule.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.bgtu.schedule.data.AbsenceLimits
import ru.bgtu.schedule.data.AbsenceStore
import ru.bgtu.schedule.data.Backup
import ru.bgtu.schedule.data.DayOverrides
import ru.bgtu.schedule.data.DayRow
import ru.bgtu.schedule.data.HiddenStore
import ru.bgtu.schedule.data.Holidays
import ru.bgtu.schedule.data.LiveStatus
import ru.bgtu.schedule.data.SubjectNotes
import ru.bgtu.schedule.data.WeekCalendar
import ru.bgtu.schedule.data.dayRows
import ru.bgtu.schedule.data.isOverdue
import ru.bgtu.schedule.data.lessonsOfKind
import ru.bgtu.schedule.data.liveStatus
import ru.bgtu.schedule.data.nextTeacherLessons
import ru.bgtu.schedule.data.searchTeachers
import ru.bgtu.schedule.data.sharedFree
import ru.bgtu.schedule.data.shownLessons
import ru.bgtu.schedule.data.Faculty
import ru.bgtu.schedule.data.FavoriteTeachers
import ru.bgtu.schedule.data.TeacherNext
import ru.bgtu.schedule.data.Group
import ru.bgtu.schedule.data.Lesson
import ru.bgtu.schedule.data.ScheduleBundle
import ru.bgtu.schedule.data.formatLongDate
import ru.bgtu.schedule.data.formatMonth
import ru.bgtu.schedule.data.greeting
import ru.bgtu.schedule.data.lessonsOn
import ru.bgtu.schedule.data.pairNumber
import ru.bgtu.schedule.data.pairsWord
import ru.bgtu.schedule.data.parseTime
import ru.bgtu.schedule.data.TodoItem
import ru.bgtu.schedule.data.typeColorKey
import ru.bgtu.schedule.data.weekBadge
import ru.bgtu.schedule.data.weekInfo
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

private val Shape = RoundedCornerShape(22.dp)
private const val FETCHED = "11 октября 2026"

@Composable
fun Mark(size: Int = 44) {
    Image(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = S.brand,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (IsDark) Color.Transparent else BrandBlue)
            .graphicsLayer {
                scaleX = 1.5f
                scaleY = 1.5f
            },
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun SplashScreen() {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val seal = minOf(248.dp, screenHeight * 0.42f)
    Box(
        Modifier
            .fillMaxSize()
            .background(BrandBlue),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = S.brand,
            modifier = Modifier
                .align(Alignment.Center)
                .size(seal)
                .clip(CircleShape)
                .graphicsLayer {
                    scaleX = 1.58f
                    scaleY = 1.58f
                },
            contentScale = ContentScale.Fit,
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 28.dp, end = 28.dp, bottom = LocalBottomInset.current + 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Брянский государственный технический университет",
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                "СтудРаспБГТУ",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
fun GroupPicker(
    bundle: ScheduleBundle,
    initialName: String,
    selectedId: String?,
    onBack: (() -> Unit)?,
    onConfirm: (String, String) -> Unit,
    askName: Boolean = true,
) {
    val s = S
    var name by remember { mutableStateOf(initialName) }
    var query by remember { mutableStateOf("") }
    var facultyShort by remember { mutableStateOf<String?>(null) }
    var chosen by remember { mutableStateOf(selectedId) }
    val faculty = bundle.faculties.firstOrNull { it.short == facultyShort }
    val groups = faculty?.groups.orEmpty().filter { group ->
        val q = query.trim()
        q.isBlank() || group.id.contains(q, true)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (faculty != null || onBack != null) {
                IconButton(onClick = {
                    if (faculty != null) {
                        facultyShort = null
                        query = ""
                    } else {
                        onBack?.invoke()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, tint = Ink)
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Mark(44)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(s.brand, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(s.fullTime, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (faculty == null) {
            Text(s.chooseFaculty, Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), color = Ink, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text(
                s.facultyThenGroup,
                Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                color = Muted,
                fontSize = 14.sp,
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(bundle.faculties, key = { it.short }) { item ->
                    FacultyCard(item) { facultyShort = item.short }
                }
            }
        } else {
            Text(faculty.short, Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), color = Ink, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text(
                S.facultyName(faculty.name),
                Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                color = Muted,
                fontSize = 14.sp,
            )
            Text(
                s.chooseGroup,
                Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            if (askName) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    label = { Text(s.name) },
                    placeholder = { Text(s.nameHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = fieldColors(),
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text(s.findGroup) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Muted) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors(),
            )
            LazyColumn(
                modifier = Modifier.weight(1f).padding(top = 10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(groups, key = { it.id }) { group ->
                    GroupRow(group, faculty, chosen == group.id) { chosen = group.id }
                }
                if (groups.isEmpty()) {
                    item { Text(s.groupMissing, color = Muted, modifier = Modifier.padding(8.dp)) }
                }
            }
            Button(
                onClick = { chosen?.let { onConfirm(it, name.trim()) } },
                enabled = faculty.groups.any { it.id == chosen },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = LocalBottomInset.current + 12.dp)
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue, disabledContainerColor = Blue.copy(alpha = 0.35f)),
            ) {
                Text(if (askName) s.openSchedule else s.done, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun FacultyCard(faculty: Faculty, onClick: () -> Unit) {
    val s = S
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(6.dp, Shape, ambientColor = Color(0x1420336B), spotColor = Color(0x1020336B))
            .clip(Shape)
            .background(Card)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(facultyColor(faculty.short).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(faculty.short, color = facultyColor(faculty.short), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(S.facultyName(faculty.name), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(s.groupsCount(faculty.groups.size), color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Muted)
    }
}

@Composable
private fun GroupRow(group: Group, faculty: Faculty, selected: Boolean, onClick: () -> Unit) {
    val s = S
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) BlueSoft else Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(group.id, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                listOfNotNull(s.levelName(group.level), group.year?.let { s.cohortValue(it) }, faculty.short).joinToString(" · "),
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Blue)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    bundle: ScheduleBundle,
    faculty: Faculty,
    group: Group,
    studentName: String,
    dark: Boolean,
    onDark: (Boolean, Offset) -> Unit,
    noteOf: (LocalDate, String) -> String,
    onNotes: () -> Unit,
    onOpenProfile: () -> Unit,
    lang: String?,
    onLanguage: (String) -> Unit,
    tasksTick: Int,
    onOpenTasks: () -> Unit,
    friendId: String?,
    onTasksChanged: () -> Unit = {},
    onLesson: (Lesson, LocalDate) -> Unit,
) {
    val s = S
    val context = LocalContext.current
    val taskStore = remember { ru.bgtu.schedule.data.TodoStore(context) }
    val openTasks = remember(tasksTick) {
        ru.bgtu.schedule.data.sortedTodos(taskStore.all()).filterNot { it.done }
    }
    val clock = rememberNow()
    val today = clock.toLocalDate()
    val tomorrow = today.plusDays(1)
    val now = clock.toLocalTime()
    val info = weekInfo(today, bundle.semesterStart)
    var quickTick by remember { mutableIntStateOf(0) }
    val hidden = remember(quickTick) { HiddenStore(context).keys() }
    val overrides = remember { DayOverrides(context) }
    val missed = AbsenceStore(context).marks()
    val todayLessons = shownLessons(group, today.dayOfWeek.value, today, bundle.semesterStart, "auto", hidden, overrides)
    val tomorrowLessons = shownLessons(group, tomorrow.dayOfWeek.value, tomorrow, bundle.semesterStart, "auto", hidden, overrides)
    val status = liveStatus(group, today, now, bundle.semesterStart, hidden, overrides)
    val upcoming = upcomingLessons(group, today, now, bundle.semesterStart, hidden, overrides)
    val friend = friendId?.let { bundle.find(it)?.second }
    val shared = if (friend == null) emptyList() else sharedFree(group, friend, today, now, bundle.semesterStart, hidden, overrides)
    val hour = now.hour
    var themeCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var tasksExpanded by rememberSaveable { mutableStateOf(false) }
    var openedTeacher by remember { mutableStateOf<String?>(null) }
    var gapEditor by remember { mutableStateOf<Pair<LocalDate, String>?>(null) }
    val upcomingRows = upcoming.groupBy { it.first }.flatMap { (day, pairs) ->
        dayRows(pairs.map { it.second }).map { row -> day to row }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mark(44)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        s.brand,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(faculty.short, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                LanguageMark(lang, onLanguage)
                IconButton(
                    modifier = Modifier.onGloballyPositioned { themeCoords = it },
                    onClick = {
                        val coords = themeCoords ?: return@IconButton
                        val center = coords.positionInWindow()
                        onDark(!dark, Offset(center.x + coords.size.width / 2f, center.y + coords.size.height / 2f))
                    },
                ) {
                    Icon(
                        if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = s.theme,
                        tint = Ink,
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(onClick = onNotes) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = s.reminders,
                        tint = Ink,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        item { AnnouncementsBanner() }
        item {
            Appear(0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(listOf(HeroStart, HeroEnd)))
                        .clickable(onClick = onOpenProfile)
                        .padding(22.dp),
                ) {
                    Column {
                        Text(s.greeting(hour), color = Color.White.copy(alpha = 0.82f), fontSize = 14.sp)
                        Text(
                            liveLine(s, status),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Text(
                            studentName.ifBlank { s.studentFallback },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(group.id, color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            s.facultyName(faculty.name),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(s.formatLongDateCap(today), color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill(s.todayPill(todayLessons.size))
                            Pill(s.evenWeekTitle(info.even))
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), s.today, todayLessons.size.toString(), s.pairsWord(todayLessons.size))
                StatCard(Modifier.weight(1f), s.tomorrow, tomorrowLessons.size.toString(), s.pairsWord(tomorrowLessons.size))
            }
        }
        if (friend != null) {
            item {
                SoftCard {
                    Text(s.bothFreeTitle, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    if (shared.isEmpty()) {
                        Text(s.noSharedWindow, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    } else {
                        shared.forEach { (day, time) ->
                            Text(
                                s.bothFreeLine(day, today, time),
                                color = Blue,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
        }
        item {
            SoftCard(onClick = onOpenTasks) {
                Text(s.tasks, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(
                    if (openTasks.isEmpty()) s.tasksEmpty else s.openTasksLine(openTasks.size),
                    color = if (openTasks.any { it.isOverdue() }) Red else Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
                openTasks.take(2).forEach { item -> TaskPreviewLine(item) }
                AnimatedVisibility(
                    visible = tasksExpanded && openTasks.size > 2,
                    enter = expandVertically(tween(320, easing = FastOutSlowInEasing)) + fadeIn(tween(220)),
                    exit = shrinkVertically(tween(260, easing = FastOutSlowInEasing)) + fadeOut(tween(160)),
                ) {
                    Column {
                        openTasks.drop(2).take(4).forEach { item -> TaskPreviewLine(item) }
                    }
                }
                if (openTasks.size > 2) {
                    val turn by animateFloatAsState(
                        targetValue = if (tasksExpanded) 180f else 0f,
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        label = "tasksChevron",
                    )
                    Row(
                        Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { tasksExpanded = !tasksExpanded }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AnimatedContent(
                            targetState = tasksExpanded,
                            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                            label = "tasksToggle",
                        ) { expanded ->
                            Text(
                                if (expanded) s.collapseTasks else s.expandTasks,
                                color = Blue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Icon(
                            Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = Blue,
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .size(18.dp)
                                .graphicsLayer { rotationZ = turn },
                        )
                    }
                }
            }
        }
        val favoriteNames = FavoriteTeachers(context).keys().sorted()
        if (favoriteNames.isNotEmpty()) {
            item {
                SoftCard {
                    Text(s.favoriteTeachers, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    favoriteNames.forEach { name ->
                        val next = nextTeacherLessons(bundle, name, today, now, bundle.semesterStart, limit = 1).firstOrNull()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { openedTeacher = name }
                                .padding(top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(name, color = Ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (next == null) s.noTeacherSoon else teacherFollowLine(s, today, next),
                                    color = if (next == null) Muted else Blue,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        item {
            Text(s.upcomingLessons, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
            Text(s.studyWeekLine(info.number, info.even), color = Muted, fontSize = 13.sp)
        }
        if (upcoming.isEmpty()) {
            item {
                SoftCard {
                    Text(s.freeWindow, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(s.freeWindowText, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            itemsIndexed(upcomingRows, key = { index, item ->
                val (day, row) = item
                when (row) {
                    is DayRow.Class -> "${day}|${row.lesson.key}"
                    is DayRow.Gap -> "gap-$index|$day|${row.gap.startsAt}"
                }
            }) { index, (day, row) ->
                when (row) {
                    is DayRow.Class -> Appear(index + 1) {
                        ScheduleLesson(
                            lesson = row.lesson,
                            date = day,
                            caption = s.whenLabel(day, today),
                            note = noteOf(day, row.lesson.key),
                            tasksTick = tasksTick,
                            onOpen = { onLesson(row.lesson, day) },
                            onChanged = { quickTick++ },
                            missed = absenceMark(day, row.lesson) in missed,
                        )
                    }
                    is DayRow.Gap -> Text(
                        "${s.liveWindow} ${s.span(row.gap.minutes)}",
                        color = if (row.gap.minutes >= 60) Blue else Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .then(
                                if (row.gap.minutes >= 60) {
                                    Modifier.clickable { gapEditor = day to row.gap.startsAt }
                                } else {
                                    Modifier
                                },
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
    openedTeacher?.let { name ->
        TeacherDialog(bundle, name, onClose = { openedTeacher = null }, onFavoriteChanged = { quickTick++ })
    }
    val gapAt = gapEditor
    if (gapAt != null) {
        TodoEditorDialog(
            group = group,
            initial = null,
            presetLesson = null,
            presetDate = gapAt.first.toString(),
            presetTime = gapAt.second,
            onDismiss = { gapEditor = null },
            onSave = { item ->
                ru.bgtu.schedule.data.TodoStore(context).upsert(item)
                onTasksChanged()
                gapEditor = null
            },
        )
    }
}

@Composable
fun ScheduleScreen(
    bundle: ScheduleBundle,
    faculty: Faculty,
    group: Group,
    date: LocalDate,
    filter: String,
    onDate: (LocalDate) -> Unit,
    onFilter: (String) -> Unit,
    onLesson: (Lesson) -> Unit,
    noteOf: (LocalDate, String) -> String,
    tasksTick: Int = 0,
    onTasksChanged: () -> Unit = {},
) {
    val s = S
    val context = LocalContext.current
    val tick = tasksTick
    var quickTick by remember { mutableIntStateOf(0) }
    val hidden = remember(quickTick) { HiddenStore(context).keys() }
    val overrides = remember { DayOverrides(context) }
    val holiday = Holidays.on(date)
    val monday = date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val info = weekInfo(date, bundle.semesterStart)
    val lessons = shownLessons(group, date.dayOfWeek.value, date, bundle.semesterStart, filter, hidden, overrides)
    val missed = AbsenceStore(context).marks()
    val official = if (holiday == null) lessonsOn(group, date.dayOfWeek.value, date, bundle.semesterStart, filter) else emptyList()
    val skipped = official.filter { it.key in hidden }
    val cancelledKeys = overrides.cancelledOn(date.toString())
    val cancelled = official.filter { it.key !in hidden && it.key in cancelledKeys }
    var hiddenOpen by rememberSaveable { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var gapEditor by remember { mutableStateOf<String?>(null) }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) {
            exportMessage = exportWeek(context, s, group, bundle.semesterStart, date, hidden)
        }
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(s.schedule, color = Ink, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text("${group.id} · ${s.facultyName(faculty.name)}", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                s.formatLongDateCap(date) + " · " + s.evenWeekTitle(info.even),
                color = Ink,
                fontSize = 14.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        val shared = if (filter == "both" && holiday == null) {
                            val num = lessonsOfKind(group, date.dayOfWeek.value, "num", hidden).mapNotNull { overrides.apply(date, it) }
                            val den = lessonsOfKind(group, date.dayOfWeek.value, "den", hidden).mapNotNull { overrides.apply(date, it) }
                            "${s.formatLongDateCap(date)}\n${group.id}\n${s.numeratorColumn}\n${shareBody(num)}\n${s.denominatorColumn}\n${shareBody(den)}"
                        } else {
                            shareText(s, group, date, lessons, holiday)
                        }
                        putExtra(Intent.EXTRA_TEXT, shared)
                    }, s.shareDay))
                }) { Text(s.shareDay, color = Blue) }
                TextButton(onClick = {
                    if (WeekCalendar.canWrite(context)) {
                        exportMessage = exportWeek(context, s, group, bundle.semesterStart, date, hidden)
                    } else {
                        calendarPermission.launch(
                            arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR),
                        )
                    }
                }) { Text(s.exportWeek, color = Blue) }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (offset in 0..5) {
                    val day = monday.plusDays(offset.toLong())
                    DayChip(day, day == date) { onDate(day) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(s.filterAuto, filter == "auto") { onFilter("auto") }
                FilterChip(s.filterAll, filter == "all") { onFilter("all") }
                FilterChip(s.numerator.replaceFirstChar { it.uppercase() }, filter == "num") { onFilter("num") }
                FilterChip(s.denominator.replaceFirstChar { it.uppercase() }, filter == "den") { onFilter("den") }
                FilterChip(s.bothWeeks, filter == "both") { onFilter("both") }
            }
        }
        if (holiday != null) {
            item {
                SoftCard {
                    Text(if (s.code == "en") holiday.nameEn else holiday.nameRu, color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(s.holidayQuiet, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else if (filter == "both") {
            item {
                BothColumns(
                    left = lessonsOfKind(group, date.dayOfWeek.value, "num", hidden).mapNotNull { overrides.apply(date, it) },
                    right = lessonsOfKind(group, date.dayOfWeek.value, "den", hidden).mapNotNull { overrides.apply(date, it) },
                    noteOf = { noteOf(date, it) },
                    onLesson = onLesson,
                    date = date,
                    tasksTick = tick,
                    onChanged = { quickTick++ },
                    missed = missed,
                )
            }
        } else if (lessons.isEmpty()) {
            item { EmptyDay() }
        } else {
            val rows = dayRows(lessons)
            items(count = rows.size, key = { index ->
                when (val row = rows[index]) {
                    is DayRow.Class -> row.lesson.key
                    is DayRow.Gap -> "gap-$index-${row.gap.startsAt}"
                }
            }) { index ->
                when (val row = rows[index]) {
                    is DayRow.Class -> Appear(index) {
                        ScheduleLesson(
                            lesson = row.lesson,
                            date = date,
                            note = noteOf(date, row.lesson.key),
                            tasksTick = tick,
                            onOpen = { onLesson(row.lesson) },
                            onChanged = { quickTick++ },
                            missed = absenceMark(date, row.lesson) in missed,
                        )
                    }
                    is DayRow.Gap -> Text(
                        "${s.liveWindow} ${s.span(row.gap.minutes)}",
                        color = if (row.gap.minutes >= 60) Blue else Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .then(if (row.gap.minutes >= 60) Modifier.clickable { gapEditor = row.gap.startsAt } else Modifier)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        if (skipped.isNotEmpty() || cancelled.isNotEmpty()) {
            item(key = "hidden-section") {
                HiddenSection(
                    open = hiddenOpen,
                    onToggle = { hiddenOpen = !hiddenOpen },
                    skipped = skipped,
                    cancelled = cancelled,
                    onRestoreSkipped = { lesson ->
                        HiddenStore(context).set(lesson.key, false)
                        refreshAfterLessonChange(context)
                        quickTick++
                    },
                    onRestoreCancelled = { lesson ->
                        overrides.clear(date.toString(), lesson.key)
                        refreshAfterLessonChange(context)
                        quickTick++
                    },
                )
            }
        }
    }
    val gapAt = gapEditor
    if (gapAt != null) {
        TodoEditorDialog(
            group = group,
            initial = null,
            presetLesson = null,
            presetDate = date.toString(),
            presetTime = gapAt,
            onDismiss = { gapEditor = null },
            onSave = { item ->
                ru.bgtu.schedule.data.TodoStore(context).upsert(item)
                onTasksChanged()
                gapEditor = null
            },
        )
    }
    exportMessage?.let { text ->
        AlertDialog(
            onDismissRequest = { exportMessage = null },
            containerColor = Card,
            titleContentColor = Ink,
            textContentColor = Muted,
            title = { Text(s.exportWeek) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = { exportMessage = null }) { Text(s.ok, color = Blue) }
            },
        )
    }
}

@Composable
fun GroupsScreen(bundle: ScheduleBundle, currentId: String, onPick: (String) -> Unit) {
    val s = S
    val context = LocalContext.current
    val favorites = remember { FavoriteTeachers(context) }
    var favTick by remember { mutableIntStateOf(0) }
    val favoriteNames = remember(favTick) { favorites.keys().sorted() }
    var query by remember { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf("groups") }
    var opened by rememberSaveable { mutableStateOf<String?>(null) }
    var openedTeacher by remember { mutableStateOf<String?>(null) }
    val searching = query.trim().length >= 2
    val teachers = remember(query, bundle, mode, currentId, favTick) {
        if (mode != "teachers") return@remember emptyList()
        val found = searchTeachers(bundle, if (searching) query.trim() else "", bundle.find(currentId)?.second)
        if (searching) found else found.filter { it.name !in favoriteNames }
    }
    val faculty = bundle.faculties.firstOrNull { it.short == opened }
    val search = query.trim()
    val results = remember(search, bundle) {
        if (search.isBlank()) emptyList() else bundle.groups.filter { (item, group) ->
            group.id.contains(search, true) ||
                item.name.contains(search, true) ||
                item.short.contains(search, true) ||
                group.level.contains(search, true)
        }
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            if (faculty != null && search.isBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { opened = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.toFaculties, tint = Ink)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(faculty.short, color = Ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text(s.facultyName(faculty.name), color = Muted, fontSize = 13.sp)
                    }
                }
            } else {
                Text(s.groupsTitle, color = Ink, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                Text(s.groupsHint, color = Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(s.groupsMode, mode == "groups") { mode = "groups" }
                FilterChip(s.teachersMode, mode == "teachers") { mode = "teachers" }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(if (mode == "teachers") s.findTeacher else s.findGroupOrFaculty) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Muted) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors(),
            )
        }
        if (mode == "teachers") {
            if (!searching && favoriteNames.isNotEmpty()) {
                item(key = "favTitle") { SectionTitle(s.favoriteTeachers) }
                items(favoriteNames, key = { "fav|$it" }) { name ->
                    TeacherRow(bundle, name, null, favorite = true, onOpen = { openedTeacher = name }) {
                        favorites.set(name, false)
                        favTick++
                    }
                }
            }
            if (searching && teachers.isEmpty()) {
                item(key = "none") { Text(s.nothingFound, color = Muted, modifier = Modifier.padding(8.dp)) }
            }
            if (teachers.any { it.mine }) {
                item(key = "mineTitle") { SectionTitle(s.more.myTeachers, Modifier.padding(top = 4.dp)) }
            }
            if (!searching && teachers.isEmpty() && favoriteNames.isEmpty()) {
                item(key = "mineEmpty") { Text(s.more.myTeachersEmpty, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp)) }
            }
            teachers.forEachIndexed { index, card ->
                if (!card.mine && (index == 0 || teachers[index - 1].mine)) {
                    item(key = "otherTitle") { SectionTitle(s.more.otherTeachers, Modifier.padding(top = 4.dp)) }
                }
                item(key = "teacher|${card.name}") {
                    val starred = card.name in favoriteNames
                    TeacherRow(bundle, card.name, card, favorite = starred, onOpen = { openedTeacher = card.name }) {
                        favorites.set(card.name, !starred)
                        favTick++
                    }
                }
            }
            if (!searching) {
                item(key = "hint") { Text(s.more.teacherSearchHint, color = Muted, modifier = Modifier.padding(8.dp)) }
            }
        } else if (search.isNotBlank()) {
            items(results, key = { it.second.id + it.first.short }) { (item, group) ->
                GroupRow(group, item, group.id == currentId) { onPick(group.id) }
            }
            if (results.isEmpty()) {
                item { Text(s.nothingFound, color = Muted, modifier = Modifier.padding(8.dp)) }
            }
        } else if (faculty == null) {
            items(bundle.faculties, key = { it.short }) { item ->
                FacultyCard(item) { opened = item.short }
            }
        } else {
            items(faculty.groups, key = { it.id }) { group ->
                GroupRow(group, faculty, group.id == currentId) { onPick(group.id) }
            }
        }
    }
    openedTeacher?.let { name ->
        TeacherDialog(bundle, name, onClose = { openedTeacher = null }, onFavoriteChanged = { favTick++ })
    }
}

@Composable
private fun TeacherRow(
    bundle: ScheduleBundle,
    name: String,
    card: ru.bgtu.schedule.data.TeacherCard?,
    favorite: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
) {
    val s = S
    val today = LocalDate.now()
    val next = remember(name, bundle) {
        nextTeacherLessons(bundle, name, today, LocalTime.now(), bundle.semesterStart, limit = 1).firstOrNull()
    }
    val subjects = card?.subjects.orEmpty()
    SoftCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(name, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                RatingLine(name, onOpen)
                if (subjects.isNotEmpty()) {
                    Text(
                        subjects.take(2).joinToString(" · ") + if (subjects.size > 2) " +${subjects.size - 2}" else "",
                        color = Muted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (card != null) Text(s.teacherGroups(card.groups), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                Text(
                    if (next == null) s.noTeacherSoon else teacherFollowLine(s, today, next),
                    color = if (next == null) Muted else Blue,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            TeacherStar(favorite, onFavorite)
        }
    }
}

@Composable
fun CalendarScreen(
    bundle: ScheduleBundle,
    group: Group,
    date: LocalDate,
    filter: String,
    onDate: (LocalDate) -> Unit,
    onLesson: (Lesson) -> Unit,
    noteOf: (LocalDate, String) -> String,
    tasksTick: Int = 0,
) {
    val s = S
    val tick = tasksTick
    val month = YearMonth.from(date)
    val first = month.atDay(1)
    val pad = first.dayOfWeek.value - 1
    val cells = buildList<LocalDate?> {
        repeat(pad) { add(null) }
        for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
    }
    val context = LocalContext.current
    val todayDate = rememberNow().toLocalDate()
    val hidden = HiddenStore(context).keys()
    val overrides = remember { DayOverrides(context) }
    val holiday = Holidays.on(date)
    val lessons = shownLessons(group, date.dayOfWeek.value, date, bundle.semesterStart, filter, hidden, overrides)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(s.calendar, color = Ink, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        Spacer(Modifier.height(8.dp))
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onDate(date.minusMonths(1)) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = s.previousMonth, tint = Ink)
                }
                Text(s.formatMonth(date), modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                IconButton(onClick = { onDate(date.plusMonths(1)) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = s.nextMonth, tint = Ink)
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                s.dayShort.forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        color = Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    for (index in 0 until 7) {
                        val cell = week.getOrNull(index)
                        Box(Modifier.weight(1f).heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
                            if (cell != null) {
                                val selected = cell == date
                                val today = cell == todayDate
                                val busy = Holidays.on(cell) == null && shownLessons(group, cell.dayOfWeek.value, cell, bundle.semesterStart, filter, hidden, overrides).isNotEmpty()
                                val off = Holidays.on(cell) != null
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onDate(cell) }) {
                                    Box(
                                        Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (selected) Blue else Color.Transparent)
                                            .border(if (today && !selected) 1.5.dp else 0.dp, Blue, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            cell.dayOfMonth.toString(),
                                            color = if (selected) Color.White else if (off) Blue else Ink,
                                            fontSize = 13.sp,
                                            fontWeight = if (selected || today) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }
                                    Box(
                                        Modifier
                                            .padding(top = 2.dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (busy) Blue else Color.Transparent),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(s.formatLongDateCap(date), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        if (holiday != null) {
            Text(if (s.code == "en") holiday.nameEn else holiday.nameRu, color = Ink, fontWeight = FontWeight.SemiBold)
            Text(s.holidayQuiet, color = Muted, modifier = Modifier.padding(top = 4.dp))
        } else if (lessons.isEmpty()) {
            Text(s.noLessonsThatDay, color = Muted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val missed = AbsenceStore(context).marks()
                lessons.forEach { lesson ->
                    LessonCard(
                        lesson,
                        note = noteOf(date, lesson.key),
                        tasksTick = tick,
                        missed = absenceMark(date, lesson) in missed,
                    ) { onLesson(lesson) }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun ProfileScreen(
    bundle: ScheduleBundle,
    faculty: Faculty,
    group: Group,
    studentName: String,
    dark: Boolean,
    onDark: (Boolean, Offset) -> Unit,
    onName: (String) -> Unit,
    onChangeGroup: () -> Unit,
    onReset: () -> Unit,
    onOpenFiles: () -> Unit,
    lang: String?,
    onLanguage: (String) -> Unit,
    onOpenTasks: () -> Unit,
    onPickFriend: () -> Unit,
    onFriendChanged: () -> Unit,
    onRestored: (String, String) -> Unit = { _, _ -> },
    onOpenChats: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
    followSystem: Boolean = false,
    onFollowSystem: (Boolean) -> Unit = {},
    onWhatsNew: () -> Unit = {},
) {
    val s = S
    val context = LocalContext.current
    val prefs = remember { ru.bgtu.schedule.data.Prefs(context.applicationContext) }
    var chatNotifyOn by remember { mutableStateOf(prefs.chatNotify()) }
    var ongoingOn by remember { mutableStateOf(prefs.ongoingLesson()) }
    var afterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val extraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) afterPermission?.invoke()
        afterPermission = null
    }
    fun withNotifications(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            afterPermission = action
            extraPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }
    var name by remember(studentName) { mutableStateOf(studentName) }
    var confirmReset by remember { mutableStateOf(false) }
    var themeCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var avatarStamp by remember { mutableIntStateOf(0) }
    var pendingCrop by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var notifyOn by remember { mutableStateOf(prefs.notifyEnabled()) }
    var lead by remember { mutableIntStateOf(prefs.notifyLead()) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            pendingCrop?.recycle()
            pendingCrop = ru.bgtu.schedule.data.decodeBitmap(context, uri)
        }
    }
    var profileTick by remember { mutableIntStateOf(0) }
    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val wrote = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(Backup.exportJson(context).toByteArray(Charsets.UTF_8))
            }
        }.isSuccess
        Toasts.show(if (wrote) s.backupDone else s.backupFailed)
    }
    val loadBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull()
        if (text.isNullOrBlank()) {
            Toasts.show(s.backupFailed)
            return@rememberLauncherForActivityResult
        }
        val restored = runCatching { Backup.importJson(context, text) }.getOrNull()
        if (restored == null) {
            Toasts.show(s.backupFailed)
        } else {
            onRestored(restored.first, restored.second)
            profileTick++
            Toasts.show(s.backupRestored)
        }
    }
    val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            notifyOn = true
            prefs.setNotifyEnabled(true)
            ru.bgtu.schedule.notify.LessonAlerts.reschedule(context)
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.profile, modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            LanguageMark(lang, onLanguage)
            Spacer(Modifier.width(8.dp))
            Icon(
                if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = null,
                tint = Blue,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = dark,
                onCheckedChange = { value ->
                    val coords = themeCoords
                    val center = if (coords == null) {
                        Offset.Zero
                    } else {
                        val raw = coords.positionInWindow()
                        Offset(raw.x + coords.size.width / 2f, raw.y + coords.size.height / 2f)
                    }
                    onDark(value, center)
                },
                modifier = Modifier.onGloballyPositioned { themeCoords = it },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Blue,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = Line,
                    uncheckedThumbColor = Color.White,
                    uncheckedBorderColor = Line,
                ),
            )
        }
        Spacer(Modifier.height(16.dp))
        SoftCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                ProfileAvatar(avatarStamp, name) {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                Row {
                    TextButton(
                        onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    ) { Text(s.changePhoto, color = Blue) }
                    if (ru.bgtu.schedule.data.avatarFile(context).exists()) {
                        TextButton(
                            onClick = {
                                ru.bgtu.schedule.data.avatarFile(context).delete()
                                avatarStamp++
                            },
                        ) { Text(s.removePhoto, color = Muted) }
                    }
                }
            }
            key(dark) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(60)
                        onName(name)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(s.name) },
                    placeholder = { Text(s.nameHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors(),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard {
            InfoLine(s.group, group.id)
            InfoLine(s.faculty, s.facultyName(faculty.name))
            InfoLine(s.level, s.levelName(group.level))
            group.year?.let { InfoLine(s.cohort, s.cohortValue(it)) }
            InfoLine(s.period, s.periodLabel(bundle.periodLabel).ifBlank { "—" })
            InfoLine(s.form, s.fullTimeForm)
            PrimaryButton(s.changeGroup, onChangeGroup, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        }
        Spacer(Modifier.height(12.dp))
        AccountCard(groupId = group.id, onOpenChats = onOpenChats)
        if (ru.bgtu.schedule.BuildConfig.ADMIN) {
            Spacer(Modifier.height(12.dp))
            SoftCard(onClick = onOpenAdmin) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        ProfileCardTitle(s.more.adminPanel, s.more.adminHint)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Muted)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard {
            val friendId = remember(profileTick) { prefs.friendGroup() }
            val friendLabel = friendId?.let { id -> bundle.find(id)?.let { "${it.second.id} · ${it.first.short}" } } ?: friendId
            ProfileCardTitle(s.secondGroup, s.secondGroupHint)
            if (!friendLabel.isNullOrBlank()) {
                Text(friendLabel, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            }
            Row {
                TextButton(onClick = onPickFriend) { Text(s.pickSecond, color = Blue) }
                if (!friendId.isNullOrBlank()) {
                    TextButton(onClick = {
                        prefs.setFriendGroup(null)
                        profileTick++
                        onFriendChanged()
                    }) { Text(s.clearSecond, color = Muted) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(s.lessonReminder, color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(s.lessonReminderHint, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Switch(
                    checked = notifyOn,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            notifyOn = false
                            prefs.setNotifyEnabled(false)
                            ru.bgtu.schedule.notify.LessonAlerts.cancelAll(context)
                        } else if (Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            notifyOn = true
                            prefs.setNotifyEnabled(true)
                            ru.bgtu.schedule.notify.LessonAlerts.reschedule(context)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Blue,
                        checkedThumbColor = Color.White,
                        uncheckedTrackColor = Line,
                        uncheckedThumbColor = Color.White,
                        uncheckedBorderColor = Line,
                    ),
                )
            }
            if (notifyOn) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    s.leadOptions().forEach { (minutes, label) ->
                        FilterChip(label, lead == minutes) {
                            lead = minutes
                            prefs.setNotifyLead(minutes)
                            ru.bgtu.schedule.notify.LessonAlerts.reschedule(context)
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarms = context.getSystemService(AlarmManager::class.java)
                    if (alarms != null && !alarms.canScheduleExactAlarms()) {
                        TextButton(onClick = {
                            val settings = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { context.startActivity(settings) }
                        }) { Text(s.allowExact, color = Blue) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard {
            Text(s.more.settingsTitle, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            SettingSwitch(s.more.themeSystem, s.more.themeSystemHint, followSystem) { onFollowSystem(it) }
            SettingSwitch(s.more.hideProfanity, s.more.hideProfanityHint, AppSignals.hideProfanity) { on ->
                AppSignals.hideProfanity = on
                prefs.setHideProfanity(on)
            }
            val account by ru.bgtu.schedule.data.Account.state.collectAsState()
            SettingSwitch(
                s.more.chatNotify,
                if (account.signedIn) s.more.chatNotifyHint else s.more.chatNotifySignIn,
                chatNotifyOn,
            ) { on ->
                if (!on) {
                    chatNotifyOn = false
                    prefs.setChatNotify(false)
                    ru.bgtu.schedule.notify.ChatNotify.schedule(context)
                } else if (!account.signedIn) {
                    Toasts.show(s.more.chatNotifySignIn)
                } else {
                    withNotifications {
                        chatNotifyOn = true
                        prefs.setChatNotify(true)
                        ru.bgtu.schedule.notify.ChatNotify.markSeen(context)
                        ru.bgtu.schedule.notify.ChatNotify.schedule(context)
                    }
                }
            }
            SettingSwitch(s.more.ongoingLesson, s.more.ongoingLessonHint, ongoingOn) { on ->
                val apply = {
                    ongoingOn = on
                    prefs.setOngoingLesson(on)
                    Thread { ru.bgtu.schedule.notify.OngoingLesson.reschedule(context) }.start()
                }
                if (on) withNotifications(apply) else apply()
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard {
            val feed = remember(group.id) {
                "${ru.bgtu.schedule.data.Http.BASE_URL}/api/ics/" +
                    java.net.URLEncoder.encode(group.id, "UTF-8").replace("+", "%20") + ".ics"
            }
            val webcal = feed.replaceFirst("https://", "webcal://")
            ProfileCardTitle(s.more.calendarSubTitle, s.more.calendarSubHint)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick = {
                    val url = "https://calendar.google.com/calendar/render?cid=" + java.net.URLEncoder.encode(webcal, "UTF-8")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                }) { Text(s.more.calendarGoogle, color = Blue) }
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webcal))) }
                        .onFailure {
                            copyToClipboard(context, feed)
                            Toasts.show(s.more.linkCopied)
                        }
                }) { Text(s.more.calendarOther, color = Blue) }
                TextButton(onClick = {
                    copyToClipboard(context, feed)
                    Toasts.show(s.more.linkCopied)
                }) { Text(s.more.copyLink, color = Blue) }
            }
        }
        Spacer(Modifier.height(12.dp))
        val openTodos = remember(profileTick) {
            ru.bgtu.schedule.data.TodoStore(context).all().count { !it.done }
        }
        val library = remember(profileTick) { ru.bgtu.schedule.data.listLibrary(context) }
        SoftCard(onClick = onOpenTasks) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    ProfileCardTitle(s.tasks, if (openTodos == 0) s.tasksEmpty else s.openTasksLine(openTodos))
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Muted)
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard(onClick = onOpenFiles) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    ProfileCardTitle(
                        s.files,
                        if (library.isEmpty()) s.filesHint else "${library.size} · ${s.formatBytes(library.sumOf { it.size })}",
                    )
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Muted)
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard {
            val absences = remember(profileTick) { AbsenceStore(context).all() }.groupBy { it.subject.ifBlank { "—" } }
            val limits = remember(profileTick) { AbsenceLimits(context) }
            ProfileCardTitle(s.absences, if (absences.isEmpty()) s.absencesEmpty else null)
            if (absences.isNotEmpty()) {
                absences.entries.sortedBy { it.key.lowercase() }.forEach { (subject, items) ->
                    val count = items.size
                    val limit = limits.get(subject)
                    val usedUp = limit != null && count >= limit
                    Text(subject, color = if (usedUp) Red else Ink, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                    Text(s.absenceCount(count), color = if (usedUp) Red else Blue, fontSize = 12.sp)
                    if (limit == null) {
                        TextButton(
                            onClick = {
                                limits.set(subject, 4)
                                profileTick++
                            },
                            contentPadding = PaddingValues(0.dp),
                        ) { Text(s.setLimit, color = Blue) }
                    } else {
                        val left = (limit - count).coerceAtLeast(0)
                        Text(
                            if (usedUp) s.limitUsed else s.skipsLeft(left),
                            color = if (usedUp) Red else Blue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                limits.set(subject, (limit - 1).coerceAtLeast(0))
                                profileTick++
                            }) { Text("−", color = Blue, fontSize = 18.sp) }
                            Text("${s.setLimit} $limit", color = Ink, fontSize = 13.sp)
                            TextButton(onClick = {
                                limits.set(subject, limit + 1)
                                profileTick++
                            }) { Text("+", color = Blue, fontSize = 18.sp) }
                            TextButton(onClick = {
                                limits.set(subject, null)
                                profileTick++
                            }) { Text(s.clearLimit, color = Muted) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard {
            val notes = remember(profileTick) { SubjectNotes(context).all().entries.sortedBy { it.key.lowercase() } }
            ProfileCardTitle(s.more.subjectNotesTitle, if (notes.isEmpty()) s.more.subjectNotesEmpty else null)
            notes.forEach { (subject, note) ->
                Text(subject, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                Text(note, color = Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard {
            val hiddenLessons = remember(profileTick) {
                HiddenStore(context).keys().mapNotNull { key -> group.lessons.firstOrNull { it.key == key } }
            }
            ProfileCardTitle(s.hiddenTitle, if (hiddenLessons.isEmpty()) s.hiddenEmpty else null)
            hiddenLessons.forEach { lesson ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Text(
                        "${s.dayName(lesson.dayName)} · ${lesson.start} · ${lesson.subject}",
                        color = Ink,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        HiddenStore(context).set(lesson.key, false)
                        profileTick++
                        ru.bgtu.schedule.widget.ScheduleWidget.refresh(context)
                        if (prefs.notifyEnabled()) ru.bgtu.schedule.notify.LessonAlerts.reschedule(context)
                    }) { Text(s.restore, color = Blue) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard {
            ProfileCardTitle(s.backupSave, s.backupHint)
            Row {
                TextButton(onClick = { saveBackup.launch("studrasp-backup-${LocalDate.now()}.json") }) { Text(s.backupSave, color = Blue) }
                TextButton(onClick = { loadBackup.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text(s.backupLoad, color = Blue) }
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard {
            ProfileCardTitle(s.contact, s.updatesInTelegram)
            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ag3r_software"))) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_telegram),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(s.telegram, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(BlueSoft)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = Blue,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(s.localData, color = Ink, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlineButton(s.resetData, onClick = { confirmReset = true }, color = Red)
        Spacer(Modifier.height(12.dp))
        SoftCard {
            val week = weekInfo(LocalDate.now(), bundle.semesterStart)
            ProfileCardTitle(s.weekHelpTitle)
            Text(
                s.weekNow(week.number, week.even),
                color = Blue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(s.weekHelp, color = Ink, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp))
            Text(s.fetchedLine, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            if (bundle.source.isNotBlank()) {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bundle.source)))
                }, contentPadding = PaddingValues(0.dp)) {
                    Text(s.openSite, color = Blue)
                }
            }
        }
        Text(
            "${s.brand} ${AppVersion.LABEL}" + if (ru.bgtu.schedule.BuildConfig.ADMIN) " · admin" else "",
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            color = Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onWhatsNew, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(s.more.whatsNewLink, color = Blue, fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
        pendingCrop?.let { source ->
            CropPhotoDialog(
                source = source,
                onCancel = {
                    source.recycle()
                    pendingCrop = null
                },
                onSave = { cropped ->
                    ru.bgtu.schedule.data.saveAvatar(context, cropped)
                    cropped.recycle()
                    source.recycle()
                    pendingCrop = null
                    avatarStamp++
                },
            )
        }
        if (confirmReset) {
            val library = remember { ru.bgtu.schedule.data.listLibrary(context) }
            var dropFiles by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { confirmReset = false },
                containerColor = Card,
                titleContentColor = Ink,
                textContentColor = Muted,
                title = { Text(s.resetTitle) },
                text = {
                    Column {
                        Text(s.resetText)
                        if (library.isNotEmpty()) {
                            Row(
                                Modifier
                                    .padding(top = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { dropFiles = !dropFiles },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = dropFiles,
                                    onCheckedChange = { dropFiles = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Red),
                                )
                                Text(
                                    s.resetFiles(library.size, ru.bgtu.schedule.data.formatBytes(library.sumOf { it.size })),
                                    color = Ink,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmReset = false
                        ru.bgtu.schedule.data.wipeUserData(context, dropFiles)
                        avatarStamp++
                        onReset()
                    }) { Text(s.reset, color = Red) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmReset = false }) { Text(s.cancel, color = Muted) }
                },
            )
        }
    }
}

@Composable
fun NotesOverlay(
    bundle: ScheduleBundle,
    group: Group,
    onClose: () -> Unit,
    onLesson: (Lesson, LocalDate) -> Unit = { _, _ -> },
) {
    val s = S
    val today = LocalDate.now()
    val info = weekInfo(today, bundle.semesterStart)
    val items = upcomingLessons(
        group, today, LocalTime.now(), bundle.semesterStart,
        HiddenStore(LocalContext.current).keys(),
        overrides = DayOverrides(LocalContext.current),
        limit = 8,
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = LocalBottomInset.current + 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.reminders, modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = s.close, tint = Ink)
            }
        }
        Text(s.remindersHint, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        SoftCard {
            Text(s.studyWeekLine(info.number, info.even), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(s.weekNow(info.number, info.even), color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(10.dp))
        if (items.isEmpty()) {
            Text(s.noUpcoming, color = Muted, fontSize = 14.sp)
        } else {
            items.forEach { (day, lesson) ->
                LessonCard(lesson, caption = "${s.whenLabel(day, today)} · ${lesson.start}") {
                    onClose()
                    onLesson(lesson, day)
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ProfileAvatar(stamp: Int, name: String, onPick: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(stamp) {
        val file = ru.bgtu.schedule.data.avatarFile(context)
        if (!file.exists()) null else BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
    }
    Box(
        Modifier
            .size(104.dp)
            .clip(CircleShape)
            .background(BlueSoft)
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = S.photo, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(
                name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "С",
                color = Blue,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
            )
        }
    }
}

@Composable
private fun ProfileCardTitle(title: String, hint: String? = null) {
    Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    if (hint != null) Text(hint, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun SettingSwitch(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Ink, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(hint, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Blue,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = Line,
                uncheckedThumbColor = Color.White,
                uncheckedBorderColor = Line,
            ),
        )
    }
}

@Composable
fun FilesOverlay(group: Group, onClose: () -> Unit) {
    val s = S
    val context = LocalContext.current
    val tags = remember { ru.bgtu.schedule.data.FileTags(context) }
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf(ru.bgtu.schedule.data.listLibrary(context)) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var tagging by remember { mutableStateOf<ru.bgtu.schedule.data.StoredFile?>(null) }
    var removing by remember { mutableStateOf<ru.bgtu.schedule.data.StoredFile?>(null) }
    var tagQuery by remember { mutableStateOf("") }
    var tagTick by remember { mutableIntStateOf(0) }
    val lessons = remember(group.id) {
        group.lessons.distinctBy { it.key }.sortedWith(compareBy({ it.day }, { it.start }))
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val copied = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri -> runCatching { ru.bgtu.schedule.data.importToLibrary(context, uri) }.getOrNull() }
            }
            items = ru.bgtu.schedule.data.listLibrary(context)
            busy = false
            message = if (copied.isEmpty()) {
                s.copyFailed
            } else {
                s.filesCopied(copied.joinToString { it.name })
            }
        }
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
            Text(s.files, modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = s.close, tint = Ink)
            }
        }
        Text(
            s.filesBody,
            color = Muted,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { picker.launch(arrayOf("*/*")) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
        ) { Text(if (busy) s.copyingFiles else s.addFiles) }
        Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) {
            SoftCard { Text(if (busy) s.copyingFiles else s.empty, color = Muted) }
        } else {
            items.forEach { stored ->
                val linked = remember(tagTick, stored.file.name) { tags.lessonKeys(stored.file.name) }
                SoftCard {
                    Text(stored.file.name, color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(ru.bgtu.schedule.data.formatBytes(stored.size), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    linked.mapNotNull { key -> lessons.firstOrNull { it.key == key } }.forEach { lesson ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${s.dayName(lesson.dayName)} · ${lesson.start} · ${lesson.subject}",
                                color = Blue,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                tags.link(stored.file.name, lesson.key, false)
                                tagTick++
                            }) { Text(s.removeLesson, color = Muted, fontSize = 12.sp) }
                        }
                    }
                    Row {
                        TextButton(onClick = { openStored(context, stored.file) }) { Text(s.open, color = Blue) }
                        TextButton(onClick = {
                            tagQuery = ""
                            tagging = stored
                        }) { Text(s.attachToLesson, color = Blue) }
                        TextButton(onClick = { removing = stored }) { Text(s.delete, color = Red) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        removing?.let { stored ->
            AlertDialog(
                onDismissRequest = { removing = null },
                containerColor = Card,
                titleContentColor = Ink,
                textContentColor = Muted,
                title = { Text(s.delete) },
                text = { Text(stored.file.name) },
                dismissButton = { TextButton(onClick = { removing = null }) { Text(s.cancel, color = Muted) } },
                confirmButton = {
                    TextButton(onClick = {
                        removing = null
                        tags.forget(stored.file.name)
                        stored.file.delete()
                        items = ru.bgtu.schedule.data.listLibrary(context)
                    }) { Text(s.delete, color = Red) }
                },
            )
        }
        val target = tagging
        if (target != null) {
            val chosen = tags.lessonKeys(target.file.name)
            val filtered = lessons.filter { lesson ->
                val q = tagQuery.trim()
                q.isBlank() || lesson.subject.contains(q, true) || lesson.dayName.contains(q, true) || lesson.start.contains(q)
            }
            Dialog(onDismissRequest = { tagging = null }) {
                Column(
                    Modifier
                        .padding(16.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Card)
                        .padding(16.dp),
                ) {
                    Text(s.attachToLesson, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(target.file.name, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tagQuery,
                        onValueChange = { tagQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(s.subjectOrDay) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                        if (filtered.isEmpty()) {
                            Text(s.nothingFound, color = Muted)
                        }
                        filtered.forEach { lesson ->
                            val on = lesson.key in chosen
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        tags.link(target.file.name, lesson.key, !on)
                                        tagTick++
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(lesson.subject, color = Ink, fontWeight = FontWeight.SemiBold)
                                    Text("${s.dayName(lesson.dayName)} · ${lesson.start}", color = Muted, fontSize = 12.sp)
                                }
                                if (on) Text(s.attached, color = Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    TextButton(onClick = { tagging = null }, modifier = Modifier.align(Alignment.End)) {
                        Text(s.done, color = Blue)
                    }
                }
            }
        }
        val notice = message
        if (notice != null) {
            AlertDialog(
                onDismissRequest = { message = null },
                containerColor = Card,
                titleContentColor = Ink,
                textContentColor = Muted,
                title = { Text(s.files) },
                text = { Text(notice) },
                confirmButton = {
                    TextButton(onClick = { message = null }) { Text(s.ok, color = Blue) }
                },
            )
        }
    }
}

private fun openStored(context: android.content.Context, file: java.io.File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(Intent.createChooser(intent, copyOf(ru.bgtu.schedule.data.Prefs(context).language()).open)) }
}

@Composable
fun LessonDialog(
    lesson: Lesson,
    group: Group,
    date: LocalDate,
    initialNote: String,
    onNote: (String) -> Unit,
    onTasksChanged: () -> Unit,
    onRefresh: () -> Unit = {},
    onClose: () -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomGap = LocalBottomInset.current + 40.dp
    val maxSheet = (screenHeight - topInset - bottomGap - 20.dp).coerceAtLeast(280.dp)
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.setBackgroundDrawable(
                ColorDrawable(android.graphics.Color.TRANSPARENT),
            )
        }
        val progress = remember { Animatable(0f) }
        val s = S
        val context = LocalContext.current
        var note by remember(lesson.key, date) { mutableStateOf(initialNote) }
        val subjectNotes = remember { SubjectNotes(context) }
        var subjectNote by remember(lesson.subject) { mutableStateOf(subjectNotes.get(lesson.subject)) }
        val hiddenStore = remember { HiddenStore(context) }
        var hidden by remember(lesson.key) { mutableStateOf(hiddenStore.has(lesson.key)) }
        val teacherFavorites = remember { FavoriteTeachers(context) }
        var starred by remember(lesson.teacher) { mutableStateOf(teacherFavorites.has(lesson.teacher)) }
        var ratingFor by remember(lesson.key) { mutableStateOf<String?>(null) }
        val absences = remember { AbsenceStore(context) }
        var missed by remember(lesson.key, date) { mutableStateOf(absences.has(date.toString(), lesson.key)) }
        val dayOverrides = remember { DayOverrides(context) }
        var dayChange by remember(lesson.key, date) { mutableStateOf(dayOverrides.find(date.toString(), lesson.key)) }
        var shownRoom by remember(lesson.key, date) { mutableStateOf(lesson.room) }
        var roomDraft by remember(lesson.key, date) { mutableStateOf(dayChange?.room ?: lesson.room) }
        val taskStore = remember { ru.bgtu.schedule.data.TodoStore(context) }
        var taskTick by remember { mutableIntStateOf(0) }
        var editingTask by remember { mutableStateOf(false) }
        var openTask by remember { mutableStateOf<ru.bgtu.schedule.data.TodoItem?>(null) }
        val lessonTasks = remember(taskTick, lesson.key) { taskStore.forLesson(lesson.key) }
        val tags = remember { ru.bgtu.schedule.data.FileTags(context) }
        var fileTick by remember { mutableIntStateOf(0) }
        val attached = remember(fileTick, lesson.key) { tags.filesFor(context, lesson.key) }
        val attachPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
            val copied = uris.mapNotNull { uri -> runCatching { ru.bgtu.schedule.data.importToLibrary(context, uri) }.getOrNull() }
            copied.forEach { tags.link(it.name, lesson.key, true) }
            fileTick++
            Toasts.show(if (copied.isEmpty()) s.copyFailed else s.filesCopied(copied.joinToString { it.name }))
        }
        LaunchedEffect(lesson.key) { progress.animateTo(1f, tween(280)) }
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = bottomGap)
                    .graphicsLayer {
                        alpha = progress.value
                        translationY = (1f - progress.value) * 70f
                    }
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .heightIn(max = maxSheet)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Card)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 22.dp, end = 22.dp, top = 16.dp),
                ) {
                    Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(CircleShape).background(Line))
                    Spacer(Modifier.height(16.dp))
                    Text(s.weekBadge(lesson.week), color = accent(lesson.type), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(lesson.subject, color = Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Spacer(Modifier.height(14.dp))
                    DetailRow(
                        s.time,
                        listOf(lesson.start, lesson.end).filter { it.isNotBlank() }.joinToString("–").ifBlank { lesson.time.ifBlank { "—" } },
                    )
                    pairNumber(lesson.start)?.let { DetailRow(s.pair, it.toString()) }
                    s.lessonType(lesson.type).takeIf { it.isNotBlank() }?.let { DetailRow(s.type, it) }
                    s.roomName(shownRoom).takeIf { it.isNotBlank() }?.let { DetailRow(s.room, it) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .then(if (lesson.teacher.isNotBlank()) Modifier.clickable { ratingFor = lesson.teacher } else Modifier),
                        ) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                Text(s.teacher, color = Muted, fontSize = 12.sp)
                                Text(
                                    lesson.teacher.ifBlank { "—" },
                                    color = if (lesson.teacher.isNotBlank()) Blue else Ink,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                        if (lesson.teacher.isNotBlank()) {
                            TeacherStar(starred) {
                                starred = !starred
                                teacherFavorites.set(lesson.teacher, starred)
                            }
                        }
                    }
                    if (lesson.teacher.isNotBlank()) {
                        val people = remember(lesson.teacher) { ru.bgtu.schedule.data.splitTeachers(lesson.teacher) }
                        people.forEach { person ->
                            if (people.size > 1) Text(person, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                            RatingLine(person) { ratingFor = lesson.teacher }
                        }
                    }
                    DetailRow(s.day, s.formatLongDateCap(date))
                    Spacer(Modifier.height(10.dp))
                    Text(s.tasksForLesson, color = Ink, fontWeight = FontWeight.SemiBold)
                    if (lessonTasks.isEmpty()) {
                        Text(s.noTasksForLesson, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    } else {
                        lessonTasks.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = item.done,
                                    onCheckedChange = {
                                        taskStore.upsert(item.copy(done = !item.done))
                                        taskTick++
                                        onTasksChanged()
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Blue, uncheckedColor = Muted, checkmarkColor = Color.White),
                                )
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { openTask = item },
                                ) {
                                    Text(
                                        item.title,
                                        color = Ink,
                                        fontSize = 14.sp,
                                        textDecoration = if (item.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                    )
                                    Text(deadlineText(s, item), color = if (item.isOverdue()) Red else Muted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    TextButton(onClick = { editingTask = true }, contentPadding = PaddingValues(0.dp)) {
                        Text(s.addTask, color = Blue)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(s.files, color = Ink, fontWeight = FontWeight.SemiBold)
                    if (attached.isEmpty()) {
                        Text(s.nothingAttached, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    } else {
                        attached.forEach { stored ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { openStored(context, stored.file) },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        stored.file.name,
                                        color = Blue,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                TextButton(onClick = {
                                    tags.link(stored.file.name, lesson.key, false)
                                    fileTick++
                                }) { Text(s.removeLesson, color = Muted, fontSize = 13.sp) }
                            }
                        }
                    }
                    TextButton(
                        onClick = { attachPicker.launch(arrayOf("*/*")) },
                        contentPadding = PaddingValues(0.dp),
                    ) { Text(s.attachFile, color = Blue) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subjectNote,
                        onValueChange = {
                            subjectNote = it
                            subjectNotes.set(lesson.subject, it)
                            onRefresh()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(s.subjectNote) },
                        placeholder = { Text(s.subjectNoteHint) },
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors(),
                        minLines = 2,
                    )
                    Row {
                        TextButton(onClick = {
                            hidden = !hidden
                            hiddenStore.set(lesson.key, hidden)
                            onRefresh()
                            ru.bgtu.schedule.widget.ScheduleWidget.refresh(context)
                            if (ru.bgtu.schedule.data.Prefs(context).notifyEnabled()) {
                                ru.bgtu.schedule.notify.LessonAlerts.reschedule(context)
                            }
                        }) { Text(if (hidden) s.attendLesson else s.hideLesson, color = Blue) }
                        TextButton(onClick = {
                            absences.toggle(date.toString(), lesson.key, lesson.subject)
                            missed = absences.has(date.toString(), lesson.key)
                            onRefresh()
                        }) { Text(if (missed) s.clearAbsence else s.wasAbsent, color = Blue) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(s.todayChange, color = Ink, fontWeight = FontWeight.SemiBold)
                    run {
                        Text(s.todayOnly, color = Muted, fontSize = 13.sp)
                        OutlinedTextField(
                            value = roomDraft,
                            onValueChange = { roomDraft = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            label = { Text(s.todayRoom) },
                            placeholder = { Text(s.roomExample) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors(),
                        )
                        TextButton(onClick = {
                            dayOverrides.set(date.toString(), lesson.key, cancelled = false, room = roomDraft)
                            dayChange = dayOverrides.find(date.toString(), lesson.key)
                            shownRoom = roomDraft.trim().ifBlank { lesson.room }
                            onRefresh()
                            ru.bgtu.schedule.widget.ScheduleWidget.refresh(context)
                            if (ru.bgtu.schedule.data.Prefs(context).notifyEnabled()) {
                                ru.bgtu.schedule.notify.LessonAlerts.reschedule(context)
                            }
                        }, contentPadding = PaddingValues(0.dp)) { Text(s.saveRoom, color = Blue) }
                        TextButton(onClick = {
                            dayOverrides.set(date.toString(), lesson.key, cancelled = true, room = null)
                            onRefresh()
                            ru.bgtu.schedule.widget.ScheduleWidget.refresh(context)
                            if (ru.bgtu.schedule.data.Prefs(context).notifyEnabled()) {
                                ru.bgtu.schedule.notify.LessonAlerts.reschedule(context)
                            }
                            onClose()
                        }, contentPadding = PaddingValues(0.dp)) { Text(s.cancelToday, color = Red) }
                        if (dayChange != null) {
                            TextButton(onClick = {
                                dayOverrides.clear(date.toString(), lesson.key)
                                dayChange = null
                                shownRoom = group.lessons.firstOrNull { it.key == lesson.key }?.room ?: lesson.room
                                roomDraft = shownRoom
                                onRefresh()
                                ru.bgtu.schedule.widget.ScheduleWidget.refresh(context)
                                if (ru.bgtu.schedule.data.Prefs(context).notifyEnabled()) {
                                    ru.bgtu.schedule.notify.LessonAlerts.reschedule(context)
                                }
                            }, contentPadding = PaddingValues(0.dp)) { Text(s.officialAgain, color = Blue) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = {
                            note = it
                            onNote(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(s.note) },
                        placeholder = { Text(s.noteHint) },
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors(),
                        minLines = 2,
                    )
                }
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 18.dp)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                ) { Text(s.close) }
                val teacherBundle = LocalBundle.current
                ratingFor?.let { person ->
                    if (teacherBundle != null) {
                        TeacherDialog(teacherBundle, person, onClose = { ratingFor = null }, onFavoriteChanged = {
                            starred = teacherFavorites.has(lesson.teacher)
                        })
                    }
                }
                if (editingTask) {
                    TodoEditorDialog(
                        group = group,
                        initial = null,
                        presetLesson = lesson,
                        onDismiss = { editingTask = false },
                        onSave = { item ->
                            taskStore.upsert(item)
                            taskTick++
                            onTasksChanged()
                            editingTask = false
                        },
                    )
                }
                openTask?.let { current ->
                    TodoEditorDialog(
                        group = group,
                        initial = current,
                        presetLesson = lesson,
                        onDismiss = { openTask = null },
                        onSave = { item ->
                            taskStore.upsert(item)
                            taskTick++
                            onTasksChanged()
                            openTask = null
                        },
                        onDelete = {
                            taskStore.remove(current.id)
                            taskTick++
                            onTasksChanged()
                            openTask = null
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LessonCard(
    lesson: Lesson,
    caption: String? = null,
    note: String = "",
    tasksTick: Int = 0,
    missed: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val s = S
    val context = LocalContext.current
    val tick = tasksTick
    val color = accent(lesson.type)
    val hasTask = remember(tick, lesson.key) {
        ru.bgtu.schedule.data.TodoStore(context).hasOpen(lesson.key)
    }
    val fade by animateFloatAsState(if (missed) 0.5f else 1f, tween(220), label = "missedFade")
    Box(Modifier.fillMaxWidth()) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .graphicsLayer { alpha = fade }
            .shadow(8.dp, Shape, ambientColor = Color(0x1420336B), spotColor = Color(0x1020336B))
            .clip(Shape)
            .background(Card)
            .then(
                if (onLongClick == null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                },
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.widthIn(min = 64.dp, max = 92.dp)) {
            Text(lesson.start.ifBlank { "—" }, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Text(lesson.end, color = Muted, fontSize = 12.sp, maxLines = 1)
            pairNumber(lesson.start)?.let {
                Text(s.pairLabel(it), color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
        Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (caption != null) {
                Text(caption, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(end = if (missed) 64.dp else 0.dp))
            }
            Text(
                lesson.subject,
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.padding(end = if (missed && caption == null) 64.dp else 0.dp),
            )
            val meta = listOf(s.lessonType(lesson.type), s.roomName(lesson.room)).filter { it.isNotBlank() }.joinToString(" · ")
            if (meta.isNotBlank()) Text(meta, color = Muted, fontSize = 12.sp)
            if (lesson.teacher.isNotBlank()) {
                Text(lesson.teacher, color = Ink.copy(alpha = 0.75f), fontSize = 12.sp)
            }
            Text(s.weekBadge(lesson.week), color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
            val subjectNote = SubjectNotes(context).get(lesson.subject)
            if (note.isNotBlank() || subjectNote.isNotBlank()) {
                Text(
                    listOf(note, subjectNote).filter { it.isNotBlank() }.joinToString("\n"),
                    color = Blue,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (ru.bgtu.schedule.data.FileTags(LocalContext.current).has(lesson.key)) {
                Text(s.hasFile, color = Blue, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
            if (hasTask) {
                Text(s.hasTask, color = Blue, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
    if (missed) {
        Text(
            s.missedBadge,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Red)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
    }
}

@Composable
internal fun SoftCard(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, Shape, ambientColor = Color(0x1420336B), spotColor = Color(0x1020336B))
            .clip(Shape)
            .background(Card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun StatCard(modifier: Modifier, title: String, value: String, hint: String) {
    Column(
        modifier
            .shadow(8.dp, Shape, ambientColor = Color(0x1420336B), spotColor = Color(0x1020336B))
            .clip(Shape)
            .background(Card)
            .padding(16.dp),
    ) {
        Text(title, color = Muted, fontSize = 13.sp)
        Text(value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        Text(hint, color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Pill(text: String) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
internal fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) Blue else Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (selected) Color.White else Ink,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun DayChip(date: LocalDate, selected: Boolean, onClick: () -> Unit) {
    val today = date == LocalDate.now()
    Column(
        Modifier
            .widthIn(min = 50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Blue else Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(S.dayShort[date.dayOfWeek.value - 1], color = if (selected) Color.White.copy(alpha = 0.8f) else Muted, fontSize = 11.sp)
        Text(
            date.dayOfMonth.toString(),
            color = when {
                selected -> Color.White
                today -> Blue
                else -> Ink
            },
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(value, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
internal fun DetailRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(value, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun HiddenSection(
    open: Boolean,
    onToggle: () -> Unit,
    skipped: List<Lesson>,
    cancelled: List<Lesson>,
    onRestoreSkipped: (Lesson) -> Unit,
    onRestoreCancelled: (Lesson) -> Unit,
) {
    val s = S
    val turn by animateFloatAsState(if (open) 180f else 0f, tween(240), label = "hiddenChevron")
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(s.hiddenSection(skipped.size + cancelled.size), color = Muted, fontSize = 12.sp)
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.padding(start = 2.dp).size(16.dp).graphicsLayer { rotationZ = turn },
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(tween(260)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(220)) + fadeOut(tween(140)),
        ) {
            Column(Modifier.padding(horizontal = 6.dp)) {
                if (skipped.isNotEmpty()) HiddenGroup(s.hideLesson, skipped, onRestoreSkipped)
                if (cancelled.isNotEmpty()) HiddenGroup(s.goneToday, cancelled, onRestoreCancelled)
            }
        }
    }
}

@Composable
private fun HiddenGroup(title: String, lessons: List<Lesson>, onRestore: (Lesson) -> Unit) {
    val s = S
    Text(title, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
    lessons.forEach { lesson ->
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                listOf(lesson.start, lesson.subject.ifBlank { s.lessonFallback }).filter { it.isNotBlank() }.joinToString("  "),
                color = Muted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onRestore(lesson) }) { Text(s.restore, color = Blue, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun EmptyDay() {
    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(S.noLessons, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(S.noLessonsHint, color = Muted, fontSize = 13.sp)
        }
    }
}

private val HEADING_NAME = Regex("""[А-ЯЁA-Z][а-яёa-z\-]+(?:\s+[А-ЯЁA-Z]?\.){1,2}""")

internal fun teacherHeading(raw: String): String {
    if (HEADING_NAME.containsMatchIn(raw)) return ru.bgtu.schedule.data.splitTeachers(raw).joinToString("\n")
    val people = raw.split(Regex("[,;/]|\\s+и\\s+"))
    return people.map { formatTeacher(it) }.filter { it.isNotBlank() }.joinToString("\n").ifBlank { raw.trim() }
}

private fun formatTeacher(raw: String): String {
    val words = raw.trim().split(Regex("\\s+")).filter { it.any(Char::isLetter) }
    if (words.isEmpty()) return ""
    val surname = words.first().trim('.')
    val letters = words.drop(1).flatMap { word ->
        val core = word.filter(Char::isLetter)
        when {
            core.isEmpty() -> emptyList()
            core.length > 2 -> listOf(core.first().uppercaseChar().toString())
            else -> core.map { it.uppercaseChar().toString() }
        }
    }
    return if (letters.isEmpty()) surname else "$surname ${letters.joinToString(" ") { "$it." }}"
}

@Composable
internal fun TeacherStar(on: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            if (on) Icons.Filled.Star else Icons.Filled.StarBorder,
            contentDescription = if (on) S.removeFavorite else S.addFavorite,
            tint = if (on) Blue else Muted,
        )
    }
}

internal fun teacherFollowLine(copy: Copy, today: LocalDate, item: TeacherNext): String {
    val place = listOf(item.lesson.subject, item.groupId, copy.roomName(item.lesson.room)).filter { it.isNotBlank() }.joinToString(" · ")
    return "${copy.whenLabel(item.day, today)} · ${item.lesson.start} · $place"
}

@Composable
private fun TaskPreviewLine(item: TodoItem) {
    Text(item.title, color = Ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
    Text(deadlineText(S, item), color = Muted, fontSize = 12.sp)
}

@Composable
internal fun Appear(index: Int, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index.coerceAtMost(8) * 36).toLong())
        progress.animateTo(1f, tween(320))
    }
    Box(
        Modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 16f
        },
    ) { content() }
}

@Composable
internal fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Blue,
    unfocusedBorderColor = Line,
    focusedContainerColor = Card,
    unfocusedContainerColor = Card,
    cursorColor = Blue,
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
)

@Composable
private fun accent(type: String): Color = when (typeColorKey(type)) {
    "lab" -> Teal
    "practice" -> Amber
    "online" -> Violet
    "lecture" -> Blue
    else -> Color(0xFF5C6B8A)
}

@Composable
private fun facultyColor(short: String): Color = when (short) {
    "ФИТ" -> Blue
    "МТФ" -> Color(0xFF3B6FE0)
    "УНИТ" -> Teal
    "УНТИ" -> Amber
    "ФОЦЭ" -> Violet
    "ФЭЭ" -> Color(0xFF2E8CFF)
    else -> Blue
}

private fun prettyLevel(level: String): String =
    level.replaceFirstChar { it.uppercase() }.ifBlank { "—" }

private fun liveLine(copy: Copy, status: LiveStatus): String {
    return when (status) {
        is LiveStatus.Off -> if (copy.code == "en") status.holiday.nameEn else status.holiday.nameRu
        is LiveStatus.InClass -> listOf(
            copy.liveInClass,
            status.lesson.subject,
            status.lesson.room,
            copy.leftPhrase(status.minutesLeft),
        ).filter { it.isNotBlank() }.joinToString(" · ")
        is LiveStatus.Soon -> listOf(
            "${copy.liveSoon} ${copy.untilPhrase(status.minutesUntil)}",
            status.lesson.subject,
            status.lesson.room,
        ).filter { it.isNotBlank() }.joinToString(" · ")
        is LiveStatus.Window -> listOf(
            "${copy.liveWindow} ${copy.span(status.minutes)}",
            status.next.subject,
            status.next.start,
        ).filter { it.isNotBlank() }.joinToString(" · ")
        is LiveStatus.Done -> {
            val next = status.next
            if (next == null) copy.liveDone else "${copy.liveDone}. ${copy.liveNext} ${next.second.start} · ${next.second.subject}"
        }
        LiveStatus.None -> copy.liveNone
    }
}

private fun shareText(
    copy: Copy,
    group: Group,
    date: LocalDate,
    lessons: List<Lesson>,
    holiday: ru.bgtu.schedule.data.Holiday?,
): String {
    val head = "${copy.formatLongDateCap(date)}\n${group.id}"
    if (holiday != null) {
        val name = if (copy.code == "en") holiday.nameEn else holiday.nameRu
        return "$head\n$name"
    }
    if (lessons.isEmpty()) return "$head\n${copy.noLessonsThatDay}"
    return "$head\n${shareBody(lessons)}"
}

private fun shareBody(lessons: List<Lesson>): String {
    if (lessons.isEmpty()) return "—"
    return lessons.joinToString("\n") { lesson ->
        listOf(lesson.time.ifBlank { lesson.start }, lesson.subject, lesson.room).filter { it.isNotBlank() }.joinToString(" · ")
    }
}

private fun exportWeek(
    context: android.content.Context,
    copy: Copy,
    group: Group,
    semesterStart: java.time.LocalDate,
    date: java.time.LocalDate,
    hidden: Set<String>,
): String {
    val added = WeekCalendar.export(context, group, semesterStart, date, hidden)
    return when {
        added < 0 -> copy.exportNoCalendar
        added == 0 -> copy.exportEmpty
        else -> copy.exported(added)
    }
}

@Composable
private fun BothColumns(
    left: List<Lesson>,
    right: List<Lesson>,
    noteOf: (String) -> String,
    onLesson: (Lesson) -> Unit,
    date: LocalDate,
    tasksTick: Int,
    onChanged: () -> Unit,
    missed: Set<String>,
) {
    val s = S
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WeekColumn(s.numeratorColumn, left, Modifier.weight(1f), noteOf, onLesson, date, tasksTick, onChanged, missed)
        WeekColumn(s.denominatorColumn, right, Modifier.weight(1f), noteOf, onLesson, date, tasksTick, onChanged, missed)
    }
}

@Composable
private fun WeekColumn(
    title: String,
    lessons: List<Lesson>,
    modifier: Modifier,
    noteOf: (String) -> String,
    onLesson: (Lesson) -> Unit,
    date: LocalDate,
    tasksTick: Int,
    onChanged: () -> Unit,
    missed: Set<String>,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        if (lessons.isEmpty()) {
            Text(S.noLessons, color = Muted, fontSize = 12.sp)
        } else {
            lessons.forEach { lesson ->
                ScheduleLesson(
                    lesson = lesson,
                    date = date,
                    note = noteOf(lesson.key),
                    tasksTick = tasksTick,
                    onOpen = { onLesson(lesson) },
                    onChanged = onChanged,
                    missed = absenceMark(date, lesson) in missed,
                )
            }
        }
    }
}

@Composable
private fun ScheduleLesson(
    lesson: Lesson,
    date: LocalDate,
    note: String,
    tasksTick: Int,
    onOpen: () -> Unit,
    onChanged: () -> Unit,
    caption: String? = null,
    missed: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        LessonCard(
            lesson,
            caption = caption,
            note = note,
            tasksTick = tasksTick,
            missed = missed,
            onLongClick = { open = true },
            onClick = onOpen,
        )
        if (open) {
            LessonQuickMenu(
                lesson = lesson,
                date = date,
                onDismiss = { open = false },
                onChanged = {
                    open = false
                    onChanged()
                },
            )
        }
    }
}

@Composable
private fun LessonQuickMenu(
    lesson: Lesson,
    date: LocalDate,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
) {
    val s = S
    val context = LocalContext.current
    val accent = accent(lesson.type)
    val rise = remember { Animatable(0.94f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.setBackgroundDrawable(
                ColorDrawable(android.graphics.Color.TRANSPARENT),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .graphicsLayer {
                        scaleX = rise.value
                        scaleY = rise.value
                        alpha = rise.value
                    }
                    .shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x3320336B), spotColor = Color(0x2420336B))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Card)
                    .padding(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 10.dp),
            ) {
                Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accent),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            lesson.subject.ifBlank { s.lessonFallback },
                            color = Ink,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOf(lesson.start, lesson.end).filter { it.isNotBlank() }.joinToString("–"),
                            color = Muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val hiddenStore = HiddenStore(context)
                val hidden = hiddenStore.has(lesson.key)
                QuickAction(
                    icon = if (hidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    label = if (hidden) s.attendLesson else s.hideLesson,
                ) {
                    hiddenStore.set(lesson.key, !hidden)
                    refreshAfterLessonChange(context)
                    onChanged()
                }
                val absences = AbsenceStore(context)
                val missed = absences.has(date.toString(), lesson.key)
                QuickAction(
                    icon = Icons.Filled.EventBusy,
                    label = if (missed) s.clearAbsence else s.wasAbsent,
                ) {
                    absences.toggle(date.toString(), lesson.key, lesson.subject)
                    onChanged()
                }
                val dayOverrides = DayOverrides(context)
                if (dayOverrides.find(date.toString(), lesson.key) != null) {
                    QuickAction(icon = Icons.AutoMirrored.Filled.Undo, label = s.officialAgain) {
                        dayOverrides.clear(date.toString(), lesson.key)
                        refreshAfterLessonChange(context)
                        onChanged()
                    }
                }
                QuickAction(
                    icon = Icons.Filled.Close,
                    label = s.goneToday,
                    tint = Red,
                    well = Color(0x1AE15B64),
                ) {
                    dayOverrides.set(date.toString(), lesson.key, cancelled = true, room = null)
                    refreshAfterLessonChange(context)
                    onChanged()
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Blue,
    well: Color = BlueSoft,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(well),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

private fun absenceMark(date: LocalDate, lesson: Lesson) = "$date|${lesson.key}"

private fun refreshAfterLessonChange(context: android.content.Context) {
    ru.bgtu.schedule.widget.ScheduleWidget.refresh(context)
    if (ru.bgtu.schedule.data.Prefs(context).notifyEnabled()) {
        ru.bgtu.schedule.notify.LessonAlerts.reschedule(context)
    }
}

private fun upcomingLessons(
    group: Group,
    today: LocalDate,
    now: LocalTime,
    semesterStart: LocalDate,
    hidden: Set<String> = emptySet(),
    overrides: DayOverrides? = null,
    limit: Int = 4,
): List<Pair<LocalDate, Lesson>> {
    val result = mutableListOf<Pair<LocalDate, Lesson>>()
    for (offset in 0..7) {
        val day = today.plusDays(offset.toLong())
        if (Holidays.on(day) != null) continue
        val lessons = shownLessons(group, day.dayOfWeek.value, day, semesterStart, "auto", hidden, overrides)
        for (lesson in lessons) {
            if (offset == 0) {
                val start = parseTime(lesson.start)
                if (start != null && start.isBefore(now.minusMinutes(15))) continue
            }
            result += day to lesson
            if (result.size >= limit) return result
        }
    }
    return result
}
