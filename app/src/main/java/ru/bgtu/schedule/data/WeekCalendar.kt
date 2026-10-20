package ru.bgtu.schedule.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object WeekCalendar {
    fun canWrite(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    fun export(context: Context, group: Group, semesterStart: LocalDate, anchor: LocalDate, hidden: Set<String>): Int {
        val calendarId = writableCalendar(context) ?: return -1
        val monday = anchor.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val zone = ZoneId.systemDefault()
        var added = 0
        for (offset in 0..5) {
            val date = monday.plusDays(offset.toLong())
            val lessons = shownLessons(group, date.dayOfWeek.value, date, semesterStart, "auto", hidden, DayOverrides(context))
            lessons.forEach { lesson ->
                val start = parseTime(lesson.start) ?: return@forEach
                val end = parseTime(lesson.end) ?: start.plusMinutes(95)
                val marker = marker(date, lesson)
                if (alreadyThere(context, calendarId, date, start, marker)) return@forEach
                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.TITLE, lesson.subject.ifBlank { "Пара" })
                    put(CalendarContract.Events.EVENT_LOCATION, lesson.room)
                    put(
                        CalendarContract.Events.DESCRIPTION,
                        listOf(lesson.type, lesson.teacher, group.id, marker).filter { it.isNotBlank() }.joinToString("\n"),
                    )
                    put(CalendarContract.Events.DTSTART, date.atTime(start).atZone(zone).toInstant().toEpochMilli())
                    put(CalendarContract.Events.DTEND, date.atTime(end).atZone(zone).toInstant().toEpochMilli())
                    put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
                    put(CalendarContract.Events.EVENT_END_TIMEZONE, zone.id)
                }
                context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                added++
            }
        }
        return added
    }

    private fun marker(date: LocalDate, lesson: Lesson) = "bgtu:${date}|${lesson.key}"

    private fun writableCalendar(context: Context): Long? {
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val accessIndex = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
            while (cursor.moveToNext()) {
                val access = cursor.getInt(accessIndex)
                if (access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) return cursor.getLong(idIndex)
            }
        }
        return null
    }

    private fun alreadyThere(context: Context, calendarId: Long, date: LocalDate, start: LocalTime, marker: String): Boolean {
        val from = date.atTime(start).minusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val to = date.atTime(start).plusHours(6).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val uri = CalendarContract.Events.CONTENT_URI
        val selection = "${CalendarContract.Events.CALENDAR_ID}=? AND ${CalendarContract.Events.DTSTART}>=? AND ${CalendarContract.Events.DTSTART}<=?"
        context.contentResolver.query(
            uri,
            arrayOf(CalendarContract.Events.DESCRIPTION),
            selection,
            arrayOf(calendarId.toString(), from.toString(), to.toString()),
            null,
        )?.use { cursor ->
            val index = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
            while (cursor.moveToNext()) {
                if (cursor.getString(index).orEmpty().contains(marker)) return true
            }
        }
        return false
    }
}
