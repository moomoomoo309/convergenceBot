@file:Suppress("unused", "UNCHECKED_CAST")

package convergence

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sigpwned.emoji4j.core.grapheme.Emoji
import java.io.InputStream
import java.net.URI
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
    abstract fun configLoaded()
    abstract fun aliasCreated(alias: Alias)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Protocol) return false

        if (name != other.name) return false

        return true
    }

    override fun compareTo(other: Protocol) = this.name.compareTo(other.name)
    override fun toString(): String = this::class.java.simpleName
    override fun hashCode(): Int = name.hashCode()

    fun receivedMessage(chat: Chat, message: IncomingMessage, sender: User) = runCommand(chat, message, sender)
    abstract fun sendMessage(chat: Chat, message: OutgoingMessage): Boolean
    fun sendMessage(chat: Chat, message: String) = sendMessage(chat, SimpleOutgoingMessage(message))
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
    override fun sendMessage(chat: Chat, message: OutgoingMessage): Boolean = false
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

    override fun configLoaded() {
        // Do nothing
    }

    override fun aliasCreated(alias: Alias) {
        // Do nothing
    }
}

// Used to represent the universal chat.
object UniversalChat: Chat(UniversalProtocol, "Universal") {
    override fun toKey() = "UniversalChat"
}

enum class ArgumentType {
    NUMBER,
    STRING,
    BOOLEAN,
    INTEGER
}

data class ArgumentSpec(val name: String, val type: ArgumentType, val optional: Boolean = false)

sealed class CommandLike(
    open val protocol: Protocol,
    open val name: String,
): Comparable<CommandLike> {
    override fun compareTo(other: CommandLike) = "$protocol-$name".compareTo("${other.protocol}-${other.name}")
}

data class Command(
    override val protocol: Protocol,
    override val name: String,
    @JsonIgnore val argSpecs: List<ArgumentSpec>,
    @JsonIgnore val function: (List<String>, Chat, User) -> OutgoingMessage?,
    @JsonIgnore val helpText: String,
    @JsonIgnore val syntaxText: String
): CommandLike(protocol, name) {
    constructor(
        protocol: Protocol,
        name: String,
        argSpecs: List<ArgumentSpec>,
        function: () -> OutgoingMessage?,
        helpText: String,
        syntaxText: String
    ): this(protocol, name, argSpecs, { _, _, _ -> function() }, helpText, syntaxText)

    constructor(
        protocol: Protocol,
        name: String,
        argSpecs: List<ArgumentSpec>,
        function: (args: List<String>) -> OutgoingMessage?,
        helpText: String,
        syntaxText: String
    ): this(protocol, name, argSpecs, { args: List<String>, _, _ -> function(args) }, helpText, syntaxText)

    constructor(
        protocol: Protocol,
        name: String,
        argSpecs: List<ArgumentSpec>,
        function: (args: List<String>, chat: Chat) -> OutgoingMessage?,
        helpText: String,
        syntaxText: String
    ): this(protocol, name, argSpecs, { args: List<String>, chat: Chat, _ -> function(args, chat) }, helpText, syntaxText)


    companion object {
        fun of(
            protocol: Protocol,
            name: String,
            argSpecs: List<ArgumentSpec>,
            function: (args: List<String>, chat: Chat, sender: User) -> String?,
            helpText: String,
            syntaxText: String
        ) = Command(
            protocol,
            name,
            argSpecs,
            { args: List<String>, chat: Chat, sender: User -> function(args, chat, sender)?.let { SimpleOutgoingMessage(it) } },
            helpText,
            syntaxText
        )
        fun of(
            protocol: Protocol,
            name: String,
            argSpecs: List<ArgumentSpec>,
            function: (args: List<String>, chat: Chat) -> String?,
            helpText: String,
            syntaxText: String
        ) = Command(
            protocol,
            name,
            argSpecs,
            { args: List<String>, chat: Chat -> function(args, chat)?.let { SimpleOutgoingMessage(it) } },
            helpText,
            syntaxText
        )
        fun of(
            protocol: Protocol,
            name: String,
            argSpecs: List<ArgumentSpec>,
            function: (args: List<String>) -> String?,
            helpText: String,
            syntaxText: String
        ) = Command(
            protocol,
            name,
            argSpecs,
            { args: List<String> -> function(args)?.let { SimpleOutgoingMessage(it) } },
            helpText,
            syntaxText
        )
        fun of(
            protocol: Protocol,
            name: String,
            argSpecs: List<ArgumentSpec>,
            function: () -> String?,
            helpText: String,
            syntaxText: String
        ) = Command(
            protocol,
            name,
            argSpecs,
            { -> function()?.let { SimpleOutgoingMessage(it) } },
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
        scope.protocol.name,
        args
    )
}

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
        ?: throw IllegalArgumentException("Tried to register callback for unregistered class ${event::class.simpleName}.")
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

        in 0..5 -> throw IllegalArgumentException("invokeTyped called with ${args.size} parameters, but 4 expected")
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

        in 0..4 -> throw IllegalArgumentException("invokeTyped called with ${args.size} parameters, but 4 expected")
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

interface HasNicknames {
    fun getUserNickname(chat: Chat, user: User): String?
    fun getBotNickname(chat: Chat): String?

    fun changedNickname(chat: Chat, user: User, oldName: String) = runCallbacks<ChangedNickname>(chat, user, oldName)
}

abstract class Image {
    abstract fun getURL(): URI
    abstract fun getStream(): InputStream
    fun getBytes(): ByteArray = getStream().readAllBytes()
}
class ReceivedImages(val fct: (chat: Chat, message: IncomingMessage?, sender: User, image: Array<Image>) -> Boolean):
    ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, message: IncomingMessage?, sender: User, image: Array<Image>): Boolean =
        fct(chat, message ?: SimpleIncomingMessage(""), sender, image)
}

interface HasImages {
    fun sendImages(chat: Chat, message: OutgoingMessage, sender: User, vararg images: Image)
    fun sendImages(chat: Chat, message: String, sender: User, vararg images: Image) =
        sendImages(chat, SimpleOutgoingMessage(message), sender, *images)
    fun receivedImages(chat: Chat, message: String, sender: User, vararg images: Image) =
        receivedImages(chat, SimpleIncomingMessage(message), sender, *images)
    fun receivedImages(chat: Chat, message: IncomingMessage, sender: User, vararg images: Image) =
        runCallbacks<ReceivedImages>(chat, message, sender, images)
}

class EditMessage(val fct: (oldMessage: String, sender: User, newMessage: String) -> Boolean): ChatEvent {
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

class MentionedUser(val fct: (chat: Chat, message: String, users: Set<User>) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any) = invokeTyped(fct, args)
    fun invoke(chat: Chat, message: String, users: Set<User>) = fct(chat, message, users)
}

interface CanMentionUsers {
    fun mention(chat: Chat, user: User, message: String?)
    fun mention(chat: Chat, user: User) = mention(chat, user, null)
    fun mentionedUsers(chat: Chat, message: String, users: Set<User>, sender: User) =
        runCallbacks<MentionedUser>(chat, message, users, sender)
}

class StartedTyping(val fct: (chat: Chat, user: User) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, user: User): Boolean = fct(chat, user)
}

class StoppedTyping(val fct: (chat: Chat, user: User) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, user: User): Boolean = fct(chat, user)
}

interface HasTypingStatus {
    fun setBotTypingStatus(chat: Chat, status: Boolean)
    fun startedTyping(chat: Chat, user: User) = runCallbacks<StartedTyping>(chat, user)
    fun stoppedTyping(chat: Chat, user: User) = runCallbacks<StoppedTyping>(chat, user)
}

abstract class Sticker(val name: String, val url: String?)
class ReceivedSticker(val fct: (chat: Chat, sticker: Sticker, user: User) -> Boolean): ChatEvent {
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

abstract class Availability(val description: String)
class ChangedAvailability(val fct: (chat: Chat, user: User, availability: Availability) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, user: User, availability: Availability): Boolean = fct(chat, user, availability)
}

interface HasUserAvailability {
    fun setBotAvailability(chat: Chat, availability: Availability)
    fun getUserAvailability(chat: Chat, user: User): Availability
    fun changedAvailability(chat: Chat, user: User, availability: Availability) =
        runCallbacks<ChangedAvailability>(chat, user, availability)
}

class ReadByUser(val fct: (chat: Chat, message: MessageHistory, user: User) -> Boolean): ChatEvent {
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
    val name: String = name.lowercase(Locale.getDefault())

    companion object {
        val bold = Format("bold")
        val italics = Format("italics")
        val underline = Format("underline")
        val monospace = Format("monospace")
        val code = Format("code")
        val strikethrough = Format("strikethrough")
        val spoiler = Format("spoiler")
        val greentext = Format("greentext")
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

interface IEmoji {
    fun asString(): String
}
abstract class CustomEmoji(open val name: String, open val url: String?): IEmoji
class UnicodeEmoji(val emoji: Emoji): IEmoji {
    constructor(s: String): this(s.toEmoji()!!)
    override fun asString(): String = emoji.toString()
}
interface HasCustomEmoji {
    fun getEmojis(chat: Chat): List<CustomEmoji>
}

interface HasReactions {
    fun react(message: IncomingMessage, emoji: IEmoji)
    fun unreact(message: IncomingMessage, emoji: IEmoji)
    fun getReactions(message: IncomingMessage): Map<IEmoji, Int>
    fun reactionChanged(sender: User, chat: Chat, message: IncomingMessage, emoji: IEmoji, oldAmount: Int, newAmount: Int) =
        runCallbacks<ReactionChanged>(sender, chat, message, emoji, oldAmount, newAmount)
}

class ReactionChanged(val fct: (sender: User, chat: Chat, message: IncomingMessage, emoji: IEmoji, oldAmount: Int, newAmount: Int) -> Boolean): ChatEvent {
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(sender: User, chat: Chat, message: IncomingMessage, emoji: IEmoji, oldAmount: Int, newAmount: Int): Boolean = fct(sender, chat, message, emoji, oldAmount, newAmount)
}

interface HasServer<T: Server> {
    val server: T
}

interface HasServers<T: Server> {
    fun getServers(): List<T>
}

abstract class IncomingMessage {
    abstract fun toSimple(): SimpleIncomingMessage
    abstract fun toOutgoing(): OutgoingMessage
}

class SimpleIncomingMessage(val text: String): IncomingMessage() {
    override fun toSimple() = this
    override fun toOutgoing() = SimpleOutgoingMessage(text)
}

abstract class OutgoingMessage {
    abstract fun toSimple(): SimpleOutgoingMessage
}

class SimpleOutgoingMessage(val text: String): OutgoingMessage() {
    override fun toSimple() = this
}
