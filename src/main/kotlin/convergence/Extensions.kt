package convergence

import com.sigpwned.emoji4j.core.Grapheme
import com.sigpwned.emoji4j.core.GraphemeMatcher
import com.sigpwned.emoji4j.core.grapheme.Emoji
import convergence.discord.calendar.defaultZoneOffset
import java.time.OffsetDateTime
import java.util.*
import kotlin.streams.asSequence

// This file has all the extension functions that aren't related to a specific file.
// I.E: CalendarProcessor has some calendar event-related extensions, those can stay there.
// This file has stuff on Lists or Strings or Maps, more generic stuff.

fun <K, V, K2, V2> Map<K, V>.mapEntries(fct: (Map.Entry<K, V>) -> Pair<K2, V2>): Map<K2, V2> {
    val destination = LinkedHashMap<K2, V2>(mapCapacity(size))
    for (entry in this) {
        val pair = fct(entry)
        destination[pair.first] = pair.second
    }
    return destination
}
fun <K, V> Map<K, V>.mutable() = this as MutableMap<K, V>


// This is copied straight from the Kotlin stdlib.
private fun mapCapacity(expectedSize: Int): Int = when {
    expectedSize < 0 -> expectedSize
    expectedSize < 3 -> expectedSize + 1
    expectedSize < INT_MAX_POWER_OF_TWO -> ((expectedSize / 0.75F) + 1.0F).toInt()
    else -> Int.MAX_VALUE
}
private const val INT_MAX_POWER_OF_TWO: Int = 1 shl (Int.SIZE_BITS - 2)

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
    val grapheme = GraphemeMatcher(this).results().asSequence().first().grapheme()
    return if (grapheme.type == Grapheme.Type.EMOJI)
        grapheme as Emoji
    else
        null
}

fun <K, V> MutableMap<K, V>.clearThen() = this.apply { this.clear() }
fun <T> MutableList<T>.clearThen() = this.apply { this.clear() }
