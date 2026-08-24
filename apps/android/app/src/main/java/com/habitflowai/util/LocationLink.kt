package com.habitflowai.util

private val LOCATION_SHARE_REGEX = Regex("""^https://www\.google\.com/maps\?q=(-?\d+\.?\d*),(-?\d+\.?\d*)$""")

/** Builds the location-share link sent in a chat message or post (a plain Google Maps link). */
fun buildLocationShareLink(lat: Double, lng: Double): String = "https://www.google.com/maps?q=$lat,$lng"

/**
 * Parses a link built by [buildLocationShareLink] back to (lat, lng), or null if the text
 * isn't one. Rendering opens it via a `geo:` intent instead of the raw URL, so the user's
 * OS picks whichever navigation app they have — Google Maps, Waze, or anything else.
 */
fun parseLocationShareLink(text: String): Pair<Double, Double>? {
    val match = LOCATION_SHARE_REGEX.find(text.trim()) ?: return null
    val lat = match.groupValues[1].toDoubleOrNull() ?: return null
    val lng = match.groupValues[2].toDoubleOrNull() ?: return null
    return lat to lng
}
