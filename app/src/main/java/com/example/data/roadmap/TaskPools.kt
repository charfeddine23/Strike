package com.example.data.roadmap

import com.example.data.profile.SkipKeyType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random

/**
 * 1000-Task Matrix & Exponential Roadmap XP Engine for StrikePulse:
 * - Exactly 1000 Tasks across 4 Categories (250 Daily, 250 Weekly, 250 Monthly, 250 Annual/Everlasting)
 * - Total XP Pool across all 1000 tasks: Exactly 434,725 XP
 * - 500 Stations Exponential Curve: Station 1 = 50 XP, Station 500 = 434,725 XP
 * - Realistic Timeframe Scaling:
 *   * Daily (5 active slots): Light, practical 24h micro-discipline & single-action triggers
 *   * Weekly (7 active slots): 7-day consistency, streaks, strategic potions & moderate key allocation
 *   * Monthly (10 active slots): Medium-term systemic progression & structured milestones
 *   * Annual (all 250 displayed at once): High-tier macro-achievements (skip-keys strictly prohibited)
 * - Progressive Ordering: Catalogs and active slices are strictly ordered from easiest to hardest,
 *   with XP rewards scaling monotonically with true effort.
 */
object TaskPools {

    const val TOTAL_TASKS_COUNT = 1000
    const val CATEGORY_TASKS_COUNT = 250
    const val TOTAL_XP_POOL = 434725L

    private data class TaskBlueprint(
        val title: String,
        val description: String,
        val titleEn: String,
        val descriptionEn: String,
        val targetCount: Int,
        val difficultyTier: Int,  // 1 = Easy, 2 = Medium, 3 = Challenging, 4 = Elite
        val difficultyScore: Int, // Fine-grained effort score for monotonic ordering & XP mapping
        val actionTag: String
    )

    // ==========================================
    // 5-TIER REWARD MATRIX (BILINGUAL)
    // ==========================================

    val HIERARCHICAL_REWARDS = listOf(
        // Tier 1: 24h Protective Shield
        StationReward(
            type = RewardType.SHIELD_24H,
            title = "درع حماية 24 ساعة (تفعيل فوري)",
            description = "حماية فورية وتلقائية للشعلة من غرامة الـ 3 نقاط اليومية لمدة 24 ساعة",
            iconName = "shield_24h",
            quantity = 1,
            titleEn = "24h Protective Shield (Instant)",
            descriptionEn = "Immediate automatic flame protection against daily +3 inactivity penalty for 24 hours"
        ),
        // Tier 2: Wooden Skip Key
        StationReward(
            type = RewardType.KEY_WOODEN,
            title = "مفتاح تجاوز خشبي (Wooden Key)",
            description = "مفتاح باستعمال واحد لتجاوز المهام اليومية مع حصد نقاط XP فورياً",
            iconName = "key_wooden",
            quantity = 1,
            titleEn = "Wooden Skip Key",
            descriptionEn = "Single-use key to bypass eligible tasks and claim XP immediately"
        ),
        // Tier 3: Pulse Elixir
        StationReward(
            type = RewardType.POTION_PULSE_ELIXIR,
            title = "إكسير النبض (Pulse Elixir)",
            description = "يضيف +5 نقاط إضافية لزر التبريد Deflame للـ 3 تفعيلات القادمة",
            iconName = "potion_pulse",
            quantity = 1,
            titleEn = "Pulse Elixir",
            descriptionEn = "Adds +5 bonus cooldown power to your next 3 Deflame activations"
        ),
        // Tier 1: 48h Shield
        StationReward(
            type = RewardType.SHIELD_48H,
            title = "درع حماية 48 ساعة (تفعيل فوري)",
            description = "حماية فورية وتلقائية للشعلة من الخمول لمدة يومين كاملين",
            iconName = "shield_48h",
            quantity = 1,
            titleEn = "48h Protective Shield (Instant)",
            descriptionEn = "Immediate automatic flame protection against inactivity for 2 full days"
        ),
        // Tier 2: Bronze Skip Key
        StationReward(
            type = RewardType.KEY_BRONZE,
            title = "مفتاح تجاوز برونزي (Bronze Key)",
            description = "مفتاح استثنائي باستعمالين لتجاوز المهام وحصد نقاط XP فورياً",
            iconName = "key_bronze",
            quantity = 1,
            titleEn = "Bronze Skip Key",
            descriptionEn = "Tactical key with 2 skip charges for eligible tasks with instant XP"
        ),
        // Tier 3: Shadow-Grip Serum
        StationReward(
            type = RewardType.POTION_SHADOW_SERUM,
            title = "مصل قبضة الظل (Shadow-Grip Serum)",
            description = "يقلل وتيرة صعود نقاط الخمول بنسبة 50% لمدة 3 أيام متتالية",
            iconName = "potion_shadow",
            quantity = 1,
            titleEn = "Shadow-Grip Serum",
            descriptionEn = "Reduces inactivity flame buildup rate by 50% for 3 consecutive days"
        ),
        // Tier 1: 72h Shield
        StationReward(
            type = RewardType.SHIELD_72H,
            title = "درع حماية 72 ساعة (تفعيل فوري)",
            description = "حماية متينة للشعلة لمدة 3 أيام متتالية من أي غرامة خمول",
            iconName = "shield_72h",
            quantity = 1,
            titleEn = "72h Protective Shield (Instant)",
            descriptionEn = "Heavy-duty flame protection for 3 consecutive days against inactivity penalties"
        ),
        // Tier 2: Silver Skip Key
        StationReward(
            type = RewardType.KEY_SILVER,
            title = "مفتاح تجاوز فضي (Silver Key)",
            description = "مفتاح متقدم بثلاثة استعمالات لتجاوز المهام وحصد نقاط XP فورياً",
            iconName = "key_silver",
            quantity = 1,
            titleEn = "Silver Skip Key",
            descriptionEn = "Advanced key with 3 skip charges for tough eligible tasks with instant XP"
        ),
        // Tier 3: Gabriel's Purity Droplet
        StationReward(
            type = RewardType.POTION_GABRIEL_DROPLET,
            title = "قطرة نقاء جبريل (Gabriel's Droplet)",
            description = "تُسرّع نمو نقاط الشعلة بنسبة 50% لمدة 7 أيام (تفعيل تلقائي وفوري)",
            iconName = "potion_gabriel",
            quantity = 1,
            titleEn = "Gabriel's Purity Droplet",
            descriptionEn = "Accelerates daily flame energy growth by 50% for 7 days (instant auto-activation)"
        ),
        // Tier 3: Absolute Cryo-Infusion
        StationReward(
            type = RewardType.POTION_CRYO_INFUSION,
            title = "الحقن التجميدي المطلق (Cryo-Infusion)",
            description = "+100 نقطة بونص لتعزيز الستريك التالي (تفعيل اختياري)",
            iconName = "potion_cryo",
            quantity = 1,
            titleEn = "Absolute Cryo-Infusion",
            descriptionEn = "+100 bonus stability boost for your next strike (manual activation)"
        )
    )

    fun getRewardForStation(stationId: Int): StationReward {
        return when {
            stationId == 500 -> StationReward(
                type = RewardType.POTION_ETERNAL_REBIRTH,
                title = "جرعة الانبعاث الأبدي + لقب سيد النقوشة",
                description = "الجائزة الكبرى للمحطة 500: تمنح درع مناعة أبدي ضد التصفير ولقب سيد النقوشة (Gary Master)",
                iconName = "potion_rebirth",
                quantity = 1,
                titleEn = "Eternal Resurgence Potion + Gary Master Title",
                descriptionEn = "Station 500 Grand Prize: Grants permanent reset immunity shield and the legendary Gary Master title"
            )
            stationId % 20 == 0 -> StationReward(
                type = RewardType.KEY_SILVER,
                title = "مكافأة نخبوية: محطة #$stationId",
                description = "جائزة المحطة العشرينية: مفتاحان فضيان لتجاوز المهام وفتح ألقاب وأفاتار حصرية في الملف الشخصي",
                iconName = "key_silver",
                quantity = 2,
                titleEn = "Elite Milestone: Station #$stationId",
                descriptionEn = "Every-20-Station Reward: 2x Silver Skip Keys plus exclusive Profile Title & Avatar unlocks"
            )
            else -> {
                val idx = (stationId - 1) % HIERARCHICAL_REWARDS.size
                HIERARCHICAL_REWARDS[idx]
            }
        }
    }

    /**
     * Exponential Wavy Roadmap XP Curve (500 Stations):
     * - Station 1: 50 XP
     * - Station 500: exactly 434,725 XP
     */
    fun getRequiredXpForStation(stationId: Int): Long {
        val s = stationId.coerceIn(1, 500)
        if (s == 1) return 50L
        if (s == 500) return TOTAL_XP_POOL
        val ratio = 434725.0 / 50.0
        val power = (s - 1.0) / 499.0
        return Math.round(50.0 * Math.pow(ratio, power))
    }

    // ==========================================
    // MATHEMATICALLY EXACT XP GENERATOR
    // ==========================================

    private fun generateExactXpList(n: Int, minV: Int, maxV: Int, targetSum: Int): List<Int> {
        val vals = IntArray(n) { i ->
            Math.round(minV + (maxV - minV).toDouble() * i / (n - 1)).toInt()
        }
        var diff = targetSum - vals.sum()
        var idx = 1
        while (diff != 0) {
            val step = if (diff > 0) 1 else -1
            if (vals[idx] + step in minV..maxV) {
                vals[idx] += step
                diff -= step
            }
            idx = (idx % (n - 2)) + 1
        }
        return vals.sorted()
    }

    val DAILY_XP_LIST: List<Int> = generateExactXpList(250, 50, 240, 36250)
    val WEEKLY_XP_LIST: List<Int> = generateExactXpList(250, 100, 290, 48750)
    val MONTHLY_XP_LIST: List<Int> = generateExactXpList(250, 200, 635, 104375)
    val EVERLASTING_XP_LIST: List<Int> = generateExactXpList(250, 500, 1480, 245350)

    // ==========================================
    // 1. DAILY TASKS POOL (250 REALISTIC 24H TASKS)
    // ==========================================

    private val dailyContexts = listOf(
        "النبضة الصباحية" to "Morning Pulse",
        "تهدئة الظهيرة" to "Midday Cooldown",
        "موازنة المساء" to "Evening Balance",
        "بروتوكول الأمان" to "Safety Protocol",
        "تثبيت النواة" to "Core Stabilization",
        "تبريد فوري" to "Instant Chill",
        "حاجز الانضباط" to "Discipline Barrier",
        "ضبط التردد" to "Frequency Tuning",
        "حماية الشعلة" to "Flame Guard",
        "تصفية الشحنات" to "Charge Purge",
        "إعادة المعايرة" to "Recalibration",
        "تأمين السلسلة" to "Streak Lock",
        "التحكم الدقيق" to "Precision Control",
        "مراقبة الاستقرار" to "Stability Check",
        "ختم اليوم" to "Day Seal"
    )

    private val dailySingleShieldContexts = listOf(
        "الحفاظ على الدرع النشط اليوم" to "Keep Active Shield Today",
        "تأمين الشعلة بدرع حماية لـ 24 ساعة" to "Secure Flame with 24h Shield",
        "تفعيل حاجز الوقاية ضد غرامة الخمول" to "Activate Inactivity Penalty Barrier",
        "صيانة الدرع الواقي للنظام اليوم" to "Maintain System Defense Shield Today",
        "إبقاء الشعلة تحت الحماية الدرعية" to "Keep Flame Under Shield Cover",
        "تغطية دورة اليوم بدرع حماية واحد" to "Cover Today's Cycle with One Shield",
        "درع الثبات اليومي ضد ارتفاع الخمول" to "Daily Stability Shield vs Inactivity",
        "تحصين السلسلة بدرع واحد فعال" to "Fortify Streak with 1 Active Shield",
        "تثبيت دفاعات الشعلة لليوم الكامل" to "Lock Flame Defenses for Full Day",
        "درع الوقاية الأول لصد نقاط الخمول" to "First Defense Shield vs +3 Penalty",
        "تفعيل خط الدفاع الدرعي اليوم" to "Engage Shield Defense Line Today",
        "تأمين النقاط عبر درع حماية نشط" to "Secure Score via Active Shield",
        "درع الاستقرار اليومي للشعلة" to "Daily Flame Stability Shield",
        "مراقبة الدرع الفعال وتجنب التراكم" to "Verify Active Shield & Prevent Drift",
        "حماية الـ 24 ساعة بدرع واحد مفعّل" to "24h Protection with One Active Shield",
        "تفعيل الدرع الوقائي في الفترة الصباحية" to "Morning Shield Activation",
        "تفعيل الدرع الوقائي في الفترة المسائية" to "Evening Shield Activation",
        "تأمين الرصيد ضد غرامة +3 نقاط اليومية" to "Block Daily +3 Inactivity Penalty",
        "التحقق من استمرار صلاحية الدرع اليوم" to "Confirm Active Shield Coverage",
        "تغطية اليوم بدرع السلسلة الوقائي" to "Cover Day with Streak Shield"
    )

    val ALL_DAILY_TASKS: List<TaskItem> by lazy {
        val blueprints = mutableListOf<TaskBlueprint>()

        // 1. Deflame Micro-Discipline (60 tasks: targets 1..8)
        val deflameDistribution = listOf(
            1 to 15, 2 to 12, 3 to 10, 4 to 8, 5 to 6, 6 to 4, 7 to 3, 8 to 2
        )
        deflameDistribution.forEach { (target, count) ->
            for (k in 0 until count) {
                val (ctxAr, ctxEn) = dailyContexts[(k + target) % dailyContexts.size]
                val tier = when {
                    target <= 2 -> 1
                    target <= 4 -> 2
                    else -> 3
                }
                val countLabel = if (target == 1) "مرة واحدة" else "$target مرات"
                val enLabel = if (target == 1) "once" else "$target times"
                blueprints.add(
                    TaskBlueprint(
                        title = "تبريد Deflame ($countLabel) — $ctxAr",
                        description = "اضغط على زر التخفيض Deflame $countLabel خلال اليوم للحفاظ على هدوء الشعلة ($ctxAr)",
                        titleEn = "Deflame Cooldown ($enLabel) — $ctxEn",
                        descriptionEn = "Tap the Deflame cooldown button $enLabel today to keep flame energy calm ($ctxEn).",
                        targetCount = target,
                        difficultyTier = tier,
                        difficultyScore = 10 + target * 14 + k,
                        actionTag = "DEFLAME"
                    )
                )
            }
        }

        // 2. Girl Cooldown Micro-Discipline (60 tasks: targets 1..8)
        val girlDistribution = listOf(
            1 to 15, 2 to 12, 3 to 10, 4 to 8, 5 to 6, 6 to 4, 7 to 3, 8 to 2
        )
        girlDistribution.forEach { (target, count) ->
            for (k in 0 until count) {
                val (ctxAr, ctxEn) = dailyContexts[(k + target + 5) % dailyContexts.size]
                val tier = when {
                    target <= 2 -> 1
                    target <= 4 -> 2
                    else -> 3
                }
                val countLabel = if (target == 1) "مرة واحدة" else "$target مرات"
                val enLabel = if (target == 1) "once" else "$target times"
                blueprints.add(
                    TaskBlueprint(
                        title = "تبريد Girl ($countLabel) — $ctxAr",
                        description = "نفّذ تبريد النظام بالضغط على زر Girl $countLabel اليوم ضمن مسار ($ctxAr)",
                        titleEn = "Girl Cooldown ($enLabel) — $ctxEn",
                        descriptionEn = "Perform system cooldown using the Girl button $enLabel today ($ctxEn).",
                        targetCount = target,
                        difficultyTier = tier,
                        difficultyScore = 12 + target * 14 + k,
                        actionTag = "GIRL"
                    )
                )
            }
        }

        // 3. Alternating Thermal Balance (45 tasks: targets 1..6)
        val balanceDistribution = listOf(
            1 to 12, 2 to 10, 3 to 9, 4 to 6, 5 to 5, 6 to 3
        )
        balanceDistribution.forEach { (target, count) ->
            for (k in 0 until count) {
                val (ctxAr, ctxEn) = dailyContexts[(k * 2 + target) % dailyContexts.size]
                val tier = when {
                    target <= 2 -> 1
                    target <= 4 -> 2
                    else -> 3
                }
                val label = if (target == 1) "دورة واحدة" else "$target دورات"
                val enLabel = if (target == 1) "1 cycle" else "$target cycles"
                blueprints.add(
                    TaskBlueprint(
                        title = "توازن حراري ($label) — $ctxAr",
                        description = "نفّذ $label من التبريد والتخفيض (Deflame أو Girl) اليوم لضبط الإيقاع ($ctxAr)",
                        titleEn = "Thermal Balance ($enLabel) — $ctxEn",
                        descriptionEn = "Complete $enLabel of Deflame or Girl cooldown today to balance rhythm ($ctxEn).",
                        targetCount = target,
                        difficultyTier = tier,
                        difficultyScore = 18 + target * 16 + k,
                        actionTag = "BALANCE"
                    )
                )
            }
        }

        // 4. Realistic Daily Strikes (35 tasks: 1..3 strikes max per day)
        val strikeContexts = listOf(
            "الضربة النظيفة" to "Clean Strike",
            "تركيز الهدف" to "Target Focus",
            "تنفيذ دقيق" to "Precision Hit",
            "اختراق يومي" to "Daily Breakthrough",
            "نبضة الحسم" to "Decisive Pulse",
            "انضباط المسار" to "Track Discipline",
            "تسجيل موثق" to "Verified Log",
            "توقيت مثالي" to "Optimal Timing",
            "دقة الإصابة" to "Pinpoint Accuracy",
            "تثبيت السجل" to "Record Lock",
            "ضربة الاستهلال" to "Opening Strike",
            "ضربة الختام" to "Closing Strike",
            "مسار الصمود" to "Endurance Path",
            "ضربة الكفاءة" to "Efficiency Strike",
            "إنجاز فوري" to "Instant Execution",
            "تحدي اليوم" to "Today's Challenge",
            "إشارة التأكيد" to "Confirm Signal",
            "مناورة سريعة" to "Swift Maneuver"
        )
        for (k in 0 until 18) {
            val (sAr, sEn) = strikeContexts[k]
            blueprints.add(
                TaskBlueprint(
                    title = "تسجيل سترايك واحد اليوم — $sAr",
                    description = "نفّذ ضربة واحدة موثقة بالتاريخ والوقت دون تعريض الشعلة للضغط المفرط",
                    titleEn = "Log 1 Strike Today — $sEn",
                    descriptionEn = "Register 1 timestamped strike without exposing the flame to overload.",
                    targetCount = 1,
                    difficultyTier = 1,
                    difficultyScore = 22 + k,
                    actionTag = "STRIKE"
                )
            )
        }
        for (k in 0 until 11) {
            val (sAr, sEn) = strikeContexts[k]
            blueprints.add(
                TaskBlueprint(
                    title = "تنفيذ سترايكين (2) منضبطين اليوم — $sAr",
                    description = "سجّل ضربتين خلال 24 ساعة مع جاهزية الدرع الوقائي للحفاظ على الاستقرار",
                    titleEn = "Log 2 Controlled Strikes Today — $sEn",
                    descriptionEn = "Record 2 strikes within 24 hours while keeping protective stability ready.",
                    targetCount = 2,
                    difficultyTier = 2,
                    difficultyScore = 68 + k * 2,
                    actionTag = "STRIKE"
                )
            )
        }
        for (k in 0 until 6) {
            val (sAr, sEn) = strikeContexts[k]
            blueprints.add(
                TaskBlueprint(
                    title = "ثلاثية يومية (3 سترايك) لتفعيل الدرع — $sAr",
                    description = "سجّل 3 سترايكات اليوم لتفعيل درع الـ 7 أيام التلقائي دون تجاوز حد الحمل الأقصى",
                    titleEn = "Daily Trio (3 Strikes) Shield Trigger — $sEn",
                    descriptionEn = "Log 3 strikes today to trigger the automatic 7-day shield safely below overload.",
                    targetCount = 3,
                    difficultyTier = 3,
                    difficultyScore = 115 + k * 3,
                    actionTag = "STRIKE"
                )
            )
        }

        // 5. Daily Protective Shield Maintenance (20 tasks: strictly 1 action)
        dailySingleShieldContexts.forEachIndexed { idx, (labelAr, labelEn) ->
            blueprints.add(
                TaskBlueprint(
                    title = labelAr,
                    description = "فعّل درع حماية واحداً أو حافظ على بقاء درعك نشطاً لحماية الشعلة من غرامة الـ +3 نقاط اليوم",
                    titleEn = labelEn,
                    descriptionEn = "Activate 1 protective shield or keep an active barrier to prevent today's +3 inactivity penalty.",
                    targetCount = 1,
                    difficultyTier = 1,
                    difficultyScore = 16 + idx * 2,
                    actionTag = "SHIELD"
                )
            )
        }

        // 6. Daily Profile Logging (14 tasks: 1..2 individuals)
        for (k in 0 until 10) {
            val (ctxAr, ctxEn) = dailyContexts[k]
            blueprints.add(
                TaskBlueprint(
                    title = "إضافة فرد واحد للسجلات اليوم — $ctxAr",
                    description = "أضف ملفاً جديداً واحداً إلى السجلات مع احتساب الحجم والتعزيز تلقائياً",
                    titleEn = "Add 1 Profile to Roster Today — $ctxEn",
                    descriptionEn = "Add 1 new profile to records with automatic Size and Boost calculation.",
                    targetCount = 1,
                    difficultyTier = 1,
                    difficultyScore = 30 + k * 2,
                    actionTag = "PERSON"
                )
            )
        }
        for (k in 0 until 4) {
            val (ctxAr, ctxEn) = dailyContexts[k + 10]
            blueprints.add(
                TaskBlueprint(
                    title = "توثيق فردين (2) في السجلات اليوم — $ctxAr",
                    description = "سجّل شخصين جديدين في قاعدة البيانات خلال اليوم ($ctxAr)",
                    titleEn = "Log 2 New Profiles Today — $ctxEn",
                    descriptionEn = "Register 2 new individuals in the database today ($ctxEn).",
                    targetCount = 2,
                    difficultyTier = 2,
                    difficultyScore = 76 + k * 3,
                    actionTag = "PERSON"
                )
            )
        }

        // 7. Single Tactical Skip-Key Use (8 tasks: strictly 1 key max)
        val singleKeyLabels = listOf(
            "استخدام مفتاح تجاوز واحد بتكتيك — فك العقدة" to "Use 1 Tactical Skip Key — Unblock Path",
            "تخطي مهمة متوقفة بمفتاح واحد — تسريع المسار" to "Bypass 1 Stalled Task — Speed Boost",
            "استثمار مفتاح تجاوز فردي اليوم — كفاءة XP" to "Invest 1 Skip Key Today — XP Efficiency",
            "تفعيل مفتاح واحد عند الحاجة — إدارة الموارد" to "Use 1 Key on Demand — Resource Flow",
            "استعمال مفتاح تجاوز منفرد — توفير الجهد" to "Spend 1 Single Skip Key — Save Effort",
            "تحرير مهمة عالقة بمفتاح واحد — مرونة يومية" to "Clear 1 Blocker with 1 Key — Flexibility",
            "صرف مفتاح واحد بحكمة اليوم — خطة التقدم" to "Allocate 1 Key Wisely — Growth Plan",
            "استخدام مفتاح تجاوز واحد اليوم — دعم المحطة" to "Deploy 1 Skip Key Today — Station Support"
        )
        singleKeyLabels.forEachIndexed { idx, (labelAr, labelEn) ->
            blueprints.add(
                TaskBlueprint(
                    title = labelAr,
                    description = "استعمل مفتاح تجاوز واحداً فقط عند الحاجة لتحصيل نقاط XP دون هدر المخزون",
                    titleEn = labelEn,
                    descriptionEn = "Use just 1 skip key strategically to earn instant XP without wasting inventory.",
                    targetCount = 1,
                    difficultyTier = 2,
                    difficultyScore = 58 + idx * 2,
                    actionTag = "KEY"
                )
            )
        }

        // 8. Daily Tactical Potion / Station Claim (8 tasks: 1 action)
        for (k in 0 until 4) {
            blueprints.add(
                TaskBlueprint(
                    title = "تفعيل جرعة سيبرانية واحدة اليوم — تعزيز #${k + 1}",
                    description = "استفد من المخزون السيبراني بتفعيل جرعة واحدة داعمة للشعلة أو التبريد",
                    titleEn = "Activate 1 Cyber Potion Today — Boost #${k + 1}",
                    descriptionEn = "Use 1 potion from your cyber inventory to boost flame stability or cooldown.",
                    targetCount = 1,
                    difficultyTier = 2,
                    difficultyScore = 64 + k * 3,
                    actionTag = "POTION"
                )
            )
        }
        for (k in 0 until 4) {
            blueprints.add(
                TaskBlueprint(
                    title = "ضبط تردد التهدئة (ضغطتان) — مسار التوازن #${k + 1}",
                    description = "نفّذ ضغطتي تهدئة اليوم عبر زر Deflame أو Girl لتعزيز الانضباط اليومي",
                    titleEn = "Precision Cooldown Duo (2x) — Balance Route #${k + 1}",
                    descriptionEn = "Complete 2 Deflame or Girl cooldown taps today to reinforce daily discipline.",
                    targetCount = 2,
                    difficultyTier = 2,
                    difficultyScore = 60 + k * 3,
                    actionTag = "BALANCE"
                )
            )
        }

        val sorted = blueprints
            .take(CATEGORY_TASKS_COUNT)
            .sortedWith(
                compareBy<TaskBlueprint> { it.difficultyTier }
                    .thenBy { it.difficultyScore }
                    .thenBy { it.targetCount }
                    .thenBy { it.title }
            )

        sorted.mapIndexed { i, bp ->
            val xp = DAILY_XP_LIST[i]
            TaskItem(
                id = "daily_${i + 1}",
                period = TaskPeriod.DAILY,
                title = bp.title,
                description = bp.description,
                xpBounty = xp,
                targetCount = bp.targetCount,
                requiredSkipKey = if (xp < 150) SkipKeyType.WOODEN else SkipKeyType.BRONZE,
                actionTag = bp.actionTag,
                difficultyTier = bp.difficultyTier,
                titleEn = bp.titleEn,
                descriptionEn = bp.descriptionEn
            )
        }
    }

    // ==========================================
    // 2. WEEKLY TASKS POOL (250 7-DAY CONSISTENCY TASKS)
    // ==========================================

    private val weeklyContexts = listOf(
        "اتساق الأسبوع" to "Weekly Consistency",
        "موجة الثبات" to "Steadiness Wave",
        "إيقاع الـ 7 أيام" to "7-Day Rhythm",
        "بروتوكول الاستمرارية" to "Continuity Protocol",
        "دورة الصمود" to "Endurance Cycle",
        "انضباط الوسط" to "Midweek Discipline",
        "توازن المدى" to "Range Balance",
        "مناورة التراكم" to "Stacking Maneuver",
        "انضباط الجولات" to "Round Discipline",
        "درع الاستقرار" to "Stability Bastion",
        "خط التبريد" to "Cooling Line",
        "حزمة الإتقان" to "Mastery Pack",
        "خطة التحصين" to "Fortification Plan",
        "استراتيجية النبض" to "Pulse Strategy",
        "الذروة الأسبوعية" to "Weekly Peak"
    )

    val ALL_WEEKLY_TASKS: List<TaskItem> by lazy {
        val blueprints = mutableListOf<TaskBlueprint>()

        // 1. Weekly Deflame Consistency (55 tasks: 4..21 across 7 days)
        for (k in 0 until 55) {
            val target = 4 + (k * 18 / 55)
            val (ctxAr, ctxEn) = weeklyContexts[k % weeklyContexts.size]
            val sub = (k / weeklyContexts.size) + 1
            val tier = when {
                target <= 8 -> 1
                target <= 14 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "تبريد Deflame ($target مرة أسبوعياً) — $ctxAr ($sub)",
                    description = "وزّع $target عمليات تخفيض عبر زر Deflame على مدار أيام الأسبوع ($ctxAr)",
                    titleEn = "Weekly Deflame (${target}x) — $ctxEn ($sub)",
                    descriptionEn = "Distribute $target Deflame cooldown actions across the 7-day cycle ($ctxEn).",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 15 + target * 9 + (k % 3),
                    actionTag = "DEFLAME"
                )
            )
        }

        // 2. Weekly Girl Consistency (55 tasks: 4..21 across 7 days)
        for (k in 0 until 55) {
            val target = 4 + (k * 18 / 55)
            val (ctxAr, ctxEn) = weeklyContexts[(k + 4) % weeklyContexts.size]
            val sub = (k / weeklyContexts.size) + 1
            val tier = when {
                target <= 8 -> 1
                target <= 14 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "تبريد Girl ($target مرة أسبوعياً) — $ctxAr ($sub)",
                    description = "حافظ على هدوء النظام بواقع $target ضغطات تبريد موزعة خلال 7 أيام ($ctxAr)",
                    titleEn = "Weekly Girl Cooldown (${target}x) — $ctxEn ($sub)",
                    descriptionEn = "Keep system energy calm with $target Girl cooldown presses across 7 days ($ctxEn).",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 17 + target * 9 + (k % 3),
                    actionTag = "GIRL"
                )
            )
        }

        // 3. Weekly Alternating Balance (40 tasks: 4..18 across 7 days)
        for (k in 0 until 40) {
            val target = 4 + (k * 15 / 40)
            val (ctxAr, ctxEn) = weeklyContexts[(k + 7) % weeklyContexts.size]
            val sub = (k / weeklyContexts.size) + 1
            val tier = when {
                target <= 7 -> 1
                target <= 12 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "انضباط حراري أسبوعي ($target خطوة) — $ctxAr ($sub)",
                    description = "نفّذ $target جولة تبريد وتخفيض متزنة خلال الأسبوع للحفاظ على مسار الشعلة",
                    titleEn = "Weekly Thermal Discipline ($target Steps) — $ctxEn ($sub)",
                    descriptionEn = "Complete $target balanced Deflame/Girl cooldown steps this week ($ctxEn).",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 24 + target * 10 + (k % 2),
                    actionTag = "BALANCE"
                )
            )
        }

        // 4. Weekly Balanced Strikes (40 tasks: 2..12 strikes across 7 days)
        for (k in 0 until 40) {
            val target = 2 + (k * 11 / 40)
            val (ctxAr, ctxEn) = weeklyContexts[(k + 2) % weeklyContexts.size]
            val sub = (k / weeklyContexts.size) + 1
            val tier = when {
                target <= 4 -> 1
                target <= 8 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "تنفيذ $target سترايك خلال الأسبوع — $ctxAr ($sub)",
                    description = "سجّل $target ضربات موزعة بذكاء على أيام الأسبوع السبعة دون إفراط يومي",
                    titleEn = "Log $target Weekly Strikes — $ctxEn ($sub)",
                    descriptionEn = "Register $target well-paced strikes across the week without daily overload.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 26 + target * 15 + (k % 2),
                    actionTag = "STRIKE"
                )
            )
        }

        // 5. Weekly Streak & Shield Maintenance (22 tasks: 1..3 activations)
        for (k in 0 until 22) {
            val target = 1 + (k * 3 / 22)
            val (ctxAr, ctxEn) = weeklyContexts[k % weeklyContexts.size]
            val tier = if (target == 1) 1 else if (target == 2) 2 else 3
            blueprints.add(
                TaskBlueprint(
                    title = "حماية السلسلة بالدرع ($target مرات أسبوعياً) — $ctxAr",
                    description = "أمّن أسبوعك عبر تفعيل الدروع الوقائية ($target مرات) ومنع تراكم نقاط الخمول",
                    titleEn = "Weekly Shield Defense (${target}x) — $ctxEn",
                    descriptionEn = "Protect your weekly streak with $target shield activations against inactivity accrual.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 30 + target * 40 + k,
                    actionTag = "SHIELD"
                )
            )
        }

        // 6. Strategic Weekly Potions Usage (16 tasks: 1..3 potions)
        val potionNames = listOf(
            "إكسير النبض" to "Pulse Elixir",
            "مصل قبضة الظل" to "Shadow Serum",
            "قطرة جبريل" to "Gabriel's Droplet",
            "الحقن التجميدي" to "Cryo-Infusion"
        )
        for (k in 0 until 16) {
            val target = 1 + (k * 3 / 16)
            val (pAr, pEn) = potionNames[k % potionNames.size]
            val cycle = (k / potionNames.size) + 1
            val tier = if (target == 1) 2 else 3
            blueprints.add(
                TaskBlueprint(
                    title = "توظيف الجرعات الاستراتيجي (${target}x — $pAr #$cycle)",
                    description = "فعّل $target من الجرعات السيبرانية في الأوقات الحرجة لتعظيم فائدة الأسبوع",
                    titleEn = "Strategic Potion Timing (${target}x — $pEn #$cycle)",
                    descriptionEn = "Activate $target cyber potions at optimal moments this week.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 70 + target * 35 + k,
                    actionTag = "POTION"
                )
            )
        }

        // 7. Moderate Weekly Key Allocation (12 tasks: 1..3 keys)
        for (k in 0 until 12) {
            val target = 1 + (k * 3 / 12)
            val (ctxAr, ctxEn) = weeklyContexts[(k + 9) % weeklyContexts.size]
            val tier = if (target <= 2) 2 else 3
            blueprints.add(
                TaskBlueprint(
                    title = "تخصيص معتدل للمفاتيح ($target مفتاح) — $ctxAr",
                    description = "وزّع استخدام $target مفاتيح تجاوز خلال الأسبوع لتجاوز المهام الصعبة بحكمة",
                    titleEn = "Moderate Key Allocation ($target Keys) — $ctxEn",
                    descriptionEn = "Allocate $target skip keys wisely across the week to clear tough bottlenecks.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 60 + target * 32 + k,
                    actionTag = "KEY"
                )
            )
        }

        // 8. Weekly Records & Roadmap Stations (10 tasks: 5 PERSON + 5 ROADMAP)
        for (k in 0 until 5) {
            val target = 2 + k
            blueprints.add(
                TaskBlueprint(
                    title = "توسيع السجلات الأسبوعي ($target أفراد)",
                    description = "سجّل $target ملفات جديدة في السجلات خلال دورة هذا الأسبوع",
                    titleEn = "Weekly Roster Growth ($target Profiles)",
                    descriptionEn = "Register $target new individuals in your records during this week's cycle.",
                    targetCount = target,
                    difficultyTier = if (target <= 3) 1 else 2,
                    difficultyScore = 40 + target * 18,
                    actionTag = "PERSON"
                )
            )
        }
        for (k in 0 until 5) {
            val target = 5 + k * 2
            blueprints.add(
                TaskBlueprint(
                    title = "تثبيت التوازن الأسبوعي ($target خطوات متواصلة) — تحدي #${k + 1}",
                    description = "حافظ على ثبات المؤشر خلال الأسبوع بتنفيذ $target جولات تبريد وتخفيض (Deflame أو Girl)",
                    titleEn = "Weekly Equilibrium Lock ($target Steps) — Trial #${k + 1}",
                    descriptionEn = "Maintain weekly meter stability by completing $target Deflame/Girl cooldown steps.",
                    targetCount = target,
                    difficultyTier = if (target <= 7) 2 else 3,
                    difficultyScore = 65 + (k + 1) * 22,
                    actionTag = "BALANCE"
                )
            )
        }

        val sorted = blueprints
            .take(CATEGORY_TASKS_COUNT)
            .sortedWith(
                compareBy<TaskBlueprint> { it.difficultyTier }
                    .thenBy { it.difficultyScore }
                    .thenBy { it.targetCount }
                    .thenBy { it.title }
            )

        sorted.mapIndexed { i, bp ->
            val xp = WEEKLY_XP_LIST[i]
            TaskItem(
                id = "weekly_${i + 1}",
                period = TaskPeriod.WEEKLY,
                title = bp.title,
                description = bp.description,
                xpBounty = xp,
                targetCount = bp.targetCount,
                requiredSkipKey = when {
                    xp < 155 -> SkipKeyType.WOODEN
                    xp < 230 -> SkipKeyType.BRONZE
                    else -> SkipKeyType.SILVER
                },
                actionTag = bp.actionTag,
                difficultyTier = bp.difficultyTier,
                titleEn = bp.titleEn,
                descriptionEn = bp.descriptionEn
            )
        }
    }

    // ==========================================
    // 3. MONTHLY TASKS POOL (250 SYSTEMIC MILESTONES)
    // ==========================================

    private val monthlyContexts = listOf(
        "المرحلة التأسيسية" to "Foundation Phase",
        "البناء المنهجي" to "Systematic Build",
        "التكامل الشهري" to "Monthly Integration",
        "عمق النظام" to "System Depth",
        "الاستقرار الممتد" to "Extended Stability",
        "إدارة الدورة" to "Cycle Management",
        "مسار الصمود" to "Endurance Path",
        "هندسة الطاقة" to "Energy Engineering",
        "تحصين الشهر" to "Monthly Fortification",
        "ذروة التقدم" to "Progression Peak"
    )

    val ALL_MONTHLY_TASKS: List<TaskItem> by lazy {
        val blueprints = mutableListOf<TaskBlueprint>()

        // 1. Monthly Deflame Progression (55 tasks: 15..85)
        for (k in 0 until 55) {
            val target = 15 + (k * 71 / 55)
            val (ctxAr, ctxEn) = monthlyContexts[k % monthlyContexts.size]
            val rank = (k / monthlyContexts.size) + 1
            val tier = when {
                target <= 32 -> 1
                target <= 58 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "إنجاز $target عملية Deflame شهرية — $ctxAr ($rank)",
                    description = "حافظ على التوازن التراكمي للشعلة بإتمام $target تخفيضاً خلال هذا الشهر",
                    titleEn = "Monthly Deflame Milestone (${target}x) — $ctxEn ($rank)",
                    descriptionEn = "Maintain cumulative flame balance by completing $target Deflame cooldowns this month.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 15 + target * 3 + (k % 2),
                    actionTag = "DEFLAME"
                )
            )
        }

        // 2. Monthly Girl Progression (55 tasks: 15..85)
        for (k in 0 until 55) {
            val target = 15 + (k * 71 / 55)
            val (ctxAr, ctxEn) = monthlyContexts[(k + 3) % monthlyContexts.size]
            val rank = (k / monthlyContexts.size) + 1
            val tier = when {
                target <= 32 -> 1
                target <= 58 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "إنجاز $target تبريد Girl شهري — $ctxAr ($rank)",
                    description = "نفّذ $target جولة تبريد منتظمة عبر زر Girl على مدار الشهر الكامل",
                    titleEn = "Monthly Girl Cooldown (${target}x) — $ctxEn ($rank)",
                    descriptionEn = "Execute $target steady Girl cooldown rounds across the full monthly cycle.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 17 + target * 3 + (k % 2),
                    actionTag = "GIRL"
                )
            )
        }

        // 3. Monthly Balance Discipline (40 tasks: 15..75)
        for (k in 0 until 40) {
            val target = 15 + (k * 61 / 40)
            val (ctxAr, ctxEn) = monthlyContexts[(k + 5) % monthlyContexts.size]
            val rank = (k / monthlyContexts.size) + 1
            val tier = when {
                target <= 30 -> 1
                target <= 52 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "موازنة نظام شهرية ($target دورة) — $ctxAr ($rank)",
                    description = "ارفع كفاءة التحكم الحراري عبر $target عملية موازنة بين Deflame وGirl في الشهر",
                    titleEn = "Monthly System Balance ($target Cycles) — $ctxEn ($rank)",
                    descriptionEn = "Boost thermal control efficiency via $target combined Deflame/Girl rounds this month.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 25 + target * 4,
                    actionTag = "BALANCE"
                )
            )
        }

        // 4. Monthly Strike Milestones (40 tasks: 8..40)
        for (k in 0 until 40) {
            val target = 8 + (k * 33 / 40)
            val (ctxAr, ctxEn) = monthlyContexts[(k + 2) % monthlyContexts.size]
            val rank = (k / monthlyContexts.size) + 1
            val tier = when {
                target <= 15 -> 1
                target <= 27 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "تراكم $target سترايك خلال الشهر — $ctxAr ($rank)",
                    description = "سجّل $target ضربات موثقة موزعة بانتظام عبر أسابيع الشهر الأربعة",
                    titleEn = "Monthly Strike Accumulation ($target Strikes) — $ctxEn ($rank)",
                    descriptionEn = "Log $target verified strikes evenly paced across all four weeks of the month.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 30 + target * 6,
                    actionTag = "STRIKE"
                )
            )
        }

        // 5. Monthly Shield Defense Milestones (20 tasks: 2..8 shields)
        for (k in 0 until 20) {
            val target = 2 + (k * 7 / 20)
            val (ctxAr, ctxEn) = monthlyContexts[k % monthlyContexts.size]
            val tier = when {
                target <= 3 -> 1
                target <= 5 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "تحصين شهري شامل ($target دروع) — $ctxAr #${k + 1}",
                    description = "فعّل $target دروع حماية خلال الشهر لضمان بقاء السلسلة محمية من الخمول",
                    titleEn = "Monthly Shield Bastion ($target Shields) — $ctxEn #${k + 1}",
                    descriptionEn = "Activate $target protective shields this month to keep your streak guarded.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 35 + target * 28 + k,
                    actionTag = "SHIELD"
                )
            )
        }

        // 6. Monthly Potion Deployment (15 tasks: 2..6 potions)
        for (k in 0 until 15) {
            val target = 2 + (k * 5 / 15)
            val (ctxAr, ctxEn) = monthlyContexts[k % monthlyContexts.size]
            val tier = if (target <= 3) 2 else 3
            blueprints.add(
                TaskBlueprint(
                    title = "تكتيك الجرعات الشهري ($target جرعات) — $ctxAr",
                    description = "استهلك $target جرعات سيبرانية في المفاصل الزمنية للشهر لتحقيق أقصى عائد",
                    titleEn = "Monthly Potion Tactics ($target Potions) — $ctxEn",
                    descriptionEn = "Deploy $target cyber potions at strategic moments throughout the month.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 110 + target * 25 + k,
                    actionTag = "POTION"
                )
            )
        }

        // 7. Monthly Key Economy (10 tasks: 2..6 keys)
        for (k in 0 until 10) {
            val target = 2 + (k * 5 / 10)
            val (ctxAr, ctxEn) = monthlyContexts[k % monthlyContexts.size]
            val tier = if (target <= 3) 2 else 3
            blueprints.add(
                TaskBlueprint(
                    title = "إدارة مفاتيح التجاوز الشهرية ($target مفاتيح) — $ctxAr",
                    description = "وظّف $target مفاتيح تجاوز خلال الشهر لفتح اختناقات التقدم وحصد الـ XP",
                    titleEn = "Monthly Skip-Key Economy ($target Keys) — $ctxEn",
                    descriptionEn = "Use $target skip keys across the month to clear bottlenecks and harvest XP.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 100 + target * 24 + k,
                    actionTag = "KEY"
                )
            )
        }

        // 8. Monthly Database Growth & Roadmap Progression (15 tasks: 8 PERSON + 7 ROADMAP)
        for (k in 0 until 8) {
            val target = 4 + (k * 12 / 8)
            blueprints.add(
                TaskBlueprint(
                    title = "توسعة السجلات الشهرية ($target ملفات جديدة)",
                    description = "أضف $target أفراد جدد إلى شبكة السجلات والتحليل خلال الشهر الحالي",
                    titleEn = "Monthly Roster Expansion ($target Profiles)",
                    descriptionEn = "Add $target new individuals to your database and analytics network this month.",
                    targetCount = target,
                    difficultyTier = when {
                        target <= 6 -> 1
                        target <= 10 -> 2
                        else -> 3
                    },
                    difficultyScore = 45 + target * 14,
                    actionTag = "PERSON"
                )
            )
        }
        for (k in 0 until 7) {
            val target = 20 + k * 8
            blueprints.add(
                TaskBlueprint(
                    title = "درع الاستمرارية الشهري ($target جولة متزنة) — مستوى #${k + 1}",
                    description = "حافظ على إيقاع التحكم الحراري بتسجيل $target جولة Deflame أو Girl خلال الشهر الكامل",
                    titleEn = "Monthly Continuity Guard ($target Balanced Rounds) — Stage #${k + 1}",
                    descriptionEn = "Sustain thermal regulation with $target combined Deflame or Girl rounds this month.",
                    targetCount = target,
                    difficultyTier = if (target <= 36) 2 else 3,
                    difficultyScore = 90 + (3 + k * 3) * 10,
                    actionTag = "BALANCE"
                )
            )
        }

        val sorted = blueprints
            .take(CATEGORY_TASKS_COUNT)
            .sortedWith(
                compareBy<TaskBlueprint> { it.difficultyTier }
                    .thenBy { it.difficultyScore }
                    .thenBy { it.targetCount }
                    .thenBy { it.title }
            )

        sorted.mapIndexed { i, bp ->
            val xp = MONTHLY_XP_LIST[i]
            TaskItem(
                id = "monthly_${i + 1}",
                period = TaskPeriod.MONTHLY,
                title = bp.title,
                description = bp.description,
                xpBounty = xp,
                targetCount = bp.targetCount,
                requiredSkipKey = if (xp < 400) SkipKeyType.BRONZE else SkipKeyType.SILVER,
                actionTag = bp.actionTag,
                difficultyTier = bp.difficultyTier,
                titleEn = bp.titleEn,
                descriptionEn = bp.descriptionEn
            )
        }
    }

    // ==========================================
    // 4. ANNUAL TASKS POOL (250 MACRO-ACHIEVEMENTS, NO SKIPS ALLOWED)
    // ==========================================

    private val annualTiers = listOf(
        "الأساس السنوي" to "Annual Foundation",
        "الالتزام المستدام" to "Sustained Commitment",
        "السيطرة الاستراتيجية" to "Strategic Control",
        "المجد السيبراني" to "Cyber Glory",
        "الذروة الأبدية" to "Eternal Zenith"
    )

    val ALL_EVERLASTING_TASKS: List<TaskItem> by lazy {
        val blueprints = mutableListOf<TaskBlueprint>()

        // 1. Annual Macro Strikes Ladder (55 tasks: 10 -> 500 verified cumulative strikes)
        for (k in 0 until 55) {
            val target = 10 + (k * 490 / 54)
            val (phaseAr, phaseEn) = annualTiers[(k * annualTiers.size / 55).coerceAtMost(4)]
            val tier = when {
                target <= 60 -> 1
                target <= 175 -> 2
                target <= 325 -> 3
                else -> 4
            }
            blueprints.add(
                TaskBlueprint(
                    title = "إنجاز سنوي: $target سترايك موثق ($phaseAr)",
                    description = "تحقيق حاجز $target سترايك فعلي وحقيقي على مدار السنة دون أي مفتاح تجاوز",
                    titleEn = "Annual Achievement: $target Verified Strikes ($phaseEn)",
                    descriptionEn = "Reach $target genuine logged strikes across the year — skip keys prohibited.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 50 + target * 2,
                    actionTag = "STRIKE"
                )
            )
        }

        // 2. Annual Macro Deflame Discipline (45 tasks: 50 -> 1000 cumulative actions)
        for (k in 0 until 45) {
            val target = 50 + (k * 950 / 44)
            val (phaseAr, phaseEn) = annualTiers[(k * annualTiers.size / 45).coerceAtMost(4)]
            val tier = when {
                target <= 180 -> 1
                target <= 450 -> 2
                target <= 750 -> 3
                else -> 4
            }
            blueprints.add(
                TaskBlueprint(
                    title = "سيادة التبريد السنوية: $target ضغطة Deflame ($phaseAr)",
                    description = "إتمام $target عملية تخفيض حراري فعلية عبر زر Deflame خلال رحلة العام",
                    titleEn = "Annual Deflame Mastery: $target Presses ($phaseEn)",
                    descriptionEn = "Complete $target genuine Deflame cooldowns throughout your annual journey.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 45 + target,
                    actionTag = "DEFLAME"
                )
            )
        }

        // 3. Annual Macro Girl Discipline (45 tasks: 50 -> 1000 cumulative actions)
        for (k in 0 until 45) {
            val target = 50 + (k * 950 / 44)
            val (phaseAr, phaseEn) = annualTiers[(k * annualTiers.size / 45).coerceAtMost(4)]
            val tier = when {
                target <= 180 -> 1
                target <= 450 -> 2
                target <= 750 -> 3
                else -> 4
            }
            blueprints.add(
                TaskBlueprint(
                    title = "انضباط Girl السنوي: $target عملية تبريد ($phaseAr)",
                    description = "تسجيل $target تفعيل تبريد فعلي لزر Girl على مدار المسار السنوي",
                    titleEn = "Annual Girl Discipline: $target Cooldowns ($phaseEn)",
                    descriptionEn = "Log $target real Girl cooldown activations along your annual path.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 47 + target,
                    actionTag = "GIRL"
                )
            )
        }

        // 4. Annual Thermal Balance (30 tasks: 50 -> 800 cumulative actions)
        for (k in 0 until 30) {
            val target = 50 + (k * 750 / 29)
            val (phaseAr, phaseEn) = annualTiers[(k * annualTiers.size / 30).coerceAtMost(4)]
            val tier = when {
                target <= 150 -> 1
                target <= 360 -> 2
                target <= 600 -> 3
                else -> 4
            }
            blueprints.add(
                TaskBlueprint(
                    title = "الاتزان الحراري السنوي: $target دورة ($phaseAr)",
                    description = "الجمع الموثق بين Deflame وGirl بواقع $target عملية متوازنة سنوياً",
                    titleEn = "Annual Thermal Equilibrium: $target Cycles ($phaseEn)",
                    descriptionEn = "Accumulate $target combined Deflame and Girl balance cycles across the year.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 60 + target + (target / 4),
                    actionTag = "BALANCE"
                )
            )
        }

        // 5. Annual Archive Expansion (25 tasks: 5 -> 120 registered profiles)
        for (k in 0 until 25) {
            val target = 5 + (k * 115 / 24)
            val (phaseAr, phaseEn) = annualTiers[(k * annualTiers.size / 25).coerceAtMost(4)]
            val tier = when {
                target <= 20 -> 1
                target <= 55 -> 2
                target <= 90 -> 3
                else -> 4
            }
            blueprints.add(
                TaskBlueprint(
                    title = "شبكة السجلات السنوية: $target ملفاً مسجلاً ($phaseAr)",
                    description = "بناء وتوثيق $target ملفات مكتملة البيانات داخل قاعدة بيانات التطبيق",
                    titleEn = "Annual Archive Network: $target Profiles ($phaseEn)",
                    descriptionEn = "Build and document $target complete individual profiles in the database.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 80 + target * 8,
                    actionTag = "PERSON"
                )
            )
        }

        // 6. Annual Shield & Potion Mastery (20 tasks: 10 SHIELD + 10 POTION)
        for (k in 0 until 10) {
            val target = 5 + k * 3
            val tier = when {
                target <= 10 -> 1
                target <= 18 -> 2
                target <= 25 -> 3
                else -> 4
            }
            blueprints.add(
                TaskBlueprint(
                    title = "درع الصمود السنوي: تفعيل $target دروع حماية",
                    description = "حماية استمرارية الشعلة عبر $target تفعيلات فعلية للدروع الواقية في السنة",
                    titleEn = "Annual Resilience Shield: $target Activations",
                    descriptionEn = "Guard your flame continuity through $target real protective shield activations.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 120 + target * 28,
                    actionTag = "SHIELD"
                )
            )
        }
        for (k in 0 until 10) {
            val target = 3 + k * 2
            val tier = when {
                target <= 7 -> 2
                target <= 15 -> 3
                else -> 4
            }
            blueprints.add(
                TaskBlueprint(
                    title = "خيمياء السيبرانية السنوية: $target جرعات مفعّلة",
                    description = "استخدام $target جرعات تكتيكية حقيقية من مخزونك لدعم الأداء السنوي",
                    titleEn = "Annual Cyber Alchemy: $target Potions Used",
                    descriptionEn = "Activate $target real tactical potions from your inventory across the year.",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 220 + target * 40,
                    actionTag = "POTION"
                )
            )
        }

        // 7. Annual Cumulative Cooldown & Discipline Pacts (15 tasks: 90 -> 1650 cumulative rounds)
        val disciplineMilestones = listOf(90, 160, 240, 330, 420, 520, 620, 730, 840, 960, 1080, 1200, 1350, 1500, 1650)
        disciplineMilestones.forEachIndexed { idx, cycleTarget ->
            val tier = when {
                idx <= 2 -> 1
                idx <= 6 -> 2
                idx <= 11 -> 3
                else -> 4
            }
            blueprints.add(
                TaskBlueprint(
                    title = "ميثاق الانضباط السنوي: إتمام $cycleTarget دورة تبريد وتوازن",
                    description = "الالتزام المستدام بتنفيذ $cycleTarget عملية تبريد وتخفيض متزنة على مدار العام (دون أي مفاتيح تجاوز)",
                    titleEn = "Annual Discipline Pact: $cycleTarget Cooldown & Balance Cycles",
                    descriptionEn = "Sustain year-long commitment with $cycleTarget balanced Deflame/Girl actions — no skip keys allowed.",
                    targetCount = cycleTarget,
                    difficultyTier = tier,
                    difficultyScore = 100 + idx * 75,
                    actionTag = "BALANCE"
                )
            )
        }

        // 8. Annual Apex Flame & Bio-Calibration Feats (15 tasks: Genuine high-tier mastery)
        data class ApexFeat(
            val titleAr: String,
            val descAr: String,
            val titleEn: String,
            val descEn: String,
            val tier: Int,
            val score: Int
        )
        val apexFeats = listOf(
            ApexFeat(
                "الاستقرار النوراني الأول: بلوغ 1,500 نقطة شعلة",
                "رفع رصيد الشعلة الفعلي إلى 1,500 نقطة والحفاظ على نقاء المسار",
                "First Luminous Stability: 1,500 Flame Pts",
                "Raise your live flame score to 1,500 pts with genuine consistency.",
                1, 180
            ),
            ApexFeat(
                "عتبة الشعلة النورانية: بلوغ 2,000 نقطة صافية",
                "تجاوز حاجز 2,000 نقطة والدخول إلى مسار النور والثبات",
                "Luminous Threshold: 2,000 Pure Flame Pts",
                "Cross 2,000 flame pts and step onto the Light Path.",
                1, 240
            ),
            ApexFeat(
                "معايرة حيوية متقدمة: بلوغ 60% تيستوستيرون",
                "ضبط المؤشر الحيوي عبر الالتزام الحقيقي للوصول إلى 60% تيستوستيرون",
                "Bio Calibration: 60% Testosterone",
                "Calibrate your biological index through real discipline to 60% Testosterone.",
                2, 360
            ),
            ApexFeat(
                "الارتقاء النوراني: بلوغ 3,500 نقطة شعلة",
                "تعزيز الانضباط اليومي حتى الوصول الفعلي لـ 3,500 نقطة",
                "Radiant Ascent: 3,500 Flame Pts",
                "Sustain daily discipline to reach 3,500 live flame points.",
                2, 440
            ),
            ApexFeat(
                "معايرة حيوية عالية: بلوغ 75% تيستوستيرون",
                "الحفاظ على الانضباط المتراكم لرفع المؤشر إلى 75% تيستوستيرون",
                "High Bio Calibration: 75% Testosterone",
                "Maintain cumulative momentum to raise your index to 75% Testosterone.",
                2, 540
            ),
            ApexFeat(
                "صمود منتصف الطريق: بلوغ 5,000 نقطة شعلة",
                "تحقيق نصف المسافة نحو ذروة النور (5,000 PTS) بجهد فعلي متواصل",
                "Midway Steadfastness: 5,000 Flame Pts",
                "Achieve 5,000 live flame pts through uninterrupted effort.",
                2, 620
            ),
            ApexFeat(
                "السيادة الحيوية: بلوغ 90% تيستوستيرون",
                "الوصول إلى مستوى 90% تيستوستيرون في سجل التطورات الجينية",
                "Biological Sovereignty: 90% Testosterone",
                "Reach the 90% Testosterone mark in the biological progression tracker.",
                3, 740
            ),
            ApexFeat(
                "إشراق النخبة: بلوغ 7,500 نقطة شعلة",
                "بلوغ 7,500 نقطة في عداد الشعلة دون تصفير أو حمل زائد",
                "Elite Radiance: 7,500 Flame Pts",
                "Reach 7,500 flame points without resets or overload.",
                3, 820
            ),
            ApexFeat(
                "نقاء الـ DNA الكامل: بلوغ 100% تيستوستيرون",
                "إتقان المؤشر الجيني للوصول إلى رتبة هرقل (100% تيستوستيرون)",
                "100% Pure Calibration: Hercules Tier",
                "Master your biological progression to unlock Hercules Tier (100% Testosterone).",
                3, 900
            ),
            ApexFeat(
                "قمة جبريل النورانية: بلوغ 10,000 نقطة شعلة",
                "الارتقاء الحقيقي بالشعلة إلى 10,000 نقطة وتفعيل الهالة البيضاء",
                "Gabriel Luminous Summit: 10,000 Flame Pts",
                "Elevate your flame to 10,000 pts and awaken the pure white aura.",
                3, 960
            ),
            ApexFeat(
                "الصعود الأسمى: بلوغ 12,500 نقطة شعلة",
                "الاستمرار في الصمود والانضباط بعد تجاوز حاجز العشرة آلاف نقطة",
                "Supreme Ascent: 12,500 Flame Pts",
                "Continue steadfast discipline well past the 10,000 point threshold.",
                4, 1050
            ),
            ApexFeat(
                "حارس النور الأعلى: بلوغ 15,000 نقطة شعلة",
                "بلوغ 15,000 نقطة صافية عبر الالتزام المستدام طوال العام",
                "High Light Guardian: 15,000 Flame Pts",
                "Reach 15,000 pure points through sustained year-long commitment.",
                4, 1120
            ),
            ApexFeat(
                "أفق الكمال: بلوغ 17,500 نقطة شعلة",
                "الاقتراب من القمة المطلقة بتجميع 17,500 نقطة موثقة",
                "Horizon of Perfection: 17,500 Flame Pts",
                "Approach the absolute summit with 17,500 verified points.",
                4, 1190
            ),
            ApexFeat(
                "سيد الجمر التائب: بلوغ 20,000 نقطة شعلة",
                "تحقيق الحد الأقصى التاريخي (20,000 PTS) وفتح اللقب الحصري الأسطوري",
                "Lord of Penitent Embers: 20,000 Flame Pts",
                "Reach the 20,000 PTS maximum and unlock the legendary exclusive title.",
                4, 1260
            ),
            ApexFeat(
                "الخلود السيبراني الكامل: إتمام ذروة الانبعاث الأبدي",
                "الوصول إلى المحطة 500 وتفعيل جرعة الانبعاث الأبدي بإنجاز فعلي خالص",
                "Eternal Resurgence Pinnacle: Station 500",
                "Reach Station 500 and awaken the Eternal Resurgence Potion purely by merit.",
                4, 1320
            )
        )
        apexFeats.forEach { feat ->
            blueprints.add(
                TaskBlueprint(
                    title = feat.titleAr,
                    description = feat.descAr,
                    titleEn = feat.titleEn,
                    descriptionEn = feat.descEn,
                    targetCount = 1,
                    difficultyTier = feat.tier,
                    difficultyScore = feat.score,
                    actionTag = "GENERAL"
                )
            )
        }

        val sorted = blueprints
            .take(CATEGORY_TASKS_COUNT)
            .sortedWith(
                compareBy<TaskBlueprint> { it.difficultyTier }
                    .thenBy { it.difficultyScore }
                    .thenBy { it.targetCount }
                    .thenBy { it.title }
            )

        sorted.mapIndexed { i, bp ->
            val xp = EVERLASTING_XP_LIST[i]
            TaskItem(
                id = "everlasting_${i + 1}",
                period = TaskPeriod.YEARLY,
                title = bp.title,
                description = bp.description,
                xpBounty = xp,
                targetCount = bp.targetCount,
                requiredSkipKey = SkipKeyType.SILVER,
                actionTag = bp.actionTag,
                difficultyTier = bp.difficultyTier,
                titleEn = bp.titleEn,
                descriptionEn = bp.descriptionEn
            )
        }
    }

    val ALL_1000_TASKS: List<TaskItem> by lazy {
        ALL_DAILY_TASKS + ALL_WEEKLY_TASKS + ALL_MONTHLY_TASKS + ALL_EVERLASTING_TASKS
    }

    private val taskByIdMap: Map<String, TaskItem> by lazy {
        ALL_1000_TASKS.associateBy { it.id }
    }

    fun findById(id: String): TaskItem? = taskByIdMap[id]

    // ==========================================
    // DATE SEED & PROGRESSIVE ROTATION ENGINE
    // ==========================================

    fun todayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun currentWeekString(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return "${year}_W${String.format(Locale.US, "%02d", week)}"
    }

    fun currentMonthString(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return "${year}_M${String.format(Locale.US, "%02d", month)}"
    }

    fun sortTasksProgressively(tasks: List<TaskItem>): List<TaskItem> {
        return tasks.sortedWith(
            compareBy<TaskItem> { it.difficultyTier }
                .thenBy { it.xpBounty }
                .thenBy { it.targetCount }
                .thenBy { it.id }
        )
    }

    /**
     * Carries over all still-pending tasks from [existingPending] (preserving partial progress),
     * permanently excludes anything in [completedIds], and dynamically refills open slots up to
     * [batchSize] with fresh unused tasks from the pool, ordered progressively.
     */
    fun fillActiveBatch(
        period: TaskPeriod,
        existingPending: List<TaskItem>,
        completedIds: Set<String>,
        seedKey: String,
        batchSize: Int
    ): List<TaskItem> {
        val pool = when (period) {
            TaskPeriod.DAILY -> ALL_DAILY_TASKS
            TaskPeriod.WEEKLY -> ALL_WEEKLY_TASKS
            TaskPeriod.MONTHLY -> ALL_MONTHLY_TASKS
            TaskPeriod.YEARLY, TaskPeriod.EVERLASTING -> ALL_EVERLASTING_TASKS
        }
        val carriedOver = existingPending
            .filter { it.status == TaskStatus.PENDING && !it.isCompleted && it.id !in completedIds }
            .distinctBy { it.id }
            .map { existing ->
                val freshMeta = findById(existing.id)
                if (freshMeta != null) {
                    freshMeta.copy(
                        currentCount = existing.currentCount.coerceIn(0, freshMeta.targetCount - 1),
                        status = TaskStatus.PENDING
                    )
                } else {
                    existing
                }
            }
            .take(batchSize)

        val existingIds = carriedOver.map { it.id }.toSet()
        val needed = (batchSize - carriedOver.size).coerceAtLeast(0)
        val candidates = pool.filter { it.id !in completedIds && it.id !in existingIds }
        val newlyDrawn = if (needed > 0 && candidates.isNotEmpty()) {
            candidates.shuffled(Random(seedKey.hashCode().toLong())).take(needed)
        } else {
            emptyList()
        }
        return sortTasksProgressively(carriedOver + newlyDrawn)
    }

    fun getRotatedDailyTasks(completedIds: Set<String>, dateStr: String = todayDateString()): List<TaskItem> {
        return fillActiveBatch(TaskPeriod.DAILY, emptyList(), completedIds, dateStr, 5)
    }

    fun getRotatedWeeklyTasks(completedIds: Set<String>, weekStr: String = currentWeekString()): List<TaskItem> {
        return fillActiveBatch(TaskPeriod.WEEKLY, emptyList(), completedIds, weekStr, 7)
    }

    fun getRotatedMonthlyTasks(completedIds: Set<String>, monthStr: String = currentMonthString()): List<TaskItem> {
        return fillActiveBatch(TaskPeriod.MONTHLY, emptyList(), completedIds, monthStr, 10)
    }

    /**
     * Annual / Master Goals Tasks:
     * - Returns all 250 annual macro-achievements as a full batch from day one.
     * - Pending tasks stay at the top in progressive difficulty order; completed tasks archive to the bottom.
     */
    fun getEverlastingTasks(
        completedIds: Set<String>,
        existingSaved: List<TaskItem> = emptyList()
    ): List<TaskItem> {
        val savedById = existingSaved.associateBy { it.id }
        val merged = ALL_EVERLASTING_TASKS.map { task ->
            val saved = savedById[task.id]
            val done = (task.id in completedIds) || (saved != null && saved.isCompleted)
            if (done) {
                task.copy(status = TaskStatus.COMPLETED, currentCount = task.targetCount)
            } else {
                val savedProgress = (saved?.currentCount ?: 0).coerceIn(0, (task.targetCount - 1).coerceAtLeast(0))
                task.copy(status = TaskStatus.PENDING, currentCount = savedProgress)
            }
        }
        return merged.sortedWith(
            compareBy<TaskItem> { it.isCompleted }
                .thenBy { it.difficultyTier }
                .thenBy { it.xpBounty }
                .thenBy { it.targetCount }
                .thenBy { it.id }
        )
    }
}
