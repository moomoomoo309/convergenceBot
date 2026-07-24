import convergence.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandRegistryTest {

    @Before
    fun setup() = resetGlobalState()

    @After
    fun teardown() = resetGlobalState()

    // ─── registerCommand ────────────────────────────────────────────────────

    @Test
    fun registerCommandAddsToRegistry() {
        val cmd = Command.of(TestProtocol, "test", listOf(), { -> null }, "help", "syntax")
        assertTrue(registerCommand(cmd))
        assertTrue("test" in bot.commands[TestProtocol]!!)
    }

    @Test
    fun registerCommandReturnsFalseForDuplicate() {
        val cmd = Command.of(TestProtocol, "test", listOf(), { -> null }, "help", "syntax")
        assertTrue(registerCommand(cmd))
        assertFalse(registerCommand(cmd))
    }

    @Test
    fun registerCommandNormalizesNameToLowercase() {
        val cmd = Command.of(TestProtocol, "MyCommand", listOf(), { -> null }, "help", "syntax")
        assertTrue(registerCommand(cmd))
        assertTrue("mycommand" in bot.commands[TestProtocol]!!)
    }

    @Test
    fun registerCommandDuplicateIsCaseInsensitive() {
        val cmd1 = Command.of(TestProtocol, "Echo", listOf(), { -> null }, "help", "syntax")
        val cmd2 = Command.of(TestProtocol, "echo", listOf(), { -> null }, "help", "syntax")
        assertTrue(registerCommand(cmd1))
        assertFalse(registerCommand(cmd2))
    }

    @Test
    fun registerCommandCreatesProtocolEntryIfNeeded() {
        assertFalse(TestProtocol in bot.commands)
        val cmd = Command.of(TestProtocol, "test", listOf(), { -> null }, "help", "syntax")
        registerCommand(cmd)
        assertTrue(TestProtocol in bot.commands)
    }

    @Test
    fun registerCommandDifferentProtocolsCanHaveSameName() {
        val cmd1 = Command.of(TestProtocol, "shared", listOf(), { -> null }, "help", "syntax")
        val cmd2 = Command.of(UniversalProtocol, "shared", listOf(), { -> null }, "help", "syntax")
        assertTrue(registerCommand(cmd1))
        assertTrue(registerCommand(cmd2))
    }

    // ─── registerAlias ──────────────────────────────────────────────────────

    @Test
    fun registerAliasAddsToRegistry() {
        val cmd = Command.of(TestProtocol, "echo", listOf(), ::echo, "help", "syntax")
        val alias = Alias(testChat, "greet", cmd, listOf("hello"))
        assertTrue(registerAlias(alias))
        assertTrue("greet" in settings.aliases[testChat]!!)
    }

    @Test
    fun registerAliasReturnsFalseForDuplicate() {
        val cmd = Command.of(TestProtocol, "echo", listOf(), ::echo, "help", "syntax")
        val alias = Alias(testChat, "greet", cmd, listOf("hello"))
        assertTrue(registerAlias(alias))
        assertFalse(registerAlias(alias))
    }

    @Test
    fun registerAliasNormalizesNameToLowercase() {
        val cmd = Command.of(TestProtocol, "echo", listOf(), ::echo, "help", "syntax")
        val alias = Alias(testChat, "Greet", cmd, listOf("hello"))
        assertTrue(registerAlias(alias))
        assertTrue("greet" in settings.aliases[testChat]!!)
    }

    @Test
    fun registerAliasDifferentScopesCanHaveSameName() {
        val cmd = Command.of(UniversalProtocol, "echo", listOf(), ::echo, "help", "syntax")
        val otherChat = object : Chat(UniversalProtocol, "Other") {
            override fun toKey() = "UniversalChat(Other)"
        }
        val alias1 = Alias(testChat, "greet", cmd, listOf("hello"))
        val alias2 = Alias(otherChat, "greet", cmd, listOf("world"))
        assertTrue(registerAlias(alias1))
        assertTrue(registerAlias(alias2))
    }

    // ─── getStackTraceText ──────────────────────────────────────────────────

    @Test
    fun getStackTraceTextReturnsNonEmptyString() {
        val text = getStackTraceText(Exception("test error"))
        assertTrue(text.contains("test error"))
    }

    // ─── parseCommand (error handling wrapper) ──────────────────────────────

    @Test
    fun parseCommandWrapperReturnsNullForInvalidCommand() {
        val result = parseCommand(testChat, "!nonexistent", testUser)
        assertNull(result)
    }

    @Test
    fun parseCommandWrapperReturnsNullForInvalidEscape() {
        val result = parseCommand(testChat, "!echo \\", testUser)
        assertNull(result)
    }
}
