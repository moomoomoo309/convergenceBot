package convergence

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import net.sourceforge.argparse4j.inf.Namespace
import org.pf4j.PluginManager
import java.nio.file.Path
import java.nio.file.Paths

const val defaultCommandDelimiter = "!"
private val configuration = Configuration.conf

object Configuration {
    val conf = mutableMapOf(
            "protocols" to mutableSetOf<Protocol>(),
            "baseInterfaceMap" to mutableMapOf<Protocol, BaseInterface>(),
            "chatMap" to mutableMapOf<Int, Chat>(),
            "reverseChatMap" to mutableMapOf<Chat, Int>(),
            "currentChatID" to 0,
            "moshiBuilder" to Moshi.Builder()
                    .add(SingletonAdapterFactory)
                    .add(OffsetDateTimeAdapter)
                    .add(KotlinJsonAdapterFactory())!!,
            "moshi" to null,
            "convergencePath" to Paths.get(System.getProperty("user.home"), ".convergence"),
            "commandLineArgs" to null,
            "pluginManager" to null,
            "pluginPaths" to mutableSetOf<Path>(),
            "commandDelimiters" to DefaultMap<Chat, String>(defaultCommandDelimiter),
            "aliasVars" to mutableMapOf(
                    "sender" to { b: BaseInterface, c: Chat, s: User -> b.getName(c, s) },
                    "botname" to { b: BaseInterface, c: Chat, _: User -> b.getName(c, b.getBot(c)) },
                    "chatname" to { b: BaseInterface, c: Chat, _: User -> b.getChatName(c) }),
            "linkedChats" to hashMapOf<Chat, MutableSet<Chat>>(),
            "delimiters" to hashMapOf<Chat, String>(),
            "sortedHelpText" to arrayListOf<CommandLike>(),
            "commands" to mutableMapOf<Chat, MutableMap<String, Command>>(),
            "aliases" to mutableMapOf<Chat, MutableMap<String, Alias>>()
    )
}

internal val protocols: MutableSet<Protocol> by configuration
internal val baseInterfaceMap: MutableMap<Protocol, BaseInterface> by configuration
internal val chatMap: MutableMap<Int, Chat> by configuration
internal val reverseChatMap: MutableMap<Chat, Int> by configuration
internal var currentChatID: Int by configuration
internal val moshiBuilder: Moshi.Builder by configuration
internal var moshi: Moshi by configuration
internal val convergencePath: Path by configuration
internal var commandLineArgs: Namespace by configuration
internal var pluginManager: PluginManager by configuration
internal var pluginPaths: Set<Path> by configuration
internal val commandDelimiters: DefaultMap<Chat, String> by configuration
internal val aliasVars: MutableMap<String, (baseInterface: BaseInterface, chat: Chat, sender: User) -> String> by configuration
internal val linkedChats: MutableMap<Chat, MutableSet<Chat>> by configuration
internal val delimiters: MutableMap<Chat, String> by configuration
internal val sortedHelpText: ArrayList<CommandLike> by configuration
internal val commands: MutableMap<Chat, MutableMap<String, Command>> by configuration
internal val aliases: MutableMap<Chat, MutableMap<String, Alias>> by configuration
