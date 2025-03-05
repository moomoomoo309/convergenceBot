@file:Suppress("unused", "UNCHECKED_CAST")

package convergence

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.OffsetDateTime
import java.util.*
import kotlin.reflect.KClass

sealed interface CommandScope {
    val protocol: Protocol
    fun toKey(): String
}

abstract class Server(val name: String, override val protocol: Protocol): Comparable<Server>, CommandScope

abstract class Protocol(val name: String): Comparable<Protocol> {
    abstract fun init()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Protocol) return false

        if (name != other.name) return false

        return true
    }

    override fun compareTo(other: Protocol) = this.name.compareTo(other.name)
    override fun toString(): String = this::class.java.simpleName
    override fun hashCode(): Int = name.hashCode()

    fun receivedMessage(chat: Chat, message: String, sender: User) = runCommand(chat, message, sender)
    abstract fun sendMessage(chat: Chat, message: Message): Boolean
    fun sendMessage(chat: Chat, message: String) = sendMessage(chat, SimpleMessage(message))
    abstract fun getBot(chat: Chat): User
    abstract fun getName(chat: Chat, user: User): String
    abstract fun getChats(): List<Chat>
    abstract fun getUsers(): List<User>
    abstract fun getUsers(chat: Chat): List<User>
    abstract fun getChatName(chat: Chat): String

    abstract fun commandScopeFromKey(key: String): CommandScope?
    abstract fun userFromKey(key: String): User?
}

abstract class User(val protocol: Protocol) {
    abstract fun toKey(): String
}

abstract class Chat(override val protocol: Protocol, val name: String): Comparable<Chat>, CommandScope {
    override fun compareTo(other: Chat) =
        "${protocol.name}-${this.name}".compareTo("${other.protocol.name}-${other.name}")

    override fun toString(): String {
        return "${this::class.java.simpleName}($name)"
    }
}

object UniversalUser: User(UniversalProtocol) {
    override fun toKey() = "UniversalUser()"
}

object UniversalProtocol: Protocol("Universal") {
    override fun sendMessage(chat: Chat, message: Message): Boolean = false
    override fun getBot(chat: Chat): User = UniversalUser
    override fun getName(chat: Chat, user: User): String = ""
    override fun getChats(): List<Chat> = listOf(UniversalChat)
    override fun getUsers(): List<User> = listOf(UniversalUser)
    override fun getUsers(chat: Chat): List<User> = listOf(UniversalUser)
    override fun getChatName(chat: Chat): String = ""
    override fun commandScopeFromKey(key: String): Chat? {
        if (key == "UniversalChat")
            return UniversalChat
        return null
    }

    override fun userFromKey(key: String): User? {
        if (key == "UniversalUser")
            return UniversalUser
        return null
    }

    override fun init() {
        // Do nothing
    }
}

// Used to represent the universal chat.
object UniversalChat: Chat(UniversalProtocol, "Universal") {
    override fun toKey() = "UniversalChat"
}

sealed class CommandLike(
    open val protocol: Protocol,
    open val name: String
): Comparable<CommandLike> {
    override fun compareTo(other: CommandLike) = "$protocol.$name".compareTo("${other.protocol}.${other.name}")
}

data class Command(
    override val protocol: Protocol,
    override val name: String,
    @JsonIgnore val function: (List<String>, Chat, User) -> Message?,
    @JsonIgnore val helpText: String,
    @JsonIgnore val syntaxText: String
): CommandLike(protocol, name) {
    constructor(
        protocol: Protocol,
        name: String,
        function: () -> Message?,
        helpText: String,
        syntaxText: String
    ): this(protocol, name, { _: List<String>, _: Chat, _: User -> function() }, helpText, syntaxText)

    constructor(
        protocol: Protocol,
        name: String,
        function: (args: List<String>) -> Message?,
        helpText: String,
        syntaxText: String
    ): this(protocol, name, { args: List<String>, _: Chat, _: User -> function(args) }, helpText, syntaxText)

    constructor(
        protocol: Protocol,
        name: String,
        function: (args: List<String>, chat: Chat) -> Message?,
        helpText: String,
        syntaxText: String
    ): this(protocol, name, { args: List<String>, chat: Chat, _: User -> function(args, chat) }, helpText, syntaxText)


    companion object {
        fun of(
            protocol: Protocol,
            name: String,
            function: (args: List<String>, chat: Chat, sender: User) -> String?,
            helpText: String,
            syntaxText: String
        ) = Command(
            protocol,
            name,
            { args: List<String>, chat: Chat, sender: User -> function(args, chat, sender)?.let { SimpleMessage(it) } },
            helpText,
            syntaxText
        )
        fun of(
            protocol: Protocol,
            name: String,
            function: (args: List<String>, chat: Chat) -> String?,
            helpText: String,
            syntaxText: String
        ) = Command(
            protocol,
            name,
            { args: List<String>, chat: Chat -> function(args, chat)?.let { SimpleMessage(it) } },
            helpText,
            syntaxText
        )
        fun of(
            protocol: Protocol,
            name: String,
            function: (args: List<String>) -> String?,
            helpText: String,
            syntaxText: String
        ) = Command(
            protocol,
            name,
            { args: List<String> -> function(args)?.let { SimpleMessage(it) } },
            helpText,
            syntaxText
        )
        fun of(
            protocol: Protocol,
            name: String,
            function: () -> String?,
            helpText: String,
            syntaxText: String
        ) = Command(
            protocol,
            name,
            { -> function()?.let { SimpleMessage(it) } },
            helpText,
            syntaxText
        )
    }
}

data class Alias(
    val scope: CommandScope,
    override val name: String,
    val command: Command,
    val args: List<String>
): CommandLike(scope.protocol, name) {
    fun toDTO() = AliasDTO(
        scope.toKey(),
        name,
        command.name,
        command.protocol.name,
        args
    )
}

val callbacks = mutableMapOf<KClass<out ChatEvent>, MutableList<ChatEvent>>(
    ReceivedImages::class to mutableListOf(
        ReceivedImages { chat: Chat, message: String?, sender: User, images: Array<Image> ->
            runCommand(chat, message ?: return@ReceivedImages false, sender, images)
            true
        }
    )
)

fun registerCallback(event: ChatEvent) {
    callbacks.putIfAbsent(event::class, ArrayList())
    callbacks[event::class]?.add(event)
        ?: throw IllegalArgumentException("Tried to register callback for unregistered class ${event::class.simpleName}.")
}

fun runCallbacks(eventClass: KClass<out ChatEvent>, vararg args: Any) =
    callbacks[eventClass]?.any { callback ->
        callback(args)
    } ?: false

inline fun <reified T: ChatEvent> runCallbacks(vararg args: Any) = runCallbacks(T::class, *args)

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

open class ChangedNickname(val fct: (chat: Chat, user: User, oldName: String) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any) = invokeTyped(fct, args)
    fun invoke(chat: Chat, user: User, oldName: String) = fct(chat, user, oldName)
}

interface HasNicknames {
    fun getUserNickname(chat: Chat, user: User): String?
    fun getBotNickname(chat: Chat): String?

    fun changedNickname(chat: Chat, user: User, oldName: String) = runCallbacks<ChangedNickname>(chat, user, oldName)
}

open class Image
open class ReceivedImages(val fct: (chat: Chat, message: String?, sender: User, image: Array<Image>) -> Boolean):
    ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args[0] as Array<Any>)
    fun invoke(chat: Chat, message: String?, sender: User, image: Array<Image>): Boolean =
        fct(chat, message ?: "", sender, image)
}

interface HasImages {
    fun sendImages(chat: Chat, message: Message, sender: User, vararg images: Image)
    fun sendImages(chat: Chat, message: String, sender: User, vararg images: Image) =
        sendImages(chat, SimpleMessage(message), sender, *images)
    fun receivedImages(chat: Chat, message: String, sender: User, vararg images: Image) =
        receivedImages(chat, SimpleMessage(message), sender, *images)
    fun receivedImages(chat: Chat, message: Message, sender: User, vararg images: Image) =
        runCallbacks<ReceivedImages>(chat, message, sender, images)
}

open class EditMessage(val fct: (oldMessage: String, sender: User, newMessage: String) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(oldMessage: String, sender: User, newMessage: String): Boolean = fct(oldMessage, sender, newMessage)
}

interface CanEditOtherMessages {
    fun editMessage(message: MessageHistory, oldMessage: String, sender: User, newMessage: String)
    fun editedMessage(oldMessage: String, sender: User, newMessage: String) =
        runCallbacks<EditMessage>(oldMessage, sender, newMessage)
}

abstract class MessageHistory(var message: String, val timestamp: OffsetDateTime, val sender: User)
interface HasMessageHistory {
    fun getMessages(chat: Chat, since: OffsetDateTime? = null, until: OffsetDateTime? = null): List<MessageHistory>
    fun getUserMessages(
        chat: Chat,
        user: User,
        since: OffsetDateTime? = null,
        until: OffsetDateTime? = null
    ): List<MessageHistory>
}

open class MentionedUser(val fct: (chat: Chat, message: String, users: Set<User>) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any) = invokeTyped(fct, args)
    fun invoke(chat: Chat, message: String, users: Set<User>) = fct(chat, message, users)
}

interface CanMentionUsers {
    fun mention(chat: Chat, user: User, message: String?)
    fun mention(chat: Chat, user: User) = mention(chat, user, null)
    fun mentionedUsers(chat: Chat, message: String, users: Set<User>, sender: User) =
        runCallbacks<MentionedUser>(chat, message, users, sender)
}

open class StartedTyping(val fct: (chat: Chat, user: User) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, user: User): Boolean = fct(chat, user)
}

open class StoppedTyping(val fct: (chat: Chat, user: User) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, user: User): Boolean = fct(chat, user)
}

interface HasTypingStatus {
    fun setBotTypingStatus(chat: Chat, status: Boolean)
    fun startedTyping(chat: Chat, user: User) = runCallbacks<StartedTyping>(chat, user)
    fun stoppedTyping(chat: Chat, user: User) = runCallbacks<StoppedTyping>(chat, user)
}

open class Sticker(val name: String, val URL: String?)
open class ReceivedSticker(val fct: (chat: Chat, sticker: Sticker, user: User) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, sticker: Sticker, user: User): Boolean = fct(chat, sticker, user)
}

interface HasStickers {
    fun sendSticker(chat: Chat, sticker: Sticker)
    fun receivedSticker(chat: Chat, sticker: Sticker, sender: User) =
        runCallbacks<ReceivedSticker>(chat, sticker, sender)
}

interface HasUserStatus { // Like your status on Skype.
    fun setBotStatus(chat: Chat, status: String)
    fun getUserStatus(chat: Chat, user: User): String
}

open class Availability(val description: String)
open class ChangedAvailability(val fct: (chat: Chat, user: User, availability: Availability) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, user: User, availability: Availability): Boolean = fct(chat, user, availability)
}

interface HasUserAvailability {
    fun setBotAvailability(chat: Chat, availability: Availability)
    fun getUserAvailability(chat: Chat, user: User): Availability
    fun changedAvailability(chat: Chat, user: User, availability: Availability) =
        runCallbacks<ChangedAvailability>(chat, user, availability)
}

open class ReadByUser(val fct: (chat: Chat, message: MessageHistory, user: User) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, message: MessageHistory, user: User): Boolean = fct(chat, message, user)
}

interface HasReadStatus {
    fun getReadStatus(chat: Chat, message: MessageHistory): Set<User>
    fun setRead(chat: Chat, message: MessageHistory, user: User)
    fun readByUser(chat: Chat, message: MessageHistory, user: User) = runCallbacks<ReadByUser>(chat, message, user)
}

// If possible, the name would be an enum instead, but I want the ability for protocols to add extra formats
// beyond the defaults in the companion object, so it's an open class.
open class Format(name: String) {
    val name: String = name.uppercase(Locale.getDefault())

    companion object {
        val bold = Format("BOLD")
        val italics = Format("ITALICS")
        val underline = Format("UNDERLINE")
        val monospace = Format("MONOSPACE")
        val code = Format("CODE")
        val strikethrough = Format("STRIKETHROUGH")
        val spoiler = Format("SPOILER")
        val greentext = Format("GREENTEXT")
    }
}

interface CanFormatMessages {

    val supportedFormats: Set<Format>

    fun getDelimiters(format: Format): Pair<String, String>?

    companion object {
        val defaultFormats = mutableSetOf(
            Format.bold,
            Format.italics,
            Format.underline,
            Format.monospace,
            Format.code,
            Format.strikethrough,
            Format.spoiler,
            Format.greentext
        )
    }
}

abstract class CustomEmoji(open val name: String, val URL: String?)
interface HasCustomEmoji {
    fun getEmojis(chat: Chat): List<CustomEmoji>
}

interface HasServer {
    val server: Server
}

interface HasServers {
    fun getServers(): List<Server>
}

abstract class Message {
    abstract fun toSimple(): SimpleMessage
}
class SimpleMessage(val text: String): Message() {
    override fun toSimple() = this
}
