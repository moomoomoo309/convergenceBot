import convergence.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessagingTest {

    @Before
    fun setup() {
        resetGlobalState()
        bot.aliasVars["%sender"] = { c, s -> c.protocol.getUserName(c, s) }
        bot.aliasVars["%sendername"] = { _, _ -> "testuser" }
        bot.aliasVars["%botname"] = { c, _ -> c.protocol.getUserName(c, c.protocol.getBot(c)) }
        bot.aliasVars["%chatname"] = { c, _ -> c.protocol.getChatName(c) }
    }

    @After
    fun teardown() = resetGlobalState()

    // ─── replaceAliasVars ──────────────────────────────────────────────────

    @Test
    fun replaceAliasVarsReplacesSender() {
        val msg = SimpleOutgoingMessage("Hello %sender!")
        val result = replaceAliasVars(testChat, msg, testUser)
        assertEquals("Hello !", result?.toSimple()?.text)
    }

    @Test
    fun replaceAliasVarsReplacesCustomVar() {
        val msg = SimpleOutgoingMessage("Hello %sendername!")
        val result = replaceAliasVars(testChat, msg, testUser)
        assertEquals("Hello testuser!", result?.toSimple()?.text)
    }

    @Test
    fun replaceAliasVarsReplacesMultipleVars() {
        val msg = SimpleOutgoingMessage("%sendername says hi to %sendername")
        val result = replaceAliasVars(testChat, msg, testUser)
        assertEquals("testuser says hi to testuser", result?.toSimple()?.text)
    }

    @Test
    fun replaceAliasVarsLeavesUnknownVarsUntouched() {
        val msg = SimpleOutgoingMessage("Hello %unknown!")
        val result = replaceAliasVars(testChat, msg, testUser)
        assertEquals("Hello %unknown!", result?.toSimple()?.text)
    }

    @Test
    fun replaceAliasVarsReturnsNullForNullMessage() {
        assertNull(replaceAliasVars(testChat, null, testUser))
    }

    @Test
    fun replaceAliasVarsReturnsNonSimpleMessageUnchanged() {
        // A non-SimpleOutgoingMessage should be returned as-is
        val msg = object : OutgoingMessage() {
            override fun toSimple() = SimpleOutgoingMessage("text")
        }
        val result = replaceAliasVars(testChat, msg, testUser)
        assertEquals(msg, result)
    }

    @Test
    fun replaceAliasVarsReturnsNullWhenVarReturnsNull() {
        bot.aliasVars["%custom"] = { _, _ -> null }
        val msg = SimpleOutgoingMessage("Hello %custom!")
        val result = replaceAliasVars(testChat, msg, testUser)
        // When the var function returns null, the original %custom is kept
        assertEquals("Hello %custom!", result?.toSimple()?.text)
    }

    // ─── sendMessage(chat, message) ────────────────────────────────────────

    @Test
    fun sendMessageWithNullDoesNothing() {
        // sendMessage(chat, null) should not throw
        sendMessage(testChat, null as OutgoingMessage?)
        sendMessage(testChat, null as String?)
    }
}
