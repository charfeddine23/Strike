package com.example.data.roadmap

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

object TaskPools {

    // 10 Strategic Rewards mapped cyclically across the 500 stations
    val STRATEGIC_REWARDS = listOf(
        StationReward(
            type = RewardType.SHIELD_24H,
            title = "درع حماية 24 ساعة",
            description = "إيقاف تراكم نقاط الخمول لمدة 24 ساعة وحفظ السلسلة",
            iconName = "shield_24h",
            quantity = 1
        ),
        StationReward(
            type = RewardType.KEY_COMMON,
            title = "مفتاح تجاوز شائع (Common Key)",
            description = "تجاوز فوري لأي مهمة يومية شائعة مع منح نقاط الـ XP",
            iconName = "key_common",
            quantity = 1
        ),
        StationReward(
            type = RewardType.SHIELD_48H,
            title = "درع حماية 48 ساعة",
            description = "حماية السلسلة لمدة يومين كاملين من الخمول",
            iconName = "shield_48h",
            quantity = 1
        ),
        StationReward(
            type = RewardType.KEY_NORMAL,
            title = "مفتاح تجاوز عادي (Normal Key)",
            description = "تجاوز فوري للمهام اليومية والأسبوعية العادية",
            iconName = "key_normal",
            quantity = 1
        ),
        StationReward(
            type = RewardType.GIRL_BOOST_HAMMER_7D,
            title = "مطرقة تعزيز Girl (7 أيام)",
            description = "زيادة فورية لقيمة زر Girl إلى +50 نقطة لكل ضغطة لمدة أسبوع كامل",
            iconName = "hammer_girl",
            quantity = 1
        ),
        StationReward(
            type = RewardType.SHIELD_72H,
            title = "درع حماية 72 ساعة (3 أيام)",
            description = "درع دفاعي متين يوقف الخمول لمدة 3 أيام",
            iconName = "shield_72h",
            quantity = 1
        ),
        StationReward(
            type = RewardType.KEY_RARE,
            title = "مفتاح تجاوز نادر (Rare Key)",
            description = "تجاوز المهام الأسبوعية والشهرية المتقدمة فورياً",
            iconName = "key_rare",
            quantity = 1
        ),
        StationReward(
            type = RewardType.DEFLAME_BOOST_HAMMER_7D,
            title = "مطرقة تعزيز Deflame (7 أيام)",
            description = "تعزيز تلقائي لزر Deflame إلى +50 نقطة لكل ضغطة لمدة 7 أيام",
            iconName = "hammer_deflame",
            quantity = 1
        ),
        StationReward(
            type = RewardType.KEY_LEGENDARY,
            title = "مفتاح تجاوز أسطوري (Legendary Key)",
            description = "مفتاح ذهبي خارق يتجاوز أي مهمة شهرية أو سنوية أسطورية بنقاطها كاملة",
            iconName = "key_legendary",
            quantity = 1
        ),
        StationReward(
            type = RewardType.THOR_HAMMER_30D,
            title = "مطرقة ثور الأسطورية (Thor's Hammer 30D)",
            description = "تخفيض بنسبة 60% من أثر أي ضربة قادمة لمدة شهر كامل (30 يوماً)",
            iconName = "hammer_thor",
            quantity = 1
        )
    )

    /**
     * Generates a StationReward for any station index from 1 to 500.
     * Milestones (every 25, 50, 100, 500) award Legendary and Epic rewards.
     */
    fun getRewardForStation(stationId: Int): StationReward {
        return when {
            stationId == 500 -> StationReward(
                type = RewardType.THOR_HAMMER_30D,
                title = "تاج السيادة المطلقة + مطرقة ثور الخارقة",
                description = "الجائزة الكبرى للمحطة 500: حماية ثور لمدة شهر + 10 مفاتيح أسطورية",
                iconName = "hammer_thor",
                quantity = 10
            )
            stationId % 100 == 0 -> StationReward(
                type = RewardType.THOR_HAMMER_30D,
                title = "مطرقة ثور الكبرى (Thor's Hammer)",
                description = "جائزة المئة الكبرى: تقليل 60% من الضربات لمدة 30 يوماً",
                iconName = "hammer_thor",
                quantity = 1
            )
            stationId % 50 == 0 -> StationReward(
                type = RewardType.KEY_LEGENDARY,
                title = "مفتاح أسطوري مزدوج (2x Legendary Keys)",
                description = "مفتاحان أسطوريان لتجاوز المهام الكبرى",
                iconName = "key_legendary",
                quantity = 2
            )
            stationId % 25 == 0 -> StationReward(
                type = RewardType.GIRL_BOOST_HAMMER_7D,
                title = "مطرقة تعزيز Girl الأسبوعية",
                description = "+50 نقطة لكل ضغطة زر Girl لمدة 7 أيام",
                iconName = "hammer_girl",
                quantity = 1
            )
            else -> {
                val idx = (stationId - 1) % STRATEGIC_REWARDS.size
                STRATEGIC_REWARDS[idx]
            }
        }
    }

    /**
     * Formula: Required Total XP = Station ID * 100 XP
     * Station 1: 100 XP
     * Station 10: 1,000 XP
     * Station 50: 5,000 XP
     * Station 500: 50,000 XP
     */
    fun getRequiredXpForStation(stationId: Int): Long {
        return stationId.toLong() * 100L
    }

    // ==========================================
    // EXPLICIT TASK POOLS
    // ==========================================

    val DAILY_CANDIDATE_POOL = listOf(
        TaskItem(
            id = "daily_deflame_1",
            period = TaskPeriod.DAILY,
            title = "تخفيض سريع 1 مرة (Deflame x1)",
            description = "اضغط على زر Deflame مرة واحدة لخفض الشعلة",
            xpBounty = 25,
            targetCount = 1,
            requiredKeyRarity = KeyRarity.COMMON
        ),
        TaskItem(
            id = "daily_deflame_2",
            period = TaskPeriod.DAILY,
            title = "تخفيض الشعلة 2 مرات (Deflame x2)",
            description = "قم بخفض الشعلة مرتين لضبط الطاقة الحركية",
            xpBounty = 40,
            targetCount = 2,
            requiredKeyRarity = KeyRarity.COMMON
        ),
        TaskItem(
            id = "daily_deflame_3",
            period = TaskPeriod.DAILY,
            title = "تخفيض ثلاثي للشعلة (Deflame x3)",
            description = "اضغط على Deflame 3 مرات اليوم",
            xpBounty = 60,
            targetCount = 3,
            requiredKeyRarity = KeyRarity.COMMON
        ),
        TaskItem(
            id = "daily_deflame_5",
            period = TaskPeriod.DAILY,
            title = "تخفيض مكثف 5 مرات (Deflame x5)",
            description = "قم بـ 5 عمليات خفض متتالية للتحكم الفوري في الشعلة",
            xpBounty = 80,
            targetCount = 5,
            requiredKeyRarity = KeyRarity.COMMON
        ),
        TaskItem(
            id = "daily_girl_1",
            period = TaskPeriod.DAILY,
            title = "تبريد Girl 1 مرة (Girl x1)",
            description = "اضغط على زر Girl مرة واحدة اليوم",
            xpBounty = 25,
            targetCount = 1,
            requiredKeyRarity = KeyRarity.COMMON
        ),
        TaskItem(
            id = "daily_girl_2",
            period = TaskPeriod.DAILY,
            title = "تبريد مزدوج Girl (Girl x2)",
            description = "اضغط على زر Girl مرتين لضبط التوازن",
            xpBounty = 45,
            targetCount = 2,
            requiredKeyRarity = KeyRarity.COMMON
        ),
        TaskItem(
            id = "daily_girl_4",
            period = TaskPeriod.DAILY,
            title = "تبريد عالي Girl (Girl x4)",
            description = "اضغط على زر Girl 4 مرات اليوم لتخفيف الضغط",
            xpBounty = 70,
            targetCount = 4,
            requiredKeyRarity = KeyRarity.COMMON
        ),
        TaskItem(
            id = "daily_alternating_shock",
            period = TaskPeriod.DAILY,
            title = "صدمة التناوب الحركي (Alternating Shock)",
            description = "تفعيل متناوب دقيق بين زر Deflame وزر Girl",
            xpBounty = 55,
            targetCount = 2,
            requiredKeyRarity = KeyRarity.COMMON
        ),
        TaskItem(
            id = "daily_active_strike",
            period = TaskPeriod.DAILY,
            title = "ضربة الهدف النشط (Active-Only Strike)",
            description = "تسجيل ضربة ضد هدف في حالة 'Active' حصرياً وليس في وضع الاستعداد",
            xpBounty = 50,
            targetCount = 1,
            requiredKeyRarity = KeyRarity.COMMON
        ),
        TaskItem(
            id = "daily_double_strike",
            period = TaskPeriod.DAILY,
            title = "الضربة الثنائية المركزة (Double Strike)",
            description = "تنفيذ ضربتين على هدفين نشطين في نفس اليوم",
            xpBounty = 75,
            targetCount = 2,
            requiredKeyRarity = KeyRarity.NORMAL
        ),
        TaskItem(
            id = "daily_counter_strike",
            period = TaskPeriod.DAILY,
            title = "الضربة المضادة (Counter-Strike)",
            description = "تنفيذ ضربة مضادة تكتيكية ضد هدف نشط",
            xpBounty = 60,
            targetCount = 1,
            requiredKeyRarity = KeyRarity.NORMAL
        ),
        TaskItem(
            id = "daily_inspect_dna",
            period = TaskPeriod.DAILY,
            title = "فحص الشيفرة الجينية (Inspect DNA)",
            description = "الدخول لشاشة الانحرافات وتدوير هيكل الـ DNA ثلاثي الأبعاد",
            xpBounty = 30,
            targetCount = 1,
            requiredKeyRarity = KeyRarity.COMMON
        ),
        TaskItem(
            id = "daily_shield_check",
            period = TaskPeriod.DAILY,
            title = "تدقيق درع السلسلة (Shield Audit)",
            description = "التحقق من ساعات درع السلسلة ومخزون الدروع المتاحة",
            xpBounty = 30,
            targetCount = 1,
            requiredKeyRarity = KeyRarity.COMMON
        ),
        TaskItem(
            id = "daily_early_login",
            period = TaskPeriod.DAILY,
            title = "حضور الفجر المبكر (Early Login)",
            description = "فتح التطبيق وتفقد المنظومة قبل الساعة 8:00 صباحاً",
            xpBounty = 45,
            targetCount = 1,
            requiredKeyRarity = KeyRarity.COMMON
        ),
        TaskItem(
            id = "daily_balance_gauge",
            period = TaskPeriod.DAILY,
            title = "معاينة الاتزان الهرموني (Check Bio-Gauge)",
            description = "فحص مقياس الإستروجين والتيستوستيرون في شاشة الانحرافات",
            xpBounty = 35,
            targetCount = 1,
            requiredKeyRarity = KeyRarity.COMMON
        ),
        TaskItem(
            id = "daily_clear_cache",
            period = TaskPeriod.DAILY,
            title = "صيانة الذاكرة الرقمية (System Maintenance)",
            description = "التحقق من استقرار النظام وصحة السجلات",
            xpBounty = 30,
            targetCount = 1,
            requiredKeyRarity = KeyRarity.COMMON
        )
    )

    val WEEKLY_CORE_TASKS = listOf(
        TaskItem(
            id = "weekly_1_shield",
            period = TaskPeriod.WEEKLY,
            title = "1. درع الانضباط الأسبوعي",
            description = "إكمال المهام اليومية لـ 7 أيام متتالية دون انقطاع",
            xpBounty = 200,
            targetCount = 7,
            requiredKeyRarity = KeyRarity.RARE
        ),
        TaskItem(
            id = "weekly_2_strategic_strikes",
            period = TaskPeriod.WEEKLY,
            title = "2. الضربات الاستراتيجية",
            description = "تنفيذ 5 ضربات ناجحة على أهداف نشطة دون هجمات مرتدة",
            xpBounty = 180,
            targetCount = 5,
            requiredKeyRarity = KeyRarity.NORMAL
        ),
        TaskItem(
            id = "weekly_3_energy_balance",
            period = TaskPeriod.WEEKLY,
            title = "3. اتزان الطاقة البيولوجية",
            description = "الحفاظ على المقياس البيولوجي متزناً لمدة 3 أيام متتالية",
            xpBounty = 160,
            targetCount = 3,
            requiredKeyRarity = KeyRarity.NORMAL
        ),
        TaskItem(
            id = "weekly_4_roadmap_leap",
            period = TaskPeriod.WEEKLY,
            title = "4. قفزة مسار المحطات",
            description = "التقدم بـ 10 محطات جديدة على المسار المتموج (Roadmap)",
            xpBounty = 220,
            targetCount = 10,
            requiredKeyRarity = KeyRarity.RARE
        ),
        TaskItem(
            id = "weekly_5_cyber_defense",
            period = TaskPeriod.WEEKLY,
            title = "5. الدفاع السيبراني المحصن",
            description = "صد أو تجنب 3 ضربات قادمة بنجاح باستخدام دروع السلسلة",
            xpBounty = 190,
            targetCount = 3,
            requiredKeyRarity = KeyRarity.NORMAL
        ),
        TaskItem(
            id = "weekly_6_flame_ignition",
            period = TaskPeriod.WEEKLY,
            title = "6. إيقاد شعلة الطاقة",
            description = "رفع مجموع نقاط الشعلة بـ +1000 نقطة تراكمية",
            xpBounty = 250,
            targetCount = 1000,
            requiredKeyRarity = KeyRarity.RARE
        ),
        TaskItem(
            id = "weekly_7_archive_review",
            period = TaskPeriod.WEEKLY,
            title = "7. مراجعة الأرشيف الأسبوعي",
            description = "مراجعة سجلات الرتب المظلمة والنورانية وتوثيق تحولات الأسبوع",
            xpBounty = 150,
            targetCount = 1,
            requiredKeyRarity = KeyRarity.NORMAL
        )
    )

    val MONTHLY_MILESTONES = listOf(
        TaskItem(
            id = "monthly_1_fortress",
            period = TaskPeriod.MONTHLY,
            title = "1. الحصن المنيع (Unyielding Fortress)",
            description = "اجتياز 30 يوماً كاملة فوق خط الخطر المظلم (> 1000 نقطة شعلة)",
            xpBounty = 500,
            targetCount = 30,
            requiredKeyRarity = KeyRarity.LEGENDARY
        ),
        TaskItem(
            id = "monthly_2_conquest",
            period = TaskPeriod.MONTHLY,
            title = "2. فتح المحطات (Roadmap Conquest)",
            description = "اجتياز 50 محطة كاملة على المسار المتموج خلال شهر واحد",
            xpBounty = 450,
            targetCount = 50,
            requiredKeyRarity = KeyRarity.LEGENDARY
        ),
        TaskItem(
            id = "monthly_3_elite_supremacy",
            period = TaskPeriod.MONTHLY,
            title = "3. السيادة النورانية (Elite Supremacy)",
            description = "الوصول إلى الرتب النورانية المتقدمة (أفق جبريل) والمحافظة عليها لأسبوع",
            xpBounty = 400,
            targetCount = 7,
            requiredKeyRarity = KeyRarity.LEGENDARY
        ),
        TaskItem(
            id = "monthly_4_hercules_flow",
            period = TaskPeriod.MONTHLY,
            title = "4. السيطرة البيولوجية (Biological Dominance)",
            description = "تثبيت المقياس الهرموني في ذروة 'هرقل' أو السيولة الكاملة لنصف شهر",
            xpBounty = 400,
            targetCount = 15,
            requiredKeyRarity = KeyRarity.RARE
        ),
        TaskItem(
            id = "monthly_5_arsenal_mgmt",
            period = TaskPeriod.MONTHLY,
            title = "5. إدارة ترسانة المفاتيح (Key Arsenal Management)",
            description = "استخدام مفاتيح التجاوز بحكمة واستراتيجية أثناء إدارة الأزمات",
            xpBounty = 300,
            targetCount = 3,
            requiredKeyRarity = KeyRarity.RARE
        ),
        TaskItem(
            id = "monthly_6_cyber_warfare",
            period = TaskPeriod.MONTHLY,
            title = "6. الحرب السيبرانية (Cyber Warfare)",
            description = "قيادة سلسلة ضربات استراتيجية ناجحة ضد أهداف نشطة",
            xpBounty = 350,
            targetCount = 10,
            requiredKeyRarity = KeyRarity.RARE
        ),
        TaskItem(
            id = "monthly_7_ultimate_shock",
            period = TaskPeriod.MONTHLY,
            title = "7. الصدمة المطلقة (Ultimate Shock)",
            description = "تفعيل مطرقة ثور في لحظة حرجة لتخفيض أثر ضربة كبرى بـ 60%",
            xpBounty = 500,
            targetCount = 1,
            requiredKeyRarity = KeyRarity.LEGENDARY
        ),
        TaskItem(
            id = "monthly_8_architectural_sync",
            period = TaskPeriod.MONTHLY,
            title = "8. التناغم المعماري (Architectural Sync)",
            description = "تفعيل متناوب دقيق بين Deflame و Girl عبر 20 جلسة مختلفة",
            xpBounty = 350,
            targetCount = 20,
            requiredKeyRarity = KeyRarity.RARE
        ),
        TaskItem(
            id = "monthly_9_neural_log",
            period = TaskPeriod.MONTHLY,
            title = "9. السجل العصبي (Neural Log)",
            description = "توثيق 30 تأملاً يومياً في سجل النشاط والذاكرة",
            xpBounty = 380,
            targetCount = 30,
            requiredKeyRarity = KeyRarity.RARE
        ),
        TaskItem(
            id = "monthly_10_legendary_transit",
            period = TaskPeriod.MONTHLY,
            title = "10. العبور الأسطوري (Legendary Transit)",
            description = "إكمال 100% من مهام الأسابيع الأربعة بنجاح مطلق",
            xpBounty = 600,
            targetCount = 4,
            requiredKeyRarity = KeyRarity.LEGENDARY
        )
    )

    val YEARLY_EPIC_GOALS = listOf(
        TaskItem(
            id = "yearly_1_divine_throne",
            period = TaskPeriod.YEARLY,
            title = "1. العرش الإلهي (Divine Throne)",
            description = "بلوغ رتبة 'جبريل' (10,000 نقطة) وتثبيت توهج الـ DNA الأبيض النقي لـ شهر كامل",
            xpBounty = 1000,
            targetCount = 30,
            requiredKeyRarity = KeyRarity.LEGENDARY
        ),
        TaskItem(
            id = "yearly_2_station_500",
            period = TaskPeriod.YEARLY,
            title = "2. السيادة المطلقة (Station 500 Sovereign)",
            description = "الوصول إلى المحطة 500 في نهاية المسار المتموج وفتح القطع الأثرية الخالدة",
            xpBounty = 2500,
            targetCount = 500,
            requiredKeyRarity = KeyRarity.LEGENDARY
        ),
        TaskItem(
            id = "yearly_3_immortal_endurance",
            period = TaskPeriod.YEARLY,
            title = "3. الصمود الخالد (Immortal Endurance)",
            description = "إتمام عام كامل من الانضباط المستمر دون انهيار النظام إلى نقطة الصفر",
            xpBounty = 1500,
            targetCount = 365,
            requiredKeyRarity = KeyRarity.LEGENDARY
        ),
        TaskItem(
            id = "yearly_4_hercules_legend",
            period = TaskPeriod.YEARLY,
            title = "4. أسطورة هرقل (Hercules Legend)",
            description = "المحافظة على الرتبة البيولوجية الـ 50 لأكثر من 90 يوماً تراكمياً",
            xpBounty = 1200,
            targetCount = 90,
            requiredKeyRarity = KeyRarity.LEGENDARY
        ),
        TaskItem(
            id = "yearly_5_century_leap",
            period = TaskPeriod.YEARLY,
            title = "5. المئة الأولى العظمى",
            description = "تجاوز أول 100 محطة وفتح مطرقة ثور الأولى",
            xpBounty = 800,
            targetCount = 100,
            requiredKeyRarity = KeyRarity.LEGENDARY
        ),
        TaskItem(
            id = "yearly_6_bicentennial",
            period = TaskPeriod.YEARLY,
            title = "6. المئوية الثانية للسيادة",
            description = "بلوغ المحطة 200 بنجاح وجني كافة مكافآت البيض السيبراني",
            xpBounty = 1000,
            targetCount = 200,
            requiredKeyRarity = KeyRarity.LEGENDARY
        ),
        TaskItem(
            id = "yearly_7_master_artisan",
            period = TaskPeriod.YEARLY,
            title = "7. المعلم الحرفي للانضباط",
            description = "إكمال 500 مهمة يومية وأسبوعية وشهرية مجمعة",
            xpBounty = 1200,
            targetCount = 500,
            requiredKeyRarity = KeyRarity.LEGENDARY
        )
    )

    /**
     * Date-seeded Daily Shuffle:
     * Selects exactly 5 random distinct daily tasks from DAILY_CANDIDATE_POOL
     * seeded by the date string (YYYY-MM-DD), ensuring consistency throughout the day
     * and a fresh, non-repetitive task set every midnight at 00:00!
     */
    fun getDailyTasksForDate(dateStr: String): List<TaskItem> {
        val seed = dateStr.hashCode().toLong()
        val rng = Random(seed)
        val shuffled = DAILY_CANDIDATE_POOL.toMutableList()
        // Fisher-Yates shuffle with seeded rng
        for (i in shuffled.indices.reversed()) {
            val j = rng.nextInt(i + 1)
            val temp = shuffled[i]
            shuffled[i] = shuffled[j]
            shuffled[j] = temp
        }
        return shuffled.take(5)
    }

    fun todayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
