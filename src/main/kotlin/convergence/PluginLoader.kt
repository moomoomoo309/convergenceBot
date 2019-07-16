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

    fun load(s: String): List<Plugin> = nullIfException(load(Paths.get(s)))
    fun load(p: Path): List<Plugin> = nullIfException(load(p.toUri()))
    fun load(u: URI): List<Plugin> = nullIfException(load(u.toURL()))
    fun load(u: URL): List<Plugin> {
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
