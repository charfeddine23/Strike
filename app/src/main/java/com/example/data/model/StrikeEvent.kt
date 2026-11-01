package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "strike_events")
data class StrikeEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val individualId: Long,
    val individualName: String,
    val date: String, // e.g. "2026-11-01"
    val strikeType: String, // "Standard", "Power", "Precision", "Boost"
    val performanceValue: Double, // Performance / Impact metric
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        val STRIKE_TYPES = listOf("Standard", "Power", "Precision", "Boost")

        fun currentDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}
