package com.example.data.deviations

import androidx.compose.ui.graphics.Color

/**
 * Data structures and calculation engine for the "Deviations" feature:
 * - Dual-track Ranks (Dark Entity / Cyber-Shadow: 0-1000 pts vs Light Entity / Cyber-Angel: 1001-10000 pts)
 * - 50-Tier Biological / Energy Progression System (Estrogen vs Testosterone balance)
 */

data class DarkRank(
    val index: Int, // 1..20
    val title: String,
    val minScore: Double,
    val maxScore: Double,
    val gripProgress: Float // 0.0 (at 1000 pts) to 1.0 (at 0 pts)
)

data class LightRank(
    val index: Int, // 1..20
    val title: String,
    val minScore: Double,
    val maxScore: Double,
    val approachProgress: Float // 0.0 (at 1001 pts) to 1.0 (at 10000 pts)
)

data class BiologicalTier(
    val index: Int, // 1..50
    val title: String,
    val minScore: Double,
    val maxScore: Double,
    val testosteronePercent: Float, // 0f (0% Testosterone / 100% Estrogen) to 1f (100% Testosterone)
    val estrogenPercent: Float
)

object DeviationsManager {

    val DARK_RANKS = listOf(
        "المتغير",
        "المتذبذب",
        "المائل عن المسار",
        "أسير التردد",
        "المتآكل الرقمي",
        "مشوه الشيفرة",
        "عابر الظلال",
        "سجين الهاوية",
        "المنفي من النور",
        "سيد التصدعات",
        "شبح النظام",
        "صانع الفوضى",
        "المتلبس بالخطيئة",
        "مهندس الانهيار",
        "أمير التلاشي",
        "حارق الجذور",
        "سفير العدم",
        "كاهن اللعنة",
        "سيد الظلمات الأبدي",
        "بافوميت"
    )

    val LIGHT_RANKS = listOf(
        "التائب",
        "السالك في النور",
        "المتطهر",
        "الصاعد",
        "حارس العهد",
        "السائر في الضياء",
        "نقي السريرة",
        "العابد المخلص",
        "حارس الفجر",
        "المنير",
        "عابر السماوات",
        "الصديق",
        "المقرب",
        "حامل المشعل",
        "نجم الأفق",
        "صاحب الجناح الأبيض",
        "حارس الملكوت",
        "سفير النور",
        "نور السماوات والأرض",
        "جبريل"
    )

    val BIOLOGICAL_50_TIERS = listOf(
        "الأنثى المطلقة",
        "الطيف الناعم",
        "النسيم العليل",
        "الساكن في الظل",
        "العابر الهادئ",
        "قطرة الندى",
        "صدى الكلمات",
        "المتأرجح بخفة",
        "الخامل الوديع",
        "المستكين للراحة",
        "العابر الباهت",
        "الحالم الهامد",
        "التائه في السكينة",
        "النسمة الباردة",
        "المتردد الحائر",
        "السطح الساكن",
        "المتراجع برقة",
        "الظل المتلاشي",
        "الخافت النبرة",
        "نقطة التعادل",
        "المستيقظ حديثاً",
        "الساعي بثبات",
        "النابض بالحركة",
        "المقاوم للتيار",
        "المتأهب للنداء",
        "الثابت على العهد",
        "صلب العود",
        "الحارس اليقظ",
        "المحارب المبتدئ",
        "ذو العزم الحازم",
        "الصاقل لإرادته",
        "المندفع بقوة",
        "الفارس الميداني",
        "صانع الشرر",
        "الجلمود الصلب",
        "صقر الأفق",
        "الفاتح المنتصر",
        "الجبار المنتصب",
        "الصارم الحاد",
        "أسد الشراسيب",
        "الهزبر الهصور",
        "سيد الساحات",
        "القاهر الأبدي",
        "المارد العظيم",
        "الطود الشامخ",
        "الأسطورة الحية",
        "ملك الجبابرة",
        "صاحب القوة المطلقة",
        "العملاق الأبدي",
        "هرقل"
    )

    const val MAX_FLAME = 20000.0
    const val DARK_THRESHOLD = 2000.0

    /**
     * Dark Ranks (Score 0 to 2000):
     * As score drops from 2000 down to 0, rank ascends from 1 (المتغير) to 20 (بافوميت).
     * Grip progress goes from 0.0 at 2000 up to 1.0 at 0.
     */
    fun getDarkRank(score: Double): DarkRank {
        val clamped = score.coerceIn(0.0, DARK_THRESHOLD)
        // 2000 -> 0 rank index 0..19
        // At 2000: index = 0 ("المتغير")
        // At 0: index = 19 ("بافوميت")
        val fraction = (DARK_THRESHOLD - clamped) / DARK_THRESHOLD // 0.0 at 2000, 1.0 at 0
        val rawIndex = (fraction * 19.999).toInt().coerceIn(0, 19)
        val rankNumber = rawIndex + 1
        val step = DARK_THRESHOLD / 20.0
        val minSc = (DARK_THRESHOLD - (rawIndex + 1) * step).coerceAtLeast(0.0)
        val maxSc = (DARK_THRESHOLD - rawIndex * step).coerceAtMost(DARK_THRESHOLD)

        return DarkRank(
            index = rankNumber,
            title = DARK_RANKS[rawIndex],
            minScore = minSc,
            maxScore = maxSc,
            gripProgress = fraction.toFloat()
        )
    }

    /**
     * Light Ranks (Score 2001 to 20000):
     * As score rises from 2001 to 20000, rank ascends from 1 (التائب) to 20 (جبريل).
     * Approach progress goes from 0.0 at 2001 up to 1.0 at 20000 (transforming into pure white).
     */
    fun getLightRank(score: Double): LightRank {
        val clamped = score.coerceIn(2001.0, MAX_FLAME)
        val fraction = (clamped - 2001.0) / (MAX_FLAME - 2001.0) // 0.0 to 1.0
        val rawIndex = (fraction * 19.999).toInt().coerceIn(0, 19)
        val rankNumber = rawIndex + 1
        val step = (MAX_FLAME - 2001.0) / 20.0
        val minSc = 2001.0 + rawIndex * step
        val maxSc = 2001.0 + (rawIndex + 1) * step

        return LightRank(
            index = rankNumber,
            title = LIGHT_RANKS[rawIndex],
            minScore = minSc,
            maxScore = maxSc,
            approachProgress = fraction.toFloat()
        )
    }

    /**
     * Biological 50-Tier Progression System (Score 0 to 20000):
     * Tier 1: الأنثى المطلقة (0 pts, 100% Estrogen, 0% Testosterone)
     * Tier 20: نقطة التعادل (Score ~8000 pts, 50% Estrogen / 50% Testosterone equilibrium)
     * Tier 50: هرقل (20000 pts, 0% Estrogen, 100% Testosterone)
     */
    fun getBiologicalTier(score: Double): BiologicalTier {
        val clamped = score.coerceIn(0.0, MAX_FLAME)
        // 50 tiers: each tier spans 400 points
        val step = MAX_FLAME / 50.0 // 400.0 pts per tier
        val rawIndex = (clamped / step).toInt().coerceIn(0, 49)
        val tierNumber = rawIndex + 1
        val minSc = rawIndex * step
        val maxSc = (rawIndex + 1) * step

        val tPercent = (clamped / MAX_FLAME).toFloat().coerceIn(0f, 1f)
        val ePercent = 1f - tPercent

        return BiologicalTier(
            index = tierNumber,
            title = BIOLOGICAL_50_TIERS[rawIndex],
            minScore = minSc,
            maxScore = maxSc,
            testosteronePercent = tPercent,
            estrogenPercent = ePercent
        )
    }

    fun isDarkTrack(score: Double): Boolean = score <= DARK_THRESHOLD
}
