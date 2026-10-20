package ru.bgtu.schedule.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.bgtu.schedule.data.Faculty
import ru.bgtu.schedule.data.Group
import ru.bgtu.schedule.data.Lesson
import ru.bgtu.schedule.data.Prefs
import ru.bgtu.schedule.data.ScheduleRepository
import ru.bgtu.schedule.notify.LessonAlerts
import java.time.LocalDate
import kotlin.math.hypot
import androidx.compose.ui.platform.LocalContext

private class ShellUi {
    var tab by mutableIntStateOf(0)
    var selectedDate by mutableStateOf(LocalDate.now().toString())
    var filter by mutableStateOf("auto")
    var reminders by mutableStateOf(false)
    var files by mutableStateOf(false)
    var opened by mutableStateOf<Lesson?>(null)
    var openedDate by mutableStateOf(LocalDate.now())
    var notesVersion by mutableIntStateOf(0)
    var tasks by mutableStateOf(false)
    var tasksVersion by mutableIntStateOf(0)
}

private data class ThemeReveal(val toDark: Boolean, val origin: Offset)

private class CircularRevealShape(
    private val origin: Offset,
    private val radius: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    origin.x - radius,
                    origin.y - radius,
                    origin.x + radius,
                    origin.y + radius,
                ),
            )
        }
        return Outline.Generic(path)
    }
}

private data class TabSpec(val label: String, val icon: ImageVector)

@Composable
fun BgtuApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context.applicationContext) }
    var dark by rememberSaveable { mutableStateOf(prefs.darkTheme()) }
    var lang by rememberSaveable { mutableStateOf(prefs.language()) }
    var splash by rememberSaveable { mutableStateOf(true) }
    var bundle by remember { mutableStateOf<ru.bgtu.schedule.data.ScheduleBundle?>(null) }
    var failed by remember { mutableStateOf(false) }
    var groupId by rememberSaveable { mutableStateOf(prefs.groupId()) }
    var studentName by rememberSaveable { mutableStateOf(prefs.name()) }
    var picking by rememberSaveable { mutableStateOf(groupId.isNullOrBlank()) }
    var pickingFriend by rememberSaveable { mutableStateOf(false) }
    var onboarded by rememberSaveable { mutableStateOf(prefs.onboarded()) }
    var onboardPage by rememberSaveable { mutableIntStateOf(0) }
    var reveal by remember { mutableStateOf<ThemeReveal?>(null) }
    var rootInWindow by remember { mutableStateOf(Offset.Zero) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    val shellUi = remember { ShellUi() }

    LaunchedEffect(Unit) {
        val started = System.currentTimeMillis()
        val loaded = withContext(Dispatchers.IO) {
            runCatching { ScheduleRepository.load(context.applicationContext) }.getOrNull()
        }
        val wait = 1500L - (System.currentTimeMillis() - started)
        if (wait > 0) delay(wait)
        if (loaded == null) failed = true else bundle = loaded
        splash = false
    }

    LaunchedEffect(groupId, splash) {
        if (!splash) {
            ru.bgtu.schedule.widget.ScheduleWidget.refresh(context.applicationContext)
            withContext(Dispatchers.IO) {
                ru.bgtu.schedule.notify.DeadlineAlerts.reschedule(context.applicationContext)
                if (prefs.notifyEnabled()) LessonAlerts.reschedule(context.applicationContext)
            }
        }
    }

    fun beginTheme(toDark: Boolean, centerInWindow: Offset) {
        if (reveal != null || toDark == dark) return
        reveal = ThemeReveal(toDark, centerInWindow - rootInWindow)
    }

    fun applyLanguage(code: String) {
        if (code == lang) return
        lang = code
        prefs.setLanguage(code)
        ru.bgtu.schedule.widget.ScheduleWidget.refresh(context.applicationContext)
    }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                rootInWindow = it.positionInWindow()
                rootSize = it.size
            },
    ) {
        AppLayer(
            dark = dark,
            splash = splash,
            failed = failed,
            bundle = bundle,
            picking = picking,
            groupId = groupId,
            studentName = studentName,
            prefs = prefs,
            shellUi = shellUi,
            lang = lang,
            windows = true,
            onboarded = onboarded,
            onboardPage = onboardPage,
            pickingFriend = pickingFriend,
            onPage = { onboardPage = it },
            onFinishOnboarding = {
                if (lang == null) applyLanguage("ru")
                prefs.setOnboarded(true)
                onboarded = true
            },
            onPickFriend = { id ->
                prefs.setFriendGroup(id)
                pickingFriend = false
                shellUi.notesVersion++
            },
            onCancelFriend = { pickingFriend = false },
            onStartFriend = { pickingFriend = true },
            onPick = { id, name ->
                prefs.setGroup(id)
                prefs.setName(name)
                groupId = id
                studentName = name
                picking = false
            },
            onBackPick = { picking = false },
            onStartPick = { picking = true },
            onReset = {
                prefs.clearProfile()
                prefs.setFriendGroup(null)
                LessonAlerts.cancelAll(context)
                groupId = null
                studentName = ""
                picking = true
                pickingFriend = false
            },
            onPickGroup = { id ->
                prefs.setGroup(id)
                groupId = id
            },
            onName = { value ->
                prefs.setName(value)
                studentName = value
            },
            onDark = ::beginTheme,
            onLanguage = ::applyLanguage,
        )
        val current = reveal
        if (current != null) {
            val radius = remember(current) { Animatable(0f) }
            val maxRadius = remember(current, rootSize) {
                val width = rootSize.width.toFloat().coerceAtLeast(1f)
                val height = rootSize.height.toFloat().coerceAtLeast(1f)
                hypot(
                    maxOf(current.origin.x, width - current.origin.x),
                    maxOf(current.origin.y, height - current.origin.y),
                )
            }
            LaunchedEffect(current) {
                radius.animateTo(maxRadius, tween(440, easing = FastOutSlowInEasing))
                dark = current.toDark
                prefs.setDarkTheme(current.toDark)
                ru.bgtu.schedule.widget.ScheduleWidget.refresh(context.applicationContext)
                reveal = null
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircularRevealShape(current.origin, radius.value)),
                ) {
                    AppLayer(
                        dark = current.toDark,
                        splash = splash,
                        failed = failed,
                        bundle = bundle,
                        picking = picking,
                        groupId = groupId,
                        studentName = studentName,
                        prefs = prefs,
                        shellUi = shellUi,
                        lang = lang,
                        windows = false,
                        onboarded = onboarded,
                        onboardPage = onboardPage,
                        pickingFriend = pickingFriend,
                        onPage = { onboardPage = it },
                        onFinishOnboarding = {},
                        onPickFriend = {},
                        onCancelFriend = {},
                        onStartFriend = {},
                        onPick = { _, _ -> },
                        onBackPick = {},
                        onStartPick = {},
                        onReset = {},
                        onPickGroup = {},
                        onName = {},
                        onDark = ::beginTheme,
                        onLanguage = ::applyLanguage,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLayer(
    dark: Boolean,
    splash: Boolean,
    failed: Boolean,
    bundle: ru.bgtu.schedule.data.ScheduleBundle?,
    picking: Boolean,
    groupId: String?,
    studentName: String,
    prefs: Prefs,
    shellUi: ShellUi,
    lang: String?,
    windows: Boolean,
    onboarded: Boolean,
    onboardPage: Int,
    pickingFriend: Boolean,
    onPage: (Int) -> Unit,
    onFinishOnboarding: () -> Unit,
    onPickFriend: (String) -> Unit,
    onCancelFriend: () -> Unit,
    onStartFriend: () -> Unit,
    onPick: (String, String) -> Unit,
    onBackPick: () -> Unit,
    onStartPick: () -> Unit,
    onReset: () -> Unit,
    onPickGroup: (String) -> Unit,
    onName: (String) -> Unit,
    onDark: (Boolean, Offset) -> Unit,
    onLanguage: (String) -> Unit,
) {
    CompositionLocalProvider(LocalCopy provides copyOf(lang)) {
    BgtuTheme(dark = dark, lightStatusIcons = splash) {
        val data = bundle
        val copy = LocalCopy.current
        when {
            splash -> SplashScreen()
            failed -> InfoState(copy.loadFailedTitle, copy.loadFailedText)
            data == null -> SplashScreen()
            !onboarded -> OnboardingScreen(
                lang = lang,
                dark = dark,
                page = onboardPage,
                onPage = onPage,
                onLanguage = onLanguage,
                onDark = onDark,
                onStart = onFinishOnboarding,
            )
            pickingFriend && data.find(groupId) != null -> GroupPicker(
                bundle = data,
                initialName = "",
                selectedId = prefs.friendGroup(),
                onBack = onCancelFriend,
                onConfirm = { id, _ -> onPickFriend(id) },
                askName = false,
            )
            picking || data.find(groupId) == null -> GroupPicker(
                bundle = data,
                initialName = studentName,
                selectedId = groupId,
                onBack = if (data.find(groupId) != null) onBackPick else null,
                onConfirm = onPick,
            )
            else -> {
                val found = data.find(groupId)!!
                val version = shellUi.notesVersion
                MainShell(
                    bundle = data,
                    faculty = found.first,
                    group = found.second,
                    studentName = studentName,
                    dark = dark,
                    prefs = prefs,
                    shellUi = shellUi,
                    windows = windows,
                    noteOf = { date, key ->
                        version
                        prefs.note(date.toString(), key)
                    },
                    onDark = onDark,
                    onName = onName,
                    onChangeGroup = onStartPick,
                    onReset = onReset,
                    onPickGroup = onPickGroup,
                    lang = lang,
                    onLanguage = onLanguage,
                    onPickFriend = onStartFriend,
                )
            }
        }
    }
    }
}

@Composable
private fun InfoState(title: String, text: String) {
    Column(
        modifier = Modifier.fillMaxSize().background(Bg).padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(Modifier.height(8.dp))
        Text(text, color = Muted, fontSize = 15.sp)
    }
}

@Composable
private fun MainShell(
    bundle: ru.bgtu.schedule.data.ScheduleBundle,
    faculty: Faculty,
    group: Group,
    studentName: String,
    dark: Boolean,
    prefs: Prefs,
    shellUi: ShellUi,
    windows: Boolean,
    noteOf: (LocalDate, String) -> String,
    onDark: (Boolean, Offset) -> Unit,
    onName: (String) -> Unit,
    onChangeGroup: () -> Unit,
    onReset: () -> Unit,
    onPickGroup: (String) -> Unit,
    lang: String?,
    onLanguage: (String) -> Unit,
    onPickFriend: () -> Unit,
) {
    val context = LocalContext.current
    val date = remember(shellUi.selectedDate) { LocalDate.parse(shellUi.selectedDate) }
    val tasksTick = shellUi.tasksVersion

    Box(Modifier.fillMaxSize().background(Bg)) {
        Scaffold(
            containerColor = Bg,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            bottomBar = {
                AppBar(shellUi.tab) { shellUi.tab = it }
            },
        ) { padding ->
            AnimatedContent(
                targetState = shellUi.tab,
                modifier = Modifier.fillMaxSize().padding(padding),
                transitionSpec = {
                    val forward = targetState > initialState
                    val offset = if (forward) 48 else -48
                    (fadeIn(tween(240)) + slideInHorizontally { offset }).togetherWith(
                        fadeOut(tween(160)) + slideOutHorizontally { -offset / 2 },
                    )
                },
                label = "tabs",
            ) { page ->
                when (page) {
                    0 -> HomeScreen(
                        bundle, faculty, group, studentName,
                        dark = dark,
                        onDark = onDark,
                        noteOf = noteOf,
                        onNotes = { shellUi.reminders = true },
                        onOpenProfile = { shellUi.tab = 4 },
                        lang = lang,
                        onLanguage = onLanguage,
                        tasksTick = tasksTick,
                        onOpenTasks = { shellUi.tasks = true },
                        friendId = prefs.friendGroup(),
                        onTasksChanged = {
                            shellUi.tasksVersion++
                            ru.bgtu.schedule.notify.DeadlineAlerts.reschedule(context)
                        },
                    ) { lesson, day ->
                        shellUi.opened = lesson
                        shellUi.openedDate = day
                        shellUi.selectedDate = day.toString()
                    }
                    1 -> ScheduleScreen(
                        bundle, faculty, group, date, shellUi.filter,
                        onDate = { shellUi.selectedDate = it.toString() },
                        onFilter = { shellUi.filter = it },
                        onLesson = {
                            shellUi.opened = it
                            shellUi.openedDate = date
                        },
                        noteOf = noteOf,
                        tasksTick = tasksTick,
                        onTasksChanged = {
                            shellUi.tasksVersion++
                            ru.bgtu.schedule.notify.DeadlineAlerts.reschedule(context)
                        },
                    )
                    2 -> GroupsScreen(bundle, group.id) { id ->
                        onPickGroup(id)
                        shellUi.tab = 1
                    }
                    3 -> CalendarScreen(
                        bundle, group, date, shellUi.filter,
                        onDate = { shellUi.selectedDate = it.toString() },
                        onLesson = {
                            shellUi.opened = it
                            shellUi.openedDate = date
                        },
                        noteOf = noteOf,
                        tasksTick = tasksTick,
                    )
                    else -> ProfileScreen(
                        bundle, faculty, group, studentName, dark, onDark, onName, onChangeGroup, onReset,
                        onOpenFiles = { shellUi.files = true },
                        lang = lang,
                        onLanguage = onLanguage,
                        onOpenTasks = { shellUi.tasks = true },
                        onPickFriend = onPickFriend,
                        onFriendChanged = { shellUi.notesVersion++ },
                        onRestored = { restoredName, restoredGroup ->
                            onName(restoredName)
                            if (restoredGroup.isNotBlank()) onPickGroup(restoredGroup)
                            shellUi.notesVersion++
                            shellUi.tasksVersion++
                            ru.bgtu.schedule.notify.DeadlineAlerts.reschedule(context)
                            ru.bgtu.schedule.widget.ScheduleWidget.refresh(context)
                        },
                    )
                }
            }
        }

        if (windows) {
            AnimatedVisibility(
                visible = shellUi.reminders,
                enter = fadeIn(tween(180)) + slideInVertically(tween(280)) { it / 8 },
                exit = fadeOut(tween(160)) + slideOutVertically(tween(220)) { it / 8 },
            ) {
                NotesOverlay(bundle, group) { shellUi.reminders = false }
            }
            if (shellUi.files) {
                FilesOverlay(group) { shellUi.files = false }
            }
            if (shellUi.tasks) {
                TasksOverlay(
                    group = group,
                    onClose = { shellUi.tasks = false },
                    onChanged = {
                        shellUi.tasksVersion++
                        ru.bgtu.schedule.notify.DeadlineAlerts.reschedule(context)
                    },
                )
            }
            val lesson = shellUi.opened
            if (lesson != null) {
                LessonDialog(
                    lesson = lesson,
                    group = group,
                    date = shellUi.openedDate,
                    initialNote = prefs.note(shellUi.openedDate.toString(), lesson.key),
                    onNote = { text ->
                        prefs.setNote(shellUi.openedDate.toString(), lesson.key, text)
                        shellUi.notesVersion++
                    },
                    onTasksChanged = {
                        shellUi.tasksVersion++
                        ru.bgtu.schedule.notify.DeadlineAlerts.reschedule(context)
                    },
                    onRefresh = { shellUi.notesVersion++ },
                    onClose = { shellUi.opened = null },
                )
            }
        }
    }
}

@Composable
private fun LanguageScreen(onPick: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = LocalBottomInset.current + 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Язык", color = Ink, fontWeight = FontWeight.Bold, fontSize = 32.sp)
        Text("Language", color = Muted, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(8.dp))
        Text("Выберите язык приложения", color = Ink, fontSize = 15.sp)
        Text("Choose the app language", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        LanguageChoice("Русский", "Russian") { onPick("ru") }
        Spacer(Modifier.height(10.dp))
        LanguageChoice("English", "Английский") { onPick("en") }
    }
}

@Composable
private fun LanguageChoice(title: String, hint: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Card)
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text(hint, color = Blue, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
internal fun LanguageMark(lang: String?, onLanguage: (String) -> Unit) {
    val current = if (lang == "en") "en" else "ru"
    Text(
        if (current == "en") "EN" else "RU",
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BlueSoft)
            .clickable { onLanguage(if (current == "en") "ru" else "en") }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = Blue,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
    )
}

@Composable
private fun AppBar(current: Int, onSelect: (Int) -> Unit) {
    val copy = S
    val tabs = listOf(
        TabSpec(copy.tabHome, Icons.Filled.Home),
        TabSpec(copy.tabSchedule, Icons.Filled.ViewDay),
        TabSpec(copy.tabGroups, Icons.Filled.Groups),
        TabSpec(copy.tabCalendar, Icons.Filled.CalendarMonth),
        TabSpec(copy.tabProfile, Icons.Filled.Person),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(Card)
            .padding(bottom = LocalBottomInset.current)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == current
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (selected) BlueSoft else Card),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) Blue else Muted,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    tab.label,
                    color = if (selected) Blue else Muted,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
