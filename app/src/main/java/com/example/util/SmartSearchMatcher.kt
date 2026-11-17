package com.example.util

import com.example.data.model.Individual
import java.util.Locale
import kotlin.math.min

enum class SmartMatchReason {
    EXACT_NAME,
    PREFIX_NAME,
    TOKEN_MATCH,
    CONTAINS_NAME,
    TYPE_MATCH,
    PHONE_MATCH,
    FUZZY_NAME,
    STAT_MATCH
}

data class SmartSearchResult(
    val individual: Individual,
    val score: Int,
    val reason: SmartMatchReason
)

/**
 * Instant, zero-allocation-friendly smart search engine supporting:
 * - Arabic character normalization (Alef variants, Ta Marbuta, Alef Maqsura, Hamza forms, Tashkeel/Tatweel removal)
 * - Exact, prefix, word-boundary, multi-token, phone-digit, and type-synonym matching
 * - Fuzzy typo tolerance (Damerau/Levenshtein bounded edit distance) & ordered initials/subsequence matching
 */
object SmartSearchMatcher {

    private val ARABIC_DIACRITICS_REGEX = Regex("[\\u064B-\\u065F\\u0670\\u0640]")
    private val MULTI_SPACE_REGEX = Regex("\\s+")

    fun normalizeText(input: String): String {
        if (input.isBlank()) return ""
        val stripped = ARABIC_DIACRITICS_REGEX.replace(input.trim(), "")
        val sb = StringBuilder(stripped.length)
        for (ch in stripped) {
            val mapped = when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                'ى' -> 'ي'
                'ة' -> 'ه'
                'ؤ' -> 'و'
                'ئ' -> 'ي'
                else -> ch.lowercaseChar()
            }
            sb.append(mapped)
        }
        return MULTI_SPACE_REGEX.replace(sb.toString().trim(), " ")
    }

    private fun digitsOnly(input: String): String = input.filter { it.isDigit() }

    private fun typeKeywords(type: String): List<String> = when (type.uppercase(Locale.US)) {
        "M" -> listOf("m", "male", "man", "ذكر", "رجل")
        "G" -> listOf("g", "girl", "woman", "female", "انثي", "بنت", "فتاه")
        "TS" -> listOf("ts", "trans", "mtf", "متحول", "ترانس")
        "FG" -> listOf("fg", "fem", "full girl", "كامله")
        else -> listOf(type.lowercase(Locale.US))
    }

    fun filterAndRank(
        individuals: List<Individual>,
        rawQuery: String
    ): List<SmartSearchResult> {
        val normQuery = normalizeText(rawQuery)
        if (normQuery.isEmpty()) {
            return individuals.map {
                SmartSearchResult(it, score = 0, reason = SmartMatchReason.EXACT_NAME)
            }
        }

        val queryTokens = normQuery.split(' ').filter { it.isNotEmpty() }
        val queryDigits = digitsOnly(rawQuery)

        return individuals.mapNotNull { ind ->
            scoreIndividual(ind, normQuery, queryTokens, queryDigits)
        }.sortedWith(
            compareByDescending<SmartSearchResult> { it.score }
                .thenByDescending { it.individual.strikeCount }
                .thenByDescending { it.individual.boost }
                .thenBy { it.individual.fullName.lowercase(Locale.US) }
        )
    }

    private fun scoreIndividual(
        ind: Individual,
        normQuery: String,
        queryTokens: List<String>,
        queryDigits: String
    ): SmartSearchResult? {
        val normName = normalizeText(ind.fullName)
        val nameWords = normName.split(' ').filter { it.isNotEmpty() }
        val normType = normalizeText(ind.type)
        val typeAliases = typeKeywords(ind.type)
        val phoneDigits = digitsOnly(ind.phoneNumber)

        // 1. Exact full name
        if (normName == normQuery) {
            return SmartSearchResult(ind, 1000, SmartMatchReason.EXACT_NAME)
        }

        // 2. Prefix of full name
        if (normName.startsWith(normQuery)) {
            return SmartSearchResult(ind, 880, SmartMatchReason.PREFIX_NAME)
        }

        // 3. Any word prefix match
        if (nameWords.any { it.startsWith(normQuery) }) {
            return SmartSearchResult(ind, 790, SmartMatchReason.TOKEN_MATCH)
        }

        // 4. Direct substring in full name
        if (normName.contains(normQuery)) {
            return SmartSearchResult(ind, 690, SmartMatchReason.CONTAINS_NAME)
        }

        // 5. Type exact or alias match
        if (normType == normQuery || typeAliases.any { it == normQuery || (normQuery.length >= 2 && it.startsWith(normQuery)) }) {
            return SmartSearchResult(ind, 640, SmartMatchReason.TYPE_MATCH)
        }

        // 6. Phone digits match (minimum 2 digits to avoid noisy single-digit hits unless name matches)
        if (queryDigits.length >= 2 && phoneDigits.contains(queryDigits)) {
            val bonus = if (phoneDigits.endsWith(queryDigits) || phoneDigits.startsWith(queryDigits)) 610 else 560
            return SmartSearchResult(ind, bonus, SmartMatchReason.PHONE_MATCH)
        }

        // 7. Multi-token match across name + type + phone + strikes
        if (queryTokens.size > 1) {
            val searchablePool = "$normName $normType ${typeAliases.joinToString(" ")} $phoneDigits ${ind.strikeCount}"
            val allTokensPresent = queryTokens.all { token ->
                searchablePool.contains(token) || nameWords.any { isWithinFuzzyThreshold(it, token) }
            }
            if (allTokensPresent) {
                return SmartSearchResult(ind, 540, SmartMatchReason.TOKEN_MATCH)
            }
        }

        // 8. Ordered initials / subsequence match (e.g. "tm" -> "Tariq Al-Mansoor", "fz" -> "Fatima Zahra")
        if (normQuery.length >= 2) {
            val initials = nameWords.mapNotNull { it.firstOrNull() }.joinToString("")
            if (initials.startsWith(normQuery) || initials.contains(normQuery)) {
                return SmartSearchResult(ind, 460, SmartMatchReason.FUZZY_NAME)
            }
            if (isSubsequence(normQuery.replace(" ", ""), normName.replace(" ", ""))) {
                return SmartSearchResult(ind, 390, SmartMatchReason.FUZZY_NAME)
            }
        }

        // 9. Bounded fuzzy typo tolerance on individual words or full name
        if (normQuery.length >= 3) {
            val wordFuzzy = nameWords.any { word -> isWithinFuzzyThreshold(word, normQuery) }
            val fullFuzzy = normName.length <= normQuery.length + 3 && isWithinFuzzyThreshold(normName, normQuery)
            if (wordFuzzy || fullFuzzy) {
                return SmartSearchResult(ind, 340, SmartMatchReason.FUZZY_NAME)
            }
        }

        // 10. Numeric metric match (strike count, boost int, size int)
        if (queryDigits.isNotEmpty() && queryDigits == normQuery) {
            val qInt = queryDigits.toIntOrNull()
            if (qInt != null && (ind.strikeCount == qInt || ind.boost.toInt() == qInt || ind.size.toInt() == qInt)) {
                return SmartSearchResult(ind, 260, SmartMatchReason.STAT_MATCH)
            }
        }

        return null
    }

    private fun isSubsequence(needle: String, haystack: String): Boolean {
        if (needle.isEmpty()) return true
        if (needle.length > haystack.length) return false
        var i = 0
        for (ch in haystack) {
            if (ch == needle[i]) {
                i++
                if (i == needle.length) return true
            }
        }
        return false
    }

    private fun isWithinFuzzyThreshold(word: String, query: String): Boolean {
        val maxDist = when {
            query.length <= 3 -> 1
            query.length <= 6 -> 1
            else -> 2
        }
        if (kotlin.math.abs(word.length - query.length) > maxDist) return false
        return levenshteinDistance(word, query) <= maxDist
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = min(
                    min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
                )
            }
            System.arraycopy(curr, 0, prev, 0, prev.size)
        }
        return prev[b.length]
    }
}
