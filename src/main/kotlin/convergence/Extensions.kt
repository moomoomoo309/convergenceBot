package convergence

import com.sigpwned.emoji4j.core.Grapheme
import com.sigpwned.emoji4j.core.GraphemeMatcher
import com.sigpwned.emoji4j.core.grapheme.Emoji
import convergence.discord.calendar.defaultZoneOffset
import java.time.OffsetDateTime
import java.util.*
import kotlin.jvm.optionals.getOrNull

// This file has all the extension functions that aren't related to a specific file.
// I.E: CalendarProcessor has some calendar event-related extensions, those can stay there.
// This file has stuff on Lists or Strings or Maps, more generic stuff.

fun String.substringBetween(startDelimiter: String, endDelimiter: String): String {
    val startIndex = this.indexOf(startDelimiter)
    val endIndex = this.lastIndexOf(endDelimiter)
    if (startIndex == -1 || endIndex == -1) {
        return ""
    }
    return this.substring(startIndex + startDelimiter.length, endIndex)
}

fun String.titleCase() = "${first().uppercase()}${substring(1).lowercase()}"
fun Date.toOffsetDatetime(): OffsetDateTime = this.toInstant().atOffset(defaultZoneOffset)

fun String.toEmoji(): Emoji? {
    val result = GraphemeMatcher(this).results().findFirst().getOrNull() ?: return null
    // Ensure the match covers the entire string, not just a substring
    if (result.start() != 0 || result.end() != this.length) return null
    val grapheme = result.grapheme()
    return if (grapheme?.type == Grapheme.Type.EMOJI) grapheme as Emoji else null
}
