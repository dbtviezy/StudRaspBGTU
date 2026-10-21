package ru.bgtu.schedule.data

/**
 * Rough campus building from an audience string, enough to warn about a walk between classes.
 * А213 / Б205 → letter; 57Б → letter; plain numbers → main (#); named halls stay as themselves.
 */
object Buildings {
    fun key(room: String): String? {
        val raw = room.trim()
        if (raw.isBlank()) return null
        val upper = raw.uppercase()
        if (upper.startsWith("АУД")) return "Д"
        if (upper.startsWith("ЧИТ") || upper.startsWith("СПОР") || upper.startsWith("ЛАБ") ||
            upper.startsWith("КРЕМ") || upper.startsWith("БЭМ") || upper.startsWith("1-Т")
        ) {
            return upper.take(16)
        }
        val first = upper.first()
        if (first in '\u0410'..'\u042F' || first in 'A'..'Z') return first.toString()
        Regex("""(\d+)\s*([\u0410-\u042FA-Z])\b""").find(upper)?.let { return it.groupValues[2] }
        if (upper.any { it.isDigit() }) return "#"
        return upper.take(20)
    }

    fun different(previousRoom: String?, nextRoom: String?): Boolean {
        val a = previousRoom?.let { key(it) } ?: return false
        val b = nextRoom?.let { key(it) } ?: return false
        return a != b
    }
}
