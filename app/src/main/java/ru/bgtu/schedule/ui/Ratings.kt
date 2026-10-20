package ru.bgtu.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.bgtu.schedule.data.AdminApi
import ru.bgtu.schedule.data.ApiFailure
import ru.bgtu.schedule.data.FavoriteTeachers
import ru.bgtu.schedule.data.RatingApi
import ru.bgtu.schedule.data.RatingResult
import ru.bgtu.schedule.data.RatingSummary
import ru.bgtu.schedule.data.ScheduleBundle
import ru.bgtu.schedule.data.TeacherRatings
import ru.bgtu.schedule.data.TeacherReview
import ru.bgtu.schedule.data.VoteResult
import ru.bgtu.schedule.data.nextTeacherLessons
import ru.bgtu.schedule.data.splitTeachers
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.Locale

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
        val current = summary
        Stars(current?.overall ?: 0.0, size = 13)
        Spacer(Modifier.width(6.dp))
        Text(
            if (current == null || current.count == 0) s.ratingNone else "${score(current.overall)} · ${s.reviewsCount(current.count)}",
            color = if (current == null || current.count == 0) Muted else Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Teacher page: favorite star, rating card per person and every class in the next two weeks. */
@Composable
fun TeacherDialog(bundle: ScheduleBundle, name: String, onClose: () -> Unit, onFavoriteChanged: () -> Unit = {}) {
    val s = S
    val context = LocalContext.current
    val favorites = remember { FavoriteTeachers(context) }
    val key = name.trim()
    var favorite by remember(key) { mutableStateOf(favorites.has(key)) }
    val people = remember(key) { splitTeachers(key) }
    val lessons = remember(key, bundle) {
        nextTeacherLessons(bundle, key, LocalDate.now(), LocalTime.now(), bundle.semesterStart, limit = 60)
    }
    val days = lessons.groupBy { it.day }
    BareDialog(onDismiss = onClose, fullScreen = true) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .background(Bg),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = LocalBottomInset.current + 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "head") {
                Row(verticalAlignment = Alignment.Top) {
                    IconButton(onClick = onClose, modifier = Modifier.padding(start = 0.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, tint = Ink)
                    }
                    Text(
                        teacherHeading(key),
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        modifier = Modifier.weight(1f).padding(top = 10.dp),
                    )
                    IconButton(onClick = {
                        favorite = !favorite
                        favorites.set(key, favorite)
                        onFavoriteChanged()
                    }) {
                        Icon(
                            if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (favorite) s.removeFavorite else s.addFavorite,
                            tint = if (favorite) Blue else Muted,
                        )
                    }
                }
            }
            items(people, key = { "rating|$it" }) { person ->
                RatingCard(person, showName = people.size > 1)
            }
            item(key = "soon") {
                Text(
                    s.teacherSoon,
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (days.isEmpty()) Text(s.noTeacherSoon, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
            days.forEach { (day, items) ->
                item(key = "day|$day") {
                    Column {
                        Text(s.formatLongDateCap(day), color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        items.forEach { item ->
                            Row(
                                Modifier
                                    .padding(top = 6.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Card)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(item.lesson.start, color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(48.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.lesson.subject, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(
                                        listOf(item.groupId, item.facultyShort, s.roomName(item.lesson.room), s.lessonType(item.lesson.type))
                                            .filter { it.isNotBlank() }
                                            .joinToString(" · "),
                                        color = Muted,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ratingErrorOf(s: Copy, error: Throwable): String = when (val code = (error as? ApiFailure)?.code) {
    null -> s.more.ratingFailed
    "offline", "db_unavailable", "not_configured" -> s.ratingOffline
    else -> if (code == "own_review" || code == "banned") s.errorText(code) else s.ratingError(code)
}

@Composable
private fun RatingCard(name: String, showName: Boolean) {
    val s = S
    val context = LocalContext.current
    var data by remember(name) { mutableStateOf<TeacherRatings?>(null) }
    var failed by remember(name) { mutableStateOf(false) }
    var attempt by remember(name) { mutableIntStateOf(0) }
    var rating by remember(name) { mutableStateOf(false) }
    LaunchedEffect(name, attempt) {
        val loaded = RatingApi.teacher(context, name)
        failed = loaded == null
        if (loaded != null) data = loaded
    }
    val summary = data?.summary ?: RatingApi.cachedSummaries()[name]
    val mine = data?.mine
    val others = data?.reviews.orEmpty().filter { mine == null || it.id != mine.id }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Card)
            .padding(16.dp),
    ) {
        Text(if (showName) "${s.ratingTitle} · $name" else s.ratingTitle, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        if (summary != null && summary.count > 0) {
            SummaryView(summary)
        } else {
            Text(s.ratingNone, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        if (failed && data == null) {
            Text(s.ratingOffline, color = Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            TextButton(onClick = { attempt++ }, contentPadding = PaddingValues(0.dp)) { Text(s.retry, color = Blue) }
        } else {
            PrimaryButton(
                if (mine != null) s.editReview else s.rateTeacher,
                onClick = { rating = true },
                enabled = data != null,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        if (mine != null) ReviewCard(mine, title = s.more.yourReview, own = true, onChanged = { attempt++ })
        if (data != null) {
            Text(s.ratingOpen, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(top = 16.dp))
            if (others.isEmpty() && mine == null) Text(s.noReviews, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            others.forEach { review -> ReviewCard(review, onChanged = { attempt++ }) }
        }
    }
    if (rating) {
        RatingSheet(
            teacher = name,
            initial = mine,
            onDismiss = { rating = false },
            onSaved = {
                rating = false
                attempt++
            },
        )
    }
}

@Composable
private fun SummaryView(summary: RatingSummary) {
    val s = S
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Text(score(summary.overall), color = Ink, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 34.sp)
        Spacer(Modifier.width(10.dp))
        Column {
            Stars(summary.overall, size = 18)
            Text(s.reviewsCount(summary.count), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
    ScoreRow(s.ratingDelivery, summary.delivery)
    ScoreRow(s.ratingStrictness, summary.strictness)
    ScoreRow(s.ratingLoyalty, summary.loyalty)
}

@Composable
private fun ScoreRow(label: String, value: Double) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Stars(value, size = 14)
        Text(
            if (value > 0) score(value) else "—",
            color = Ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(32.dp),
        )
    }
}

private fun shortDate(s: Copy, iso: String): String {
    val date = runCatching { OffsetDateTime.parse(iso).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(iso.take(10)) }.getOrNull()
        ?: return ""
    return "${date.dayOfMonth} ${s.monthsGenitive[date.monthValue - 1]} ${date.year}"
}

@Composable
private fun ReviewCard(review: TeacherReview, title: String? = null, own: Boolean = false, onChanged: () -> Unit) {
    val s = S
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var votes by remember(review.id, review.updatedAt) { mutableStateOf(VoteResult(review.likes, review.dislikes, review.myVote)) }
    var reporting by remember { mutableStateOf(false) }
    var banning by remember { mutableStateOf(false) }
    val adminToken by AdminApi.token.collectAsState()

    fun vote(value: Int) {
        val next = if (votes.myVote == value) 0 else value
        val previous = votes
        votes = VoteResult(
            likes = votes.likes - (if (votes.myVote == 1) 1 else 0) + (if (next == 1) 1 else 0),
            dislikes = votes.dislikes - (if (votes.myVote == -1) 1 else 0) + (if (next == -1) 1 else 0),
            myVote = next,
        )
        scope.launch {
            try {
                votes = RatingApi.vote(context, review.id, next)
            } catch (error: ApiFailure) {
                votes = previous
                Toasts.show(s.errorText(error.code))
            }
        }
    }

    fun moderate(action: String) {
        scope.launch {
            try {
                AdminApi.action(action, mapOf("id" to review.id))
                Toasts.show(s.more.adminDone)
                onChanged()
            } catch (error: ApiFailure) {
                Toasts.show(s.errorText(error.code))
            }
        }
    }

    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            Stars(review.overall.toDouble(), size = 14)
            Text(
                title.orEmpty(),
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Text(shortDate(s, review.updatedAt), color = Muted, fontSize = 11.sp)
        }
        Text(
            "${s.ratingDelivery} ${review.delivery} · ${s.ratingStrictness} ${review.strictness} · ${s.ratingLoyalty} ${review.loyalty}",
            color = Muted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (review.text.isNotBlank()) {
            Text(review.text, color = Ink, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        if (review.id > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                if (own) {
                    Icon(Icons.Outlined.ThumbUp, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
                    Text(votes.likes.toString(), color = Muted, fontSize = 11.sp)
                    Icon(Icons.Outlined.ThumbDown, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
                    Text(votes.dislikes.toString(), color = Muted, fontSize = 11.sp)
                } else {
                    VoteButton(votes.myVote == 1, up = true, count = votes.likes, label = s.more.like) { vote(1) }
                    VoteButton(votes.myVote == -1, up = false, count = votes.dislikes, label = s.more.dislike) { vote(-1) }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { reporting = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Flag, contentDescription = s.more.report, tint = Muted, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (adminToken != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { moderate("review.hide") }) { Text(s.more.hide, color = Blue, fontSize = 13.sp) }
                    TextButton(onClick = { moderate("review.delete") }) { Text(s.more.deleteForever, color = Red, fontSize = 13.sp) }
                    TextButton(onClick = { banning = true }) { Text(s.more.ban, color = Red, fontSize = 13.sp) }
                }
            }
        }
    }
    if (reporting) {
        ReportSheet(
            onDismiss = { reporting = false },
            onSend = { reason -> RatingApi.report(context, "review", review.id, reason) },
        )
    }
    if (banning) {
        BanSheet(
            withHideAll = true,
            onDismiss = { banning = false },
            onBan = { days, reason, extra ->
                AdminApi.action("ban.review", mapOf("id" to review.id, "days" to days, "reason" to reason, "hideAll" to extra))
                onChanged()
            },
        )
    }
}

@Composable
private fun VoteButton(on: Boolean, up: Boolean, count: Int, label: String, onClick: () -> Unit) {
    val icon = when {
        up && on -> Icons.Filled.ThumbUp
        up -> Icons.Outlined.ThumbUp
        on -> Icons.Filled.ThumbDown
        else -> Icons.Outlined.ThumbDown
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (on) BlueSoft else Card)
            .border(1.dp, if (on) Blue else Line, RoundedCornerShape(99.dp))
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = if (on) Blue else Muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(count.toString(), color = if (on) Blue else Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Stars filled to the nearest whole value. */
@Composable
fun Stars(value: Double, size: Int, onPick: ((Int) -> Unit)? = null) {
    val filled = Math.round(value).toInt()
    Row {
        for (index in 1..5) {
            val on = index <= filled
            Icon(
                if (on) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = if (on) StarYellow else Muted.copy(alpha = 0.6f),
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

    fun finish(result: RatingResult) {
        busy = false
        when (result) {
            RatingResult.Ok -> onSaved()
            is RatingResult.Failed -> error = s.ratingError(result.code)
        }
    }

    BottomSheet(onDismiss) {
        Text(if (initial != null) s.editReview else s.rateTeacher, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(teacherHeading(teacher), color = Muted, fontSize = 13.sp)
        StarsField(s.ratingOverall, overall, 34) { overall = it }
        StarsField(s.ratingDelivery, delivery, 28) { delivery = it }
        StarsField(s.ratingStrictness, strictness, 28) { strictness = it }
        StarsField(s.ratingLoyalty, loyalty, 28) { loyalty = it }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(RatingApi.MAX_TEXT) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(s.reviewText) },
            placeholder = { Text(s.reviewHint) },
            supportingText = { Text("${text.length}/${RatingApi.MAX_TEXT}", color = Muted) },
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors(),
            minLines = 4,
        )
        Text(s.ratingAnonymous, color = Muted, fontSize = 12.sp)
        ErrorLine(error)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (initial != null) {
                TextButton(onClick = {
                    busy = true
                    error = null
                    scope.launch { finish(RatingApi.delete(context, teacher)) }
                }, enabled = !busy) { Text(s.deleteReview, color = Red) }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text(s.cancel, color = Muted) }
            TextButton(
                onClick = {
                    if (listOf(overall, delivery, strictness, loyalty).any { it == 0 }) {
                        error = s.more.pickOverall
                        return@TextButton
                    }
                    busy = true
                    error = null
                    scope.launch {
                        finish(RatingApi.submit(context, teacher, overall, delivery, strictness, loyalty, text))
                    }
                },
                enabled = !busy,
            ) { Text(if (initial != null) s.save else s.sendReview, color = Blue, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun StarsField(label: String, value: Int, size: Int, onPick: (Int) -> Unit) {
    Column {
        Text(label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Stars(value.toDouble(), size = size, onPick = onPick)
    }
}

/** Asks for an optional reason and sends a report; shared by reviews and chat messages. */
@Composable
fun ReportSheet(onDismiss: () -> Unit, onSend: suspend (String) -> Unit) {
    val s = S
    val scope = rememberCoroutineScope()
    var reason by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    BottomSheet(onDismiss) {
        Text(s.more.reportTitle, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(s.more.reportText, color = Muted, fontSize = 13.sp)
        InputField(reason, { reason = it.take(200) }, s.more.reportTitle, singleLine = false, minLines = 3)
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
                            onSend(reason)
                            Toasts.show(s.more.reportSent)
                            onDismiss()
                        } catch (failure: ApiFailure) {
                            error = s.errorText(failure.code)
                        } finally {
                            busy = false
                        }
                    }
                },
            ) { Text(s.sendReview, color = Blue, fontWeight = FontWeight.SemiBold) }
        }
    }
}
