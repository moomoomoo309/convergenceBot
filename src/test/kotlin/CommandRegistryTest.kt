import convergence.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandRegistryTest : KoinComponent {
    private val commandRegistry: CommandRegistryService by inject()
    private val commandParser: CommandParserService by inject()

    @Before
    fun setup() {
        ensureKoinStarted()
        resetGlobalState()
    }

    @After
    fun teardown() = resetGlobalState()

    // ─── registerCommand ────────────────────────────────────────────────────

    @Test
    fun registerCommandAddsToRegistry() {
        val cmd = Command.of(TestProtocol, "test", listOf(), { -> null }, "help", "syntax")
        assertTrue(commandRegistry.registerCommand(cmd))
        assertTrue("test" in bot.commands[TestProtocol]!!)
    }

    @Test
    fun registerCommandReturnsFalseForDuplicate() {
        val cmd = Command.of(TestProtocol, "test", listOf(), { -> null }, "help", "syntax")
        assertTrue(commandRegistry.registerCommand(cmd))
        assertFalse(commandRegistry.registerCommand(cmd))
    }

    @Test
    fun registerCommandNormalizesNameToLowercase() {
        val cmd = Command.of(TestProtocol, "MyCommand", listOf(), { -> null }, "help", "syntax")
        assertTrue(commandRegistry.registerCommand(cmd))
        assertTrue("mycommand" in bot.commands[TestProtocol]!!)
    }

    @Test
    fun registerCommandDuplicateIsCaseInsensitive() {
        val cmd1 = Command.of(TestProtocol, "Echo", listOf(), { -> null }, "help", "syntax")
        val cmd2 = Command.of(TestProtocol, "echo", listOf(), { -> null }, "help", "syntax")
        assertTrue(commandRegistry.registerCommand(cmd1))
        assertFalse(commandRegistry.registerCommand(cmd2))
    }

    @Test
    fun registerCommandCreatesProtocolEntryIfNeeded() {
        assertFalse(TestProtocol in bot.commands)
        val cmd = Command.of(TestProtocol, "test", listOf(), { -> null }, "help", "syntax")
        commandRegistry.registerCommand(cmd)
        assertTrue(TestProtocol in bot.commands)
    }

    @Test
    fun registerCommandDifferentProtocolsCanHaveSameName() {
        val cmd1 = Command.of(TestProtocol, "shared", listOf(), { -> null }, "help", "syntax")
        val cmd2 = Command.of(UniversalProtocol, "shared", listOf(), { -> null }, "help", "syntax")
        assertTrue(commandRegistry.registerCommand(cmd1))
        assertTrue(commandRegistry.registerCommand(cmd2))
    }

    // ─── registerAlias ──────────────────────────────────────────────────────

    @Test
    fun registerAliasAddsToRegistry() {
        val cmd = Command.of(TestProtocol, "echo", listOf(), ::echo, "help", "syntax")
        val alias = Alias(testChat, "greet", cmd, listOf("hello"))
        assertTrue(commandRegistry.registerAlias(alias))
        assertTrue("greet" in settings.aliases[testChat]!!)
    }

    @Test
    fun registerAliasReturnsFalseForDuplicate() {
        val cmd = Command.of(TestProtocol, "echo", listOf(), ::echo, "help", "syntax")
        val alias = Alias(testChat, "greet", cmd, listOf("hello"))
        assertTrue(commandRegistry.registerAlias(alias))
        assertFalse(commandRegistry.registerAlias(alias))
    }

    @Test
    fun registerAliasNormalizesNameToLowercase() {
        val cmd = Command.of(TestProtocol, "echo", listOf(), ::echo, "help", "syntax")
        val alias = Alias(testChat, "Greet", cmd, listOf("hello"))
        assertTrue(commandRegistry.registerAlias(alias))
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
        assertTrue(commandRegistry.registerAlias(alias1))
        assertTrue(commandRegistry.registerAlias(alias2))
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
        val result = commandParser.parseSafe(testChat, "!nonexistent", testUser, commandRegistry::getCommand)
        assertNull(result)
    }

    @Test
    fun parseCommandWrapperReturnsNullForInvalidEscape() {
        val result = commandParser.parseSafe(testChat, "!echo \\", testUser, commandRegistry::getCommand)
        assertNull(result)
    }
}
