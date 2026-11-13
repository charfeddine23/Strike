package com.example.data.profile

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

        // Equipped avatar
        val eqAvId = prefs.getString(KEY_EQUIPPED_AVATAR_ID, "av_core_node") ?: "av_core_node"
        _equippedAvatar.value = ProfileMasterInventory.MASTER_AVATARS.find { it.id == eqAvId }
            ?: ProfileMasterInventory.MASTER_AVATARS.first()

        // Equipped title
        val eqTitleId = prefs.getInt(KEY_EQUIPPED_TITLE_ID, 1)
        _equippedTitle.value = if (eqTitleId == ProfileMasterInventory.MILESTONE_PEAK_TITLE.id) {
            ProfileMasterInventory.MILESTONE_PEAK_TITLE
        } else {
            ProfileMasterInventory.MASTER_TITLES.find { it.id == eqTitleId } ?: ProfileMasterInventory.MASTER_TITLES.first()
        }

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
    }

    /**
     * Called whenever Flame score hits 20,000 points.
     * Unlocks the exclusive peak milestone title "سيد الجمر التائب".
     */
    @Synchronized
    fun checkMilestonePeakUnlock(context: Context, flameScore: Double): Boolean {
        init(context)
        if (flameScore >= 20000.0) {
            if (!_unlockedTitleIds.value.contains(ProfileMasterInventory.MILESTONE_PEAK_TITLE.id)) {
                unlockTitle(context, ProfileMasterInventory.MILESTONE_PEAK_TITLE.id)
                return true
            }
        }
        return false
    }

    /**
     * Unlocks rewards associated with reaching a roadmap station (Avatars & Titles).
     */
    @Synchronized
    fun onStationUnlocked(context: Context, stationId: Int) {
        init(context)
        // Unlock matching titles
        ProfileMasterInventory.MASTER_TITLES.forEach { t ->
            if (t.unlockStation != null && t.unlockStation <= stationId) {
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
    fun grantPotion(context: Context, type: PotionType, qty: Int = 1) {
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
                activateGabrielDroplet(context)
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
    fun activateGabrielDroplet(context: Context) {
        init(context)
        val now = System.currentTimeMillis()
        val exp = now + 7 * 24 * 3600 * 1000L
        _gabrielDropletExpiresAt.value = exp
        getPrefs(context).edit()
            .putLong(KEY_GABRIEL_DROPLET_EXPIRES_AT, exp)
            .apply()
        com.example.data.roadmap.RoadmapManager.onActionOccurred(context, "POTION")
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
        unlockTitle(context, 50) // Unlock Gary Master
        getPrefs(context).edit()
            .putInt(KEY_POTION_ETERNAL_REBIRTH, newCount)
            .putBoolean(KEY_HAS_REBIRTH_ACTIVE, true)
            .apply()
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
     * Literally resets all unlocked avatars, titles, potions, active potion effects, and skip-keys back to base zero state.
     */
    @Synchronized
    fun resetAllProgressToZero(context: Context) {
        init(context)
        val defaultAvatar = ProfileMasterInventory.MASTER_AVATARS.first()
        val defaultTitle = ProfileMasterInventory.MASTER_TITLES.first()

        _unlockedAvatarIds.value = setOf(defaultAvatar.id)
        _unlockedTitleIds.value = setOf(defaultTitle.id)
        _equippedAvatar.value = defaultAvatar
        _equippedTitle.value = defaultTitle

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
            .putStringSet(KEY_UNLOCKED_AVATARS, setOf(defaultAvatar.id))
            .putStringSet(KEY_UNLOCKED_TITLES, setOf(defaultTitle.id.toString()))
            .putString(KEY_EQUIPPED_AVATAR_ID, defaultAvatar.id)
            .putInt(KEY_EQUIPPED_TITLE_ID, defaultTitle.id)
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
    fun resetForTesting(context: Context) {
        isInitialized = false
        init(context)
    }
}
