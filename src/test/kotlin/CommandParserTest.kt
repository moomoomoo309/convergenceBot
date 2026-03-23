import convergence.*
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TestChat: Chat(UniversalProtocol, "Test") {
    override fun toKey() = "TestChat(Test)"
}

object TestProtocol: Protocol("Test") {
    override fun init() {
        // Do nothing
    }

    override fun configLoaded() {
        TODO("Not needed for tests")
    }

    override fun aliasCreated(alias: Alias) {
        TODO("Not needed for tests")
    }

    override fun sendMessage(chat: Chat, message: OutgoingMessage): Boolean {
        TODO("Not needed for tests")
    }

    override fun getBot(chat: Chat): User {
        TODO("Not needed for tests")
    }

    override fun getUserName(chat: Chat, user: User): String {
        TODO("Not needed for tests")
    }

    override fun getChats(): List<Chat> {
        TODO("Not needed for tests")
    }

    override fun getUsers(): List<User> {
        TODO("Not needed for tests")
    }

    override fun getUsers(chat: Chat): List<User> {
        TODO("Not needed for tests")
    }

    override fun getChatName(chat: Chat): String {
        TODO("Not needed for tests")
    }

    override fun commandScopeFromKey(key: String): Chat? {
        TODO("Not needed for tests")
    }

    override fun userFromKey(key: String): User? {
        TODO("Not needed for tests")
    }
}

fun doNothing(unused: List<String>, unused2: Chat, unused3: User): String? {
    unused.run { unused2.run { unused3.run {} } }
    return null
}

class CommandParserTest {
    private fun loadCommandWithArgs(command: String): CommandWithArgs? {
        val testChat = TestChat()
        val testIndex = command.indexOf(" ")
        val testCommandStr = command.substring(1, if (testIndex == -1) command.length else testIndex)
        val testCommand = Command.of(testChat.protocol, testCommandStr, listOf(), ::doNothing, "test", "test")
        commands[testChat.protocol] = mutableMapOf(testCommandStr to testCommand)
        return parseCommand(command, testChat)
    }

    @Test
    fun validEscapes() {
        val testCommandWithArgs = loadCommandWithArgs("!test \\f \\\\ \\u0014 \\tb \\b")
        assertEquals("test", testCommandWithArgs?.command?.name, "Did not load test command correctly.")
        assertEquals("\u000c", testCommandWithArgs?.args?.get(0), "Did not escape \\f properly.")
        assertEquals("\\", testCommandWithArgs?.args?.get(1), "Did not escape \\\\ properly.")
        assertEquals("\u0014", testCommandWithArgs?.args?.get(2), "Did not escape \\u0014 properly.")
        assertEquals(
            "\tb",
            testCommandWithArgs?.args?.get(3),
            "Did not escape \\tb properly. (a \\t with a letter after it)"
        )
        assertEquals("\b", testCommandWithArgs?.args?.get(4), "Did not escape \\b properly.")
    }

    @Test
    fun validEscapesInQuotes() {
        val testCommandWithArgs = loadCommandWithArgs("!test \"\\f\" \"\\\\\" \"\\u0014\" \"\\tb\" \"\\b\"")
        assertEquals("test", testCommandWithArgs?.command?.name, "Did not load test command correctly.")
        assertEquals("\u000c", testCommandWithArgs?.args?.get(0), "Did not escape \\f properly.")
        assertEquals("\\", testCommandWithArgs?.args?.get(1), "Did not escape \\\\ properly.")
        assertEquals("\u0014", testCommandWithArgs?.args?.get(2), "Did not escape \\u0014 properly.")
        assertEquals(
            "\tb",
            testCommandWithArgs?.args?.get(3),
            "Did not escape \\tb properly. (a \\t with a letter after it)"
        )
        assertEquals("\b", testCommandWithArgs?.args?.get(4), "Did not escape \\b properly.")
    }

    @Test
    fun invalidEscape() {
        assertFailsWith<InvalidEscapeSequenceException>("Did not fail on empty escape.") {
            loadCommandWithArgs("!test2 \\")
        }
    }

    @Test
    fun invalidCommand() {
        assertFailsWith<InvalidCommandParseException>("Did not fail on invalid command.") {
            loadCommandWithArgs("!test2.5 abc\"def")
        }
    }

    @Test
    fun invalidUnicodeEscapes() {
        assertFailsWith<InvalidEscapeSequenceException>("Did not fail on empty unicode escape.") {
            loadCommandWithArgs("!test3 \\u")
        }
        assertFailsWith<InvalidEscapeSequenceException>("Did not fail on partial unicode escape.") {
            loadCommandWithArgs("!test4 \\u0")
        }
    }

    @Test
    fun validOctalEscapes() {
        val testCommandWithArgs = loadCommandWithArgs("!test5 h\\0 \\10 \\100")
        assertEquals("test5", testCommandWithArgs?.command?.name, "Did not load test5 command correctly.")
        assertEquals("h\u0000", testCommandWithArgs?.args?.get(0), "Did not escape \\0 properly.")
        assertEquals("\u0008", testCommandWithArgs?.args?.get(1), "Did not escape \\10 properly.")
        assertEquals("\u0040", testCommandWithArgs?.args?.get(2), "Did not escape \\100 properly.")
    }

    @Test
    fun invalidOctalEscape() {
        assertFailsWith<InvalidEscapeSequenceException>("Did not fail on out of range octal escape.") {
            loadCommandWithArgs("!test6 \\400")
        }
    }

    @Test
    fun notACommand() {
        val testCommandWithArgs = loadCommandWithArgs("Not a command")
        assertNull(testCommandWithArgs, "Tried to load a non-command as a command.")
    }


    private fun loadAliasData(command: String): CommandWithArgs? {
        val testChat = TestChat()
        val testAliasStr = command.substringBefore(" ").substringAfter(DEFAULT_COMMAND_DELIMITER).lowercase()
        val testCommand = Command.of(testChat.protocol, "test", listOf(), ::doNothing, "test", "test")
        val testAlias =
            Alias(testChat, testAliasStr, testCommand, listOf("testArg1", "testArg2"))
        commands[testChat.protocol] = mutableMapOf("test" to testCommand)
        aliases[testChat] = mutableMapOf(testAliasStr to testAlias)
        return parseCommand(command, testChat)
    }

    @Test
    fun aliasExpansion() {
        val testCommandWithArgs = loadAliasData("!testAlias nonAliasArg1 nonAliasArg2")
        assertEquals("test", testCommandWithArgs?.command?.name, "Did not expand alias correctly.")
        assertEquals("testArg1", testCommandWithArgs?.args?.get(0), "Did not expand first alias argument correctly.")
        assertEquals("testArg2", testCommandWithArgs?.args?.get(1), "Did not expand second alias argument correctly.")
        assertEquals(
            "nonAliasArg1",
            testCommandWithArgs?.args?.get(2),
            "Did not expand first non-alias argument correctly."
        )
        assertEquals(
            "nonAliasArg2",
            testCommandWithArgs?.args?.get(3),
            "Did not expand second non-alias argument correctly."
        )
    }

    @Test
    fun validCommand() {
        val testCommandWithArgs = loadCommandWithArgs("!commands")
        assertEquals("commands", testCommandWithArgs?.command?.name, "Did not parse valid command correctly.")
    }

    @Test
    fun validCommandWithQuotes() {
        val testCommandWithArgs = loadCommandWithArgs("!schedule \"5 seconds\" \"!echo hi\"")
        assertEquals("schedule", testCommandWithArgs?.command?.name, "Did not parse valid command correctly.")
        assertEquals("5 seconds", testCommandWithArgs?.args?.get(0), "Did not parse first valid quoted argument correctly.")
        assertEquals("!echo hi", testCommandWithArgs?.args?.get(1), "Did not parse second valid quoted argument correctly.")
    }

    @Test
    fun doubleQuotedArguments() {
        val testCommandWithArgs = loadCommandWithArgs("!echo \"Hi mailman!\"")
        assertEquals(
            "echo",
            testCommandWithArgs?.command?.name,
            "Did not parse valid command with double-quoted arguments correctly."
        )
        assertEquals("Hi mailman!", testCommandWithArgs?.args?.get(0), "Did not parse double-quoted arguments correctly.")
    }

    @Before
    fun setup() {
        commands.remove(UniversalProtocol)
        aliases.clear()
        commandDelimiters.clear()
    }

    @After
    fun teardown() {
        commands.remove(UniversalProtocol)
        aliases.clear()
        commandDelimiters.clear()
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private fun registerTestCommand(name: String): Command {
        val cmd = Command.of(UniversalProtocol, name, listOf(), ::doNothing, "test", "test")
        if (UniversalProtocol !in commands)
            commands[UniversalProtocol] = mutableMapOf()
        commands[UniversalProtocol]!![name.lowercase()] = cmd
        return cmd
    }

    private fun parse(input: String): CommandWithArgs? {
        val chat = TestChat()
        return parseCommand(input, chat)
    }

    private fun parseArgs(input: String): List<String> = parse(input)!!.args

    // ─── null / no-op returns ────────────────────────────────────────────────

    @Test
    fun emptyStringReturnsNull() {
        assertNull(parse(""))
    }

    @Test
    fun justDelimiterReturnsNull() {
        assertNull(parse("!"))
    }

    @Test
    fun doubleDelimiterReturnsNull() {
        registerTestCommand("echo")
        assertNull(parse("!!echo"))
    }

    @Test
    fun noDelimiterReturnsNull() {
        registerTestCommand("echo")
        assertNull(parse("echo hello"))
    }

    // ─── multi-character delimiters ──────────────────────────────────────────

    @Test
    fun multiCharDelimiterMatchesCorrectly() {
        val chat = TestChat()
        commands[UniversalProtocol] = mutableMapOf(
            "echo" to Command.of(UniversalProtocol, "echo", listOf(), ::doNothing, "test", "test")
        )
        commandDelimiters[chat] = "!!"
        val result = parseCommand("!!echo hello", "!!", chat)
        assertEquals("echo", result?.command?.name)
        assertEquals("hello", result?.args?.get(0))
    }

    @Test
    fun singleCharPrefixDoesNotMatchMultiCharDelimiter() {
        val chat = TestChat()
        commands[UniversalProtocol] = mutableMapOf(
            "echo" to Command.of(UniversalProtocol, "echo", listOf(), ::doNothing, "test", "test")
        )
        assertNull(parseCommand("!echo hello", "!!", chat))
    }

    @Test
    fun multiCharDoubleDelimiterGuardChecksFullDoublePrefix() {
        // The guard fires on delimiter+delimiter ("!!" + "!!" = "!!!!").
        // "!!!echo" starts with "!!" but NOT "!!!!", so the guard does NOT fire.
        // After stripping "!!", the input is "!echo". ANTLR error-recovers over the
        // leading "!", but its error-recovery includes "!" in commandName.text ("!echo"),
        // so getCommand("!echo", …) throws CommandDoesNotExist rather than finding "echo".
        val chat = TestChat()
        commands[UniversalProtocol] = mutableMapOf(
            "echo" to Command.of(UniversalProtocol, "echo", listOf(), ::doNothing, "test", "test")
        )
        assertFailsWith<CommandDoesNotExist> { parseCommand("!!!echo", "!!", chat) }
    }

    // ─── command name rules ───────────────────────────────────────────────────

    @Test
    fun numericCommandNameIsValid() {
        // commandName: Alnum+ allows digit-only names
        registerTestCommand("123")
        val result = parse("!123")
        assertEquals("123", result?.command?.name)
    }

    @Test
    fun alphanumericCommandNameIsValid() {
        registerTestCommand("cmd2go")
        assertEquals("cmd2go", parse("!cmd2go")?.command?.name)
    }

    @Test
    fun commandNameLookupIsCaseInsensitive() {
        // Keys are stored lowercase; !ECHO and !echo both find the same command.
        registerTestCommand("echo")
        assertNotNull(parse("!ECHO"), "Expected !ECHO to resolve to registered 'echo' command")
        assertNotNull(parse("!Echo"), "Expected !Echo to resolve to registered 'echo' command")
    }

    @Test
    fun commandNameWithDotThrows() {
        // A dot is not Alnum, so it is not a valid commandName character.
        assertFailsWith<Exception> { parse("!echo.cmd") }
    }

    // ─── argument whitespace handling ────────────────────────────────────────

    @Test
    fun noArgsProducesEmptyList() {
        registerTestCommand("ping")
        assertEquals(emptyList(), parse("!ping")?.args)
    }

    @Test
    fun trailingWhitespaceIsIgnored() {
        registerTestCommand("echo")
        assertEquals(listOf("hello"), parseArgs("!echo hello   "))
    }

    @Test
    fun multipleSpacesBetweenArgsCollapsed() {
        registerTestCommand("echo")
        assertEquals(listOf("hello", "world"), parseArgs("!echo hello   world"))
    }

    @Test
    fun tabSeparatesArguments() {
        registerTestCommand("echo")
        assertEquals(listOf("hello", "world"), parseArgs("!echo hello\tworld"))
    }

    @Test
    fun leadingWhitespaceAfterDelimiterThrows() {
        // "! echo" → strips "!" → " echo"; commandName: Alnum+ can't start with whitespace.
        // ANTLR error-recovers and produces a commandName of "", causing CommandDoesNotExist.
        assertFailsWith<Exception> { parse("! echo hello") }
    }

    // ─── quoted argument edge cases ───────────────────────────────────────────

    @Test
    fun emptyQuotedArgument() {
        registerTestCommand("echo")
        assertEquals(listOf(""), parseArgs("!echo \"\""))
    }

    @Test
    fun quotedArgWithOnlyWhitespace() {
        registerTestCommand("echo")
        assertEquals(listOf("   "), parseArgs("!echo \"   \""))
    }

    @Test
    fun quotedArgPreservesInternalSpaces() {
        registerTestCommand("echo")
        assertEquals(listOf("hello world foo"), parseArgs("!echo \"hello world foo\""))
    }

    @Test
    fun singleQuoteInsideDoubleQuotes() {
        registerTestCommand("echo")
        assertEquals(listOf("it's fine"), parseArgs("!echo \"it's fine\""))
    }

    @Test
    fun escapedDoubleQuoteInsideQuotedArg() {
        registerTestCommand("echo")
        assertEquals(listOf("say \"hello\""), parseArgs("!echo \"say \\\"hello\\\"\""))
    }

    @Test
    fun unclosedQuoteIsRecoveredByAntlr() {
        // ANTLR error-recovers by inserting the missing closing quote, so no exception
        // is thrown — the arg content is returned as if the quote were closed.
        registerTestCommand("echo")
        val result = parse("!echo \"unclosed")
        assertNotNull(result)
        assertEquals("unclosed", result.args[0])
    }

    @Test
    fun adjacentQuotedArgsWithoutSpaceThrows() {
        // After the first closing quote there is no Whitespace+, so the parser cannot
        // start the next argument — this IS caught and throws.
        registerTestCommand("echo")
        assertFailsWith<Exception> { parse("!echo \"a\"\"b\"") }
    }

    @Test
    fun escapedNewlineInsideQuotedArg() {
        registerTestCommand("echo")
        assertEquals(listOf("line1\nline2"), parseArgs("!echo \"line1\\nline2\""))
    }

    // ─── special characters in non-quoted arguments ───────────────────────────

    @Test
    fun exclamationMarkInsideArg() {
        registerTestCommand("echo")
        assertEquals(listOf("hello!"), parseArgs("!echo hello!"))
    }

    @Test
    fun hyphenInsideArg() {
        registerTestCommand("echo")
        assertEquals(listOf("hello-world"), parseArgs("!echo hello-world"))
    }

    @Test
    fun atSignInsideArg() {
        registerTestCommand("echo")
        assertEquals(listOf("@user"), parseArgs("!echo @user"))
    }

    @Test
    fun percentSignInsideArg() {
        registerTestCommand("echo")
        assertEquals(listOf("%sender"), parseArgs("!echo %sender"))
    }

    @Test
    fun dotInsideArg() {
        registerTestCommand("echo")
        assertEquals(listOf("hello.world"), parseArgs("!echo hello.world"))
    }

    @Test
    fun nonAsciiCharInsideArg() {
        registerTestCommand("echo")
        assertEquals(listOf("héllo"), parseArgs("!echo héllo"))
    }

    // ─── valid octal boundary cases ──────────────────────────────────────────

    @Test
    fun octalSingleDigitMin() {
        registerTestCommand("test")
        assertEquals("\u0000", parseArgs("!test \\0")[0])
    }

    @Test
    fun octalSingleDigitMax() {
        registerTestCommand("test")
        assertEquals("\u0007", parseArgs("!test \\7")[0])
    }

    @Test
    fun octalTwoDigitMax() {
        // \77 = 63 decimal
        registerTestCommand("test")
        assertEquals(63.toChar().toString(), parseArgs("!test \\77")[0])
    }

    @Test
    fun octalThreeDigitBoundaryLow() {
        // \100 = 64 decimal
        registerTestCommand("test")
        assertEquals(64.toChar().toString(), parseArgs("!test \\100")[0])
    }

    @Test
    fun octalThreeDigitBoundaryHigh() {
        // \377 = 255 decimal — maximum valid octal in the grammar
        registerTestCommand("test")
        assertEquals(255.toChar().toString(), parseArgs("!test \\377")[0])
    }

    @Test
    fun octalThreeDigitMid() {
        // \200 = 128 decimal
        registerTestCommand("test")
        assertEquals(128.toChar().toString(), parseArgs("!test \\200")[0])
    }

    @Test
    fun octalThreeHundredRange() {
        // \300 = 192 decimal
        registerTestCommand("test")
        assertEquals(192.toChar().toString(), parseArgs("!test \\300")[0])
    }

    @Test
    fun octalMixedWithOtherChars() {
        // \30 (= 24) followed by literal "8" — grammar stops octal at non-octal digit
        registerTestCommand("test")
        val args = parseArgs("!test \\308")
        assertEquals(24.toChar() + "8", args[0])
    }

    @Test
    fun octalTwoDigitFollowedByNonOctalDigit() {
        // \1 (= 1) followed by literal "8" — grammar stops at 8 since it's not [0-7]
        registerTestCommand("test")
        val args = parseArgs("!test \\18")
        assertEquals(1.toChar() + "8", args[0])
    }

    // ─── invalid octal: caught correctly by grammar ───────────────────────────

    @Test
    fun octalFourHundredIsInvalid() {
        registerTestCommand("test")
        assertFailsWith<InvalidEscapeSequenceException> { parse("!test \\400") }
    }

    @Test
    fun octalThreeSevenEightIsInvalid() {
        // \378 caught by InvalidOctalEscape: THREE SEVEN EightOrNine
        registerTestCommand("test")
        assertFailsWith<InvalidEscapeSequenceException> { parse("!test \\378") }
    }

    @Test
    fun octalThreeEightZeroIsInvalid() {
        // \380 caught by InvalidOctalEscape: THREE EightOrNine Number
        registerTestCommand("test")
        assertFailsWith<InvalidEscapeSequenceException> { parse("!test \\380") }
    }

    @Test
    fun singleDigit8IsInvalid() {
        // \8 caught by new InvalidOctalEscape: EightOrNine
        registerTestCommand("test")
        assertFailsWith<InvalidEscapeSequenceException> { parse("!test \\8") }
    }

    @Test
    fun singleDigit9IsInvalid() {
        // \9 caught by new InvalidOctalEscape: EightOrNine
        registerTestCommand("test")
        assertFailsWith<InvalidEscapeSequenceException> { parse("!test \\9") }
    }

    @Test
    fun octalFiveHundredRangeIsInvalid() {
        registerTestCommand("test")
        assertFailsWith<InvalidEscapeSequenceException> { parse("!test \\500") }
    }

    // ─── valid regular escapes ────────────────────────────────────────────────

    @Test
    fun singleQuoteEscape() {
        registerTestCommand("test")
        assertEquals("'", parseArgs("!test \\'")[0])
    }

    @Test
    fun carriageReturnEscape() {
        registerTestCommand("test")
        assertEquals("\r", parseArgs("!test \\r")[0])
    }

    @Test
    fun newlineEscape() {
        registerTestCommand("test")
        assertEquals("\n", parseArgs("!test \\n")[0])
    }

    @Test
    fun escapeFollowedByNonEscapeChar() {
        // \t is a tab; \tb = tab + 'b' (not an invalid sequence)
        registerTestCommand("test")
        assertEquals("\tb", parseArgs("!test \\tb")[0])
    }

    // ─── invalid regular escapes ──────────────────────────────────────────────

    @Test
    fun invalidEscapeLetter_e() {
        registerTestCommand("test")
        assertFailsWith<InvalidEscapeSequenceException> { parse("!test \\e") }
    }

    @Test
    fun invalidEscapeLetter_a() {
        registerTestCommand("test")
        assertFailsWith<InvalidEscapeSequenceException> { parse("!test \\a") }
    }

    @Test
    fun invalidEscapeSpace() {
        registerTestCommand("test")
        assertFailsWith<InvalidEscapeSequenceException> { parse("!test \\ ") }
    }

    @Test
    fun loneBackslashAtEndOfInput() {
        registerTestCommand("test")
        assertFailsWith<InvalidEscapeSequenceException> { parse("!test \\") }
    }

    // ─── invalid unicode escapes ──────────────────────────────────────────────

    @Test
    fun unicodeEscapeThreeDigitsIsInvalid() {
        registerTestCommand("test")
        assertFailsWith<InvalidEscapeSequenceException> { parse("!test \\u001") }
    }

    @Test
    fun unicodeEscapeWithNonHexCharIsInvalid() {
        registerTestCommand("test")
        assertFailsWith<InvalidEscapeSequenceException> { parse("!test \\u00GG") }
    }

    @Test
    fun unicodeEscapeEmptyIsInvalid() {
        registerTestCommand("test")
        assertFailsWith<InvalidEscapeSequenceException> { parse("!test \\u") }
    }

    // ─── valid unicode escapes ────────────────────────────────────────────────

    @Test
    fun unicodeEscapeUppercaseHex() {
        registerTestCommand("test")
        assertEquals('\uABCD'.toString(), parseArgs("!test \\uABCD")[0])
    }

    @Test
    fun unicodeEscapeZero() {
        registerTestCommand("test")
        assertEquals("\u0000", parseArgs("!test \\u0000")[0])
    }

    @Test
    fun unicodeEscapeMax() {
        registerTestCommand("test")
        assertEquals("\uFFFF", parseArgs("!test \\uFFFF")[0])
    }

    @Test
    fun unicodeEscapeMixedCase() {
        registerTestCommand("test")
        assertEquals('\u0aB3'.toString(), parseArgs("!test \\u0aB3")[0])
    }

    // ─── argument count and mixing ────────────────────────────────────────────

    @Test
    fun manyArgumentsParsedCorrectly() {
        registerTestCommand("test")
        assertEquals(
            listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"),
            parseArgs("!test a b c d e f g h i j")
        )
    }

    @Test
    fun mixedQuotedAndUnquotedArgs() {
        registerTestCommand("test")
        assertEquals(
            listOf("plain", "quoted arg", "another"),
            parseArgs("!test plain \"quoted arg\" another")
        )
    }

    @Test
    fun allWhitespaceBetweenArgsIsCollapsed() {
        registerTestCommand("test")
        assertEquals(listOf("a", "b", "c"), parseArgs("!test a  b\t\tc"))
    }

    // ─── command lookup priority ──────────────────────────────────────────────

    @Test
    fun chatAliasTakesPriorityOverProtocolCommand() {
        val chat = TestChat()
        val underlyingCmd = Command.of(UniversalProtocol, "ping", listOf(), ::doNothing, "test", "test")
        commands[UniversalProtocol] = mutableMapOf("ping" to underlyingCmd)

        // Alias "ping" has a preset arg — it should win over the bare protocol command.
        val alias = Alias(chat, "ping", underlyingCmd, listOf("aliasArg"))
        aliases[chat] = mutableMapOf("ping" to alias)

        val result = parseCommand("!ping", chat)
        assertEquals(listOf("aliasArg"), result?.args)
    }

    @Test
    fun unknownCommandThrowsCommandDoesNotExist() {
        val chat = TestChat()
        assertFailsWith<CommandDoesNotExist> { parseCommand("!nonexistent", chat) }
    }
}
