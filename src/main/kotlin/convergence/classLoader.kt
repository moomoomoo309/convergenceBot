package convergence

interface plugin {
    val name: String

    val baseInterface: BaseInterface
    fun init()
}
