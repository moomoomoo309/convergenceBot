import convergence.classLoader
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ClassLoaderTest {
    @Test
    fun loadBasicPlugin() {
        val pluginVal = classLoader.loadClass("/home/nicholasdelello/IntelliJIDEAProjects/convergenceBot/basicPlugin/build/libs/")
        assertNotNull(pluginVal, "Could not load plugin!")
        val strOut = ByteArrayOutputStream()
        val oldOut = System.out
        System.setOut(PrintStream(strOut))
        pluginVal.init()
        System.out.close()
        System.setOut(oldOut)
        assertEquals("basicPlugin init\n", strOut.toString(), "basicPlugin did not print expected output.")
    }
}
