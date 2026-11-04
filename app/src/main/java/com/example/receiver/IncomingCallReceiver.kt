package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.util.CallNotificationHelper
import com.example.util.PhoneNumberMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver monitoring incoming phone calls.
 * Triggers a custom notification ("A striker is calling") when an incoming
 * call matches a phone number linked to an individual profile.
 */
class IncomingCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                Log.d(TAG, "Incoming call detected. State: $state, Number: $incomingNumber")

                if (!incomingNumber.isNullOrBlank()) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            val individualsWithPhone = db.individualDao().getIndividualsWithPhone()

                            val matchedStriker = individualsWithPhone.firstOrNull { individual ->
                                PhoneNumberMatcher.isMatch(individual.phoneNumber, incomingNumber)
                            }

                            if (matchedStriker != null) {
                                Log.i(TAG, "Striker match found! Notifying for: ${matchedStriker.fullName}")
                                CallNotificationHelper.showStrikerCallNotification(context, matchedStriker)
                            } else {
                                Log.d(TAG, "No striker matched for incoming call number.")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error looking up striker for incoming call", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "IncomingCallReceiver"
    }
}
