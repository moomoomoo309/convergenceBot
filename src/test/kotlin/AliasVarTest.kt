import convergence.SimpleOutgoingMessage
import convergence.aliasVars
import convergence.replaceAliasVars
import kotlin.test.Test

class AliasVarTest {
    @Test
    fun aliasVarTest() {
        val testCommand = SimpleOutgoingMessage("!echo %sendername")
        aliasVars.clear()
        aliasVars["%sender"] = { _, _ -> "ligma" }
        aliasVars["%sendername"] = { _, _ -> "chokoma" }
        println(replaceAliasVars(testChat, testCommand, testUser)?.toSimple()?.text)
    }
}
