package convergence.testPlugins.basicPlugin

import convergence.*
import org.pf4j.PluginWrapper

object BasicBaseInterface: BaseInterface {
    override val protocols: List<Protocol> = listOf(UniversalProtocol)

    override fun sendMessage(chat: Chat, message: String): Boolean = false
    override fun getBot(chat: Chat): User = UniversalUser
    override fun getName(chat: Chat, user: User): String = ""
    override fun getChats(): List<Chat> = emptyList()
    override fun getUsers(chat: Chat): List<User> = emptyList()
    override fun getChatName(chat: Chat): String = ""
    override val name: String = "BasicBaseInterface"
}

class BasicPlugin(wrapper: PluginWrapper): Plugin(wrapper) {
    override val name = "basicPlugin"
    override val baseInterface: BaseInterface = BasicBaseInterface
    override fun init() {
        println("basicPlugin init")
    }
}
