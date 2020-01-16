package convergence

import kotlinx.serialization.Serializable

@Serializable
data class CommandData(var command: Command, var args: List<String>) {
    constructor(alias: Alias, args: List<String>): this(alias.command, alias.args + args)

    operator fun invoke(args: List<String>, sender: User): String? = this.command.function(args, sender)
    operator fun invoke(sender: User): String? = invoke(args, sender)
}

private fun isEscapeCharacter(c: Char, l: Int): Boolean {
    return when (c) {
        'b', 't', 'n', 'f', 'r', '"', '\'', '\\', 'u' -> l == 0
        in '0'..'9' -> true
        else -> false
    }
}

private val validUnicodeChars = setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'a', 'B', 'b', 'C', 'c', 'D', 'd', 'E', 'e', 'F', 'f')
private fun isEscapeSequence(s: String, throwException: Boolean = true): Boolean {
    if (s.isEmpty())
        return false
    val c = s[0]
    val l = s.length
    return when (c) {
        'u' -> l == 5 && s.subSequence(1, 5).all { it in validUnicodeChars }
        in '0'..'9' -> {
            var i = 0
            if (s[0] !in '0'..'3')
                return if (throwException) false else throw InvalidEscapeSequence("\"$s\" (Did not capture entire escape, stopped at first invalid character)")
            s.all {
                when (i) {
                    0 -> s[i++] in '0'..'3'
                    1, 2 -> s[i++] in '0'..'7'
                    else -> if (throwException) false else throw InvalidEscapeSequence("\"$s\" (Did not capture entire escape, stopped at first invalid character)")
                }
            }
        }
        else -> isEscapeCharacter(c, l)
    }
}

class InvalidEscapeSequence(message: String): Exception(message)

fun getCommand(command: String, chat: Chat): CommandLike {
    return when {
        chat in commands && commands[chat] is MutableMap<String, Command> && command in commands[chat]!! -> commands[chat]!![command] as CommandLike
        chat in aliases && aliases[chat] is MutableMap<String, Alias> && command in aliases[chat]!! -> aliases[chat]!![command] as CommandLike
        UniversalChat in commands && commands[UniversalChat] is MutableMap<String, Command> && command in commands[UniversalChat]!! -> commands[UniversalChat]!![command] as CommandLike
        else -> throw CommandDoesNotExist(command)
    }
}

fun parseCommand(command: String, chat: Chat): CommandData? = parseCommand(command, commandDelimiters[chat], chat)
fun parseCommand(command: String, commandDelimiter: String, chat: Chat): CommandData? {
    var commandName: String? = null
    val argList = ArrayList<String>()
    var inQuote = false
    var hasCommandDelimiter = false
    var unicodeEscapeCharactersRead: Byte = 0
    var escapeLength = 0
    val currentContent = StringBuilder()
    val currentEscapeCharacter = StringBuilder()
    var lastCharWasEscape = false // Needed if the last character is not an escape.
    for ((i, c) in command.withIndex()) {
        // Make sure the command delimiter is on there, or it's not a command.
        if (!hasCommandDelimiter) {
            when {
                currentContent.toString() == commandDelimiter -> {
                    hasCommandDelimiter = true
                    currentContent.setLength(0)
                }
                commandDelimiter.startsWith(currentContent.toString()) -> currentContent.append(c)
                else -> return null
            }
        }
        // Deal with escape characters.
        if (escapeLength > 0) {
            escapeLength++
            currentEscapeCharacter.append(c)
            if (c == 'u')
                unicodeEscapeCharactersRead = 1
            else if (unicodeEscapeCharactersRead in 1..4)
                if (c in validUnicodeChars)
                    unicodeEscapeCharactersRead++
                else
                    throw InvalidEscapeSequence(currentEscapeCharacter.toString())
            // Previously, this was an else if, but on the last character, we still need to go here,
            // even if one of the two branches above run.
            if (unicodeEscapeCharactersRead !in 1..4 && !isEscapeSequence(currentEscapeCharacter.toString()) || i == command.length - 1)
                if (escapeLength < 2)
                    throw InvalidEscapeSequence(currentEscapeCharacter.toString())
                else {
                    val currentEscapeStr = currentEscapeCharacter.toString()
                    currentContent.append(when (currentEscapeStr) {
                        "b" -> '\b'
                        "t" -> '\t'
                        "n" -> '\n'
                        "f" -> '\u000c' // Kotlin thinks \f is an invalid escape, and it's wrong.
                        "r" -> '\r'
                        "\"" -> '"'
                        "'" -> '\''
                        "\\" -> '\\'
                        "u" -> throw InvalidEscapeSequence(currentEscapeStr)
                        else -> {
                            unicodeEscapeCharactersRead = 0
                            if (currentEscapeStr.isEmpty()) // For an invalid octal escape starting with a character higher than 3.
                                throw InvalidEscapeSequence(currentEscapeStr)
                            when (currentEscapeStr[0]) {
                                'u' -> if (currentEscapeStr.length >= 5 && isEscapeSequence(currentEscapeStr.substring(0, 5)))
                                    Integer.parseInt(currentEscapeStr.substring(1, 5), 16).toChar()
                                else
                                    throw InvalidEscapeSequence(currentEscapeStr) // This will only run if the last character is part of an invalid unicode escape.
                                in '0'..'9' -> {
                                    val octalInt = Integer.parseInt(currentEscapeStr.trim(), 8)
                                    if (octalInt > 255)
                                        throw InvalidEscapeSequence(currentEscapeStr)
                                    else
                                        octalInt.toChar()
                                }
                                else -> throw InvalidEscapeSequence(currentEscapeStr)
                            }
                        }
                    })
                    lastCharWasEscape = true
                    currentEscapeCharacter.setLength(0)
                    escapeLength = 0
                    if (c == '\\') continue
                }
        }

        // Actually get the command and its arguments.
        if (hasCommandDelimiter && escapeLength == 0)
            when {
                c == '\\' -> when {
                    i == command.length - 1 -> throw InvalidEscapeSequence("\\") // Only occurs when the last character is an empty escape (just a lone backslash)
                    escapeLength == 0 -> escapeLength = 1
                }
                c.isWhitespace() || i == command.length - 1 ->
                    if (!inQuote) {
                        if (!lastCharWasEscape && i == command.length - 1) // If the last char isn't an escape and we're at the end of the string, this kicks in.
                            currentContent.append(c)
                        if (commandName == null)
                            commandName = currentContent.toString()
                        else if (currentContent.isNotEmpty())
                            argList.add(currentContent.toString())
                        currentContent.setLength(0)
                    } else {
                        if (i == command.length - 1) {
                            if (c != '"')
                                throw InvalidEscapeSequence("Unmatched \"")
                            argList.add(currentContent.toString())
                            currentContent.setLength(0)
                        } else
                            currentContent.append(c)
                    }
                c == '"' -> {
                    inQuote = !inQuote
                    if (!inQuote) {
                        argList.add(currentContent.toString())
                        currentContent.setLength(0)
                    }
                }
                else -> if (!lastCharWasEscape) currentContent.append(c)
            }
        lastCharWasEscape = false
    }

    val cmd = if (commandName != null) getCommand(commandName, chat) else return null
    return if (cmd is Command) CommandData(cmd, argList) else CommandData(cmd as Alias, argList)
}

