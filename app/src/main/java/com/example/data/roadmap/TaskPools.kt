package com.example.data.roadmap

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random

/**
 * 1000-Task Matrix & Exponential Roadmap XP Engine for StrikePulse:
 * - Exactly 1000 Tasks across 4 Categories (250 Daily, 250 Weekly, 250 Monthly, 250 Everlasting)
 * - Total XP Pool across all 1000 tasks: Exactly 434,725 XP
 * - 500 Stations Exponential Curve: Station 1 = 50 XP, Station 500 = 434,725 XP
 * - Non-repeating consumption rotation with date-seeded shuffle (00:00 reset)
 */
object TaskPools {

    const val TOTAL_TASKS_COUNT = 1000
    const val CATEGORY_TASKS_COUNT = 250
    const val TOTAL_XP_POOL = 434725L

    // ==========================================
    // 5-TIER REWARD MATRIX (PURGED OF HAMMERS)
    // ==========================================

    // 5-Tier Reward Matrix (Tier 1: Shields, Tier 2: Skip Keys, Tier 3: Cyber Potions, Tier 4: Avatars & Titles, Tier 5: Station 500 Eternal Rebirth)
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
            // Extra Legendary Reward every 20 levels (20, 40, 60... 480) paired with titles/avatars
            stationId % 20 == 0 -> StationReward(
                type = RewardType.KEY_LEGENDARY,
                title = "مكافأة أسطورية محطة #$stationId (Legendary Apex)",
                description = "جائزة المستوى العشرين: مفتاح أسطوري مزدوج وفتح ألقاب وأفاتار حصرية في الملف الشخصي",
                iconName = "key_legendary",
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
     * Formula: round(50.0 * ((434725.0 / 50.0) ^ ((stationId - 1) / 499.0)))
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
        return vals.toList()
    }

    val DAILY_XP_LIST: List<Int> = generateExactXpList(250, 50, 240, 36250)
    val WEEKLY_XP_LIST: List<Int> = generateExactXpList(250, 100, 290, 48750)
    val MONTHLY_XP_LIST: List<Int> = generateExactXpList(250, 200, 635, 104375)
    val EVERLASTING_XP_LIST: List<Int> = generateExactXpList(250, 500, 1480, 245350)

    // ==========================================
    // 1000-TASK MATRIX (FULL INVENTORY)
    // ==========================================

    val ALL_DAILY_TASKS: List<TaskItem> by lazy {
        (0 until 250).map { i ->
            val idx = i + 1
            val t = i % 11
            val cnt10 = (i % 10) + 1
            val cnt5 = (i % 5) + 1
            val pct = 10 + (i % 10) * 10
            val xp = DAILY_XP_LIST[i]

            val (title, desc, targetCount) = when (t) {
                0 -> Triple(
                    "حقق $cnt10 سترايك في يوم واحد",
                    "قم بتسجيل $cnt10 ضربات نشطة اليوم لتعزيز تدفق الطاقة",
                    cnt10
                )
                1 -> Triple(
                    "حقق $cnt10 سترايك في أقل من ساعة",
                    "نفذ هجوماً سريعاً مكثفاً بواقع $cnt10 سترايك خلال 60 دقيقة",
                    cnt10
                )
                2 -> Triple(
                    "اضغط على زر التبريد Girl ($cnt10) مرات اليوم",
                    "قم بتهدئة النظام وضغط زر التبريد Girl $cnt10 مرات لخفض حرارة الشعلة",
                    cnt10
                )
                3 -> Triple(
                    "اضغط على زر التخفيض Deflame ($cnt10) مرات اليوم",
                    "نفذ تخفيضاً فورياً لطاقة الشعلة بالضغط على Deflame $cnt10 مرات",
                    cnt10
                )
                4 -> Triple(
                    "استعمل درع الـ 24 ساعة ($cnt5) مرات",
                    "قم بتفعيل درع حماية الـ 24 ساعة $cnt5 مرات لحماية السلسلة من الخمول",
                    cnt5
                )
                5 -> Triple(
                    "استعمل درع الـ 48 ساعة ($cnt5) مرات",
                    "حصّن النظام بتفعيل درع الـ 48 ساعة $cnt5 مرات",
                    cnt5
                )
                6 -> Triple(
                    "استعمل درع الـ 72 ساعة ($cnt5) مرات",
                    "تفعيل دفاعات السلسلة عبر درع 3 أيام ($cnt5 مرات)",
                    cnt5
                )
                7 -> {
                    val rarityStr = when (i % 3) {
                        0 -> "الشائع"
                        1 -> "العادي"
                        else -> "النادر"
                    }
                    Triple(
                        "استعمل المفتاح $rarityStr ($cnt5) مرات",
                        "استهلك مفتاح تجاوز $rarityStr لتسريع إنجاز المهام ($cnt5 مرات)",
                        cnt5
                    )
                }
                8 -> Triple(
                    "اضغط على زر التخفيض والتبريد بالتناوب ($cnt5) مرات",
                    "نفذ توازناً حرارياً بالتناوب بين زر التخفيض Deflame وزر التبريد Girl ($cnt5 مرات)",
                    cnt5
                )
                9 -> Triple(
                    "أضف شخصاً في اليوم ($cnt5 مرات)",
                    "سجل $cnt5 أفراد جدد في مصفوفة الاستهداف والتحليل اليوم",
                    cnt5
                )
                else -> {
                    val hormone = if (i % 2 == 0) "استروجين" else "تيستيستيرون"
                    Triple(
                        "حقق مستوى $pct% $hormone اليوم",
                        "اضبط ميزان الطاقة الحيوية للوصول إلى تركيز $pct% $hormone",
                        1
                    )
                }
            }

            TaskItem(
                id = "daily_$idx",
                period = TaskPeriod.DAILY,
                title = title,
                description = desc,
                xpBounty = xp,
                targetCount = targetCount,
                requiredKeyRarity = if (xp < 120) KeyRarity.COMMON else KeyRarity.NORMAL
            )
        }
    }

    val ALL_WEEKLY_TASKS: List<TaskItem> by lazy {
        (0 until 250).map { i ->
            val idx = i + 1
            val t = i % 10
            val cnt4_15 = 4 + (i % 12)
            val cnt1_15 = 1 + (i % 15)
            val cnt1_20 = 1 + (i % 20)
            val cnt1_10 = 1 + (i % 10)
            val xp = WEEKLY_XP_LIST[i]

            val (title, desc, targetCount) = when (t) {
                0 -> Triple(
                    "حقق سترايك واحد في الأسبوع (المتكدس #$idx)",
                    "نفذ ضربة نظام استراتيجية ناجحة خلال هذا الأسبوع (التكرار #$idx)",
                    1
                )
                1 -> Triple(
                    "اضغط على زر التبريد ($cnt4_15) مرات في الأسبوع",
                    "حافظ على استقرار الشعلة بالضغط على زر التبريد $cnt4_15 مرة خلال الأسبوع",
                    cnt4_15
                )
                2 -> Triple(
                    "اضغط على زر التخفيض يومياً لمدة 7 أيام (التكرار #$idx)",
                    "انضباط يومي مستمر: خفض الشعلة كل يوم لمدة أسبوع كامل",
                    7
                )
                3 -> Triple(
                    "استعمل درع الـ 7 أيام مرة واحدة (الدورة #$idx)",
                    "تفعيل درع الصمود الأسبوعي التلقائي أو اليدوي لحماية السلسلة",
                    1
                )
                4 -> Triple(
                    "استعمل المفتاح الأسطوري مرة واحدة (الأسبوع #$idx)",
                    "استهلك مفتاحاً أسطورياً لتخطي المهام الأسبوعية المعقدة فورياً",
                    1
                )
                5 -> Triple(
                    "حقق انضباط التبريد الأسبوعي المكثف (الجولة #$idx)",
                    "تنفيذ 5 دورات تبريد مركّزة في أوقات متفرقة خلال الأسبوع",
                    5
                )
                6 -> Triple(
                    "أضف ($cnt1_15) أشخاص في الأسبوع",
                    "توسيع شبكة الاستهداف بإضافة $cnt1_15 أفراد جدد خلال هذا الأسبوع",
                    cnt1_15
                )
                7 -> Triple(
                    "حقق ($cnt1_20) سترايك خلال هذا الأسبوع",
                    "تسجيل $cnt1_20 ضربة نشطة خلال الجولة الأسبوعية المفتوحة",
                    cnt1_20
                )
                8 -> Triple(
                    "حافظ على معدل التبريد أسبوعياً بواقع ($cnt1_10) ضغطات",
                    "توازن ديناميكي منتظم لتبريد الشعلة عبر $cnt1_10 عمليات تبريد أسبوعية",
                    cnt1_10
                )
                else -> Triple(
                    "تجنب السقوط تحت الشعلة النورانية في الأسبوع رقم #$idx",
                    "البقاء فوق حاجز 2000 نقطة والحفاظ على مسار الضياء طوال الأسبوع",
                    1
                )
            }

            TaskItem(
                id = "weekly_$idx",
                period = TaskPeriod.WEEKLY,
                title = title,
                description = desc,
                xpBounty = xp,
                targetCount = targetCount,
                requiredKeyRarity = if (xp < 180) KeyRarity.NORMAL else KeyRarity.RARE
            )
        }
    }

    val ALL_MONTHLY_TASKS: List<TaskItem> by lazy {
        (0 until 250).map { i ->
            val idx = i + 1
            val t = i % 6
            val coolCounts = listOf(1, 2, 3, 4, 5, 8, 12, 15)
            val downCounts = listOf(1, 2, 3, 5, 10, 15, 20)
            val cntCool = coolCounts[i % coolCounts.size]
            val cntDown = downCounts[i % downCounts.size]
            val cntPeople = 1 + (i % 20)
            val xp = MONTHLY_XP_LIST[i]

            val (title, desc, targetCount) = when (t) {
                0 -> Triple(
                    "حقق سترايك واحد في الشهر (رقم #$idx)",
                    "إنجاز ضربة شهرية حاسمة ضمن المسار الشهري المتقدم",
                    1
                )
                1 -> Triple(
                    "اضغط على زر التبريد ($cntCool) مرات في الشهر",
                    "تنفيذ $cntCool عمليات تبريد للشعلة خلال هذا الشهر",
                    cntCool
                )
                2 -> Triple(
                    "اضغط على زر التخفيض ($cntDown) مرة في الشهر",
                    "خفض طاقة الشعلة التراكمية $cntDown مرة خلال هذا الشهر",
                    cntDown
                )
                3 -> Triple(
                    "حقق الاستقرار الحراري الكامل للشهر (المستوى #$idx)",
                    "المحافظة على التوازن الشهري وتفادي قفزات الشعلة العشوائية",
                    1
                )
                4 -> Triple(
                    "أضف ($cntPeople) شخصاً خلال هذا الشهر",
                    "إضافة $cntPeople شخصاً إلى الأرشيف السحابي والتحليلي خلال الشهر",
                    cntPeople
                )
                else -> Triple(
                    "تنفيذ التحديات الشهرية الهيكلية الكبرى (المعامل #$idx)",
                    "الصمود والانضباط الشهري الكامل وتحقيق مستويات الاستقرار الحيوي",
                    1
                )
            }

            TaskItem(
                id = "monthly_$idx",
                period = TaskPeriod.MONTHLY,
                title = title,
                description = desc,
                xpBounty = xp,
                targetCount = targetCount,
                requiredKeyRarity = if (xp < 380) KeyRarity.RARE else KeyRarity.LEGENDARY
            )
        }
    }

    val ALL_EVERLASTING_TASKS: List<TaskItem> by lazy {
        (0 until 250).map { i ->
            val idx = i + 1
            val t = i % 11
            val strikeThresholds = listOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 120, 150, 200)
            val strikeCount = strikeThresholds[i % strikeThresholds.size]
            val xp = EVERLASTING_XP_LIST[i]

            val (title, desc, targetCount) = when (t) {
                0 -> Triple(
                    "حقق سترايك واحد في السنة (الموسم #$idx)",
                    "تسجيل ضربة أسطورية في السجل السنوي التاريخي للهدف رقم #$idx",
                    1
                )
                1 -> Triple(
                    "حقق أكثر من $strikeCount سترايك في السنة",
                    "تجاوز حاجز $strikeCount ضربة تراكمية في السجل السنوي العام",
                    strikeCount
                )
                2 -> if (i % 2 == 0) Triple(
                    "أضف شخصاً واحداً في السنة (التوسعة #$idx)",
                    "إضافة فرد استراتيجي جديد للأرشيف السنوي التاريخي",
                    1
                ) else Triple(
                    "أضف شخصين في السنة (التوسعة المزدوجة #$idx)",
                    "إضافة فردين استراتيجيين للأرشيف السنوي التاريخي",
                    2
                )
                3 -> Triple(
                    "أضف 50 شخصاً (الدفعة الاستراتيجية الكبرى #$idx)",
                    "توسيع نطاق قاعدة البيانات والوصول إلى 50 شخصاً مسجلاً في النظام",
                    50
                )
                4 -> Triple(
                    "أضف 100 شخص (سيادة الشبكة الكاملة #$idx)",
                    "بناء شبكة تحليلية عملاقة تحتوي على 100 شخص مسجل بالكامل",
                    100
                )
                5 -> Triple(
                    "احصل على لقب \"الأنثى المطلقة\" (الإنجاز الأسطوري #$idx)",
                    "الوصول إلى قاع المؤشر الهرموني (0 PTS) وتحقيق لقب الأنثى المطلقة",
                    1
                )
                6 -> Triple(
                    "احصل على لقب \"بافوميت\" (قاع الظلام المطلق #$idx)",
                    "ملامسة رتبة بافوميت السفلية (0 PTS) واختبار القبضة الكاملة للكيان المظلم",
                    1
                )
                7 -> if (i % 2 == 0) Triple(
                    "حقق مستوى 100% استروجين (التوازن الهرموني الأقصى #$idx)",
                    "إتقان المؤشر الحيوي للوصول لنسبة 100% استروجين كاملة",
                    1
                ) else Triple(
                    "حقق مستوى 100% تيستيستيرون (السيادة الحيوية القصوى #$idx)",
                    "إتقان المؤشر الحيوي للوصول لنسبة 100% تيستيستيرون كاملة",
                    1
                )
                8 -> Triple(
                    "حقق النتيجة 0 للشعلة (الوصول للهاوية #$idx)",
                    "تجميد الشعلة بالكامل عند النقطة 0 والتحكم التام في درجات البرودة المطلقة",
                    1
                )
                9 -> Triple(
                    "حقق النتيجة 10,000 للشعلة (قمة جبريل الأبدية #$idx)",
                    "الارتقاء بالشعلة إلى الذروة المطلقة 10000 نقطة وتفعيل لمسة جبريل البيضاء الصافية",
                    1
                )
                else -> Triple(
                    "مصفوفة الأهداف الأبدية الممتدة (الإنجاز السيبراني #$idx)",
                    "إتمام المحطات العظمى لمصفوفة الأهداف الأبدية الخالدة في تطبيق StrikePulse",
                    1
                )
            }

            TaskItem(
                id = "everlasting_$idx",
                period = TaskPeriod.YEARLY,
                title = title,
                description = desc,
                xpBounty = xp,
                targetCount = targetCount,
                requiredKeyRarity = KeyRarity.LEGENDARY
            )
        }
    }

    val ALL_1000_TASKS: List<TaskItem> by lazy {
        ALL_DAILY_TASKS + ALL_WEEKLY_TASKS + ALL_MONTHLY_TASKS + ALL_EVERLASTING_TASKS
    }

    // ==========================================
    // DATE SEED & ROTATION ENGINE
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

    /**
     * Non-repeating consumption rotation for Daily Tasks:
     * - Filters out all tasks in [completedIds].
     * - Shuffles remaining using date seed (00:00 reset).
     * - Takes 5 active tasks.
     */
    fun getRotatedDailyTasks(completedIds: Set<String>, dateStr: String = todayDateString()): List<TaskItem> {
        val uncompleted = ALL_DAILY_TASKS.filter { it.id !in completedIds }
        if (uncompleted.isEmpty()) {
            return ALL_DAILY_TASKS.takeLast(5).map { it.copy(status = TaskStatus.COMPLETED, currentCount = it.targetCount) }
        }
        val seed = dateStr.hashCode().toLong()
        val shuffled = uncompleted.shuffled(Random(seed))
        return shuffled.take(5)
    }

    /**
     * Non-repeating consumption rotation for Weekly Tasks:
     * - Filters out all tasks in [completedIds].
     * - Shuffles remaining using week seed.
     * - Takes 7 active tasks.
     */
    fun getRotatedWeeklyTasks(completedIds: Set<String>, weekStr: String = currentWeekString()): List<TaskItem> {
        val uncompleted = ALL_WEEKLY_TASKS.filter { it.id !in completedIds }
        if (uncompleted.isEmpty()) {
            return ALL_WEEKLY_TASKS.takeLast(7).map { it.copy(status = TaskStatus.COMPLETED, currentCount = it.targetCount) }
        }
        val seed = weekStr.hashCode().toLong()
        val shuffled = uncompleted.shuffled(Random(seed))
        return shuffled.take(7)
    }

    /**
     * Non-repeating consumption rotation for Monthly Tasks:
     * - Filters out all tasks in [completedIds].
     * - Shuffles remaining using month seed.
     * - Takes 10 active tasks.
     */
    fun getRotatedMonthlyTasks(completedIds: Set<String>, monthStr: String = currentMonthString()): List<TaskItem> {
        val uncompleted = ALL_MONTHLY_TASKS.filter { it.id !in completedIds }
        if (uncompleted.isEmpty()) {
            return ALL_MONTHLY_TASKS.takeLast(10).map { it.copy(status = TaskStatus.COMPLETED, currentCount = it.targetCount) }
        }
        val seed = monthStr.hashCode().toLong()
        val shuffled = uncompleted.shuffled(Random(seed))
        return shuffled.take(10)
    }

    /**
     * Everlasting / Master Goals Tasks:
     * - Returns all 250 everlasting tasks at once with their completion status reflected.
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
