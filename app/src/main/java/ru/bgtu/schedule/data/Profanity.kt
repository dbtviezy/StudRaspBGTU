package ru.bgtu.schedule.data

/**
 * Masks swear words as "б***" for people who turned the filter on. Runs only on the phone.
 * Keep the patterns identical to `web/src/lib/profanity.ts`.
 */
object Profanity {
    private val word = Regex("\\p{L}+")

    private val patterns = listOf(
        "^(на|по|за|вы|от|до|ни|о|у)?ху[йеяию]",
        "пизд",
        "^(за|вы|до|на|по|от|отъ|раз|разъ|рас|съ|въ|объ|подъ|об|у|при|пере|недо|долбо)?еб(а|у|л|н|и|ш|т|е|ы|$)",
        "^бля(д|т|$)",
        "^сук(а|и|ой|у|е|ам)$",
        "^сучк",
        "^муда[кч]",
        "^мудил",
        "^п[ие]д[оа]р",
        "^г[ао]ндон",
        "залуп",
        "^шлю[хш]",
        "fuck",
        "^(shit|bitch|cunt|asshole|motherf|dickhead)",
    ).map { Regex(it) }

    private val lookalikes = mapOf(
        'a' to 'а', 'e' to 'е', 'o' to 'о', 'p' to 'р', 'c' to 'с', 'x' to 'х', 'y' to 'у', 'k' to 'к', 'b' to 'в', 'm' to 'м', 'h' to 'н', 't' to 'т',
    )

    private fun normalize(raw: String): String {
        val lower = raw.lowercase().replace('ё', 'е')
        val cyrillic = lower.any { it in 'а'..'я' }
        return if (cyrillic) lower.map { lookalikes[it] ?: it }.joinToString("") else lower
    }

    fun isBad(raw: String): Boolean {
        val text = normalize(raw)
        return patterns.any { it.containsMatchIn(text) }
    }

    fun mask(text: String): String = word.replace(text) { match ->
        val value = match.value
        if (value.length > 1 && isBad(value)) value.first() + "*".repeat(value.length - 1) else value
    }

    fun maskIf(enabled: Boolean, text: String): String = if (enabled) mask(text) else text
}
