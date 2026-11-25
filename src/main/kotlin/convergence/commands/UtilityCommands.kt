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

fun getUserFromName(chat: Chat<*>, name: String): User<*>? {
    var alternateOption: User<*>? = null
    val protocol = chat.protocol
    if (protocol is CanMentionUsers<*, *, *>) {
        protocol.getUserFromMentionTextFor(chat, name)?.let {
            return it
        }
    }
    val nickProtocol = protocol as? HasNicknames<*, *>
    for (user in protocol.getUsersOf(chat)) {
        val currentName = protocol.getUserNameFor(chat, user)
        val nickname = nickProtocol?.getUserNicknameFor(chat, user)
        if (nickname == name || currentName == name)
            return user
        else if (alternateOption == null && name in currentName)
            alternateOption = user
    }
    return alternateOption
}

fun echo(args: List<String>) = args.joinToString(" ")

@Suppress("FunctionOnlyReturningConstant")
fun ping() = "Pong!"

fun me(args: List<String>, chat: Chat<*>, sender: User<*>): String {
    val protocol = chat.protocol as? CanFormatMessages
    val (boldOpen, boldClose) = protocol?.getDelimiters(Format.bold) ?: Pair("", "")
    return "$boldOpen*${getUserName(chat, sender)} ${args.joinToString(" ")}$boldClose."
}

fun target(args: List<String>, chat: Chat<*>): String {
    if (args.isEmpty()) {
        return "You need to pass some arguments! Syntax: " + bot.commands[UniversalProtocol]!!["target"]!!.syntaxText
    }
    val user = getUserFromName(chat, args[args.size - 1])
        ?: return "No user by the name \"${args[args.size - 1]}\" found."
    return args.subList(0, args.size - 1).joinToString(" ").replace("%target", chat.protocol.getUserNameFor(chat, user))
}

fun targetNick(args: List<String>, chat: Chat<*>): String {
    if (args.isEmpty()) {
        return "You need to pass some arguments! Syntax: " + bot.commands[UniversalProtocol]!!["target"]!!.syntaxText
    }
    val protocol = chat.protocol as? HasNicknames<*, *> ?: return "This protocol doesn't support nicknames!"
    val user = getUserFromName(chat, args[args.size - 1])
        ?: return "No user by the name \"${args[args.size - 1]}\" found."
    return args.subList(0, args.size - 1).joinToString(" ")
        .replace("%target", protocol.getUserNicknameFor(chat, user) ?: chat.protocol.getUserNameFor(chat, user))
}
