package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import com.example.data.flame.FlameManager
import com.example.data.local.SecurityPreferences
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import com.example.data.profile.ProfileManager
import com.example.data.repository.IndividualRepository
import com.example.data.roadmap.RoadmapManager
import com.example.data.roadmap.XpCalculationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupResult(
    val success: Boolean,
    val message: String,
    val individualsCount: Int = 0,
    val strikeEventsCount: Int = 0
)

class BackupManager(private val repository: IndividualRepository) {

    companion object {
        private const val MAX_BACKUP_CHARS = 25 * 1024 * 1024
        private val SUPPORTED_APP_IDS = setOf("Obsidian", "StrikePulse")
    }

    private fun finiteNonNegative(value: Double, fallback: Double = 0.0): Double =
        if (value.isFinite() && value >= 0.0) value else fallback

    /**
     * Exports a 100% complete, literal snapshot of the entire StrikePulse application state:
     * - Room DB (`individuals` & `strike_events`)
     * - Flame score, stats, shields, dates & infinite flame logs (`flame_state`)
     * - 500-station roadmap XP, claimed stations, all D/W/M & Annual archives (`roadmap_state`)
     * - Inventory potions (including Eternal Resurgence), skip keys & permanent unlocks (`profile_state`)
     * - Security & user preferences (`preferences_state`)
     */
    suspend fun exportBackup(
        context: Context,
        destinationUri: Uri,
        lang: String = "ar"
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val individuals = repository.getAllIndividualsList()
            val strikeEvents = repository.getAllStrikeEventsList()
            val flameSnapshot = FlameManager.exportSnapshot(context)
            val roadmapSnapshot = RoadmapManager.exportSnapshot(context)
            val xpEngineSnapshot = XpCalculationEngine.exportSnapshot(context)
            val profileSnapshot = ProfileManager.exportSnapshot(context)
            val prefsSnapshot = SecurityPreferences(context).exportSnapshot()

            val rootJson = JSONObject().apply {
                put("app", "Obsidian")
                put("backup_schema_version", 2)
                put("version_code", BuildConfig.VERSION_CODE)
                put("version_name", BuildConfig.VERSION_NAME)
                put("export_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                put("export_timestamp", System.currentTimeMillis())
                put("individuals_count", individuals.size)
                put("strike_events_count", strikeEvents.size)

                // 1. Individuals Array
                val individualsArray = JSONArray()
                for (item in individuals) {
                    val obj = JSONObject().apply {
                        put("id", item.id)
                        put("fullName", item.fullName)
                        put("type", item.type)
                        put("length", item.length)
                        put("width", item.width)
                        put("size", item.size)
                        put("sScore", item.sScore)
                        put("fScore", item.fScore)
                        put("cScore", item.cScore)
                        put("boost", item.boost)
                        put("strikeCount", item.strikeCount)
                        put("phoneNumber", item.phoneNumber)
                        put("updatedAt", item.updatedAt)
                        if (item.firstStrikeLat != null && item.firstStrikeLat.isFinite()) {
                            put("firstStrikeLat", item.firstStrikeLat)
                        }
                        if (item.firstStrikeLng != null && item.firstStrikeLng.isFinite()) {
                            put("firstStrikeLng", item.firstStrikeLng)
                        }
                        put("firstStrikePlaceName", item.firstStrikePlaceName)
                    }
                    individualsArray.put(obj)
                }
                put("individuals", individualsArray)

                // 2. Strike Events Array
                val eventsArray = JSONArray()
                for (event in strikeEvents) {
                    val obj = JSONObject().apply {
                        put("id", event.id)
                        put("individualId", event.individualId)
                        put("individualName", event.individualName)
                        put("year", event.year)
                        put("date", event.date)
                        put("time", event.time)
                        put("count", event.count)
                        put("strikeType", event.strikeType)
                        put("performanceValue", event.performanceValue)
                        put("notes", event.notes)
                        put("timestamp", event.timestamp)
                    }
                    eventsArray.put(obj)
                }
                put("strike_events", eventsArray)

                // 3. Complete Literal State Snapshots
                put("flame_state", flameSnapshot)
                put("roadmap_state", roadmapSnapshot)
                put("xp_engine_state", xpEngineSnapshot)
                put("profile_state", profileSnapshot)
                put("preferences_state", prefsSnapshot)
            }

            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(rootJson.toString(2))
                    writer.flush()
                }
            } ?: return@withContext BackupResult(
                success = false,
                message = if (lang == "ar") "تعذّر فتح ملف الوجهة للكتابة على Drive" else "Failed to open destination file on Drive"
            )

            val successMsg = if (lang == "ar") {
                "تم حفظ النسخة الاحتياطية الكاملة بنجاح (${individuals.size} فرد، ${strikeEvents.size} سترايك، وكامل التقدم والمخزون والألقاب)"
            } else {
                "Full backup saved to Drive (${individuals.size} profiles, ${strikeEvents.size} strikes, and all progress, inventory & unlocks)"
            }
            BackupResult(
                success = true,
                message = successMsg,
                individualsCount = individuals.size,
                strikeEventsCount = strikeEvents.size
            )
        } catch (_: Exception) {
            val errMsg = if (lang == "ar") {
                "فشل حفظ النسخة الاحتياطية؛ تعذّر الوصول إلى ملف الوجهة."
            } else {
                "Backup export failed; unable to write to destination file."
            }
            BackupResult(false, errMsg)
        }
    }

    /**
     * Reads a complete literal backup JSON from Google Drive (or storage) and restores Room +
     * FlameManager + RoadmapManager + ProfileManager + SecurityPreferences seamlessly.
     */
    suspend fun importBackup(
        context: Context,
        sourceUri: Uri,
        replaceExisting: Boolean,
        lang: String = "ar"
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    val sb = StringBuilder()
                    val buffer = CharArray(8192)
                    var totalChars = 0
                    while (true) {
                        val read = reader.read(buffer)
                        if (read == -1) break
                        totalChars += read
                        if (totalChars > MAX_BACKUP_CHARS) {
                            return@withContext BackupResult(
                                success = false,
                                message = if (lang == "ar") {
                                    "حجم ملف النسخة الاحتياطية يتجاوز الحد الأقصى المسموح به."
                                } else {
                                    "Backup file exceeds the maximum allowed size."
                                }
                            )
                        }
                        sb.append(buffer, 0, read)
                    }
                    sb.toString()
                }
            } ?: return@withContext BackupResult(
                success = false,
                message = if (lang == "ar") "تعذّر قراءة ملف النسخة الاحتياطية من Drive" else "Failed to read backup file from Drive"
            )

            val rootJson = JSONObject(jsonString)
            val appHeader = rootJson.optString("app", "").trim()
            if (appHeader.isNotEmpty() && appHeader !in SUPPORTED_APP_IDS) {
                return@withContext BackupResult(
                    success = false,
                    message = if (lang == "ar") "ملف النسخة الاحتياطية لا يخص هذا التطبيق." else "Selected backup file does not belong to Obsidian."
                )
            }

            val individualsJson = rootJson.optJSONArray("individuals") ?: JSONArray()
            val eventsJson = rootJson.optJSONArray("strike_events") ?: JSONArray()

            val flameState = rootJson.optJSONObject("flame_state")
            val roadmapState = rootJson.optJSONObject("roadmap_state")
            val xpEngineState = rootJson.optJSONObject("xp_engine_state")
            val profileState = rootJson.optJSONObject("profile_state")
            val prefsState = rootJson.optJSONObject("preferences_state")

            val hasFullState = flameState != null || roadmapState != null || xpEngineState != null || profileState != null || prefsState != null
            if (individualsJson.length() == 0 && eventsJson.length() == 0 && !hasFullState) {
                return@withContext BackupResult(
                    success = false,
                    message = if (lang == "ar") "الملف المحدد فارغ أو لا يحتوي على بيانات صالحة." else "Selected file is empty or contains no valid Obsidian data."
                )
            }

            val individualsList = mutableListOf<Individual>()
            for (i in 0 until individualsJson.length()) {
                val obj = individualsJson.getJSONObject(i)
                val length = finiteNonNegative(obj.optDouble("length", 0.0))
                val width = finiteNonNegative(obj.optDouble("width", 0.0))
                val sScore = finiteNonNegative(obj.optDouble("sScore", 0.0))
                val fScore = finiteNonNegative(obj.optDouble("fScore", 0.0))
                val cScore = finiteNonNegative(obj.optDouble("cScore", 0.0))
                val rawSize = if (obj.has("size")) finiteNonNegative(obj.optDouble("size", 0.0)) else Individual.calculateSize(length, width)
                val rawBoost = if (obj.has("boost")) finiteNonNegative(obj.optDouble("boost", 0.0)) else Individual.calculateBoost(sScore, fScore, cScore, rawSize)

                val parsedLat = if (obj.has("firstStrikeLat") && !obj.isNull("firstStrikeLat")) {
                    obj.optDouble("firstStrikeLat", Double.NaN).takeIf { it.isFinite() && it in -90.0..90.0 }
                } else null
                val parsedLng = if (obj.has("firstStrikeLng") && !obj.isNull("firstStrikeLng")) {
                    obj.optDouble("firstStrikeLng", Double.NaN).takeIf { it.isFinite() && it in -180.0..180.0 }
                } else null
                val parsedPlace = com.example.data.map.NominatimGeocoder.stripTifinagh(
                    obj.optString("firstStrikePlaceName", "").take(250)
                )

                individualsList.add(
                    Individual(
                        id = obj.optLong("id", 0L).coerceAtLeast(0L),
                        fullName = obj.optString("fullName", "Unnamed").ifBlank { "Unnamed" }.take(200),
                        type = obj.optString("type", "M").ifBlank { "M" }.take(20),
                        length = length,
                        width = width,
                        size = rawSize,
                        sScore = sScore,
                        fScore = fScore,
                        cScore = cScore,
                        boost = rawBoost,
                        strikeCount = obj.optInt("strikeCount", 0).coerceAtLeast(0),
                        phoneNumber = obj.optString("phoneNumber", "").take(64),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        firstStrikeLat = if (parsedLat != null && parsedLng != null) parsedLat else null,
                        firstStrikeLng = if (parsedLat != null && parsedLng != null) parsedLng else null,
                        firstStrikePlaceName = parsedPlace
                    )
                )
            }

            val eventsList = mutableListOf<StrikeEvent>()
            for (i in 0 until eventsJson.length()) {
                val obj = eventsJson.getJSONObject(i)
                eventsList.add(
                    StrikeEvent(
                        id = obj.optLong("id", 0L).coerceAtLeast(0L),
                        individualId = obj.optLong("individualId", 1L).coerceAtLeast(1L),
                        individualName = obj.optString("individualName", "").take(200),
                        year = obj.optInt("year", 2026).coerceIn(1970, 2100),
                        date = obj.optString("date", "2026-01-01").take(32),
                        time = obj.optString("time", "12:00:00").take(32),
                        count = obj.optInt("count", 1).coerceAtLeast(1),
                        strikeType = obj.optString("strikeType", "Automatic").take(64),
                        performanceValue = finiteNonNegative(obj.optDouble("performanceValue", 0.0)),
                        notes = obj.optString("notes", "").take(1000),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            val result = repository.restoreBackup(individualsList, eventsList, replaceExisting)

            // Restore complete literal subsystems in deterministic order
            profileState?.let { ProfileManager.restoreSnapshot(context, it, replaceExisting) }
            flameState?.let { FlameManager.restoreSnapshot(context, it, replaceExisting) }
            roadmapState?.let { RoadmapManager.restoreSnapshot(context, it, replaceExisting) }
            xpEngineState?.let { XpCalculationEngine.restoreSnapshot(context, it, replaceExisting) }
            if (replaceExisting && prefsState != null) {
                SecurityPreferences(context).restoreSnapshot(prefsState)
            }

            val msg = if (lang == "ar") {
                "تم استرجاع النسخة الاحتياطية الكاملة بنجاح: ${result.first} فرد و ${result.second} سترايك مع استعادة كافة المستويات والمخزون والألقاب"
            } else {
                "Full backup restored successfully: ${result.first} profiles & ${result.second} strikes along with all stats, tasks, inventory & unlocks"
            }
            BackupResult(
                success = true,
                message = msg,
                individualsCount = result.first,
                strikeEventsCount = result.second
            )
        } catch (_: Exception) {
            val errMsg = if (lang == "ar") {
                "ملف النسخة الاحتياطية غير صالح أو تالف."
            } else {
                "Invalid or corrupted backup file."
            }
            BackupResult(false, errMsg)
        }
    }
}
