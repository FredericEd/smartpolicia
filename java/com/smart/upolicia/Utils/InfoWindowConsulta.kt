package com.smart.upolicia.Utils

import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.beust.klaxon.JsonObject
import com.smart.upolicia.R
import com.squareup.picasso.Picasso
import org.json.JSONArray
import java.lang.Exception

class InfoWindowConsulta: GoogleMap.InfoWindowAdapter {

    private var context: Context
    private var nombre: String
    private var foto: String
    private var loaded = false

    constructor(ctx: Context, nombre: String, foto: String) {
        context = ctx
        this.nombre = nombre
        this.foto = foto
    }
    override fun getInfoContents(p0: Marker?): View {
        val view = (context as Activity).layoutInflater .inflate(R.layout.item_info_consulta, null)
        val textNombre = view.findViewById<TextView>(R.id.textNombre)
        textNombre.text = nombre
        return view
    }

    override fun getInfoWindow(p0: Marker?): View {
        val view = (context as Activity).layoutInflater .inflate(R.layout.item_info_consulta, null)
        val textNombre = view.findViewById<TextView>(R.id.textNombre)
        val imgIcon = view.findViewById<ImageView>(R.id.imgIcon)

        textNombre.text = nombre
        if (!foto.isNullOrBlank())
            Picasso.get().load(Utils.URL_MEDIA + foto).error(R.drawable.men).placeholder(R.drawable.men).noFade().into(imgIcon, object: com.squareup.picasso.Callback {
                override fun onSuccess() {
                    if (!loaded) {
                        loaded = true
                        p0!!.hideInfoWindow()
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