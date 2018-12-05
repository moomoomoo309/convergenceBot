import convergence.Plugin
import convergence.PluginLoader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PluginLoaderTest {
    var pluginList: List<Plugin> = emptyList()
    private var strOut: ByteArrayOutputStream? = null
    private var oldOut: PrintStream? = null
    private var printStreamOut: PrintStream? = null
    private fun loadPlugin() {
        val basicPluginPath = File("basicPlugin/build/libs").absoluteFile.toPath()
        assertTrue(Files.exists(basicPluginPath), "basicPluginDir is not a file or folder.")
        assertTrue(Files.isDirectory(basicPluginPath), "basicPluginDir is not a folder.")
        assertTrue(Files.list(basicPluginPath).count() > 0, "No files exist in basicPluginDir.")
        pluginList = PluginLoader.loadPlugin(basicPluginPath)
        assertNotNull(pluginList, "Could not load plugin!")
    }

    private fun pushOut() {
        strOut = strOut ?: ByteArrayOutputStream()
        printStreamOut = printStreamOut ?: PrintStream(strOut)
        System.setOut(printStreamOut)
        oldOut = System.out
    }

    private fun popOut(): String {
        assertNotNull(oldOut, "popOut called without pushOut!")
        assertEquals(printStreamOut, System.out, "popOut called without pushOut following a previous correct usage of push and popOut.")
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
        try {
            pluginList.first { it.name == "basicPlugin" }.init()
        } catch (e: NoSuchElementException) {
            assertTrue(false, "basicPlugin was not loaded. Loaded Plugins: $pluginList")
        }
        val pluginPrintVal = popOut()

        assertEquals("basicPlugin init\n", pluginPrintVal, "basicPlugin did not print expected output.")
    }
}
