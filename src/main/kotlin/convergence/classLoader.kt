package convergence

import org.xeustechnologies.jcl.JarClassLoader
import org.xeustechnologies.jcl.JclObjectFactory
import org.xeustechnologies.jcl.JclUtils
import org.xeustechnologies.jcl.exception.JclException
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

interface Plugin {
    val name: String

    val baseInterface: BaseInterface
    fun init()
}

object classLoader {
    private val jcl = JarClassLoader()
    private val factory = JclObjectFactory.getInstance()
    private val classNameFile = "MainClass.txt"

    private fun nullIfException(result: Plugin?): Plugin? = try {
        result
    } catch (e: IOException) {
        null
    }

    fun loadClass(s: String): Plugin? = nullIfException(loadClass(Paths.get(s)))
    fun loadClass(p: Path): Plugin? = nullIfException(loadClass(p.toUri().toURL()))
    fun loadClass(u: URL): Plugin? {
        jcl.add(u)
        var mainClassName: String? = null
        for (entry in jcl.loadedResources)
            if (entry.key.endsWith(classNameFile)) {
                mainClassName = String(entry.value).trim()
                break
            }
        return try {
            JclUtils.cast(factory.create(jcl, mainClassName), Plugin::class.java)
        } catch (e: JclException) {
            null
        }
    }
}
