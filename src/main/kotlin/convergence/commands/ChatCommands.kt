package convergence.commands

import convergence.bot
import convergence.defaultLogger
import convergence.model.Chat
import convergence.settings
import convergence.updateSettings

fun chats(): String {
    val builder = StringBuilder()
    for (protocol in bot.protocols) {
        try {
            val chats = protocol.getChats()
            builder.append("${protocol.name}\n\t")
            chats.forEach {
                val id = bot.reverseChatMap[it]
                if (id != null)
                    builder.append(it).append(" (").append(id).append("), ")
            }
            if (chats.any { it in bot.reverseChatMap }) {
                builder.setLength(builder.length - 2) // Remove the last ", ".
            }
            builder.append('\n')
        } catch(e: Exception) {
            defaultLogger.error("Error getting chats for ${protocol.name}.", e)
        }
    }
    return builder.toString()
}

fun link(args: List<String>, chat: Chat): String {
    val index = args[0].toIntOrNull() ?: return "${args[0]} is not a chat ID!"

    val chatToLink = bot.chatMap[index]
    return if (chatToLink != null) {
        settings.linkedChats.getOrPut(chat) { mutableSetOf() }.add(chatToLink)
        updateSettings()
        "${chatToLink.name} linked to ${chat.name}."
    } else
        "No chat with ID $index found."
}

fun unlink(args: List<String>, chat: Chat): String {
    val index = args[0].toIntOrNull() ?: return "${args[0]} is not a chat ID!"
    val toUnlink = bot.chatMap[index] ?: return "No chat with ID $index found."

    return when {
        chat !in settings.linkedChats -> "There are no chats linked to this one!"
        settings.linkedChats[chat]!!.remove(toUnlink) -> {
            if (settings.linkedChats[chat]!!.isEmpty())
                settings.linkedChats.remove(chat)
            updateSettings()
            "Removed ${toUnlink.name} from this chat's links."
        }

        else -> "That chat isn't linked to this one!"
    }
}

fun links(chat: Chat): String {
    return if (chat in settings.linkedChats)
        "Linked chats: ${settings.linkedChats[chat]!!.joinToString(", ") { c: Chat -> "$c (${bot.reverseChatMap[c]})" }}"
    else
        "No chats are linked to this one."
}
