package com.example.data.roadmap

import com.example.data.profile.SkipKeyType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random

/**
 * 1000-Task Matrix & Exponential Roadmap XP Engine for StrikePulse (Update III Overhaul):
 * - Exactly 1000 Tasks across 4 Categories (250 Daily, 250 Weekly, 250 Monthly, 250 Annual/Everlasting)
 * - Total XP Pool across all 1000 tasks: Exactly 434,725 XP
 * - 500 Stations Exponential Curve: Station 1 = 50 XP, Station 500 = 434,725 XP
 * - Realistic Timeframe Scaling:
 *   * Daily: Light, practical 24h micro-discipline & single-action triggers (no impossible multi-day shield or key spam)
 *   * Weekly: 7-day consistency, streaks, strategic potions & moderate key allocation
 *   * Monthly: Medium-term systemic progression & structured milestones
 *   * Annual: High-tier macro-achievements (skip-keys strictly prohibited, genuine completion required)
 * - Progressive Ordering: Catalogs and active rotated slices are strictly ordered from easiest to hardest,
 *   with XP rewards scaling monotonically with true cognitive/procedural effort.
 */
object TaskPools {

    const val TOTAL_TASKS_COUNT = 1000
    const val CATEGORY_TASKS_COUNT = 250
    const val TOTAL_XP_POOL = 434725L

    private data class TaskBlueprint(
        val title: String,
        val description: String,
        val targetCount: Int,
        val difficultyTier: Int,  // 1 = Easy, 2 = Medium, 3 = Challenging, 4 = Elite
        val difficultyScore: Int, // Fine-grained effort score for monotonic ordering & XP mapping
        val actionTag: String
    )

    // ==========================================
    // 5-TIER REWARD MATRIX (PURGED OF HAMMERS)
    // ==========================================

    val HIERARCHICAL_REWARDS = listOf(
        // Tier 1: Common Protective Shields
        StationReward(RewardType.SHIELD_24H, "درع حماية 24 ساعة (تفعيل فوري)", "حماية فورية وتلقائية للشعلة من غرامة الـ 3 نقاط اليومية لمدة 24 ساعة", "shield_24h", 1),
        // Tier 2: Task-Skip Keys
        StationReward(RewardType.KEY_WOODEN, "مفتاح تجاوز خشبي (Wooden Key)", "استخدام فردي واحد لتجاوز المهام اليومية مع حصد الـ XP فورياً", "key_wooden", 1),
        // Tier 3: Cybernetic Potions
        StationReward(RewardType.POTION_PULSE_ELIXIR, "إكسير النبض (Pulse Elixir)", "+5 نقاط إضافية لزر التبريد Deflame للـ 3 تفعيلات القادمة", "potion_pulse", 1),
        // Tier 1: 48h Shield
        StationReward(RewardType.SHIELD_48H, "درع حماية 48 ساعة (تفعيل فوري)", "حماية فورية وتلقائية للشعلة من الخمول لمدة يومين كاملين", "shield_48h", 1),
        // Tier 2: Bronze Key (2 uses)
        StationReward(RewardType.KEY_BRONZE, "مفتاح تجاوز برونزي (Bronze Key)", "مفتاح استثنائي بـ استخدامين لتجاوز المهام وحصد الـ XP فورياً", "key_bronze", 1),
        // Tier 3: Shadow-Grip Serum
        StationReward(RewardType.POTION_SHADOW_SERUM, "مصل قبضة الظل (Shadow-Grip Serum)", "يقلل وتيرة صعود نقاط الشعلة بنسبة 50% لمدة 3 أيام متتالية", "potion_shadow", 1),
        // Tier 1: 72h Shield
        StationReward(RewardType.SHIELD_72H, "درع حماية 72 ساعة (تفعيل فوري)", "حماية متينة للشعلة لمدة 3 أيام متتالية من أي عقوبة خمول", "shield_72h", 1),
        // Tier 2: Silver Key (3 uses)
        StationReward(RewardType.KEY_SILVER, "مفتاح تجاوز فضي (Silver Key)", "مفتاح متقدم بـ 3 استخدامات لتجاوز المهام وحصد الـ XP فورياً", "key_silver", 1),
        // Tier 3: Gabriel's Purity Droplet
        StationReward(RewardType.POTION_GABRIEL_DROPLET, "قطرة نقاء جبريل (Gabriel's Droplet)", "تسارع نمو نقاط الشعلة بنسبة 50% لمدة 7 أيام (تفعيل تلقائي وفوري)", "potion_gabriel", 1),
        // Tier 3: Absolute Cryo-Infusion
        StationReward(RewardType.POTION_CRYO_INFUSION, "الحقن التجميدي المطلق (Cryo-Infusion)", "+100 نقطة بونص لتعزيز الستريك التالي (تفعيل اختياري)", "potion_cryo", 1)
    )

    fun getRewardForStation(stationId: Int): StationReward {
        return when {
            // Tier 5: Station 500 Ultimate Legendary Reward
            stationId == 500 -> StationReward(
                type = RewardType.POTION_ETERNAL_REBIRTH,
                title = "جرعة الانبعاث الأبدي (Eternal Rebirth) + لقب سيد النقوشة",
                description = "الجائزة الكبرى للمحطة 500: تمنح درع مناعة أبدي ضد التصفير ولقب سيد النقوشة (Gary Master)",
                iconName = "potion_rebirth",
                quantity = 1
            )
            // Extra Elite Reward every 20 levels (20, 40, 60... 480) paired with titles/avatars
            stationId % 20 == 0 -> StationReward(
                type = RewardType.KEY_SILVER,
                title = "مكافأة نخبوية محطة #$stationId (Silver Apex)",
                description = "جائزة المستوى العشرين: مفتاح فضي مزدوج لتجاوز المهام وفتح ألقاب وأفاتار حصرية في الملف الشخصي",
                iconName = "key_silver",
                quantity = 2
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
        "النبضة الصباحية", "تهدئة الظهيرة", "موازنة المساء", "بروتوكول الأمان", "تثبيت النواة",
        "تبريد فوري", "حاجز الانضباط", "ضبط التردد", "حماية الشعلة", "تصفية الشحنات",
        "إعادة المعايرة", "تأمين السلسلة", "التحكم الدقيق", "مراقبة الاستقرار", "ختم اليوم"
    )

    private val dailySingleShieldContexts = listOf(
        "الحفاظ على الدرع النشط اليوم",
        "تأمين الشعلة بدرع حماية لـ 24 ساعة",
        "تفعيل حاجز الوقاية ضد غرامة الخمول",
        "صيانة الدرع الواقي للنظام اليوم",
        "إبقاء الشعلة تحت الحماية الدرعية",
        "تغطية دورة اليوم بدرع حماية واحد",
        "درع الثبات اليومي ضد ارتفاع الخمول",
        "تحصين السلسلة بدرع واحد فعال",
        "تثبيت دفاعات الشعلة لليوم الكامل",
        "درع الوقاية الأول لصد نقاط الخمول",
        "تفعيل خط الدفاع الدرعي اليوم",
        "تأمين النقاط عبر درع حماية نشط",
        "درع الاستقرار اليومي للشعلة",
        "مراقبة الدرع الفعال وتجنب التراكم",
        "حماية الـ 24 ساعة بدرع واحد مفعّل",
        "تفعيل الدرع الوقائي في الفترة الصباحية",
        "تفعيل الدرع الوقائي في الفترة المسائية",
        "تأمين الرصيد ضد غرامة +3 نقاط اليومية",
        "التحقق من استمرار صلاحية الدرع اليوم",
        "تغطية اليوم بدرع السلسلة الوقائي"
    )

    val ALL_DAILY_TASKS: List<TaskItem> by lazy {
        val blueprints = mutableListOf<TaskBlueprint>()

        // 1. Deflame Micro-Discipline (60 tasks: targets 1..8)
        val deflameDistribution = listOf(
            1 to 15, 2 to 12, 3 to 10, 4 to 8, 5 to 6, 6 to 4, 7 to 3, 8 to 2
        )
        deflameDistribution.forEach { (target, count) ->
            for (k in 0 until count) {
                val ctx = dailyContexts[(k + target) % dailyContexts.size]
                val tier = when {
                    target <= 2 -> 1
                    target <= 4 -> 2
                    else -> 3
                }
                val countLabel = if (target == 1) "مرة واحدة" else "$target مرات"
                blueprints.add(
                    TaskBlueprint(
                        title = "تبريد Deflame ($countLabel) — $ctx",
                        description = "اضغط على زر التخفيض Deflame $countLabel خلال اليوم للحفاظ على هدوء الشعلة ($ctx)",
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
                val ctx = dailyContexts[(k + target + 5) % dailyContexts.size]
                val tier = when {
                    target <= 2 -> 1
                    target <= 4 -> 2
                    else -> 3
                }
                val countLabel = if (target == 1) "مرة واحدة" else "$target مرات"
                blueprints.add(
                    TaskBlueprint(
                        title = "تبريد Girl ($countLabel) — $ctx",
                        description = "نفّذ تبريد النظام بالضغط على زر Girl $countLabel اليوم ضمن مسار ($ctx)",
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
                val ctx = dailyContexts[(k * 2 + target) % dailyContexts.size]
                val tier = when {
                    target <= 2 -> 1
                    target <= 4 -> 2
                    else -> 3
                }
                val label = if (target == 1) "دورة واحدة" else "$target دورات"
                blueprints.add(
                    TaskBlueprint(
                        title = "توازن حراري ($label) — $ctx",
                        description = "نفّذ $label من التبريد والتخفيض (Deflame أو Girl) اليوم لضبط الإيقاع ($ctx)",
                        targetCount = target,
                        difficultyTier = tier,
                        difficultyScore = 18 + target * 16 + k,
                        actionTag = "BALANCE"
                    )
                )
            }
        }

        // 4. Realistic Daily Strikes (35 tasks: 1..3 strikes max per day — strictly avoiding 6+ Overload!)
        val strikeContexts = listOf(
            "الضربة النظيفة", "تركيز الهدف", "تنفيذ دقيق", "اختراق يومي", "نبضة الحسم",
            "انضباط المسار", "تسجيل موثق", "توقيت مثالي", "دقة الإصابة", "تثبيت السجل",
            "ضربة الاستهلال", "ضربة الختام", "مسار الصمود", "ضربة الكفاءة", "إنجاز فوري",
            "تحدي اليوم", "إشارة التأكيد", "مناورة سريعة"
        )
        for (k in 0 until 18) {
            blueprints.add(
                TaskBlueprint(
                    title = "تسجيل سترايك واحد اليوم — ${strikeContexts[k]}",
                    description = "نفّذ ضربة واحدة موثقة بالتاريخ والوقت دون تعريض الشعلة للضغط المفرط",
                    targetCount = 1,
                    difficultyTier = 1,
                    difficultyScore = 22 + k,
                    actionTag = "STRIKE"
                )
            )
        }
        for (k in 0 until 11) {
            blueprints.add(
                TaskBlueprint(
                    title = "تنفيذ سترايكين (2) منضبطين اليوم — ${strikeContexts[k]}",
                    description = "سجّل ضربتين خلال 24 ساعة مع جاهزية الدرع الوقائي للحفاظ على الاستقرار",
                    targetCount = 2,
                    difficultyTier = 2,
                    difficultyScore = 68 + k * 2,
                    actionTag = "STRIKE"
                )
            )
        }
        for (k in 0 until 6) {
            blueprints.add(
                TaskBlueprint(
                    title = "ثلاثية يومية (3 سترايك) لتفعيل الدرع — ${strikeContexts[k]}",
                    description = "سجّل 3 سترايكات اليوم لتفعيل درع الـ 7 أيام التلقائي دون تجاوز حد الحمل الأقصى",
                    targetCount = 3,
                    difficultyTier = 3,
                    difficultyScore = 115 + k * 3,
                    actionTag = "STRIKE"
                )
            )
        }

        // 5. Daily Protective Shield Maintenance (20 tasks: strictly 1 action)
        dailySingleShieldContexts.forEachIndexed { idx, label ->
            blueprints.add(
                TaskBlueprint(
                    title = label,
                    description = "فعّل درع حماية واحداً أو حافظ على بقاء درعك نشطاً لحماية الشعلة من غرامة الـ +3 نقاط اليوم",
                    targetCount = 1,
                    difficultyTier = 1,
                    difficultyScore = 16 + idx * 2,
                    actionTag = "SHIELD"
                )
            )
        }

        // 6. Daily Profile Logging (14 tasks: 1..2 individuals)
        for (k in 0 until 10) {
            val ctx = dailyContexts[k]
            blueprints.add(
                TaskBlueprint(
                    title = "إضافة فرد واحد للسجلات اليوم — $ctx",
                    description = "أضف ملفاً جديداً واحداً إلى السجلات مع احتساب الحجم والتعزيز تلقائياً",
                    targetCount = 1,
                    difficultyTier = 1,
                    difficultyScore = 30 + k * 2,
                    actionTag = "PERSON"
                )
            )
        }
        for (k in 0 until 4) {
            val ctx = dailyContexts[k + 10]
            blueprints.add(
                TaskBlueprint(
                    title = "توثيق فردين (2) في السجلات اليوم — $ctx",
                    description = "سجّل شخصين جديدين في قاعدة البيانات خلال اليوم ($ctx)",
                    targetCount = 2,
                    difficultyTier = 2,
                    difficultyScore = 76 + k * 3,
                    actionTag = "PERSON"
                )
            )
        }

        // 7. Single Tactical Skip-Key Use (8 tasks: strictly 1 key max, no spamming!)
        val singleKeyLabels = listOf(
            "استخدام مفتاح تجاوز واحد بتكتيك — فك العقدة",
            "تخطي مهمة متوقفة بمفتاح واحد — تسريع المسار",
            "استثمار مفتاح تجاوز فردي اليوم — كفاءة XP",
            "تفعيل مفتاح واحد عند الحاجة — إدارة الموارد",
            "استعمال مفتاح تجاوز منفرد — توفير الجهد",
            "تحرير مهمة عالقة بمفتاح واحد — مرونة يومية",
            "صرف مفتاح واحد بحكمة اليوم — خطة التقدم",
            "استخدام مفتاح تجاوز يتيم اليوم — دعم المحطة"
        )
        singleKeyLabels.forEachIndexed { idx, label ->
            blueprints.add(
                TaskBlueprint(
                    title = label,
                    description = "استعمل مفتاح تجاوز واحداً فقط عند الحاجة لتحصيل نقاط XP دون هدر المخزون",
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
                    title = "جني مكافأة محطة متاحة اليوم — محطة #${k + 1}",
                    description = "افتح خريطة المسار واجنِ مكافأة واحدة من المحطات التي وصلت لمتطلبها",
                    targetCount = 1,
                    difficultyTier = 2,
                    difficultyScore = 60 + k * 3,
                    actionTag = "ROADMAP"
                )
            )
        }

        // Sort strictly from easiest to hardest before pairing ascending XP list
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
                difficultyTier = bp.difficultyTier
            )
        }
    }

    // ==========================================
    // 2. WEEKLY TASKS POOL (250 7-DAY CONSISTENCY TASKS)
    // ==========================================

    private val weeklyContexts = listOf(
        "اتساق الأسبوع", "موجة الثبات", "إيقاع الـ 7 أيام", "بروتوكول الاستمرارية", "دورة الصمود",
        "انضباط الوسط", "توازن المدى", "مناورة التراكم", "انضباط الجولات", "درع الاستقرار",
        "خط التبريد", "حزمة الإتقان", "خطة التحصين", "استراتيجية النبض", "الذروة الأسبوعية"
    )

    val ALL_WEEKLY_TASKS: List<TaskItem> by lazy {
        val blueprints = mutableListOf<TaskBlueprint>()

        // 1. Weekly Deflame Consistency (55 tasks: 4..21 across 7 days)
        for (k in 0 until 55) {
            val target = 4 + (k * 18 / 55) // smooth 4..21
            val ctx = weeklyContexts[k % weeklyContexts.size]
            val sub = (k / weeklyContexts.size) + 1
            val tier = when {
                target <= 8 -> 1
                target <= 14 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "تبريد Deflame ($target مرة أسبوعياً) — $ctx ($sub)",
                    description = "وزّع $target عمليات تخفيض عبر زر Deflame على مدار أيام الأسبوع ($ctx)",
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
            val ctx = weeklyContexts[(k + 4) % weeklyContexts.size]
            val sub = (k / weeklyContexts.size) + 1
            val tier = when {
                target <= 8 -> 1
                target <= 14 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "تبريد Girl ($target مرة أسبوعياً) — $ctx ($sub)",
                    description = "حافظ على هدوء النظام بواقع $target ضغطات تبريد موزعة خلال 7 أيام ($ctx)",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 17 + target * 9 + (k % 3),
                    actionTag = "GIRL"
                )
            )
        }

        // 3. Weekly Alternating Balance (40 tasks: 4..18 across 7 days)
        for (k in 0 until 40) {
            val target = 4 + (k * 15 / 40) // 4..18
            val ctx = weeklyContexts[(k + 7) % weeklyContexts.size]
            val sub = (k / weeklyContexts.size) + 1
            val tier = when {
                target <= 7 -> 1
                target <= 12 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "انضباط حراري أسبوعي ($target خطوة) — $ctx ($sub)",
                    description = "نفّذ $target جولة تبريد وتخفيض متزنة خلال الأسبوع للحفاظ على مسار الشعلة",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 24 + target * 10 + (k % 2),
                    actionTag = "BALANCE"
                )
            )
        }

        // 4. Weekly Balanced Strikes (40 tasks: 2..12 strikes across 7 days)
        for (k in 0 until 40) {
            val target = 2 + (k * 11 / 40) // 2..12
            val ctx = weeklyContexts[(k + 2) % weeklyContexts.size]
            val sub = (k / weeklyContexts.size) + 1
            val tier = when {
                target <= 4 -> 1
                target <= 8 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "تنفيذ $target سترايك خلال الأسبوع — $ctx ($sub)",
                    description = "سجّل $target ضربات موزعة بذكاء على أيام الأسبوع السبعة دون إفراط يومي",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 26 + target * 15 + (k % 2),
                    actionTag = "STRIKE"
                )
            )
        }

        // 5. Weekly Streak & Shield Maintenance (22 tasks: 1..3 activations/retentions)
        for (k in 0 until 22) {
            val target = 1 + (k * 3 / 22) // 1..3
            val ctx = weeklyContexts[k % weeklyContexts.size]
            val tier = if (target == 1) 1 else if (target == 2) 2 else 3
            blueprints.add(
                TaskBlueprint(
                    title = "حماية السلسلة بالدرع ($target مرات أسبوعياً) — $ctx",
                    description = "أمّن أسبوعك عبر تفعيل الدروع الوقائية ($target مرات) ومنع تراكم نقاط الخمول",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 30 + target * 40 + k,
                    actionTag = "SHIELD"
                )
            )
        }

        // 6. Strategic Weekly Potions Usage (16 tasks: 1..3 potions)
        val potionNames = listOf("إكسير النبض", "مصل قبضة الظل", "قطرة جبريل", "الحقن التجميدي")
        for (k in 0 until 16) {
            val target = 1 + (k * 3 / 16) // 1..3
            val pName = potionNames[k % potionNames.size]
            val cycle = (k / potionNames.size) + 1
            val tier = if (target == 1) 2 else 3
            blueprints.add(
                TaskBlueprint(
                    title = "توظيف الجرعات الاستراتيجي (${target}x — $pName #$cycle)",
                    description = "فعّل $target من الجرعات السيبرانية في الأوقات الحرجة لتعظيم فائدة الأسبوع",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 70 + target * 35 + k,
                    actionTag = "POTION"
                )
            )
        }

        // 7. Moderate Weekly Key Allocation (12 tasks: 1..3 keys)
        for (k in 0 until 12) {
            val target = 1 + (k * 3 / 12) // 1..3
            val ctx = weeklyContexts[(k + 9) % weeklyContexts.size]
            val tier = if (target <= 2) 2 else 3
            blueprints.add(
                TaskBlueprint(
                    title = "تخصيص معتدل للمفاتيح ($target مفتاح) — $ctx",
                    description = "وزّع استخدام $target مفاتيح تجاوز خلال الأسبوع لتجاوز المهام الصعبة بحكمة",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 60 + target * 32 + k,
                    actionTag = "KEY"
                )
            )
        }

        // 8. Weekly Records & Roadmap Stations (10 tasks: 5 PERSON + 5 ROADMAP)
        for (k in 0 until 5) {
            val target = 2 + k // 2..6
            blueprints.add(
                TaskBlueprint(
                    title = "توسيع السجلات الأسبوعي ($target أفراد)",
                    description = "سجّل $target ملفات جديدة في السجلات خلال دورة هذا الأسبوع",
                    targetCount = target,
                    difficultyTier = if (target <= 3) 1 else 2,
                    difficultyScore = 40 + target * 18,
                    actionTag = "PERSON"
                )
            )
        }
        for (k in 0 until 5) {
            val target = 1 + k // 1..5 stations
            blueprints.add(
                TaskBlueprint(
                    title = "عبور $target محطات على المسار هذا الأسبوع",
                    description = "افتح واجنِ مكافآت $target محطات متتالية في خريطة الـ 500 محطة",
                    targetCount = target,
                    difficultyTier = if (target <= 2) 2 else 3,
                    difficultyScore = 65 + target * 22,
                    actionTag = "ROADMAP"
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
                difficultyTier = bp.difficultyTier
            )
        }
    }

    // ==========================================
    // 3. MONTHLY TASKS POOL (250 SYSTEMIC MILESTONES)
    // ==========================================

    private val monthlyContexts = listOf(
        "المرحلة التأسيسية", "البناء المنهجي", "التكامل الشهري", "عمق النظام", "الاستقرار الممتد",
        "إدارة الدورة", "مسار الصمود", "هندسة الطاقة", "تحصين الشهر", "ذروة التقدم"
    )

    val ALL_MONTHLY_TASKS: List<TaskItem> by lazy {
        val blueprints = mutableListOf<TaskBlueprint>()

        // 1. Monthly Deflame Progression (55 tasks: 15..85)
        for (k in 0 until 55) {
            val target = 15 + (k * 71 / 55) // 15..85
            val ctx = monthlyContexts[k % monthlyContexts.size]
            val rank = (k / monthlyContexts.size) + 1
            val tier = when {
                target <= 32 -> 1
                target <= 58 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "إنجاز $target عملية Deflame شهرية — $ctx ($rank)",
                    description = "حافظ على التوازن التراكمي للشعلة بإتمام $target تخفيضاً خلال هذا الشهر",
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
            val ctx = monthlyContexts[(k + 3) % monthlyContexts.size]
            val rank = (k / monthlyContexts.size) + 1
            val tier = when {
                target <= 32 -> 1
                target <= 58 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "إنجاز $target تبريد Girl شهري — $ctx ($rank)",
                    description = "نفّذ $target جولة تبريد منتظمة عبر زر Girl على مدار الشهر الكامل",
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
            val ctx = monthlyContexts[(k + 5) % monthlyContexts.size]
            val rank = (k / monthlyContexts.size) + 1
            val tier = when {
                target <= 30 -> 1
                target <= 52 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "موازنة نظام شهرية ($target دورة) — $ctx ($rank)",
                    description = "ارفع كفاءة التحكم الحراري عبر $target عملية موازنة بين Deflame وGirl في الشهر",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 25 + target * 4,
                    actionTag = "BALANCE"
                )
            )
        }

        // 4. Monthly Strike Milestones (40 tasks: 8..40)
        for (k in 0 until 40) {
            val target = 8 + (k * 33 / 40) // 8..40
            val ctx = monthlyContexts[(k + 2) % monthlyContexts.size]
            val rank = (k / monthlyContexts.size) + 1
            val tier = when {
                target <= 15 -> 1
                target <= 27 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "تراكم $target سترايك خلال الشهر — $ctx ($rank)",
                    description = "سجّل $target ضربات موثقة موزعة بانتظام عبر أسابيع الشهر الأربعة",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 30 + target * 6,
                    actionTag = "STRIKE"
                )
            )
        }

        // 5. Monthly Shield Defense Milestones (20 tasks: 2..8 shields)
        for (k in 0 until 20) {
            val target = 2 + (k * 7 / 20) // 2..8
            val ctx = monthlyContexts[k % monthlyContexts.size]
            val tier = when {
                target <= 3 -> 1
                target <= 5 -> 2
                else -> 3
            }
            blueprints.add(
                TaskBlueprint(
                    title = "تحصين شهري شامل ($target دروع) — $ctx #${k + 1}",
                    description = "فعّل $target دروع حماية خلال الشهر لضمان بقاء السلسلة محمية من الخمول",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 35 + target * 28 + k,
                    actionTag = "SHIELD"
                )
            )
        }

        // 6. Monthly Potion Deployment (15 tasks: 2..6 potions)
        for (k in 0 until 15) {
            val target = 2 + (k * 5 / 15) // 2..6
            val ctx = monthlyContexts[k % monthlyContexts.size]
            val tier = if (target <= 3) 2 else 3
            blueprints.add(
                TaskBlueprint(
                    title = "تكتيك الجرعات الشهري ($target جرعات) — $ctx",
                    description = "استهلك $target جرعات سيبرانية في المفاصل الزمنية للشهر لتحقيق أقصى عائد",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 110 + target * 25 + k,
                    actionTag = "POTION"
                )
            )
        }

        // 7. Monthly Key Economy (10 tasks: 2..6 keys)
        for (k in 0 until 10) {
            val target = 2 + (k * 5 / 10) // 2..6
            val ctx = monthlyContexts[k % monthlyContexts.size]
            val tier = if (target <= 3) 2 else 3
            blueprints.add(
                TaskBlueprint(
                    title = "إدارة مفاتيح التجاوز الشهرية ($target مفاتيح) — $ctx",
                    description = "وظّف $target مفاتيح تجاوز خلال الشهر لفتح اختناقات التقدم وحصد الـ XP",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 100 + target * 24 + k,
                    actionTag = "KEY"
                )
            )
        }

        // 8. Monthly Database Growth & Roadmap Progression (15 tasks: 8 PERSON + 7 ROADMAP)
        for (k in 0 until 8) {
            val target = 4 + (k * 12 / 8) // 4..15
            blueprints.add(
                TaskBlueprint(
                    title = "توسعة السجلات الشهرية ($target ملفات جديدة)",
                    description = "أضف $target أفراد جدد إلى شبكة السجلات والتحليل خلال الشهر الحالي",
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
            val target = 3 + k * 3 // 3, 6, 9, 12, 15, 18, 21
            blueprints.add(
                TaskBlueprint(
                    title = "الارتقاء في المسار ($target محطات شهرية)",
                    description = "افتح واستلم مكافآت $target محطات جديدة على المسار الموجي هذا الشهر",
                    targetCount = target,
                    difficultyTier = if (target <= 9) 2 else 3,
                    difficultyScore = 90 + target * 10,
                    actionTag = "ROADMAP"
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
                difficultyTier = bp.difficultyTier
            )
        }
    }

    // ==========================================
    // 4. ANNUAL TASKS POOL (250 MACRO-ACHIEVEMENTS, NO SKIPS ALLOWED)
    // ==========================================

    private val annualTiers = listOf(
        "الأساس السنوي", "الالتزام المستدام", "السيطرة الاستراتيجية", "المجد السيبراني", "الذروة الأبدية"
    )

    val ALL_EVERLASTING_TASKS: List<TaskItem> by lazy {
        val blueprints = mutableListOf<TaskBlueprint>()

        // 1. Annual Macro Strikes Ladder (55 tasks: 10 -> 500 verified cumulative strikes)
        for (k in 0 until 55) {
            val target = 10 + (k * 490 / 54) // 10..500 progressive
            val phase = annualTiers[(k * annualTiers.size / 55).coerceAtMost(4)]
            val tier = when {
                target <= 60 -> 1
                target <= 175 -> 2
                target <= 325 -> 3
                else -> 4
            }
            blueprints.add(
                TaskBlueprint(
                    title = "إنجاز سنوي: $target سترايك موثق ($phase)",
                    description = "تحقيق حاجز $target سترايك فعلي وحقيقي على مدار السنة دون أي مفتاح تجاوز",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 50 + target * 2,
                    actionTag = "STRIKE"
                )
            )
        }

        // 2. Annual Macro Deflame Discipline (45 tasks: 50 -> 1000 cumulative actions)
        for (k in 0 until 45) {
            val target = 50 + (k * 950 / 44) // 50..1000
            val phase = annualTiers[(k * annualTiers.size / 45).coerceAtMost(4)]
            val tier = when {
                target <= 180 -> 1
                target <= 450 -> 2
                target <= 750 -> 3
                else -> 4
            }
            blueprints.add(
                TaskBlueprint(
                    title = "سيادة التبريد السنوية: $target ضغطة Deflame ($phase)",
                    description = "إتمام $target عملية تخفيض حراري فعلية عبر زر Deflame خلال رحلة العام",
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
            val phase = annualTiers[(k * annualTiers.size / 45).coerceAtMost(4)]
            val tier = when {
                target <= 180 -> 1
                target <= 450 -> 2
                target <= 750 -> 3
                else -> 4
            }
            blueprints.add(
                TaskBlueprint(
                    title = "انضباط Girl السنوي: $target عملية تبريد ($phase)",
                    description = "تسجيل $target تفعيل تبريد فعلي لزر Girl على مدار المسار السنوي",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 47 + target,
                    actionTag = "GIRL"
                )
            )
        }

        // 4. Annual Thermal Balance (30 tasks: 50 -> 800 cumulative actions)
        for (k in 0 until 30) {
            val target = 50 + (k * 750 / 29) // 50..800
            val phase = annualTiers[(k * annualTiers.size / 30).coerceAtMost(4)]
            val tier = when {
                target <= 150 -> 1
                target <= 360 -> 2
                target <= 600 -> 3
                else -> 4
            }
            blueprints.add(
                TaskBlueprint(
                    title = "الاتزان الحراري السنوي: $target دورة ($phase)",
                    description = "الجمع الموثق بين Deflame وGirl بواقع $target عملية متوازنة سنوياً",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 60 + target + (target / 4),
                    actionTag = "BALANCE"
                )
            )
        }

        // 5. Annual Archive Expansion (25 tasks: 5 -> 120 registered profiles)
        for (k in 0 until 25) {
            val target = 5 + (k * 115 / 24) // 5..120
            val phase = annualTiers[(k * annualTiers.size / 25).coerceAtMost(4)]
            val tier = when {
                target <= 20 -> 1
                target <= 55 -> 2
                target <= 90 -> 3
                else -> 4
            }
            blueprints.add(
                TaskBlueprint(
                    title = "شبكة السجلات السنوية: $target ملفاً مسجلاً ($phase)",
                    description = "بناء وتوثيق $target ملفات مكتملة البيانات داخل قاعدة بيانات التطبيق",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 80 + target * 8,
                    actionTag = "PERSON"
                )
            )
        }

        // 6. Annual Shield & Potion Mastery (20 tasks: 10 SHIELD + 10 POTION)
        for (k in 0 until 10) {
            val target = 5 + k * 3 // 5..32 shields
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
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 120 + target * 28,
                    actionTag = "SHIELD"
                )
            )
        }
        for (k in 0 until 10) {
            val target = 3 + k * 2 // 3..21 potions
            val tier = when {
                target <= 7 -> 2
                target <= 15 -> 3
                else -> 4
            }
            blueprints.add(
                TaskBlueprint(
                    title = "خيمياء السيبرانية السنوية: $target جرعات مفعّلة",
                    description = "استخدام $target جرعات تكتيكية حقيقية من مخزونك لدعم الأداء السنوي",
                    targetCount = target,
                    difficultyTier = tier,
                    difficultyScore = 220 + target * 40,
                    actionTag = "POTION"
                )
            )
        }

        // 7. Annual Roadmap Conquest Milestones (15 tasks: Stations 10 -> 500)
        val stationMilestones = listOf(10, 25, 50, 75, 100, 140, 180, 220, 260, 300, 350, 400, 440, 480, 500)
        stationMilestones.forEachIndexed { idx, stationTarget ->
            val tier = when {
                stationTarget <= 75 -> 1
                stationTarget <= 220 -> 2
                stationTarget <= 400 -> 3
                else -> 4
            }
            blueprints.add(
                TaskBlueprint(
                    title = "غزو المسار الموجي: جني مكافآت $stationTarget محطة",
                    description = "الوصول الفعلي للمحطة #$stationTarget واستلام مكافآت خريطة الـ 500 محطة (دون أي مفاتيح تجاوز)",
                    targetCount = stationTarget,
                    difficultyTier = tier,
                    difficultyScore = 100 + stationTarget * 2,
                    actionTag = "ROADMAP"
                )
            )
        }

        // 8. Annual Apex Flame & Bio-Calibration Feats (15 tasks: Genuine high-tier mastery)
        val apexFeats = listOf(
            Triple("الاستقرار النوراني الأول: بلوغ 1,500 نقطة شعلة", "رفع رصيد الشعلة الفعلي إلى 1,500 نقطة والحفاظ على نقاء المسار", 1 to 180),
            Triple("عتبة الشعلة النورانية: بلوغ 2,000 نقطة صافية", "تجاوز حاجز 2,000 نقطة والدخول إلى مسار النور والثبات", 1 to 240),
            Triple("معايرة حيوية متقدمة: بلوغ 60% تيستوستيرون", "ضبط المؤشر الحيوي عبر الالتزام الحقيقي للوصول إلى 60% تيستوستيرون", 2 to 360),
            Triple("الارتقاء النوراني: بلوغ 3,500 نقطة شعلة", "تعزيز الانضباط اليومي حتى الوصول الفعلي لـ 3,500 نقطة", 2 to 440),
            Triple("معايرة حيوية عالية: بلوغ 75% تيستوستيرون", "الحفاظ على الانضباط المتراكم لرفع المؤشر إلى 75% تيستوستيرون", 2 to 540),
            Triple("صمود منتصف الطريق: بلوغ 5,000 نقطة شعلة", "تحقيق نصف المسافة نحو ذروة جبريل (5,000 PTS) بجهد فعلي متواصل", 2 to 620),
            Triple("السيادة الحيوية: بلوغ 90% تيستوستيرون", "الوصول إلى مستوى 90% تيستوستيرون في سجل التطورات الجينية", 3 to 740),
            Triple("إشراق النخبة: بلوغ 7,500 نقطة شعلة", "بلوغ 7,500 نقطة في عداد الشعلة دون تصفير أو حمل زائد", 3 to 820),
            Triple("نقاء الـ DNA الكامل: بلوغ 100% تيستوستيرون", "إتقان المؤشر الجيني للوصول إلى رتبة هرقل (100% تيستوستيرون)", 3 to 900),
            Triple("قمة جبريل النورانية: بلوغ 10,000 نقطة شعلة", "الارتقاء الحقيقي بالشعلة إلى 10,000 نقطة وتفعيل لمسة جبريل البيضاء", 3 to 960),
            Triple("الصعود الأسمى: بلوغ 12,500 نقطة شعلة", "الاستمرار في الصمود والانضباط بعد تجاوز حاجز العشرة آلاف نقطة", 4 to 1050),
            Triple("حارس النور الأعلى: بلوغ 15,000 نقطة شعلة", "بلوغ 15,000 نقطة صافية عبر الالتزام المستدام طوال العام", 4 to 1120),
            Triple("أفق الكمال: بلوغ 17,500 نقطة شعلة", "الاقتراب من القمة المطلقة بتجميع 17,500 نقطة موثقة", 4 to 1190),
            Triple("سيد الجمر التائب: بلوغ 20,000 نقطة شعلة", "تحقيق الحد الأقصى التاريخي (20,000 PTS) وفتح اللقب الحصري الأسطوري", 4 to 1260),
            Triple("الخلود السيبراني الكامل: إتمام ذروة الانبعاث الأبدي", "الوصول إلى المحطة 500 وتفعيل جرعة الانبعاث الأبدي بإنجاز فعلي خالص", 4 to 1320)
        )
        apexFeats.forEach { (title, desc, tierAndScore) ->
            blueprints.add(
                TaskBlueprint(
                    title = title,
                    description = desc,
                    targetCount = 1,
                    difficultyTier = tierAndScore.first,
                    difficultyScore = tierAndScore.second,
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
                difficultyTier = bp.difficultyTier
            )
        }
    }

    val ALL_1000_TASKS: List<TaskItem> by lazy {
        ALL_DAILY_TASKS + ALL_WEEKLY_TASKS + ALL_MONTHLY_TASKS + ALL_EVERLASTING_TASKS
    }

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

    private fun sortTasksProgressively(tasks: List<TaskItem>): List<TaskItem> {
        return tasks.sortedWith(
            compareBy<TaskItem> { it.difficultyTier }
                .thenBy { it.xpBounty }
                .thenBy { it.targetCount }
                .thenBy { it.id }
        )
    }

    /**
     * Non-repeating consumption rotation for Daily Tasks:
     * - Filters out all tasks in [completedIds].
     * - Shuffles remaining using date seed (00:00 reset) & picks 5 active tasks.
     * - Returns the 5 tasks strictly ordered from easiest to hardest.
     */
    fun getRotatedDailyTasks(completedIds: Set<String>, dateStr: String = todayDateString()): List<TaskItem> {
        val uncompleted = ALL_DAILY_TASKS.filter { it.id !in completedIds }
        if (uncompleted.isEmpty()) {
            return sortTasksProgressively(
                ALL_DAILY_TASKS.takeLast(5).map { it.copy(status = TaskStatus.COMPLETED, currentCount = it.targetCount) }
            )
        }
        val seed = dateStr.hashCode().toLong()
        val selected = uncompleted.shuffled(Random(seed)).take(5)
        return sortTasksProgressively(selected)
    }

    /**
     * Non-repeating consumption rotation for Weekly Tasks:
     * - Filters out all tasks in [completedIds].
     * - Shuffles remaining using week seed & picks 7 active tasks.
     * - Returns the 7 tasks strictly ordered from easiest to hardest.
     */
    fun getRotatedWeeklyTasks(completedIds: Set<String>, weekStr: String = currentWeekString()): List<TaskItem> {
        val uncompleted = ALL_WEEKLY_TASKS.filter { it.id !in completedIds }
        if (uncompleted.isEmpty()) {
            return sortTasksProgressively(
                ALL_WEEKLY_TASKS.takeLast(7).map { it.copy(status = TaskStatus.COMPLETED, currentCount = it.targetCount) }
            )
        }
        val seed = weekStr.hashCode().toLong()
        val selected = uncompleted.shuffled(Random(seed)).take(7)
        return sortTasksProgressively(selected)
    }

    /**
     * Non-repeating consumption rotation for Monthly Tasks:
     * - Filters out all tasks in [completedIds].
     * - Shuffles remaining using month seed & picks 10 active tasks.
     * - Returns the 10 tasks strictly ordered from easiest to hardest.
     */
    fun getRotatedMonthlyTasks(completedIds: Set<String>, monthStr: String = currentMonthString()): List<TaskItem> {
        val uncompleted = ALL_MONTHLY_TASKS.filter { it.id !in completedIds }
        if (uncompleted.isEmpty()) {
            return sortTasksProgressively(
                ALL_MONTHLY_TASKS.takeLast(10).map { it.copy(status = TaskStatus.COMPLETED, currentCount = it.targetCount) }
            )
        }
        val seed = monthStr.hashCode().toLong()
        val selected = uncompleted.shuffled(Random(seed)).take(10)
        return sortTasksProgressively(selected)
    }

    /**
     * Annual / Master Goals Tasks:
     * - Returns all 250 annual macro-achievements ordered progressively from easiest to hardest.
     */
    fun getEverlastingTasks(completedIds: Set<String>): List<TaskItem> {
        return ALL_EVERLASTING_TASKS.map { task ->
            if (task.id in completedIds) {
                task.copy(status = TaskStatus.COMPLETED, currentCount = task.targetCount)
            } else {
                task.copy(status = TaskStatus.PENDING, currentCount = 0)
            }
        }
    }
}
