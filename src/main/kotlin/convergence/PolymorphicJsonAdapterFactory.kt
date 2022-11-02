/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// This is modified from Moshi-adapters. Its license is above. The modifications are:
// - Porting it to Kotlin and making the code more idiomatic to Kotlin
// - Switching the return types from JsonAdapter<Any> to JsonAdapter<T>
// - Having it use to the fallback adapter if the label is missing (not just if it's the incorrect label).

@file:Suppress("unused")

package convergence

import com.squareup.moshi.*
import java.io.IOException
import java.lang.reflect.Type
import javax.annotation.CheckReturnValue

/**
 * A JsonAdapter factory for objects that include type information in the JSON. When decoding JSON
 * Moshi uses this type information to determine which class to decode to. When encoding Moshi uses
 * the object’s class to determine what type information to include.
 *
 *
 * Suppose we have an interface, its implementations, and a class that uses them:
 *
 * <pre>`interface HandOfCards {
 * }
 *
 * class BlackjackHand implements HandOfCards {
 * Card hidden_card;
 * List<Card> visible_cards;
 * }
 *
 * class HoldemHand implements HandOfCards {
 * Set<Card> hidden_cards;
 * }
 *
 * class Player {
 * String name;
 * HandOfCards hand;
 * }
`</pre> *
 *
 *
 * We want to decode the following JSON into the player model above:
 *
 * <pre>`{
 * "name": "Jesse",
 * "hand": {
 * "hand_type": "blackjack",
 * "hidden_card": "9D",
 * "visible_cards": ["8H", "4C"]
 * }
 * }
`</pre> *
 *
 *
 * Left unconfigured, Moshi would incorrectly attempt to decode the hand object to the abstract
 * `HandOfCards` interface. We configure it to use the appropriate subtype instead:
 *
 * <pre>`Moshi moshi = new Moshi.Builder()
 * .add(PolymorphicJsonAdapterFactory.of(HandOfCards.class, "hand_type")
 * .withSubtype(BlackjackHand.class, "blackjack")
 * .withSubtype(HoldemHand.class, "holdem"))
 * .build();
`</pre> *
 *
 *
 * This class imposes strict requirements on its use:
 *
 *
 *  * Base types may be classes or interfaces.
 *  * Subtypes must encode as JSON objects.
 *  * Type information must be in the encoded object. Each message must have a type label like
 * `hand_type` whose value is a string like `blackjack` that identifies which type
 * to use.
 *  * Each type identifier must be unique.
 *
 *
 *
 * For best performance type information should be the first field in the object. Otherwise Moshi
 * must reprocess the JSON stream once it knows the object's type.
 *
 *
 * If an unknown subtype is encountered when decoding:
 *
 *
 *  * If [.withDefaultValue] is used, then `defaultValue` will be returned.
 *  * If [.withFallbackJsonAdapter] is used, then the `fallbackJsonAdapter.fromJson(reader)` result will be returned.
 *  * Otherwise a [JsonDataException] will be thrown.
 *
 *
 *
 * If an unknown type is encountered when encoding:
 *
 *
 *  * If [.withFallbackJsonAdapter] is used, then the `fallbackJsonAdapter.toJson(writer, value)` result will be returned.
 *  * Otherwise a [IllegalArgumentException] will be thrown.
 *
 *
 *
 * If the same subtype has multiple labels the first one is used when encoding.
 */
class PolymorphicJsonAdapterFactory<T> internal constructor(
    val baseType: Class<T>,
    val labelKey: String,
    val labels: List<String>,
    val subtypes: List<Type>,
    val fallbackJsonAdapter: JsonAdapter<T>?
): JsonAdapter.Factory {
    /** Returns a new factory that decodes instances of `subtype`.  */
    fun withSubtype(subtype: Class<out T>?, label: String?): PolymorphicJsonAdapterFactory<T> {
        if (subtype == null) throw NullPointerException("subtype == null")
        if (label == null) throw NullPointerException("label == null")
        if (labels.contains(label)) {
            throw IllegalArgumentException("Labels must be unique.")
        }
        val newLabels = labels.toMutableList()
        newLabels.add(label)
        val newSubtypes = subtypes.toMutableList()
        newSubtypes.add(subtype)
        return PolymorphicJsonAdapterFactory(
            baseType, labelKey, newLabels, newSubtypes, fallbackJsonAdapter
        )
    }

    /**
     * Returns a new factory that with default to `fallbackJsonAdapter.fromJson(reader)` upon
     * decoding of unrecognized labels.
     *
     *
     * The [JsonReader] instance will not be automatically consumed, so make sure to consume
     * it within your implementation of [JsonAdapter.fromJson]
     */
    fun withFallbackJsonAdapter(
        fallbackJsonAdapter: JsonAdapter<T>
    ): PolymorphicJsonAdapterFactory<T> {
        return PolymorphicJsonAdapterFactory(baseType, labelKey, labels, subtypes, fallbackJsonAdapter)
    }

    /**
     * Returns a new factory that will default to `defaultValue` upon decoding of unrecognized
     * labels. The default value should be immutable.
     */
    fun withDefaultValue(defaultValue: T?): PolymorphicJsonAdapterFactory<T> {
        return withFallbackJsonAdapter(buildFallbackJsonAdapter(defaultValue))
    }

    private fun buildFallbackJsonAdapter(defaultValue: T?): JsonAdapter<T> {
        return object: JsonAdapter<T>() {
            @Throws(IOException::class)
            override fun fromJson(reader: JsonReader): T? {
                reader.skipValue()
                return defaultValue
            }

            @Throws(IOException::class)
            override fun toJson(writer: JsonWriter, value: T?) {
                throw IllegalArgumentException(
                    "Expected one of $subtypes but found $value, a ${value!!.javaClass}. Register this subtype."
                )
            }
        }
    }

    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) != baseType || annotations.isNotEmpty()) {
            return null
        }
        val jsonAdapters: MutableList<JsonAdapter<T>> = MutableList(subtypes.size) { i -> moshi.adapter(subtypes[i]) }
        return PolymorphicJsonAdapter(labelKey, labels, subtypes, jsonAdapters, fallbackJsonAdapter)
            .nullSafe()
    }

    internal class PolymorphicJsonAdapter<T>(
        val labelKey: String,
        val labels: List<String>,
        val subtypes: List<Type>,
        val jsonAdapters: List<JsonAdapter<T>>,
        val fallbackJsonAdapter: JsonAdapter<T>?
    ): JsonAdapter<T?>() {
        /** Single-element options containing the label's key only.  */
        val labelKeyOptions: JsonReader.Options = JsonReader.Options.of(labelKey)

        /** Corresponds to subtypes.  */
        val labelOptions: JsonReader.Options = JsonReader.Options.of(*labels.toTypedArray())

        @Throws(IOException::class)
        override fun fromJson(reader: JsonReader): T? {
            val peeked = reader.peekJson()
            peeked.setFailOnUnknown(false)
            val labelIndex: Int
            peeked.use {
                labelIndex = labelIndex(it)
            }
            return if (labelIndex == -1) {
                // This should throw a NullPointerException if there's no fallback adapter.
                fallbackJsonAdapter!!.fromJson(reader)
            } else {
                jsonAdapters[labelIndex].fromJson(reader)
            }
        }

        @Throws(IOException::class)
        private fun labelIndex(reader: JsonReader): Int {
            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.selectName(labelKeyOptions) == -1) {
                    reader.skipName()
                    reader.skipValue()
                    continue
                }
                val labelIndex = reader.selectString(labelOptions)
                if (labelIndex == -1 && fallbackJsonAdapter == null) {
                    throw JsonDataException(
                        "Expected one of $labels for key '$labelKey' but found '${reader.nextString()}'. Register a subtype for this label."
                    )
                }
                return labelIndex
            }
            if (fallbackJsonAdapter == null)
                throw JsonDataException("Missing label for $labelKey")
            return -1
        }

        @Throws(IOException::class)
        override fun toJson(writer: JsonWriter, value: T?) {
            val type = value!!.javaClass
            val labelIndex = subtypes.indexOf(type)
            val adapter: JsonAdapter<T> = if (labelIndex == -1) {
                fallbackJsonAdapter ?: throw IllegalArgumentException(
                    "Expected one of $subtypes but found $value, a ${value.javaClass}. Register this subtype."
                )
            } else {
                jsonAdapters[labelIndex]
            }
            writer.beginObject()
            if (adapter !== fallbackJsonAdapter) {
                writer.name(labelKey).value(labels[labelIndex])
            }
            val flattenToken = writer.beginFlatten()
            adapter.toJson(writer, value)
            writer.endFlatten(flattenToken)
            writer.endObject()
        }

        override fun toString(): String {
            return "PolymorphicJsonAdapter($labelKey)"
        }
    }

    companion object {
        /**
         * @param baseType The base type for which this factory will create adapters. Cannot be Object.
         * @param labelKey The key in the JSON object whose value determines the type to which to map the
         * JSON object.
         */
        @CheckReturnValue
        fun <T> of(baseType: Class<T>?, labelKey: String?): PolymorphicJsonAdapterFactory<T> {
            if (baseType == null) throw NullPointerException("baseType == null")
            if (labelKey == null) throw NullPointerException("labelKey == null")
            return PolymorphicJsonAdapterFactory(
                baseType, labelKey, emptyList(), emptyList(), null
            )
        }
    }
}
