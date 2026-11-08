package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "strike_events",
    foreignKeys = [
        ForeignKey(
            entity = Individual::class,
            parentColumns = ["id"],
            childColumns = ["individualId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["individualId"])]
)
data class StrikeEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val individualId: Long,
    val individualName: String,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val date: String = currentDateString(), // e.g. "2026-11-01"
    val time: String = currentTimeString(), // e.g. "20:55:00"
    val count: Int = 1, // Number of strikes in this event (e.g. 1, 5, 10 or historical batch)
    val strikeType: String = "Automatic", // "Automatic", "Quick +5", "Quick +10", "Historical"
    val performanceValue: Double = 0.0, // Impact metric / boost
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getEffectivePenalty(): Double {
        if (notes.contains("PENALTY:")) {
            val part = notes.substringAfter("PENALTY:").substringBefore("|").toDoubleOrNull()
            if (part != null) return part
        }
        return if (performanceValue > 0.0) performanceValue else 0.0
    }

    companion object {
        val STRIKE_TYPES = listOf("Automatic", "Quick +1", "Quick +5", "Quick +10", "Historical", "Power", "Precision")

        fun currentDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        fun currentTimeString(): String {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date())
        }

        fun currentYear(): Int {
            return Calendar.getInstance().get(Calendar.YEAR)
        }
    }
}
