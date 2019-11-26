package convergence.testPlugins.basicPlugin

import convergence.*

object BasicBaseInterface: BaseInterface {
    override val protocol: Protocol = UniversalProtocol

    override fun sendMessage(chat: Chat, message: String): Boolean = false
    override fun getBot(chat: Chat): User = UniversalUser
    override fun getName(chat: Chat, user: User): String = ""
    override fun getChats(): List<Chat> = emptyList()
    override fun getUsers(chat: Chat): List<User> = emptyList()
    override fun getChatName(chat: Chat): String = ""
    override val name: String = "FakeBaseInterface"
}

object Main: Plugin {
    override val name = "basicPlugin"
    override val baseInterface: BaseInterface = BasicBaseInterface
    override fun init() {
        println("basicPlugin init")
    }
}