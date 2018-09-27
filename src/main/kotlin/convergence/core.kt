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


val commands = mutableMapOf<Chat, MutableMap<String, Command>>()
/**
 * Adds a command to the command registry.
 * @return true if a command with that name does not already exist in the registry, false otherwise.
 */
fun registerCommand(chat: Chat, command: Command): Boolean {
    if (!commands.containsKey(chat))
        commands[chat] = mutableMapOf(command.name to command)

    if (commands[chat]!!.contains(command.name))
        return false
    commands[chat]!![command.name] = command
    return true
}

val aliases = mutableMapOf<Chat, MutableMap<String, Alias>>()
/**
 * Adds an alias to the alias registry.
 * @return true if an alias with that name does not already exist in the registry, false otherwise.
 */

fun registerAlias(chat: Chat, alias: Alias) {
    if (aliases[chat] !is MutableMap<String, Alias>)
        aliases[chat] = mutableMapOf(alias.name to alias)
    else
        (aliases[chat] as MutableMap<String, Alias>)[alias.name] = alias
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

/**
 * Run the Command in the given message, or do nothing if none exists.
 */
fun runCommand(chat: Chat, message: String) {
    val commandData: CommandData?
    try {
        commandData = parseCommand(message, commandDelimiters.getOrDefault(chat, defaultCommandDelimiter), chat)
    } catch (e: CommandDoesNotExist) {
        if (chat.protocol.name !in protocols) {
            System.err.println("Protocol \"${chat.protocol.name}\" not properly registered!")
            return
        }
        val currentProtocol = protocols[chat.protocol.name]!!
        val bot = currentProtocol.getBot(chat)
        currentProtocol.sendMessage(chat, "No command exists with name \"${e.message}\".", bot)
        return
    } catch (e: InvalidEscapeSequence) {
        if (chat.protocol.name !in protocols) {
            System.err.println("Protocol \"${chat.protocol.name}\" not properly registered!")
            return
        }
        val currentProtocol = protocols[chat.protocol.name]!!
        val bot = currentProtocol.getBot(chat)
        currentProtocol.sendMessage(chat, "Invalid escape sequence passed. Are your backslashes correct?", bot)
        return
    }
    commandData?.command?.function?.accept(commandData.args)
}

