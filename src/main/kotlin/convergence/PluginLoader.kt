package convergence

import org.xeustechnologies.jcl.JarClassLoader
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.jvm.internal.Reflection
import kotlin.reflect.jvm.jvmName

interface Plugin {
    val name: String

    val baseInterface: BaseInterface
    fun init()
}

val jcl = JarClassLoader()

object PluginLoader {
    val plugins = mutableListOf<Plugin>()

    init {
        jcl.localLoader.order = 100
        jcl.addLoader(jcl.localLoader)
    }

    fun add(s: String) = add(Paths.get(s))
    fun add(p: Path) = add(p.toUri())
    fun add(u: URI) = add(u.toURL())
    fun add(u: URL) = jcl.add(u)
    fun load(): List<Plugin> {
        for (rawClassName in jcl.loadedResources.keys.filter { it.startsWith("convergence/") && it.endsWith("Main.class") }) {
            val className = rawClassName.substringBefore(".class").replace('/', '.')
            try {
                val pluginClass = Reflection.createKotlinClass(jcl.loadClass(className))
                val plugin = pluginClass.objectInstance as Plugin?
                if (plugin == null) {
                    logErr("Plugin with classname ${pluginClass.simpleName ?: pluginClass.jvmName} is not a singleton!")
                    continue
                }
                if (plugin.name == "DiscordPlugin")
                    continue
                // Don't register test plugins, since they won't actually be used outside of tests.
                if (plugin.baseInterface !is FakeBaseInterface)
                    if (!registerProtocol(plugin.baseInterface.protocol, plugin.baseInterface))
                        logErr("Tried to load duplicate plugin with name ${plugin.name}.")
                    else
                        plugins.add(plugin)
            } catch (e: ClassCastException) {
                logErr("Class \"$className\" in convergence package called \"Main.class\" but is not a plugin! Report this to the plugin author.")
            }
        }
        return plugins
    }
}
