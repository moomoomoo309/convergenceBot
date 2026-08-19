package convergence.protocol

import convergence.callbacks.*
import convergence.model.*
import java.time.OffsetDateTime

interface HasNicknames {
    fun getUserNickname(chat: Chat, user: User): String?
    fun getBotNickname(chat: Chat): String?
    fun setUserNickname(chat: Chat, user: User, newName: String): String?
    fun setBotNickname(chat: Chat, newName: String): String?

    fun changedNickname(chat: Chat, user: User, oldName: String) = runCallbacks<ChangedNickname>(chat, user, oldName)
}

interface HasImages {
    fun sendImages(chat: Chat, sender: User, message: OutgoingMessage, vararg images: Image)
    fun sendImages(chat: Chat, sender: User, message: String, vararg images: Image) =
        sendImages(chat, sender, SimpleOutgoingMessage(message), *images)

    fun receivedImages(chat: Chat, sender: User, message: String, vararg images: Image) =
        receivedImages(chat, sender, SimpleIncomingMessage(message), *images)

    fun receivedImages(chat: Chat, sender: User, message: IncomingMessage, vararg images: Image) =
        runCallbacks<ReceivedImages>(chat, sender, message, images)
}

interface CanEditOtherMessages {
    fun editMessage(message: MessageHistory, oldMessage: IncomingMessage, sender: User, newMessage: OutgoingMessage)
    fun editedMessage(oldMessage: IncomingMessage, sender: User, newMessage: IncomingMessage) =
        runCallbacks<EditMessage>(oldMessage, sender, newMessage)
}

interface HasMessageHistory {
    fun getMessages(chat: Chat, since: OffsetDateTime? = null, until: OffsetDateTime? = null): List<MessageHistory>
    fun getUserMessages(
        chat: Chat,
        user: User,
        since: OffsetDateTime? = null,
        until: OffsetDateTime? = null
    ): List<MessageHistory>
}

interface CanMentionUsers {
    fun mention(chat: Chat, users: List<User>, message: OutgoingMessage?)
    fun mention(chat: Chat, users: List<User>) = mention(chat, users, null)
    fun mentionedUsers(chat: Chat, sender: User, message: IncomingMessage, users: List<User>) =
        runCallbacks<MentionedUser>(chat, sender, message, users)
    fun getUserFromMentionText(chat: Chat, mention: String): User?
    fun getMentions(message: IncomingMessage): List<User>
}

interface HasTypingStatus {
    fun setBotTypingStatus(chat: Chat, status: Boolean)
    fun startedTyping(chat: Chat, user: User) = runCallbacks<StartedTyping>(chat, user)
    fun stoppedTyping(chat: Chat, user: User) = runCallbacks<StoppedTyping>(chat, user)
}

interface HasStickers {
    fun sendSticker(chat: Chat, sticker: Sticker)
    fun receivedSticker(chat: Chat, sender: User, sticker: Sticker) =
        runCallbacks<ReceivedSticker>(chat, sender, sticker)
}

interface HasUserStatus {
    fun setBotStatus(chat: Chat, status: String)
    fun getUserStatus(chat: Chat, user: User): String
}

interface HasUserAvailability {
    fun setBotAvailability(chat: Chat, availability: Availability)
    fun getUserAvailability(chat: Chat, user: User): Availability
    fun changedAvailability(chat: Chat, user: User, availability: Availability) =
        runCallbacks<ChangedAvailability>(chat, user, availability)
}

interface HasReadStatus {
    fun getReadStatus(chat: Chat, message: MessageHistory): Set<User>
    fun setRead(chat: Chat, user: User, message: MessageHistory)
    fun readByUser(chat: Chat, user: User, message: MessageHistory) = runCallbacks<ReadByUser>(chat, user, message)
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

interface HasCustomEmoji {
    fun getEmojis(chat: Chat): List<CustomEmoji>
}

interface HasReactions {
    fun react(message: IncomingMessage, emoji: Emoji)
    fun unreact(message: IncomingMessage, emoji: Emoji)
    fun getReactions(message: IncomingMessage): Map<Emoji, Int>
    fun reactionChanged(
        sender: User,
        chat: Chat,
        message: IncomingMessage,
        emoji: Emoji,
        oldAmount: Int,
        newAmount: Int
    ) = runCallbacks<ReactionChanged>(sender, chat, message, emoji, oldAmount, newAmount)
}

interface HasServer<T: Server> {
    val server: T
}

interface HasServers<T: Server> {
    fun getServers(): List<T>
}

interface HasRoles<RoleType: Role> {
    fun getRoles(server: Server): List<RoleType>
    fun getUserRoles(server: Server, user: User): List<RoleType>
    fun userHasRole(server: Server, user: User, role: RoleType): Boolean =
        getUserRoles(server, user).any { it == role }
}
