package convergence

import com.squareup.moshi.*
import java.lang.reflect.Type
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.reflect.jvm.jvmName

fun numToOrdinal(num: Int): String {
    val suffixes = arrayOf("th", "st", "nd", "rd")
    return "$num${suffixes.getOrNull(num % 10) ?: "th"}"
}

fun getReadStringOrThrow(reader: JsonReader, className: String, startCountAt: Int = 1): (String) -> String {
    var count = startCountAt
    fun readStringOrThrow(fieldName: String): String {
        if (reader.nextName() != fieldName)
            throw JsonDataException("$fieldName should be the ${numToOrdinal(count++)} field for a $className")
        return reader.nextString()
    }
    return ::readStringOrThrow
}

fun getReadStringOrNull(reader: JsonReader): (String) -> String? {
    fun readStringOrNull(fieldName: String): String? {
        if (reader.nextName() != fieldName)
            return null
        return reader.nextString()
    }
    return ::readStringOrNull
}

object OffsetDateTimeAdapter: JsonAdapter<OffsetDateTime>() {
    @FromJson
    override fun fromJson(reader: JsonReader): OffsetDateTime? {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(reader.nextLong()), ZoneId.systemDefault())
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: OffsetDateTime?) {
        value?.let {
            writer.value(value.toInstant().toEpochMilli())
        }
    }
}

object SingletonAdapterFactory: JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
        val kclass = type.rawType.kotlin
        val className = kclass.simpleName ?: kclass.jvmName
        if (kclass.objectInstance != null) {
            return object: JsonAdapter<Any>() {
                @FromJson
                override fun fromJson(reader: JsonReader): Any? {
                    val readStringOrNull = getReadStringOrNull(reader)
                    if (readStringOrNull("type") != className)
                        return null
                    return kclass.objectInstance!!
                }

                @ToJson
                override fun toJson(writer: JsonWriter, value: Any?) {
                    writer.name("type")
                    writer.value(className)
                }
            }
        }
        return null
    }
}

object BaseInterfaceAdapter: JsonAdapter<BaseInterface>() {
    override fun fromJson(reader: JsonReader): BaseInterface? {
        when (reader.nextName()) {
            "type" -> if (reader.nextString() != "BaseInterface")
                return null

            "name" -> {
                val name = reader.nextString()
                return protocols.firstOrNull { it.name == name }?.let {
                    baseInterfaceMap[it]
                }
            }
        }
        return null
    }

    override fun toJson(writer: JsonWriter, value: BaseInterface?) {
        value?.let {
            writer.name("type")
            writer.value("BaseInterface")
            writer.name("name")
            writer.value(value.name)
        }
    }
}

object BaseInterfaceAdapterFactory: JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
        return if (type.rawType.isAssignableFrom(BaseInterface::class.java)) BaseInterfaceAdapter else null
    }
}

var chatAdapterFactory: PolymorphicJsonAdapterFactory<Chat> by SharedVariables
var userAdapterFactory: PolymorphicJsonAdapterFactory<User> by SharedVariables
var imageAdapterFactory: PolymorphicJsonAdapterFactory<Image> by SharedVariables
var messageHistoryAdapterFactory: PolymorphicJsonAdapterFactory<MessageHistory> by SharedVariables
var stickerAdapterFactory: PolymorphicJsonAdapterFactory<Sticker> by SharedVariables
var formatAdapterFactory: PolymorphicJsonAdapterFactory<Format> by SharedVariables
var customEmojiAdapterFactory: PolymorphicJsonAdapterFactory<CustomEmoji> by SharedVariables
