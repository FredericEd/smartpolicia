package com.smart.upolicia

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
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
import com.google.android.gms.maps.model.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.smart.hero.AlarmaFinalizarFragment
import com.smart.upolicia.Utils.NetworkUtils
import com.smart.upolicia.Utils.Utils
import kotlinx.android.synthetic.main.fragment_alarma_map.*
import kotlinx.android.synthetic.main.fragment_alarma_map.contentView
import kotlinx.android.synthetic.main.fragment_alarma_map.progressView
import kotlinx.android.synthetic.main.fragment_picker.*
import org.json.JSONObject
import java.util.*

class AlarmaMapFragment: Fragment(), OnMapReadyCallback {

    private lateinit var prefs: SharedPreferences
    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLoc: LatLng
    private val DEFAULT_ZOOM = 13f
    private lateinit var polyLine: Polyline
    private lateinit var alarma: JSONObject
    private var isCurrent = false

    private var mapFragment: SupportMapFragment? = null

    companion object {
        fun newInstance(): AlarmaMapFragment {
            return AlarmaMapFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_alarma_map, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
        currentLoc = LatLng(prefs.getString("latitud", "")!!.toDouble(), prefs.getString("longitud", "")!!.toDouble())

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance()
        }
        childFragmentManager.beginTransaction().replace(R.id.map, mapFragment as Fragment).commit()
        mapFragment!!.getMapAsync(this)
        btnActualizar.setOnClickListener {
            progressView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
            Handler().postDelayed({
                progressView.visibility = View.GONE
                contentView.visibility = View.VISIBLE
            }, 2000)
            currentLoc = LatLng(prefs.getString("latitud", "")!!.toDouble(), prefs.getString("longitud", "")!!.toDouble())
            setRouteBetweenMarkers()
        }
        btnSubmit.setOnClickListener {
            val fragment = AlarmaFinalizarFragment()
            val bundle = Bundle()
            bundle.putString("EXTRA1", alarma.toString())
            fragment.arguments = bundle
            fragmentManager!!.beginTransaction().replace(R.id.frame_container, fragment).commit()
        }

        alarma = JSONObject(arguments!!.getString("EXTRA1"))
        if (prefs.getString("alarma", "")!! != "") {
            val savedAlarma = JSONObject(prefs.getString("alarma", "")!!)
            if (savedAlarma.getString("id_usuario_alarma") == alarma.getString("id_usuario_alarma")) {
                isCurrent = true
                return
            }
        }
        textDetalle.visibility = View.VISIBLE
        textDetalle.text = alarma.getString("detalle")
        layFinalizar.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        if (isCurrent) (activity as MainActivity).requestLocationUpdates()
    }

    override fun onStop() {
        super.onStop()
        if (isCurrent) (activity as MainActivity).removeLocationUpdates()
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
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, DEFAULT_ZOOM))
                        setRouteBetweenMarkers()
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

    private fun setRouteBetweenMarkers(){
        if (::polyLine.isInitialized) {
            polyLine.remove()
        }
        var points: ArrayList<LatLng> = ArrayList()
        val lineOptions = PolylineOptions()
        points.add(LatLng(currentLoc.latitude, currentLoc.longitude))
        points.add(LatLng(alarma.getString("latitud").toDouble(), alarma.getString("longitud").toDouble()))
        mMap.addMarker(MarkerOptions().position(LatLng(
            currentLoc.latitude,
            currentLoc.longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.policia)))
        mMap.addMarker(MarkerOptions().position(LatLng(
            alarma.getString("latitud").toDouble(),
            alarma.getString("longitud").toDouble())).title(alarma.getJSONObject("usuario").getString("nombre1") + " " + alarma.getJSONObject("usuario").getString("apellido1")))
        lineOptions.addAll(points)
        lineOptions.width(10.toFloat())
        lineOptions.color(R.color.colorAccent)

        if(lineOptions != null) {
            polyLine = mMap.addPolyline(lineOptions)
        } else {
            Log.wtf("onPostExecute","without Polylines drawn")
        }
    }
}