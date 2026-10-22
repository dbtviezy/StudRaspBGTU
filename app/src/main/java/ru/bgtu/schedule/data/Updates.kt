package ru.bgtu.schedule.data

import android.content.Context

/** Asks GitHub for the latest release at most once a day; the admin APK is updated by hand and skips this. */
object Updates {
    const val DOWNLOAD_URL = "https://github.com/dbtviezy/StudRaspBGTU/releases/latest/download/StudRaspBGTU.apk"
    private const val LATEST = "https://api.github.com/repos/dbtviezy/StudRaspBGTU/releases/latest"
    private const val FILE = "bgtu_updates"
    private const val DAY = 24 * 3600 * 1000L

    /** Returns the newer version tag (like "v1.3.0"), or null. */
    suspend fun newerVersion(context: Context, current: String): String? {
        val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val cached = sp.getString("latest", null)
        val tag = if (now - sp.getLong("checked_at", 0L) < DAY && cached != null) {
            cached
        } else {
            val reply = Http.call("GET", LATEST, headers = mapOf("Accept" to "application/vnd.github+json")) ?: return null
            if (!reply.ok) return null
            val fresh = reply.json()?.optString("tag_name").orEmpty()
            if (fresh.isBlank()) return null
            sp.edit().putString("latest", fresh).putLong("checked_at", now).apply()
            fresh
        }
        if (tag == sp.getString("dismissed", null) && now - sp.getLong("dismissed_at", 0L) < 3 * DAY) return null
        return tag.takeIf { isNewer(it, current) }
    }

    /** "Later": ask again in three days. */
    fun dismiss(context: Context, tag: String) {
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("dismissed", tag)
            .putLong("dismissed_at", System.currentTimeMillis())
            .apply()
    }

    private fun parts(version: String): List<Int> =
        version.trim().removePrefix("v").split('.', '-').map { it.toIntOrNull() ?: 0 }

    fun isNewer(candidate: String, current: String): Boolean {
        val a = parts(candidate)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size, 3)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
