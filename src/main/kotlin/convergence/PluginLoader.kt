package convergence

import org.xeustechnologies.jcl.JarClassLoader
import org.xeustechnologies.jcl.JclObjectFactory
import org.xeustechnologies.jcl.JclUtils
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

interface Plugin {
    val name: String

    val baseInterface: BaseInterface
    fun init()
}

object PluginLoader {
    private val jcl = JarClassLoader()
    private val factory = JclObjectFactory.getInstance()
    private const val classNameFile = "MainClass.txt"

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
        var mainClassName: String
        val pluginList: ArrayList<Plugin> = ArrayList()
        for (entry in jcl.loadedResources)
            if (entry.key.endsWith(classNameFile)) {
                mainClassName = String(entry.value).trim()
                val plugin = JclUtils.cast(factory.create(jcl, mainClassName), Plugin::class.java)
                // Don't register test plugins, since they won't actually be used outside of tests.
                if (plugin.baseInterface !is FakeBaseInterface)
                    registerProtocol(plugin.baseInterface.protocol)
                pluginList.add(plugin)
            }
        return pluginList
    }
}
