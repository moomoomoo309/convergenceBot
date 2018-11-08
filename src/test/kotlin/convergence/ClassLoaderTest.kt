package convergence

import org.junit.Test
import org.xeustechnologies.jcl.JarClassLoader
import org.xeustechnologies.jcl.JclObjectFactory
import org.xeustechnologies.jcl.JclUtils

class ClassLoaderTest {
    @Test
    fun loadBasicPlugin() {
        val jcl = JarClassLoader()
        jcl.add("/home/nicholasdelello/IntelliJIDEAProjects/convergenceBot/basicPlugin/build/libs/")
        println(jcl.loadedResources)
        val factory = JclObjectFactory.getInstance()
        val basicPlugin = factory.create(jcl, "convergence.testPlugins.basicPlugin.Main")
        JclUtils.cast(basicPlugin, plugin::class.java).init()
    }
}
