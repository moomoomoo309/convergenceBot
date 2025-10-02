package convergence

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.TerminalNode

class InvalidCommandParseException: Exception {
    constructor(msg: String): super(msg)
    constructor(e: Exception): super(e)
}

data class CommandData(var command: Command, var args: List<String>) {
    constructor(alias: Alias, args: List<String>): this(alias.command, alias.args + args)

    operator fun invoke(args: List<String>, chat: Chat, sender: User) = this.command.function(args, chat, sender)
    operator fun invoke(vararg args: String, chat: Chat, sender: User) = invoke(args.toList(), chat, sender)
    operator fun invoke(chat: Chat, sender: User): OutgoingMessage? = invoke(args, chat, sender)
}

class InvalidEscapeSequenceException(message: String): Exception(message)

private fun <CommandType: CommandLike, ScopeType> commandAvailable(
    list: MutableMap<ScopeType, MutableMap<String, CommandType>>,
    scope: ScopeType,
    command: String
) = scope in list && command in list[scope]!!

fun getCommand(command: String, chat: Chat): CommandLike {
    return when {
        commandAvailable(aliases, chat, command) -> aliases[chat]!![command]
        chat is HasServer<*> && commandAvailable(aliases, chat.server, command) -> aliases[chat]!![command]
        commandAvailable(commands, chat.protocol, command) -> commands[chat.protocol]!![command]
        commandAvailable(commands, UniversalProtocol, command) -> commands[UniversalProtocol]!![command]
        else -> null
    } ?: throw CommandDoesNotExist(command)
}

private val escapeMap = mapOf(
    'r' to '\r',
    'n' to '\n',
    'b' to '\b',
    't' to '\t',
    'f' to '\u000c',
    '\'' to '\'',
    '"' to '"',
    '\\' to '\\'
)

// This function replaces the escape sequences with their replaced variants, and ignores quotes, so the quotes don't
// show up in the argument text.
fun CommonToken.text() = when(this.type) {
    CommandLexer.OctalEscape -> Integer.parseInt(this.text.substring(1), 8).toChar()
    CommandLexer.UnicodeEscape -> Integer.parseInt(this.text.substring(2), 16).toChar()
    CommandLexer.RegularEscape -> escapeMap[this.text[1]] ?: throw InvalidEscapeSequenceException(this.text)
    CommandLexer.Quote -> "" // This prevents quoted arguments from having the quotes around the text.
    else -> this.text
}.toString()

fun parseCommand(command: String, chat: Chat): CommandData? =
    parseCommand(command, commandDelimiters.getOrDefault(chat, DEFAULT_COMMAND_DELIMITER), chat)

@SuppressWarnings("ThrowsCount")
fun parseCommand(command: String, commandDelimiter: String, chat: Chat): CommandData? {
    // Check for the command delimiter, so the grammar doesn't have to worry about it
    if (!command.startsWith(commandDelimiter) || command.isEmpty() || command == commandDelimiter)
        return null
    if (command.startsWith(commandDelimiter + commandDelimiter))
        return null
    val tree = try {
        // Set up antlr
        val input = command.substring(commandDelimiter.length)
        val chars = CharStreams.fromString(input, chat.name)
        val lexer = CommandLexer(chars)
        val tokens = CommonTokenStream(lexer)
        val parser = CommandParser(tokens)
        parser.buildParseTree = true
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
        // Read a command
        parser.command()
    } catch(e: Exception) {
        throw InvalidCommandParseException(e)
    }
    val errorTokens = tree.children.filterIsInstance<ErrorNode>()
    if (errorTokens.isNotEmpty()) {
        throw InvalidCommandParseException("The following invalid tokens were detected: " +
                errorTokens.joinToString("\", \"", "\"", "\"") { it.text })
    }

    // See if the command was actually parsed successfully, and error if it wasn't.
    if (tree.exception != null)
        throw InvalidCommandParseException(tree.exception)

    val commandName = tree.commandName().text
    val args = tokenArgsToStringArgs(tree)

    val cmd = commandName?.let { getCommand(it.lowercase(), chat) } ?: return null
    return when(cmd) {
        is Command -> CommandData(cmd, args)
        is Alias -> CommandData(cmd, args)
    }
}

private fun tokenArgsToStringArgs(tree: CommandParser.CommandContext): List<String> = tree.argument().map {
    (it.children.first() as ParserRuleContext).children.joinToString("") { tokOrRule ->
        if (tokOrRule.childCount == 0) // If we're looking at a token, use the text extension function above
            (tokOrRule.payload as CommonToken).text()
        else if (tokOrRule.payload is CommandParser.NotQuoteContext) {
            val notQuote = tokOrRule.payload as CommandParser.NotQuoteContext
            val node = notQuote.children.first() as TerminalNode
            val token = node.symbol as CommonToken
            token.text()
        } else
            tokOrRule.text // If it's any other parser rule, just return text, don't mess with it
    }
}
