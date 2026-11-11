package com.example.data.roadmap

/**
 * Task Categories & Data Models for the Dynamic Task Engine & XP Progression:
 * - Daily Tasks (5 randomized via date-seeded shuffle)
 * - Weekly Tasks (7 core disciplinary tasks)
 * - Monthly Tasks (10 strategic milestones)
 * - Yearly Tasks (Epic long-term goals)
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
    COMPLETED,
    SKIPPED
}

enum class KeyRarity {
    COMMON,
    NORMAL,
    RARE,
    LEGENDARY
}

enum class RewardType {
    SHIELD_24H,
    SHIELD_48H,
    SHIELD_72H,
    KEY_COMMON,
    KEY_NORMAL,
    KEY_RARE,
    KEY_LEGENDARY,
    KEY_WOODEN,
    KEY_BRONZE,
    KEY_SILVER,
    POTION_PULSE_ELIXIR,
    POTION_SHADOW_SERUM,
    POTION_GABRIEL_DROPLET,
    POTION_CRYO_INFUSION,
    POTION_ETERNAL_REBIRTH,
    REWARD_AVATAR,
    REWARD_TITLE,
    GIRL_BOOST_HAMMER_7D,
    DEFLAME_BOOST_HAMMER_7D,
    THOR_HAMMER_30D
}

data class StationReward(
    val type: RewardType,
    val title: String,
    val description: String,
    val iconName: String,
    val quantity: Int = 1
)

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
    val requiredKeyRarity: KeyRarity = KeyRarity.COMMON
) {
    val isCompleted: Boolean get() = status == TaskStatus.COMPLETED || status == TaskStatus.SKIPPED || currentCount >= targetCount
    val progressFraction: Float get() = (currentCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
}
