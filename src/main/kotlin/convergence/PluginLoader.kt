package convergence

import org.xeustechnologies.jcl.JarClassLoader
import org.xeustechnologies.jcl.JclObjectFactory
import org.xeustechnologies.jcl.JclUtils
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

interface Plugin {
    val name: String

    val baseInterface: BaseInterface
    fun init()
}

private val jcl = JarClassLoader()
object PluginLoader {
    init {
        jcl.localLoader.order = 100
        jcl.addLoader(jcl.localLoader)
    }

    private val factory = JclObjectFactory.getInstance()

    private fun nullIfException(result: List<Plugin>): List<Plugin> = try {
        result
    } catch (e: Exception) {
        emptyList()
    }

    fun loadPlugin(s: String): List<Plugin> = nullIfException(loadPlugin(Paths.get(s)))
    fun loadPlugin(p: Path): List<Plugin> = nullIfException(loadPlugin(p.toUri()))
    fun loadPlugin(u: URI): List<Plugin> = nullIfException(loadPlugin(u.toURL()))
    fun loadPlugin(u: URL): List<Plugin> {
        jcl.add(u)
        val pluginList: ArrayList<Plugin> = ArrayList()
        for ((className, _) in jcl.loadedResources.filterKeys { it.startsWith("convergence/") && it.endsWith("Main.class") }) {
            try {
                val plugin = JclUtils.deepClone(factory.create(jcl, className.substringBefore(".class").replace('/', '.'))) as Plugin
                // Don't register test plugins, since they won't actually be used outside of tests.
                if (plugin.baseInterface !is FakeBaseInterface)
                    registerProtocol(plugin.baseInterface.protocol, plugin.baseInterface)
                pluginList.add(plugin)
            } catch (e: ClassCastException) {
                logErr("Class \"$className\" in convergence package called \"Main.class\" but is not a plugin! Report this to the plugin author.")
            }
        }
        return pluginList
    }
}
