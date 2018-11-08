@file:Suppress("unused", "UNUSED_PARAMETER")

package convergence

import java.lang.StringBuilder
import java.time.LocalDateTime
import java.util.*

class CommandDoesNotExist(cmd: String): Exception(cmd)

val protocols = mutableSetOf<Protocol>()
/**
 * Adds a protocol to the protocol registry.
 * @return true if a protocol with that name does not already exist in the registry, false otherwise.
 */
fun registerProtocol(protocol: Protocol): Boolean {
    if (protocol in protocols)
        return false
    protocols += protocol
    return true
}

val sortedHelpText = arrayListOf<CommandLike>()
val commands = mutableMapOf<Chat, MutableMap<String, Command>>()
/**
 * Adds a command to the command registry.
 * @return true if a command with that name does not already exist in the registry, false otherwise.
 */
fun registerCommand(chat: Chat, command: Command): Boolean {
    if (chat !in commands || commands[chat] !is MutableMap)
        commands[chat] = mutableMapOf(command.name to command)

    if (command.name in commands[chat]!!)
        return false

    if (sortedHelpText.isNotEmpty())
        for (i in 0..sortedHelpText.size) {
            if (command.name < sortedHelpText[i].name || i == sortedHelpText.size - 1) {
                sortedHelpText.add(i, command)
                break
            }
        }
    else
        sortedHelpText[0] = command

    commands[chat]!![command.name] = command
    return true
}

val aliases = mutableMapOf<Chat, MutableMap<String, Alias>>()
/**
 * Adds an alias to the alias registry.
 * @return true if an alias with that name does not already exist in the registry, false otherwise.
 */
fun registerAlias(chat: Chat, alias: Alias): Boolean {
    if (chat !in aliases || aliases[chat] !is MutableMap<String, Alias>)
        aliases[chat] = mutableMapOf(alias.name to alias)

    if (alias.name in aliases[chat]!!)
        return false

    if (sortedHelpText.isNotEmpty())
        for (i in 0..sortedHelpText.size) {
            if (alias.name < sortedHelpText[i].name || i == sortedHelpText.size - 1) {
                sortedHelpText.add(i, alias)
                break
            }
        }
    else
        sortedHelpText[0] = alias

    aliases[chat]!![alias.name] = alias
    return true
}

const val defaultCommandDelimiter = "!"
val commandDelimiters = mutableMapOf<Chat, String>()
/**
 * Sets the Command delimiter used for the bot's commands. (is it !help, |help, @help, or something else?)
 */
fun setCommandDelimiter(chat: Chat, commandDelimiter: String): Boolean {
    if (commandDelimiter.any { it.isWhitespace() || it == '"' })
        return false
    commandDelimiters[chat] = commandDelimiter
    return true
}

fun sendMessage(chat: Chat, message: String?) {
    if (message == null)
        return
    if (chat.protocol !in protocols) {
        System.err.println("Protocol \"${chat.protocol.name}\" not properly registered!")
        return
    }
    val baseInterface = chat.protocol.baseInterface
    val bot = baseInterface.getBot(chat)
    baseInterface.sendMessage(chat, message, bot)
}

fun getUserName(chat: Chat, sender: User): String {
    val baseInterface = chat.protocol.baseInterface
    return if (baseInterface is INickname)
        baseInterface.getUserNickname(chat, sender) ?: baseInterface.getName(chat, sender)
    else
        baseInterface.getName(chat, sender)
}

val aliasVars = mutableSetOf("sender", "botname", "chatname")
fun replaceAliasVars(chat: Chat, message: String?, sender: User): String? {
    if (message == null)
        return null
    val stringBuilder = StringBuilder()
    var charIndex = -1
    val bitSet = BitSet(aliasVars.size)
    var anyTrue = false
    for (currentChar in message) {
        stringBuilder.append(currentChar)
        if (currentChar == '%')
            charIndex = 0
        if (charIndex >= 0) {
            var i = -1
            for (aliasVar in aliasVars) {
                i++ // I didn't use forEachIndexed here because of the return later on.
                if (bitSet[i]) {
                    anyTrue = true
                    if (charIndex == aliasVar.length) {
                        val baseInterface = chat.protocol.baseInterface
                        stringBuilder.setLength(stringBuilder.length - aliasVar.length - 1)
                        stringBuilder.append(when (aliasVar) {
                            "sender" -> baseInterface.getName(chat, sender)
                            "botname" -> baseInterface.getName(chat, baseInterface.getBot(chat))
                            "chatname" -> baseInterface.getChatName(chat)
                            else -> return "Invalid alias var! This is a bug that shouldn't happen!"
                        })
                        charIndex = -1
                        break
                    }
                    if (currentChar != aliasVar[charIndex])
                        bitSet[i] = false
                }
            }
        }
        charIndex = if (anyTrue) charIndex + 1 else -1
    }
    return stringBuilder.toString()
}

/**
 * Run the Command in the given message, or do nothing if none exists.
 */
fun runCommand(chat: Chat, message: String, sender: User) {
    val commandData: CommandData?
    try {
        commandData = parseCommand(message, commandDelimiters.getOrDefault(chat, defaultCommandDelimiter), chat)
    } catch (e: CommandDoesNotExist) {
        sendMessage(chat, "No command exists with name \"${e.message}\".")
        return
    } catch (e: InvalidEscapeSequence) {
        sendMessage(chat, "Invalid escape sequence \"${e.message}\" passed. Are your backslashes correct?")
        return
    }
    sendMessage(chat, replaceAliasVars(chat, commandData?.command?.function?.invoke(chat, commandData.args, sender), sender))
}

/**
 * Schedules [command] to run at [time] in [chat].
 */
fun schedule(chat: Chat, command: CommandData, time: LocalDateTime): String? {
    return "Not yet implemented."
}

