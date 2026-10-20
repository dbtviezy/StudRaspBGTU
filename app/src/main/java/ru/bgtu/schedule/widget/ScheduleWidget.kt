package ru.bgtu.schedule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import ru.bgtu.schedule.MainActivity
import ru.bgtu.schedule.R
import ru.bgtu.schedule.data.Lesson
import ru.bgtu.schedule.data.Prefs
import ru.bgtu.schedule.data.ScheduleBundle
import ru.bgtu.schedule.data.ScheduleRepository
import ru.bgtu.schedule.data.lessonsOn
import ru.bgtu.schedule.ui.copyOf
import ru.bgtu.schedule.data.weekInfo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class ScheduleWidget : android.appwidget.AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        update(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_DAY) {
            intent.getStringExtra(EXTRA_DATE)?.let { Prefs(context).setWidgetDate(it) }
            refresh(context)
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        private const val ACTION_DAY = "ru.bgtu.schedule.WIDGET_DAY"
        private const val EXTRA_DATE = "date"
        private var cache: ScheduleBundle? = null

        private val dayIds = intArrayOf(R.id.day0, R.id.day1, R.id.day2, R.id.day3, R.id.day4, R.id.day5)
        private val nameIds = intArrayOf(R.id.day0_name, R.id.day1_name, R.id.day2_name, R.id.day3_name, R.id.day4_name, R.id.day5_name)
        private val numIds = intArrayOf(R.id.day0_num, R.id.day1_num, R.id.day2_num, R.id.day3_num, R.id.day4_num, R.id.day5_num)

        fun refresh(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, ScheduleWidget::class.java))
            ids.forEach { update(app, manager, it) }
            if (ids.isNotEmpty()) manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val options = manager.getAppWidgetOptions(id)
            val (width, height) = currentSize(context, options)
            val compact = height < 150
            val palette = palette(Prefs(context).darkTheme())
            val views = RemoteViews(context.packageName, R.layout.widget_schedule)
            views.setInt(R.id.widget_root, "setBackgroundResource", palette.canvas)

            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_date, open)
            views.setOnClickPendingIntent(R.id.widget_sub, open)

            val prefs = Prefs(context)
            val copy = copyOf(prefs.language())
            val shown = shownDate(prefs)
            prefs.setWidgetDate(shown.toString())
            val monday = shown.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val nameSize = if (width < 220) 9f else 10f
            val numSize = if (width < 220) 13f else 15f
            for (index in 0..5) {
                val day = monday.plusDays(index.toLong())
                val on = day == shown
                views.setTextViewText(nameIds[index], copy.dayShort[index].uppercase())
                views.setTextViewText(numIds[index], day.dayOfMonth.toString())
                views.setTextViewTextSize(nameIds[index], TypedValue.COMPLEX_UNIT_SP, nameSize)
                views.setTextViewTextSize(numIds[index], TypedValue.COMPLEX_UNIT_SP, numSize)
                views.setInt(dayIds[index], "setBackgroundResource", if (on) palette.chipOn else palette.chip)
                val nameColor = if (on) 0xFFFFFFFF.toInt() else palette.muted
                val numColor = if (on) 0xFFFFFFFF.toInt() else palette.ink
                views.setTextColor(nameIds[index], nameColor)
                views.setTextColor(numIds[index], numColor)
                val intent = Intent(context, ScheduleWidget::class.java).apply {
                    action = ACTION_DAY
                    putExtra(EXTRA_DATE, day.toString())
                    data = Uri.parse("bgtu://widget/day/$day")
                }
                views.setOnClickPendingIntent(
                    dayIds[index],
                    PendingIntent.getBroadcast(
                        context,
                        index,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }

            views.setViewVisibility(R.id.widget_days, if (compact) View.GONE else View.VISIBLE)
            views.setTextViewText(R.id.widget_date, copy.formatLongDateCap(shown))
            views.setTextColor(R.id.widget_date, palette.ink)
            views.setTextViewTextSize(R.id.widget_date, TypedValue.COMPLEX_UNIT_SP, if (compact) 15f else 16f)

            val bundle = load(context)
            val info = bundle?.let { weekInfo(shown, it.semesterStart) }
            if (compact || info == null) {
                views.setViewVisibility(R.id.widget_sub, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_sub, View.VISIBLE)
                views.setTextColor(R.id.widget_sub, palette.muted)
                views.setTextViewText(R.id.widget_sub, copy.widgetWeek(info.number, info.even))
            }

            val groupMissing = bundle?.find(prefs.groupId()) == null
            val holiday = ru.bgtu.schedule.data.Holidays.on(shown)
            val empty = when {
                groupMissing -> copy.widgetPickGroup
                holiday != null -> if (copy.code == "en") holiday.nameEn else holiday.nameRu
                else -> copy.widgetNoLessons
            }
            views.setTextViewText(R.id.widget_empty, empty)
            views.setTextColor(R.id.widget_empty, palette.muted)

            val service = Intent(context, LessonWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                data = Uri.parse("bgtu://widget/list/$id")
            }
            views.setRemoteAdapter(R.id.widget_list, service)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)
            manager.updateAppWidget(id, views)
            manager.notifyAppWidgetViewDataChanged(id, R.id.widget_list)
        }

        internal fun shownDate(prefs: Prefs): LocalDate {
            val today = LocalDate.now()
            val saved = prefs.widgetDate()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val anchor = when {
                saved == null -> today
                saved.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) !=
                    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) -> today
                else -> saved
            }
            return if (anchor.dayOfWeek == DayOfWeek.SUNDAY) anchor.minusDays(1) else anchor
        }

        internal fun lessons(context: Context): List<Lesson> {
            val prefs = Prefs(context)
            val bundle = load(context) ?: return emptyList()
            val group = bundle.find(prefs.groupId())?.second ?: return emptyList()
            val date = shownDate(prefs)
            val hidden = ru.bgtu.schedule.data.HiddenStore(context).keys()
            return ru.bgtu.schedule.data.shownLessons(
                group, date.dayOfWeek.value, date, bundle.semesterStart, "auto", hidden,
                ru.bgtu.schedule.data.DayOverrides(context),
            )
        }

        internal fun palette(dark: Boolean): WidgetPalette {
            return if (dark) {
                WidgetPalette(
                    canvas = R.drawable.widget_canvas_dark,
                    chip = R.drawable.widget_card_dark,
                    chipOn = R.drawable.widget_chip_on_dark,
                    card = R.drawable.widget_card_dark,
                    ink = 0xFFF4F6FB.toInt(),
                    muted = 0xFF9AA6BF.toInt(),
                    blue = 0xFF5B86FF.toInt(),
                )
            } else {
                WidgetPalette(
                    canvas = R.drawable.widget_canvas_light,
                    chip = R.drawable.widget_card_light,
                    chipOn = R.drawable.widget_chip_on,
                    card = R.drawable.widget_card_light,
                    ink = 0xFF1B1E28.toInt(),
                    muted = 0xFF8B93A7.toInt(),
                    blue = 0xFF2A56F6.toInt(),
                )
            }
        }

        private fun currentSize(context: Context, options: Bundle): Pair<Int, Int> {
            val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val width = options.getInt(
                if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                250,
            )
            val height = options.getInt(
                if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                180,
            )
            return width to height
        }

        private fun load(context: Context): ScheduleBundle? {
            cache?.let { return it }
            return runCatching { ScheduleRepository.load(context.applicationContext) }.getOrNull()?.also { cache = it }
        }
    }
}

internal data class WidgetPalette(
    val canvas: Int,
    val chip: Int,
    val chipOn: Int,
    val card: Int,
    val ink: Int,
    val muted: Int,
    val blue: Int,
)

class LessonWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return LessonFactory(applicationContext)
    }
}

private class LessonFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var lessons: List<Lesson> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        lessons = ScheduleWidget.lessons(context)
    }

    override fun onDestroy() {
        lessons = emptyList()
    }

    override fun getCount(): Int = lessons.size

    override fun getViewAt(position: Int): RemoteViews {
        val lesson = lessons[position]
        val prefs = Prefs(context)
        val copy = ru.bgtu.schedule.ui.copyOf(prefs.language())
        val palette = ScheduleWidget.palette(prefs.darkTheme())
        val views = RemoteViews(context.packageName, R.layout.widget_lesson)
        views.setInt(R.id.lesson_root, "setBackgroundResource", palette.card)
        views.setTextViewText(R.id.lesson_start, lesson.start.ifBlank { "—" })
        views.setTextColor(R.id.lesson_start, palette.ink)
        views.setTextViewText(R.id.lesson_end, lesson.end)
        views.setTextColor(R.id.lesson_end, palette.muted)
        views.setInt(R.id.lesson_bar, "setBackgroundColor", palette.blue)
        views.setTextViewText(R.id.lesson_title, lesson.subject.ifBlank { copy.lessonFallback })
        views.setTextColor(R.id.lesson_title, palette.ink)
        val meta = listOf(copy.lessonType(lesson.type), lesson.room).filter { it.isNotBlank() }.joinToString(" · ")
        if (meta.isBlank()) {
            views.setViewVisibility(R.id.lesson_meta, View.GONE)
        } else {
            views.setViewVisibility(R.id.lesson_meta, View.VISIBLE)
            views.setTextViewText(R.id.lesson_meta, meta)
            views.setTextColor(R.id.lesson_meta, palette.muted)
        }
        val open = PendingIntent.getActivity(
            context,
            40 + position,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.lesson_root, open)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
