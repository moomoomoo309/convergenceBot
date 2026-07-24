package convergence

import java.io.ByteArrayOutputStream
import java.io.PrintStream

class CommandDoesNotExist(cmd: String): Exception(cmd)

internal fun getStackTraceText(e: Exception): String = ByteArrayOutputStream().let {
    e.printStackTrace(PrintStream(it))
    it.toString("UTF8")
}

/**
 * Service for managing command registration and execution.
 * Demonstrates how to use Koin dependency injection.
 *
 * New code should follow this pattern: receive dependencies via constructor injection
 * rather than referencing global top-level vals.
 */
class CommandRegistryService(
    private val botState: BotState,
    private val settings: Settings,
    private val messaging: MessagingService,
    private val commandParser: CommandParserService
) {
    /**
     * Run the Command in the given message, or do nothing if none exists.
     */
    fun runCommand(chat: Chat, message: IncomingMessage, sender: User, images: Array<Image> = emptyArray()) {
        val text = message.toSimple().text
        messageLogger.info(
            "[${if (chat is HasServer<*>) chat.server.name + "#" else ""}${chat.name}]" +
                    " ${messaging.getUserName(chat, sender)}: $text" +
                    if (images.isNotEmpty()) " +${images.size} images" else ""
        )
        messaging.forwardToLinkedChats(chat, message.toOutgoing(), sender, images)
        try {
            commandParser.parseSafe(chat, text, sender, ::getCommand)?.let { (command, args) ->
                messaging.sendMessage(chat, sender, command.function(args, chat, sender))
            }
        } catch(e: Exception) {
            messaging.sendMessage(
                chat, sender,
                "Error while running command! Stack trace:\n" +
                        "${if (settings.debugMode) getStackTraceText(e) else e.message}"
            )
            if (!settings.debugMode)
                defaultLogger.error("Error while running command!", e)
        }
    }

    /**
     * Adds a command to the command registry.
     * @return true if a command with that name does not already exist in the registry, false otherwise.
     */
    fun registerCommand(command: Command): Boolean {
        val protocol = command.protocol
        val commandsInProtocol = botState.commands.getOrPut(protocol) { mutableMapOf() }

        if (command.name.lowercase() in commandsInProtocol)
            return false

        commandsInProtocol[command.name.lowercase()] = command
        return true
    }

    /**
     * Adds an alias to the alias registry.
     * @return true if an alias with that name does not already exist in the registry, false otherwise.
     */
    fun registerAlias(alias: Alias): Boolean {
        val chat = alias.scope
        val aliases = settings.aliases
        val aliasesInChat = aliases.getOrPut(chat) { mutableMapOf() }

        if (alias.name.lowercase() in aliasesInChat)
            return false

        alias.protocol.aliasCreated(alias)
        aliasesInChat[alias.name.lowercase()] = alias
        return true
    }

    /**
     * Looks up a command by name within a chat's scope.
     * Resolution order: chat alias → server alias → protocol command → universal command.
     */
    fun getCommand(command: String, chat: Chat): CommandLike {
        return when {
            // Chat Alias
            commandAvailable(settings.aliases, chat, command) -> settings.aliases[chat]!![command]
            // Server Alias
            chat is HasServer<*> && commandAvailable(
                settings.aliases,
                chat.server,
                command
            ) -> settings.aliases[chat.server]!![command]
            // Protocol Command
            commandAvailable(botState.commands, chat.protocol, command) -> botState.commands[chat.protocol]!![command]
            // Universal Command
            commandAvailable(
                botState.commands,
                UniversalProtocol,
                command
            ) -> botState.commands[UniversalProtocol]!![command]

            else -> null
        } ?: throw CommandDoesNotExist(command)
    }

    companion object {
        private fun <CommandType: CommandLike, ScopeType> commandAvailable(
            list: MutableMap<ScopeType, MutableMap<String, CommandType>>,
            scope: ScopeType,
            command: String
        ) = scope in list && command in list[scope]!!
    }
}
