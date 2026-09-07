package convergence.commands

import convergence.formatTime
import convergence.settings
import convergence.updateSettings
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun createTimer(args: List<String>): String {
    if (args.isEmpty()) {
        return "A name has to be provided."
    }
    val name = args.joinToString(" ")
    if (name in settings.timers)
        return "That timer already exists!"
    settings.timers[name] = OffsetDateTime.now()
    updateSettings()
    return "New timer \"$name\" created."
}

fun resetTimer(args: List<String>): String {
    if (args.isEmpty()) {
        return "A name has to be provided."
    }
    val name = args.joinToString(" ")
    if (name !in settings.timers)
        return "That timer doesn't exist!"
    val oldVal = settings.timers[name]
    settings.timers[name] = OffsetDateTime.now()
    val timestamp = oldVal!!.format(DateTimeFormatter.ISO_INSTANT)
    updateSettings()
    return "Timer reset. " +
            "The time it was created or last time the timer was reset was ${formatTime(oldVal)} ($timestamp)."
}

fun checkTimer(args: List<String>): String {
    if (args.isEmpty())
        return "A name has to be provided."
    val name = args.joinToString(" ")
    if (name !in settings.timers)
        return "That timer doesn't exist!"
    val oldVal = settings.timers[name]!!.truncatedTo(ChronoUnit.SECONDS)
    val timestamp = oldVal!!.format(DateTimeFormatter.ISO_INSTANT)
    updateSettings()
    return "The time it was created or last time the timer was reset was ${formatTime(oldVal)} ($timestamp)."
}
