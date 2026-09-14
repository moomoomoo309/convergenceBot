package convergence.commands

import convergence.UniversalProtocol
import convergence.bot
import convergence.getUserName
import convergence.model.Chat
import convergence.model.Format
import convergence.model.User
import convergence.protocol.CanFormatMessages
import convergence.protocol.CanMentionUsers
import convergence.protocol.HasNicknames

fun getUserFromName(chat: Chat, name: String): User? {
    var alternateOption: User? = null
    val protocol = chat.protocol
    if (protocol is CanMentionUsers) {
        protocol.getUserFromMentionText(chat, name)?.let {
            return it
        }
    }
    if (protocol is HasNicknames) {
        for (user in protocol.getUsers(chat)) {
            val currentName = protocol.getUserName(chat, user)
            val nickname = protocol.getUserNickname(chat, user)
            if (nickname == name || currentName == name)
                return user
            else if (alternateOption == null && name in currentName)
                alternateOption = user
        }
    } else {
        for (user in protocol.getUsers(chat)) {
            val currentName = protocol.getUserName(chat, user)
            if (currentName == name)
                return user
            else if (alternateOption == null && name in currentName)
                alternateOption = user
        }
    }
    return alternateOption
}

fun echo(args: List<String>) = args.joinToString(" ")

@Suppress("FunctionOnlyReturningConstant")
fun ping() = "Pong!"

fun me(args: List<String>, chat: Chat, sender: User): String {
    val protocol = chat.protocol as? CanFormatMessages
    val (boldOpen, boldClose) = protocol?.getDelimiters(Format.bold) ?: Pair("", "")
    return "$boldOpen*${getUserName(chat, sender)} ${args.joinToString(" ")}$boldClose."
}

fun target(args: List<String>, chat: Chat): String {
    if (args.isEmpty()) {
        return "You need to pass some arguments! Syntax: " + bot.commands[UniversalProtocol]!!["target"]!!.syntaxText
    }
    val user = getUserFromName(chat, args[args.size - 1])
        ?: return "No user by the name \"${args[args.size - 1]}\" found."
    return args.subList(0, args.size - 1).joinToString(" ").replace("%target", chat.protocol.getUserName(chat, user))
}

fun targetNick(args: List<String>, chat: Chat): String {
    if (args.isEmpty()) {
        return "You need to pass some arguments! Syntax: " + bot.commands[UniversalProtocol]!!["target"]!!.syntaxText
    }
    if (chat.protocol !is HasNicknames)
        return "This protocol doesn't support nicknames!"
    val protocol = chat.protocol as HasNicknames
    val user = getUserFromName(chat, args[args.size - 1])
        ?: return "No user by the name \"${args[args.size - 1]}\" found."
    return args.subList(0, args.size - 1).joinToString(" ")
        .replace("%target", protocol.getUserNickname(chat, user) ?: chat.protocol.getUserName(chat, user))
}
