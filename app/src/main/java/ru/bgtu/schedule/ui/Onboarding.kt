package ru.bgtu.schedule.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import ru.bgtu.schedule.data.Prefs
import ru.bgtu.schedule.notify.LessonAlerts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    lang: String?,
    dark: Boolean,
    page: Int,
    onPage: (Int) -> Unit,
    onLanguage: (String) -> Unit,
    onDark: (Boolean, Offset) -> Unit,
    onStart: () -> Unit,
) {
    val s = S
    val context = LocalContext.current
    val prefs = remember { Prefs(context.applicationContext) }
    var notifyChoice by remember { mutableStateOf(if (prefs.notifyEnabled()) true else null as Boolean?) }
    val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifyChoice = granted
        prefs.setNotifyEnabled(granted)
        if (granted) LessonAlerts.reschedule(context) else LessonAlerts.cancelAll(context)
    }
    fun turnNotifications(on: Boolean) {
        if (!on) {
            notifyChoice = false
            prefs.setNotifyEnabled(false)
            LessonAlerts.cancelAll(context)
            return
        }
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notifyChoice = true
            prefs.setNotifyEnabled(true)
            LessonAlerts.reschedule(context)
        }
    }
    val pager = rememberPagerState(initialPage = page.coerceIn(0, 2), pageCount = { 3 })
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage) { onPage(pager.currentPage) }
    val pages = listOf(
        s.welcomeTitle to s.welcomeText,
        s.notifyPageTitle to s.notifyPageText,
        s.searchPageTitle to s.searchPageText,
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .padding(bottom = LocalBottomInset.current + 12.dp),
    ) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
        ) { index ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                WelcomeArt(index)
                Spacer(Modifier.height(28.dp))
                Text(
                    pages[index].first,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp,
                )
                Text(
                    pages[index].second,
                    color = Muted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                if (index == 1) {
                    Text(
                        s.notifyAsk,
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 22.dp),
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChoiceChip(s.notifyYes, notifyChoice == true, Modifier.weight(1f)) { _ -> turnNotifications(true) }
                        ChoiceChip(s.notifyNo, notifyChoice == false, Modifier.weight(1f)) { _ -> turnNotifications(false) }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { index ->
                val selected = pager.currentPage == index
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (selected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (selected) Blue else Line),
                )
            }
        }
        if (pager.currentPage == 0) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoiceChip(
                    s.langRu,
                    (lang ?: "ru") != "en",
                    Modifier.weight(1f),
                ) { onLanguage("ru") }
                ChoiceChip(
                    s.langEn,
                    lang == "en",
                    Modifier.weight(1f),
                ) { onLanguage("en") }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoiceChip(s.themeLight, !dark, Modifier.weight(1f)) { onDark(false, it) }
                ChoiceChip(s.themeDark, dark, Modifier.weight(1f)) { onDark(true, it) }
            }
        }
        Button(
            onClick = {
                if (pager.currentPage < 2) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                } else {
                    onStart()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
        ) {
            Text(
                if (pager.currentPage < 2) s.nextPage else s.begin,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
        Text(
            "${s.brand} ${AppVersion.LABEL}",
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            color = Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WelcomeArt(page: Int) {
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(148.dp)
                .offset(x = (-34).dp, y = 22.dp)
                .clip(CircleShape)
                .background(BlueSoft),
        )
        Box(
            Modifier
                .size(112.dp)
                .offset(x = 46.dp, y = (-30).dp)
                .clip(CircleShape)
                .background(Blue),
        )
        Box(
            Modifier
                .size(132.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Card)
                .border(3.dp, Blue, RoundedCornerShape(36.dp)),
            contentAlignment = Alignment.Center,
        ) {
            when (page) {
                0 -> Mark(64)
                1 -> ArtIcon(Icons.Filled.Notifications)
                else -> ArtIcon(Icons.Filled.Search)
            }
        }
    }
}

@Composable
private fun ArtIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, tint = Blue, modifier = Modifier.size(58.dp))
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (Offset) -> Unit,
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    Text(
        label,
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                origin = position + Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
            }
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Blue else BlueSoft)
            .clickable { onClick(origin) }
            .padding(vertical = 10.dp),
        color = if (selected) Color.White else Blue,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
    )
}
