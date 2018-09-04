package convergence

data class CommandData(val command: Command, val args: List<String>)

private fun isEscapeCharacter(c: Char, l: Int): Boolean {
    return when (c) {
        'b', 't', 'n', 'f', 'r', '"', '\'', '\\', 'u' -> l == 1
        in '0'..'9' -> true
        else -> false
    }
}

private val validUnicodeChars = setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'a', 'B', 'b', 'C', 'c', 'D', 'd', 'E', 'e', 'F', 'f')
private fun isEscapeSequence(s: String): Boolean {
    val c = s[0]
    val l = s.length
    return when (c) {
        'u' -> s.length <= 5 && s.subSequence(1, s.length).all { it in validUnicodeChars }
        in ('0'..'9') -> when (l) {
            1 -> ('0'..'3').contains(c)
            2, 3 -> ('0'..'7').contains(c)
            else -> false
        }
        else -> isEscapeCharacter(c, l)
    }
}

class InvalidEscapeSequence : Exception()

fun getCommand(command: String, chat: Chat): Command? {
    return when {
        chat in universalCommands -> universalCommands[chat]!![command]
        commands[chat] != null -> commands[chat]!![command]
        else -> null
    }
}

fun parse(command: String, commandDelimiter: String, chat: Chat): CommandData? {
    var commandName: String? = null
    var argList: List<String> = emptyList()
    var inQuote = false
    var hasCommandDelimiter = false
    var escapeLength = 0
    val currentContent = StringBuilder()
    val currentEscapeCharacter = StringBuilder()
    for (c: Char in command) {
        // Make sure the command delimiter is on there, or it's not a command.
        if (!hasCommandDelimiter) {
            if (currentContent.toString() == commandDelimiter) {
                hasCommandDelimiter = true
                currentContent.setLength(0)
            }
        } else {
            // Deal with escape characters.
            if (escapeLength > 0) {
                escapeLength++
                currentEscapeCharacter.append(c)
                if (!isEscapeSequence(currentEscapeCharacter.toString()))
                    if (escapeLength <= 2)
                        throw InvalidEscapeSequence()
                    else {
                        currentEscapeCharacter.setLength(0)
                        escapeLength = 0
                    }
            }
            // Actually get the command and its arguments.
            if (escapeLength == 0)
                when {
                    c.isWhitespace() ->
                        if (!inQuote) {
                            if (commandName == null)
                                commandName = currentContent.toString()
                            else
                                argList += currentContent.toString()
                            currentContent.setLength(0)
                        }
                    c == '"' -> inQuote = !inQuote
                    c == '\\' -> escapeLength++
                    else -> currentContent.append(c)
                }
        }
    }
    val cmd = if (commandName != null) getCommand(commandName, chat) else null
    return if (cmd != null) CommandData(cmd, argList) else null
}

