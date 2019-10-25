package com.smart.upolicia

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.smart.upolicia.EspecialMapFragment
import com.smart.upolicia.R
import com.smart.upolicia.Utils.Utils
import com.smart.upolicia.adapters.EspecialSingleAdapter
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_especial.*

class EspecialFragment: Fragment(), EspecialSingleUIObserver {

    lateinit var especial: JsonObject

    companion object {
        fun newInstance(): EspecialFragment {
            return EspecialFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_especial, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        especial = Parser.default().parse(StringBuilder(arguments!!.getString("EXTRA1"))) as JsonObject
        textNombre.text = "${especial.string("nombre1")} ${especial.string("apellido1")} ${especial.string("apellido2")}"
        textVisto.text = especial.array<JsonObject>("consultas")!![0].string("fecha")
        textPeligro.text = especial.obj("tipo_alerta")!!.string("nombre")
        cardView.setBackgroundColor(Color.parseColor("#" + especial.obj("tipo_alerta")!!.string("color")))
        if (especial.string("imagen")!!.isNotEmpty())
            Picasso.get().load(Utils.URL_MEDIA + especial.string("imagen")).error(R.drawable.men).placeholder(R.drawable.men).noFade().into(profilePicture)

        val singleAdapter = EspecialSingleAdapter(activity!!.applicationContext, especial.array<JsonObject>("consultas")!!, this)
        recyclerView.layoutManager = LinearLayoutManager(activity!!.applicationContext)
        recyclerView.adapter = singleAdapter
    }

    override fun onMapClicked(consulta: JsonObject) {
        val fragment = EspecialMapFragment()
        val bundle = Bundle()
        bundle.putString("EXTRA1", especial.toJsonString())
        bundle.putString("nombre", "${especial.string("nombre1")} ${especial.string("apellido1")} ${especial.string("apellido2")}")
        bundle.putString("foto", consulta.string("imagen"))
        bundle.putString("latitud", consulta.string("latitud"))
        bundle.putString("longitud", consulta.string("longitud"))
        fragment.arguments = bundle
        fragmentManager!!.beginTransaction().replace(R.id.frame_container, fragment).commit()
    }
}

interface EspecialSingleUIObserver{
    fun onMapClicked(temp: JsonObject)
}