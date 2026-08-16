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

// Profile pictures are either an uploaded photo (a host-relative "/uploads/..." path that
// resolves like any image URL) or a bundled preset avatar key ("preset:1"). Preset keys
// must stay untouched — a URL resolver would otherwise mangle them into a bogus path.
fun resolveProfilePicture(raw: String?): String? =
    raw?.takeIf { it.isNotBlank() }?.let {
        if (it.startsWith("preset:", ignoreCase = true)) it else resolveImageUrl(it)
    }

/** Registered via @JsonAdapter on every REST-deserialized imageUrl field. */
class ImageUrlDeserializer : JsonDeserializer<String?> {
    override fun deserialize(json: JsonElement?, typeOfT: Type, context: JsonDeserializationContext): String? =
        resolveImageUrl(json?.takeIf { !it.isJsonNull }?.asString)
}

/** Registered via @JsonAdapter on profilePicture fields — keeps "preset:N" keys intact. */
class ProfilePictureDeserializer : JsonDeserializer<String?> {
    override fun deserialize(json: JsonElement?, typeOfT: Type, context: JsonDeserializationContext): String? =
        resolveProfilePicture(json?.takeIf { !it.isJsonNull }?.asString)
}
