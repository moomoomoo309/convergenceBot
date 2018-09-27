package convergence

class CommandDoesNotExist: Exception()

val protocols = mutableMapOf<String, BaseInterface>()
fun registerProtocol(protocol: BaseInterface): Boolean {
    if (protocols.containsKey(protocol.name))
        return false
    protocols[protocol.name] = protocol
    return true
}


val commands = mutableMapOf<Chat, MutableMap<String, Command>>()
fun registerCommand(chat: Chat, command: Command): Boolean {
    if (!commands.containsKey(chat))
        commands[chat] = mutableMapOf(command.name to command)

    if (commands[chat]!!.contains(command.name))
        return false
    commands[chat]!![command.name] = command
    return true
}

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

