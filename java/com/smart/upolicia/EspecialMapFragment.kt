package com.smart.upolicia

import android.Manifest
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.smart.upolicia.Utils.InfoWindowConsulta
import kotlinx.android.synthetic.main.fragment_map.*

class EspecialMapFragment: Fragment(), OnMapReadyCallback {

    private lateinit var prefs: SharedPreferences
    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private val DEFAULT_ZOOM = 13f

    private var mapFragment: SupportMapFragment? = null

    companion object {
        fun newInstance(): EspecialMapFragment {
            return EspecialMapFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_map, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
        activity!!.setTitle(R.string.especial_label_title_map)
        reloadButton.hide()

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
        }
        childFragmentManager.beginTransaction().replace(R.id.map, mapFragment as Fragment).commit()
        mapFragment!!.getMapAsync(this)
    }

    override fun onStop() {
        super.onStop()
        activity!!.setTitle(R.string.app_name)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        Dexter.withActivity(activity)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object: PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity!!)
                    val consultaLoc = LatLng(
                        arguments!!.getString("latitud").toDouble(),
                        arguments!!.getString("longitud").toDouble()
                    )
                    mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(consultaLoc, DEFAULT_ZOOM)
                    )
                    mMap.addMarker(
                        MarkerOptions().position(consultaLoc))
                    mMap.setInfoWindowAdapter(InfoWindowConsulta(this@EspecialMapFragment.context!!, arguments!!.getString("nombre"), arguments!!.getString("foto")))
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

    fun onBackPressed() {
        val fragment = if (arguments!!.getString("EXTRA1").isBlank()) EspecialesListaFragment() else EspecialFragment()
        val bundle = Bundle()
        bundle.putString("EXTRA1", arguments!!.getString("EXTRA1"))
        fragment.arguments = bundle
        fragmentManager!!.beginTransaction().replace(R.id.frame_container, fragment).commit()
    }
}