package com.primetv.app.data.api

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.reflect.ParameterizedType

/**
 * Xtream Codes servers sometimes return {} instead of [] for empty lists,
 * or return a JSON object with numeric keys instead of a proper array.
 * This adapter handles all those cases gracefully.
 */
class FlexibleListAdapterFactory : TypeAdapterFactory {

    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!List::class.java.isAssignableFrom(type.rawType)) return null

        val elementType = (type.type as? ParameterizedType)?.actualTypeArguments?.firstOrNull()
            ?: return null
        val elementAdapter = gson.getAdapter(TypeToken.get(elementType))
        val delegateAdapter = gson.getDelegateAdapter(this, type)

        @Suppress("UNCHECKED_CAST")
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) = delegateAdapter.write(out, value)

            override fun read(reader: JsonReader): T {
                return when (reader.peek()) {
                    JsonToken.BEGIN_ARRAY -> delegateAdapter.read(reader)
                    JsonToken.BEGIN_OBJECT -> {
                        // Could be {} empty or {"0":{...},"1":{...}} numeric-key map
                        val list = mutableListOf<Any?>()
                        reader.beginObject()
                        while (reader.hasNext()) {
                            reader.nextName() // skip key
                            list.add(elementAdapter.read(reader))
                        }
                        reader.endObject()
                        list as T
                    }
                    JsonToken.NULL -> {
                        reader.nextNull()
                        emptyList<Any>() as T
                    }
                    else -> {
                        reader.skipValue()
                        emptyList<Any>() as T
                    }
                }
            }
        } as TypeAdapter<T>
    }
}
