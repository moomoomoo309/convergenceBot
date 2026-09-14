@file:Suppress("UNCHECKED_CAST")
package convergence.callbacks

import convergence.command.processMessage
import convergence.model.*
import kotlin.reflect.KClass

val callbacks = mutableMapOf<KClass<out ChatEvent>, MutableList<ChatEvent>>(
    ReceivedImages::class to mutableListOf(
        ReceivedImages { chat: Chat, sender: User, message: IncomingMessage?, images: Array<Image> ->
            processMessage(chat, sender, message ?: return@ReceivedImages false, images)
            true
        }
    )
)

fun registerCallback(event: ChatEvent) {
    callbacks.putIfAbsent(event::class, ArrayList())
    callbacks[event::class]?.add(event)
        ?: throw IllegalArgumentException(
            "Tried to register callback for unregistered class ${event::class.simpleName}."
        )
}

fun runCallbacks(eventClass: KClass<out ChatEvent>, vararg args: Any) =
    callbacks[eventClass]?.filter { callback ->
        callback(*args)
    }

inline fun <reified T: ChatEvent> runCallbacks(vararg args: Any) = runCallbacks(T::class, *args)

/**
 * A callback attached to a specific type of event.
 */
interface ChatEvent {
    operator fun invoke(vararg args: Any): Boolean
}

class ChangedNickname(val fct: (chat: Chat, user: User, oldName: String) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any) = args.let { (chat, user, oldName) ->
        fct(chat as Chat, user as User, oldName as String)
    }
    fun invoke(chat: Chat, user: User, oldName: String) = fct(chat, user, oldName)
}

class ReceivedImages(val fct: (Chat, User, IncomingMessage?, Array<Image>) -> Boolean):
    ChatEvent {
    override fun invoke(vararg args: Any) = args.let { (chat, sender, message, images) ->
        fct(chat as Chat, sender as User, message as? IncomingMessage?, images as Array<Image>)
    }
    fun invoke(chat: Chat, sender: User, message: IncomingMessage?, images: Array<Image>): Boolean =
        fct(chat, sender, message ?: SimpleIncomingMessage(""), images)
}

class EditMessage(val fct: (oldMessage: String, sender: User, newMessage: String) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any) = args.let { (oldMessage, sender, newMessage) ->
        fct(oldMessage as String, sender as User, newMessage as String)
    }
    fun invoke(oldMessage: String, sender: User, newMessage: String): Boolean = fct(oldMessage, sender, newMessage)
}

class MentionedUser(val fct: (Chat, sender: User, message: IncomingMessage, users: List<User>) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any) = args.let { (chat, sender, message, users) ->
        fct(chat as Chat, sender as User, message as IncomingMessage, users as List<User>)
    }
    fun invoke(chat: Chat, sender: User, message: IncomingMessage, users: List<User>) =
        fct(chat, sender, message, users)
}

class StartedTyping(val fct: (Chat, User) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any) = args.let { (chat, user) ->
        fct(chat as Chat, user as User)
    }
    fun invoke(chat: Chat, user: User): Boolean = fct(chat, user)
}

class StoppedTyping(val fct: (Chat, User) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any) = args.let { (chat, user) ->
        fct(chat as Chat, user as User)
    }
    fun invoke(chat: Chat, user: User): Boolean = fct(chat, user)
}

class ReceivedSticker(val fct: (Chat, Sticker, User) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any) = args.let { (chat, sticker, user) ->
        fct(chat as Chat, sticker as Sticker, user as User)
    }
    fun invoke(chat: Chat, sticker: Sticker, user: User): Boolean = fct(chat, sticker, user)
}

class ChangedAvailability(val fct: (Chat, User, Availability) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any) = args.let { (chat, user, availability) ->
        fct(chat as Chat, user as User, availability as Availability)
    }
    fun invoke(chat: Chat, user: User, availability: Availability): Boolean = fct(chat, user, availability)
}

class ReadByUser(val fct: (chat: Chat, user: User, message: MessageHistory) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any) = args.let { (chat, user, message) ->
        fct(chat as Chat, user as User, message as MessageHistory)
    }
    fun invoke(chat: Chat, user: User, message: MessageHistory): Boolean = fct(chat, user, message)
}

class ReactionChanged(val fct: (User, Chat, IncomingMessage, Emoji, oldAmt: Int, newAmt: Int) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any) = fct(
        args[0] as User,
        args[1] as Chat,
        args[2] as IncomingMessage,
        args[3] as Emoji,
        args[4] as Int,
        args[5] as Int
    )
    fun invoke(
        sender: User,
        chat: Chat,
        message: IncomingMessage,
        emoji: Emoji,
        oldAmount: Int,
        newAmount: Int
    ): Boolean = fct(sender, chat, message, emoji, oldAmount, newAmount)
}
