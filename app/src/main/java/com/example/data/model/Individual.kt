package com.example.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.text.DecimalFormat

@Entity(tableName = "individuals")
data class Individual(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val type: String, // "M", "G", "TS", "FG"
    val length: Double,
    val width: Double,
    val size: Double, // length * width
    val sScore: Double,
    val fScore: Double,
    val cScore: Double,
    val boost: Double, // (sScore + fScore + cScore) + (size / 100.0)
    val strikeCount: Int = 0,
    val phoneNumber: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Score = boost * strikeCount (النتيجة = ضرب boost في strike)
    val totalScore: Double
        get() = calculateTotalScore(boost, strikeCount)

    companion object {
        val VALID_TYPES = listOf("M", "G", "TS", "FG")
        private val df = DecimalFormat("#.##")

        fun calculateSize(length: Double, width: Double): Double {
            return (length * width * 100.0).toLong() / 100.0
        }

        fun calculateBoost(sScore: Double, fScore: Double, cScore: Double, size: Double): Double {
            val totalScore = sScore + fScore + cScore
            val sizeComponent = size / 100.0
            return ((totalScore + sizeComponent) * 100.0).toLong() / 100.0
        }

        fun calculateTotalScore(boost: Double, strikes: Int): Double {
            return ((boost * strikes) * 100.0).toLong() / 100.0
        }

        fun formatNumber(value: Double): String {
            return df.format(value)
        }
    }
}
