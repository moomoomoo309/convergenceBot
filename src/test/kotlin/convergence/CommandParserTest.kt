package convergence

import org.junit.Test
import java.util.function.Consumer
import kotlin.test.assert
import kotlin.test.assertEquals

class TestChat : Chat(TestProtocol())
class TestProtocol : Protocol("Test")

class CommandParserTest {
    private fun loadCommandData(command: String): CommandData? {
        val testChat = TestChat()
        val testIndex = command.indexOf(" ")
        val testCommandStr = command.substring(1, if (testIndex == -1) command.length else testIndex)
        val testCommand = Command(testCommandStr, Consumer {}, "test", "test")
        universalCommands[testChat] = mutableMapOf(testCommandStr to testCommand)
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
        try {
            loadCommandData("!test2 \\")
            assert(false) { "Did not fail on empty escape." }
        } catch (e: InvalidEscapeSequence) {
        }
    }

    @Test
    fun invalidUnicodeEscapes() {
        try {
            loadCommandData("!test3 \\u")
            assert(false) { "Did not fail on empty unicode escape." }
        } catch (e: InvalidEscapeSequence) {
        }
        try {
            loadCommandData("!test4 \\u0")
            assert(false) { "Did not fail on partial unicode escape." }
        } catch (e: InvalidEscapeSequence) {
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
        try {
            loadCommandData("!test6 \\400")
            assert(false) { "Did not fail on out of range octal escape." }
        } catch (e: InvalidEscapeSequence) {
        }
    }
}
