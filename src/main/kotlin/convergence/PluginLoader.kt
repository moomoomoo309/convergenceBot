package convergence

import org.pf4j.PluginWrapper

abstract class Plugin(wrapper: PluginWrapper): org.pf4j.Plugin(wrapper) {
    abstract val name: String

    abstract val baseInterface: BaseInterface

    val configuration: MutableMap<String, Any?> by lazy { (ClassLoader.getSystemClassLoader().loadClass("convergence.Configuration").kotlin.objectInstance!! as Configuration).conf }

    override fun start() {
        preinit()
        init()
    }

    /**
     * Preinit is primarily used to add to [moshiBuilder], but can be used for any initialization logic needed on
     * the main thread before the [init] function runs. [moshi] is not initialized in this function, use [init]
     * if you need it for any serialization or deserialization.
     */
    open fun preinit() {
    }

    /**
     * Initializes the plugin. [moshi] is built when this is run, so it can be used.
     */
    open fun init() {
    }
}
