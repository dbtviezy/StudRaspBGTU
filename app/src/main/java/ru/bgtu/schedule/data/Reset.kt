package ru.bgtu.schedule.data

import android.content.Context

private val USER_PREFS = listOf(
    "bgtu_hidden",
    "bgtu_subject_notes",
    "bgtu_absences",
    "bgtu_favorite_teachers",
    "bgtu_day_overrides",
    "bgtu_absence_limits",
    "bgtu_todos",
    "bgtu_announcements",
)

fun wipeUserData(context: Context, deleteFiles: Boolean) {
    val app = context.applicationContext
    Prefs(app).wipeUserData()
    USER_PREFS.forEach { app.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
    avatarFile(app).delete()
    Account.forget()
    ChatStore.clear()
    NearbyChat.stop()
    NearbyChat.clear()
    if (deleteFiles) {
        libraryDir(app).deleteRecursively()
        app.getSharedPreferences("bgtu_file_tags", Context.MODE_PRIVATE).edit().clear().commit()
    }
}
