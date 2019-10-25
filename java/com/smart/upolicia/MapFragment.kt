package com.smart.upolicia

import android.Manifest
import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.smart.upolicia.Utils.InfoWindowAlarma
import com.smart.upolicia.Utils.InfoWindowUser
import com.smart.upolicia.Utils.NetworkUtils
import com.smart.upolicia.Utils.Utils
import kotlinx.android.synthetic.main.fragment_map.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class MapFragment: Fragment(), OnMapReadyCallback {

    private lateinit var prefs: SharedPreferences
    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLoc: LatLng
    private val DEFAULT_ZOOM = 13f
    private var alarmasArray = JsonArray<JsonObject>()

    private var mapFragment: SupportMapFragment? = null

    companion object {
        fun newInstance(): MapFragment {
            return MapFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_map, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
        currentLoc = LatLng(prefs.getString("latitud", "")!!.toDouble(), prefs.getString("longitud", "")!!.toDouble())
        activity!!.setTitle(R.string.alarma_label_title2)//.alarma_label_title)

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance()
        }
        childFragmentManager.beginTransaction().replace(R.id.map, mapFragment as Fragment).commit()
        mapFragment!!.getMapAsync(this)
    }

    override fun onStop() {
        super.onStop()
        activity!!.setTitle(R.string.app_name)
    }

    private fun getDeviceLocation() {
        try {
            val locationResult = mFusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener{
                try {
                    if (it.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        val mLastKnownLocation = it.result
                        prefs.edit().putString("latitud", mLastKnownLocation!!.latitude.toString()).apply()
                        prefs.edit().putString("longitud", mLastKnownLocation.longitude.toString()).apply()
                        currentLoc = LatLng(
                            mLastKnownLocation!!.latitude,
                            mLastKnownLocation.longitude
                        )
                        mMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(currentLoc, DEFAULT_ZOOM)
                        )
                        mMap.addMarker(MarkerOptions().position(LatLng(mLastKnownLocation.latitude, mLastKnownLocation.longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.policia)))
                        loadUsers()
                        reloadButton.setOnClickListener{
                            loadUsers()
                        }
                    } else {
                        Log.d("ERROR", "Current location is null. Using defaults.")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(activity, R.string.error_location, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //mMap.addMarker(MarkerOptions().position(currentLoc).icon(BitmapDescriptorFactory.fromResource(R.drawable.policia)))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLoc))

        Dexter.withActivity(activity)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object: PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity!!)
                    getDeviceLocation()
                }
                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    Toast.makeText(activity, R.string.error_permissions, Toast.LENGTH_SHORT).show()
                }
                override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                    token!!.continuePermissionRequest()
                }
            }).
                withErrorListener{ Toast.makeText(activity, R.string.error_permissions, Toast.LENGTH_SHORT).show()}
            .onSameThread()
            .check()
    }

    private fun setMarkersOnMap() {
        for (i in 0 until alarmasArray.size) {
            val marker = mMap.addMarker(
                MarkerOptions().position(
                    LatLng(alarmasArray[i].string("latitud")!!.toDouble(),
                        alarmasArray[i].string("longitud")!!.toDouble())
                ).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_user)))
            marker.tag = i
        }
        mMap.setInfoWindowAdapter(InfoWindowUser(this@MapFragment.context!!, JSONArray(alarmasArray.toJsonString())))
        /*mMap.setInfoWindowAdapter(InfoWindowAlarma(this@MapFragment.context!!, JSONArray(alarmasArray.toJsonString())))
        mMap.setOnInfoWindowClickListener {
            try {
                if (alarmasArray.size > (it.tag as Int)) {
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle(getString(R.string.app_name)).setMessage(R.string.alarma_message_aceptar)
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.si)) { dialog, _ ->
                            dialog.cancel()
                            val single = alarmasArray[it.tag as Int]
                            acceptAlarma(single)
                            Log.wtf("ALARMA", single.toJsonString())
                        }
                        .setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.cancel() }
                    val alert = builder.create()
                    alert.show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }*/
    }

    private fun loadUsers(){
        if (!NetworkUtils.isConnected(context!!)) {
            Toast.makeText(activity, R.string.error_internet2, Toast.LENGTH_LONG).show()
        } else {
            progressView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
            reloadButton.hide()
            val queue = Volley.newRequestQueue(activity)
            var URL = "${Utils.URL_SERVER}/policias/usuarios"
            val stringRequest = object : StringRequest(Method.POST, URL, Response.Listener<String> { response ->
                if (isAdded) {
                    try {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        reloadButton.show()
                        val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                        alarmasArray = json.array<JsonObject>("usuario_location")!!
                        setMarkersOnMap()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(activity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                    }
                }
            }, Response.ErrorListener { error ->
                if (isAdded) {
                    try {
                        error.printStackTrace()
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        reloadButton.show()
                        Toast.makeText(activity, JSONObject(String(error.networkResponse.data)).getString("message"), Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(activity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                    }
                }
            }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers.put("token", prefs.getString("api_key", "")!!)
                    return headers
                }
                override fun getParams(): MutableMap<String, String> {
                    val parameters = HashMap<String, String>()
                    parameters["latitud"] = currentLoc.latitude.toString()
                    parameters["longitud"] = currentLoc.longitude.toString()
                    return parameters
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }

    private fun loadAlarmas(){
        if (!NetworkUtils.isConnected(context!!)) {
            Toast.makeText(activity, R.string.error_internet2, Toast.LENGTH_LONG).show()
        } else {
            progressView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
            reloadButton.hide()
            val queue = Volley.newRequestQueue(activity)
            var URL = "${Utils.URL_SERVER}/alarmas/search"
            val stringRequest = object : StringRequest(Method.POST, URL, Response.Listener<String> { response ->
                if (isAdded) {
                    try {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        reloadButton.show()
                        val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                        alarmasArray = json.array<JsonObject>("alarmas")!!
                        if (alarmasArray.size > 0) setMarkersOnMap()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(activity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                    }
                }
            }, Response.ErrorListener { error ->
                if (isAdded) {
                try {
                    error.printStackTrace()
                    progressView.visibility = View.GONE
                    contentView.visibility = View.VISIBLE
                    reloadButton.show()
                    Toast.makeText(activity, JSONObject(String(error.networkResponse.data)).getString("message"), Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(activity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                }
                }
            }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers.put("token", prefs.getString("api_key", "")!!)
                    return headers
                }
                override fun getParams(): MutableMap<String, String> {
                    val parameters = HashMap<String, String>()
                    parameters["latitud"] = currentLoc.latitude.toString()
                    parameters["longitud"] = currentLoc.longitude.toString()
                    return parameters
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }

    private fun acceptAlarma(alarma: JsonObject) {
        if (!NetworkUtils.isConnected(context!!)) {
            Toast.makeText(activity, R.string.error_internet, Toast.LENGTH_LONG).show()
        } else {
            progressView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
            val queue = Volley.newRequestQueue(activity)
            var URL = "${Utils.URL_SERVER}/alarmas/${alarma.int("id_usuario_alarma")}/take"
            val stringRequest = object : StringRequest(Method.POST, URL, Response.Listener<String> { response ->
                if (isAdded) {
                    try {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        Log.wtf("RESPONSE", response)
                        val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                        Toast.makeText(activity!!.applicationContext, json.string("message"), Toast.LENGTH_LONG).show()
                        prefs.edit().putString("alarma", alarma.toJsonString()).apply()
                        val fragment = AlarmaMapFragment()
                        val bundle = Bundle()
                        bundle.putString("EXTRA1", alarma.toJsonString())
                        fragment.arguments = bundle
                        fragmentManager!!.beginTransaction().replace(R.id.frame_container, fragment).commit()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(activity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                    }
                }
            }, Response.ErrorListener { error ->
                if (isAdded) {
                    try {
                        error.printStackTrace()
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        Toast.makeText(activity, JSONObject(String(error.networkResponse.data)).getString("message"), Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(activity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                    }
                }
            }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers.put("token", prefs.getString("api_key", "")!!)
                    return headers
                }

                override fun getParams(): MutableMap<String, String> {
                    val parameters = HashMap<String, String>()
                    parameters["latitud"] = prefs.getString("latitud", "")!!
                    parameters["longitud"] = prefs.getString("longitud", "")!!
                    return parameters
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }
}