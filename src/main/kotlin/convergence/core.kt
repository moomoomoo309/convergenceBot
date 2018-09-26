package convergence

class ProtocolAlreadyExists: Exception()
class CommandAlreadyExists: Exception()
class CommandDoesNotExist: Exception()
class InvalidCommandDelimiter: Exception()

val protocols = mutableMapOf<String, BaseInterface>()
fun registerProtocol(protocol: BaseInterface) {
    if (protocols.containsKey(protocol.name))
        throw ProtocolAlreadyExists()
    protocols[protocol.name] = protocol
}


val commands = mutableMapOf<Chat, MutableMap<String, Command>>()
fun registerCommand(chat: Chat, command: Command) {
    if (!commands.containsKey(chat))
        commands[chat] = mutableMapOf(command.name to command)

    if (commands[chat]!!.contains(command.name))
        throw CommandAlreadyExists()
    commands[chat]!![command.name] = command
}

val commandDelimiters = mutableMapOf<Chat, String>()
/**
 * Sets the Command delimiter used for the bot's commands. (is it !help, |help, @help, or something else?)
 */
fun setCommandDelimiter(chat: Chat, commandDelimiter: String) {
    if (commandDelimiter.any { it.isWhitespace() || it == '"' })
        throw InvalidCommandDelimiter()
    commandDelimiters[chat] = commandDelimiter
}

val aliases = mutableMapOf<Chat, MutableMap<String, Alias>>()
fun setAlias(chat: Chat, alias: Alias) {
    if (aliases[chat] !is MutableMap<String, Alias>)
        aliases[chat] = mutableMapOf(alias.name to alias)
    else
        (aliases[chat] as MutableMap<String, Alias>)[alias.name] = alias
}

/**
 * Run the Command in the given message, or do nothing if none exists.
 */
fun runCommand(message: String) {

}

