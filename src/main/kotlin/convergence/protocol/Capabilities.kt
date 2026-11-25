@file:Suppress("unused")

package convergence.protocol

import convergence.Protocol
import convergence.callbacks.*
import convergence.model.*
import java.time.OffsetDateTime

interface HasNicknames<C : Chat<*>, U : User<*>> {
    fun getUserNickname(chat: C, user: U): String?
    fun getBotNickname(chat: C): String?
    fun setUserNickname(chat: C, user: U, newName: String): String?
    fun setBotNickname(chat: C, newName: String): String?

    val changedNicknameEvent: ChangedNicknameEvent<C, U>
    fun changedNickname(chat: C, user: U, oldName: String) = changedNicknameEvent.fire(chat, user, oldName)

    fun getUserNicknameFor(chat: Chat<*>, user: User<*>): String? {
        val proto = this as? Protocol<*, *, *, *> ?: return null
        if (chat.protocol !== proto || user.protocol !== proto) return null
        @Suppress("UNCHECKED_CAST")
        return getUserNickname(chat as C, user as U)
    }
}

interface HasImages<C : Chat<*>, U : User<*>> {
    fun sendImages(chat: C, sender: U, message: OutgoingMessage, vararg images: Image): Boolean
    fun sendImages(chat: C, sender: U, message: String, vararg images: Image): Boolean =
        sendImages(chat, sender, SimpleOutgoingMessage(message), *images)

    val receivedImagesEvent: ReceivedImagesEvent<C, U>
    fun receivedImages(chat: C, sender: U, message: IncomingMessage<*>, vararg images: Image) =
        receivedImagesEvent.fire(chat, sender, message, images)

    fun sendImagesTo(chat: Chat<*>, sender: User<*>, message: OutgoingMessage, vararg images: Image): Boolean {
        val proto = this as? Protocol<*, *, *, *> ?: return false
        if (chat.protocol !== proto || sender.protocol !== proto) return false
        @Suppress("UNCHECKED_CAST")
        return sendImages(chat as C, sender as U, message, *images)
    }
}

interface CanEditOtherMessages<C : Chat<*>, U : User<*>, IM : IncomingMessage<*>, OM : OutgoingMessage> {
    fun editMessage(message: MessageHistory<IM>, oldMessage: IM, sender: U, newMessage: OM)

    val editedMessageEvent: EditMessageEvent<U>
    fun editedMessage(oldMessage: IM, sender: U, newMessage: IM) =
        editedMessageEvent.fire(oldMessage.toSimple().text, sender, newMessage.toSimple().text)
}

interface HasMessageHistory<C : Chat<*>, U : User<*>, IM : IncomingMessage<*>> {
    fun getMessages(chat: C, since: OffsetDateTime? = null, until: OffsetDateTime? = null): List<MessageHistory<IM>>
    fun getUserMessages(
        chat: C,
        user: U,
        since: OffsetDateTime? = null,
        until: OffsetDateTime? = null
    ): List<MessageHistory<IM>>
}

interface CanMentionUsers<C : Chat<*>, U : User<*>, IM : IncomingMessage<*>> {
    fun mention(chat: C, users: List<U>, message: OutgoingMessage?)
    fun mention(chat: C, users: List<U>) = mention(chat, users, null)

    val mentionedUsersEvent: MentionedUserEvent<C, U, IM>
    fun mentionedUsers(chat: C, sender: U, message: IM, users: List<U>) =
        mentionedUsersEvent.fire(chat, sender, message, users)

    fun getUserFromMentionText(chat: C, mention: String): U?
    fun getMentions(message: IM): List<U>

    fun mentionTo(chat: Chat<*>, users: List<User<*>>, message: OutgoingMessage?) {
        val proto = this as? Protocol<*, *, *, *> ?: return
        if (chat.protocol !== proto || users.any { it.protocol !== proto }) return
        @Suppress("UNCHECKED_CAST")
        mention(chat as C, users as List<U>, message)
    }

    fun getUserFromMentionTextFor(chat: Chat<*>, mention: String): User<*>? {
        val proto = this as? Protocol<*, *, *, *> ?: return null
        if (chat.protocol !== proto) return null
        @Suppress("UNCHECKED_CAST")
        return getUserFromMentionText(chat as C, mention)
    }
}

interface HasTypingStatus<C : Chat<*>, U : User<*>> {
    fun setBotTypingStatus(chat: C, status: Boolean)

    val startedTypingEvent: StartedTypingEvent<C, U>
    val stoppedTypingEvent: StoppedTypingEvent<C, U>
    fun startedTyping(chat: C, user: U) = startedTypingEvent.fire(chat, user)
    fun stoppedTyping(chat: C, user: U) = stoppedTypingEvent.fire(chat, user)
}

interface HasStickers<C : Chat<*>, U : User<*>> {
    fun sendSticker(chat: C, sticker: Sticker)

    val receivedStickerEvent: ReceivedStickerEvent<C, U>
    fun receivedSticker(chat: C, sender: U, sticker: Sticker) = receivedStickerEvent.fire(chat, sender, sticker)
}

interface HasUserStatus<C : Chat<*>, U : User<*>> {
    fun setBotStatus(chat: C, status: String)
    fun getUserStatus(chat: C, user: U): String
}

interface HasUserAvailability<C : Chat<*>, U : User<*>> {
    fun setBotAvailability(chat: C, availability: Availability)
    fun getUserAvailability(chat: C, user: U): Availability

    val changedAvailabilityEvent: ChangedAvailabilityEvent<C, U>
    fun changedAvailability(chat: C, user: U, availability: Availability) =
        changedAvailabilityEvent.fire(chat, user, availability)
}

interface HasReadStatus<C : Chat<*>, U : User<*>, IM : IncomingMessage<*>> {
    fun getReadStatus(chat: C, message: MessageHistory<IM>): Set<U>
    fun setRead(chat: C, user: U, message: MessageHistory<IM>)

    val readByUserEvent: ReadByUserEvent<C, U>
    fun readByUser(chat: C, user: U, message: MessageHistory<IM>) = readByUserEvent.fire(chat, user, message)
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

interface HasCustomEmoji<C : Chat<*>> {
    fun getEmojis(chat: C): List<CustomEmoji>
}

interface HasReactions<C : Chat<*>, U : User<*>, IM : IncomingMessage<*>> {
    fun react(message: IM, emoji: Emoji)
    fun unreact(message: IM, emoji: Emoji)
    fun getReactions(message: IM): Map<Emoji, Int>

    val reactionChangedEvent: ReactionChangedEvent<C, U, IM>
    fun reactionChanged(
        sender: U,
        chat: C,
        message: IM,
        emoji: Emoji,
        oldAmount: Int,
        newAmount: Int
    ) = reactionChangedEvent.fire(sender, chat, message, emoji, oldAmount, newAmount)
}

interface HasServer<S : Server<*>> {
    val server: S
}

interface HasServers<S : Server<*>> {
    fun getServers(): List<S>
}

interface HasRoles<RoleType: Role> {
    fun getRoles(server: Server<*>): List<RoleType>
    fun getUserRoles(server: Server<*>, user: User<*>): List<RoleType>
    fun userHasRole(server: Server<*>, user: User<*>, role: RoleType): Boolean =
        getUserRoles(server, user).any { it == role }
}
