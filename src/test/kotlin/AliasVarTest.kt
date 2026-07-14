import convergence.SimpleOutgoingMessage
import convergence.aliasVars
import convergence.replaceAliasVars
import kotlin.test.Test
import kotlin.test.assertEquals

class AliasVarTest {
    @Test
    fun aliasVarReplacement() {
        val testCommand = SimpleOutgoingMessage("!echo %sendername")
        aliasVars.clear()
        aliasVars["%sender"] = { _, _ -> "ligma" }
        aliasVars["%sendername"] = { _, _ -> "chokoma" }
        val result = replaceAliasVars(testChat, testCommand, testUser)?.toSimple()?.text
        assertEquals("!echo chokoma", result)
    }
}
