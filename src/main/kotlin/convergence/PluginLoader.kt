package convergence

import org.xeustechnologies.jcl.JarClassLoader
import java.lang.reflect.AccessibleObject
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.NoSuchElementException

interface Plugin {
    val name: String

    val baseInterface: BaseInterface
    fun init()
}

val jcl = JarClassLoader()
object PluginLoader {
    init {
        jcl.localLoader.order = 100
        jcl.addLoader(jcl.localLoader)
    }

    private fun nullIfException(result: List<Plugin>): List<Plugin> = try {
        result
    } catch (e: Exception) {
        emptyList()
    }

    fun load(s: String): List<Plugin> = nullIfException(load(Paths.get(s)))
    fun load(p: Path): List<Plugin> = nullIfException(load(p.toUri()))
    fun load(u: URI): List<Plugin> = nullIfException(load(u.toURL()))
    fun load(u: URL): List<Plugin> {
        if (u.file.toLowerCase().startsWith("discordplugin"))
            return emptyList()
        jcl.add(u)
        val pluginList: ArrayList<Plugin> = ArrayList()
        for (rawClassName in jcl.loadedResources.keys.filter { it.startsWith("convergence/") && it.endsWith("Main.class") }) {
            val className = rawClassName.substringBefore(".class").replace('/', '.')
            try {
                val pluginClass = jcl.loadClass(className)
                val constructors = pluginClass.declaredConstructors
                AccessibleObject.setAccessible(constructors, true)
                val plugin = try {
                    constructors.first { it.parameterCount == 0 }.newInstance()
                } catch (e: NoSuchElementException) {
                    logErr("No default constructor found for class \"$className\".")
                    continue
                } as Plugin
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
