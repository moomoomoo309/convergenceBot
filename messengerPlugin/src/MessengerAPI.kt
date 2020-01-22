@file:Suppress("unused")
package convergence.testPlugins.messengerPlugin

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import convergence.Chat
import convergence.Image
import convergence.Sticker
import convergence.User
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.client.request.post
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

const val address = "127.0.0.1"
@KtorExperimentalAPI
val client = HttpClient(CIO) {
    install(WebSockets)
}

@KtorExperimentalAPI
fun sendToAPI(method: String, vararg args: Any?) = runBlocking {
    client.post<String>(host = address, port = 5672) {
        body = JsonObject(mapOf(
                "method" to method,
                "arguments" to args
        )).toString()
    }
}

class MessengerChat(val id: String): Chat(MessengerProtocol, id)
class MessengerUser(val id: String, chat: MessengerChat): User(chat)
class MessengerImage(val url: String): Image()
class MessengerSticker(name: String, val url: String): Sticker(name, url)

enum class emojiSize(val size: Int) { small(32), medium(64), large(128) }

enum class emojiPixelRatio(val ratio: String) { low("1.0"), high("1.5") }
@Suppress("enumEntryName")
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
enum class ThreadColor(val color: String?) {
    MessengerBlue(null),
    GoldenPoppy("#ffc300"),
    RadicalRed("#fa3c4c"),
    Shocking("#d696bb"),
    PictonBlue("#6699cc"),
    FreeSpeechGreen("#13cf13"),
    Pumpkin("#ff7e29"),
    LightCoral("#e68585"),
    MediumSlateBlue("#7646ff"),
    DeepSkyBlue("#20cef5"),
    Fern("#67b868"),
    Cameo("#d4a88c"),
    BrilliantRose("#ff5ca1"),
    BilobaFlower("#a695c7")
}

@Suppress("unused")
@KtorExperimentalAPI
object Api {
    fun addUserToGroup(userId: String, threadId: String) = sendToAPI("addUserToGroup", userId, threadId)
    fun changeAdminStatus(threadId: String, adminIds: Array<String>, adminStatus: Boolean) = sendToAPI("changeAdminStatus", threadId, adminIds, adminStatus)
    fun changeArchivedStatus(threads: Array<Int>, archive: Boolean) = sendToAPI("changeArchivedStatus", threads, archive)
    fun changeBlockedStatus(userId: String, block: Boolean) = sendToAPI("changedBlockedStatus", userId, block)
    fun changeGroupImage(image: Any, threadId: String) = sendToAPI("changeGroupImage", image, threadId)
    fun changeNickname(nickname: String, threadId: String, participantId: String) = sendToAPI("changeNickname", nickname, threadId, participantId)
    fun changeThreadColor(color: ThreadColor, threadId: String) = sendToAPI("changeThreadColor", color.color, threadId)
    fun changeThreadEmoji(emoji: String, threadId: String) = sendToAPI("changeThreadEmoji", emoji, threadId)
    fun createPoll(title: String, threadId: String, options: Map<String, Boolean>) = sendToAPI("createPoll", title, threadId, options)
    fun deleteMessage(messages: Array<String>) = sendToAPI("deleteMessage", messages)
    fun deleteThread(threads: Array<String>) = sendToAPI("deleteThread", threads)
    fun forwardAttachment(attachmentId: String, users: Array<String>) = sendToAPI("forwardAttachment", attachmentId, users)
    fun getAppState() = jsonParser.parse<Map<*, *>>(sendToAPI("getAppState"))
    fun getCurrentUserId() = sendToAPI("getCurrentUserId")
    fun getEmojiUrl(c: String, size: emojiSize, pixelRatio: emojiPixelRatio = emojiPixelRatio.low) = sendToAPI("getEmojiUrl", c, size.size, pixelRatio.ratio)
    fun getFriendsList() = sendToAPI("getFriendsList")
    fun getThreadHistory(threadId: String, amount: Int, timestamp: Long?) = sendToAPI("getThreadHistory", threadId, amount, timestamp)
    fun getThreadInfo(threadId: String) = sendToAPI("getThreadInfo", threadId)
    fun getThreadList(limit: Int, timestamp: Long?, tags: Array<String>) = sendToAPI("getThreadList", limit, timestamp, tags)
    fun getThreadPictures(threadId: String, offset: Int, limit: Int) = sendToAPI("getThreadPictures", threadId, offset, limit)
    fun getUserId(name: String) = sendToAPI("getUserId", name)
    fun getUserInfo(vararg ids: String) = sendToAPI("getUserInfo", ids)
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

    fun handleMessageRequest(threadId: String, accept: Boolean) = sendToAPI("handleMessageRequest", threadId, accept)
    fun listen() {
        sendToAPI("listen")
        GlobalScope.launch {
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
                                println("[${MessengerInterface.getUserNickname(chat, sender)}]: ${msg["body"] as String}")
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
    }

    fun logout() = sendToAPI("logout")
    fun markAsRead(threadId: String, read: Boolean = true) = sendToAPI("markAsRead", threadId, read)
    fun markAsReadAll() = sendToAPI("markAsReadAll")
    fun muteThread(threadId: String, muteSeconds: Int) = sendToAPI("muteThread", threadId, muteSeconds)
    fun removeUserFromGroup(userId: String, threadId: String) = sendToAPI("removeUserFromGroup", userId, threadId)
    fun resolvePhotoUrl(photoId: String) = sendToAPI("resolvePhotoUrl", photoId)
    fun sendMessage(message: String, threadId: String, messageId: String? = null) = sendToAPI("sendMessage", message, threadId, messageId)
    fun sendTypingIndicator(threadId: String) = sendToAPI("sendTypingIndicator", threadId)
    fun setMessageReaction(reaction: messageReaction, messageId: String) = sendToAPI("setMessageReaction", reaction, messageId)
    fun setOptions(options: Map<String, Any>) = sendToAPI("setOptions", options)
    fun setTitle(newTitle: String, threadId: String) = sendToAPI("setTitle", newTitle, threadId)
    fun unsendMessage(messageId: String) = sendToAPI("unsendMessage", messageId)
}
