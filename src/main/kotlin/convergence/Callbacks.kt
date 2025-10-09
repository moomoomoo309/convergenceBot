@file:Suppress("unused", "UNCHECKED_CAST")
package convergence

import kotlin.reflect.KClass

val callbacks = mutableMapOf<KClass<out ChatEvent>, MutableList<ChatEvent>>(
    ReceivedImages::class to mutableListOf(
        ReceivedImages { chat: Chat, message: IncomingMessage?, sender: User, images: Array<Image> ->
            runCommand(chat, message ?: return@ReceivedImages false, sender, images)
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
 * Cuts down on type casts/checks because [ChatEvent.invoke] takes varargs of [Any].
 */
fun <T1, T2, T3, T4, T5, T6> invokeTyped(
    fct: (T1, T2, T3, T4, T5, T6) -> Boolean,
    args: Array<out Any>,
    default1: T1? = null,
    default2: T2? = null,
    default3: T3? = null,
    default4: T4? = null,
    default5: T5? = null,
    default6: T6? = null,
): Boolean {
    when(args.size) {
        6 -> {
        }

        in 0..5 -> throw IllegalArgumentException("invokeTyped called with ${args.size} parameters, but 6 expected")
        else -> defaultLogger.error("invokeTyped called with ${args.size} parameters, but 6 expected")
    }
    val arg1 = args.getOrNull(0) ?: default1
    val arg2 = args.getOrNull(1) ?: default2
    val arg3 = args.getOrNull(2) ?: default3
    val arg4 = args.getOrNull(3) ?: default4
    val arg5 = args.getOrNull(4) ?: default5
    val arg6 = args.getOrNull(5) ?: default6

    return fct(arg1 as T1, arg2 as T2, arg3 as T3, arg4 as T4, arg5 as T5, arg6 as T6)
}

fun <T1, T2, T3, T4, T5> invokeTyped(
    fct: (T1, T2, T3, T4, T5) -> Boolean,
    args: Array<out Any>,
    default1: T1? = null,
    default2: T2? = null,
    default3: T3? = null,
    default4: T4? = null,
    default5: T5? = null
): Boolean {
    when(args.size) {
        5 -> {
        }

        in 0..4 -> throw IllegalArgumentException("invokeTyped called with ${args.size} parameters, but 5 expected")
        else -> defaultLogger.error("invokeTyped called with ${args.size} parameters, but 5 expected")
    }
    val arg1 = args.getOrNull(0) ?: default1
    val arg2 = args.getOrNull(1) ?: default2
    val arg3 = args.getOrNull(2) ?: default3
    val arg4 = args.getOrNull(3) ?: default4
    val arg5 = args.getOrNull(4) ?: default5

    return fct(arg1 as T1, arg2 as T2, arg3 as T3, arg4 as T4, arg5 as T5)
}

/**
 * Cuts down on type casts/checks because [ChatEvent.invoke] takes varargs of [Any].
 */
fun <T1, T2, T3, T4> invokeTyped(
    fct: (T1, T2, T3, T4) -> Boolean,
    args: Array<out Any>,
    default1: T1? = null,
    default2: T2? = null,
    default3: T3? = null,
    default4: T4? = null
): Boolean {
    when(args.size) {
        4 -> {
        }

        0, 1, 2, 3 -> throw IllegalArgumentException("invokeTyped called with ${args.size} parameters, but 4 expected")
        else -> defaultLogger.error("invokeTyped called with ${args.size} parameters, but 4 expected")
    }
    val arg1 = args.getOrNull(0) ?: default1
    val arg2 = args.getOrNull(1) ?: default2
    val arg3 = args.getOrNull(2) ?: default3
    val arg4 = args.getOrNull(3) ?: default4

    return fct(arg1 as T1, arg2 as T2, arg3 as T3, arg4 as T4)
}

fun <T1, T2, T3> invokeTyped(
    fct: (T1, T2, T3) -> Boolean,
    args: Array<out Any>,
    default1: T1? = null,
    default2: T2? = null,
    default3: T3? = null
): Boolean {
    when(args.size) {
        3 -> {
        }

        0, 1, 2 -> throw IllegalArgumentException("invokeTyped called with ${args.size} parameters, but 3 expected")
        else -> defaultLogger.error("invokeTyped called with ${args.size} parameters, but 3 expected")
    }
    val arg1 = args.getOrNull(0) ?: default1
    val arg2 = args.getOrNull(1) ?: default2
    val arg3 = args.getOrNull(2) ?: default3

    return fct(arg1 as T1, arg2 as T2, arg3 as T3)
}

fun <T1, T2> invokeTyped(
    fct: (T1, T2) -> Boolean,
    args: Array<out Any>,
    default1: T1? = null,
    default2: T2? = null
): Boolean {
    when(args.size) {
        2 -> {
        }

        0, 1 -> throw IllegalArgumentException("invokeTyped called with ${args.size} parameters, but 2 expected")
        else -> defaultLogger.error("invokeTyped called with ${args.size} parameters, but 2 expected")
    }
    val arg1 = args.getOrNull(0) ?: default1
    val arg2 = args.getOrNull(1) ?: default2

    return fct(arg1 as T1, arg2 as T2)
}

fun <T> invokeTyped(fct: (T) -> Boolean, args: Array<out Any>, default: T? = null): Boolean {
    when(args.size) {
        1 -> {
        }

        0 -> throw IllegalArgumentException("invokeTyped called with 0 parameters, but 1 expected")
        else -> defaultLogger.error("invokeTyped called with ${args.size} parameters, but 1 expected")
    }
    val arg1 = args.getOrNull(0) ?: default

    return fct(arg1 as T)
}

/**
 * A callback attached to a specific type of event.
 */
interface ChatEvent {
    operator fun invoke(vararg args: Any): Boolean
}

class ChangedNickname(val fct: (chat: Chat, user: User, oldName: String) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any) = invokeTyped(fct, args)
    fun invoke(chat: Chat, user: User, oldName: String) = fct(chat, user, oldName)
}

class ReceivedImages(val fct: (chat: Chat, message: IncomingMessage?, sender: User, image: Array<Image>) -> Boolean):
    ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, message: IncomingMessage?, sender: User, image: Array<Image>): Boolean =
        fct(chat, message ?: SimpleIncomingMessage(""), sender, image)
}

class EditMessage(val fct: (oldMessage: String, sender: User, newMessage: String) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(oldMessage: String, sender: User, newMessage: String): Boolean = fct(oldMessage, sender, newMessage)
}

class MentionedUser(val fct: (Chat, message: IncomingMessage, sender: User, users: Set<User>) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any) = invokeTyped(fct, args)
    fun invoke(chat: Chat, message: IncomingMessage, sender: User, users: Set<User>) = fct(chat, message, sender, users)
}

class StartedTyping(val fct: (Chat, User) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, user: User): Boolean = fct(chat, user)
}

class StoppedTyping(val fct: (Chat, User) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, user: User): Boolean = fct(chat, user)
}

class ReceivedSticker(val fct: (Chat, Sticker, User) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, sticker: Sticker, user: User): Boolean = fct(chat, sticker, user)
}

class ChangedAvailability(val fct: (Chat, User, Availability) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, user: User, availability: Availability): Boolean = fct(chat, user, availability)
}

class ReadByUser(val fct: (chat: Chat, message: MessageHistory, user: User) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, message: MessageHistory, user: User): Boolean = fct(chat, message, user)
}

class ReactionChanged(val fct: (User, Chat, IncomingMessage, IEmoji, oldAmt: Int, newAmt: Int) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(
        sender: User,
        chat: Chat,
        message: IncomingMessage,
        emoji: IEmoji,
        oldAmount: Int,
        newAmount: Int
    ): Boolean = fct(sender, chat, message, emoji, oldAmount, newAmount)
}
