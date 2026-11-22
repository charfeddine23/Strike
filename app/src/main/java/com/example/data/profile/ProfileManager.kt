package com.example.data.profile

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * State & Persistence Manager for User Profile, Equipped Avatars, Active Titles,
 * Cybernetic Potions, and Skip-Keys Inventory.
 */
object ProfileManager {

    private const val PREFS_NAME = "strikepulse_profile_prefs"

    private const val KEY_USER_NAME = "key_user_name"
    private const val KEY_EQUIPPED_AVATAR_ID = "key_equipped_avatar_id"
    private const val KEY_EQUIPPED_TITLE_ID = "key_equipped_title_id"
    private const val KEY_UNLOCKED_AVATARS = "key_unlocked_avatars"
    private const val KEY_UNLOCKED_TITLES = "key_unlocked_titles"
    private const val KEY_TITLE_REGRIND_TARGETS_JSON = "key_title_regrind_targets_json"

    const val MILESTONE_PEAK_REGRIND_FLAME_GAIN: Long = 5_000L

    // Potions Inventory (Capacity cap: 3 per item)
    private const val KEY_POTION_PULSE_ELIXIR = "key_potion_pulse_elixir"
    private const val KEY_POTION_SHADOW_SERUM = "key_potion_shadow_serum"
    private const val KEY_POTION_GABRIEL_DROPLET = "key_potion_gabriel_droplet"
    private const val KEY_POTION_CRYO_INFUSION = "key_potion_cryo_infusion"
    private const val KEY_POTION_ETERNAL_REBIRTH = "key_potion_eternal_rebirth"

    // Active Potions Effects
    private const val KEY_PULSE_ELIXIR_USES_LEFT = "key_pulse_elixir_uses_left"
    private const val KEY_SHADOW_SERUM_EXPIRES_AT = "key_shadow_serum_expires_at"
    private const val KEY_GABRIEL_DROPLET_EXPIRES_AT = "key_gabriel_droplet_expires_at"
    private const val KEY_CRYO_INFUSION_AUTO_EXPIRES_AT = "key_cryo_infusion_auto_expires_at"
    private const val KEY_HAS_CRYO_BUFF_ARMED = "key_has_cryo_buff_armed"
    private const val KEY_HAS_REBIRTH_ACTIVE = "key_has_rebirth_active"

    // Skip Keys Inventory (Capacity cap: 30 keys per type)
    private const val KEY_KEYS_WOODEN = "key_keys_wooden"
    private const val KEY_KEYS_BRONZE = "key_keys_bronze"
    private const val KEY_KEYS_SILVER = "key_keys_silver"

    // StateFlows
    private val _userName = MutableStateFlow("Cyber Striker")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _equippedAvatar = MutableStateFlow(ProfileMasterInventory.MASTER_AVATARS.first())
    val equippedAvatar: StateFlow<AvatarItem> = _equippedAvatar.asStateFlow()

    private val _equippedTitle = MutableStateFlow(ProfileMasterInventory.MASTER_TITLES.first())
    val equippedTitle: StateFlow<TitleItem> = _equippedTitle.asStateFlow()

    private val _unlockedAvatarIds = MutableStateFlow<Set<String>>(setOf("av_core_node"))
    val unlockedAvatarIds: StateFlow<Set<String>> = _unlockedAvatarIds.asStateFlow()

    private val _unlockedTitleIds = MutableStateFlow<Set<Int>>(setOf(1))
    val unlockedTitleIds: StateFlow<Set<Int>> = _unlockedTitleIds.asStateFlow()

    private val _titleRegrindTargets = MutableStateFlow<Map<Int, TitleRegrindTarget>>(emptyMap())
    val titleRegrindTargets: StateFlow<Map<Int, TitleRegrindTarget>> = _titleRegrindTargets.asStateFlow()

    // Potions Inventory Counts (0..3)
    private val _pulseElixirCount = MutableStateFlow(0)
    val pulseElixirCount: StateFlow<Int> = _pulseElixirCount.asStateFlow()

    private val _shadowSerumCount = MutableStateFlow(0)
    val shadowSerumCount: StateFlow<Int> = _shadowSerumCount.asStateFlow()

    private val _gabrielDropletCount = MutableStateFlow(0)
    val gabrielDropletCount: StateFlow<Int> = _gabrielDropletCount.asStateFlow()

    private val _cryoInfusionCount = MutableStateFlow(0)
    val cryoInfusionCount: StateFlow<Int> = _cryoInfusionCount.asStateFlow()

    private val _eternalRebirthCount = MutableStateFlow(0)
    val eternalRebirthCount: StateFlow<Int> = _eternalRebirthCount.asStateFlow()

    // Active Effects Live State
    private val _pulseElixirUsesLeft = MutableStateFlow(0)
    val pulseElixirUsesLeft: StateFlow<Int> = _pulseElixirUsesLeft.asStateFlow()

    private val _shadowSerumExpiresAt = MutableStateFlow<Long?>(null)
    val shadowSerumExpiresAt: StateFlow<Long?> = _shadowSerumExpiresAt.asStateFlow()

    private val _gabrielDropletExpiresAt = MutableStateFlow<Long?>(null)
    val gabrielDropletExpiresAt: StateFlow<Long?> = _gabrielDropletExpiresAt.asStateFlow()

    private val _hasCryoBuffArmed = MutableStateFlow(false)
    val hasCryoBuffArmed: StateFlow<Boolean> = _hasCryoBuffArmed.asStateFlow()

    private val _hasRebirthImmunity = MutableStateFlow(false)
    val hasRebirthImmunity: StateFlow<Boolean> = _hasRebirthImmunity.asStateFlow()

    // Keys Counts (0..30)
    private val _woodenKeysCount = MutableStateFlow(0)
    val woodenKeysCount: StateFlow<Int> = _woodenKeysCount.asStateFlow()

    private val _bronzeKeysCount = MutableStateFlow(0)
    val bronzeKeysCount: StateFlow<Int> = _bronzeKeysCount.asStateFlow()

    private val _silverKeysCount = MutableStateFlow(0)
    val silverKeysCount: StateFlow<Int> = _silverKeysCount.asStateFlow()

    private var isInitialized = false

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        val prefs = getPrefs(context)

        _userName.value = prefs.getString(KEY_USER_NAME, "Cyber Striker") ?: "Cyber Striker"

        // Unlocked sets
        val savedAvatars = prefs.getStringSet(KEY_UNLOCKED_AVATARS, null) ?: setOf("av_core_node")
        _unlockedAvatarIds.value = savedAvatars

        val savedTitles = prefs.getStringSet(KEY_UNLOCKED_TITLES, null) ?: setOf("1")
        _unlockedTitleIds.value = savedTitles.mapNotNull { it.toIntOrNull() }.toSet()

        _titleRegrindTargets.value = parseRegrindTargets(prefs.getString(KEY_TITLE_REGRIND_TARGETS_JSON, null))

        // Equipped avatar
        val eqAvId = prefs.getString(KEY_EQUIPPED_AVATAR_ID, "av_core_node") ?: "av_core_node"
        _equippedAvatar.value = ProfileMasterInventory.MASTER_AVATARS.find { it.id == eqAvId }
            ?: ProfileMasterInventory.MASTER_AVATARS.first()

        // Equipped title (supports NO_TITLE_PLACEHOLDER id = 0 when all titles reset)
        val eqTitleId = prefs.getInt(KEY_EQUIPPED_TITLE_ID, 1)
        _equippedTitle.value = resolveTitleByIdIfUnlocked(eqTitleId, _unlockedTitleIds.value)

        // Potions counts
        _pulseElixirCount.value = prefs.getInt(KEY_POTION_PULSE_ELIXIR, 0)
        _shadowSerumCount.value = prefs.getInt(KEY_POTION_SHADOW_SERUM, 0)
        _gabrielDropletCount.value = prefs.getInt(KEY_POTION_GABRIEL_DROPLET, 0)
        _cryoInfusionCount.value = prefs.getInt(KEY_POTION_CRYO_INFUSION, 0)
        _eternalRebirthCount.value = prefs.getInt(KEY_POTION_ETERNAL_REBIRTH, 0)

        // Active Potion effects
        _pulseElixirUsesLeft.value = prefs.getInt(KEY_PULSE_ELIXIR_USES_LEFT, 0)

        val now = System.currentTimeMillis()
        val sExp = prefs.getLong(KEY_SHADOW_SERUM_EXPIRES_AT, 0L)
        _shadowSerumExpiresAt.value = if (sExp > now) sExp else null

        val gExp = prefs.getLong(KEY_GABRIEL_DROPLET_EXPIRES_AT, 0L)
        _gabrielDropletExpiresAt.value = if (gExp > now) gExp else null

        _hasCryoBuffArmed.value = prefs.getBoolean(KEY_HAS_CRYO_BUFF_ARMED, false)
        _hasRebirthImmunity.value = prefs.getBoolean(KEY_HAS_REBIRTH_ACTIVE, false)

        // Keys counts
        _woodenKeysCount.value = prefs.getInt(KEY_KEYS_WOODEN, 0)
        _bronzeKeysCount.value = prefs.getInt(KEY_KEYS_BRONZE, 0)
        _silverKeysCount.value = prefs.getInt(KEY_KEYS_SILVER, 0)

        isInitialized = true
    }

    @Synchronized
    fun setUserName(context: Context, name: String) {
        init(context)
        val trimmed = name.trim().ifBlank { "Cyber Striker" }
        _userName.value = trimmed
        getPrefs(context).edit().putString(KEY_USER_NAME, trimmed).apply()
    }

    @Synchronized
    fun equipAvatar(context: Context, avatarId: String): Boolean {
        init(context)
        if (!_unlockedAvatarIds.value.contains(avatarId)) return false
        val av = ProfileMasterInventory.MASTER_AVATARS.find { it.id == avatarId } ?: return false
        _equippedAvatar.value = av
        getPrefs(context).edit().putString(KEY_EQUIPPED_AVATAR_ID, avatarId).apply()
        return true
    }

    @Synchronized
    fun equipTitle(context: Context, titleId: Int): Boolean {
        init(context)
        if (!_unlockedTitleIds.value.contains(titleId)) return false
        val t = if (titleId == ProfileMasterInventory.MILESTONE_PEAK_TITLE.id) {
            ProfileMasterInventory.MILESTONE_PEAK_TITLE
        } else {
            ProfileMasterInventory.MASTER_TITLES.find { it.id == titleId } ?: return false
        }
        _equippedTitle.value = t
        getPrefs(context).edit().putInt(KEY_EQUIPPED_TITLE_ID, titleId).apply()
        return true
    }

    @Synchronized
    fun unlockAvatar(context: Context, avatarId: String) {
        init(context)
        val updated = _unlockedAvatarIds.value.toMutableSet()
        if (updated.add(avatarId)) {
            _unlockedAvatarIds.value = updated
            getPrefs(context).edit().putStringSet(KEY_UNLOCKED_AVATARS, updated).apply()
        }
    }

    @Synchronized
    fun unlockTitle(context: Context, titleId: Int) {
        init(context)
        val updated = _unlockedTitleIds.value.toMutableSet()
        if (updated.add(titleId)) {
            _unlockedTitleIds.value = updated
            getPrefs(context).edit()
                .putStringSet(KEY_UNLOCKED_TITLES, updated.map { it.toString() }.toSet())
                .apply()
        }
        // If no real title was equipped yet (NO_TITLE_PLACEHOLDER), auto-equip this newly unlocked title
        if (_equippedTitle.value.id == 0) {
            findTitleSpec(titleId)?.let { resolved ->
                _equippedTitle.value = resolved
                getPrefs(context).edit().putInt(KEY_EQUIPPED_TITLE_ID, resolved.id).apply()
            }
        }
    }

    /**
     * Resets an unlocked title back to Locked so the user can re-grind for it cleanly.
     * Should ONLY be invoked from the Profile Titles long-press (10s) gesture.
     */
    @Synchronized
    fun resetTitleForReGrind(context: Context, titleId: Int): TitleRegrindTarget? {
        init(context)
        if (!_unlockedTitleIds.value.contains(titleId)) return null
        val titleSpec = findTitleSpec(titleId) ?: return null

        com.example.data.roadmap.RoadmapManager.init(context)
        com.example.data.flame.FlameManager.init(context)

        val target = if (titleSpec.id == ProfileMasterInventory.MILESTONE_PEAK_TITLE.id) {
            val baselineFlame = com.example.data.flame.FlameManager.totalPointsGained.value
                .toLong()
                .coerceAtLeast(0L)
            TitleRegrindTarget(
                titleId = titleSpec.id,
                baselineProgress = baselineFlame,
                requiredGain = MILESTONE_PEAK_REGRIND_FLAME_GAIN
            )
        } else {
            val station = titleSpec.unlockStation ?: 1
            val baselineXp = com.example.data.roadmap.RoadmapManager.totalXp.value.coerceAtLeast(0L)
            val reqXp = com.example.data.roadmap.TaskPools.getRequiredXpForStation(station)
                .coerceAtLeast(com.example.data.roadmap.TaskPools.ROADMAP_LEVEL_1_XP)
            TitleRegrindTarget(
                titleId = titleSpec.id,
                baselineProgress = baselineXp,
                requiredGain = reqXp
            )
        }

        val updatedUnlocked = _unlockedTitleIds.value.toMutableSet().apply { remove(titleId) }
        _unlockedTitleIds.value = updatedUnlocked

        val updatedTargets = _titleRegrindTargets.value.toMutableMap().apply {
            put(titleId, target)
        }
        _titleRegrindTargets.value = updatedTargets

        val newEquipped = if (_equippedTitle.value.id == titleId) {
            fallbackEquippedTitleForUnlocked(updatedUnlocked)
        } else {
            _equippedTitle.value
        }
        _equippedTitle.value = newEquipped

        getPrefs(context).edit()
            .putStringSet(KEY_UNLOCKED_TITLES, updatedUnlocked.map { it.toString() }.toSet())
            .putInt(KEY_EQUIPPED_TITLE_ID, newEquipped.id)
            .putString(KEY_TITLE_REGRIND_TARGETS_JSON, regrindTargetsToJson(updatedTargets).toString())
            .apply()

        return target
    }

    /**
     * Checks whether any titles reset for re-grind have met their newly earned XP target.
     */
    @Synchronized
    fun onTotalXpUpdated(context: Context, currentTotalXp: Long) {
        init(context)
        val targets = _titleRegrindTargets.value
        if (targets.isEmpty()) return

        var modified = false
        val updatedTargets = targets.toMutableMap()
        val titlesToUnlock = mutableListOf<Int>()

        for ((titleId, target) in targets) {
            if (titleId == ProfileMasterInventory.MILESTONE_PEAK_TITLE.id) continue
            if (currentTotalXp < target.baselineProgress) {
                updatedTargets[titleId] = target.copy(baselineProgress = currentTotalXp.coerceAtLeast(0L))
                modified = true
                continue
            }
            val gained = currentTotalXp - target.baselineProgress
            if (gained >= target.requiredGain) {
                updatedTargets.remove(titleId)
                titlesToUnlock.add(titleId)
                modified = true
            }
        }

        if (modified) {
            _titleRegrindTargets.value = updatedTargets
            getPrefs(context).edit()
                .putString(KEY_TITLE_REGRIND_TARGETS_JSON, regrindTargetsToJson(updatedTargets).toString())
                .apply()
        }
        titlesToUnlock.forEach { tid -> unlockTitle(context, tid) }
    }

    /**
     * Called whenever Flame score hits 20,000 points or flame points accrue.
     * Unlocks the exclusive peak milestone title "سيد الجمر التائب", respecting any active re-grind challenge.
     */
    @Synchronized
    fun checkMilestonePeakUnlock(
        context: Context,
        flameScore: Double,
        totalGainedPoints: Double = 0.0
    ): Boolean {
        init(context)
        val peakId = ProfileMasterInventory.MILESTONE_PEAK_TITLE.id
        if (_unlockedTitleIds.value.contains(peakId)) return false

        val activeTarget = _titleRegrindTargets.value[peakId]
        if (activeTarget != null) {
            val currentTotalGained = totalGainedPoints.toLong().coerceAtLeast(0L)
            if (currentTotalGained < activeTarget.baselineProgress) {
                val updated = _titleRegrindTargets.value.toMutableMap().apply {
                    put(peakId, activeTarget.copy(baselineProgress = currentTotalGained))
                }
                _titleRegrindTargets.value = updated
                getPrefs(context).edit()
                    .putString(KEY_TITLE_REGRIND_TARGETS_JSON, regrindTargetsToJson(updated).toString())
                    .apply()
                return false
            }
            val gained = currentTotalGained - activeTarget.baselineProgress
            if (flameScore >= 20000.0 && gained >= activeTarget.requiredGain) {
                val updated = _titleRegrindTargets.value.toMutableMap().apply { remove(peakId) }
                _titleRegrindTargets.value = updated
                getPrefs(context).edit()
                    .putString(KEY_TITLE_REGRIND_TARGETS_JSON, regrindTargetsToJson(updated).toString())
                    .apply()
                unlockTitle(context, peakId)
                return true
            }
            return false
        }

        if (flameScore >= 20000.0) {
            unlockTitle(context, peakId)
            return true
        }
        return false
    }

    /**
     * Unlocks rewards associated with reaching a roadmap station (Avatars & Titles).
     * Titles currently in an active Re-Grind challenge are skipped until their fresh XP target is met.
     */
    @Synchronized
    fun onStationUnlocked(context: Context, stationId: Int) {
        init(context)
        val activeRegrindIds = _titleRegrindTargets.value.keys
        // Unlock matching titles (skipping any title intentionally reset for re-grind)
        ProfileMasterInventory.MASTER_TITLES.forEach { t ->
            if (t.unlockStation != null && t.unlockStation <= stationId && !activeRegrindIds.contains(t.id)) {
                unlockTitle(context, t.id)
            }
        }
        // Unlock matching avatars
        ProfileMasterInventory.MASTER_AVATARS.forEach { av ->
            if (av.unlockStation <= stationId) {
                unlockAvatar(context, av.id)
            }
        }
    }

    // POTIONS INVENTORY & USE

    @Synchronized
    fun grantPotion(context: Context, type: PotionType, qty: Int = 1, notifyRoadmapTask: Boolean = true) {
        init(context)
        val prefs = getPrefs(context)
        when (type) {
            PotionType.PULSE_ELIXIR -> {
                val newCount = (_pulseElixirCount.value + qty).coerceAtMost(3)
                _pulseElixirCount.value = newCount
                prefs.edit().putInt(KEY_POTION_PULSE_ELIXIR, newCount).apply()
            }
            PotionType.SHADOW_GRIP_SERUM -> {
                val newCount = (_shadowSerumCount.value + qty).coerceAtMost(3)
                _shadowSerumCount.value = newCount
                prefs.edit().putInt(KEY_POTION_SHADOW_SERUM, newCount).apply()
            }
            PotionType.GABRIEL_PURITY_DROPLET -> {
                // Instantly auto-activates upon receipt!
                activateGabrielDroplet(context, notifyRoadmapTask = notifyRoadmapTask)
            }
            PotionType.ABSOLUTE_CRYO_INFUSION -> {
                val newCount = (_cryoInfusionCount.value + qty).coerceAtMost(3)
                _cryoInfusionCount.value = newCount
                prefs.edit().putInt(KEY_POTION_CRYO_INFUSION, newCount).apply()
            }
            PotionType.ETERNAL_REBIRTH_ELIXIR -> {
                val newCount = (_eternalRebirthCount.value + qty).coerceAtMost(3)
                _eternalRebirthCount.value = newCount
                prefs.edit().putInt(KEY_POTION_ETERNAL_REBIRTH, newCount).apply()
            }
        }
    }

    @Synchronized
    fun usePulseElixir(context: Context): Boolean {
        init(context)
        if (_pulseElixirCount.value <= 0) return false
        val newCount = _pulseElixirCount.value - 1
        _pulseElixirCount.value = newCount
        _pulseElixirUsesLeft.value = 3
        getPrefs(context).edit()
            .putInt(KEY_POTION_PULSE_ELIXIR, newCount)
            .putInt(KEY_PULSE_ELIXIR_USES_LEFT, 3)
            .apply()
        com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "POTION")
        return true
    }

    /**
     * Checks if Pulse Elixir is active for Deflame. Consumes 1 use and returns +5.0 bonus.
     */
    @Synchronized
    fun consumePulseElixirUseIfActive(context: Context): Double {
        init(context)
        if (_pulseElixirUsesLeft.value > 0) {
            val left = _pulseElixirUsesLeft.value - 1
            _pulseElixirUsesLeft.value = left
            getPrefs(context).edit().putInt(KEY_PULSE_ELIXIR_USES_LEFT, left).apply()
            return 5.0
        }
        return 0.0
    }

    @Synchronized
    fun useShadowSerum(context: Context): Boolean {
        init(context)
        if (_shadowSerumCount.value <= 0) return false
        val newCount = _shadowSerumCount.value - 1
        val now = System.currentTimeMillis()
        val exp = now + 3 * 24 * 3600 * 1000L
        _shadowSerumCount.value = newCount
        _shadowSerumExpiresAt.value = exp
        getPrefs(context).edit()
            .putInt(KEY_POTION_SHADOW_SERUM, newCount)
            .putLong(KEY_SHADOW_SERUM_EXPIRES_AT, exp)
            .apply()
        com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "POTION")
        return true
    }

    fun isShadowSerumActive(): Boolean {
        val exp = _shadowSerumExpiresAt.value ?: return false
        return System.currentTimeMillis() < exp
    }

    @Synchronized
    fun activateGabrielDroplet(context: Context, notifyRoadmapTask: Boolean = true) {
        init(context)
        val now = System.currentTimeMillis()
        val exp = now + 7 * 24 * 3600 * 1000L
        _gabrielDropletExpiresAt.value = exp
        getPrefs(context).edit()
            .putLong(KEY_GABRIEL_DROPLET_EXPIRES_AT, exp)
            .apply()
        if (notifyRoadmapTask) {
            com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "POTION")
        }
    }

    fun isGabrielDropletActive(): Boolean {
        val exp = _gabrielDropletExpiresAt.value ?: return false
        return System.currentTimeMillis() < exp
    }

    @Synchronized
    fun useCryoInfusion(context: Context): Boolean {
        init(context)
        if (_cryoInfusionCount.value <= 0) return false
        val newCount = _cryoInfusionCount.value - 1
        _cryoInfusionCount.value = newCount
        _hasCryoBuffArmed.value = true
        getPrefs(context).edit()
            .putInt(KEY_POTION_CRYO_INFUSION, newCount)
            .putBoolean(KEY_HAS_CRYO_BUFF_ARMED, true)
            .apply()
        com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "POTION")
        return true
    }

    @Synchronized
    fun consumeCryoBuffIfArmed(context: Context): Double {
        init(context)
        if (_hasCryoBuffArmed.value) {
            _hasCryoBuffArmed.value = false
            getPrefs(context).edit().putBoolean(KEY_HAS_CRYO_BUFF_ARMED, false).apply()
            return 100.0
        }
        return 0.0
    }

    @Synchronized
    fun useEternalRebirth(context: Context): Boolean {
        init(context)
        if (_eternalRebirthCount.value <= 0) return false
        val newCount = _eternalRebirthCount.value - 1
        _eternalRebirthCount.value = newCount
        _hasRebirthImmunity.value = true
        val editor = getPrefs(context).edit()
            .putInt(KEY_POTION_ETERNAL_REBIRTH, newCount)
            .putBoolean(KEY_HAS_REBIRTH_ACTIVE, true)
        if (_titleRegrindTargets.value.containsKey(50)) {
            val updated = _titleRegrindTargets.value.toMutableMap().apply { remove(50) }
            _titleRegrindTargets.value = updated
            editor.putString(KEY_TITLE_REGRIND_TARGETS_JSON, regrindTargetsToJson(updated).toString())
        }
        editor.apply()
        unlockTitle(context, 50) // Unlock Gary Master
        com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "POTION")
        return true
    }

    // SKIP KEYS INVENTORY (Cap: 30 keys per type)

    @Synchronized
    fun grantSkipKeys(context: Context, type: SkipKeyType, qty: Int = 1) {
        init(context)
        val prefs = getPrefs(context)
        when (type) {
            SkipKeyType.WOODEN -> {
                val count = (_woodenKeysCount.value + qty).coerceAtMost(30)
                _woodenKeysCount.value = count
                prefs.edit().putInt(KEY_KEYS_WOODEN, count).apply()
            }
            SkipKeyType.BRONZE -> {
                val count = (_bronzeKeysCount.value + qty).coerceAtMost(30)
                _bronzeKeysCount.value = count
                prefs.edit().putInt(KEY_KEYS_BRONZE, count).apply()
            }
            SkipKeyType.SILVER -> {
                val count = (_silverKeysCount.value + qty).coerceAtMost(30)
                _silverKeysCount.value = count
                prefs.edit().putInt(KEY_KEYS_SILVER, count).apply()
            }
        }
    }

    @Synchronized
    fun consumeSkipKey(context: Context, type: SkipKeyType): Boolean {
        init(context)
        val prefs = getPrefs(context)
        return when (type) {
            SkipKeyType.WOODEN -> {
                if (_woodenKeysCount.value > 0) {
                    val count = _woodenKeysCount.value - 1
                    _woodenKeysCount.value = count
                    prefs.edit().putInt(KEY_KEYS_WOODEN, count).apply()
                    true
                } else false
            }
            SkipKeyType.BRONZE -> {
                if (_bronzeKeysCount.value > 0) {
                    val count = _bronzeKeysCount.value - 1
                    _bronzeKeysCount.value = count
                    prefs.edit().putInt(KEY_KEYS_BRONZE, count).apply()
                    true
                } else false
            }
            SkipKeyType.SILVER -> {
                if (_silverKeysCount.value > 0) {
                    val count = _silverKeysCount.value - 1
                    _silverKeysCount.value = count
                    prefs.edit().putInt(KEY_KEYS_SILVER, count).apply()
                    true
                } else false
            }
        }
    }

    @Synchronized
    fun resetSkipKeysToZero(context: Context) {
        init(context)
        _woodenKeysCount.value = 0
        _bronzeKeysCount.value = 0
        _silverKeysCount.value = 0
        getPrefs(context).edit()
            .putInt(KEY_KEYS_WOODEN, 0)
            .putInt(KEY_KEYS_BRONZE, 0)
            .putInt(KEY_KEYS_SILVER, 0)
            .apply()
    }

    /**
     * Resets consumables (potions, active potion effects, and skip-keys) to zero,
     * and clears/locks all unlocked Titles and Avatars back to their initial starter state.
     */
    @Synchronized
    fun resetAllProgressToZero(context: Context) {
        init(context)

        val initialAvatar = ProfileMasterInventory.MASTER_AVATARS.first()
        val initialTitle = ProfileMasterInventory.MASTER_TITLES.first()

        _equippedAvatar.value = initialAvatar
        _equippedTitle.value = initialTitle
        _unlockedAvatarIds.value = setOf(initialAvatar.id)
        _unlockedTitleIds.value = setOf(initialTitle.id)
        _titleRegrindTargets.value = emptyMap()

        _pulseElixirCount.value = 0
        _shadowSerumCount.value = 0
        _gabrielDropletCount.value = 0
        _cryoInfusionCount.value = 0
        _eternalRebirthCount.value = 0

        _pulseElixirUsesLeft.value = 0
        _shadowSerumExpiresAt.value = null
        _gabrielDropletExpiresAt.value = null
        _hasCryoBuffArmed.value = false
        _hasRebirthImmunity.value = false

        _woodenKeysCount.value = 0
        _bronzeKeysCount.value = 0
        _silverKeysCount.value = 0

        getPrefs(context).edit()
            .putString(KEY_EQUIPPED_AVATAR_ID, initialAvatar.id)
            .putInt(KEY_EQUIPPED_TITLE_ID, initialTitle.id)
            .putStringSet(KEY_UNLOCKED_AVATARS, setOf(initialAvatar.id))
            .putStringSet(KEY_UNLOCKED_TITLES, setOf(initialTitle.id.toString()))
            .remove(KEY_TITLE_REGRIND_TARGETS_JSON)
            .putInt(KEY_POTION_PULSE_ELIXIR, 0)
            .putInt(KEY_POTION_SHADOW_SERUM, 0)
            .putInt(KEY_POTION_GABRIEL_DROPLET, 0)
            .putInt(KEY_POTION_CRYO_INFUSION, 0)
            .putInt(KEY_POTION_ETERNAL_REBIRTH, 0)
            .putInt(KEY_PULSE_ELIXIR_USES_LEFT, 0)
            .remove(KEY_SHADOW_SERUM_EXPIRES_AT)
            .remove(KEY_GABRIEL_DROPLET_EXPIRES_AT)
            .remove(KEY_CRYO_INFUSION_AUTO_EXPIRES_AT)
            .putBoolean(KEY_HAS_CRYO_BUFF_ARMED, false)
            .putBoolean(KEY_HAS_REBIRTH_ACTIVE, false)
            .putInt(KEY_KEYS_WOODEN, 0)
            .putInt(KEY_KEYS_BRONZE, 0)
            .putInt(KEY_KEYS_SILVER, 0)
            .apply()
    }

    @Synchronized
    fun exportSnapshot(context: Context): JSONObject {
        init(context)
        val prefs = getPrefs(context)
        return JSONObject().apply {
            put("userName", _userName.value)
            put("equippedAvatarId", _equippedAvatar.value.id)
            put("equippedTitleId", _equippedTitle.value.id)
            put("unlockedAvatarIds", JSONArray(_unlockedAvatarIds.value.sorted()))
            put("unlockedTitleIds", JSONArray(_unlockedTitleIds.value.sorted()))
            put("titleRegrindTargets", regrindTargetsToJson(_titleRegrindTargets.value))

            put("pulseElixirCount", _pulseElixirCount.value)
            put("shadowSerumCount", _shadowSerumCount.value)
            put("gabrielDropletCount", _gabrielDropletCount.value)
            put("cryoInfusionCount", _cryoInfusionCount.value)
            put("eternalRebirthCount", _eternalRebirthCount.value)

            put("pulseElixirUsesLeft", _pulseElixirUsesLeft.value)
            put("shadowSerumExpiresAt", _shadowSerumExpiresAt.value ?: 0L)
            put("gabrielDropletExpiresAt", _gabrielDropletExpiresAt.value ?: 0L)
            put("cryoInfusionAutoExpiresAt", prefs.getLong(KEY_CRYO_INFUSION_AUTO_EXPIRES_AT, 0L))
            put("hasCryoBuffArmed", _hasCryoBuffArmed.value)
            put("hasRebirthImmunity", _hasRebirthImmunity.value)

            put("woodenKeysCount", _woodenKeysCount.value)
            put("bronzeKeysCount", _bronzeKeysCount.value)
            put("silverKeysCount", _silverKeysCount.value)
        }
    }

    @Synchronized
    fun restoreSnapshot(context: Context, json: JSONObject, replaceExisting: Boolean) {
        init(context)
        val prefs = getPrefs(context)
        val now = System.currentTimeMillis()

        val importedName = json.optString("userName", "").trim().ifBlank { _userName.value }
        val importedAvatarsArr = json.optJSONArray("unlockedAvatarIds")
        val importedAvatars = mutableSetOf("av_core_node")
        if (importedAvatarsArr != null) {
            for (i in 0 until importedAvatarsArr.length()) {
                importedAvatarsArr.optString(i).takeIf { it.isNotBlank() }?.let { importedAvatars.add(it) }
            }
        }
        val importedTitlesArr = json.optJSONArray("unlockedTitleIds")
        val importedTitles = if (importedTitlesArr != null) {
            mutableSetOf<Int>().apply {
                for (i in 0 until importedTitlesArr.length()) {
                    val tid = importedTitlesArr.optInt(i, -1)
                    if (tid > 0) add(tid)
                }
            }
        } else {
            mutableSetOf(1)
        }

        val importedRegrindMap = parseRegrindTargets(json.optJSONArray("titleRegrindTargets")?.toString())

        val finalAvatars = if (replaceExisting) importedAvatars else (_unlockedAvatarIds.value + importedAvatars)
        val finalTitles = if (replaceExisting) importedTitles else (_unlockedTitleIds.value + importedTitles)
        val finalRegrindTargets = if (replaceExisting) {
            importedRegrindMap.filterKeys { !finalTitles.contains(it) }
        } else {
            (_titleRegrindTargets.value + importedRegrindMap).filterKeys { !finalTitles.contains(it) }
        }

        val eqAvId = json.optString("equippedAvatarId", _equippedAvatar.value.id)
        val resolvedAvatar = ProfileMasterInventory.MASTER_AVATARS.find { it.id == eqAvId && finalAvatars.contains(it.id) }
            ?: ProfileMasterInventory.MASTER_AVATARS.first()

        val eqTitleId = json.optInt("equippedTitleId", _equippedTitle.value.id)
        val resolvedTitle = resolveTitleByIdIfUnlocked(eqTitleId, finalTitles)

        val pulse = if (replaceExisting) json.optInt("pulseElixirCount", 0) else (_pulseElixirCount.value + json.optInt("pulseElixirCount", 0))
        val shadow = if (replaceExisting) json.optInt("shadowSerumCount", 0) else (_shadowSerumCount.value + json.optInt("shadowSerumCount", 0))
        val gabriel = if (replaceExisting) json.optInt("gabrielDropletCount", 0) else (_gabrielDropletCount.value + json.optInt("gabrielDropletCount", 0))
        val cryo = if (replaceExisting) json.optInt("cryoInfusionCount", 0) else (_cryoInfusionCount.value + json.optInt("cryoInfusionCount", 0))
        val eternal = if (replaceExisting) json.optInt("eternalRebirthCount", 0) else (_eternalRebirthCount.value + json.optInt("eternalRebirthCount", 0))

        val pulseUses = if (replaceExisting) json.optInt("pulseElixirUsesLeft", 0) else maxOf(_pulseElixirUsesLeft.value, json.optInt("pulseElixirUsesLeft", 0))
        val rawShadowExp = json.optLong("shadowSerumExpiresAt", 0L)
        val shadowExp = if (replaceExisting) rawShadowExp else maxOf(_shadowSerumExpiresAt.value ?: 0L, rawShadowExp)
        val rawGabrielExp = json.optLong("gabrielDropletExpiresAt", 0L)
        val gabrielExp = if (replaceExisting) rawGabrielExp else maxOf(_gabrielDropletExpiresAt.value ?: 0L, rawGabrielExp)
        val cryoAutoExp = json.optLong("cryoInfusionAutoExpiresAt", 0L)

        val cryoArmed = if (replaceExisting) json.optBoolean("hasCryoBuffArmed", false) else (_hasCryoBuffArmed.value || json.optBoolean("hasCryoBuffArmed", false))
        val rebirthImmune = if (replaceExisting) json.optBoolean("hasRebirthImmunity", false) else (_hasRebirthImmunity.value || json.optBoolean("hasRebirthImmunity", false))

        val wooden = if (replaceExisting) json.optInt("woodenKeysCount", 0) else (_woodenKeysCount.value + json.optInt("woodenKeysCount", 0))
        val bronze = if (replaceExisting) json.optInt("bronzeKeysCount", 0) else (_bronzeKeysCount.value + json.optInt("bronzeKeysCount", 0))
        val silver = if (replaceExisting) json.optInt("silverKeysCount", 0) else (_silverKeysCount.value + json.optInt("silverKeysCount", 0))

        _userName.value = importedName
        _unlockedAvatarIds.value = finalAvatars
        _unlockedTitleIds.value = finalTitles
        _titleRegrindTargets.value = finalRegrindTargets
        _equippedAvatar.value = resolvedAvatar
        _equippedTitle.value = resolvedTitle

        _pulseElixirCount.value = pulse.coerceIn(0, 3)
        _shadowSerumCount.value = shadow.coerceIn(0, 3)
        _gabrielDropletCount.value = gabriel.coerceIn(0, 3)
        _cryoInfusionCount.value = cryo.coerceIn(0, 3)
        _eternalRebirthCount.value = eternal.coerceIn(0, 3)

        _pulseElixirUsesLeft.value = pulseUses.coerceAtLeast(0)
        _shadowSerumExpiresAt.value = if (shadowExp > now) shadowExp else null
        _gabrielDropletExpiresAt.value = if (gabrielExp > now) gabrielExp else null
        _hasCryoBuffArmed.value = cryoArmed
        _hasRebirthImmunity.value = rebirthImmune

        _woodenKeysCount.value = wooden.coerceIn(0, 30)
        _bronzeKeysCount.value = bronze.coerceIn(0, 30)
        _silverKeysCount.value = silver.coerceIn(0, 30)

        prefs.edit().apply {
            putString(KEY_USER_NAME, _userName.value)
            putStringSet(KEY_UNLOCKED_AVATARS, _unlockedAvatarIds.value)
            putStringSet(KEY_UNLOCKED_TITLES, _unlockedTitleIds.value.map { it.toString() }.toSet())
            if (finalRegrindTargets.isNotEmpty()) {
                putString(KEY_TITLE_REGRIND_TARGETS_JSON, regrindTargetsToJson(finalRegrindTargets).toString())
            } else {
                remove(KEY_TITLE_REGRIND_TARGETS_JSON)
            }
            putString(KEY_EQUIPPED_AVATAR_ID, resolvedAvatar.id)
            putInt(KEY_EQUIPPED_TITLE_ID, resolvedTitle.id)
            putInt(KEY_POTION_PULSE_ELIXIR, _pulseElixirCount.value)
            putInt(KEY_POTION_SHADOW_SERUM, _shadowSerumCount.value)
            putInt(KEY_POTION_GABRIEL_DROPLET, _gabrielDropletCount.value)
            putInt(KEY_POTION_CRYO_INFUSION, _cryoInfusionCount.value)
            putInt(KEY_POTION_ETERNAL_REBIRTH, _eternalRebirthCount.value)
            putInt(KEY_PULSE_ELIXIR_USES_LEFT, _pulseElixirUsesLeft.value)
            if (_shadowSerumExpiresAt.value != null) putLong(KEY_SHADOW_SERUM_EXPIRES_AT, _shadowSerumExpiresAt.value!!) else remove(KEY_SHADOW_SERUM_EXPIRES_AT)
            if (_gabrielDropletExpiresAt.value != null) putLong(KEY_GABRIEL_DROPLET_EXPIRES_AT, _gabrielDropletExpiresAt.value!!) else remove(KEY_GABRIEL_DROPLET_EXPIRES_AT)
            if (cryoAutoExp > 0L) putLong(KEY_CRYO_INFUSION_AUTO_EXPIRES_AT, cryoAutoExp) else remove(KEY_CRYO_INFUSION_AUTO_EXPIRES_AT)
            putBoolean(KEY_HAS_CRYO_BUFF_ARMED, _hasCryoBuffArmed.value)
            putBoolean(KEY_HAS_REBIRTH_ACTIVE, _hasRebirthImmunity.value)
            putInt(KEY_KEYS_WOODEN, _woodenKeysCount.value)
            putInt(KEY_KEYS_BRONZE, _bronzeKeysCount.value)
            putInt(KEY_KEYS_SILVER, _silverKeysCount.value)
            apply()
        }
    }

    private fun findTitleSpec(titleId: Int): TitleItem? = when (titleId) {
        ProfileMasterInventory.MILESTONE_PEAK_TITLE.id -> ProfileMasterInventory.MILESTONE_PEAK_TITLE
        else -> ProfileMasterInventory.MASTER_TITLES.find { it.id == titleId }
    }

    private fun fallbackEquippedTitleForUnlocked(unlockedIds: Set<Int>): TitleItem {
        val highestMaster = ProfileMasterInventory.MASTER_TITLES.lastOrNull { unlockedIds.contains(it.id) }
        if (highestMaster != null) return highestMaster
        if (unlockedIds.contains(ProfileMasterInventory.MILESTONE_PEAK_TITLE.id)) {
            return ProfileMasterInventory.MILESTONE_PEAK_TITLE
        }
        return ProfileMasterInventory.NO_TITLE_PLACEHOLDER
    }

    private fun resolveTitleByIdIfUnlocked(titleId: Int, unlockedIds: Set<Int>): TitleItem {
        if (titleId == 0) return ProfileMasterInventory.NO_TITLE_PLACEHOLDER
        val candidate = findTitleSpec(titleId)
        return if (candidate != null && unlockedIds.contains(candidate.id)) {
            candidate
        } else {
            fallbackEquippedTitleForUnlocked(unlockedIds)
        }
    }

    private fun parseRegrindTargets(rawJson: String?): Map<Int, TitleRegrindTarget> {
        if (rawJson.isNullOrBlank()) return emptyMap()
        return try {
            val arr = JSONArray(rawJson)
            val result = mutableMapOf<Int, TitleRegrindTarget>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val titleId = obj.optInt("titleId", -1)
                val baseline = obj.optLong("baselineProgress", 0L).coerceAtLeast(0L)
                val required = obj.optLong("requiredGain", 0L)
                if (titleId > 0 && required > 0L) {
                    result[titleId] = TitleRegrindTarget(
                        titleId = titleId,
                        baselineProgress = baseline,
                        requiredGain = required
                    )
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun regrindTargetsToJson(map: Map<Int, TitleRegrindTarget>): JSONArray {
        val arr = JSONArray()
        map.values.sortedBy { it.titleId }.forEach { target ->
            arr.put(JSONObject().apply {
                put("titleId", target.titleId)
                put("baselineProgress", target.baselineProgress)
                put("requiredGain", target.requiredGain)
            })
        }
        return arr
    }

    @Synchronized
    fun resetForTesting(context: Context) {
        isInitialized = false
        init(context)
    }
}
