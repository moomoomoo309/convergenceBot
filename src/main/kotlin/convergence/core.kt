package convergence

val protocols = mutableMapOf<String, BaseInterface>()
fun registerProtocol(protocol: BaseInterface) {

}

data class Command(val name: String, val function: Runnable, val helpText: String, val syntaxText: String)
val universalCommands = mutableMapOf<String, Command>()
fun registerUniversalCommand(chat: Chat, command: Command) {

}

val commands = mutableMapOf<Chat, Command>()
fun registerCommand(command: Command) {

}

val commandDelimiters = mutableMapOf<Chat, String>()
/**
 * Sets the command delimiter used for the bot's commands. (is it !help, |help, @help, or something else?)
 */
fun setCommandDelimiter(chat: Chat, commandDelimiter: String) {
    commandDelimiters[chat] = commandDelimiter
}

data class Alias(val command: String, val helpText: String, val syntaxText: String)
val aliases = mutableMapOf<Chat, Alias>()
fun registerAlias(protocol: BaseInterface, chat: Chat, alias: Alias) {

}

/**
 * Run the command in the given message, or do nothing if none exists.
 */
fun runCommand(message: String) {

}

