package convergence.commands

import convergence.UniversalChat
import convergence.UniversalProtocol
import convergence.bot
import convergence.command.Alias
import convergence.command.Command
import convergence.command.CommandDoesNotExist
import convergence.command.getCommand
import convergence.model.Chat
import convergence.settings
import kotlin.reflect.jvm.jvmName

const val COMMANDS_PER_PAGE = 10

fun help(args: List<String>, chat: Chat): String {
    val sortedCommands = bot.commands.values.flatMap { it.values }.sortedBy { it.name }
    val numPages = ceil(sortedCommands.size.toDouble() / COMMANDS_PER_PAGE)
    val pageOrCommand = if (args.isEmpty()) 1 else args[0].toIntOrNull()?.coerceIn(1..numPages) ?: args[0]
    return when(pageOrCommand) {
        is Int -> buildString {
            append("Help page $pageOrCommand/$numPages:\n")
            for (i in 0 until COMMANDS_PER_PAGE) {
                val index = i + (pageOrCommand - 1) * COMMANDS_PER_PAGE
                if (index >= sortedCommands.size)
                    break
                val currentCommand = sortedCommands[index]
                append("${currentCommand.name} - ${currentCommand.helpText}\n")
            }
        }

        is String -> {
            val currentCommand = try {
                getCommand(pageOrCommand.lowercase(), chat)
            } catch(_: CommandDoesNotExist) {
                return "There is no command with the name \"$pageOrCommand\"."
            }
            buildString {
                append("(")
                if (currentCommand is Command) {
                    append("C) - ")
                    append(currentCommand.helpText)
                    append("\nUsage: ")
                    append(currentCommand.syntaxText)
                } else {
                    append("A) - Runs \"")
                    append((currentCommand as Alias).commandText())
                    append("\"")
                }
            }
        }

        else -> "Expected Int or String, got ${pageOrCommand::class.simpleName ?: pageOrCommand::class.jvmName}."
    }
}

fun commands(chat: Chat): String {
    val commandList = mutableListOf<String>()
    bot.commands[chat.protocol]?.forEach { commandList.add(it.key) }
    bot.commands[UniversalProtocol]?.forEach { commandList.add(it.key) }
    settings.linkedChats[chat]?.forEach { linked -> bot.commands[linked.protocol]?.forEach { commandList.add(it.key) } }
    commandList.sort()
    return if (commandList.isNotEmpty()) commandList.joinToString(", ") else "No commands found."
}

fun aliases(chat: Chat): String {
    val aliasList = mutableListOf<String>()
    settings.aliases[chat]?.forEach { aliasList.add(it.key) }
    settings.aliases[UniversalChat]?.forEach { aliasList.add(it.key) }
    settings.linkedChats[chat]?.forEach { linked -> settings.aliases[linked]?.forEach { aliasList.add(it.key) } }
    aliasList.sort()
    return if (aliasList.isNotEmpty()) "Aliases: ${aliasList.joinToString(", ")}" else "No aliases found."
}

private fun ceil(value: Double): Int = kotlin.math.ceil(value).toInt()
