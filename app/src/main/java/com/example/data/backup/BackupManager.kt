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
            val profileSnapshot = ProfileManager.exportSnapshot(context)
            val prefsSnapshot = SecurityPreferences(context).exportSnapshot()

            val rootJson = JSONObject().apply {
                put("app", "StrikePulse")
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
        } catch (e: Exception) {
            val errMsg = if (lang == "ar") {
                "فشل حفظ النسخة الاحتياطية: ${e.localizedMessage}"
            } else {
                "Backup export failed: ${e.localizedMessage}"
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
                    reader.readText()
                }
            } ?: return@withContext BackupResult(
                success = false,
                message = if (lang == "ar") "تعذّر قراءة ملف النسخة الاحتياطية من Drive" else "Failed to read backup file from Drive"
            )

            val rootJson = JSONObject(jsonString)

            val individualsJson = rootJson.optJSONArray("individuals") ?: JSONArray()
            val eventsJson = rootJson.optJSONArray("strike_events") ?: JSONArray()

            val flameState = rootJson.optJSONObject("flame_state")
            val roadmapState = rootJson.optJSONObject("roadmap_state")
            val profileState = rootJson.optJSONObject("profile_state")
            val prefsState = rootJson.optJSONObject("preferences_state")

            val hasFullState = flameState != null || roadmapState != null || profileState != null || prefsState != null
            if (individualsJson.length() == 0 && eventsJson.length() == 0 && !hasFullState) {
                return@withContext BackupResult(
                    success = false,
                    message = if (lang == "ar") "الملف المحدد فارغ أو لا يحتوي على بيانات صالحة." else "Selected file is empty or contains no valid StrikePulse data."
                )
            }

            val individualsList = mutableListOf<Individual>()
            for (i in 0 until individualsJson.length()) {
                val obj = individualsJson.getJSONObject(i)
                val length = obj.optDouble("length", 0.0)
                val width = obj.optDouble("width", 0.0)
                val sScore = obj.optDouble("sScore", 0.0)
                val fScore = obj.optDouble("fScore", 0.0)
                val cScore = obj.optDouble("cScore", 0.0)
                val size = if (obj.has("size")) obj.optDouble("size") else Individual.calculateSize(length, width)
                val boost = if (obj.has("boost")) obj.optDouble("boost") else Individual.calculateBoost(sScore, fScore, cScore, size)

                individualsList.add(
                    Individual(
                        id = obj.optLong("id", 0L),
                        fullName = obj.optString("fullName", "Unnamed"),
                        type = obj.optString("type", "M"),
                        length = length,
                        width = width,
                        size = size,
                        sScore = sScore,
                        fScore = fScore,
                        cScore = cScore,
                        boost = boost,
                        strikeCount = obj.optInt("strikeCount", 0),
                        phoneNumber = obj.optString("phoneNumber", ""),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val eventsList = mutableListOf<StrikeEvent>()
            for (i in 0 until eventsJson.length()) {
                val obj = eventsJson.getJSONObject(i)
                eventsList.add(
                    StrikeEvent(
                        id = obj.optLong("id", 0L),
                        individualId = obj.optLong("individualId", 1L),
                        individualName = obj.optString("individualName", ""),
                        year = obj.optInt("year", 2026),
                        date = obj.optString("date", "2026-01-01"),
                        time = obj.optString("time", "12:00:00"),
                        count = obj.optInt("count", 1),
                        strikeType = obj.optString("strikeType", "Automatic"),
                        performanceValue = obj.optDouble("performanceValue", 0.0),
                        notes = obj.optString("notes", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            val result = repository.restoreBackup(individualsList, eventsList, replaceExisting)

            // Restore complete literal subsystems in deterministic order
            profileState?.let { ProfileManager.restoreSnapshot(context, it, replaceExisting) }
            flameState?.let { FlameManager.restoreSnapshot(context, it, replaceExisting) }
            roadmapState?.let { RoadmapManager.restoreSnapshot(context, it, replaceExisting) }
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
        } catch (e: Exception) {
            val errMsg = if (lang == "ar") {
                "ملف غير صالح أو حدث خطأ أثناء الاسترجاع: ${e.localizedMessage}"
            } else {
                "Invalid backup file or restore error: ${e.localizedMessage}"
            }
            BackupResult(false, errMsg)
        }
    }
}
