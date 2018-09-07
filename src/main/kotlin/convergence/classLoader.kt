package convergence


import java.net.URL
import java.net.URLClassLoader
import java.io.File




class classLoader(urls: Array<URL>) : URLClassLoader(urls) {

    val convergencePackageDir = File(this::class.java.protectionDomain.codeSource.location.path).parent
    val convergencePluginsDir = File(convergencePackageDir).parent + File.separator + "plugins"

    fun addFile(path: String) {
        val urlPath = "jar:file://$path!/"
        addURL(URL(urlPath))
    }

    override fun loadClass(url: String): Class<*>? {
        try {
            println("First attempt...")
            Class.forName(url)
        } catch (ex: Exception) {
            println("Failed.")
        }

        try {
            val cl = classLoader(arrayOf())
            cl.addFile("plugins/")
            println("Second attempt...")
            val returnVal = cl.loadClass(url)
            println("Success!")
            return returnVal
        } catch (ex: Exception) {
            println("Failed.")
            ex.printStackTrace()
        }
        return null
    }
}
