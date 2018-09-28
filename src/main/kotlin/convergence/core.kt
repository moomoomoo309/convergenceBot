package convergence

class CommandDoesNotExist(cmd: String): Exception(cmd)

val protocols = mutableMapOf<String, BaseInterface>()
/**
 * Adds a protocol to the protocol registry.
 * @return true if a protocol with that name does not already exist in the registry, false otherwise.
 */
fun registerProtocol(protocol: BaseInterface): Boolean {
    if (protocol.name in protocols)
        return false
    protocols[protocol.name] = protocol
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
    if (chat.protocol.name !in protocols) {
        System.err.println("Protocol \"${chat.protocol.name}\" not properly registered!")
        return
    }
    val currentProtocol = protocols[chat.protocol.name]!!
    val bot = currentProtocol.getBot(chat)
    currentProtocol.sendMessage(chat, message, bot)
}

fun getUserName(chat: Chat, sender: User): String {
    val protocol = protocols[chat.protocol.name]!!
    return if (protocol is INickname)
        protocol.getUserNickname(chat, sender) ?: protocol.getName(chat, sender)
    else
        protocol.getName(chat, sender)
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
        sendMessage(chat, "Invalid escape sequence passed. Are your backslashes correct?")
        return
    }
    sendMessage(chat, commandData?.command?.function?.invoke(chat, commandData.args, sender))
}

