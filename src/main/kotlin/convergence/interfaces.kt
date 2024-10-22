@file:Suppress("unused", "UNCHECKED_CAST")

package convergence

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.OffsetDateTime
import java.util.*
import kotlin.reflect.KClass

abstract class Protocol(val name: String, val baseInterface: BaseInterface): Comparable<Protocol> {
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
}

//Intentionally empty, because it might be represented as an int or a string or whatever.
abstract class User(val protocol: Protocol)

abstract class Chat(val protocol: Protocol, val name: String): Comparable<Chat> {
    override fun compareTo(other: Chat) =
        "${protocol.name}-${this.name}".compareTo("${other.protocol.name}-${other.name}")

    override fun toString(): String {
        return "${this::class.java.simpleName}($name)"
    }
}

object UniversalUser: User(UniversalProtocol)

object UniversalProtocol: Protocol("Universal", DefaultBaseInterface) {
    override fun init() {
        // Do nothing
    }
}

// Used to represent the universal chat.
object UniversalChat: Chat(UniversalProtocol, "Universal")

object DefaultBaseInterface: BaseInterface {
    override fun sendMessage(chat: Chat, message: String): Boolean = false
    override fun getBot(chat: Chat): User = UniversalUser
    override fun getName(chat: Chat, user: User): String = ""
    override fun getChats(): List<Chat> = listOf(UniversalChat)
    override fun getUsers(): List<User> = listOf(UniversalUser)
    override fun getUsers(chat: Chat): List<User> = listOf(UniversalUser)
    override fun getChatName(chat: Chat): String = ""

    override val name: String = "FakeBaseInterface"
}

sealed class CommandLike(
    open val chat: Chat,
    open val name: String,
    @Transient open val helpText: String,
    @Transient open val syntaxText: String
): Comparable<CommandLike> {
    override fun compareTo(other: CommandLike) = "$chat.$name".compareTo("${other.chat}.${other.name}")

    data class Command(
        override val chat: Chat,
        override val name: String,
        @Transient val function: (List<String>, Chat, User) -> String?,
        @Transient override val helpText: String,
        @Transient override val syntaxText: String
    ): CommandLike(chat, name, helpText, syntaxText)

    data class Alias(
        override val chat: Chat,
        override val name: String,
        val command: Command,
        val args: List<String>,
        @Transient override val helpText: String,
        @Transient override val syntaxText: String
    ): CommandLike(chat, name, helpText, syntaxText)
}
typealias Command = CommandLike.Command
typealias Alias = CommandLike.Alias


fun Map<String, Any>.json(): String = objectMapper.writeValueAsString(this)

interface BaseInterface {
    val name: String
    fun receivedMessage(chat: Chat, message: String, sender: User) = runCommand(chat, message, sender)
    fun sendMessage(chat: Chat, message: String): Boolean
    fun getBot(chat: Chat): User
    fun getName(chat: Chat, user: User): String

    @JsonIgnore
    fun getChats(): List<Chat>
    fun getUsers(): List<User>
    fun getUsers(chat: Chat): List<User>
    fun getChatName(chat: Chat): String
}

private val callbacks = mutableMapOf<KClass<out ChatEvent>, MutableList<ChatEvent>>(
    ReceivedImages::class to mutableListOf(ReceivedImages { chat: Chat, message: String?, sender: User, images: Array<Image> ->
        runCommand(chat, message ?: return@ReceivedImages false, sender, images)
        true
    })
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
    when (args.size) {
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
    when (args.size) {
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
    when (args.size) {
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
    when (args.size) {
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
    /**
     * Callback functionality
     */
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
    override fun invoke(vararg args: Any): Boolean = invokeTyped(fct, args)
    fun invoke(chat: Chat, message: String?, sender: User, image: Array<Image>): Boolean =
        fct(chat, message ?: "", sender, image)
}

interface HasImages {
    fun sendImages(chat: Chat, message: String, sender: User, vararg images: Image)
    fun receivedImages(chat: Chat, message: String, sender: User, vararg images: Image) =
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
