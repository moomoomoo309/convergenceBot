package convergence

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext

class InvalidCommandException(msg: String): Exception(msg)

data class CommandData(var command: Command, var args: List<String>): JsonConvertible {
    constructor(alias: Alias, args: List<String>): this(alias.command, alias.args + args)

    operator fun invoke(vararg args: String, sender: User): String? = this.command.function(args.toList(), sender)
    operator fun invoke(args: List<String>, sender: User): String? = this.command.function(args, sender)
    operator fun invoke(sender: User): String? = invoke(args, sender)
}

class InvalidEscapeSequence(message: String): Exception(message)

private fun <T: CommandLike> commandAvailable(
    list: MutableMap<Chat, MutableMap<String, T>>,
    chat: Chat,
    command: String
) =
    chat in list && list[chat] is MutableMap<String, *> && command in list[chat]!!

fun getCommand(command: String, chat: Chat): CommandLike {
    return when {
        commandAvailable(commands, chat, command) -> commands[chat]!![command]
        commandAvailable(aliases, chat, command) -> aliases[chat]!![command]
        commandAvailable(commands, UniversalChat, command) -> commands[UniversalChat]!![command]
        else -> throw CommandDoesNotExist(command)
    } ?: throw CommandDoesNotExist(command)
}

// This function replaces the escape sequences with their replaced variants, and ignores quotes, so the quotes don't
// show up in the argument text.
fun CommonToken.text() = when (this.type) {
    commandLexer.OctalEscape -> Integer.parseInt(this.text.substring(1), 8).toChar()
    commandLexer.UnicodeEscape -> Integer.parseInt(this.text.substring(2), 16).toChar()
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
    }
    commandLexer.Quote -> "" // This prevents quoted arguments from having the quotes around the text.
    else -> this.text
}.toString()

fun parseCommand(command: String, chat: Chat): CommandData? = parseCommand(command, commandDelimiters[chat]!!, chat)
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
    if (!tree.exception?.message.isNullOrBlank())
        throw InvalidCommandException(tree.exception?.message ?: "")
    else if (tree.exception != null)
        throw tree.exception


    // Grab the command name
    val commandName = tree.commandName().text

    // Grab the args
    val args = tree.argument().map {
        (it.children.first() as ParserRuleContext).children.joinToString("") { tok ->
            if (tok.childCount == 0) // If we're looking at a token, use the text extension function above
                (tok.payload as CommonToken).text()
            else
                tok.text // If it's a parser rule, just return text, don't mess with it
        }
    }

    // Return the command or alias object
    val cmd = if (commandName != null) getCommand(commandName, chat) else return null
    return if (cmd is Command) CommandData(cmd, args) else CommandData(cmd as Alias, args)
}
