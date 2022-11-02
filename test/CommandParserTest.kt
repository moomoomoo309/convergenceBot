package convergence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TestChat: Chat(TestProtocol(), "Test")
class TestProtocol: Protocol("Test")

fun doNothing(unused: List<String>, unused2: User): String? {
    unused.run { unused2.run {} }
    return null
}

class CommandParserTest {
    private fun loadCommandData(command: String): CommandData? {
        val testChat = TestChat()
        val testIndex = command.indexOf(" ")
        val testCommandStr = command.substring(1, if (testIndex == -1) command.length else testIndex)
        val testCommand = Command(testChat, testCommandStr, ::doNothing, "test", "test")
        commands[testChat] = mutableMapOf(testCommandStr to testCommand)
        return parseCommand(command, testChat)
    }

    @Test
    fun validEscapes() {
        val testCommandData = loadCommandData("!test \\f \\\\ \\u0014 \\tb \\b")
        assertEquals("test", testCommandData?.command?.name, "Did not load test command correctly.")
        assertEquals("\u000c", testCommandData?.args?.get(0), "Did not escape \\f properly.")
        assertEquals("\\", testCommandData?.args?.get(1), "Did not escape \\\\ properly.")
        assertEquals("\u0014", testCommandData?.args?.get(2), "Did not escape \\u0014 properly.")
        assertEquals("\tb", testCommandData?.args?.get(3), "Did not escape \\tb properly. (a \\t with a letter after it)")
        assertEquals("\b", testCommandData?.args?.get(4), "Did not escape \\b properly.")
    }

    @Test
    fun invalidEscape() {
        assertFailsWith<InvalidEscapeSequence>("Did not fail on empty escape.") {
            loadCommandData("!test2 \\")
        }
    }

    @Test
    fun invalidCommand() {
        assertFailsWith<InvalidCommandException>("Did not fail on invalid command.") {
            loadCommandData("!test2.5 abc\"def")
        }
    }

    @Test
    fun invalidUnicodeEscapes() {
        assertFailsWith<InvalidEscapeSequence>("Did not fail on empty unicode escape.") {
            loadCommandData("!test3 \\u")
        }
        assertFailsWith<InvalidEscapeSequence>("Did not fail on partial unicode escape.") {
            loadCommandData("!test4 \\u0")
        }
    }

    @Test
    fun validOctalEscapes() {
        val testCommandData = loadCommandData("!test5 h\\0 \\10 \\100")
        assertEquals("test5", testCommandData?.command?.name, "Did not load test5 command correctly.")
        assertEquals("h\u0000", testCommandData?.args?.get(0), "Did not escape \\0 properly.")
        assertEquals("\u0008", testCommandData?.args?.get(1), "Did not escape \\10 properly.")
        assertEquals("\u0040", testCommandData?.args?.get(2), "Did not escape \\100 properly.")
    }

    @Test
    fun invalidOctalEscape() {
        assertFailsWith<InvalidEscapeSequence>("Did not fail on out of range octal escape.") {
            loadCommandData("!test6 \\400")
        }
    }

    @Test
    fun notACommand() {
        val testCommandData = loadCommandData("Not a command")
        assertNull(testCommandData, "Tried to load a non-command as a command.")
    }


    private fun loadAliasData(command: String): CommandData? {
        val testChat = TestChat()
        val testIndex = command.indexOf(" ")
        val testAliasStr = command.substring(1, if (testIndex == -1) command.length else testIndex)
        val testCommand = Command(testChat, "test", ::doNothing, "test", "test")
        val testAlias = Alias(testChat, testAliasStr, testCommand, listOf("testArg1", "testArg2"), "testAlias", "testAlias")
        commands[testChat] = mutableMapOf("test" to testCommand)
        aliases[testChat] = mutableMapOf(testAliasStr to testAlias)
        return parseCommand(command, testChat)
    }

    @Test
    fun aliasExpansion() {
        val testCommandData = loadAliasData("!testAlias nonAliasArg1 nonAliasArg2")
        assertEquals("test", testCommandData?.command?.name, "Did not expand alias correctly.")
        assertEquals("testArg1", testCommandData?.args?.get(0), "Did not expand first alias argument correctly.")
        assertEquals("testArg2", testCommandData?.args?.get(1), "Did not expand second alias argument correctly.")
        assertEquals("nonAliasArg1", testCommandData?.args?.get(2), "Did not expand first non-alias argument correctly.")
        assertEquals("nonAliasArg2", testCommandData?.args?.get(3), "Did not expand second non-alias argument correctly.")
    }

    @Test
    fun validCommand() {
        val testCommandData = loadCommandData("!commands")
        assertEquals("commands", testCommandData?.command?.name, "Did not parse valid command correctly.")
    }

    @Test
    fun validCommandWithQuotes() {
        val testCommandData = loadCommandData("!schedule \"5 seconds\" \"!echo hi\"")
        assertEquals("schedule", testCommandData?.command?.name, "Did not parse valid command correctly.")
        assertEquals("5 seconds", testCommandData?.args?.get(0), "Did not parse first valid quoted argument correctly.")
        assertEquals("!echo hi", testCommandData?.args?.get(1), "Did not parse second valid quoted argument correctly.")
    }

    @Test
    fun doubleQuotedArguments() {
        val testCommandData = loadCommandData("!echo \"Hi mailman!\"")
        assertEquals("echo", testCommandData?.command?.name, "Did not parse valid command with double-quoted arguments correctly.")
        assertEquals("Hi mailman!", testCommandData?.args?.get(0), "Did not parse double-quoted arguments correctly.")
    }
}
