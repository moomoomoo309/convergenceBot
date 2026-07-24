package convergence

import java.util.concurrent.atomic.AtomicInteger

class BotState {
    val chatMap: MutableMap<Int, Chat> = mutableMapOf()
    val reverseChatMap: MutableMap<Chat, Int> = mutableMapOf()
    val commands: MutableMap<Protocol, MutableMap<String, Command>> = mutableMapOf()
    val protocols: MutableList<Protocol> = mutableListOf()
    val currentChatID: AtomicInteger = AtomicInteger(0)
    val aliasVars: MutableMap<String, (chat: Chat, sender: User) -> String?> = mutableMapOf(
        "%sender" to { c: Chat, s: User -> c.protocol.getUserName(c, s) },
        "%nick" to { c, s -> (c.protocol as? HasNicknames)?.getUserNickname(c, s) },
        "%botname" to { c: Chat, _: User -> c.protocol.getUserName(c, c.protocol.getBot(c)) },
        "%chatname" to { c: Chat, _: User -> c.protocol.getChatName(c) }
    )
    val messageCallbacks: MutableList<MessageCallback> = mutableListOf(
        { chat, message, sender ->
            commandRegistryService.runCommand(chat, message, sender)
        }
    )

    private val commandRegistryService: CommandRegistryService by lazy { getKoinService<CommandRegistryService>() }

    fun scopeStrToProtocol(s: String) = protocols.sortedBy { -it.name.length }
        .firstOrNull { s.substringBefore("(").startsWith(it.name) }
}

val bot = BotState()
