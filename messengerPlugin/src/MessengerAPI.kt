package convergence.testPlugins.messengerPlugin

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import convergence.Chat
import convergence.Image
import convergence.Sticker
import convergence.User
import io.ktor.client.HttpClient
import io.ktor.client.engine.jetty.Jetty
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.util.KtorExperimentalAPI

val address = "127.0.0.1"
@KtorExperimentalAPI
val client = HttpClient(Jetty) {
    install(WebSockets)
}

fun sendToAPI(vararg args: Any?): Any? {
    return null
}

class MessengerChat(val id: String): Chat(MessengerProtocol, id)
class MessengerUser(val id: String, chat: MessengerChat): User(chat)
class MessengerImage(val url: String): Image()
class MessengerSticker(name: String, val url: String): Sticker(name, url)

@Suppress("unused")
object Api {
    fun addUserToGroup(userId: String, threadId: String): Any? =
            sendToAPI("addUserToGroup", userId, threadId)

    fun changeAdminStatus(threadId: String, adminIds: Array<String>, adminStatus: Boolean): Any? =
            sendToAPI("changeAdminStatus", threadId, adminIds, adminStatus)

    fun changeArchivedStatus(threads: Array<Int>, archive: Boolean): Any? =
            sendToAPI("changeArchivedStatus", threads, archive)

    fun changeBlockedStatus(userId: String, block: Boolean): Any? =
            sendToAPI("changedBlockedStatus", userId, block)

    fun changeGroupImage(image: Any, threadId: String): Any? =
            sendToAPI("changeGroupImage", image, threadId)

    fun changeNickname(nickname: String, threadId: String, participantId: String): Any? =
            sendToAPI("changeNickname", nickname, threadId, participantId)

    fun changeThreadColor(color: ThreadColor, threadId: String): Any? =
            sendToAPI("changeThreadColor", color.color, threadId)

    fun changeThreadEmoji(emoji: String, threadId: String): Any? =
            sendToAPI("changeThreadEmoji", emoji, threadId)

    fun createPoll(title: String, threadId: String, options: Map<String, Boolean>): Any? =
            sendToAPI("crearePoll", title, threadId, options)

    fun deleteMessage(messages: Array<String>): Any? =
            sendToAPI("deleteMessage", messages)

    fun deleteThread(threads: Array<String>): Any? =
            sendToAPI("deleteThread", threads)

    fun forwardAttachment(attachmentId: String, users: Array<String>): Any? =
            sendToAPI("forwardAttachment", attachmentId, users)

    fun getAppState(): Any? = sendToAPI("getAppState") as Map<*, *>
    fun getCurrentUserId(): Any? = sendToAPI("getCurrentUserId") as String
    fun getEmojiUrl(c: String, size: emojiSize, pixelRatio: emojiPixelRatio = emojiPixelRatio.low): Any? =
            sendToAPI("getEmojiUrl", c, size.size, pixelRatio.ratio) as String

    fun getFriendsList(callback: (Any) -> Unit): Any? = sendToAPI("getFriendsList")
    fun getThreadHistory(threadId: String, amount: Int, timestamp: Long?): Any? =
            sendToAPI("getThreadHistory", threadId, amount, timestamp)

    fun getThreadInfo(threadId: String): Any? =
            sendToAPI("getThreadInfo", threadId)

    fun getThreadList(limit: Int, timestamp: Long?, tags: Array<String>): Any? =
            sendToAPI("getThreadList", limit, timestamp, tags)

    fun getThreadPictures(threadId: String, offset: Int, limit: Int): Any? =
            sendToAPI("getThreadPictures", threadId, offset, limit)

    fun getUserId(name: String): Any? =
            sendToAPI("getUserId", name)

    fun getUserInfo(vararg ids: String): Any? =
            sendToAPI("getUserInfo", ids)

    val threadColors = mapOf(
            "MessengerBlue" to ThreadColor.MessengerBlue,
            "GoldenPoppy" to ThreadColor.GoldenPoppy,
            "RadicalRed" to ThreadColor.RadicalRed,
            "Shocking" to ThreadColor.Shocking,
            "PictonBlue" to ThreadColor.PictonBlue,
            "FreeSpeechGreen" to ThreadColor.FreeSpeechGreen,
            "Pumpkin" to ThreadColor.Pumpkin,
            "LightCoral" to ThreadColor.LightCoral,
            "MediumSlateBlue" to ThreadColor.MediumSlateBlue,
            "DeepSkyBlue" to ThreadColor.DeepSkyBlue,
            "Fern" to ThreadColor.Fern,
            "Cameo" to ThreadColor.Cameo,
            "BrilliantRose" to ThreadColor.BrilliantRose,
            "BilobaFlower" to ThreadColor.BilobaFlower
    )

    fun handleMessageRequest(threadId: String, accept: Boolean): Any? =
            sendToAPI("handleMessageRequest", threadId, accept)

    suspend fun listen() {
        sendToAPI("listen")
        client.ws(method = HttpMethod.Get, host = address, port = 50672) {
            for (frame in incoming) {
                if (frame !is Frame.Text) {
                    println("Got invalid frame! Frame type: ${frame.frameType}")
                    continue
                } else {
                    val msg = jsonParser.parse<JsonObject>(String(frame.data))
                    if (msg == null) {
                        println("Got invalid body! Body: ${frame.data}")
                        continue
                    }
                    when (msg["type"] as String) {
                        "message" -> {
                            val chat = MessengerChat(msg["threadID"] as String)
                            val sender = MessengerUser(msg["senderID"] as String, chat)
                            if ("attachments" in msg) {
                                @Suppress("UNCHECKED_CAST")
                                for (attachment in msg["attachments"] as JsonArray<JsonObject>) {
                                    when (attachment["type"] as String) {
                                        "photo" -> MessengerInterface.receivedImage(chat, MessengerImage(attachment["largePreviewUrl"] as String), sender, msg["body"] as String)
                                        "sticker" -> MessengerInterface.receivedSticker(chat, MessengerSticker(attachment["caption"] as String, attachment["url"] as String), sender)
                                        "video", "audio", "file" -> MessengerInterface.receivedMessage(chat, msg["body"] as String + "\n${attachment["url"] as String}", sender)
                                    }
                                }
                            } else
                                MessengerInterface.receivedMessage(chat, msg["body"] as String, sender)
                        }
                        "typ" -> {
                            val chat = MessengerChat(msg["threadID"] as String)
                            val sender = MessengerUser(msg["from"] as String, chat)
                            if ((msg["isTyping"] as String).toBoolean())
                                MessengerInterface.startedTyping(chat, sender)
                            else
                                MessengerInterface.stoppedTyping(chat, sender)
                        }
                    }
                }
            }
        }
    }

    fun logout(): Any? = sendToAPI("logout")
    fun markAsRead(threadId: String, read: Boolean = true): Any? =
            sendToAPI("markAsRead", threadId, read)

    fun markAsReadAll(): Any? =
            sendToAPI("markAsReadAll")

    fun muteThread(threadId: String, muteSeconds: Int): Any? =
            sendToAPI("muteThread", threadId, muteSeconds)

    fun removeUserFromGroup(userId: String, threadId: String): Any? =
            sendToAPI("removeUserFromGroup", userId, threadId)

    fun resolvePhotoUrl(photoId: String): Any? =
            sendToAPI("resolvePhotoUrl", photoId)

    fun sendMessage(message: String, threadId: String, messageId: String? = null): Any? =
            sendToAPI("sendMessage", message, threadId, messageId)

    fun sendTypingIndicator(threadId: String): Any? =
            sendToAPI("sendTypingIndicator", threadId)

    fun setMessageReaction(reaction: messageReaction, messageId: String): Any? =
            sendToAPI("setMessageReaction", reaction, messageId)

    fun setOptions(options: Map<String, Any>): Any? = sendToAPI("setOptions", options)
    fun setTitle(newTitle: String, threadId: String): Any? =
            sendToAPI("setTitle", newTitle, threadId)

    fun unsendMessage(messageId: String): Any? =
            sendToAPI("unsendMessage", messageId)
}
