package com.example.util

import android.telephony.PhoneNumberUtils

object PhoneNumberMatcher {

    /**
     * Normalizes a phone string to digits only.
     */
    fun normalize(raw: String): String {
        return raw.filter { it.isDigit() }
    }

    /**
     * Checks if two phone numbers match.
     * Uses android.telephony.PhoneNumberUtils.compare as primary check,
     * with a robust digit suffix comparison (matching the last 7 to 9 digits)
     * as a fallback for varying international and local dialing prefixes.
     */
    fun isMatch(storedNumber: String?, incomingNumber: String?): Boolean {
        if (storedNumber.isNullOrBlank() || incomingNumber.isNullOrBlank()) {
            return false
        }

        val trimmedStored = storedNumber.trim()
        val trimmedIncoming = incomingNumber.trim()

        // 1. Android official telephony comparison
        try {
            if (PhoneNumberUtils.compare(trimmedStored, trimmedIncoming)) {
                return true
            }
        } catch (_: Exception) {
            // Ignore and fall back to digit comparison
        }

        // 2. Normalized digit comparison
        val normStored = normalize(trimmedStored)
        val normIncoming = normalize(trimmedIncoming)

        if (normStored.isEmpty() || normIncoming.isEmpty()) {
            return false
        }

        if (normStored == normIncoming) {
            return true
        }

        // 3. Match suffix for national vs international dial codes (e.g. 0555... vs +213555...)
        val minLen = minOf(normStored.length, normIncoming.length)
        val suffixLen = minOf(minLen, 8)
        if (suffixLen >= 6) {
            if (normStored.takeLast(suffixLen) == normIncoming.takeLast(suffixLen)) {
                return true
            }
        }

        return false
    }
}
