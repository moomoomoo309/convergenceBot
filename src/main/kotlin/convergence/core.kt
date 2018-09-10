package convergence

import java.util.function.Consumer

class ProtocolAlreadyExists: Exception()
class CommandAlreadyExists: Exception()
class CommandDoesNotExist: Exception()

val protocols = mutableMapOf<String, BaseInterface>()
fun registerProtocol(protocol: BaseInterface) {
    if (protocols.containsKey(protocol.name))
        throw ProtocolAlreadyExists()
    protocols[protocol.name] = protocol
}

data class Command(val name: String, val function: Consumer<Array<String>>, val helpText: String, val syntaxText: String)
val universalCommands = mutableMapOf<Chat, MutableMap<String, Command>>()
fun registerUniversalCommand(chat: Chat, command: Command) {
    // Make sure the map exists, and if it does, add the Command.
    if (!universalCommands.containsKey(chat))
        universalCommands[chat] = mutableMapOf(command.name to command)

    // The cast is required here because the key could be undefined, and the compiler decided not to smart cast it.
    if (command.name in universalCommands[chat]!!)
        throw CommandAlreadyExists()
    universalCommands[chat]!![command.name] = command
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
    commandDelimiters[chat] = commandDelimiter
}

data class Alias(val command: String, val helpText: String, val syntaxText: String)
val aliases = mutableMapOf<Chat, Alias>()
fun setAlias(chat: Chat, alias: Alias) {
    aliases[chat] = alias
}

/**
 * Run the Command in the given message, or do nothing if none exists.
 */
fun runCommand(message: String) {

}

