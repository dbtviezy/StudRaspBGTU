package ru.bgtu.schedule.data

/** One field that decides which notification kinds are allowed. Easy to extend later. */
object NotifyProfile {
    const val NORMAL = "normal"
    const val QUIET = "quiet"
    const val SESSION = "session"
    const val OFF = "off"

    val ALL = listOf(NORMAL, QUIET, SESSION, OFF)

    fun clean(value: String?): String = value?.takeIf { it in ALL } ?: NORMAL

    fun allowsLessons(profile: String) = profile == NORMAL

    fun allowsDigest(profile: String) = profile == NORMAL

    fun allowsDeadlines(profile: String) = profile != OFF

    fun allowsChats(profile: String) = profile == NORMAL || profile == QUIET

    fun allowsOngoing(profile: String) = profile == NORMAL
}
