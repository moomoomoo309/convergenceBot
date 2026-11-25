package convergence

import convergence.command.Command
import convergence.command.processMessage
import convergence.model.Chat
import convergence.model.Image
import convergence.model.IncomingMessage
import convergence.model.User
import convergence.protocol.HasNicknames
import java.util.concurrent.atomic.AtomicInteger

typealias MessageCallback = (chat: Chat<*>, sender: User<*>, message: IncomingMessage<*>, images: Array<out Image>) -> Unit
class BotState {
    val chatMap: MutableMap<Int, Chat<*>> = mutableMapOf()
    val reverseChatMap: MutableMap<Chat<*>, Int> = mutableMapOf()
    val commands: MutableMap<ProtocolRef, MutableMap<String, Command>> = mutableMapOf()
    val protocols: MutableList<Protocol<*, *, *, *>> = mutableListOf()
    val currentChatID: AtomicInteger = AtomicInteger(0)
    val aliasVars: MutableMap<String, (chat: Chat<*>, sender: User<*>) -> String?> = mutableMapOf(
        "%sender" to { c: Chat<*>, s: User<*> -> c.protocol.getUserNameFor(c, s) },
        "%nick" to { c, s -> (c.protocol as? HasNicknames<*, *>)?.getUserNicknameFor(c, s) },
        "%botname" to { c: Chat<*>, _: User<*> -> c.protocol.getUserNameFor(c, c.protocol.getBotOf(c)) },
        "%chatname" to { c: Chat<*>, _: User<*> -> c.protocol.getChatNameOf(c) }
    )
    val messageCallbacks: MutableList<MessageCallback> = mutableListOf(::processMessage)

    fun scopeStrToProtocol(s: String) = protocols.sortedBy { -it.name.length }
        .firstOrNull { s.substringBefore("(").startsWith(it.name) }
}

val bot = BotState()
