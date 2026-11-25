package convergence.callbacks

import convergence.model.*

/**
 * A typed event carrying its own handler list. Handlers return [Boolean]
 * for compatibility with the old callback API; [fire] ignores the result.
 */
class ChangedNicknameEvent<C : Chat<*>, U : User<*>> {
    private val handlers = mutableListOf<(C, U, String) -> Boolean>()
    fun register(handler: (C, U, String) -> Boolean) { handlers.add(handler) }
    fun fire(chat: C, user: U, oldName: String) { handlers.forEach { it(chat, user, oldName) } }
    val handlerCount: Int get() = handlers.size
}

class ReceivedImagesEvent<C : Chat<*>, U : User<*>> {
    private val handlers = mutableListOf<(C, U, IncomingMessage<*>, Array<out Image>) -> Boolean>()
    fun register(handler: (C, U, IncomingMessage<*>, Array<out Image>) -> Boolean) { handlers.add(handler) }
    fun fire(chat: C, sender: U, message: IncomingMessage<*>, images: Array<out Image>) {
        handlers.forEach { it(chat, sender, message, images) }
    }
    val handlerCount: Int get() = handlers.size
}

class EditMessageEvent<U : User<*>> {
    private val handlers = mutableListOf<(oldMessage: String, sender: U, newMessage: String) -> Boolean>()
    fun register(handler: (oldMessage: String, sender: U, newMessage: String) -> Boolean) { handlers.add(handler) }
    fun fire(oldMessage: String, sender: U, newMessage: String) {
        handlers.forEach { it(oldMessage, sender, newMessage) }
    }
    val handlerCount: Int get() = handlers.size
}

class MentionedUserEvent<C : Chat<*>, U : User<*>, IM : IncomingMessage<*>> {
    private val handlers = mutableListOf<(C, U, IM, List<User<*>>) -> Boolean>()
    fun register(handler: (C, U, IM, List<User<*>>) -> Boolean) { handlers.add(handler) }
    fun fire(chat: C, sender: U, message: IM, users: List<User<*>>) {
        handlers.forEach { it(chat, sender, message, users) }
    }
    val handlerCount: Int get() = handlers.size
}

class StartedTypingEvent<C : Chat<*>, U : User<*>> {
    private val handlers = mutableListOf<(C, U) -> Boolean>()
    fun register(handler: (C, U) -> Boolean) { handlers.add(handler) }
    fun fire(chat: C, user: U) { handlers.forEach { it(chat, user) } }
    val handlerCount: Int get() = handlers.size
}

class StoppedTypingEvent<C : Chat<*>, U : User<*>> {
    private val handlers = mutableListOf<(C, U) -> Boolean>()
    fun register(handler: (C, U) -> Boolean) { handlers.add(handler) }
    fun fire(chat: C, user: U) { handlers.forEach { it(chat, user) } }
    val handlerCount: Int get() = handlers.size
}

class ReceivedStickerEvent<C : Chat<*>, U : User<*>> {
    private val handlers = mutableListOf<(C, U, Sticker) -> Boolean>()
    fun register(handler: (C, U, Sticker) -> Boolean) { handlers.add(handler) }
    fun fire(chat: C, sender: U, sticker: Sticker) { handlers.forEach { it(chat, sender, sticker) } }
    val handlerCount: Int get() = handlers.size
}

class ChangedAvailabilityEvent<C : Chat<*>, U : User<*>> {
    private val handlers = mutableListOf<(C, U, Availability) -> Boolean>()
    fun register(handler: (C, U, Availability) -> Boolean) { handlers.add(handler) }
    fun fire(chat: C, user: U, availability: Availability) { handlers.forEach { it(chat, user, availability) } }
    val handlerCount: Int get() = handlers.size
}

class ReadByUserEvent<C : Chat<*>, U : User<*>> {
    private val handlers = mutableListOf<(C, U, MessageHistory<*>) -> Boolean>()
    fun register(handler: (C, U, MessageHistory<*>) -> Boolean) { handlers.add(handler) }
    fun fire(chat: C, user: U, message: MessageHistory<*>) { handlers.forEach { it(chat, user, message) } }
    val handlerCount: Int get() = handlers.size
}

class ReactionChangedEvent<C : Chat<*>, U : User<*>, IM : IncomingMessage<*>> {
    private val handlers = mutableListOf<(U, C, IM, Emoji, Int, Int) -> Boolean>()
    fun register(handler: (U, C, IM, Emoji, Int, Int) -> Boolean) { handlers.add(handler) }
    fun fire(sender: U, chat: C, message: IM, emoji: Emoji, oldAmount: Int, newAmount: Int) {
        handlers.forEach { it(sender, chat, message, emoji, oldAmount, newAmount) }
    }
    val handlerCount: Int get() = handlers.size
}
