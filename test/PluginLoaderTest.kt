import convergence.Plugin
import convergence.PluginLoader
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PluginLoaderTest {
    private var pluginList: List<Plugin> = emptyList()
    private var strOut: ByteArrayOutputStream? = null
    private var oldOut: PrintStream? = null
    private var printStreamOut: PrintStream? = null
    private fun loadPlugin() {
        val basicPluginPath = File("basicPlugin/build/libs").absoluteFile.toPath()
        assertTrue(Files.exists(basicPluginPath), "basicPluginDir is not a file or folder.")
        assertTrue(Files.isDirectory(basicPluginPath), "basicPluginDir is not a folder.")
        assertTrue(Files.list(basicPluginPath).count() > 0, "No files exist in basicPluginDir.")
        PluginLoader.add(basicPluginPath)
        pluginList = PluginLoader.load()
        assertNotNull(pluginList, "Could not load plugin!")
    }

    private fun pushOut() {
        strOut = strOut ?: ByteArrayOutputStream()
        printStreamOut = printStreamOut ?: PrintStream(strOut)
        System.setOut(printStreamOut)
        oldOut = System.out
    }

    private fun popOut(): String {
        assertNotNull(oldOut, "popOut called before pushOut was called at all!")
        assertEquals(printStreamOut, System.out, "popOut called twice before a pushOut.")
        System.out.close()
        System.setOut(oldOut)
        val outStr = strOut.toString()
        strOut?.reset()
        return outStr
    }

    @Test
    fun loadBasicPlugin() {
        loadPlugin()

        pushOut()
        val basicPlugin = pluginList.first { it.name == "basicPlugin" }
        assertNotNull(basicPlugin, "basicPlugin was not loaded. Loaded Plugins: $pluginList")
        basicPlugin.init()
        val pluginPrintOutput = popOut()

        assertEquals("basicPlugin init\n", pluginPrintOutput, "basicPlugin did not print expected output.")

    }
}
