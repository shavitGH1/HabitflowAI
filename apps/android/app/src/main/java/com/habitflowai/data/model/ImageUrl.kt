package com.habitflowai.data.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.habitflowai.BuildConfig
import java.lang.reflect.Type

// Backend sends imageUrl as a host-relative path (e.g. "/uploads/demo/x.jpg"); resolve
// it against BASE_URL once, at the point it enters the app, so every consumer (Coil,
// local cache, UI) only ever sees a directly loadable absolute URL.
fun resolveImageUrl(raw: String?): String? =
    raw?.takeIf { it.isNotBlank() }?.let {
        if (it.startsWith("http", ignoreCase = true)) it else BuildConfig.BASE_URL.trimEnd('/') + it
    }

/** Registered via @JsonAdapter on every REST-deserialized imageUrl field. */
class ImageUrlDeserializer : JsonDeserializer<String?> {
    override fun deserialize(json: JsonElement?, typeOfT: Type, context: JsonDeserializationContext): String? =
        resolveImageUrl(json?.takeIf { !it.isJsonNull }?.asString)
}
