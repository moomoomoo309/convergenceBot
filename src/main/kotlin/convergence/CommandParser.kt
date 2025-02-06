package convergence

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext

class InvalidCommandParseException(msg: String): Exception(msg)

data class CommandData(var command: Command, var args: List<String>) {
    constructor(alias: Alias, args: List<String>): this(alias.command, alias.args + args)

    operator fun invoke(args: List<String>, chat: Chat, sender: User): String? =
        this.command.function(args, chat, sender)

    operator fun invoke(vararg args: String, chat: Chat, sender: User): String? =
        invoke(args.toList(), chat, sender)

    operator fun invoke(chat: Chat, sender: User): String? = invoke(args, chat, sender)
}

class InvalidEscapeSequenceException(message: String): Exception(message)

private fun <T: CommandLike> commandAvailable(
    list: MutableMap<*, MutableMap<String, T>>,
    chat: CommandScope,
    command: String
) = chat in list && list[chat] is MutableMap<String, *> && command in list[chat]!!

fun getCommand(command: String, commandScope: CommandScope): CommandLike {
    return when {
        commandAvailable(aliases, commandScope, command) -> aliases[commandScope]!![command]
        commandAvailable(commands, commandScope, command) -> commands[commandScope.protocol]!![command]
        commandAvailable(commands, UniversalChat, command) -> commands[UniversalProtocol]!![command]
        else -> null
    } ?: throw CommandDoesNotExist(command)
}

// This function replaces the escape sequences with their replaced variants, and ignores quotes, so the quotes don't
// show up in the argument text.
fun CommonToken.text() = when(this.type) {
    CommandLexer.OctalEscape -> Integer.parseInt(this.text.substring(1), 8).toChar()
    CommandLexer.UnicodeEscape -> Integer.parseInt(this.text.substring(2), 16).toChar()
    CommandLexer.RegularEscape -> when(this.text[1]) {
        'r' -> '\r'
        'n' -> '\n'
        'b' -> '\b'
        't' -> '\t'
        'f' -> '\u000c'
        '\'' -> '\''
        '"' -> '"'
        '\\' -> '\\'
        else -> throw InvalidEscapeSequenceException(this.text)
    }

    CommandLexer.Quote -> "" // This prevents quoted arguments from having the quotes around the text.
    else -> this.text
}.toString()

fun parseCommand(command: String, chat: Chat): CommandData? =
    parseCommand(command, commandDelimiters.getOrDefault(chat, defaultCommandDelimiter), chat)

fun parseCommand(command: String, commandDelimiter: String, chat: Chat): CommandData? {
    // Check for the command delimiter, so the grammar doesn't have to worry about it
    if (!command.startsWith(commandDelimiter))
        return null
    // Set up antlr
    val input = command.substring(commandDelimiter.length)
    val chars = CharStreams.fromString(input, chat.name)
    val lexer = CommandLexer(chars)
    val tokens = CommonTokenStream(lexer)
    val parser = CommandParser(tokens)
    parser.buildParseTree = true
    // Read a command
    val tree = parser.command()

    // Check if there were any invalid escape sequences, and error if there were.
    val invalidEscapes = tokens.tokens.filter { it.type == CommandParser.InvalidEscape }
    if (invalidEscapes.isNotEmpty())
        throw InvalidEscapeSequenceException(
            "Command \"$command\" contains the following invalid escape sequences: \"${
                invalidEscapes.joinToString(
                    "\", \""
                ) { it.text }
            }\"."
        )

    // See if the command was actually parsed successfully, and error if it wasn't.
    if (!tree.exception?.message.isNullOrBlank())
        throw InvalidCommandParseException(tree.exception?.message ?: "")
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
    val cmd = commandName?.let { getCommand(it, chat) } ?: return null
    return when(cmd) {
        is Command -> CommandData(cmd, args)
        is Alias -> CommandData(cmd, args)
    }
}
