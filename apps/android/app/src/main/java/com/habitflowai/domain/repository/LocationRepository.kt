package com.habitflowai.domain.repository

import android.location.Location
import com.habitflowai.data.local.entity.LocationEntity
import com.habitflowai.data.model.LocationResponse

/** Result of resolving a typed address via [LocationRepository.geocodeAddress]. [displayAddress] is the
 *  Geocoder's own formatted match (e.g. "Hahagana St 3, Kiryat Ono, Israel") — surfacing it lets the
 *  caller show the user what the address actually resolved to, since street-level geocoding can land
 *  in the wrong city for an ambiguous or malformed query. */
data class GeocodedAddress(val latitude: Double, val longitude: Double, val displayAddress: String?)

interface LocationRepository {
    suspend fun captureAndSaveLocation(
        habitId: String?,
        isPublic: Boolean = true,
        type: String = "task"
    )
    /** One-shot device location fetch (not saved/synced) — null if permission is missing or it can't be resolved. */
    suspend fun getCurrentDeviceLocation(): Location?
    /** Resolves a typed address to coordinates (+ the Geocoder's own formatted match) — null if it can't be found. */
    suspend fun geocodeAddress(address: String): GeocodedAddress?
    fun getLastLocation(): LocationEntity?
    suspend fun getLocationsForHabit(habitId: String): List<LocationEntity>
    suspend fun getLocations(): List<LocationEntity>
    suspend fun getMyLocations(): List<LocationResponse>
    suspend fun getPublicLocations(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        since: Long? = null,
        scope: String? = null
    ): List<LocationResponse>
}
