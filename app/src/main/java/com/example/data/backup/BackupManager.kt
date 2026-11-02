package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import com.example.data.model.Individual
import com.example.data.model.StrikeEvent
import com.example.data.repository.IndividualRepository
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
     * Exports all database records (individuals & strike events) to a JSON file at destinationUri (Google Drive or local storage).
     */
    suspend fun exportBackup(context: Context, destinationUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val individuals = repository.getAllIndividualsList()
            val strikeEvents = repository.getAllStrikeEventsList()

            val rootJson = JSONObject().apply {
                put("app", "StrikePulse")
                put("version_code", BuildConfig.VERSION_CODE)
                put("version_name", BuildConfig.VERSION_NAME)
                put("export_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                put("export_timestamp", System.currentTimeMillis())
                put("individuals_count", individuals.size)
                put("strike_events_count", strikeEvents.size)

                // Individuals Array
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
                        put("updatedAt", item.updatedAt)
                    }
                    individualsArray.put(obj)
                }
                put("individuals", individualsArray)

                // Strike Events Array
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
            }

            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(rootJson.toString(2))
                    writer.flush()
                }
            } ?: return@withContext BackupResult(false, "تعذر فتح ملف الوجهة للكتابة على Drive")

            BackupResult(
                success = true,
                message = "تم حفظ النسخة الاحتياطية بنجاح على Google Drive (${individuals.size} سجل و ${strikeEvents.size} ضربة)",
                individualsCount = individuals.size,
                strikeEventsCount = strikeEvents.size
            )
        } catch (e: Exception) {
            BackupResult(false, "فشل حفظ النسخة الاحتياطية: ${e.localizedMessage}")
        }
    }

    /**
     * Reads a backup JSON file from Google Drive (sourceUri) and restores all individuals and strike events into Room.
     */
    suspend fun importBackup(
        context: Context,
        sourceUri: Uri,
        replaceExisting: Boolean
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext BackupResult(false, "تعذر قراءة ملف النسخة الاحتياطية من Drive")

            val rootJson = JSONObject(jsonString)

            val individualsJson = rootJson.optJSONArray("individuals") ?: JSONArray()
            val eventsJson = rootJson.optJSONArray("strike_events") ?: JSONArray()

            if (individualsJson.length() == 0 && eventsJson.length() == 0) {
                return@withContext BackupResult(false, "الملف المحدد فارغ أو لا يحتوي على سجلات صالحة.")
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

            BackupResult(
                success = true,
                message = "تم استرجاع البيانات بنجاح: ${result.first} سجل و ${result.second} ضربة",
                individualsCount = result.first,
                strikeEventsCount = result.second
            )
        } catch (e: Exception) {
            BackupResult(false, "ملف غير صالح أو حدث خطأ أثناء قراءة الملف: ${e.localizedMessage}")
        }
    }
}
