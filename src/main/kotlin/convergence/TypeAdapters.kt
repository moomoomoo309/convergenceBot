package convergence

import com.squareup.moshi.*
import java.lang.reflect.Type
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

object OffsetDateTimeAdapter: JsonAdapter<OffsetDateTime>() {
    override fun fromJson(reader: JsonReader): OffsetDateTime? {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(reader.nextLong()), ZoneId.systemDefault())
    }

    override fun toJson(writer: JsonWriter, value: OffsetDateTime?) {
        value?.let {
            writer.value(value.toInstant().toEpochMilli())
        }
    }
}

object SingletonAdapterFactory: JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
        val kclass = type.rawType.kotlin
        if (kclass.objectInstance != null) {
            return object: JsonAdapter<Any>() {
                override fun fromJson(reader: JsonReader): Any {
                    return kclass.objectInstance!!
                }

                override fun toJson(writer: JsonWriter, value: Any?) {
                    writer.name("type")
                    writer.value(kclass.simpleName!!)
                }
            }
        }
        return null
    }
}
