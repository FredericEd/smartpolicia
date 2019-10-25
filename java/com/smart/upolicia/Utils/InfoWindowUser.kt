package com.smart.upolicia.Utils

import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.smart.upolicia.R
import com.squareup.picasso.Picasso
import org.json.JSONArray
import java.lang.Exception

class InfoWindowUser: GoogleMap.InfoWindowAdapter {

    private var context: Context
    private var data: JSONArray

    constructor(ctx: Context, array: JSONArray) {
        context = ctx
        data = array
    }
    override fun getInfoContents(p0: Marker?): View {
        if ( p0!!.tag.toString() == "null") return View(context)
        val view = (context as Activity).layoutInflater .inflate(R.layout.item_info_alarma, null)
        val textNombre = view.findViewById<TextView>(R.id.textNombre)
        val textCedula = view.findViewById<TextView>(R.id.textCedula)
        val btnResponser = view.findViewById<TextView>(R.id.btnResponser)
        btnResponser.visibility = View.GONE

        val usuario = data.getJSONObject(p0!!.tag as Int).getJSONObject("usuario")
        textNombre.text = "${usuario.getString("nombre1")}  ${usuario.getString("apellido1")}"
        textCedula.text = usuario.getString("cedula")
        return view
    }

    override fun getInfoWindow(p0: Marker?): View {
        if ( p0!!.tag.toString() == "null") return View(context)
        val view = (context as Activity).layoutInflater .inflate(R.layout.item_info_alarma, null)
        val textNombre = view.findViewById<TextView>(R.id.textNombre)
        val textCedula = view.findViewById<TextView>(R.id.textCedula)
        val imgIcon = view.findViewById<ImageView>(R.id.imgIcon)
        val btnResponser = view.findViewById<TextView>(R.id.btnResponser)
        btnResponser.visibility = View.GONE

        val usuario = data.getJSONObject(p0!!.tag as Int).getJSONObject("usuario")
        textNombre.text = "${usuario.getString("nombre1")}  ${usuario.getString("apellido1")}"
        textCedula.text = usuario.getString("cedula")
        if (!usuario.getString("imagen").isNullOrBlank())
            Picasso.get().load(Utils.URL_MEDIA + usuario.getString("imagen")).error(R.drawable.men).placeholder(R.drawable.men).noFade().into(imgIcon, object: com.squareup.picasso.Callback {
                override fun onSuccess() {
                    if (!usuario.has("loaded")) {
                        usuario.put("loaded", "1")
                        p0.hideInfoWindow()
                        p0.showInfoWindow()
                    }
                }
                override fun onError(e: Exception?) {
                    // Nothing to do here
                }
            })
        return view
    }
}