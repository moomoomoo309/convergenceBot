import convergence.SimpleOutgoingMessage
import convergence.bot
import convergence.replaceAliasVars
import kotlin.test.Test
import kotlin.test.assertEquals

class AliasVarTest {
    @Test
    fun aliasVarReplacement() {
        val testCommand = SimpleOutgoingMessage("!echo %sendername")
        bot.aliasVars.clear()
        bot.aliasVars["%sender"] = { _, _ -> "ligma" }
        bot.aliasVars["%sendername"] = { _, _ -> "chokoma" }
        val result = replaceAliasVars(testChat, testCommand, testUser)?.toSimple()?.text
        assertEquals("!echo chokoma", result)
    }
}
