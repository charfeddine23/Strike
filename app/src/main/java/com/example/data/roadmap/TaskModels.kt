package com.example.data.roadmap

import com.example.data.profile.SkipKeyType

/**
 * Task Categories & Data Models for the Dynamic Task Engine & XP Progression:
 * - Daily Tasks (5 active slots, new tasks unlock at 12:00 AM midnight)
 * - Weekly Tasks (7 active slots, grant day is Sunday 12:00 AM)
 * - Monthly Tasks (10 active slots, grant day is the 1st of each month 12:00 AM)
 * - Eternal / Annual Tasks (250 long-term goals; upon collection they transfer to the Tasks Ledger)
 */

enum class TaskPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    EVERLASTING
}

enum class TaskStatus {
    PENDING,
    READY_TO_CLAIM, // Target reached via user activity; stays visible until user explicitly clicks Collect ("جمع")
    COMPLETED,
    SKIPPED
}

enum class RewardType {
    SHIELD_24H,
    SHIELD_48H,
    SHIELD_72H,
    KEY_WOODEN,
    KEY_BRONZE,
    KEY_SILVER,
    POTION_PULSE_ELIXIR,
    POTION_SHADOW_SERUM,
    POTION_GABRIEL_DROPLET,
    POTION_CRYO_INFUSION,
    POTION_ETERNAL_REBIRTH,
    REWARD_AVATAR,
    REWARD_TITLE
}

data class StationReward(
    val type: RewardType,
    val title: String,
    val description: String,
    val iconName: String,
    val quantity: Int = 1,
    val titleEn: String = title,
    val descriptionEn: String = description
) {
    fun getLocalizedTitle(lang: String): String = if (lang == "ar") title else titleEn
    fun getLocalizedDescription(lang: String): String = if (lang == "ar") description else descriptionEn
}

enum class StationState {
    LOCKED,
    UNLOCKED, // Ready to claim
    CLAIMED
}

data class RoadmapStation(
    val id: Int, // 1..500
    val requiredTotalXp: Long,
    val reward: StationReward,
    val state: StationState = StationState.LOCKED
)

data class TaskItem(
    val id: String,
    val period: TaskPeriod,
    val title: String,
    val description: String,
    val xpBounty: Int,
    val targetCount: Int = 1,
    val currentCount: Int = 0,
    val status: TaskStatus = TaskStatus.PENDING,
    val requiredSkipKey: SkipKeyType = SkipKeyType.WOODEN,
    val actionTag: String = "GENERAL",
    val difficultyTier: Int = 1,
    val titleEn: String = title,
    val descriptionEn: String = description,
    val unlockAtMillis: Long = 0L // 0L = unlocked immediately; > nowMillis = locked on cycle cooldown
) {
    val isReadyToClaim: Boolean get() = status == TaskStatus.READY_TO_CLAIM
    val isCompleted: Boolean get() = status == TaskStatus.COMPLETED || status == TaskStatus.SKIPPED
    val progressFraction: Float get() = (currentCount.toFloat() / targetCount.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
    fun isLocked(nowMillis: Long = System.currentTimeMillis()): Boolean = unlockAtMillis > nowMillis
    // Eternal / Annual tasks CANNOT be skipped with keys — genuine completion is strictly enforced
    val canBeSkipped: Boolean get() = period != TaskPeriod.YEARLY && period != TaskPeriod.EVERLASTING
    fun getLocalizedTitle(lang: String): String = if (lang == "ar") title else titleEn
    fun getLocalizedDescription(lang: String): String = if (lang == "ar") description else descriptionEn
}

/**
 * Snapshot entry for the comprehensive Tasks Ledger ("دفتر المهام") showing every task
 * across all 1,000 milestones (Completed, Ready-to-Claim, Active Pending, and Pool Pending).
 */
data class TaskLedgerEntry(
    val task: TaskItem,
    val completedAtMillis: Long? = null
)
