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

fun <K, V, K2, V2> Map<K, V>.mapEntries(fct: (Map.Entry<K, V>) -> Pair<K2, V2>): Map<K2, V2> {
    val capacity = if (size < 3) size + 1 else ((size / 0.75F) + 1.0F).toInt()
    val destination = LinkedHashMap<K2, V2>(capacity)
    for (entry in this) {
        val pair = fct(entry)
        destination[pair.first] = pair.second
    }
    return destination
}
// Reuse the receiver when it's already mutable (avoiding a copy), otherwise make a safe mutable copy.
// This avoids the ClassCastException that a blind `as MutableMap` cast would throw on a read-only collection.
fun <K, V> Map<K, V>.mutable(): MutableMap<K, V> = this as? MutableMap<K, V> ?: toMutableMap()
fun <T> List<T>.mutable(): MutableList<T> = this as? MutableList<T> ?: toMutableList()

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

fun <K, V> MutableMap<K, V>.clearThen() = this.apply { this.clear() }
fun <T> MutableList<T>.clearThen() = this.apply { this.clear() }
