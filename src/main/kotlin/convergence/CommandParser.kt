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

data class CommandWithArgs(var command: Command, var args: List<String>) {
    constructor(alias: Alias, args: List<String>): this(alias.command, alias.args + args)
}

class InvalidEscapeSequenceException(message: String): Exception(message)

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

typealias CommandExtractor = (String, Chat) -> CommandLike

class CommandParserService(
    private val settings: Settings,
    private val messaging: MessagingService
) {
    fun parse(command: String, chat: Chat, getCommand: CommandExtractor): CommandWithArgs? =
        parse(command, settings.commandDelimiters.getOrDefault(chat, DEFAULT_COMMAND_DELIMITER), chat, getCommand)

    fun parseSafe(chat: Chat, message: String, sender: User, getCommand: CommandExtractor): CommandWithArgs? = try {
        parse(message, chat, getCommand)
    } catch(e: CommandDoesNotExist) {
        messaging.sendMessage(chat, sender, "No command exists with name \"${e.message}\".")
        null
    } catch(e: InvalidEscapeSequenceException) {
        messaging.sendMessage(chat, sender, "Invalid escape sequence \"${e.message}\" passed." +
                " Are your backslashes correct?")
        null
    }

    @SuppressWarnings("ThrowsCount")
    fun parse(command: String, commandDelimiter: String, chat: Chat, getCommand: CommandExtractor): CommandWithArgs? {
        // Check for the command delimiter, so the grammar doesn't have to worry about it
        if (!command.startsWith(commandDelimiter) || command.isEmpty() || command == commandDelimiter)
            return null
        if (command.startsWith(commandDelimiter + commandDelimiter))
            return null
        // Set up ANTLR and fill the token stream so all tokens are available for inspection.
        val input = command.substring(commandDelimiter.length)
        val chars = CharStreams.fromString(input, chat.name)
        val lexer = CommandLexer(chars)
        val tokens = CommonTokenStream(lexer)
        tokens.fill()

        // Check for invalid escape sequences before parsing. This must be outside the
        // try-catch below so that InvalidEscapeSequenceException propagates to the caller
        // rather than being wrapped in InvalidCommandParseException.
        val invalidEscapes = tokens.tokens.filter { it.type == CommandParser.InvalidEscape }
        if (invalidEscapes.isNotEmpty())
            throw InvalidEscapeSequenceException(
                "Command \"$command\" contains the following invalid escape sequences: \"${
                    invalidEscapes.joinToString("\", \"") { it.text }
                }\"."
            )

        val tree = try {
            val antlrParser = CommandParser(tokens)
            antlrParser.buildParseTree = true
            // Read a command
            antlrParser.command()
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

        val cmd = commandName?.let {
            getCommand(it.lowercase(), chat)
        } ?: return null
        return when(cmd) {
            is Command -> CommandWithArgs(cmd, args)
            is Alias -> CommandWithArgs(cmd, args)
        }
    }
}

// This function replaces the escape sequences with their replaced variants, and ignores quotes, so the quotes don't
// show up in the argument text.
fun CommonToken.text() = when(this.type) {
    CommandLexer.OctalEscape -> Integer.parseInt(this.text.substring(1), 8).toChar()
    CommandLexer.UnicodeEscape -> Integer.parseInt(this.text.substring(2), 16).toChar()
    CommandLexer.RegularEscape -> escapeMap[this.text[1]] ?: throw InvalidEscapeSequenceException(this.text)
    CommandLexer.Quote -> "" // This prevents quoted arguments from having the quotes around the text.
    else -> this.text
}.toString()

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
