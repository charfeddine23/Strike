package com.example.data.roadmap

import android.content.Context
import android.content.SharedPreferences
import com.example.data.flame.FlameManager
import com.example.data.profile.ProfileManager
import com.example.data.profile.SkipKeyType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Singleton state and persistence manager for:
 * - 500-Station Wavy Roadmap & Station Claims (Exponential XP scaling from 50 to 434,725 XP)
 * - 1000-Task Matrix & Progression Engine
 * - Non-Repeating Consumption Rotation Engine (Daily, Weekly, Monthly)
 * - Master Goals Interface (Everlasting Tasks)
 * - Cyber Inventory (Skip Keys & Protective Shields)
 */
object RoadmapManager {

    private const val PREFS_NAME = "strikepulse_roadmap_prefs"

    private const val KEY_TOTAL_XP = "key_total_xp"
    private const val KEY_CLAIMED_STATIONS = "key_claimed_stations"
    private const val KEY_COMPLETED_TASK_IDS = "key_completed_task_ids"
    private const val KEY_LAST_DAILY_DATE = "key_last_daily_date"
    private const val KEY_LAST_WEEKLY_CODE = "key_last_weekly_code"
    private const val KEY_LAST_MONTHLY_CODE = "key_last_monthly_code"
    private const val KEY_DAILY_TASKS_JSON = "key_daily_tasks_json"
    private const val KEY_WEEKLY_TASKS_JSON = "key_weekly_tasks_json"
    private const val KEY_MONTHLY_TASKS_JSON = "key_monthly_tasks_json"

    // Shields Inventory
    private const val KEY_SHIELDS_24H = "key_shields_24h"
    private const val KEY_SHIELDS_48H = "key_shields_48h"
    private const val KEY_SHIELDS_72H = "key_shields_72h"

    // Flows
    private val _totalXp = MutableStateFlow(50L) // Default initial XP starts at Station 1
    val totalXp: StateFlow<Long> = _totalXp.asStateFlow()

    private val _claimedStations = MutableStateFlow<Set<Int>>(emptySet())
    val claimedStations: StateFlow<Set<Int>> = _claimedStations.asStateFlow()

    private val _completedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    val completedTaskIds: StateFlow<Set<String>> = _completedTaskIds.asStateFlow()

    private val _dailyTasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val dailyTasks: StateFlow<List<TaskItem>> = _dailyTasks.asStateFlow()

    private val _weeklyTasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val weeklyTasks: StateFlow<List<TaskItem>> = _weeklyTasks.asStateFlow()

    private val _monthlyTasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val monthlyTasks: StateFlow<List<TaskItem>> = _monthlyTasks.asStateFlow()

    private val _yearlyTasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val yearlyTasks: StateFlow<List<TaskItem>> = _yearlyTasks.asStateFlow()
    val everlastingTasks: StateFlow<List<TaskItem>> = _yearlyTasks.asStateFlow()

    fun getRemainingPoolCount(period: TaskPeriod): Int {
        val completed = _completedTaskIds.value
        return when (period) {
            TaskPeriod.DAILY -> TaskPools.ALL_DAILY_TASKS.count { !completed.contains(it.id) }
            TaskPeriod.WEEKLY -> TaskPools.ALL_WEEKLY_TASKS.count { !completed.contains(it.id) }
            TaskPeriod.MONTHLY -> TaskPools.ALL_MONTHLY_TASKS.count { !completed.contains(it.id) }
            TaskPeriod.YEARLY, TaskPeriod.EVERLASTING -> TaskPools.ALL_EVERLASTING_TASKS.count { !completed.contains(it.id) }
        }
    }

    // Inventory Skip Keys (Single source of truth via ProfileManager: Wooden, Bronze, Silver)
    val woodenKeysCount: StateFlow<Int> get() = ProfileManager.woodenKeysCount
    val bronzeKeysCount: StateFlow<Int> get() = ProfileManager.bronzeKeysCount
    val silverKeysCount: StateFlow<Int> get() = ProfileManager.silverKeysCount

    // Shields Inventory (Initialized to 0 - earned strictly via tasks and rewards)
    private val _shields24h = MutableStateFlow(0)
    val shields24h: StateFlow<Int> = _shields24h.asStateFlow()

    private val _shields48h = MutableStateFlow(0)
    val shields48h: StateFlow<Int> = _shields48h.asStateFlow()

    private val _shields72h = MutableStateFlow(0)
    val shields72h: StateFlow<Int> = _shields72h.asStateFlow()

    private var isInitialized = false

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        ProfileManager.init(context)
        val prefs = getPrefs(context)

        _totalXp.value = prefs.getLong(KEY_TOTAL_XP, 50L)

        // Read claimed stations
        val claimedStrSet = prefs.getStringSet(KEY_CLAIMED_STATIONS, emptySet()) ?: emptySet()
        _claimedStations.value = claimedStrSet.mapNotNull { it.toIntOrNull() }.toSet()

        // Read permanently completed task IDs
        val completedStrSet = prefs.getStringSet(KEY_COMPLETED_TASK_IDS, emptySet()) ?: emptySet()
        _completedTaskIds.value = completedStrSet

        // Ensure shields inventory starts clean at 0 (or loads saved non-default earned inventory)
        val hasResetInventory = prefs.getBoolean("has_reset_inventory_to_zero_v3", false)
        if (!hasResetInventory) {
            _shields24h.value = 0
            _shields48h.value = 0
            _shields72h.value = 0
            prefs.edit()
                .putInt(KEY_SHIELDS_24H, 0)
                .putInt(KEY_SHIELDS_48H, 0)
                .putInt(KEY_SHIELDS_72H, 0)
                .putBoolean("has_reset_inventory_to_zero_v3", true)
                .apply()
        } else {
            _shields24h.value = prefs.getInt(KEY_SHIELDS_24H, 0)
            _shields48h.value = prefs.getInt(KEY_SHIELDS_48H, 0)
            _shields72h.value = prefs.getInt(KEY_SHIELDS_72H, 0)
        }

        // 0. Cache Versioning & Legacy Keys/Tasks Purge:
        // Evicts cached task JSONs from SharedPreferences to load Inventory SkipKeyType tasks cleanly
        val tasksCacheVersion = prefs.getInt("key_tasks_matrix_version", 0)
        if (tasksCacheVersion < 7) {
            prefs.edit()
                .remove("key_keys_common")
                .remove("key_keys_normal")
                .remove("key_keys_rare")
                .remove("key_keys_legendary")
                .remove(KEY_DAILY_TASKS_JSON)
                .remove(KEY_WEEKLY_TASKS_JSON)
                .remove(KEY_MONTHLY_TASKS_JSON)
                .remove(KEY_LAST_DAILY_DATE)
                .remove(KEY_LAST_WEEKLY_CODE)
                .remove(KEY_LAST_MONTHLY_CODE)
                .putInt("key_tasks_matrix_version", 7)
                .apply()
        }

        // 1. Evaluate Daily Tasks (Seeded by date & non-repeating consumption)
        val today = TaskPools.todayDateString()
        val lastDate = prefs.getString(KEY_LAST_DAILY_DATE, null)
        if (lastDate != today) {
            val freshTasks = TaskPools.getRotatedDailyTasks(_completedTaskIds.value, today)
            _dailyTasks.value = freshTasks
            saveTasks(context, KEY_DAILY_TASKS_JSON, freshTasks)
            prefs.edit().putString(KEY_LAST_DAILY_DATE, today).apply()
        } else {
            _dailyTasks.value = loadTasks(context, KEY_DAILY_TASKS_JSON) ?: TaskPools.getRotatedDailyTasks(_completedTaskIds.value, today)
        }

        // 2. Evaluate Weekly Tasks (Seeded by week & non-repeating consumption)
        val currentWeek = TaskPools.currentWeekString()
        val lastWeek = prefs.getString(KEY_LAST_WEEKLY_CODE, null)
        if (lastWeek != currentWeek) {
            val freshWeekly = TaskPools.getRotatedWeeklyTasks(_completedTaskIds.value, currentWeek)
            _weeklyTasks.value = freshWeekly
            saveTasks(context, KEY_WEEKLY_TASKS_JSON, freshWeekly)
            prefs.edit().putString(KEY_LAST_WEEKLY_CODE, currentWeek).apply()
        } else {
            _weeklyTasks.value = loadTasks(context, KEY_WEEKLY_TASKS_JSON) ?: TaskPools.getRotatedWeeklyTasks(_completedTaskIds.value, currentWeek)
        }

        // 3. Evaluate Monthly Tasks (Seeded by month & non-repeating consumption)
        val currentMonth = TaskPools.currentMonthString()
        val lastMonth = prefs.getString(KEY_LAST_MONTHLY_CODE, null)
        if (lastMonth != currentMonth) {
            val freshMonthly = TaskPools.getRotatedMonthlyTasks(_completedTaskIds.value, currentMonth)
            _monthlyTasks.value = freshMonthly
            saveTasks(context, KEY_MONTHLY_TASKS_JSON, freshMonthly)
            prefs.edit().putString(KEY_LAST_MONTHLY_CODE, currentMonth).apply()
        } else {
            _monthlyTasks.value = loadTasks(context, KEY_MONTHLY_TASKS_JSON) ?: TaskPools.getRotatedMonthlyTasks(_completedTaskIds.value, currentMonth)
        }

        // 4. Everlasting / Master Goals Tasks (All 250 everlasting master goals displayed at once)
        _yearlyTasks.value = TaskPools.getEverlastingTasks(_completedTaskIds.value)

        isInitialized = true
    }

    @Synchronized
    fun resetForTesting(context: Context) {
        isInitialized = false
        init(context)
    }

    @Synchronized
    fun resetInventoryToZero(context: Context) {
        init(context)
        ProfileManager.resetSkipKeysToZero(context)
        _shields24h.value = 0
        _shields48h.value = 0
        _shields72h.value = 0
        getPrefs(context).edit()
            .putInt(KEY_SHIELDS_24H, 0)
            .putInt(KEY_SHIELDS_48H, 0)
            .putInt(KEY_SHIELDS_72H, 0)
            .apply()
    }

    /**
     * Hard reset / wipe of all task completion states across Daily, Weekly, Monthly, and Everlasting pools.
     * Triggered by 5-second long-press on the Profile icon in the top bar.
     */
    @Synchronized
    fun hardResetAllTaskCompletionStates(context: Context) {
        init(context)
        _completedTaskIds.value = emptySet()

        val today = TaskPools.todayDateString()
        val currentWeek = TaskPools.currentWeekString()
        val currentMonth = TaskPools.currentMonthString()

        val freshDaily = TaskPools.getRotatedDailyTasks(emptySet(), today)
        val freshWeekly = TaskPools.getRotatedWeeklyTasks(emptySet(), currentWeek)
        val freshMonthly = TaskPools.getRotatedMonthlyTasks(emptySet(), currentMonth)
        val freshEverlasting = TaskPools.getEverlastingTasks(emptySet())

        _dailyTasks.value = freshDaily
        _weeklyTasks.value = freshWeekly
        _monthlyTasks.value = freshMonthly
        _yearlyTasks.value = freshEverlasting

        getPrefs(context).edit()
            .remove(KEY_COMPLETED_TASK_IDS)
            .remove(KEY_DAILY_TASKS_JSON)
            .remove(KEY_WEEKLY_TASKS_JSON)
            .remove(KEY_MONTHLY_TASKS_JSON)
            .putString(KEY_LAST_DAILY_DATE, today)
            .putString(KEY_LAST_WEEKLY_CODE, currentWeek)
            .putString(KEY_LAST_MONTHLY_CODE, currentMonth)
            .apply()

        saveTasks(context, KEY_DAILY_TASKS_JSON, freshDaily)
        saveTasks(context, KEY_WEEKLY_TASKS_JSON, freshWeekly)
        saveTasks(context, KEY_MONTHLY_TASKS_JSON, freshMonthly)
    }

    /**
     * Literally resets all Roadmap levels, XP (back to 0 XP / Station 0), claimed stations,
     * completed tasks, and shield inventory counts back to zero.
     */
    @Synchronized
    fun resetAllLevelsToZero(context: Context) {
        init(context)
        _totalXp.value = 0L
        _claimedStations.value = emptySet()
        _completedTaskIds.value = emptySet()
        _shields24h.value = 0
        _shields48h.value = 0
        _shields72h.value = 0

        val today = TaskPools.todayDateString()
        val currentWeek = TaskPools.currentWeekString()
        val currentMonth = TaskPools.currentMonthString()

        val freshDaily = TaskPools.getRotatedDailyTasks(emptySet(), today)
        val freshWeekly = TaskPools.getRotatedWeeklyTasks(emptySet(), currentWeek)
        val freshMonthly = TaskPools.getRotatedMonthlyTasks(emptySet(), currentMonth)
        val freshEverlasting = TaskPools.getEverlastingTasks(emptySet())

        _dailyTasks.value = freshDaily
        _weeklyTasks.value = freshWeekly
        _monthlyTasks.value = freshMonthly
        _yearlyTasks.value = freshEverlasting

        getPrefs(context).edit()
            .putLong(KEY_TOTAL_XP, 0L)
            .remove(KEY_CLAIMED_STATIONS)
            .remove(KEY_COMPLETED_TASK_IDS)
            .remove(KEY_DAILY_TASKS_JSON)
            .remove(KEY_WEEKLY_TASKS_JSON)
            .remove(KEY_MONTHLY_TASKS_JSON)
            .putString(KEY_LAST_DAILY_DATE, today)
            .putString(KEY_LAST_WEEKLY_CODE, currentWeek)
            .putString(KEY_LAST_MONTHLY_CODE, currentMonth)
            .putInt(KEY_SHIELDS_24H, 0)
            .putInt(KEY_SHIELDS_48H, 0)
            .putInt(KEY_SHIELDS_72H, 0)
            .putBoolean("has_reset_inventory_to_zero_v3", true)
            .apply()

        saveTasks(context, KEY_DAILY_TASKS_JSON, freshDaily)
        saveTasks(context, KEY_WEEKLY_TASKS_JSON, freshWeekly)
        saveTasks(context, KEY_MONTHLY_TASKS_JSON, freshMonthly)
    }

    /**
     * Calculates the highest unlocked station (1 to 500) based on Total XP.
     * Follows the Exponential Curve: Station 1 = 50 XP, Station 500 = 434,725 XP.
     */
    fun calculateUnlockedStationId(currentXp: Long): Int {
        for (s in 500 downTo 1) {
            if (currentXp >= TaskPools.getRequiredXpForStation(s)) {
                return s
            }
        }
        return 0
    }

    fun generateStations(totalXp: Long, claimedStations: Set<Int>): List<RoadmapStation> {
        return (1..500).map { id ->
            val reqXp = TaskPools.getRequiredXpForStation(id)
            val state = when {
                claimedStations.contains(id) -> StationState.CLAIMED
                totalXp >= reqXp -> StationState.UNLOCKED
                else -> StationState.LOCKED
            }
            RoadmapStation(
                id = id,
                requiredTotalXp = reqXp,
                state = state,
                reward = TaskPools.getRewardForStation(id)
            )
        }
    }

    fun getStationState(stationId: Int): StationState {
        if (_claimedStations.value.contains(stationId)) return StationState.CLAIMED
        val reqXp = TaskPools.getRequiredXpForStation(stationId)
        return if (_totalXp.value >= reqXp) StationState.UNLOCKED else StationState.LOCKED
    }

    @Synchronized
    fun addXp(context: Context, amount: Int): Long {
        init(context)
        val newXp = (_totalXp.value + amount).coerceAtLeast(0L)
        _totalXp.value = newXp
        getPrefs(context).edit().putLong(KEY_TOTAL_XP, newXp).apply()
        return newXp
    }

    @Synchronized
    fun addKey(context: Context, keyType: SkipKeyType, qty: Int = 1) {
        init(context)
        ProfileManager.grantSkipKeys(context, keyType, qty)
    }

    @Synchronized
    fun claimStationReward(context: Context, stationId: Int): StationReward? {
        init(context)
        val state = getStationState(stationId)
        if (state != StationState.UNLOCKED) return null // Only UNLOCKED can be claimed

        val reward = TaskPools.getRewardForStation(stationId)
        val updatedSet = _claimedStations.value.toMutableSet()
        updatedSet.add(stationId)
        _claimedStations.value = updatedSet

        val strSet = updatedSet.map { it.toString() }.toSet()
        getPrefs(context).edit().putStringSet(KEY_CLAIMED_STATIONS, strSet).apply()

        // Distribute reward to inventory
        applyReward(context, reward)
        com.example.data.profile.ProfileManager.onStationUnlocked(context, stationId)
        onActionOccurred(context, "ROADMAP")
        return reward
    }

    private fun applyReward(context: Context, reward: StationReward) {
        when (reward.type) {
            RewardType.SHIELD_24H -> {
                // Tier 1: Mandatory and instantaneous activation upon receipt
                FlameManager.grant1DayShield(context, reward.quantity)
                FlameManager.activate1DayShield(context)
            }
            RewardType.SHIELD_48H -> {
                FlameManager.grant48hShield(context, reward.quantity)
                FlameManager.activate48hShield(context)
            }
            RewardType.SHIELD_72H -> {
                FlameManager.grantManualShield(context, days = 3)
                FlameManager.activateManualShield(context, days = 3)
            }
            RewardType.KEY_WOODEN -> {
                ProfileManager.grantSkipKeys(context, SkipKeyType.WOODEN, reward.quantity)
            }
            RewardType.KEY_BRONZE -> {
                ProfileManager.grantSkipKeys(context, SkipKeyType.BRONZE, reward.quantity)
            }
            RewardType.KEY_SILVER -> {
                ProfileManager.grantSkipKeys(context, SkipKeyType.SILVER, reward.quantity)
            }
            RewardType.POTION_PULSE_ELIXIR -> {
                ProfileManager.grantPotion(context, com.example.data.profile.PotionType.PULSE_ELIXIR, reward.quantity)
            }
            RewardType.POTION_SHADOW_SERUM -> {
                ProfileManager.grantPotion(context, com.example.data.profile.PotionType.SHADOW_GRIP_SERUM, reward.quantity)
            }
            RewardType.POTION_GABRIEL_DROPLET -> {
                // Instantly auto-activates upon receipt
                ProfileManager.grantPotion(context, com.example.data.profile.PotionType.GABRIEL_PURITY_DROPLET, reward.quantity)
            }
            RewardType.POTION_CRYO_INFUSION -> {
                ProfileManager.grantPotion(context, com.example.data.profile.PotionType.ABSOLUTE_CRYO_INFUSION, reward.quantity)
            }
            RewardType.POTION_ETERNAL_REBIRTH -> {
                ProfileManager.grantPotion(context, com.example.data.profile.PotionType.ETERNAL_REBIRTH_ELIXIR, reward.quantity)
            }
            RewardType.REWARD_AVATAR -> {
                // ProfileManager station checks handle avatar unlocking
            }
            RewardType.REWARD_TITLE -> {
                // ProfileManager station checks handle title unlocking
            }
        }
    }

    /**
     * Completes a task, awards its exact XP bounty, and permanently adds its ID to [completedTaskIds]
     * so it never repeats until the entire category pool is exhausted.
     */
    @Synchronized
    fun completeTask(context: Context, taskId: String): Boolean {
        init(context)
        var found = false
        var awardedXp = 0

        fun updateList(list: List<TaskItem>): List<TaskItem> {
            return list.map { item ->
                if (item.id == taskId && item.status == TaskStatus.PENDING) {
                    found = true
                    awardedXp = item.xpBounty
                    item.copy(status = TaskStatus.COMPLETED, currentCount = item.targetCount)
                } else item
            }
        }

        _dailyTasks.value = updateList(_dailyTasks.value).also { if (found) saveTasks(context, KEY_DAILY_TASKS_JSON, it) }
        if (!found) _weeklyTasks.value = updateList(_weeklyTasks.value).also { if (found) saveTasks(context, KEY_WEEKLY_TASKS_JSON, it) }
        if (!found) _monthlyTasks.value = updateList(_monthlyTasks.value).also { if (found) saveTasks(context, KEY_MONTHLY_TASKS_JSON, it) }
        if (!found) _yearlyTasks.value = updateList(_yearlyTasks.value)

        if (found) {
            val updatedCompleted = _completedTaskIds.value.toMutableSet()
            updatedCompleted.add(taskId)
            _completedTaskIds.value = updatedCompleted
            getPrefs(context).edit().putStringSet(KEY_COMPLETED_TASK_IDS, updatedCompleted).apply()

            if (awardedXp > 0) {
                addXp(context, awardedXp)
            }
        }
        return found
    }

    /**
     * Consumes an appropriate Inventory Skip Key (Wooden, Bronze, or Silver)
     * to instantly complete a task and award full XP.
     */
    @Synchronized
    fun useSkipKeyForTask(context: Context, taskId: String): Boolean {
        init(context)
        val allTasks = _dailyTasks.value + _weeklyTasks.value + _monthlyTasks.value + _yearlyTasks.value
        val task = allTasks.find { it.id == taskId } ?: return false
        if (task.status != TaskStatus.PENDING) return false
        if (!task.canBeSkipped) return false // STRICT: Annual / Yearly tasks CANNOT be skipped with keys

        val hasKey = ProfileManager.consumeSkipKey(context, task.requiredSkipKey)
        if (!hasKey) return false

        val prefs = getPrefs(context)
        var awardedXp = 0
        fun updateList(list: List<TaskItem>): List<TaskItem> {
            return list.map { item ->
                if (item.id == taskId) {
                    awardedXp = item.xpBounty
                    item.copy(status = TaskStatus.SKIPPED, currentCount = item.targetCount)
                } else item
            }
        }

        _dailyTasks.value = updateList(_dailyTasks.value).also { saveTasks(context, KEY_DAILY_TASKS_JSON, it) }
        _weeklyTasks.value = updateList(_weeklyTasks.value).also { saveTasks(context, KEY_WEEKLY_TASKS_JSON, it) }
        _monthlyTasks.value = updateList(_monthlyTasks.value).also { saveTasks(context, KEY_MONTHLY_TASKS_JSON, it) }
        _yearlyTasks.value = updateList(_yearlyTasks.value)

        val updatedCompleted = _completedTaskIds.value.toMutableSet()
        updatedCompleted.add(taskId)
        _completedTaskIds.value = updatedCompleted
        prefs.edit().putStringSet(KEY_COMPLETED_TASK_IDS, updatedCompleted).apply()

        if (awardedXp > 0) {
            addXp(context, awardedXp)
        }
        onActionOccurred(context, "KEY")
        return true
    }

    /**
     * Auto-increments progress for relevant matching tasks across active pools.
     */
    @Synchronized
    fun onActionOccurred(context: Context, actionType: String) {
        init(context)
        val newlyCompletedIds = mutableListOf<String>()

        fun incrementMatching(list: List<TaskItem>): List<TaskItem> {
            return list.map { item ->
                if (item.status == TaskStatus.PENDING && shouldIncrement(item, actionType)) {
                    val newCount = item.currentCount + 1
                    if (newCount >= item.targetCount) {
                        newlyCompletedIds.add(item.id)
                        addXp(context, item.xpBounty)
                        item.copy(currentCount = newCount, status = TaskStatus.COMPLETED)
                    } else {
                        item.copy(currentCount = newCount)
                    }
                } else item
            }
        }

        _dailyTasks.value = incrementMatching(_dailyTasks.value).also { saveTasks(context, KEY_DAILY_TASKS_JSON, it) }
        _weeklyTasks.value = incrementMatching(_weeklyTasks.value).also { saveTasks(context, KEY_WEEKLY_TASKS_JSON, it) }
        _monthlyTasks.value = incrementMatching(_monthlyTasks.value).also { saveTasks(context, KEY_MONTHLY_TASKS_JSON, it) }
        _yearlyTasks.value = incrementMatching(_yearlyTasks.value)

        if (newlyCompletedIds.isNotEmpty()) {
            val updatedCompleted = _completedTaskIds.value.toMutableSet()
            updatedCompleted.addAll(newlyCompletedIds)
            _completedTaskIds.value = updatedCompleted
            getPrefs(context).edit().putStringSet(KEY_COMPLETED_TASK_IDS, updatedCompleted).apply()
        }
    }

    private fun shouldIncrement(item: TaskItem, actionType: String): Boolean {
        return when (actionType) {
            "DEFLAME" -> item.actionTag == "DEFLAME" || item.actionTag == "BALANCE"
            "GIRL" -> item.actionTag == "GIRL" || item.actionTag == "BALANCE"
            "STRIKE" -> item.actionTag == "STRIKE"
            "SHIELD" -> item.actionTag == "SHIELD"
            "KEY" -> item.actionTag == "KEY"
            "POTION" -> item.actionTag == "POTION"
            "PERSON" -> item.actionTag == "PERSON"
            "ROADMAP" -> item.actionTag == "ROADMAP"
            else -> false
        }
    }

    private fun saveTasks(context: Context, key: String, tasks: List<TaskItem>) {
        val array = JSONArray()
        for (item in tasks) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("period", item.period.name)
                put("title", item.title)
                put("description", item.description)
                put("xpBounty", item.xpBounty)
                put("targetCount", item.targetCount)
                put("currentCount", item.currentCount)
                put("status", item.status.name)
                put("requiredSkipKey", item.requiredSkipKey.name)
                put("actionTag", item.actionTag)
                put("difficultyTier", item.difficultyTier)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(key, array.toString()).apply()
    }

    private fun loadTasks(context: Context, key: String): List<TaskItem>? {
        val str = getPrefs(context).getString(key, null) ?: return null
        return try {
            val array = JSONArray(str)
            val list = mutableListOf<TaskItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val skipKeyName = obj.optString("requiredSkipKey", SkipKeyType.WOODEN.name)
                val skipKey = SkipKeyType.entries.find { it.name == skipKeyName } ?: SkipKeyType.WOODEN
                list.add(
                    TaskItem(
                        id = obj.getString("id"),
                        period = TaskPeriod.valueOf(obj.getString("period")),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        xpBounty = obj.getInt("xpBounty"),
                        targetCount = obj.optInt("targetCount", 1),
                        currentCount = obj.optInt("currentCount", 0),
                        status = TaskStatus.valueOf(obj.optString("status", "PENDING")),
                        requiredSkipKey = skipKey,
                        actionTag = obj.optString("actionTag", "GENERAL"),
                        difficultyTier = obj.optInt("difficultyTier", 1)
                    )
                )
            }
            list.sortedWith(
                compareBy<TaskItem> { it.difficultyTier }
                    .thenBy { it.xpBounty }
                    .thenBy { it.targetCount }
                    .thenBy { it.id }
            )
        } catch (_: Exception) {
            null
        }
    }
}
