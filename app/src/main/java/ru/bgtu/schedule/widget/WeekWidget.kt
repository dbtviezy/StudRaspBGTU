package ru.bgtu.schedule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import ru.bgtu.schedule.MainActivity
import ru.bgtu.schedule.R
import ru.bgtu.schedule.data.DayOverrides
import ru.bgtu.schedule.data.HiddenStore
import ru.bgtu.schedule.data.Holidays
import ru.bgtu.schedule.data.Lesson
import ru.bgtu.schedule.data.Prefs
import ru.bgtu.schedule.data.ScheduleRepository
import ru.bgtu.schedule.data.shownLessons
import ru.bgtu.schedule.data.weekInfo
import ru.bgtu.schedule.ui.copyOf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Home-screen widget with every class from Monday to Saturday of the current week (next week on Sundays). */
class WeekWidget : android.appwidget.AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        update(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED -> refresh(context)
            else -> super.onReceive(context, intent)
        }
    }

    companion object {
        fun refresh(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, WeekWidget::class.java))
            ids.forEach { update(app, manager, it) }
            if (ids.isNotEmpty()) manager.notifyAppWidgetViewDataChanged(ids, R.id.week_list)
        }

        internal fun monday(): LocalDate {
            val today = LocalDate.now()
            val base = if (today.dayOfWeek == DayOfWeek.SUNDAY) today.plusDays(1) else today
            return base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = Prefs(context)
            val copy = copyOf(prefs.language())
            val palette = ScheduleWidget.palette(prefs.darkTheme())
            val views = RemoteViews(context.packageName, R.layout.widget_week)
            views.setInt(R.id.week_root, "setBackgroundResource", palette.canvas)
            val bundle = runCatching { ScheduleRepository.load(context.applicationContext) }.getOrNull()
            val start = monday()
            val info = bundle?.let { weekInfo(start, it.semesterStart) }
            views.setTextViewText(R.id.week_title, info?.let { copy.widgetWeek(it.number, it.even) } ?: copy.brand)
            views.setTextColor(R.id.week_title, palette.ink)
            val missing = bundle?.find(prefs.groupId()) == null
            views.setTextViewText(R.id.week_empty, if (missing) copy.widgetPickGroup else copy.widgetNoLessons)
            views.setTextColor(R.id.week_empty, palette.muted)

            val open = PendingIntent.getActivity(
                context,
                60,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.week_title, open)
            views.setPendingIntentTemplate(R.id.week_list, open)

            val service = Intent(context, WeekWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                data = Uri.parse("bgtu://widget/week/$id")
            }
            views.setRemoteAdapter(R.id.week_list, service)
            views.setEmptyView(R.id.week_list, R.id.week_empty)
            manager.updateAppWidget(id, views)
            manager.notifyAppWidgetViewDataChanged(id, R.id.week_list)
        }
    }
}

private sealed interface WeekRow {
    data class Day(val date: LocalDate, val note: String?) : WeekRow
    data class Item(val lesson: Lesson, val today: Boolean) : WeekRow
}

class WeekWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = WeekFactory(applicationContext)
}

private class WeekFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var rows: List<WeekRow> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val prefs = Prefs(context)
        val copy = copyOf(prefs.language())
        val bundle = runCatching { ScheduleRepository.load(context) }.getOrNull()
        val group = bundle?.find(prefs.groupId())?.second
        if (bundle == null || group == null) {
            rows = emptyList()
            return
        }
        val hidden = HiddenStore(context).keys()
        val overrides = DayOverrides(context)
        val today = LocalDate.now()
        val monday = WeekWidget.monday()
        rows = buildList {
            for (offset in 0..5) {
                val date = monday.plusDays(offset.toLong())
                val holiday = Holidays.on(date)
                val lessons = if (holiday != null) {
                    emptyList()
                } else {
                    shownLessons(group, date.dayOfWeek.value, date, bundle.semesterStart, "auto", hidden, overrides)
                }
                val note = when {
                    holiday != null -> if (copy.code == "en") holiday.nameEn else holiday.nameRu
                    lessons.isEmpty() -> copy.widgetNoLessons
                    else -> null
                }
                add(WeekRow.Day(date, note))
                lessons.forEach { add(WeekRow.Item(it, date == today)) }
            }
        }
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val prefs = Prefs(context)
        val copy = copyOf(prefs.language())
        val palette = ScheduleWidget.palette(prefs.darkTheme())
        return when (val row = rows.getOrNull(position)) {
            is WeekRow.Day -> RemoteViews(context.packageName, R.layout.widget_week_day).apply {
                val label = copy.formatLongDateCap(row.date) + (row.note?.let { " · $it" } ?: "")
                setTextViewText(R.id.week_day_text, label)
                setTextColor(R.id.week_day_text, if (row.date == LocalDate.now()) palette.blue else palette.muted)
                setOnClickFillInIntent(R.id.week_day_text, Intent())
            }
            is WeekRow.Item -> RemoteViews(context.packageName, R.layout.widget_week_lesson).apply {
                setTextViewText(R.id.week_lesson_time, row.lesson.start.ifBlank { "—" })
                setTextColor(R.id.week_lesson_time, if (row.today) palette.blue else palette.ink)
                setTextViewText(R.id.week_lesson_title, row.lesson.subject.ifBlank { copy.lessonFallback })
                setTextColor(R.id.week_lesson_title, palette.ink)
                setTextViewText(R.id.week_lesson_room, row.lesson.room)
                setTextColor(R.id.week_lesson_room, palette.muted)
                setOnClickFillInIntent(R.id.week_lesson_root, Intent())
            }
            null -> RemoteViews(context.packageName, R.layout.widget_week_day)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false
}
