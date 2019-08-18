package com.smart.upolicia

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.view.Menu
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.smart.hero.ConsultaFragment
import com.smart.hero.ConsultaPreviaFragment
import com.smart.upolicia.Utils.LocationRequestHelper
import com.smart.upolicia.Utils.LocationUpdatesBroadcastReceiver
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private lateinit var prefs: SharedPreferences
    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var mLocationRequest: LocationRequest

    private val UPDATE_INTERVAL = (10 * 1000).toLong()
    private val FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2
    private val MAX_WAIT_TIME = UPDATE_INTERVAL * 3
    private var startLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        setBottomBar()
        buildGoogleApiClient()
        displayView(0)
    }

    override fun onConnected(@Nullable bundle: Bundle?) {
        Log.wtf(MainActivity::class.java.simpleName, "GoogleApiClient connected")
        if (startLocation) requestLocationUpdates()
    }

    fun requestLocationUpdates() {
        try {
            if (mGoogleApiClient.isConnected) {
                Log.i(MainActivity::class.java.simpleName, "Starting location updates")
                LocationRequestHelper.setRequesting(this, true)
                LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient,
                    mLocationRequest,
                    getPendingIntent()
                )
            } else startLocation = true
        } catch (e: SecurityException) {
            LocationRequestHelper.setRequesting(this, false)
            e.printStackTrace()
        }
    }

    fun removeLocationUpdates() {
        try {
            Log.i(MainActivity::class.java.simpleName, "Ending location updates")
            LocationRequestHelper.setRequesting(this, false);
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, getPendingIntent())
            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.cancelAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onConnectionSuspended(i: Int) {
        Log.wtf(MainActivity::class.java.simpleName, "Connection suspended")
    }

    override fun onConnectionFailed(@NonNull connectionResult: ConnectionResult) {
        Log.wtf(MainActivity::class.java.simpleName, "Exception while connecting to Google Play services")
    }

    private fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = UPDATE_INTERVAL

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        mLocationRequest.maxWaitTime = MAX_WAIT_TIME
    }

    private fun getPendingIntent(): PendingIntent {
        val intent: Intent = Intent(this@MainActivity, LocationUpdatesBroadcastReceiver::class.java)
        intent.setAction(LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES)
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun setBottomBar() {
        menu_map.setOnClickListener{
            displayView(0)
        }
        menu_detect.setOnClickListener{
            displayView(1)
        }
        menu_lista.setOnClickListener{
            displayView(2)
        }
    }

    override fun onBackPressed() {
        val anonymousFragment = supportFragmentManager.findFragmentById(R.id.frame_container)
        if (anonymousFragment is AlarmaMapFragment) {
            val fragment = AlarmasListaFragment()
            val fragmentManager = supportFragmentManager
            fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit()
        } else if (anonymousFragment is ConsultaFragment || anonymousFragment is ConsultaPreviaFragment) {
            val fragment = PickerFragment()
            val fragmentManager = supportFragmentManager
            fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_logout -> {
                prefs.edit().putString("usuario", "").apply()
                prefs.edit().putString("api_key", "").apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun displayView(position: Int) {
        menu_map.setColorFilter(resources.getColor(R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY)
        menu_detect.setColorFilter(resources.getColor(R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY)
        menu_lista.setColorFilter(resources.getColor(R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY)
        var fragment: Fragment? = null
        val bundl = Bundle()
        when (position) {
            0 -> {
                menu_map.setColorFilter(resources.getColor(R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                val alarma = prefs.getString("alarma", "")!!
                if (alarma != "") {
                    fragment = AlarmaMapFragment()
                    bundl.putString("EXTRA1", alarma)
                    fragment.arguments = bundl
                } else fragment = MapFragment()
            }
            1 -> {
                menu_detect.setColorFilter(resources.getColor(R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                fragment = PickerFragment()
            }
            2 -> {
                menu_lista.setColorFilter(resources.getColor(R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                fragment = AlarmasListaFragment()
            }
        }
        if (fragment != null) {
            fragment!!.arguments = bundl
            val fragmentManager = supportFragmentManager
            fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit()
        } else Log.e("MainActivity", "Error in creating fragment")
    }
}