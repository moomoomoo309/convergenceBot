package convergence

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TestChat: Chat(TestProtocol())
class TestProtocol: Protocol("Test")

fun doNothing(unused: Chat, unused2: List<String>, unused3: User): String? {
    return null
}

class CommandParserTest {
    private fun loadCommandData(command: String): CommandData? {
        val testChat = TestChat()
        val testIndex = command.indexOf(" ")
        val testCommandStr = command.substring(1, if (testIndex == -1) command.length else testIndex)
        val testCommand = Command(testCommandStr, ::doNothing, "test", "test")
        commands[testChat] = mutableMapOf(testCommandStr to testCommand)
        return parseCommand(command, "!", testChat)
    }

    @Test
    fun validEscapes() {
        val testCommandData = loadCommandData("!test \\f \\\\ \\u0014 \\b")
        assertEquals("test", testCommandData?.command?.name, "Did not load test command correctly.")
        assertEquals("\u000c", testCommandData?.args?.get(0), "Did not escape \\f properly.")
        assertEquals("\\", testCommandData?.args?.get(1), "Did not escape \\\\ properly.")
        assertEquals("\u0014", testCommandData?.args?.get(2), "Did not escape \\u0014 properly.")
        assertEquals("\b", testCommandData?.args?.get(3), "Did not escape \\b properly.")
    }

    @Test
    fun invalidEscape() {
        assertFailsWith<InvalidEscapeSequence>("Did not fail on empty escape.") {
            loadCommandData("!test2 \\")
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
        val testCommandData = loadCommandData("!test5 \\0 \\10 \\100")
        assertEquals("test5", testCommandData?.command?.name, "Did not load test5 command correctly.")
        assertEquals("\u0000", testCommandData?.args?.get(0), "Did not escape \\0 properly.")
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
        val testCommand = Command("test", ::doNothing, "test", "test")
        val testAlias = Alias(testAliasStr, testCommand, listOf("testArg1", "testArg2"), "testAlias", "testAlias")
        commands[testChat] = mutableMapOf("test" to testCommand)
        aliases[testChat] = mutableMapOf(testAliasStr to testAlias)
        return parseCommand(command, "!", testChat)
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
}
