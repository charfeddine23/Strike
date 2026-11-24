package com.example.data.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class GeocodePlace(
    val displayName: String,
    val shortLabel: String,
    val lat: Double,
    val lng: Double
)

/**
 * Nominatim forward & reverse geocoding client with strict Tifinagh glyph filtering
 * (U+2D30..U+2D7F) so North African / Maghreb addresses display cleanly in Arabic, French, and English.
 */
object NominatimGeocoder {

    private val tifinaghRegex = Regex("[\u2D30-\u2D7F]+")
    private val multiSpaceRegex = Regex("\\s{2,}")
    private val spaceBeforeCommaRegex = Regex("\\s+,")
    private val danglingSeparatorRegex = Regex("(\\s*[,\\-/|•]\\s*){2,}")

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Strips Unicode Tifinagh characters (U+2D30..U+2D7F) and normalizes leftover punctuation/whitespace.
     */
    fun stripTifinagh(raw: String): String {
        if (raw.isEmpty()) return ""
        val removed = tifinaghRegex.replace(raw, " ")
        val cleanedSeparators = danglingSeparatorRegex.replace(removed, ", ")
        val noCommaSpace = spaceBeforeCommaRegex.replace(cleanedSeparators, ",")
        return multiSpaceRegex.replace(noCommaSpace, " ")
            .trim()
            .trim(',', '-', '/', '|', '•', ' ')
            .trim()
    }

    suspend fun searchPlaces(query: String, lang: String = "en"): List<GeocodePlace> =
        withContext(Dispatchers.IO) {
            val trimmed = stripTifinagh(query)
            if (trimmed.length < 2) return@withContext emptyList()

            val acceptLang = when (lang) {
                "ar" -> "ar,fr,en"
                "fr" -> "fr,ar,en"
                else -> "en,fr,ar"
            }

            val url = "https://nominatim.openstreetmap.org/search".toHttpUrl().newBuilder()
                .addQueryParameter("format", "jsonv2")
                .addQueryParameter("q", trimmed)
                .addQueryParameter("limit", "6")
                .addQueryParameter("addressdetails", "1")
                .build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ObsidianAndroidApp/1.0")
                .header("Accept-Language", acceptLang)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) return@withContext emptyList()
                    val array = JSONArray(body)
                    val results = ArrayList<GeocodePlace>(array.length())
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val lat = obj.optString("lat").toDoubleOrNull() ?: continue
                        val lon = obj.optString("lon").toDoubleOrNull() ?: continue
                        if (!lat.isFinite() || !lon.isFinite()) continue
                        val display = stripTifinagh(obj.optString("display_name", ""))
                        val name = stripTifinagh(obj.optString("name", ""))
                        val shortLabel = buildShortLabel(obj.optJSONObject("address"), name, display, lat, lon)
                        results.add(
                            GeocodePlace(
                                displayName = display.ifBlank { shortLabel },
                                shortLabel = shortLabel,
                                lat = lat,
                                lng = lon
                            )
                        )
                    }
                    results
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

    suspend fun reverseGeocode(lat: Double, lng: Double, lang: String = "en"): String =
        withContext(Dispatchers.IO) {
            if (!lat.isFinite() || !lng.isFinite()) return@withContext ""
            val fallback = String.format(Locale.US, "%.4f, %.4f", lat, lng)
            val acceptLang = when (lang) {
                "ar" -> "ar,fr,en"
                "fr" -> "fr,ar,en"
                else -> "en,fr,ar"
            }

            val url = "https://nominatim.openstreetmap.org/reverse".toHttpUrl().newBuilder()
                .addQueryParameter("format", "jsonv2")
                .addQueryParameter("lat", String.format(Locale.US, "%.6f", lat))
                .addQueryParameter("lon", String.format(Locale.US, "%.6f", lng))
                .addQueryParameter("zoom", "16")
                .addQueryParameter("addressdetails", "1")
                .build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ObsidianAndroidApp/1.0")
                .header("Accept-Language", acceptLang)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext fallback
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) return@withContext fallback
                    val obj = JSONObject(body)
                    val display = stripTifinagh(obj.optString("display_name", ""))
                    val name = stripTifinagh(obj.optString("name", ""))
                    val shortLabel = buildShortLabel(obj.optJSONObject("address"), name, display, lat, lonFallback = lng)
                    shortLabel.ifBlank { display.ifBlank { fallback } }
                }
            } catch (_: Exception) {
                fallback
            }
        }

    internal fun buildShortLabel(
        address: JSONObject?,
        name: String,
        display: String,
        lat: Double,
        lonFallback: Double
    ): String {
        if (address != null) {
            val parts = listOf(
                address.optString("neighbourhood", ""),
                address.optString("suburb", ""),
                address.optString("city_district", ""),
                address.optString("town", ""),
                address.optString("city", ""),
                address.optString("village", ""),
                address.optString("state", ""),
                address.optString("country", "")
            )
                .map { stripTifinagh(it) }
                .filter { it.isNotBlank() }
                .distinct()

            val primaryLocality = parts.take(2)
            val country = parts.lastOrNull()
            val combined = buildList {
                if (name.isNotBlank() && !primaryLocality.any { it.equals(name, ignoreCase = true) }) {
                    add(name)
                }
                addAll(primaryLocality)
                if (country != null && !contains(country)) {
                    add(country)
                }
            }.take(3)

            if (combined.isNotEmpty()) {
                return combined.joinToString(", ")
            }
        }

        if (display.isNotBlank()) {
            val segments = display.split(",")
                .map { stripTifinagh(it) }
                .filter { it.isNotBlank() }
                .take(3)
            if (segments.isNotEmpty()) {
                return segments.joinToString(", ")
            }
        }

        return String.format(Locale.US, "%.4f, %.4f", lat, lonFallback)
    }
}
