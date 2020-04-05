package convergence

import kotlinx.serialization.Serializable
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext

class InvalidCommandException(msg: String): Exception(msg)

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

class InvalidEscapeSequence(message: String): Exception(message)

fun getCommand(command: String, chat: Chat): CommandLike {
    return when {
        chat in commands && commands[chat] is MutableMap<String, Command> && command in commands[chat]!! -> commands[chat]!![command] as CommandLike
        chat in aliases && aliases[chat] is MutableMap<String, Alias> && command in aliases[chat]!! -> aliases[chat]!![command] as CommandLike
        UniversalChat in commands && commands[UniversalChat] is MutableMap<String, Command> && command in commands[UniversalChat]!! -> commands[UniversalChat]!![command] as CommandLike
        else -> throw CommandDoesNotExist(command)
    }
}

// This function replaces the escape sequences with their replaced variants, and ignores quotes, so the quotes don't
// show up in the argument text.
fun CommonToken.text(): String = when (this.type) {
    commandLexer.OctalEscape -> Integer.parseInt(this.text.substring(1), 8).toChar().toString()
    commandLexer.UnicodeEscape -> Integer.parseInt(this.text.substring(2), 16).toChar().toString()
    commandLexer.RegularEscape -> when (this.text[1]) {
        'r' -> '\r'
        'n' -> '\n'
        'b' -> '\b'
        't' -> '\t'
        'f' -> '\u000c'
        '\'' -> '\''
        '"' -> '"'
        '\\' -> '\\'
        else -> throw InvalidEscapeSequence(this.text)
    }.toString()
    commandLexer.Quote -> ""
    else -> this.text
}

fun parseCommand(command: String, chat: Chat): CommandData? = parseCommand(command, commandDelimiters[chat], chat)
fun parseCommand(command: String, commandDelimiter: String, chat: Chat): CommandData? {
    // Check for the command delimiter, so the grammar doesn't have to worry about it
    if (!command.startsWith(commandDelimiter))
        return null
    // Set up antlr
    val input = command.substring(commandDelimiter.length)
    val chars = CharStreams.fromString(input, chat.name)
    val lexer = commandLexer(chars)
    val tokens = CommonTokenStream(lexer)
    val parser = commandParser(tokens)
    parser.buildParseTree = true
    // Read a command
    val tree = parser.command()

    // Check if there were any invalid escape sequences, and error if there were.
    val invalidEscapes = tokens.tokens.filter { it.type == commandParser.InvalidEscape }
    if (invalidEscapes.isNotEmpty())
        throw InvalidEscapeSequence("Command \"$command\" contains the following invalid escape sequences: \"${invalidEscapes.joinToString("\", \"") { it.text }}\".")

    // See if the command was actually parsed successfully, and error if it wasn't.
    if (tree.exception != null)
        throw InvalidCommandException(tree.exception?.message ?: "")

    // Grab the command name
    val commandName = tree.commandName().text

    // Grab the args
    val args = tree.argument().map {
        (it.children.first() as ParserRuleContext).children.joinToString("") { tok ->
            if (tok.childCount == 0) // If we're looking at a token, use the text extension function I made above
                (tok.payload as CommonToken).text()
            else
                tok.text // If it's a parser rule, just return text, don't mess with it
        }
    }

    // Return the command or alias object
    val cmd = if (commandName != null) getCommand(commandName, chat) else return null
    return if (cmd is Command) CommandData(cmd, args) else CommandData(cmd as Alias, args)
}

