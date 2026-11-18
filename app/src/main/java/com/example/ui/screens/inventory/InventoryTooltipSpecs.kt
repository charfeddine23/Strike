package com.example.ui.screens.inventory

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.profile.PotionType
import com.example.data.profile.ProfileMasterInventory
import com.example.data.profile.SkipKeyType
import com.example.data.profile.TitleItem
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald

data class InventoryTooltipSpec(
    val title: String,
    val subtitle: String,
    val rarityLabel: String,
    val accentColor: Color,
    val secondaryColor: Color,
    val icon: ImageVector,
    val mechanics: String,
    val bonuses: String,
    val expirationRules: String,
    val howToObtain: String
)

fun locText(lang: String, ar: String, fr: String, en: String): String = when (lang) {
    "ar" -> ar
    "fr" -> fr
    else -> en
}

object InventoryTooltipFactory {

    fun for7DayAutoShield(lang: String): InventoryTooltipSpec = InventoryTooltipSpec(
        title = locText(lang, "درع الـ 7 أيام التلقائي", "Bouclier Auto 7 Jours", "7-Day Automated Strike Shield"),
        subtitle = locText(lang, "تفعيل تلقائي نخبوي", "Défense Auto Élite", "Luxury Auto Defense"),
        rarityLabel = locText(lang, "فئة نخبوية", "NIVEAU ÉLITE", "LUXURY TIER"),
        accentColor = Color(0xFFFFD700),
        secondaryColor = Color(0xFFFF007A),
        icon = Icons.Default.VerifiedUser,
        mechanics = locText(
            lang,
            "درع وقائي أوتوماتيكي نخبوي يعمل كصمام أمان عالي المستوى عند التعرض لضربات مكثفة في يوم واحد.",
            "Bouclier protecteur automatique d'élite agissant comme sécurité lors de strikes intenses sur une seule journée.",
            "An elite automatic protective shield that acts as a high-tier safeguard during intense single-day strikes."
        ),
        bonuses = locText(
            lang,
            "يوقف إضافة +3 نقاط خمول يومية لمدة 7 أيام كاملة (168 ساعة) بمجرد تفعيله.",
            "Interrompt la pénalité d'inactivité de +3 pts/jour pendant 7 jours complets (168 heures).",
            "Halts the +3 daily inactivity accrual for 7 full days (168 hours) once activated."
        ),
        expirationRules = locText(
            lang,
            "تبدأ مدة الـ 7 أيام تلقائياً لحظة تفعيل الكومبو وتنتهي بعد انقضاء 168 ساعة، ولا يمكن تخزينه يدوياً.",
            "La durée de 7 jours démarre automatiquement au déclenchement du combo et expire après 168 h.",
            "The 7-day timer starts immediately upon combo trigger and expires after 168 hours. Cannot be stored manually."
        ),
        howToObtain = locText(
            lang,
            "يُمنح ويُفعّل تلقائياً فور تسجيل 3 إلى 5 سترايكات في نفس اليوم الحالي (Real-Time Today).",
            "Obtenu et activé automatiquement en enregistrant 3 à 5 strikes le jour même en temps réel.",
            "Granted & activated automatically upon recording 3 to 5 strikes on the same live day."
        )
    )

    fun for24hShield(lang: String): InventoryTooltipSpec = InventoryTooltipSpec(
        title = locText(lang, "درع الـ 24 ساعة", "Bouclier Protecteur 24h", "24-Hour Protective Shield"),
        subtitle = locText(lang, "حماية تكتيكية ليوم واحد", "Barrière Tactique 1 Jour", "1-Day Tactical Barrier"),
        rarityLabel = locText(lang, "شائع", "COMMUN", "COMMON"),
        accentColor = NeonCyan,
        secondaryColor = NeonEmerald,
        icon = Icons.Default.Shield,
        mechanics = locText(
            lang,
            "درع تكتيكي يجمّد عداد الخمول اليومي مؤقتاً لحماية رصيد الشعلة.",
            "Barrière tactique qui gèle temporairement la pénalité d'inactivité quotidienne.",
            "A tactical barrier that temporarily freezes daily inactivity accrual to protect your Flame score."
        ),
        bonuses = locText(
            lang,
            "يمنع إضافة +3 نقاط خمول يومية لمدة 24 ساعة متواصلة من وقت التفعيل.",
            "Empêche +3 points d'inactivité quotidiens pendant 24 heures continues.",
            "Prevents +3 daily inactivity points for 24 continuous hours upon activation."
        ),
        expirationRules = locText(
            lang,
            "ينتهي مفعوله بعد 24 ساعة من التفعيل. إذا كان هناك درع نشط، تُضاف الـ 24 ساعة إلى وقت الانتهاء.",
            "Expire 24 h après activation. Si un bouclier est déjà actif, les 24 h s'ajoutent à la durée restante.",
            "Expires 24 hours after activation. If a shield is already active, adds 24 hours to its remaining duration."
        ),
        howToObtain = locText(
            lang,
            "يُكتسب كمكافأة من محطات خارطة الطريق (المحطات 1، 11، 21... ضمن دورة المكافآت العشرية).",
            "Obtenu en récompense des stations de la Feuille de route (stations 1, 11, 21...).",
            "Earned as a Roadmap station reward (Stations 1, 11, 21... in the 10-station reward cycle)."
        )
    )

    fun for48hShield(lang: String): InventoryTooltipSpec = InventoryTooltipSpec(
        title = locText(lang, "درع الـ 48 ساعة", "Bouclier Protecteur 48h", "48-Hour Protective Shield"),
        subtitle = locText(lang, "حاجز سيبراني متوسط المدى", "Barrière Cyber Moyen Terme", "Mid-Range Cyber Barrier"),
        rarityLabel = locText(lang, "غير شائع", "PEU COMMUN", "UNCOMMON"),
        accentColor = NeonEmerald,
        secondaryColor = NeonCyan,
        icon = Icons.Default.Shield,
        mechanics = locText(
            lang,
            "حاجز سيبراني متوسط المدى يحمي استقرار الشعلة عبر يومين متتاليين.",
            "Barrière cyber protégeant la stabilité de la Flamme pendant deux jours consécutifs.",
            "A mid-range cyber barrier safeguarding Flame stability across two consecutive days."
        ),
        bonuses = locText(
            lang,
            "يوقف تزايد نقاط الخمول (+3 نقاط/يوم) لمدة 48 ساعة متواصلة.",
            "Stoppe la hausse d'inactivité (+3 pts/jour) pendant 48 heures continues.",
            "Halts inactivity growth (+3 pts/day) for 48 consecutive hours."
        ),
        expirationRules = locText(
            lang,
            "صلاحية الحماية تمتد لـ 48 ساعة متصلة؛ وفي حال وجود درع نشط مسبقاً تتراكم المدة تلقائياً.",
            "Protection de 48 heures consécutives, cumulable avec un bouclier déjà actif.",
            "Protection lasts 48 consecutive hours and stacks onto any currently active shield."
        ),
        howToObtain = locText(
            lang,
            "يُكتسب كمكافأة من محطات خارطة الطريق (المحطات 4، 14، 24... ضمن دورة المكافآت العشرية).",
            "Obtenu en récompense des stations de la Feuille de route (stations 4, 14, 24...).",
            "Earned as a Roadmap station reward (Stations 4, 14, 24... in the 10-station reward cycle)."
        )
    )

    fun for72hShield(lang: String): InventoryTooltipSpec = InventoryTooltipSpec(
        title = locText(lang, "درع الـ 72 ساعة", "Bouclier Protecteur 72h", "72-Hour Protective Shield"),
        subtitle = locText(lang, "درع حماية عالي التحمل", "Armure Haute Résistance", "Heavy-Duty Armor"),
        rarityLabel = locText(lang, "نادر", "RARE", "RARE"),
        accentColor = NeonAmber,
        secondaryColor = Color(0xFFEA580C),
        icon = Icons.Default.VerifiedUser,
        mechanics = locText(
            lang,
            "درع حماية عالي التحمل يصمد ثلاثة أيام كاملة ضد تراكم نقاط الخمول.",
            "Bouclier haute résistance bloquant l'accumulation d'inactivité pendant 3 jours complets.",
            "Heavy-duty barrier that blocks daily inactivity buildup for 3 full days."
        ),
        bonuses = locText(
            lang,
            "يجمّد نقاط الخمول اليومية (+3 نقاط/يوم) لمدة 3 أيام كاملة (72 ساعة).",
            "Gèle les points d'inactivité (+3 pts/jour) pendant 3 jours complets (72 h).",
            "Freezes daily inactivity points (+3 pts/day) for 3 full days (72 hours)."
        ),
        expirationRules = locText(
            lang,
            "يُستهلك الدرع عند التفعيل اليدوي أو الفوري ويمتد مفعوله 72 ساعة كاملة.",
            "Consommé lors de l'activation et protège pendant 72 heures complètes.",
            "Consumed on activation and extends protection for 72 full hours."
        ),
        howToObtain = locText(
            lang,
            "يُكتسب من محطات خارطة الطريق (7، 17، 27...) أو يُمنح تلقائياً كتعويض عند تفعيل عقوبة الضربة المزدوجة في اليوم الحالي.",
            "Obtenu aux stations (7, 17, 27...) ou accordé en compensation lors d'une pénalité Double Strike.",
            "Earned from Roadmap stations (7, 17, 27...) or granted automatically upon a same-day Double Strike penalty."
        )
    )

    fun forSkipKey(type: SkipKeyType, lang: String): InventoryTooltipSpec = when (type) {
        SkipKeyType.WOODEN -> InventoryTooltipSpec(
            title = type.getLocalizedName(lang),
            subtitle = locText(lang, "مفتاح تجاوز قياسي", "Clé Standard", "Standard Bypass Key"),
            rarityLabel = locText(lang, "شائع", "COMMUN", "COMMON"),
            accentColor = Color(0xFFD97706),
            secondaryColor = Color(0xFFB45309),
            icon = Icons.Default.VpnKey,
            mechanics = locText(
                lang,
                "مفتاح فك شيفرة قياسي لتجاوز المهام اليومية والأسبوعية البسيطة وحصد نقاط الخبرة فوراً.",
                "Clé standard permettant de passer des missions quotidiennes ou hebdomadaires et d'obtenir l'XP immédiatement.",
                "Standard decryption key to bypass eligible tasks and claim XP immediately."
            ),
            bonuses = locText(
                lang,
                "يمنح استخداماً واحداً لإكمال مهمة متوافقة فوراً مع الحصول على كامل مكافأة الخبرة.",
                "Offre 1 utilisation pour valider une mission compatible et recevoir son XP complète.",
                "Provides 1 use to complete a compatible task immediately with full XP bounty."
            ),
            expirationRules = locText(
                lang,
                "لا تنتهي صلاحيته بالتخزين (الحد الأقصى 30 مفتاحاً). ممنوع استخدامه في المهام السنوية.",
                "N'expire jamais en stockage (max 30 clés). Interdit sur les missions annuelles.",
                "Never expires in storage (Max 30 keys). Strictly prohibited on Annual tasks."
            ),
            howToObtain = locText(
                lang,
                "يُمنح من محطات خارطة الطريق (المحطات 2، 12، 22، 32... ضمن دورة المكافآت).",
                "Obtenu aux stations de la Feuille de route (2, 12, 22, 32...).",
                "Earned from Roadmap stations (Stations 2, 12, 22, 32... in the reward cycle)."
            )
        )
        SkipKeyType.BRONZE -> InventoryTooltipSpec(
            title = type.getLocalizedName(lang),
            subtitle = locText(lang, "مفتاح تجاوز مطوّر", "Clé Améliorée", "Upgraded Bypass Key"),
            rarityLabel = locText(lang, "غير شائع", "PEU COMMUN", "UNCOMMON"),
            accentColor = Color(0xFFF59E0B),
            secondaryColor = Color(0xFFEA580C),
            icon = Icons.Default.VpnKey,
            mechanics = locText(
                lang,
                "مفتاح مطوّر يسمح بتخطي المهام ذات الصعوبة المتوسطة ضمن القوائم اليومية والأسبوعية والشهرية.",
                "Clé améliorée capable de passer des missions moyennes (Quotidiennes, Hebdo, Mensuelles).",
                "Upgraded key capable of bypassing medium-difficulty tasks in Daily, Weekly, and Monthly pools."
            ),
            bonuses = locText(
                lang,
                "يملك قدرة استخدامين لتمرير المهام المؤهلة وحصد نقاط الخبرة دون تأخير.",
                "Dispose de 2 utilisations pour valider les missions éligibles sans attendre.",
                "Provides 2 uses to clear eligible tasks and harvest XP without delay."
            ),
            expirationRules = locText(
                lang,
                "دائم في المخزون حتى استهلاكه (السعة القصوى 30 مفتاحاً). لا يشمل المهام السنوية.",
                "Permanent jusqu'à épuisement (max 30 clés). Exclut les missions annuelles.",
                "Permanent in inventory until consumed (Max 30 keys). Cannot skip Annual tasks."
            ),
            howToObtain = locText(
                lang,
                "يُمنح من محطات خارطة الطريق (المحطات 5، 15، 25، 35... ضمن دورة المكافآت).",
                "Obtenu aux stations de la Feuille de route (5, 15, 25, 35...).",
                "Earned from Roadmap stations (Stations 5, 15, 25, 35... in the reward cycle)."
            )
        )
        SkipKeyType.SILVER -> InventoryTooltipSpec(
            title = type.getLocalizedName(lang),
            subtitle = locText(lang, "مفتاح اختراق رفيع المستوى", "Clé Haute Portée", "High-Tier Breach Key"),
            rarityLabel = locText(lang, "نادر", "RARE", "RARE"),
            accentColor = Color(0xFFE2E8F0),
            secondaryColor = NeonCyan,
            icon = Icons.Default.VpnKey,
            mechanics = locText(
                lang,
                "مفتاح اختراق رفيع المستوى قادر على تجاوز المهام عالية المتطلبات في القوائم الدورية.",
                "Clé haut niveau capable de passer des missions périodiques exigeantes.",
                "High-tier breach key capable of bypassing high-requirement recurring tasks."
            ),
            bonuses = locText(
                lang,
                "يمتلك 3 استخدامات مع أقصى كفاءة لحصد مكافآت الخبرة العالية فوراً.",
                "Possède 3 utilisations pour récolter rapidement de grosses récompenses d'XP.",
                "Carries 3 uses for maximum efficiency in harvesting high XP bounties."
            ),
            expirationRules = locText(
                lang,
                "يبقى محفوظاً حتى استنفاد شحناته (الحد الأقصى 30 مفتاحاً). يُستثنى من المهام السنوية.",
                "Conservé jusqu'à épuisement des charges (max 30 clés). Exclut les missions annuelles.",
                "Retained until charges are exhausted (Max 30 keys). Excludes Annual tasks."
            ),
            howToObtain = locText(
                lang,
                "يُمنح من محطات خارطة الطريق (8، 18، 28...) وبكمية مضاعفة (×2) عند كل محطة عشرينية (20، 40، 60...).",
                "Obtenu aux stations (8, 18, 28...) et en double (×2) à chaque multiple de 20 (20, 40, 60...).",
                "Earned at Stations (8, 18, 28...) and granted 2× at every 20th Milestone Station (20, 40, 60...)."
            )
        )
    }

    fun forPotion(type: PotionType, lang: String): InventoryTooltipSpec = when (type) {
        PotionType.PULSE_ELIXIR -> InventoryTooltipSpec(
            title = type.getLocalizedName(lang),
            subtitle = locText(lang, "الفئة الأولى", "Niveau 1", "Tier 1 Enhancer"),
            rarityLabel = locText(lang, "شائع", "COMMUN", "COMMON"),
            accentColor = type.color,
            secondaryColor = NeonEmerald,
            icon = Icons.Default.Bolt,
            mechanics = locText(
                lang,
                "منشّط نبضي يضاعف فاعلية زر التبريد (Deflame) لخفض طاقة الشعلة بسرعة أكبر.",
                "Stimulant amplifiant l'efficacité du bouton Deflame pour réduire la Flamme plus vite.",
                "Pulse booster that amplifies the Deflame cooldown button for faster Flame reduction."
            ),
            bonuses = locText(
                lang,
                "يضيف +5 نقاط خصم إضافية لكل ضغطة على زر التبريد خلال التفعيلات الثلاث القادمة.",
                "Ajoute +5 points de déduction bonus par utilisation de Deflame pour les 3 prochains clics.",
                "Adds +5 bonus deduction points per Deflame press for the next 3 activations."
            ),
            expirationRules = locText(
                lang,
                "ينتهي تأثيره تلقائياً بمجرد استهلاك التفعيلات الثلاث المتتالية (السعة القصوى 3 جرعات).",
                "Expire automatiquement après 3 utilisations consécutives (max 3 flacons en stock).",
                "Automatically expires once the 3 consecutive Deflame uses are consumed (Max 3 stored)."
            ),
            howToObtain = locText(
                lang,
                "يُكتسب من محطات خارطة الطريق (المحطات 3، 13، 23، 33... ضمن دورة المكافآت).",
                "Obtenu aux stations de la Feuille de route (3, 13, 23, 33...).",
                "Earned from Roadmap Station rewards (Stations 3, 13, 23, 33...)."
            )
        )
        PotionType.SHADOW_GRIP_SERUM -> InventoryTooltipSpec(
            title = type.getLocalizedName(lang),
            subtitle = locText(lang, "الفئة الثانية", "Niveau 2", "Tier 2 Stabilizer"),
            rarityLabel = locText(lang, "غير شائع", "PEU COMMUN", "UNCOMMON"),
            accentColor = type.color,
            secondaryColor = Color(0xFF7E22CE),
            icon = Icons.Default.Science,
            mechanics = locText(
                lang,
                "مصل كابح للانجراف الحراري يقوم بتهدئة تسارع نقاط الخمول اليومي في النظام.",
                "Sérum stabilisateur réduisant l'accélération quotidienne de la Flamme due à l'inactivité.",
                "Thermal-drift suppressor that calms daily inactivity point acceleration."
            ),
            bonuses = locText(
                lang,
                "يقلّل وتيرة صعود نقاط الشعلة الناتجة عن الخمول اليومي بنسبة 50% لمدة 3 أيام متتالية.",
                "Réduit de 50 % la montée quotidienne de la Flamme pendant 3 jours consécutifs.",
                "Reduces Flame daily inactivity climb rate by 50% for 3 consecutive days."
            ),
            expirationRules = locText(
                lang,
                "يبدأ العد التنازلي لمدة 72 ساعة لحظة التفعيل، ويعود المعدل لطبيعته بعد الانتهاء.",
                "Compte à rebours de 72 heures dès l'injection, puis retour au taux normal.",
                "72-hour countdown begins immediately on activation and reverts automatically upon expiry."
            ),
            howToObtain = locText(
                lang,
                "يُكتسب من محطات خارطة الطريق (المحطات 6، 16، 26، 36... ضمن دورة المكافآت).",
                "Obtenu aux stations de la Feuille de route (6, 16, 26, 36...).",
                "Earned from Roadmap Station rewards (Stations 6, 16, 26, 36...)."
            )
        )
        PotionType.GABRIEL_PURITY_DROPLET -> InventoryTooltipSpec(
            title = type.getLocalizedName(lang),
            subtitle = locText(lang, "الفئة الثالثة", "Niveau 3", "Tier 3 Catalyst"),
            rarityLabel = locText(lang, "نادر", "RARE", "RARE"),
            accentColor = type.color,
            secondaryColor = NeonAmber,
            icon = Icons.Default.WaterDrop,
            mechanics = locText(
                lang,
                "قطرة نقاء نورانية تتفاعل تلقائياً مع النظام فور استلامها من مكافآت المسار.",
                "Goutte de pureté réagissant automatiquement dès sa réception dans la Feuille de route.",
                "Radiant purity droplet that reacts automatically upon being claimed from Roadmap rewards."
            ),
            bonuses = locText(
                lang,
                "تُسرّع وتيرة نمو نقاط الشعلة بنسبة +50% خلال احتساب الخمول لدفع التقدم نحو المسار الملائكي.",
                "Accélère la croissance de la Flamme de +50 % pendant 7 jours vers le rang Angélique.",
                "Accelerates Flame growth by +50% during inactivity calculation toward the Angelic path."
            ),
            expirationRules = locText(
                lang,
                "تُفعّل تلقائياً وفورياً عند الجني دون تخزين وتستمر فعاليتها لمدة 7 أيام كاملة (168 ساعة).",
                "S'active immédiatement à l'obtention pendant 7 jours complets (168 h).",
                "Auto-activates immediately on receipt for 7 full days (168 hours)."
            ),
            howToObtain = locText(
                lang,
                "تُكتسب من محطات خارطة الطريق (المحطات 9، 19، 29، 39... ضمن دورة المكافآت).",
                "Obtenue aux stations de la Feuille de route (9, 19, 29, 39...).",
                "Earned from Roadmap Station rewards (Stations 9, 19, 29, 39...)."
            )
        )
        PotionType.ABSOLUTE_CRYO_INFUSION -> InventoryTooltipSpec(
            title = type.getLocalizedName(lang),
            subtitle = locText(lang, "الفئة الرابعة", "Niveau 4", "Tier 4 Cryogenic"),
            rarityLabel = locText(lang, "ملحمي", "ÉPIQUE", "EPIC"),
            accentColor = type.color,
            secondaryColor = Color(0xFF0284C7),
            icon = Icons.Default.AcUnit,
            mechanics = locText(
                lang,
                "حقنة تبريد مطلقة تسلّح الستريك القادم بطاقة تعزيز استثنائية.",
                "Infusion cryogénique armant votre prochain strike d'un bonus de puissance massif.",
                "Cryogenic infusion that arms your upcoming strike with exceptional boost power."
            ),
            bonuses = locText(
                lang,
                "تمنح +100 نقطة مكافأة لتعزيز قوة وتأثير الستريك القادم.",
                "Accorde +100 points bonus pour amplifier votre prochain strike.",
                "Grants +100 bonus points to amplify your next strike."
            ),
            expirationRules = locText(
                lang,
                "تُفعّل يدوياً من المخزون؛ وتُستهلك بضربة واحدة فور تنفيذ الستريك التالي.",
                "Activation manuelle depuis l'inventaire ; consommée lors du prochain strike.",
                "Manual activation from Inventory; consumed automatically on your next recorded strike."
            ),
            howToObtain = locText(
                lang,
                "تُكتسب من محطات خارطة الطريق العشرية الفردية (المحطات 10، 30، 50، 70...).",
                "Obtenue aux stations impaires multiples de 10 (10, 30, 50, 70...).",
                "Earned from odd-tenth Roadmap stations (Stations 10, 30, 50, 70...)."
            )
        )
        PotionType.ETERNAL_REBIRTH_ELIXIR -> InventoryTooltipSpec(
            title = type.getLocalizedName(lang),
            subtitle = locText(lang, "الفئة الخامسة الأسطورية", "Niveau 5 Légendaire", "Legendary Tier 5"),
            rarityLabel = locText(lang, "أسطوري مطلق", "LÉGENDAIRE ULTIME", "ULTIMATE LEGENDARY"),
            accentColor = Color(0xFFFF007A),
            secondaryColor = Color(0xFFFFD700),
            icon = Icons.Default.AutoAwesome,
            mechanics = locText(
                lang,
                "أندر وأقوى جوهرة كيميائية في النظام؛ تمنح حصانة دائمة ضد التصفير.",
                "L'élixir le plus rare et puissant du système, offrant une immunité permanente.",
                "The rarest pinnacle potion in StrikePulse granting eternal systemic protection."
            ),
            bonuses = locText(
                lang,
                "تمنح درع مناعة أبدي ضد التصفير الكامل للنظام، وتفتح فوراً اللقب الأسمى «سيد النقوشة».",
                "Confère une immunité éternelle contre les réinitialisations et débloque le titre Gary Master.",
                "Grants eternal immunity against system flame resets and unlocks the Gary Master title."
            ),
            expirationRules = locText(
                lang,
                "أبدية المفعول؛ بمجرد تفعيلها تظل حصانتها ومجدها مفعّلين للأبد.",
                "Effet éternel et permanent une fois activé.",
                "Permanent & infinite duration once activated."
            ),
            howToObtain = locText(
                lang,
                "تُمنح حصرياً كجائزة كبرى عند الوصول إلى المحطة النهائية #500 في خارطة الطريق والمطالبة بمكافأتها.",
                "Attribuée exclusivement comme grand prix de la station finale #500 de la Feuille de route.",
                "Awarded exclusively as the Grand Prize upon reaching & claiming Station #500 on the Roadmap."
            )
        )
    }

    fun forTitle(title: TitleItem, lang: String, isUnlocked: Boolean): InventoryTooltipSpec {
        val rarityLabel = when (lang) {
            "ar" -> title.rarity.labelAr
            else -> title.rarity.labelEn
        }
        val howObtain = when {
            title.id == ProfileMasterInventory.MILESTONE_PEAK_TITLE.id -> locText(
                lang,
                "يُفتح حصرياً عند الوصول برصيد الشعلة (Flame Score) إلى 20,000 نقطة فأكثر.",
                "Débloqué exclusivement en atteignant un score Flamme de 20 000 points ou plus.",
                "Unlocked exclusively upon reaching a Flame Score of 20,000+ points."
            )
            title.id == 50 -> locText(
                lang,
                "يُفتح عند بلوغ المحطة #500 في خارطة الطريق أو عند تفعيل «جرعة الانبعاث الأبدي».",
                "Débloqué à la station #500 ou en activant la Potion de Résurgence Éternelle.",
                "Unlocked upon reaching Station #500 on the Roadmap or activating the Eternal Resurgence Potion."
            )
            title.unlockStation != null && title.unlockStation <= 1 -> locText(
                lang,
                "لقب ابتدائي متاح ومفتوح تلقائياً لجميع اللاعبين منذ البداية (المحطة #1).",
                "Titre de départ automatiquement débloqué pour tous dès la Station #1.",
                "Starter title automatically unlocked for all players from the beginning (Station #1)."
            )
            title.unlockStation != null -> locText(
                lang,
                "يُفتح تلقائياً عند جمع نقاط الخبرة (XP) والمطالبة بمكافأة المحطة #${title.unlockStation} في خارطة الطريق.",
                "Débloqué automatiquement en atteignant et réclamant la Station #${title.unlockStation} sur la Feuille de route.",
                "Unlocked automatically upon earning enough XP and claiming Station #${title.unlockStation} on the Roadmap."
            )
            else -> locText(
                lang,
                "يُفتح عبر تحقيق إنجازات النظام الخاصة في المسار أو الشعلة.",
                "Débloqué via les accomplissements spéciaux du système.",
                "Unlocked through special Flame or Roadmap milestones."
            )
        }

        val defaultDesc = title.getLocalizedDescription(lang).ifBlank {
            locText(
                lang,
                "لقب شرفي من فئة «${title.rarity.labelAr}» يعكس رتبتك وهيبتك السيبرانية في البطاقة الشخصية.",
                "Titre honorifique de rareté «${title.rarity.labelEn}» reflétant votre prestige sur votre profil.",
                "A ${title.rarity.labelEn}-tier prestige title displayed on your cybernetic profile card and halo."
            )
        }

        return InventoryTooltipSpec(
            title = title.getLocalizedTitle(lang),
            subtitle = locText(lang, "لقب الملف الشخصي #${title.id}", "Titre de Profil #${title.id}", "Profile Title #${title.id}"),
            rarityLabel = rarityLabel,
            accentColor = title.rarity.primaryColor,
            secondaryColor = title.rarity.secondaryColor,
            icon = Icons.Default.MilitaryTech,
            mechanics = defaultDesc,
            bonuses = locText(
                lang,
                "يُزيّن ملفك الشخصي وبطاقة اللقب بهالة لونية ديناميكية تتناسب مع ندرته (${title.rarity.labelAr}).",
                "Orne votre profil d'un halo lumineux dynamique correspondant à sa rareté (${title.rarity.labelEn}).",
                "Customizes your profile identity and header halo with ${title.rarity.labelEn} dynamic visual glow."
            ),
            expirationRules = if (isUnlocked) {
                locText(
                    lang,
                    "متوفر في مخزونك للأبد ويمكنك تفعيله أو التبديل بينه وبين الألقاب المفتوحة في أي وقت.",
                    "Disponible de façon permanente ; vous pouvez l'équiper ou changer de titre à tout moment.",
                    "Permanently unlocked in your account; you can activate or swap between unlocked titles anytime."
                )
            } else {
                locText(
                    lang,
                    "مقفل حالياً؛ بمجرد استيفاء شرط الفتح سيصبح متاحاً للتفعيل الدائم.",
                    "Actuellement verrouillé ; il restera activable en permanence dès son déblocage.",
                    "Currently locked; once unlocked, it remains permanently available to equip."
                )
            },
            howToObtain = howObtain
        )
    }
}
