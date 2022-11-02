package convergence

import org.pf4j.PluginWrapper

private fun loadClass(className: String) = ClassLoader.getSystemClassLoader().loadClass(className)

abstract class Plugin(wrapper: PluginWrapper): org.pf4j.Plugin(wrapper) {
    abstract val name: String

    abstract val baseInterface: BaseInterface

    val sharedVariables: SharedVariables by lazy {
        loadClass("convergence.sharedVariables").kotlin.objectInstance!! as SharedVariables
    }
    val settings: Settings by lazy {
        loadClass("convergence.settings").kotlin.objectInstance!! as Settings
    }

    override fun start() {
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
