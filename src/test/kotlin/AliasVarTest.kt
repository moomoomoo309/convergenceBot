import convergence.*
import org.junit.Before
import org.junit.Test
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.test.assertEquals

class AliasVarTest : KoinComponent {
    private val messaging: MessagingService by inject()

    @Before
    fun setup() {
        ensureKoinStarted()
    }

    @Test
    fun aliasVarReplacement() {
        val testCommand = SimpleOutgoingMessage("!echo %sendername")
        bot.aliasVars.clear()
        bot.aliasVars["%sender"] = { _, _ -> "ligma" }
        bot.aliasVars["%sendername"] = { _, _ -> "chokoma" }
        val result = messaging.replaceAliasVars(testChat, testCommand, testUser)?.toSimple()?.text
        assertEquals("!echo chokoma", result)
    }
}
