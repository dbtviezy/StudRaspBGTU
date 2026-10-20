package ru.bgtu.schedule.ui

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.launch
import ru.bgtu.schedule.data.RatingApi
import ru.bgtu.schedule.data.RatingResult
import ru.bgtu.schedule.data.RatingSummary
import ru.bgtu.schedule.data.TeacherRatings
import ru.bgtu.schedule.data.TeacherReview
import java.util.Locale

private val StarColor = Color(0xFFF5B301)

private fun score(value: Double) = String.format(Locale.US, "%.1f", value)

@Composable
fun RatingLine(teacher: String, onOpen: () -> Unit) {
    val s = S
    var summary by remember(teacher) { mutableStateOf(RatingApi.cachedSummaries()[teacher]) }
    LaunchedEffect(teacher) {
        RatingApi.loadSummaries()?.let { summary = it[teacher] }
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = StarColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        val current = summary
        Text(
            if (current == null || current.count == 0) s.ratingNone else "${score(current.overall)} · ${s.reviewsCount(current.count)}",
            color = Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(8.dp))
        Text(s.ratingOpen, color = Blue, fontSize = 13.sp)
    }
}

@Composable
fun TeacherRatingDialog(teacher: String, onClose: () -> Unit) {
    val s = S
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
        Column(
            Modifier
                .fillMaxSize()
                .background(Bg)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = LocalBottomInset.current + 24.dp),
        ) {
            IconButton(onClick = onClose, modifier = Modifier.padding(start = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, tint = Ink)
            }
            Text(
                teacher,
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
            TeacherRatingBlock(teacher, Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
fun TeacherRatingBlock(teacher: String, modifier: Modifier = Modifier) {
    val s = S
    val context = LocalContext.current
    var data by remember(teacher) { mutableStateOf<TeacherRatings?>(null) }
    var failed by remember(teacher) { mutableStateOf(false) }
    var loadTick by remember(teacher) { mutableIntStateOf(0) }
    var editing by remember(teacher) { mutableStateOf(false) }
    LaunchedEffect(teacher, loadTick) {
        failed = false
        val loaded = RatingApi.teacher(context, teacher)
        if (loaded == null) failed = true else data = loaded
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Card)
            .padding(16.dp),
    ) {
        Text(s.ratingTitle, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        val current = data
        when {
            current == null && failed -> {
                Text(s.ratingOffline, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                TextButton(onClick = { loadTick++ }, contentPadding = PaddingValues(0.dp)) { Text(s.retry, color = Blue) }
            }
            current == null -> Text(s.loading, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            else -> {
                SummaryView(current.summary)
                TextButton(onClick = { editing = true }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (current.mine == null) s.rateTeacher else s.editReview, color = Blue, fontWeight = FontWeight.SemiBold)
                }
                val texts = current.reviews.filter { it.text.isNotBlank() }
                if (texts.isEmpty()) {
                    Text(s.noReviews, color = Muted, fontSize = 13.sp)
                } else {
                    texts.forEach { review -> ReviewRow(review) }
                }
            }
        }
    }
    if (editing) {
        RatingSheet(
            teacher = teacher,
            initial = data?.mine,
            onDismiss = { editing = false },
            onSaved = {
                editing = false
                loadTick++
            },
        )
    }
}

@Composable
private fun SummaryView(summary: RatingSummary?) {
    val s = S
    if (summary == null || summary.count == 0) {
        Text(s.ratingNone, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Text(score(summary.overall), color = Ink, fontWeight = FontWeight.Bold, fontSize = 32.sp)
        Spacer(Modifier.width(10.dp))
        Column {
            Stars(Math.round(summary.overall).toInt(), size = 18)
            Text(s.reviewsCount(summary.count), color = Muted, fontSize = 12.sp)
        }
    }
    CriterionBar(s.ratingDelivery, summary.delivery)
    CriterionBar(s.ratingStrictness, summary.strictness)
    CriterionBar(s.ratingLoyalty, summary.loyalty)
}

@Composable
private fun CriterionBar(label: String, value: Double) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 13.sp, modifier = Modifier.width(110.dp))
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Line),
        ) {
            Box(
                Modifier
                    .fillMaxWidth((value / 5.0).toFloat().coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Blue),
            )
        }
        Text(score(value), color = Ink, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ReviewRow(review: TeacherReview) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stars(review.overall, size = 14)
            Spacer(Modifier.width(8.dp))
            Text(review.updatedAt.take(10), color = Muted, fontSize = 11.sp)
        }
        Text(review.text, color = Ink, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun Stars(value: Int, size: Int, onPick: ((Int) -> Unit)? = null) {
    Row {
        for (index in 1..5) {
            val on = index <= value
            Icon(
                if (on) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = if (on) StarColor else Muted,
                modifier = Modifier
                    .size(size.dp)
                    .then(if (onPick != null) Modifier.clickable { onPick(index) } else Modifier),
            )
            if (onPick != null) Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun RatingSheet(teacher: String, initial: TeacherReview?, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val s = S
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var overall by remember { mutableIntStateOf(initial?.overall ?: 0) }
    var delivery by remember { mutableIntStateOf(initial?.delivery ?: 0) }
    var strictness by remember { mutableIntStateOf(initial?.strictness ?: 0) }
    var loyalty by remember { mutableIntStateOf(initial?.loyalty ?: 0) }
    var text by remember { mutableStateOf(initial?.text.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val ready = overall > 0 && delivery > 0 && strictness > 0 && loyalty > 0 && !busy

    fun finish(result: RatingResult) {
        busy = false
        when (result) {
            RatingResult.Ok -> onSaved()
            is RatingResult.Failed -> error = s.ratingError(result.code)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Card)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(teacher, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(s.ratingAnonymous, color = Muted, fontSize = 12.sp)
            StarsField(s.ratingOverall, overall) { overall = it }
            StarsField(s.ratingDelivery, delivery) { delivery = it }
            StarsField(s.ratingStrictness, strictness) { strictness = it }
            StarsField(s.ratingLoyalty, loyalty) { loyalty = it }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(RatingApi.MAX_TEXT) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(s.reviewText) },
                placeholder = { Text(s.reviewHint) },
                supportingText = { Text("${text.length}/${RatingApi.MAX_TEXT}", color = Muted) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Blue,
                    unfocusedBorderColor = Line,
                    focusedContainerColor = Card,
                    unfocusedContainerColor = Card,
                    cursorColor = Blue,
                ),
                minLines = 3,
            )
            error?.let { Text(it, color = Color(0xFFE15B64), fontSize = 13.sp) }
            Button(
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        finish(RatingApi.submit(context, teacher, overall, delivery, strictness, loyalty, text))
                    }
                },
                enabled = ready,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
            ) { Text(if (busy) s.loading else s.sendReview) }
            Row {
                if (initial != null) {
                    TextButton(onClick = {
                        busy = true
                        error = null
                        scope.launch { finish(RatingApi.delete(context, teacher)) }
                    }, enabled = !busy) { Text(s.deleteReview, color = Color(0xFFE15B64)) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(s.cancel, color = Blue) }
            }
        }
    }
}

@Composable
private fun StarsField(label: String, value: Int, onPick: (Int) -> Unit) {
    Column {
        Text(label, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Stars(value, size = 30, onPick = onPick)
    }
}
