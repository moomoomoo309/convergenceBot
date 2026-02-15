package convergence

import convergence.discord.calendar.CalendarProcessor
import org.ocpsoft.prettytime.PrettyTime
import org.ocpsoft.prettytime.units.JustNow
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * Checks if commands scheduled for later are ready to be run yet. Can give information on the commands in its queue.
 */
@Suppress("ConstPropertyName")
object CommandScheduler: Thread() {
    private const val allowedTimeDifferenceSeconds = 30
    private const val updatesPerSecond = 1

    private val scheduledCommands = sortedMapOf<OffsetDateTime, MutableList<ScheduledCommand>>()
    private val commandsList = sortedMapOf<Int, ScheduledCommand>()
    private var currentId: Int = 0
    val taskList = mutableListOf<ScheduledTask>()

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
                            cmd()
                            scheduledCommands.remove(cmd.time)
                            commandsList.remove(cmd.id)
                            Settings.serializedCommands.remove(cmd.id)
                        }
                        if (cmdList.isNotEmpty())
                            Settings.update()
                    } else {
                        for (cmd in cmdList) {
                            scheduledCommands.remove(cmd.time)
                            commandsList.remove(cmd.id)
                            Settings.serializedCommands.remove(cmd.id)
                        }
                        if (cmdList.isNotEmpty())
                            Settings.update()
                    }
                } else // It's already sorted chronologically, so all following events are early.
                    break
            }
            val iter = taskList.iterator()
            while (iter.hasNext()) {
                val task = iter.next()
                if (now.isAfter(task.time)) {
                    task.fct.run()
                    iter.remove()
                }
            }
            CalendarProcessor.onUpdate()
            sleep((1000.0 / updatesPerSecond).toLong())
        }
    }

    /**
     * Schedules [commandName] sent by [sender] to run at [time] with [args] as its arguments.
     * @return The response the user will get from the command.
     */
    fun schedule(chat: Chat, sender: User, commandName: String, args: List<String>, time: OffsetDateTime): String {
        if (time !in scheduledCommands)
            scheduledCommands[time] = mutableListOf()
        val cmd = ScheduledCommand(time, chat, sender, chat.protocol.name, commandName, args, currentId)
        while (commandsList.containsKey(currentId))
            currentId++
        scheduledCommands[time]!!.add(cmd)
        if (cmd.id in commandsList)
            defaultLogger.error("Duplicate IDs in schedulerThread!")
        commandsList[cmd.id] = cmd
        serializedCommands[cmd.id] = cmd
        Settings.update()
        return "Scheduled ${getUserName(chat, sender)} to run " +
                "\"$commandName ${args.joinToString(" ")}\" ${formatTime(time)} ($time)."
    }

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

private val prettyTime = PrettyTime().also { it.removeUnit(JustNow::class.java) }
fun formatTime(time: OffsetDateTime): String = prettyTime.format(time)

interface Schedulable {
    val time: OffsetDateTime
}

/**
 * A function that will run at a future time.
 */
data class ScheduledTask(
    override val time: OffsetDateTime,
    val fct: Runnable
): Schedulable

/**
 * A command sent by a user to run at a future time.
 */
data class ScheduledCommand(
    override val time: OffsetDateTime,
    val chat: Chat,
    val sender: User,
    val protocolName: String,
    val commandName: String,
    val args: List<String>,
    val id: Int,
): Schedulable {
    fun toDTO() = ScheduledCommandDTO(
        time, chat.toKey(), sender.toKey(), protocolName, commandName, args, id
    )

    operator fun invoke() = runCommand(chat, sender, getCommand(commandName.lowercase(), chat) as Command, args)
}
