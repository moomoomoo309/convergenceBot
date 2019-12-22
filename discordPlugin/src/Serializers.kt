package convergence.testPlugins.discordPlugin

import convergence.Chat
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import net.dv8tion.jda.api.entities.PrivateChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.internal.JDAImpl
import net.dv8tion.jda.internal.entities.GuildImpl
import net.dv8tion.jda.internal.entities.PrivateChannelImpl
import net.dv8tion.jda.internal.entities.TextChannelImpl
import net.dv8tion.jda.internal.entities.UserImpl

private val jdaImpl = jda as JDAImpl

@Serializer(DiscordChat::class)
object DiscordChatSerializer: KSerializer<DiscordChat> {
    override val descriptor: SerialDescriptor = StringDescriptor.withName("DiscordChat")

    override fun deserialize(decoder: Decoder): DiscordChat {
        return when (decoder.decodeByte().toInt()) {
            0 -> DiscordChat(PrivateChannelImpl(decoder.decodeLong(), UserImpl(decoder.decodeLong(), jdaImpl)))
            1 -> DiscordChat(TextChannelImpl(decoder.decodeLong(), GuildImpl(jdaImpl, decoder.decodeLong())))
            else -> throw IllegalArgumentException("DiscordChat not encoded properly!")
        }
    }

    override fun serialize(encoder: Encoder, obj: DiscordChat) {
        val channel = obj.channel
        if (channel is PrivateChannel) {
            encoder.encodeByte(0)
            encoder.encodeLong(channel.idLong)
            encoder.encodeLong(channel.user.idLong)
        } else {
            encoder.encodeByte(1)
            encoder.encodeLong(channel.idLong)
            encoder.encodeLong((channel as TextChannel).guild.idLong)
        }
    }
}

@Serializer(DiscordUser::class)
object DiscordUserSerializer: KSerializer<DiscordUser> {
    override val descriptor: SerialDescriptor = StringDescriptor.withName("DiscordUser")

    @ImplicitReflectionSerializer
    override fun deserialize(decoder: Decoder): DiscordUser {
        val chat = decoder.decode(Chat::class.serializer())
        return DiscordUser(chat, UserImpl(decoder.decodeLong(), jdaImpl))
    }

    @ImplicitReflectionSerializer
    override fun serialize(encoder: Encoder, obj: DiscordUser) {
        encoder.encode(obj.chat)
        encoder.encodeLong(obj.author.idLong)
    }

}