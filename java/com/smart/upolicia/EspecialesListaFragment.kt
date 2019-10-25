package com.smart.upolicia

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.smart.upolicia.Utils.NetworkUtils
import com.smart.upolicia.Utils.Utils
import com.smart.upolicia.adapters.EspecialAdapter
import kotlinx.android.synthetic.main.fragment_alarmas.*
import org.json.JSONException

class EspecialesListaFragment: Fragment(), EspecialesUIObserver{

    private lateinit var especialAdapter: EspecialAdapter
    private lateinit var prefs: SharedPreferences
    private var especialesList: MutableList<JsonObject> = mutableListOf()

    companion object {
        fun newInstance(): AlarmasListaFragment {
            return AlarmasListaFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_alarmas, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(context)

        especialAdapter = EspecialAdapter(this, activity!!.applicationContext, especialesList!!)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = especialAdapter
        swipeRefreshLayout.setOnRefreshListener {
            if (!NetworkUtils.isConnected(context!!)) {
                Toast.makeText(context, R.string.error_internet, Toast.LENGTH_LONG).show()
                swipeRefreshLayout.isRefreshing = false
            } else resetAndLoad()
        }
        especialAdapter.notifyDataSetChanged()
        resetAndLoad()
    }

    override fun onStart() {
        super.onStart()
        activity!!.setTitle(R.string.especial_label_title_lista)
    }

    override fun onStop() {
        super.onStop()
        activity!!.setTitle(R.string.app_name)
    }

    private fun resetAndLoad(){
        especialesList.clear()
        progressView.visibility = View.VISIBLE
        contentView.visibility = View.GONE
        loadAlarmas()
    }

    override fun onElementClicked(temp: JsonObject) {
        val fragment = EspecialFragment()
        val bundle = Bundle()
        bundle.putString("EXTRA1", temp.toJsonString())
        fragment.arguments = bundle
        fragmentManager!!.beginTransaction().replace(R.id.frame_container, fragment).commit()
    }

    override fun onMapClicked(especial: JsonObject) {
        val fragment = EspecialMapFragment()
        val bundle = Bundle()
        bundle.putString("EXTRA1", "")
        bundle.putString("nombre", "${especial.string("nombre1")} ${especial.string("apellido1")} ${especial.string("apellido2")}")
        bundle.putString("foto", especial.array<JsonObject>("consultas")!![0].string("imagen"))
        bundle.putString("latitud", especial.array<JsonObject>("consultas")!![0].string("latitud"))
        bundle.putString("longitud", especial.array<JsonObject>("consultas")!![0].string("longitud"))
        fragment.arguments = bundle
        fragmentManager!!.beginTransaction().replace(R.id.frame_container, fragment).commit()
    }

    private fun loadAlarmas(){
        if (NetworkUtils.isConnected(context!!)) {
            val queue = Volley.newRequestQueue(context)
            val stringRequest = object : StringRequest(Method.GET, "${Utils.URL_SERVER}/policias/especiales",
                Response.Listener<String> { response ->
                    if (isAdded) {
                        try {
                            progressView.visibility = View.GONE
                            contentView.visibility = View.VISIBLE
                            swipeRefreshLayout.isRefreshing = false
                            val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                            val especiales = json.array<JsonObject>("especiales")
                            especiales!!.forEach{
                                especialesList.add(it)
                            }
                            especialAdapter.notifyDataSetChanged()
                            textEmpty.visibility = if (especiales.size > 0) View.GONE else View.VISIBLE
                        } catch (e: JSONException) {
                            Toast.makeText(this@EspecialesListaFragment.context, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                    }
                }, Response.ErrorListener { error ->
                    if (isAdded) {
                        try {
                            error.printStackTrace()
                            if (especialesList.size == 0) {
                                textEmpty.visibility = View.VISIBLE
                                progressView.visibility = View.GONE
                                contentView.visibility = View.VISIBLE
                                swipeRefreshLayout.isRefreshing = false
                                swipeRefreshLayout.visibility = View.GONE
                            } else {
                                recyclerView.recycledViewPool.clear()
                                especialAdapter.notifyDataSetChanged()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@EspecialesListaFragment.context, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = java.util.HashMap<String, String>()
                    headers.put("token", prefs.getString("api_key", "")!!)
                    return headers
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        } else {
            Toast.makeText(context, R.string.error_internet, Toast.LENGTH_LONG).show()
            progressView.visibility = View.GONE
            contentView.visibility = View.VISIBLE
            swipeRefreshLayout.isRefreshing = false
            swipeRefreshLayout.visibility = View.GONE
            textEmpty.visibility = View.VISIBLE
        }
    }
}

interface EspecialesUIObserver{
    fun onElementClicked(temp: JsonObject)
    fun onMapClicked(temp: JsonObject)
}