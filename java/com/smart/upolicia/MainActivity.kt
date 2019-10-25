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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.smart.hero.AlarmaFinalizarFragment
import com.smart.hero.ConsultaFragment
import com.smart.hero.ConsultaPreviaFragment
import com.smart.upolicia.Utils.*
import com.smart.upolicia.Utils.LocationRequestHelper
import kotlinx.android.synthetic.main.activity_main.*
import java.util.HashMap
import java.util.concurrent.TimeUnit

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
        displayView(if (intent.hasExtra("noombre")) 4 else 0)

        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener{ task ->
            if (!task.isSuccessful) {
                Log.wtf("ERROR", "getInstanceId failed", task.exception)
            }
            val token = task.result?.token
            sendRegistrationToServer(token)
        }
        FirebaseMessaging.getInstance().subscribeToTopic(getString(R.string.default_notification_channel_id))
    }

    override fun onConnected(@Nullable bundle: Bundle?) {
        Log.wtf(MainActivity::class.java.simpleName, "GoogleApiClient connected")
        if (startLocation) requestLocationUpdates()
    }

    fun requestLocationUpdates() {
        /*try {
            if (mGoogleApiClient.isConnected) {
                Log.i(MainActivity::class.java.simpleName, "Starting location updates")
                LocationRequestHelper.setRequesting(this, true)
                LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient,
                    mLocationRequest,
                    getPendingIntent()
                )
            } else startLocation = true

            val locationWorker = PeriodicWorkRequestBuilder<LocationWorker>( 15, TimeUnit.MINUTES).addTag("LOCATION_WORK_TAG").build()
            WorkManager.getInstance().enqueueUniquePeriodicWork( "LOCATION_WORK_TAG", ExistingPeriodicWorkPolicy.KEEP, locationWorker)
        } catch (e: SecurityException) {
            LocationRequestHelper.setRequesting(this, false)
            e.printStackTrace()
        }*/
    }

    fun removeLocationUpdates() {
        /*try {
            Log.i(MainActivity::class.java.simpleName, "Ending location updates")
            LocationRequestHelper.setRequesting(this, false);
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, getPendingIntent())
            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.cancelAll()

            WorkManager.getInstance().cancelAllWorkByTag("LOCATION_WORK_TAG")
        } catch (e: Exception) {
            e.printStackTrace()
        }*/
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
        createLocationRequest()
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
        if (!LocationUpdatesIntentService.isServiceRunning) requestLocationUpdates()
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
        menu_especiales.setOnClickListener{
            displayView(3)
        }
    }

    override fun onBackPressed() {
        val anonymousFragment = supportFragmentManager.findFragmentById(R.id.frame_container)
        if (anonymousFragment is AlarmaMapFragment) {
            displayView(2)
        } else if (anonymousFragment is AlarmaFinalizarFragment) {
            displayView(0)
        } else if (anonymousFragment is ConsultaFragment || anonymousFragment is ConsultaPreviaFragment) {
            displayView(1)
        } else if (anonymousFragment is EspecialFragment) {
            displayView(3)
        } else if (anonymousFragment is EspecialMapFragment) {
            (anonymousFragment as EspecialMapFragment).onBackPressed()
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
                removeLocationUpdates()
                prefs.edit().putString("usuario", "").apply()
                prefs.edit().putString("api_key", "").apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun displayView(position: Int) {
        menu_map.setColorFilter(resources.getColor(R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY)
        menu_detect.setColorFilter(resources.getColor(R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY)
        menu_lista.setColorFilter(resources.getColor(R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY)
        menu_especiales.setColorFilter(resources.getColor(R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY)
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
            3 -> {
                menu_especiales.setColorFilter(resources.getColor(R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                fragment = EspecialesListaFragment()
            }
            4 -> {
                menu_especiales.setColorFilter(resources.getColor(R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                fragment = EspecialMapFragment()
                bundl.putString("EXTRA1", "")
                bundl.putString("nombre", intent.getStringExtra("nombre"))
                bundl.putString("foto", intent.getStringExtra("imagen"))
                bundl.putString("longitud", intent.getStringExtra("longitud"))
                bundl.putString("latitud", intent.getStringExtra("latitud"))
                fragment.arguments = bundl
            }
        }
        if (fragment != null) {
            fragment!!.arguments = bundl
            val fragmentManager = supportFragmentManager
            fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit()
        } else Log.e("MainActivity", "Error in creating fragment")
    }

    private fun logout() {
        if (NetworkUtils.isConnected(this@MainActivity)) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
            val queue = Volley.newRequestQueue(this@MainActivity)
            var URL = "${Utils.URL_SERVER}/policias/logout"
            val stringRequest = object : StringRequest(Method.POST, URL, Response.Listener<String> { response ->
                try {
                    Log.wtf("respuesta", response)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, Response.ErrorListener { error ->
                error.printStackTrace()
            }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers.put("token", prefs.getString("api_key", "")!!)
                    return headers
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }

    private fun sendRegistrationToServer(token: String?) {
        Log.wtf("token", "test" + token)
        if (NetworkUtils.isConnected(applicationContext)) {
            val queue = Volley.newRequestQueue(applicationContext)
            val stringRequest = object : StringRequest(Method.POST, "${Utils.URL_SERVER}/policias/token",
                Response.Listener<String> { response ->
                    Log.i("SUCCESS", response)
                }, Response.ErrorListener { error ->
                    error.printStackTrace()
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers.put("token", prefs.getString("api_key", "")!!)
                    return headers
                }

                override fun getParams(): MutableMap<String, String> {
                    val parameters = HashMap<String, String>()
                    parameters["token"] = token!!
                    Log.i("token", token)
                    return parameters
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }
}