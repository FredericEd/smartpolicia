package com.smart.upolicia.Utils

import com.google.android.gms.location.LocationResult
import android.content.Intent
import android.app.IntentService
import android.util.Log
import com.smart.upolicia.Utils.LocationResultHelper

class LocationUpdatesIntentService : IntentService(TAG) {

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            isServiceRunning = true
            val action = intent.action
            if (ACTION_PROCESS_UPDATES == action) {
                val result = LocationResult.extractResult(intent)
                if (result != null) {
                    val locations = result.locations
                    val locationResultHelper = LocationResultHelper(
                        this,
                        locations
                    )
                    // Save the location data to SharedPreferences.
                    locationResultHelper.saveResults()
                    // Show notification with the location data.
                    locationResultHelper.showNotification()
                    Log.i(TAG, LocationResultHelper.getSavedLocationResult(this))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
    }

    companion object {
        var isServiceRunning = false
        internal val ACTION_PROCESS_UPDATES =
            "com.smart.hero.Utils.action" + ".PROCESS_UPDATES"
        private val TAG = LocationUpdatesIntentService::class.java.simpleName
    }
}// Name the worker thread.