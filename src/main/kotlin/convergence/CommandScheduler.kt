package convergence

import convergence.discord.CalendarProcessor
import to.lova.humanize.time.HumanizeTime
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * Checks if commands scheduled for later are ready to be run yet. Can give information on the commands in its queue.
 */
@Suppress("ConstPropertyName")
object CommandScheduler: Thread() {
    private const val allowedTimeDifferenceSeconds = 30
    private const val updatesPerSecond = 1
    private val calendarUpdateFrequency = Duration.ofMinutes(15L)

    private val scheduledCommands = sortedMapOf<OffsetDateTime, MutableList<ScheduledCommand>>()
    private val commandsList = sortedMapOf<Int, ScheduledCommand>()
    private var currentId: Int = 0
    private var lastCalendarUpdateTime = Instant.MIN

    fun loadFromFile() {
        serializedCommands.values.forEach { cmd ->
            commandsList[cmd.id] = cmd
            scheduledCommands.getOrPut(cmd.time) { mutableListOf(cmd) }
        }
    }

    override fun run() {
        while (isAlive) {
            val now = OffsetDateTime.now()
            for ((cmdTime, cmdList) in scheduledCommands) {
                if (cmdTime.isBefore(now)) {
                    if (cmdTime.until(now, ChronoUnit.SECONDS) in 0..allowedTimeDifferenceSeconds) {
                        for (cmd in cmdList) {
                            runCommand(cmd.chat, cmd.sender, cmd.commandData)
                            scheduledCommands.remove(cmd.time)
                            commandsList.remove(cmd.id)
                        }
                    } else {
                        for (cmd in cmdList) {
                            scheduledCommands.remove(cmd.time)
                            commandsList.remove(cmd.id)
                        }
                    }
                } else // It's already sorted chronologically, so all following events are early.
                    break
            }
            if ((lastCalendarUpdateTime + calendarUpdateFrequency).isBefore(Instant.now())) {
                defaultLogger.info("Syncing calendars...")
                for (calendar in syncedCalendars)
                    Thread {
                        CalendarProcessor.syncToDiscord(calendar)
                    }.start()
                lastCalendarUpdateTime = Instant.now()
            }
            sleep((1000.0 / updatesPerSecond).toLong())
        }
    }

    /**
     * Schedules [command] sent by [sender] to run at [time].
     * @return The response the user will get from the command.
     */
    fun schedule(chat: Chat, sender: User, command: CommandData, time: OffsetDateTime): String {
        if (time !in scheduledCommands)
            scheduledCommands[time] = mutableListOf()
        val cmd = ScheduledCommand(time, chat, sender, command, currentId)
        while (commandsList.containsKey(currentId))
            currentId++
        scheduledCommands[time]!!.add(cmd)
        if (cmd.id in commandsList)
            defaultLogger.error("Duplicate IDs in schedulerThread!")
        commandsList[cmd.id] = cmd
        serializedCommands[cmd.id] = cmd
        Settings.update()
        return "Scheduled ${getUserName(chat, sender)} to run \"$command\" to run at $time."
    }

    fun schedule(scheduled: ScheduledCommand) =
        schedule(scheduled.chat, scheduled.sender, scheduled.commandData, scheduled.time)

    /**
     * Gets all the commands scheduled by [sender].
     */
    fun getCommands(sender: User?): List<ScheduledCommand> =
        if (sender == null)
            getCommands()
        else
            commandsList.values.filter { it.sender == sender }

    /**
     * Gets all the commands scheduled.
     */
    fun getCommands(): List<ScheduledCommand> = commandsList.values.toList()

    /**
     * Removes a command from the queue.
     */
    fun unschedule(index: Int) = commandsList.remove(index)?.let {
        serializedCommands.remove(it.id)
        scheduledCommands[it.time]?.remove(it)
    } != null
}

fun formatTime(time: OffsetDateTime): String = HumanizeTime.fromNow(time)

data class ScheduledCommand(
    val time: OffsetDateTime,
    val chat: Chat,
    val sender: User,
    val commandData: CommandData,
    val id: Int,
)

/**
 * Schedules [command] to run at [time] from [sender].
 */
fun schedule(chat: Chat, sender: User, command: CommandData, time: OffsetDateTime) =
    CommandScheduler.schedule(chat, sender, command, time)
