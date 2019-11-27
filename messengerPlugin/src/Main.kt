@file:Suppress("EnumEntryName")

package convergence.testPlugins.messengerPlugin

import convergence.*
import java.time.OffsetDateTime
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

object MessengerProtocol: Protocol("Messenger")

object MessengerInterface: BaseInterface, IFormatting, INickname, IImages, IMention, IMessageHistory, IReadStatus, IStickers {
    override fun getReadStatus(chat: Chat, message: MessageHistory): Set<User> {
        TODO()
    }

    override fun setRead(chat: Chat, message: MessageHistory, user: User) {
        TODO()
    }

    override val name: String = Main.name
    override val protocol: Protocol = MessengerProtocol
    override val supportedFormats: Set<Format> = setOf(Format.code, Format.monospace, Format.underline, Format.bold, Format.italics)
    val formatMap = mapOf(Format.code to Pair("```", "```"),
            Format.monospace to Pair("`", "`"),
            Format.underline to Pair("__", "__"),
            Format.bold to Pair("**", "**"),
            Format.italics to Pair("*", "*"))

    override fun getUserNickname(chat: Chat, user: User): String? = this.getName(chat, user)

    override fun getBotNickname(chat: Chat): String? = TODO()
    override fun sendImage(chat: Chat, image: Image, sender: User, message: String) = TODO()
    override fun receivedImage(chat: Chat, image: Image, sender: User, message: String) = TODO()
    override fun getMentionText(chat: Chat, user: User): String = TODO()
    override fun getMessages(chat: Chat, since: OffsetDateTime?, until: OffsetDateTime?): List<MessageHistory> = TODO("Should probably store these locally with the DB")
    override fun getUserMessages(chat: Chat, user: User, since: OffsetDateTime?, until: OffsetDateTime?): List<MessageHistory> = TODO("Should probably store these locally with the DB")
    override fun getDelimiters(format: Format): Pair<String, String>? = formatMap[format]
    override fun sendMessage(chat: Chat, message: String): Boolean = TODO()
    override fun getBot(chat: Chat): User = TODO()
    override fun getName(chat: Chat, user: User): String = TODO()

    override fun getChats(): List<Chat> = TODO()
    override fun getUsers(chat: Chat): List<User> = TODO()

    override fun getChatName(chat: Chat): String = TODO()

    override fun sendSticker(chat: Chat, sticker: Sticker) = TODO()
    override fun receivedSticker(chat: Chat, sticker: Sticker) = TODO()
}

lateinit var rawApi: Any
typealias FBCallback = ((Throwable?) -> Unit)?

@Suppress("unused")
enum class emojiSize(val size: Int) { small(32), medium(64), large(128) }
enum class emojiPixelRatio(val ratio: String) { low("1.0"), high("1.5") }
@Suppress("unused", "enumEntryName")
enum class messageReaction(val stringValue: String) {
    love("\uD83D\uDE0D"),
    haha("\uD83D\uDE06"),
    wow("\uD83D\uDE2E"),
    sad("\uD83D\uDE22"),
    angry("\uD83D\uDE20"),
    like("\uD83D\uDC4D"),
    dislike("\uD83D\uDC4E")
}

@Suppress("unused")
object api {
    fun addUserToGroup(userId: String, threadId: String, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "addUserToGroup", userId, threadId, callback)

    fun changeAdminStatus(threadId: String, adminIds: Array<String>, adminStatus: Boolean, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "changeAdminStatus", threadId, adminIds, adminStatus, callback)

    fun changeArchivedStatus(threads: Array<Int>, archive: Boolean, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "changeArchivedStatus", threads, archive, callback)

    fun changeBlockedStatus(userId: String, block: Boolean, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "changedBlockedStatus", userId, block, callback)

    fun changeGroupImage(image: Any, threadId: String, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "changeGroupImage", image, threadId, callback)

    fun changeNickname(nickname: String, threadId: String, participantId: String, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "changeNickname", nickname, threadId, participantId, callback)

    fun changeThreadColor(color: String, threadId: String, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "changeThreadColor", color, threadId, callback)

    fun changeThreadEmoji(emoji: String, threadId: String, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "changeThreadEmoji", emoji, threadId, callback)

    fun createPoll(title: String, threadId: String, options: Map<String, Boolean>, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "crearePoll", title, threadId, options, callback)

    fun deleteMessage(messages: Array<String>, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "deleteMessage", messages, callback)

    fun deleteThread(threads: Array<String>, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "deleteThread", threads, callback)

    fun forwardAttachment(attachmentId: String, users: Array<String>, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "forwardAttachment", attachmentId, users, callback)

    fun getAppState(): Any? = invocable.invokeMethod(rawApi, "getAppState") as Map<*, *>
    fun getCurrentUserId(): Any? = invocable.invokeMethod(rawApi, "getCurrentUserId") as String
    fun getEmojiUrl(c: String, size: emojiSize, pixelRatio: emojiPixelRatio = emojiPixelRatio.low): Any? =
            invocable.invokeMethod(rawApi, "getEmojiUrl", c, size.size, pixelRatio.ratio) as String

    fun getFriendsList(callback: (Any) -> Unit): Any? = invocable.invokeMethod(rawApi, "getFriendsList", callback)
    fun getThreadHistory(threadId: String, amount: Int, timestamp: Long?, callback: (Any) -> Unit): Any? =
            invocable.invokeMethod(rawApi, "getThreadHistory", threadId, amount, timestamp, callback)

    fun getThreadInfo(threadId: String, callback: (Any) -> Unit): Any? =
            invocable.invokeMethod(rawApi, "getThreadInfo", threadId, callback)

    fun getThreadList(limit: Int, timestamp: Long, tags: Array<String>, callback: (Any) -> Unit): Any? =
            invocable.invokeMethod(rawApi, "getThreadList", limit, timestamp, tags, callback)

    fun getThreadPictures(threadId: String, offset: Int, limit: Int, callback: (Any) -> Unit): Any? =
            invocable.invokeMethod(rawApi, "getThreadPictures", threadId, offset, limit, callback)

    fun getUserId(name: String, callback: (Any) -> Unit): Any? =
            invocable.invokeMethod(rawApi, "getUserId", name, callback)

    fun getUserInfo(ids: Array<String>, callback: (Any) -> Unit): Any? =
            invocable.invokeMethod(rawApi, "getUserInfo", ids, callback)

    val threadColors = mapOf(
            "MessengerBlue" to null,
            "GoldenPoppy" to "#ffc300",
            "RadicalRed" to "#fa3c4c",
            "Shocking" to "#d696bb",
            "PictonBlue" to "#6699cc",
            "FreeSpeechGreen" to "#13cf13",
            "Pumpkin" to "#ff7e29",
            "LightCoral" to "#e68585",
            "MediumSlateBlue" to "#7646ff",
            "DeepSkyBlue" to "#20cef5",
            "Fern" to "#67b868",
            "Cameo" to "#d4a88c",
            "BrilliantRose" to "#ff5ca1",
            "BilobaFlower" to "#a695c7"
    )

    fun handleMessageRequest(threadId: String, accept: Boolean, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "handleMessageRequest", threadId, accept, callback)

    fun listen(callback: (Any) -> Unit): Any? = invocable.invokeMethod(rawApi, "listen", callback)
    fun logout(callback: FBCallback): Any? = invocable.invokeMethod(rawApi, "logout", callback)
    fun markAsRead(threadId: String, read: Boolean = true, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "markAsRead", threadId, read, callback)

    fun markAsReadAll(callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "markAsReadAll", callback)

    fun muteThread(threadId: String, muteSeconds: Int, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "muteThread", threadId, muteSeconds, callback)

    fun removeUserFromGroup(userId: String, threadId: String, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "removeUserFromGroup", userId, threadId, callback)

    fun resolvePhotoUrl(photoId: String, callback: (Any) -> Unit): Any? =
            invocable.invokeMethod(rawApi, "resolvePhotoUrl", photoId, callback)

    fun sendMessage(message: String, threadId: String, callback: (Any) -> Unit, messageId: String? = null): Any? =
            invocable.invokeMethod(rawApi, "sendMessage", message, threadId, callback, messageId)

    fun sendTypingIndicator(threadId: String, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "sendTypingIndicator", threadId, callback)

    fun setMessageReaction(reaction: messageReaction, messageId: String, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "setMessageReaction", reaction, messageId, callback)

    fun setOptions(options: Map<String, Any>): Any? = invocable.invokeMethod(rawApi, "setOptions", options)
    fun setTitle(newTitle: String, threadId: String, callback: (Any) -> Unit): Any? =
            invocable.invokeMethod(rawApi, "setTitle", newTitle, threadId, callback)

    fun unsendMessage(messageId: String, callback: FBCallback): Any? =
            invocable.invokeMethod(rawApi, "unsendMessage", messageId, callback)
}


lateinit var invocable: Invocable

@Suppress("unused")
object Main: Plugin {
    override val name = "MessengerPlugin"
    override val baseInterface: BaseInterface = MessengerInterface
    val engine: ScriptEngine = ScriptEngineManager().getEngineByName("nashorn")
    override fun init() {
        println("Messenger Plugin initialized.")
        val context = engine.context
        println(context.scopes)
        println(jcl.loadedResources)
        engine.eval(String(jcl.loadedResources["facebookApi.js"]!!))
        context.setAttribute("username", "", 200)
        context.setAttribute("password", "", 200)
        engine.eval(String(jcl.loadedResources["index.js"]!!))
        invocable = engine as Invocable
        invocable.invokeFunction("init", object: Any() {
            fun setApi(_rawApi: Any) {
                rawApi = _rawApi
            }
        })
    }
}
