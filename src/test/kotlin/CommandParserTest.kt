import convergence.*
import org.antlr.v4.runtime.InputMismatchException
import kotlin.test.*

class TestChat: Chat(UniversalProtocol, "Test") {
    override fun toKey() = "TestChat(Test)"
}

object TestProtocol: Protocol("Test") {
    override fun init() {
    }

    override fun configLoaded() {
        TODO("Not yet implemented")
    }

    override fun aliasCreated(alias: Alias) {
        TODO("Not yet implemented")
    }

    override fun sendMessage(chat: Chat, message: OutgoingMessage): Boolean {
        TODO("Not yet implemented")
    }

    override fun getBot(chat: Chat): User {
        TODO("Not yet implemented")
    }

    override fun getUserName(chat: Chat, user: User): String {
        TODO("Not yet implemented")
    }

    override fun getChats(): List<Chat> {
        TODO("Not yet implemented")
    }

    override fun getUsers(): List<User> {
        TODO("Not yet implemented")
    }

    override fun getUsers(chat: Chat): List<User> {
        TODO("Not yet implemented")
    }

    override fun getChatName(chat: Chat): String {
        TODO("Not yet implemented")
    }

    override fun commandScopeFromKey(key: String): Chat? {
        TODO("Not yet implemented")
    }

    override fun userFromKey(key: String): User? {
        TODO("Not yet implemented")
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
        assertFailsWith<InputMismatchException>("Did not fail on invalid command.") {
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
}
