package convergence.commands

import convergence.formatTime
import convergence.settings
import convergence.updateSettings
import java.time.OffsetDateTime

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
    updateSettings()
    return "Timer reset. The time it was created or last time the timer was reset was " +
            "${formatTime(oldVal!!)} ($oldVal)."
}

fun checkTimer(args: List<String>): String {
    if (args.isEmpty())
        return "A name has to be provided."
    val name = args.joinToString(" ")
    if (name !in settings.timers)
        return "That timer doesn't exist!"
    val oldVal = settings.timers[name]
    updateSettings()
    return "The time it was created or last time the timer was reset was ${formatTime(oldVal!!)} ($oldVal)."
}
