package com.example.data.profile

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet

/**
 * Rarity tiers for Titles & Avatars in the StrikePulse Cyberpunk Master Inventory.
 */
enum class TitleRarity(
    val labelAr: String,
    val labelEn: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color,
    val borderComplexity: Int // 1..4 (controls halo pulses & rings)
) {
    COMMON("شائع", "Common", Color(0xFF94A3B8), Color(0xFF64748B), Color(0x6694A3B8), 1),
    UNCOMMON("غير شائع", "Uncommon", NeonEmerald, Color(0xFF059669), Color(0x8810B981), 2),
    RARE("نادر", "Rare", NeonCyan, Color(0xFF0284C7), Color(0xAA00E5FF), 3),
    LEGENDARY("أسطوري", "Legendary", NeonAmber, Color(0xFFEA580C), Color(0xCCF59E0B), 4),
    ULTIMATE("مطلق", "Ultimate", NeonViolet, Color(0xFFEC4899), Color(0xEED946EF), 4),
    MILESTONE_PEAK("قمة المجد", "Apex Milestone", Color(0xFFFF453A), NeonAmber, Color(0xFFFF5555), 4)
}

data class TitleItem(
    val id: Int,
    val titleAr: String,
    val titleEn: String,
    val rarity: TitleRarity,
    val unlockStation: Int? = null,
    val isMilestone: Boolean = false,
    val description: String = ""
)

enum class AvatarRarity(
    val labelAr: String,
    val primaryColor: Color,
    val glowColor: Color
) {
    COMMON("شائع", Color(0xFF94A3B8), Color(0x6694A3B8)),
    UNCOMMON("غير شائع", NeonEmerald, Color(0x8810B981)),
    RARE("نادر", NeonCyan, Color(0xAA00E5FF)),
    LEGENDARY("أسطوري", NeonAmber, Color(0xCCF59E0B))
}

data class AvatarItem(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val rarity: AvatarRarity,
    val unlockStation: Int = 1,
    val badgeIconName: String = "bolt"
)

enum class PotionType(
    val id: String,
    val nameAr: String,
    val descriptionAr: String,
    val durationLabelAr: String,
    val color: Color
) {
    PULSE_ELIXIR(
        id = "potion_pulse_elixir",
        nameAr = "إكسير النبض",
        descriptionAr = "يمنح +5 نقاط إضافية لزر التبريد (Deflame) للـ 3 تفعيلات القادمة، ثم يختفي تلقائياً.",
        durationLabelAr = "3 استخدامات",
        color = Color(0xFF38BDF8)
    ),
    SHADOW_GRIP_SERUM(
        id = "potion_shadow_serum",
        nameAr = "مصل قبضة الظل",
        descriptionAr = "يقلل وتيرة صعود نقاط الشعلة بنسبة 50% لمدة 3 أيام.",
        durationLabelAr = "3 أيام",
        color = Color(0xFFA855F7)
    ),
    GABRIEL_PURITY_DROPLET(
        id = "potion_gabriel_droplet",
        nameAr = "قطرة نقاء جبريل",
        descriptionAr = "تسارع نمو نقاط الشعلة بنسبة 50% لمدة 7 أيام (تفعيل تلقائي وفوري عند الاستلام).",
        durationLabelAr = "7 أيام",
        color = Color(0xFFFACC15)
    ),
    ABSOLUTE_CRYO_INFUSION(
        id = "potion_cryo_infusion",
        nameAr = "الحقن التجميدي المطلق",
        descriptionAr = "يضيف +100 نقطة بونص لتعزيز الستريك التالي (اختياري، يتفعل تلقائياً بعد 30 يوم إذا لم يُستخدم).",
        durationLabelAr = "استخدام فردي",
        color = Color(0xFF00E5FF)
    ),
    ETERNAL_REBIRTH_ELIXIR(
        id = "potion_eternal_rebirth",
        nameAr = "جرعة الانبعاث الأبدي",
        descriptionAr = "الجائزة الكبرى للمحطة 500: تمنح درع مناعة أبدي ضد التصفير ولقب سيد النقوشة (Gary Master).",
        durationLabelAr = "أبدي",
        color = Color(0xFFFF007A)
    )
}

enum class SkipKeyType(
    val id: String,
    val nameAr: String,
    val maxUses: Int,
    val color: Color
) {
    WOODEN("key_wooden", "مفتاح خشبي", 1, Color(0xFFB45309)),
    BRONZE("key_bronze", "مفتاح برونزي", 2, Color(0xFFD97706)),
    SILVER("key_silver", "مفتاح فضي", 3, Color(0xFFCBD5E1))
}

object ProfileMasterInventory {

    // MILESTONE TITLE (20,000 PTS PEAK FLAME SCORE)
    val MILESTONE_PEAK_TITLE = TitleItem(
        id = 100,
        titleAr = "سيد الجمر التائب",
        titleEn = "Eternal Repentant Ember Sovereign",
        rarity = TitleRarity.MILESTONE_PEAK,
        unlockStation = null,
        isMilestone = true,
        description = "يُفتح حصرياً عند بلوغ رصيد الشعلة الأقصى 20,000 نقطة. يحمل هالة نارية سائلة ذهبية وحمراء ديناميكية."
    )

    // 50 MASTER TITLES
    val MASTER_TITLES = listOf(
        // COMMON (1..10)
        TitleItem(1, "المبتدئ العادي", "Ordinary Novice", TitleRarity.COMMON, 1),
        TitleItem(2, "المراقب الصامت", "Silent Observer", TitleRarity.COMMON, 10),
        TitleItem(3, "مقتفي الأثر", "Trace Tracker", TitleRarity.COMMON, 20),
        TitleItem(4, "جامع الشرارات", "Spark Collector", TitleRarity.COMMON, 30),
        TitleItem(5, "حارس النبض", "Pulse Sentry", TitleRarity.COMMON, 40),
        TitleItem(6, "المكافح الهادئ", "Quiet Struggler", TitleRarity.COMMON, 50),
        TitleItem(7, "صامد في العتمة", "Darkness Endurer", TitleRarity.COMMON, 60),
        TitleItem(8, "عابر المحطات", "Station Crosser", TitleRarity.COMMON, 70),
        TitleItem(9, "المستيقظ ليلاً", "Night Awakened", TitleRarity.COMMON, 80),
        TitleItem(10, "ناشئ الدرب", "Path Initiate", TitleRarity.COMMON, 90),

        // UNCOMMON (11..20)
        TitleItem(11, "كاسر الروتين", "Routine Breaker", TitleRarity.UNCOMMON, 100),
        TitleItem(12, "نبض متصاعد", "Rising Pulse", TitleRarity.UNCOMMON, 115),
        TitleItem(13, "المتأهب للنزال", "Battle Ready", TitleRarity.UNCOMMON, 130),
        TitleItem(14, "مروض الشرارة", "Spark Tamer", TitleRarity.UNCOMMON, 145),
        TitleItem(15, "حاصد العزم", "Resolve Harvester", TitleRarity.UNCOMMON, 160),
        TitleItem(16, "عنيد المسار", "Path Stubborn", TitleRarity.UNCOMMON, 175),
        TitleItem(17, "صاقل الإرادة", "Will Refiner", TitleRarity.UNCOMMON, 190),
        TitleItem(18, "درع اليقظة", "Vigilance Aegis", TitleRarity.UNCOMMON, 205),
        TitleItem(19, "فارس الظل الخافت", "Faint Shadow Knight", TitleRarity.UNCOMMON, 220),
        TitleItem(20, "مطفئ اللهب الصغير", "Ember Quencher", TitleRarity.UNCOMMON, 235),

        // RARE (21..35)
        TitleItem(21, "سيد الانضباط الصارم", "Strict Discipline Master", TitleRarity.RARE, 250),
        TitleItem(22, "قاهر النكسة العابرة", "Relapse Vanquisher", TitleRarity.RARE, 265),
        TitleItem(23, "مهندس السيطرة", "Control Architect", TitleRarity.RARE, 280),
        TitleItem(24, "صائد التردد", "Hesitation Hunter", TitleRarity.RARE, 295),
        TitleItem(25, "فولاذي العزيمة", "Steel Will", TitleRarity.RARE, 310),
        TitleItem(26, "عاصف الردع", "Deterrence Storm", TitleRarity.RARE, 325),
        TitleItem(27, "حارس البوابة الحصينة", "Fortress Gate Keeper", TitleRarity.RARE, 340),
        TitleItem(28, "منقذ السلسلة الذهبية", "Golden Streak Savior", TitleRarity.RARE, 355),
        TitleItem(29, "جلمود الصخر", "Granite Boulder", TitleRarity.RARE, 370),
        TitleItem(30, "كاسر قيود الظلال", "Shadow Chain Breaker", TitleRarity.RARE, 385),
        TitleItem(31, "المتحكم في الطاقات", "Energy Governor", TitleRarity.RARE, 400),
        TitleItem(32, "مبيد التراجع", "Retreat Annihilator", TitleRarity.RARE, 415),
        TitleItem(33, "حارق الأوهام", "Illusion Scorcher", TitleRarity.RARE, 430),
        TitleItem(34, "المحصن السيبراني", "Cyber Bastion", TitleRarity.RARE, 445),
        TitleItem(35, "عملاق الإرادة", "Titan of Will", TitleRarity.RARE, 455),

        // LEGENDARY (36..49)
        TitleItem(36, "أسطورة الصمود المطلق", "Legend of Pure Resilience", TitleRarity.LEGENDARY, 460),
        TitleItem(37, "قاهر بافوميت", "Baphomet Conqueror", TitleRarity.LEGENDARY, 465),
        TitleItem(38, "حامل مشعل النقاء", "Purity Torchbearer", TitleRarity.LEGENDARY, 470),
        TitleItem(39, "سيد النور المتوهج", "Incandescent Light Lord", TitleRarity.LEGENDARY, 475),
        TitleItem(40, "صاحب الجناح النيوني الأبدي", "Eternal Neon Wing Sovereign", TitleRarity.LEGENDARY, 480),
        TitleItem(41, "مهندس التوازن البيولوجي", "Biological Harmony Architect", TitleRarity.LEGENDARY, 485),
        TitleItem(42, "قاهر الهاوية الرقمية", "Digital Abyss Vanquisher", TitleRarity.LEGENDARY, 488),
        TitleItem(43, "حارس التاج الناري", "Flame Crown Sentinel", TitleRarity.LEGENDARY, 490),
        TitleItem(44, "الجوهر النقي الأسمى", "Supreme Pure Essence", TitleRarity.LEGENDARY, 492),
        TitleItem(45, "المصطفى من بين الألف", "Chosen of the Millennium", TitleRarity.LEGENDARY, 494),
        TitleItem(46, "سفير السماوات والأرض", "Ambassador of Heavens", TitleRarity.LEGENDARY, 496),
        TitleItem(47, "نجم الأفق الذي لا ينطفئ", "Unquenchable Horizon Star", TitleRarity.LEGENDARY, 497),
        TitleItem(48, "عرش الثبات الأبدي", "Throne of Eternal Steadfastness", TitleRarity.LEGENDARY, 498),
        TitleItem(49, "أسطورة الستريك الخالدة", "Immortal Streak Legend", TitleRarity.LEGENDARY, 499),

        // ULTIMATE / RANK 50 (STATION 500)
        TitleItem(50, "سيد النقوشة (Gary Master)", "Gary Master", TitleRarity.ULTIMATE, 500, description = "اللقب الأسمى والأعلى في التطبيق. يُفتح عند إتمام المحطة 500 واستخدام جرعة الانبعاث الأبدي.")
    )

    // 20 PROFILE AVATARS (5 COMMON, 5 UNCOMMON, 5 RARE, 5 LEGENDARY)
    val MASTER_AVATARS = listOf(
        // COMMON (5)
        AvatarItem("av_core_node", "العقدة المركزية", "Core Node", AvatarRarity.COMMON, 1, "hub"),
        AvatarItem("av_std_circuit", "الدائرة القياسية", "Standard Circuit", AvatarRarity.COMMON, 5, "memory"),
        AvatarItem("av_raw_carbon", "الكربون الخام", "Raw Carbon", AvatarRarity.COMMON, 15, "diamond"),
        AvatarItem("av_binary_ghost", "شبح الثنائي", "Binary Ghost", AvatarRarity.COMMON, 25, "code"),
        AvatarItem("av_iron_crescent", "الهلال الحديدي", "Iron Crescent", AvatarRarity.COMMON, 35, "shield"),

        // UNCOMMON (5)
        AvatarItem("av_strike_gaze", "نظرة السترايك", "Strike Gaze", AvatarRarity.UNCOMMON, 55, "visibility"),
        AvatarItem("av_dormant_helix", "اللولب الخامل", "Dormant Helix", AvatarRarity.UNCOMMON, 75, "dna"),
        AvatarItem("av_coolant_valve", "صمام التبريد", "Coolant Valve", AvatarRarity.UNCOMMON, 95, "ac_unit"),
        AvatarItem("av_shielded_node", "العقدة المحصنة", "Shielded Node", AvatarRarity.UNCOMMON, 120, "security"),
        AvatarItem("av_neon_pulse", "النبض النيوني", "Neon Pulse", AvatarRarity.UNCOMMON, 150, "bolt"),

        // RARE (5)
        AvatarItem("av_shadow_claw", "مخلب الظل السيبراني", "Cyber-Shadow Claw", AvatarRarity.RARE, 180, "pan_tool"),
        AvatarItem("av_bio_matrix", "مصفوفة التدفق الحيوي", "Bio-Flow Matrix", AvatarRarity.RARE, 220, "biotech"),
        AvatarItem("av_overclock_core", "النواة فائقة التردد", "Overclocked Core", AvatarRarity.RARE, 260, "speed"),
        AvatarItem("av_thor_spark", "شرارة ثور", "Thor’s Spark", AvatarRarity.RARE, 300, "electric_bolt"),
        AvatarItem("av_abyss_watcher", "راصد الهاوية", "Abyss Watcher", AvatarRarity.RARE, 340, "visibility"),

        // LEGENDARY (5)
        AvatarItem("av_gabriel_wings", "أجنحة جبريل المضيئة", "Gabriel’s Wings", AvatarRarity.LEGENDARY, 380, "flutter_dash"),
        AvatarItem("av_baphomet_throne", "عرش بافوميت", "Baphomet Throne", AvatarRarity.LEGENDARY, 420, "coronavirus"),
        AvatarItem("av_gary_crest", "شعار سيد النقوشة", "Gary Master Crest", AvatarRarity.LEGENDARY, 460, "military_tech"),
        AvatarItem("av_abs_genesis", "التكوين المطلق", "Absolute Genesis", AvatarRarity.LEGENDARY, 490, "all_inclusive"),
        AvatarItem("av_eternal_flame", "ذروة اللهب الأبدي", "Eternal Flame Apex", AvatarRarity.LEGENDARY, 500, "local_fire_department")
    )
}
