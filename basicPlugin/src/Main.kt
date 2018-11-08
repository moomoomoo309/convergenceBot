package convergence.testPlugins.basicPlugin

import convergence.BaseInterface
import convergence.FakeBaseInterface
import convergence.plugin

object Main: plugin {
    override val name = "basicPlugin"
    override val baseInterface: BaseInterface = FakeBaseInterface
    override fun init() {
        println("basicPlugin init")
    }
}