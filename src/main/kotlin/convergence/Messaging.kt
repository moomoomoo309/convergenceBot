package convergence


/**
 * Sends [message] in the chat [sender] is in, forwarding the message to any linked chats.
 */
fun sendMessage(chat: Chat, sender: User, message: OutgoingMessage?) {
    if (sender != chat.protocol.getBot(chat))
        sendMessage(chat, message)
    forwardToLinkedChats(chat, message, sender, true)
}

fun sendMessage(chat: Chat, sender: User, message: String?) =
    message?.let { sendMessage(chat, sender, SimpleOutgoingMessage(message)) }

/**
 * Sends [message] in [chat], not forwarding it to linked chats.
 */
fun sendMessage(chat: Chat, message: OutgoingMessage?) {
    if (message == null)
        return
    chat.protocol.sendMessage(chat, message)
}

fun sendMessage(chat: Chat, message: String?) = message?.let { sendMessage(chat, SimpleOutgoingMessage(message)) }

/**
 * Gets the nickname (if applicable) or name of a user.
 */
fun getUserName(chat: Chat, sender: User): String {
    val protocol = chat.protocol
    return if (protocol is HasNicknames)
        protocol.getUserNickname(chat, sender) ?: protocol.getName(chat, sender)
    else
        protocol.getName(chat, sender)
}

/**
 * Replaces instances of the keys in [aliasVars] preceded by a percent sign with the result of the functions therein,
 * such as %sender with the name of the user who sent the message.
 */
fun replaceAliasVars(chat: Chat, message: OutgoingMessage?, sender: User): OutgoingMessage? {
    if (message is SimpleOutgoingMessage) {
        // Used as a mutable string, since this function does a lot of string appending.
        val text = message.text
        val stringBuilder = StringBuilder((text.length * 1.5).toInt())

        var charIndex = -1
        val possibleMatches = BooleanArray(aliasVars.size)

        var anyTrue = false
        for (currentChar in text) {
            stringBuilder.append(currentChar)
            if (currentChar == '%') {
                charIndex = 0
                continue
            }
            if (charIndex >= 0) {
                var i = -1
                for ((string, aliasVar) in aliasVars) {
                    i++
                    if (possibleMatches[i] && currentChar != string[charIndex])
                        possibleMatches[i] = false
                    if (possibleMatches[i]) {
                        anyTrue = true
                        if (charIndex == string.length - 1) {
                            stringBuilder.setLength(stringBuilder.length - string.length - 1)
                            stringBuilder.append(aliasVar(chat, sender))
                            charIndex = -1
                            break
                        }
                    }
                }
                if (!anyTrue)
                    break
            }
            charIndex = if (anyTrue) charIndex + 1 else -1
        }
        return SimpleOutgoingMessage(stringBuilder.toString())
    } else
        return message
}

fun forwardToLinkedChats(chat: Chat, message: OutgoingMessage?, sender: User, isCommand: Boolean = false) =
    forwardToLinkedChats(chat, message, sender, emptyArray(), isCommand)

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
    // Try to get the delimiters for bold, if possible.
    if (protocol is CanFormatMessages && Format.bold in protocol.supportedFormats) {
        val delimiters = protocol.getDelimiters(Format.bold)
        boldOpen = delimiters?.first ?: boldOpen
        boldClose = delimiters?.second ?: boldClose
    }

    // Send the messages out to the linked chats, if there are any. Don't error if there aren't any.
    val bot = chat.protocol.getBot(chat)
    if (isCommand || sender != bot)
        if (chat in linkedChats)
            for (linkedChat in linkedChats[chat]!!) {
                val msg = "$boldOpen${getUserName(chat, if (isCommand) bot else sender)}:$boldClose $message"
                if (linkedChat.protocol is HasImages && images.isNotEmpty())
                    (linkedChat.protocol as HasImages).sendImages(linkedChat, msg, sender, *images)
                else
                    sendMessage(linkedChat, msg)
            }
}
