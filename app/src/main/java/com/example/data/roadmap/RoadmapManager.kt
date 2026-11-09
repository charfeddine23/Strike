package com.example.data.roadmap

import android.content.Context
import android.content.SharedPreferences
import com.example.data.flame.FlameManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Singleton state and persistence manager for:
 * - 500-Station Wavy Roadmap & Station Claims
 * - XP Accumulation & Progression Engine
 * - Dynamic Task Engine (Daily, Weekly, Monthly, Yearly)
 * - Cyber Inventory (Keys, Shields, Power Hammers with Live Coroutine Expiry Timers)
 */
object RoadmapManager {

    private const val PREFS_NAME = "strikepulse_roadmap_prefs"

    private const val KEY_TOTAL_XP = "key_total_xp"
    private const val KEY_CLAIMED_STATIONS = "key_claimed_stations"
    private const val KEY_LAST_DAILY_DATE = "key_last_daily_date"
    private const val KEY_DAILY_TASKS_JSON = "key_daily_tasks_json"
    private const val KEY_WEEKLY_TASKS_JSON = "key_weekly_tasks_json"
    private const val KEY_MONTHLY_TASKS_JSON = "key_monthly_tasks_json"
    private const val KEY_YEARLY_TASKS_JSON = "key_yearly_tasks_json"

    // Inventory Keys
    private const val KEY_KEYS_COMMON = "key_keys_common"
    private const val KEY_KEYS_NORMAL = "key_keys_normal"
    private const val KEY_KEYS_RARE = "key_keys_rare"
    private const val KEY_KEYS_LEGENDARY = "key_keys_legendary"

    // Shields Inventory
    private const val KEY_SHIELDS_24H = "key_shields_24h"
    private const val KEY_SHIELDS_48H = "key_shields_48h"
    private const val KEY_SHIELDS_72H = "key_shields_72h"

    // Active Hammers Expiry Timestamps
    private const val KEY_HAMMER_GIRL_EXPIRES = "key_hammer_girl_expires"
    private const val KEY_HAMMER_DEFLAME_EXPIRES = "key_hammer_deflame_expires"
    private const val KEY_HAMMER_THOR_EXPIRES = "key_hammer_thor_expires"

    // Flows
    private val _totalXp = MutableStateFlow(250L) // Default initial XP to start at Station 2
    val totalXp: StateFlow<Long> = _totalXp.asStateFlow()

    private val _claimedStations = MutableStateFlow<Set<Int>>(emptySet())
    val claimedStations: StateFlow<Set<Int>> = _claimedStations.asStateFlow()

    private val _dailyTasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val dailyTasks: StateFlow<List<TaskItem>> = _dailyTasks.asStateFlow()

    private val _weeklyTasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val weeklyTasks: StateFlow<List<TaskItem>> = _weeklyTasks.asStateFlow()

    private val _monthlyTasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val monthlyTasks: StateFlow<List<TaskItem>> = _monthlyTasks.asStateFlow()

    private val _yearlyTasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val yearlyTasks: StateFlow<List<TaskItem>> = _yearlyTasks.asStateFlow()

    // Skipping Keys Inventory (Initialized to 0 - earned strictly via tasks and rewards)
    private val _commonKeys = MutableStateFlow(0)
    val commonKeys: StateFlow<Int> = _commonKeys.asStateFlow()

    private val _normalKeys = MutableStateFlow(0)
    val normalKeys: StateFlow<Int> = _normalKeys.asStateFlow()

    private val _rareKeys = MutableStateFlow(0)
    val rareKeys: StateFlow<Int> = _rareKeys.asStateFlow()

    private val _legendaryKeys = MutableStateFlow(0)
    val legendaryKeys: StateFlow<Int> = _legendaryKeys.asStateFlow()

    // Shields Inventory (Initialized to 0 - earned strictly via tasks and rewards)
    private val _shields24h = MutableStateFlow(0)
    val shields24h: StateFlow<Int> = _shields24h.asStateFlow()

    private val _shields48h = MutableStateFlow(0)
    val shields48h: StateFlow<Int> = _shields48h.asStateFlow()

    private val _shields72h = MutableStateFlow(0)
    val shields72h: StateFlow<Int> = _shields72h.asStateFlow()

    // Power Hammers Expiry Timestamps
    private val _girlBoostHammerExpiresAt = MutableStateFlow<Long?>(null)
    val girlBoostHammerExpiresAt: StateFlow<Long?> = _girlBoostHammerExpiresAt.asStateFlow()

    private val _deflameBoostHammerExpiresAt = MutableStateFlow<Long?>(null)
    val deflameBoostHammerExpiresAt: StateFlow<Long?> = _deflameBoostHammerExpiresAt.asStateFlow()

    private val _thorHammerExpiresAt = MutableStateFlow<Long?>(null)
    val thorHammerExpiresAt: StateFlow<Long?> = _thorHammerExpiresAt.asStateFlow()

    private var isInitialized = false

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        val prefs = getPrefs(context)

        _totalXp.value = prefs.getLong(KEY_TOTAL_XP, 250L)

        // Read claimed stations
        val claimedStrSet = prefs.getStringSet(KEY_CLAIMED_STATIONS, emptySet()) ?: emptySet()
        _claimedStations.value = claimedStrSet.mapNotNull { it.toIntOrNull() }.toSet()

        // Ensure inventory starts clean at 0 (or loads saved non-default earned inventory)
        val hasResetInventory = prefs.getBoolean("has_reset_inventory_to_zero_v2", false)
        if (!hasResetInventory) {
            _commonKeys.value = 0
            _normalKeys.value = 0
            _rareKeys.value = 0
            _legendaryKeys.value = 0
            _shields24h.value = 0
            _shields48h.value = 0
            _shields72h.value = 0
            prefs.edit()
                .putInt(KEY_KEYS_COMMON, 0)
                .putInt(KEY_KEYS_NORMAL, 0)
                .putInt(KEY_KEYS_RARE, 0)
                .putInt(KEY_KEYS_LEGENDARY, 0)
                .putInt(KEY_SHIELDS_24H, 0)
                .putInt(KEY_SHIELDS_48H, 0)
                .putInt(KEY_SHIELDS_72H, 0)
                .putBoolean("has_reset_inventory_to_zero_v2", true)
                .apply()
        } else {
            // Read keys inventory (defaults to 0)
            _commonKeys.value = prefs.getInt(KEY_KEYS_COMMON, 0)
            _normalKeys.value = prefs.getInt(KEY_KEYS_NORMAL, 0)
            _rareKeys.value = prefs.getInt(KEY_KEYS_RARE, 0)
            _legendaryKeys.value = prefs.getInt(KEY_KEYS_LEGENDARY, 0)

            // Read shields inventory (defaults to 0)
            _shields24h.value = prefs.getInt(KEY_SHIELDS_24H, 0)
            _shields48h.value = prefs.getInt(KEY_SHIELDS_48H, 0)
            _shields72h.value = prefs.getInt(KEY_SHIELDS_72H, 0)
        }

        // Read hammers expiry
        val now = System.currentTimeMillis()
        val gExp = if (prefs.contains(KEY_HAMMER_GIRL_EXPIRES)) prefs.getLong(KEY_HAMMER_GIRL_EXPIRES, 0L) else null
        _girlBoostHammerExpiresAt.value = if (gExp != null && gExp > now) gExp else null

        val dExp = if (prefs.contains(KEY_HAMMER_DEFLAME_EXPIRES)) prefs.getLong(KEY_HAMMER_DEFLAME_EXPIRES, 0L) else null
        _deflameBoostHammerExpiresAt.value = if (dExp != null && dExp > now) dExp else null

        val tExp = if (prefs.contains(KEY_HAMMER_THOR_EXPIRES)) prefs.getLong(KEY_HAMMER_THOR_EXPIRES, 0L) else null
        _thorHammerExpiresAt.value = if (tExp != null && tExp > now) tExp else null

        // Evaluate Daily Tasks (Seeded by date)
        val today = TaskPools.todayDateString()
        val lastDate = prefs.getString(KEY_LAST_DAILY_DATE, null)
        if (lastDate != today) {
            val freshTasks = TaskPools.getDailyTasksForDate(today)
            _dailyTasks.value = freshTasks
            saveTasks(context, KEY_DAILY_TASKS_JSON, freshTasks)
            prefs.edit().putString(KEY_LAST_DAILY_DATE, today).apply()
        } else {
            _dailyTasks.value = loadTasks(context, KEY_DAILY_TASKS_JSON) ?: TaskPools.getDailyTasksForDate(today)
        }

        // Load weekly, monthly, yearly tasks
        _weeklyTasks.value = loadTasks(context, KEY_WEEKLY_TASKS_JSON) ?: TaskPools.WEEKLY_CORE_TASKS
        _monthlyTasks.value = loadTasks(context, KEY_MONTHLY_TASKS_JSON) ?: TaskPools.MONTHLY_MILESTONES
        _yearlyTasks.value = loadTasks(context, KEY_YEARLY_TASKS_JSON) ?: TaskPools.YEARLY_EPIC_GOALS

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
        _commonKeys.value = 0
        _normalKeys.value = 0
        _rareKeys.value = 0
        _legendaryKeys.value = 0
        _shields24h.value = 0
        _shields48h.value = 0
        _shields72h.value = 0
        getPrefs(context).edit()
            .putInt(KEY_KEYS_COMMON, 0)
            .putInt(KEY_KEYS_NORMAL, 0)
            .putInt(KEY_KEYS_RARE, 0)
            .putInt(KEY_KEYS_LEGENDARY, 0)
            .putInt(KEY_SHIELDS_24H, 0)
            .putInt(KEY_SHIELDS_48H, 0)
            .putInt(KEY_SHIELDS_72H, 0)
            .apply()
    }

    /**
     * Calculates the highest unlocked station (1 to 500) based on Total XP.
     * Formula: Required XP = Station ID * 100 XP
     */
    fun calculateUnlockedStationId(currentXp: Long): Int {
        val st = (currentXp / 100L).toInt().coerceIn(1, 500)
        return st
    }

    fun generateStations(totalXp: Long, claimedStations: Set<Int>): List<RoadmapStation> {
        val maxUnlocked = calculateUnlockedStationId(totalXp)
        return (1..500).map { id ->
            val reqXp = id * 100L
            val state = when {
                claimedStations.contains(id) -> StationState.CLAIMED
                id <= maxUnlocked -> StationState.UNLOCKED
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
        val unlockedMax = calculateUnlockedStationId(_totalXp.value)
        return if (stationId <= unlockedMax) StationState.UNLOCKED else StationState.LOCKED
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
    fun addKey(context: Context, rarity: KeyRarity, qty: Int = 1) {
        init(context)
        val prefs = getPrefs(context)
        when (rarity) {
            KeyRarity.COMMON -> {
                val newCount = _commonKeys.value + qty
                _commonKeys.value = newCount
                prefs.edit().putInt(KEY_KEYS_COMMON, newCount).apply()
            }
            KeyRarity.NORMAL -> {
                val newCount = _normalKeys.value + qty
                _normalKeys.value = newCount
                prefs.edit().putInt(KEY_KEYS_NORMAL, newCount).apply()
            }
            KeyRarity.RARE -> {
                val newCount = _rareKeys.value + qty
                _rareKeys.value = newCount
                prefs.edit().putInt(KEY_KEYS_RARE, newCount).apply()
            }
            KeyRarity.LEGENDARY -> {
                val newCount = _legendaryKeys.value + qty
                _legendaryKeys.value = newCount
                prefs.edit().putInt(KEY_KEYS_LEGENDARY, newCount).apply()
            }
        }
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
        return reward
    }

    private fun applyReward(context: Context, reward: StationReward) {
        val prefs = getPrefs(context)
        when (reward.type) {
            RewardType.SHIELD_24H -> {
                val newCount = _shields24h.value + reward.quantity
                _shields24h.value = newCount
                prefs.edit().putInt(KEY_SHIELDS_24H, newCount).apply()
                // Also give 1-day shield to FlameManager inventory
                FlameManager.grant1DayShield(context, reward.quantity)
            }
            RewardType.SHIELD_48H -> {
                val newCount = _shields48h.value + reward.quantity
                _shields48h.value = newCount
                prefs.edit().putInt(KEY_SHIELDS_48H, newCount).apply()
                FlameManager.grant1DayShield(context, reward.quantity * 2)
            }
            RewardType.SHIELD_72H -> {
                val newCount = _shields72h.value + reward.quantity
                _shields72h.value = newCount
                prefs.edit().putInt(KEY_SHIELDS_72H, newCount).apply()
                FlameManager.grantManualShield(context, days = 3)
            }
            RewardType.KEY_COMMON -> {
                val newCount = _commonKeys.value + reward.quantity
                _commonKeys.value = newCount
                prefs.edit().putInt(KEY_KEYS_COMMON, newCount).apply()
            }
            RewardType.KEY_NORMAL -> {
                val newCount = _normalKeys.value + reward.quantity
                _normalKeys.value = newCount
                prefs.edit().putInt(KEY_KEYS_NORMAL, newCount).apply()
            }
            RewardType.KEY_RARE -> {
                val newCount = _rareKeys.value + reward.quantity
                _rareKeys.value = newCount
                prefs.edit().putInt(KEY_KEYS_RARE, newCount).apply()
            }
            RewardType.KEY_LEGENDARY -> {
                val newCount = _legendaryKeys.value + reward.quantity
                _legendaryKeys.value = newCount
                prefs.edit().putInt(KEY_KEYS_LEGENDARY, newCount).apply()
            }
            RewardType.GIRL_BOOST_HAMMER_7D -> {
                activateHammer(context, RewardType.GIRL_BOOST_HAMMER_7D)
            }
            RewardType.DEFLAME_BOOST_HAMMER_7D -> {
                activateHammer(context, RewardType.DEFLAME_BOOST_HAMMER_7D)
            }
            RewardType.THOR_HAMMER_30D -> {
                activateHammer(context, RewardType.THOR_HAMMER_30D)
            }
        }
    }

    /**
     * Power Hammers Activation:
     * - Girl Boost: 7 Days (+50 pts / tap)
     * - Deflame Boost: 7 Days (+50 pts / tap)
     * - Thor's Hammer: 30 Days (-60% strike impact)
     */
    @Synchronized
    fun activateHammer(context: Context, hammerType: RewardType): Long {
        init(context)
        val now = System.currentTimeMillis()
        val prefs = getPrefs(context)

        return when (hammerType) {
            RewardType.GIRL_BOOST_HAMMER_7D -> {
                val base = maxOf(now, _girlBoostHammerExpiresAt.value ?: now)
                val exp = base + (7 * 24 * 3600 * 1000L)
                _girlBoostHammerExpiresAt.value = exp
                prefs.edit().putLong(KEY_HAMMER_GIRL_EXPIRES, exp).apply()
                exp
            }
            RewardType.DEFLAME_BOOST_HAMMER_7D -> {
                val base = maxOf(now, _deflameBoostHammerExpiresAt.value ?: now)
                val exp = base + (7 * 24 * 3600 * 1000L)
                _deflameBoostHammerExpiresAt.value = exp
                prefs.edit().putLong(KEY_HAMMER_DEFLAME_EXPIRES, exp).apply()
                exp
            }
            RewardType.THOR_HAMMER_30D -> {
                val base = maxOf(now, _thorHammerExpiresAt.value ?: now)
                val exp = base + (30 * 24 * 3600 * 1000L)
                _thorHammerExpiresAt.value = exp
                prefs.edit().putLong(KEY_HAMMER_THOR_EXPIRES, exp).apply()
                exp
            }
            else -> 0L
        }
    }

    fun isGirlBoostHammerActive(): Boolean {
        val exp = _girlBoostHammerExpiresAt.value ?: return false
        return System.currentTimeMillis() < exp
    }

    fun isDeflameBoostHammerActive(): Boolean {
        val exp = _deflameBoostHammerExpiresAt.value ?: return false
        return System.currentTimeMillis() < exp
    }

    fun isThorHammerActive(): Boolean {
        val exp = _thorHammerExpiresAt.value ?: return false
        return System.currentTimeMillis() < exp
    }

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
        if (!found) _yearlyTasks.value = updateList(_yearlyTasks.value).also { if (found) saveTasks(context, KEY_YEARLY_TASKS_JSON, it) }

        if (found && awardedXp > 0) {
            addXp(context, awardedXp)
        }
        return found
    }

    @Synchronized
    fun useSkipKeyForTask(context: Context, taskId: String): Boolean {
        init(context)
        // Find task to know required key rarity
        val allTasks = _dailyTasks.value + _weeklyTasks.value + _monthlyTasks.value + _yearlyTasks.value
        val task = allTasks.find { it.id == taskId } ?: return false
        if (task.status != TaskStatus.PENDING) return false

        val prefs = getPrefs(context)
        val hasKey = when (task.requiredKeyRarity) {
            KeyRarity.COMMON -> {
                if (_commonKeys.value > 0) {
                    val count = _commonKeys.value - 1
                    _commonKeys.value = count
                    prefs.edit().putInt(KEY_KEYS_COMMON, count).apply()
                    true
                } else false
            }
            KeyRarity.NORMAL -> {
                if (_normalKeys.value > 0) {
                    val count = _normalKeys.value - 1
                    _normalKeys.value = count
                    prefs.edit().putInt(KEY_KEYS_NORMAL, count).apply()
                    true
                } else false
            }
            KeyRarity.RARE -> {
                if (_rareKeys.value > 0) {
                    val count = _rareKeys.value - 1
                    _rareKeys.value = count
                    prefs.edit().putInt(KEY_KEYS_RARE, count).apply()
                    true
                } else false
            }
            KeyRarity.LEGENDARY -> {
                if (_legendaryKeys.value > 0) {
                    val count = _legendaryKeys.value - 1
                    _legendaryKeys.value = count
                    prefs.edit().putInt(KEY_KEYS_LEGENDARY, count).apply()
                    true
                } else false
            }
        }

        if (!hasKey) return false

        // Mark task SKIPPED and award full XP
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
        _yearlyTasks.value = updateList(_yearlyTasks.value).also { saveTasks(context, KEY_YEARLY_TASKS_JSON, it) }

        if (awardedXp > 0) {
            addXp(context, awardedXp)
        }
        return true
    }

    /**
     * Auto-increments progress for relevant matching tasks (e.g. "daily_deflame", "daily_girl", "strike").
     */
    @Synchronized
    fun onActionOccurred(context: Context, actionType: String) {
        init(context)
        var anyCompleted = false

        fun incrementMatching(list: List<TaskItem>): List<TaskItem> {
            return list.map { item ->
                if (item.status == TaskStatus.PENDING && shouldIncrement(item.id, actionType)) {
                    val newCount = item.currentCount + 1
                    if (newCount >= item.targetCount) {
                        anyCompleted = true
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
    }

    private fun shouldIncrement(taskId: String, actionType: String): Boolean {
        return when (actionType) {
            "DEFLAME" -> taskId.contains("deflame") || taskId.contains("alternating")
            "GIRL" -> taskId.contains("girl") || taskId.contains("alternating")
            "STRIKE" -> taskId.contains("strike")
            "SHIELD" -> taskId.contains("shield") || taskId.contains("defense")
            "DNA" -> taskId.contains("dna")
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
                put("requiredKeyRarity", item.requiredKeyRarity.name)
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
                        requiredKeyRarity = KeyRarity.valueOf(obj.optString("requiredKeyRarity", "COMMON"))
                    )
                )
            }
            list
        } catch (_: Exception) {
            null
        }
    }
}
