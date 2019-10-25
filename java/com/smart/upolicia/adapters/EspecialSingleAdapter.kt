package com.smart.upolicia.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonObject
import com.smart.upolicia.EspecialSingleUIObserver
import com.smart.upolicia.R
import java.lang.Exception

class EspecialSingleAdapter(private val mContext: Context, private val list: List<JsonObject>, private val UIObserver: EspecialSingleUIObserver) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return EspecialSingleHolder(LayoutInflater.from(mContext).inflate(R.layout.item_especial_single, parent, false), UIObserver)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(genericHolder: RecyclerView.ViewHolder, position: Int) {
        val holder = genericHolder as EspecialSingleHolder
        holder.fillFields(list[position], position, list.size)
    }

    override fun getItemId(position: Int): Long {
        try {
            return list[position].int("id_consulta")!!.toLong()
        } catch (e:Exception) {
            e.printStackTrace()
        }
        return 0
    }
}

class EspecialSingleHolder(val view: View, val UIObserver: EspecialSingleUIObserver): RecyclerView.ViewHolder(view) {

    private val textVisto: TextView = view.findViewById(R.id.textVisto)
    private val imgMap: ImageView = view.findViewById(R.id.imgMap)
    private val separador: View = view.findViewById(R.id.separador)

    fun fillFields(record: JsonObject, position: Int, size: Int){
        textVisto.text = record.string("fecha")
        if (position == size - 1) separador.visibility = View.GONE
        imgMap.setOnClickListener{
            UIObserver.onMapClicked(record)
        }
    }
}