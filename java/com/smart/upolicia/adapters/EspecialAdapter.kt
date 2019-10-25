package com.smart.upolicia.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Json
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.smart.upolicia.AlarmasUIObserver
import com.smart.upolicia.EspecialesUIObserver
import com.smart.upolicia.R
import com.smart.upolicia.Utils.Utils
import com.smart.upolicia.data.model.User
import com.squareup.picasso.Picasso
import java.lang.Exception

class EspecialAdapter(private val UIObserver: EspecialesUIObserver, private val mContext: Context, private val list: List<JsonObject>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return EspecialHolder(UIObserver, LayoutInflater.from(mContext).inflate(R.layout.item_especial, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(genericHolder: RecyclerView.ViewHolder, position: Int) {
        val holder = genericHolder as EspecialHolder
        holder.fillFields(list[position], position, mContext)
    }

    override fun getItemId(position: Int): Long {
        try {
            return list[position].int("id_especial")!!.toLong()
        } catch (e:Exception) {
            e.printStackTrace()
        }
        return 0
    }
}

class EspecialHolder(val UIObserver: EspecialesUIObserver, val view: View): RecyclerView.ViewHolder(view) {

    private val textNombre: TextView = view.findViewById(R.id.textNombre)
    private val textVisto: TextView = view.findViewById(R.id.textVisto)
    private val textAlerta: TextView = view.findViewById(R.id.textAlerta)
    private val imgIcon: ImageView = view.findViewById(R.id.imgIcon)
    private val imgMap: ImageView = view.findViewById(R.id.imgMap)

    fun fillFields(especial: JsonObject, position: Int, mContext: Context){
        textNombre.text = "${especial.string("nombre1")} ${especial.string("apellido1")} ${especial.string("apellido2")}"
        textVisto.text = if (especial.array<JsonObject>("consultas")!!.size > 0) especial.array<JsonObject>("consultas")!![0].string("fecha") else "N/A"
        textAlerta.text = especial.obj("tipo_alerta")!!.string("nombre")
        if (especial.string("imagen")!!.isNotEmpty())
            Picasso.get().load(Utils.URL_MEDIA + especial.string("imagen")).error(R.drawable.men).placeholder(R.drawable.men).noFade().into(imgIcon)
        view.setOnClickListener{
            UIObserver.onElementClicked(especial)
        }
        imgMap.setOnClickListener{
            UIObserver.onMapClicked(especial)
        }
    }
}