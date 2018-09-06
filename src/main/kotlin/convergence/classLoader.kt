package convergence


import java.net.URL
import java.io.IOException
import java.net.URLClassLoader
import java.net.MalformedURLException

class JarFileLoader(urls: Array<URL>) : URLClassLoader(urls) {
    @Throws(MalformedURLException::class)
    fun addFile(path: String) {
        val urlPath = "jar:file://$path!/"
        addURL(URL(urlPath))
    }

    companion object {
        @JvmStatic
        fun loadClass(url: String) {
            try {
                println("First attempt...")
                Class.forName(url)
            } catch (ex: Exception) {
                println("Failed.")
            }

            try {
                val urls = arrayOf<URL>()
                val cl = JarFileLoader(urls)
                cl.addFile("/opt/mysql-connector-java-5.0.4/mysql-connector-java-5.0.4-bin.jar")
                println("Second attempt...")
                cl.loadClass(url)
                println("Success!")
            } catch (ex: Exception) {
                println("Failed.")
                ex.printStackTrace()
            }

        }
    }
}