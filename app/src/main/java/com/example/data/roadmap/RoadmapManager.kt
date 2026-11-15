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
 * - 500-Station Wavy Roadmap & Station Claims (Smooth exponential XP scaling from 1,345 to 1,000,000 XP)
 * - 1000-Task Matrix & Progression Engine
 * - Carry-Over & Non-Repeating Consumption Rotation Engine:
 *   * Daily (5), Weekly (7), Monthly (10): completed/skipped tasks vanish immediately and never repeat;
 *     uncompleted pending tasks persist across cycles; open slots refill dynamically on new cycles.
 *   * Annual (250): all 250 shown from day one; skip-keys prohibited; completed tasks archive to the bottom.
 * - Cyber Inventory (Skip Keys & Protective Shields)
 */
object RoadmapManager {

    private const val PREFS_NAME = "strikepulse_roadmap_prefs"

    private const val KEY_TOTAL_XP = "key_total_xp"
    private const val KEY_CLAIMED_STATIONS = "key_claimed_stations"
    private const val KEY_COMPLETED_TASK_IDS = "key_completed_task_ids"
    private const val KEY_COMPLETED_TASK_TIMESTAMPS = "key_completed_task_timestamps"
    private const val KEY_LAST_DAILY_DATE = "key_last_daily_date"
    private const val KEY_LAST_WEEKLY_CODE = "key_last_weekly_code"
    private const val KEY_LAST_MONTHLY_CODE = "key_last_monthly_code"
    private const val KEY_DAILY_TASKS_JSON = "key_daily_tasks_json"
    private const val KEY_WEEKLY_TASKS_JSON = "key_weekly_tasks_json"
    private const val KEY_MONTHLY_TASKS_JSON = "key_monthly_tasks_json"
    private const val KEY_YEARLY_TASKS_JSON = "key_yearly_tasks_json"

    // Shields Inventory
    private const val KEY_SHIELDS_24H = "key_shields_24h"
    private const val KEY_SHIELDS_48H = "key_shields_48h"
    private const val KEY_SHIELDS_72H = "key_shields_72h"

    // Flows (Tasks XP strictly starts at 0L; Roadmap stations only act as unlock thresholds)
    private val _totalXp = MutableStateFlow(0L)
    val totalXp: StateFlow<Long> = _totalXp.asStateFlow()

    private val _claimedStations = MutableStateFlow<Set<Int>>(emptySet())
    val claimedStations: StateFlow<Set<Int>> = _claimedStations.asStateFlow()

    private val _completedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    val completedTaskIds: StateFlow<Set<String>> = _completedTaskIds.asStateFlow()

    private val _completedTaskTimestamps = MutableStateFlow<Map<String, Long>>(emptyMap())
    val completedTaskTimestamps: StateFlow<Map<String, Long>> = _completedTaskTimestamps.asStateFlow()

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

    // Shields Inventory
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

        _totalXp.value = prefs.getLong(KEY_TOTAL_XP, 0L)

        val claimedStrSet = prefs.getStringSet(KEY_CLAIMED_STATIONS, emptySet()) ?: emptySet()
        _claimedStations.value = claimedStrSet.mapNotNull { it.toIntOrNull() }.toSet()

        val completedStrSet = prefs.getStringSet(KEY_COMPLETED_TASK_IDS, emptySet()) ?: emptySet()
        _completedTaskIds.value = completedStrSet.toSet()

        _completedTaskTimestamps.value = loadTimestampsMap(prefs.getString(KEY_COMPLETED_TASK_TIMESTAMPS, null))

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

        // Legacy cache purge for old task schema & phantom initial 50 XP fix
        val tasksCacheVersion = prefs.getInt("key_tasks_matrix_version", 0)
        if (tasksCacheVersion < 8) {
            val edit = prefs.edit()
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
                .putInt("key_tasks_matrix_version", 8)

            // If user never completed any tasks and never claimed any stations, clear legacy 50 XP default back to 0 XP
            if (_completedTaskIds.value.isEmpty() && _claimedStations.value.isEmpty() && _totalXp.value == 50L) {
                _totalXp.value = 0L
                edit.putLong(KEY_TOTAL_XP, 0L)
            }
            edit.apply()
        }

        // Migrate any completed IDs found in saved task JSONs before cycle evaluation
        val savedDaily = loadTasks(context, KEY_DAILY_TASKS_JSON)
        val savedWeekly = loadTasks(context, KEY_WEEKLY_TASKS_JSON)
        val savedMonthly = loadTasks(context, KEY_MONTHLY_TASKS_JSON)
        val savedYearly = loadTasks(context, KEY_YEARLY_TASKS_JSON)

        val extraCompleted = (savedDaily.orEmpty() + savedWeekly.orEmpty() + savedMonthly.orEmpty() + savedYearly.orEmpty())
            .filter { it.isCompleted }
            .map { it.id }
        if (extraCompleted.isNotEmpty() && !extraCompleted.all { it in _completedTaskIds.value }) {
            val mergedCompleted = _completedTaskIds.value + extraCompleted
            _completedTaskIds.value = mergedCompleted
            prefs.edit().putStringSet(KEY_COMPLETED_TASK_IDS, mergedCompleted).apply()
        }

        syncTimeframeCycles(context, savedDaily, savedWeekly, savedMonthly)

        // 4. Annual / Master Goals Tasks (Full 250 batch from day 1; completed shift to bottom archive)
        val annualTasks = TaskPools.getEverlastingTasks(_completedTaskIds.value, savedYearly.orEmpty())
        _yearlyTasks.value = annualTasks
        saveTasks(context, KEY_YEARLY_TASKS_JSON, annualTasks)

        isInitialized = true
    }

    @Synchronized
    fun refreshCyclesIfNeeded(context: Context) {
        if (!isInitialized) {
            init(context)
            return
        }
        syncTimeframeCycles(context, _dailyTasks.value, _weeklyTasks.value, _monthlyTasks.value)
    }

    private fun syncTimeframeCycles(
        context: Context,
        currentDaily: List<TaskItem>?,
        currentWeekly: List<TaskItem>?,
        currentMonthly: List<TaskItem>?
    ) {
        val prefs = getPrefs(context)
        val completed = _completedTaskIds.value
        val now = System.currentTimeMillis()

        fun normalizeInCycleList(list: List<TaskItem>): Pair<List<TaskItem>, Boolean> {
            var modified = false
            val filtered = list.filter { !it.isCompleted && it.id !in completed }.map { item ->
                if (item.unlockAtMillis in 1L..now) {
                    modified = true
                    item.copy(unlockAtMillis = 0L)
                } else {
                    item
                }
            }
            if (filtered.size != list.size) modified = true
            return TaskPools.sortTasksProgressively(filtered, now) to modified
        }

        // 1. Daily Cycle (Capacity 5, new tasks unlock at 12:00 AM midnight)
        val today = TaskPools.todayDateString(now)
        val lastDate = prefs.getString(KEY_LAST_DAILY_DATE, null)
        if (lastDate != today || currentDaily == null) {
            val freshDaily = TaskPools.fillActiveBatch(
                period = TaskPeriod.DAILY,
                existingPending = currentDaily.orEmpty(),
                completedIds = completed,
                seedKey = today,
                batchSize = 5,
                nowMillis = now
            )
            _dailyTasks.value = freshDaily
            saveTasks(context, KEY_DAILY_TASKS_JSON, freshDaily)
            prefs.edit().putString(KEY_LAST_DAILY_DATE, today).apply()
        } else {
            val (activeOnly, changed) = normalizeInCycleList(currentDaily)
            _dailyTasks.value = activeOnly
            if (changed) {
                saveTasks(context, KEY_DAILY_TASKS_JSON, activeOnly)
            }
        }

        // 2. Weekly Cycle (Capacity 7, Sunday 12:00 AM grant day)
        val currentWeek = TaskPools.currentWeekString(now)
        val lastWeek = prefs.getString(KEY_LAST_WEEKLY_CODE, null)
        if (lastWeek != currentWeek || currentWeekly == null) {
            val freshWeekly = TaskPools.fillActiveBatch(
                period = TaskPeriod.WEEKLY,
                existingPending = currentWeekly.orEmpty(),
                completedIds = completed,
                seedKey = currentWeek,
                batchSize = 7,
                nowMillis = now
            )
            _weeklyTasks.value = freshWeekly
            saveTasks(context, KEY_WEEKLY_TASKS_JSON, freshWeekly)
            prefs.edit().putString(KEY_LAST_WEEKLY_CODE, currentWeek).apply()
        } else {
            val (activeOnly, changed) = normalizeInCycleList(currentWeekly)
            _weeklyTasks.value = activeOnly
            if (changed) {
                saveTasks(context, KEY_WEEKLY_TASKS_JSON, activeOnly)
            }
        }

        // 3. Monthly Cycle (Capacity 10, 1st of month 12:00 AM grant day)
        val currentMonth = TaskPools.currentMonthString(now)
        val lastMonth = prefs.getString(KEY_LAST_MONTHLY_CODE, null)
        if (lastMonth != currentMonth || currentMonthly == null) {
            val freshMonthly = TaskPools.fillActiveBatch(
                period = TaskPeriod.MONTHLY,
                existingPending = currentMonthly.orEmpty(),
                completedIds = completed,
                seedKey = currentMonth,
                batchSize = 10,
                nowMillis = now
            )
            _monthlyTasks.value = freshMonthly
            saveTasks(context, KEY_MONTHLY_TASKS_JSON, freshMonthly)
            prefs.edit().putString(KEY_LAST_MONTHLY_CODE, currentMonth).apply()
        } else {
            val (activeOnly, changed) = normalizeInCycleList(currentMonthly)
            _monthlyTasks.value = activeOnly
            if (changed) {
                saveTasks(context, KEY_MONTHLY_TASKS_JSON, activeOnly)
            }
        }
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
     * Hard reset / wipe of all task completion states across Daily, Weekly, Monthly, and Annual pools.
     * Triggered by 5-second long-press on the Profile card.
     * Note: Permanent Titles & Avatars in ProfileManager remain untouched.
     */
    @Synchronized
    fun hardResetAllTaskCompletionStates(context: Context) {
        init(context)
        _completedTaskIds.value = emptySet()
        _completedTaskTimestamps.value = emptyMap()

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
            .remove(KEY_COMPLETED_TASK_TIMESTAMPS)
            .remove(KEY_DAILY_TASKS_JSON)
            .remove(KEY_WEEKLY_TASKS_JSON)
            .remove(KEY_MONTHLY_TASKS_JSON)
            .remove(KEY_YEARLY_TASKS_JSON)
            .putString(KEY_LAST_DAILY_DATE, today)
            .putString(KEY_LAST_WEEKLY_CODE, currentWeek)
            .putString(KEY_LAST_MONTHLY_CODE, currentMonth)
            .apply()

        saveTasks(context, KEY_DAILY_TASKS_JSON, freshDaily)
        saveTasks(context, KEY_WEEKLY_TASKS_JSON, freshWeekly)
        saveTasks(context, KEY_MONTHLY_TASKS_JSON, freshMonthly)
        saveTasks(context, KEY_YEARLY_TASKS_JSON, freshEverlasting)
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
        _completedTaskTimestamps.value = emptyMap()
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
            .remove(KEY_COMPLETED_TASK_TIMESTAMPS)
            .remove(KEY_DAILY_TASKS_JSON)
            .remove(KEY_WEEKLY_TASKS_JSON)
            .remove(KEY_MONTHLY_TASKS_JSON)
            .remove(KEY_YEARLY_TASKS_JSON)
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
        saveTasks(context, KEY_YEARLY_TASKS_JSON, freshEverlasting)
    }

    /**
     * Calculates the highest unlocked station (1 to 500) based on Total XP.
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
        if (state != StationState.UNLOCKED) return null

        val reward = TaskPools.getRewardForStation(stationId)
        val updatedSet = _claimedStations.value.toMutableSet()
        updatedSet.add(stationId)
        _claimedStations.value = updatedSet

        val strSet = updatedSet.map { it.toString() }.toSet()
        getPrefs(context).edit().putStringSet(KEY_CLAIMED_STATIONS, strSet).apply()

        applyReward(context, reward)
        ProfileManager.onStationUnlocked(context, stationId)
        // Roadmap stations are strictly one-way unlock thresholds fueled by Tasks XP;
        // claiming a station NEVER awards XP or triggers any tasks.
        return reward
    }

    private fun applyReward(context: Context, reward: StationReward) {
        when (reward.type) {
            RewardType.SHIELD_24H -> {
                FlameManager.grant1DayShield(context, reward.quantity)
                FlameManager.activate1DayShield(context, notifyRoadmapTask = false)
            }
            RewardType.SHIELD_48H -> {
                FlameManager.grant48hShield(context, reward.quantity)
                FlameManager.activate48hShield(context, notifyRoadmapTask = false)
            }
            RewardType.SHIELD_72H -> {
                FlameManager.grantManualShield(context, days = 3)
                FlameManager.activateManualShield(context, days = 3, notifyRoadmapTask = false)
            }
            RewardType.KEY_WOODEN -> ProfileManager.grantSkipKeys(context, SkipKeyType.WOODEN, reward.quantity)
            RewardType.KEY_BRONZE -> ProfileManager.grantSkipKeys(context, SkipKeyType.BRONZE, reward.quantity)
            RewardType.KEY_SILVER -> ProfileManager.grantSkipKeys(context, SkipKeyType.SILVER, reward.quantity)
            RewardType.POTION_PULSE_ELIXIR -> ProfileManager.grantPotion(context, com.example.data.profile.PotionType.PULSE_ELIXIR, reward.quantity, notifyRoadmapTask = false)
            RewardType.POTION_SHADOW_SERUM -> ProfileManager.grantPotion(context, com.example.data.profile.PotionType.SHADOW_GRIP_SERUM, reward.quantity, notifyRoadmapTask = false)
            RewardType.POTION_GABRIEL_DROPLET -> ProfileManager.grantPotion(context, com.example.data.profile.PotionType.GABRIEL_PURITY_DROPLET, reward.quantity, notifyRoadmapTask = false)
            RewardType.POTION_CRYO_INFUSION -> ProfileManager.grantPotion(context, com.example.data.profile.PotionType.ABSOLUTE_CRYO_INFUSION, reward.quantity, notifyRoadmapTask = false)
            RewardType.POTION_ETERNAL_REBIRTH -> ProfileManager.grantPotion(context, com.example.data.profile.PotionType.ETERNAL_REBIRTH_ELIXIR, reward.quantity, notifyRoadmapTask = false)
            RewardType.REWARD_AVATAR, RewardType.REWARD_TITLE -> Unit
        }
    }

    /**
     * Collects ("جمع") a task that has reached READY_TO_CLAIM via user actions:
     * - Awards XP bounty.
     * - Records task permanently in completedTaskIds & completedTaskTimestamps (visible in Tasks Ledger).
     * - For Daily / Weekly / Monthly: replaces slot immediately with a fresh non-repeating task
     *   locked with a countdown until next midnight (Daily), Sunday 12 AM (Weekly), or 1st of month (Monthly).
     * - For Eternal / Annual: transfers directly out of the active Eternal list into the Completed Tasks Ledger.
     */
    @Synchronized
    fun claimTaskReward(context: Context, taskId: String): Boolean {
        return finalizeAndReplaceTask(context, taskId, allowPendingDirect = false)
    }

    /**
     * Completes/collects a task directly (supports both READY_TO_CLAIM and direct programmatic completion).
     */
    @Synchronized
    fun completeTask(context: Context, taskId: String): Boolean {
        return finalizeAndReplaceTask(context, taskId, allowPendingDirect = true)
    }

    private fun finalizeAndReplaceTask(
        context: Context,
        taskId: String,
        allowPendingDirect: Boolean
    ): Boolean {
        init(context)
        val now = System.currentTimeMillis()
        var found = false
        var awardedXp = 0

        val updatedCompleted = _completedTaskIds.value.toMutableSet()
        updatedCompleted.add(taskId)

        fun tryClaimInRotationalList(
            currentList: List<TaskItem>,
            period: TaskPeriod
        ): Pair<List<TaskItem>, Boolean> {
            val idx = currentList.indexOfFirst { item ->
                item.id == taskId && !item.isLocked(now) &&
                    (item.status == TaskStatus.READY_TO_CLAIM || (allowPendingDirect && item.status == TaskStatus.PENDING))
            }
            if (idx < 0) return currentList to false

            val target = currentList[idx]
            found = true
            awardedXp = target.xpBounty

            val remaining = currentList.filterNot { it.id == taskId }.toMutableList()
            val excludedIds = updatedCompleted + remaining.map { it.id }
            val replacement = TaskPools.drawReplacementTask(
                period = period,
                excludedIds = excludedIds,
                unlockAtMillis = TaskPools.getNextCycleResetMillis(period, now),
                seedMillis = now
            )
            if (replacement != null) {
                remaining.add(replacement)
            }
            return TaskPools.sortTasksProgressively(remaining, now) to true
        }

        val (nextDaily, dailyMatched) = tryClaimInRotationalList(_dailyTasks.value, TaskPeriod.DAILY)
        if (dailyMatched) {
            _dailyTasks.value = nextDaily
            saveTasks(context, KEY_DAILY_TASKS_JSON, nextDaily)
        }

        if (!found) {
            val (nextWeekly, weeklyMatched) = tryClaimInRotationalList(_weeklyTasks.value, TaskPeriod.WEEKLY)
            if (weeklyMatched) {
                _weeklyTasks.value = nextWeekly
                saveTasks(context, KEY_WEEKLY_TASKS_JSON, nextWeekly)
            }
        }

        if (!found) {
            val (nextMonthly, monthlyMatched) = tryClaimInRotationalList(_monthlyTasks.value, TaskPeriod.MONTHLY)
            if (monthlyMatched) {
                _monthlyTasks.value = nextMonthly
                saveTasks(context, KEY_MONTHLY_TASKS_JSON, nextMonthly)
            }
        }

        if (!found) {
            val targetYearly = _yearlyTasks.value.find { item ->
                item.id == taskId && !item.isLocked(now) &&
                    (item.status == TaskStatus.READY_TO_CLAIM || (allowPendingDirect && item.status == TaskStatus.PENDING))
            }
            if (targetYearly != null) {
                found = true
                awardedXp = targetYearly.xpBounty
                // Transfer directly out of active Eternal list into Completed Tasks Ledger
                val remainingYearly = TaskPools.sortTasksProgressively(
                    _yearlyTasks.value.filterNot { it.id == taskId },
                    now
                )
                _yearlyTasks.value = remainingYearly
                saveTasks(context, KEY_YEARLY_TASKS_JSON, remainingYearly)
            }
        }

        if (found) {
            _completedTaskIds.value = updatedCompleted
            val updatedTimestamps = _completedTaskTimestamps.value.toMutableMap().apply {
                put(taskId, now)
            }
            _completedTaskTimestamps.value = updatedTimestamps

            getPrefs(context).edit()
                .putStringSet(KEY_COMPLETED_TASK_IDS, updatedCompleted)
                .putString(KEY_COMPLETED_TASK_TIMESTAMPS, timestampsToJson(updatedTimestamps))
                .apply()

            if (awardedXp > 0) {
                addXp(context, awardedXp)
            }
        }
        return found
    }

    /**
     * Consumes an Inventory Skip Key (Wooden, Bronze, or Silver) to skip a Daily, Weekly, or Monthly task:
     * - Locked replacement tasks cannot be skipped until their cycle window opens.
     * - Permanently enters completedTaskIds + Tasks Ledger and draws 1 replacement task locked until next cycle.
     * - Eternal / Yearly tasks cannot be skipped.
     */
    @Synchronized
    fun useSkipKeyForTask(context: Context, taskId: String): Boolean {
        init(context)
        val now = System.currentTimeMillis()
        val allTasks = _dailyTasks.value + _weeklyTasks.value + _monthlyTasks.value + _yearlyTasks.value
        val task = allTasks.find { it.id == taskId } ?: return false
        if (task.status != TaskStatus.PENDING) return false
        if (!task.canBeSkipped) return false
        if (task.isLocked(now)) return false

        val hasKey = ProfileManager.consumeSkipKey(context, task.requiredSkipKey)
        if (!hasKey) return false

        val prefs = getPrefs(context)
        val awardedXp = task.xpBounty

        val updatedCompleted = _completedTaskIds.value.toMutableSet()
        updatedCompleted.add(taskId)
        _completedTaskIds.value = updatedCompleted

        val updatedTimestamps = _completedTaskTimestamps.value.toMutableMap().apply {
            put(taskId, now)
        }
        _completedTaskTimestamps.value = updatedTimestamps

        fun replaceInPeriod(list: List<TaskItem>, period: TaskPeriod): List<TaskItem>? {
            if (list.none { it.id == taskId }) return null
            val remaining = list.filterNot { it.id == taskId }.toMutableList()
            val replacement = TaskPools.drawReplacementTask(
                period = period,
                excludedIds = updatedCompleted + remaining.map { it.id },
                unlockAtMillis = TaskPools.getNextCycleResetMillis(period, now),
                seedMillis = now
            )
            if (replacement != null) {
                remaining.add(replacement)
            }
            return TaskPools.sortTasksProgressively(remaining, now)
        }

        replaceInPeriod(_dailyTasks.value, TaskPeriod.DAILY)?.let {
            _dailyTasks.value = it
            saveTasks(context, KEY_DAILY_TASKS_JSON, it)
        }
        replaceInPeriod(_weeklyTasks.value, TaskPeriod.WEEKLY)?.let {
            _weeklyTasks.value = it
            saveTasks(context, KEY_WEEKLY_TASKS_JSON, it)
        }
        replaceInPeriod(_monthlyTasks.value, TaskPeriod.MONTHLY)?.let {
            _monthlyTasks.value = it
            saveTasks(context, KEY_MONTHLY_TASKS_JSON, it)
        }

        prefs.edit()
            .putStringSet(KEY_COMPLETED_TASK_IDS, updatedCompleted)
            .putString(KEY_COMPLETED_TASK_TIMESTAMPS, timestampsToJson(updatedTimestamps))
            .apply()

        if (awardedXp > 0) {
            addXp(context, awardedXp)
        }
        onActionOccurred(context, "KEY")
        return true
    }

    /**
     * Auto-increments progress for matching UNLOCKED pending tasks across D/W/M and Eternal pools.
     * - When a task reaches its targetCount, it enters READY_TO_CLAIM and stays visible until the user
     *   explicitly taps Collect ("جمع"). No XP is awarded until collection.
     * - Locked replacement tasks (unlockAtMillis > now) do not progress until their grant timer expires.
     */
    @Synchronized
    fun onActionOccurred(context: Context, actionType: String) {
        init(context)
        val now = System.currentTimeMillis()

        fun advanceList(list: List<TaskItem>): Pair<List<TaskItem>, Boolean> {
            var changed = false
            val updated = list.map { item ->
                val effectiveItem = if (item.unlockAtMillis in 1L..now) {
                    changed = true
                    item.copy(unlockAtMillis = 0L)
                } else {
                    item
                }

                if (effectiveItem.status == TaskStatus.PENDING &&
                    !effectiveItem.isLocked(now) &&
                    shouldIncrement(effectiveItem, actionType)
                ) {
                    changed = true
                    val newCount = effectiveItem.currentCount + 1
                    if (newCount >= effectiveItem.targetCount) {
                        effectiveItem.copy(
                            currentCount = effectiveItem.targetCount,
                            status = TaskStatus.READY_TO_CLAIM
                        )
                    } else {
                        effectiveItem.copy(currentCount = newCount)
                    }
                } else {
                    effectiveItem
                }
            }
            return if (changed) TaskPools.sortTasksProgressively(updated, now) to true else list to false
        }

        val (newDaily, dailyChanged) = advanceList(_dailyTasks.value)
        if (dailyChanged) {
            _dailyTasks.value = newDaily
            saveTasks(context, KEY_DAILY_TASKS_JSON, newDaily)
        }

        val (newWeekly, weeklyChanged) = advanceList(_weeklyTasks.value)
        if (weeklyChanged) {
            _weeklyTasks.value = newWeekly
            saveTasks(context, KEY_WEEKLY_TASKS_JSON, newWeekly)
        }

        val (newMonthly, monthlyChanged) = advanceList(_monthlyTasks.value)
        if (monthlyChanged) {
            _monthlyTasks.value = newMonthly
            saveTasks(context, KEY_MONTHLY_TASKS_JSON, newMonthly)
        }

        val (newYearly, yearlyChanged) = advanceList(_yearlyTasks.value)
        if (yearlyChanged) {
            _yearlyTasks.value = newYearly
            saveTasks(context, KEY_YEARLY_TASKS_JSON, newYearly)
        }
    }

    /**
     * Builds a complete 1,000-task snapshot for the Tasks Ledger ("دفتر المهام المنجزة") showing
     * every completed, ready-to-claim, active in-progress, cooldown-waiting, and remaining pool task
     * along with its live progress counts.
     */
    fun getTasksLedgerSnapshot(): List<TaskLedgerEntry> {
        val activeById = (_dailyTasks.value + _weeklyTasks.value + _monthlyTasks.value + _yearlyTasks.value)
            .associateBy { it.id }
        val completedIds = _completedTaskIds.value
        val timestamps = _completedTaskTimestamps.value

        return TaskPools.ALL_1000_TASKS.map { catalogTask ->
            val live = activeById[catalogTask.id]
            when {
                live != null -> TaskLedgerEntry(task = live, completedAtMillis = null)
                catalogTask.id in completedIds -> TaskLedgerEntry(
                    task = catalogTask.copy(
                        status = TaskStatus.COMPLETED,
                        currentCount = catalogTask.targetCount,
                        unlockAtMillis = 0L
                    ),
                    completedAtMillis = timestamps[catalogTask.id]
                )
                else -> TaskLedgerEntry(
                    task = catalogTask.copy(
                        status = TaskStatus.PENDING,
                        currentCount = 0,
                        unlockAtMillis = 0L
                    ),
                    completedAtMillis = null
                )
            }
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
            else -> false
        }
    }

    private fun timestampsToJson(map: Map<String, Long>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    private fun loadTimestampsMap(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, Long>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optLong(k, 0L)
                if (v > 0L) result[k] = v
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun tasksToJsonArray(tasks: List<TaskItem>): JSONArray {
        val array = JSONArray()
        for (item in tasks) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("period", item.period.name)
                put("title", item.title)
                put("description", item.description)
                put("titleEn", item.titleEn)
                put("descriptionEn", item.descriptionEn)
                put("xpBounty", item.xpBounty)
                put("targetCount", item.targetCount)
                put("currentCount", item.currentCount)
                put("status", item.status.name)
                put("requiredSkipKey", item.requiredSkipKey.name)
                put("actionTag", item.actionTag)
                put("difficultyTier", item.difficultyTier)
                put("unlockAtMillis", item.unlockAtMillis)
            }
            array.put(obj)
        }
        return array
    }

    private fun jsonArrayToTasks(array: JSONArray, sortForYearly: Boolean = false): List<TaskItem> {
        val list = mutableListOf<TaskItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val id = obj.getString("id")
            val catalog = TaskPools.findById(id)
            val skipKeyName = obj.optString("requiredSkipKey", SkipKeyType.WOODEN.name)
            val skipKey = catalog?.requiredSkipKey
                ?: (SkipKeyType.entries.find { it.name == skipKeyName } ?: SkipKeyType.WOODEN)
            val periodName = obj.optString("period", catalog?.period?.name ?: TaskPeriod.DAILY.name)
            val period = catalog?.period ?: (TaskPeriod.entries.find { it.name == periodName } ?: TaskPeriod.DAILY)
            val rawStatus = TaskStatus.entries.find { it.name == obj.optString("status", "PENDING") } ?: TaskStatus.PENDING
            val targetCount = catalog?.targetCount ?: obj.optInt("targetCount", 1)
            val rawCurrent = obj.optInt("currentCount", 0)
            val status = if (rawStatus == TaskStatus.PENDING && rawCurrent >= targetCount) {
                TaskStatus.READY_TO_CLAIM
            } else {
                rawStatus
            }
            val clampedCurrent = when (status) {
                TaskStatus.COMPLETED, TaskStatus.SKIPPED, TaskStatus.READY_TO_CLAIM -> targetCount
                TaskStatus.PENDING -> rawCurrent.coerceIn(0, (targetCount - 1).coerceAtLeast(0))
            }
            val unlockAtMillis = obj.optLong("unlockAtMillis", 0L).coerceAtLeast(0L)

            list.add(
                TaskItem(
                    id = id,
                    period = period,
                    title = catalog?.title ?: obj.optString("title", id),
                    description = catalog?.description ?: obj.optString("description", ""),
                    xpBounty = catalog?.xpBounty ?: obj.optInt("xpBounty", 50),
                    targetCount = targetCount,
                    currentCount = clampedCurrent,
                    status = status,
                    requiredSkipKey = skipKey,
                    actionTag = catalog?.actionTag ?: obj.optString("actionTag", "GENERAL"),
                    difficultyTier = catalog?.difficultyTier ?: obj.optInt("difficultyTier", 1),
                    titleEn = catalog?.titleEn ?: obj.optString("titleEn", catalog?.title ?: obj.optString("title", id)),
                    descriptionEn = catalog?.descriptionEn ?: obj.optString("descriptionEn", catalog?.description ?: obj.optString("description", "")),
                    unlockAtMillis = unlockAtMillis
                )
            )
        }
        return TaskPools.sortTasksProgressively(list)
    }

    private fun saveTasks(context: Context, key: String, tasks: List<TaskItem>) {
        getPrefs(context).edit().putString(key, tasksToJsonArray(tasks).toString()).apply()
    }

    private fun loadTasks(context: Context, key: String, sortForYearly: Boolean = false): List<TaskItem>? {
        val str = getPrefs(context).getString(key, null) ?: return null
        return try {
            jsonArrayToTasks(JSONArray(str), sortForYearly = sortForYearly)
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    fun exportSnapshot(context: Context): JSONObject {
        init(context)
        val prefs = getPrefs(context)
        return JSONObject().apply {
            put("totalXp", _totalXp.value)
            put("claimedStations", JSONArray(_claimedStations.value.sorted()))
            put("completedTaskIds", JSONArray(_completedTaskIds.value.sorted()))
            put("completedTaskTimestamps", JSONObject().apply {
                _completedTaskTimestamps.value.forEach { (k, v) -> put(k, v) }
            })
            put("lastDailyDate", prefs.getString(KEY_LAST_DAILY_DATE, TaskPools.todayDateString()))
            put("lastWeeklyCode", prefs.getString(KEY_LAST_WEEKLY_CODE, TaskPools.currentWeekString()))
            put("lastMonthlyCode", prefs.getString(KEY_LAST_MONTHLY_CODE, TaskPools.currentMonthString()))
            put("dailyTasks", tasksToJsonArray(_dailyTasks.value))
            put("weeklyTasks", tasksToJsonArray(_weeklyTasks.value))
            put("monthlyTasks", tasksToJsonArray(_monthlyTasks.value))
            put("yearlyTasks", tasksToJsonArray(_yearlyTasks.value))
            put("shields24h", _shields24h.value)
            put("shields48h", _shields48h.value)
            put("shields72h", _shields72h.value)
        }
    }

    @Synchronized
    fun restoreSnapshot(context: Context, json: JSONObject, replaceExisting: Boolean) {
        init(context)
        val prefs = getPrefs(context)

        val importedXp = json.optLong("totalXp", 0L).coerceAtLeast(0L)
        val finalXp = if (replaceExisting) importedXp else (_totalXp.value + importedXp)

        val importedStations = mutableSetOf<Int>()
        json.optJSONArray("claimedStations")?.let { arr ->
            for (i in 0 until arr.length()) {
                val sid = arr.optInt(i, -1)
                if (sid in 1..500) importedStations.add(sid)
            }
        }
        val finalStations = if (replaceExisting) importedStations else (_claimedStations.value + importedStations)

        val importedCompleted = mutableSetOf<String>()
        json.optJSONArray("completedTaskIds")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optString(i).takeIf { it.isNotBlank() }?.let { importedCompleted.add(it) }
            }
        }
        val finalCompleted = if (replaceExisting) importedCompleted else (_completedTaskIds.value + importedCompleted)

        val importedTimestamps = loadTimestampsMap(json.optJSONObject("completedTaskTimestamps")?.toString())
        val finalTimestamps = if (replaceExisting) {
            importedTimestamps
        } else {
            _completedTaskTimestamps.value + importedTimestamps
        }

        val importedDaily = json.optJSONArray("dailyTasks")?.let { jsonArrayToTasks(it) }
        val importedWeekly = json.optJSONArray("weeklyTasks")?.let { jsonArrayToTasks(it) }
        val importedMonthly = json.optJSONArray("monthlyTasks")?.let { jsonArrayToTasks(it) }
        val importedYearly = json.optJSONArray("yearlyTasks")?.let { jsonArrayToTasks(it, sortForYearly = true) }

        val today = TaskPools.todayDateString()
        val currentWeek = TaskPools.currentWeekString()
        val currentMonth = TaskPools.currentMonthString()

        val lastDaily = json.optString("lastDailyDate", today).ifBlank { today }
        val lastWeekly = json.optString("lastWeeklyCode", currentWeek).ifBlank { currentWeek }
        val lastMonthly = json.optString("lastMonthlyCode", currentMonth).ifBlank { currentMonth }

        fun mergeRotational(current: List<TaskItem>, imported: List<TaskItem>?, fallback: List<TaskItem>): List<TaskItem> {
            return if (replaceExisting) {
                TaskPools.sortTasksProgressively(
                    (imported ?: fallback).filter { !it.isCompleted && it.id !in finalCompleted }
                )
            } else {
                val mergedMap = (current + imported.orEmpty())
                    .filter { !it.isCompleted && it.id !in finalCompleted }
                    .groupBy { it.id }
                    .map { (_, items) -> items.maxBy { it.currentCount } }
                TaskPools.sortTasksProgressively(mergedMap)
            }
        }

        val finalDaily = mergeRotational(_dailyTasks.value, importedDaily, TaskPools.getRotatedDailyTasks(finalCompleted, lastDaily))
        val finalWeekly = mergeRotational(_weeklyTasks.value, importedWeekly, TaskPools.getRotatedWeeklyTasks(finalCompleted, lastWeekly))
        val finalMonthly = mergeRotational(_monthlyTasks.value, importedMonthly, TaskPools.getRotatedMonthlyTasks(finalCompleted, lastMonthly))

        val finalYearly = if (replaceExisting) {
            TaskPools.getEverlastingTasks(finalCompleted, importedYearly.orEmpty())
        } else {
            val mergedSaved = (_yearlyTasks.value + importedYearly.orEmpty())
                .groupBy { it.id }
                .map { (_, items) -> items.maxBy { it.currentCount } }
            TaskPools.getEverlastingTasks(finalCompleted, mergedSaved)
        }

        val s24 = if (replaceExisting) json.optInt("shields24h", 0) else (_shields24h.value + json.optInt("shields24h", 0))
        val s48 = if (replaceExisting) json.optInt("shields48h", 0) else (_shields48h.value + json.optInt("shields48h", 0))
        val s72 = if (replaceExisting) json.optInt("shields72h", 0) else (_shields72h.value + json.optInt("shields72h", 0))

        _totalXp.value = finalXp
        _claimedStations.value = finalStations
        _completedTaskIds.value = finalCompleted
        _completedTaskTimestamps.value = finalTimestamps
        _shields24h.value = s24.coerceAtLeast(0)
        _shields48h.value = s48.coerceAtLeast(0)
        _shields72h.value = s72.coerceAtLeast(0)

        _dailyTasks.value = finalDaily
        _weeklyTasks.value = finalWeekly
        _monthlyTasks.value = finalMonthly
        _yearlyTasks.value = finalYearly

        prefs.edit().apply {
            putLong(KEY_TOTAL_XP, finalXp)
            putStringSet(KEY_CLAIMED_STATIONS, finalStations.map { it.toString() }.toSet())
            putStringSet(KEY_COMPLETED_TASK_IDS, finalCompleted)
            putString(KEY_COMPLETED_TASK_TIMESTAMPS, timestampsToJson(finalTimestamps))
            putString(KEY_LAST_DAILY_DATE, lastDaily)
            putString(KEY_LAST_WEEKLY_CODE, lastWeekly)
            putString(KEY_LAST_MONTHLY_CODE, lastMonthly)
            putString(KEY_DAILY_TASKS_JSON, tasksToJsonArray(finalDaily).toString())
            putString(KEY_WEEKLY_TASKS_JSON, tasksToJsonArray(finalWeekly).toString())
            putString(KEY_MONTHLY_TASKS_JSON, tasksToJsonArray(finalMonthly).toString())
            putString(KEY_YEARLY_TASKS_JSON, tasksToJsonArray(finalYearly).toString())
            putInt(KEY_SHIELDS_24H, _shields24h.value)
            putInt(KEY_SHIELDS_48H, _shields48h.value)
            putInt(KEY_SHIELDS_72H, _shields72h.value)
            putBoolean("has_reset_inventory_to_zero_v3", true)
            putInt("key_tasks_matrix_version", 8)
            apply()
        }

        isInitialized = true
    }
}
