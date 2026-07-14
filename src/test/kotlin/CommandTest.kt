import convergence.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun makeCmd(
    name: String,
    permissions: CommandFunction = { _, _, _ -> null },
    protocol: Protocol = TestProtocol,
    function: CommandFunction
) = Command(protocol, name, listOf(), function, "help", "syntax", permissions)

class CommandTest {

    @Before
    fun setup() {
        aliases.clear()
        aliasVars.clear()
        aliasVars["%sender"] = { c, s -> c.protocol.getUserName(c, s) }
        aliasVars["%sendername"] = { _, _ -> "testuser" }
    }

    @After
    fun teardown() {
        aliases.clear()
        aliasVars.clear()
    }

    // ─── Command.invoke: permissions ────────────────────────────────────────

    @Test
    fun invokeReturnsErrorMessageWhenPermissionDenied() {
        val permFunc: CommandFunction = { _, _, _ -> SimpleOutgoingMessage("Not allowed!") }
        val cmd = makeCmd("restricted", function = { _, _, _ -> SimpleOutgoingMessage("ok") }, permissions = permFunc)
        val result = cmd.invoke(listOf(), testChat, testUser)
        assertEquals("Not allowed!", result?.toSimple()?.text)
    }

    @Test
    fun invokeProceedsWhenPermissionReturnsNull() {
        val permFunc: CommandFunction = { _, _, _ -> null }
        val cmd = makeCmd("allowed", function = { _, _, _ -> SimpleOutgoingMessage("success") }, permissions = permFunc)
        val result = cmd.invoke(listOf(), testChat, testUser)
        assertEquals("success", result?.toSimple()?.text)
    }

    @Test
    fun invokePassesArgsToFunction() {
        var receivedArgs: List<String> = emptyList()
        val cmd = makeCmd("echo") { args, _, _ ->
            receivedArgs = args
            SimpleOutgoingMessage("ok")
        }
        cmd.invoke(listOf("hello", "world"), testChat, testUser)
        assertEquals(listOf("hello", "world"), receivedArgs)
    }

    @Test
    fun invokePassesChatToFunction() {
        var receivedChat: Chat? = null
        val cmd = makeCmd("test") { _, chat, _ ->
            receivedChat = chat
            SimpleOutgoingMessage("ok")
        }
        cmd.invoke(listOf(), testChat, testUser)
        assertEquals(testChat, receivedChat)
    }

    @Test
    fun invokePassesSenderToFunction() {
        var receivedSender: User? = null
        val cmd = makeCmd("test") { _, _, sender ->
            receivedSender = sender
            SimpleOutgoingMessage("ok")
        }
        cmd.invoke(listOf(), testChat, testUser)
        assertEquals(testUser, receivedSender)
    }

    // ─── Command.invoke: alias var replacement ──────────────────────────────

    @Test
    fun invokeReplacesAliasVarsInOutput() {
        val func: () -> String? = { "Hello %sendername!" }
        val cmd = Command.of(TestProtocol, "greet", listOf(), func, "help", "syntax")
        val result = cmd.invoke(listOf(), testChat, testUser)
        assertEquals("Hello testuser!", result?.toSimple()?.text)
    }

    // ─── Command.of factory methods ─────────────────────────────────────────

    @Test
    fun ofNoArgFunctionCreatesWorkingCommand() {
        val func: () -> String? = { "done" }
        val cmd = Command.of(TestProtocol, "noop", listOf(), func, "help", "syntax")
        assertEquals("done", cmd.invoke(listOf(), testChat, testUser)?.toSimple()?.text)
    }

    @Test
    fun ofListArgFunctionCreatesWorkingCommand() {
        val func: (List<String>) -> String? = { args -> args.joinToString(" ") }
        val cmd = Command.of(TestProtocol, "echo", listOf(), func, "help", "syntax")
        assertEquals("a b", cmd.invoke(listOf("a", "b"), testChat, testUser)?.toSimple()?.text)
    }

    @Test
    fun ofListChatFunctionCreatesWorkingCommand() {
        val func: (List<String>, Chat) -> String? = { _, chat -> chat.name }
        val cmd = Command.of(TestProtocol, "name", listOf(), func, "help", "syntax")
        assertEquals("Test", cmd.invoke(listOf(), testChat, testUser)?.toSimple()?.text)
    }

    @Test
    fun ofListChatSenderFunctionCreatesWorkingCommand() {
        val func: (List<String>, Chat, User) -> String? = { _, _, _ -> "sender-invoked" }
        val cmd = Command.of(TestProtocol, "who", listOf(), func, "help", "syntax")
        val result = cmd.invoke(listOf(), testChat, testUser)
        assertEquals("sender-invoked", result?.toSimple()?.text)
    }

    @Test
    fun ofReturnsNullWhenFunctionReturnsNull() {
        val func: () -> String? = { null }
        val cmd = Command.of(TestProtocol, "nothing", listOf(), func, "help", "syntax")
        assertNull(cmd.invoke(listOf(), testChat, testUser))
    }

    // ─── Alias.commandText ──────────────────────────────────────────────────

    @Test
    fun aliasCommandTextJoinsNameAndArgs() {
        val cmd = Command.of(TestProtocol, "echo", listOf(), ::echo, "help", "syntax")
        val alias = Alias(testChat, "greet", cmd, listOf("hello", "world"))
        assertEquals("echo hello world", alias.commandText())
    }

    @Test
    fun aliasCommandTextWithEmptyArgs() {
        val cmd = Command.of(TestProtocol, "ping", listOf(), ::ping, "help", "syntax")
        val alias = Alias(testChat, "p", cmd, listOf())
        assertEquals("ping ", alias.commandText())
    }

    // ─── CommandLike ordering ───────────────────────────────────────────────

    @Test
    fun commandCanBeCompared() {
        val cmd1 = Command.of(TestProtocol, "aaa", listOf(), ::ping, "", "")
        val cmd2 = Command.of(TestProtocol, "zzz", listOf(), ::ping, "", "")
        assertTrue(cmd1 < cmd2)
    }

    @Test
    fun commandCanBeComparedAcrossProtocols() {
        val cmd1 = Command.of(UniversalProtocol, "aaa", listOf(), ::ping, "", "")
        val cmd2 = Command.of(TestProtocol, "aaa", listOf(), ::ping, "", "")
        assertTrue(cmd1.compareTo(cmd2) != 0)
    }
}
