import convergence.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessagingTest : KoinComponent {
    private val messaging: MessagingService by inject()

    @Before
    fun setup() {
        ensureKoinStarted()
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
        val result = messaging.replaceAliasVars(testChat, msg, testUser)
        assertEquals("Hello !", result?.toSimple()?.text)
    }

    @Test
    fun replaceAliasVarsReplacesCustomVar() {
        val msg = SimpleOutgoingMessage("Hello %sendername!")
        val result = messaging.replaceAliasVars(testChat, msg, testUser)
        assertEquals("Hello testuser!", result?.toSimple()?.text)
    }

    @Test
    fun replaceAliasVarsReplacesMultipleVars() {
        val msg = SimpleOutgoingMessage("%sendername says hi to %sendername")
        val result = messaging.replaceAliasVars(testChat, msg, testUser)
        assertEquals("testuser says hi to testuser", result?.toSimple()?.text)
    }

    @Test
    fun replaceAliasVarsLeavesUnknownVarsUntouched() {
        val msg = SimpleOutgoingMessage("Hello %unknown!")
        val result = messaging.replaceAliasVars(testChat, msg, testUser)
        assertEquals("Hello %unknown!", result?.toSimple()?.text)
    }

    @Test
    fun replaceAliasVarsReturnsNullForNullMessage() {
        assertNull(messaging.replaceAliasVars(testChat, null, testUser))
    }

    @Test
    fun replaceAliasVarsReturnsNonSimpleMessageUnchanged() {
        val msg = object : OutgoingMessage() {
            override fun toSimple() = SimpleOutgoingMessage("text")
        }
        val result = messaging.replaceAliasVars(testChat, msg, testUser)
        assertEquals(msg, result)
    }

    @Test
    fun replaceAliasVarsReturnsNullWhenVarReturnsNull() {
        bot.aliasVars["%custom"] = { _, _ -> null }
        val msg = SimpleOutgoingMessage("Hello %custom!")
        val result = messaging.replaceAliasVars(testChat, msg, testUser)
        assertEquals("Hello %custom!", result?.toSimple()?.text)
    }

    // ─── sendMessage(chat, message) ────────────────────────────────────────

    @Test
    fun sendMessageWithNullDoesNothing() {
        messaging.sendMessage(testChat, null as OutgoingMessage?)
        messaging.sendMessage(testChat, null as String?)
    }
}
