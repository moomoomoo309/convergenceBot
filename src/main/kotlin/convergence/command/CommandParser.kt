package convergence.command

import convergence.DEFAULT_COMMAND_DELIMITER
import convergence.UniversalProtocol
import convergence.bot
import convergence.model.Chat
import convergence.protocol.HasServer
import convergence.settings
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

data class CommandWithArgs(var command: Command, var args: List<String>) {
    constructor(alias: Alias, args: List<String>): this(alias.command, alias.args + args)
}

class InvalidEscapeSequenceException(message: String): Exception(message)

private fun <CommandType: CommandLike, ScopeType> commandAvailable(
    list: MutableMap<ScopeType, MutableMap<String, CommandType>>,
    scope: ScopeType,
    command: String
) = scope in list && command in list[scope]!!

fun getCommand(chat: Chat, command: String): CommandLike {
    return when {
        // Chat Alias
        commandAvailable(settings.aliases, chat, command) -> settings.aliases[chat]!![command]
        // Server Alias
        chat is HasServer<*> && commandAvailable(settings.aliases, chat.server, command) -> settings.aliases[chat.server]!![command]
        // Protocol Command
        commandAvailable(bot.commands, chat.protocol, command) -> bot.commands[chat.protocol]!![command]
        // Universal Command
        commandAvailable(bot.commands, UniversalProtocol, command) -> bot.commands[UniversalProtocol]!![command]
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

fun parseCommand(chat: Chat, command: String): CommandWithArgs? =
    parseCommand(chat, settings.commandDelimiters.getOrDefault(chat, DEFAULT_COMMAND_DELIMITER), command)

@SuppressWarnings("ThrowsCount")
fun parseCommand(chat: Chat, commandDelimiter: String, command: String): CommandWithArgs? {
    // Check for the command delimiter, so the grammar doesn't have to worry about it
    if (!command.startsWith(commandDelimiter) || command.isEmpty() || command == commandDelimiter)
        return null
    if (command.startsWith(commandDelimiter + commandDelimiter))
        return null
    // Set up ANTLR and fill the token stream so all tokens are available for inspection.
    val input = command.substring(commandDelimiter.length)
    val chars = CharStreams.fromString(input, chat.name)
    val lexer = convergence.CommandLexer(chars)
    val tokens = CommonTokenStream(lexer)
    tokens.fill()

    // Check for invalid escape sequences before parsing. This must be outside the
    // try-catch below so that InvalidEscapeSequenceException propagates to the caller
    // rather than being wrapped in InvalidCommandParseException.
    val invalidEscapes = tokens.tokens.filter { it.type == convergence.CommandParser.InvalidEscape }
    if (invalidEscapes.isNotEmpty())
        throw InvalidEscapeSequenceException(
            "Command \"$command\" contains the following invalid escape sequences: \"${
                invalidEscapes.joinToString("\", \"") { it.text }
            }\"."
        )

    val tree = try {
        val parser = convergence.CommandParser(tokens)
        parser.buildParseTree = true
        // Read a command
        parser.command()
    } catch(e: Exception) {
        throw InvalidCommandParseException(e)
    }
    val errorTokens = tree.children.filterIsInstance<ErrorNode>()
    if (errorTokens.isNotEmpty()) {
        throw InvalidCommandParseException(
            "The following invalid tokens were detected: " +
                    errorTokens.joinToString("\", \"", "\"", "\"") { it.text }
        )
    }

    // See if the command was actually parsed successfully, and error if it wasn't.
    if (tree.exception != null)
        throw InvalidCommandParseException(tree.exception)

    val commandName = tree.commandName().text
    val args = tokenArgsToStringArgs(tree)

    val cmd = commandName?.let { getCommand(chat, it.lowercase()) } ?: return null
    return when(cmd) {
        is Command -> CommandWithArgs(cmd, args)
        is Alias -> CommandWithArgs(cmd, args)
    }
}

// This function replaces the escape sequences with their replaced variants, and ignores quotes, so the quotes don't
// show up in the argument text.
fun CommonToken.text() = when(this.type) {
    convergence.CommandLexer.OctalEscape -> Integer.parseInt(this.text.substring(1), 8).toChar()
    convergence.CommandLexer.UnicodeEscape -> Integer.parseInt(this.text.substring(2), 16).toChar()
    convergence.CommandLexer.RegularEscape -> escapeMap[this.text[1]] ?: throw InvalidEscapeSequenceException(this.text)
    convergence.CommandLexer.Quote -> "" // This prevents quoted arguments from having the quotes around the text.
    else -> this.text
}.toString()

private fun tokenArgsToStringArgs(tree: convergence.CommandParser.CommandContext): List<String> = tree.argument().map {
    (it.children.first() as ParserRuleContext).children.joinToString("") { tokOrRule ->
        if (tokOrRule.childCount == 0) // If we're looking at a token, use the text extension function above
            (tokOrRule.payload as CommonToken).text()
        else if (tokOrRule.payload is convergence.CommandParser.NotQuoteContext) {
            val notQuote = tokOrRule.payload as convergence.CommandParser.NotQuoteContext
            val node = notQuote.children.first() as TerminalNode
            val token = node.symbol as CommonToken
            token.text()
        } else
            tokOrRule.text // If it's any other parser rule, just return text, don't mess with it
    }
}
