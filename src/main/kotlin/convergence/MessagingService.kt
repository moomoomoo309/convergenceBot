package convergence

/**
 * Service for sending messages and resolving user names.
 */
class MessagingService(
    private val botState: BotState,
    private val settings: Settings
) {
    /**
     * Sends [message] in the chat [sender] is in, forwarding the message to any linked chats.
     */
    fun sendMessage(chat: Chat, sender: User, message: OutgoingMessage?) {
        if (sender != chat.protocol.getBot(chat))
            sendMessage(chat, message)
        forwardToLinkedChats(chat, message, sender, isCommand = true)
    }

    fun sendMessage(chat: Chat, sender: User, message: String?) =
        message?.let { sendMessage(chat, sender, SimpleOutgoingMessage(message)) }

    /**
     * Sends [message] in [chat], not forwarding it to linked chats.
     */
    fun sendMessage(chat: Chat, message: OutgoingMessage?) {
        if (message == null)
            return
        if (settings.debugMode && message is SimpleOutgoingMessage)
            chat.protocol.sendMessage(chat, "[Test Mode]: ${message.text}")
        else
            chat.protocol.sendMessage(chat, message)
    }

    fun sendMessage(chat: Chat, message: String?) = message?.let { sendMessage(chat, SimpleOutgoingMessage(it)) }

    /**
     * Gets the nickname (if applicable) or name of a user.
     */
    fun getUserName(chat: Chat, sender: User): String {
        val protocol = chat.protocol
        return if (protocol is HasNicknames)
            protocol.getUserNickname(chat, sender) ?: protocol.getUserName(chat, sender)
        else
            protocol.getUserName(chat, sender)
    }

    /**
     * Replaces instances of the keys in [BotState.aliasVars] preceded by a percent sign with the result of the
     * functions therein.
     */
    fun replaceAliasVars(chat: Chat, msg: OutgoingMessage?, sender: User): OutgoingMessage? {
        val pattern = Regex(botState.aliasVars.keys.sortedBy { -it.length }.joinToString("|"))
        return if (msg is SimpleOutgoingMessage)
            SimpleOutgoingMessage(pattern.replace(msg.text) { res ->
                botState.aliasVars[res.value]!!(chat, sender) ?: res.value
            })
        else
            msg
    }

    /**
     * Forwards a message to all chats linked to [chat].
     */
    fun forwardToLinkedChats(
        chat: Chat,
        message: OutgoingMessage?,
        sender: User,
        images: Array<Image> = emptyArray(),
        isCommand: Boolean = false
    ) {
        if (message == null)
            return
        val protocol = chat.protocol
        var boldOpen = ""
        var boldClose = ""
        if (protocol is CanFormatMessages && Format.bold in protocol.supportedFormats) {
            val delimiters = protocol.getDelimiters(Format.bold)
            boldOpen = delimiters?.first ?: boldOpen
            boldClose = delimiters?.second ?: boldClose
        }

        val bot = chat.protocol.getBot(chat)
        if (isCommand || sender != bot)
            if (chat in settings.linkedChats)
                for (linkedChat in settings.linkedChats[chat]!!) {
                    val msg = "$boldOpen${getUserName(chat, if (isCommand) bot else sender)}:$boldClose $message"
                    if (linkedChat.protocol is HasImages && images.isNotEmpty())
                        (linkedChat.protocol as HasImages).sendImages(linkedChat, msg, sender, *images)
                    else
                        sendMessage(linkedChat, msg)
                }
    }
}
