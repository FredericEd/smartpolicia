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
import com.smart.upolicia.adapters.AlarmaAdapter
import kotlinx.android.synthetic.main.fragment_alarmas.*
import org.json.JSONException

class AlarmasListaFragment: Fragment(), AlarmasUIObserver{

    private lateinit var bitacoraAdapter: AlarmaAdapter
    private lateinit var prefs: SharedPreferences
    private var bitacorasList: MutableList<JsonObject> = mutableListOf()

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

        bitacoraAdapter = AlarmaAdapter(this, activity!!.applicationContext, bitacorasList!!)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = bitacoraAdapter
        swipeRefreshLayout.setOnRefreshListener {
            if (!NetworkUtils.isConnected(context!!)) {
                Toast.makeText(context, R.string.error_internet, Toast.LENGTH_LONG).show()
                swipeRefreshLayout.isRefreshing = false
            } else resetAndLoad()
        }
        bitacoraAdapter.notifyDataSetChanged()
        resetAndLoad()
    }

    override fun onStart() {
        super.onStart()
        activity!!.setTitle(R.string.alarma_label_title_lista)
    }

    override fun onStop() {
        super.onStop()
        activity!!.setTitle(R.string.app_name)
    }

    private fun resetAndLoad(){
        bitacorasList.clear()
        progressView.visibility = View.VISIBLE
        contentView.visibility = View.GONE
        loadAlarmas()
    }

    override fun onElementClicked(temp: JsonObject) {
        val fragment = AlarmaMapFragment()
        val bundle = Bundle()
        bundle.putString("EXTRA1", temp.toJsonString())
        fragment.arguments = bundle
        fragmentManager!!.beginTransaction().replace(R.id.frame_container, fragment).commit()
    }

    private fun loadAlarmas(){
        if (NetworkUtils.isConnected(context!!)) {
            val queue = Volley.newRequestQueue(context)
            val stringRequest = object : StringRequest(Method.GET, "${Utils.URL_SERVER}/policias/alarmas",
                Response.Listener<String> { response ->
                    if (isAdded) {
                        try {
                            progressView.visibility = View.GONE
                            contentView.visibility = View.VISIBLE
                            swipeRefreshLayout.isRefreshing = false
                            val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                            val bitacoras = json.array<JsonObject>("alarmas")
                            bitacoras!!.forEach{
                                bitacorasList.add(it)
                            }
                            bitacoraAdapter.notifyDataSetChanged()
                            textEmpty.visibility = if (bitacoras.size > 0) View.GONE else View.VISIBLE
                        } catch (e: JSONException) {
                            Toast.makeText(this@AlarmasListaFragment.context, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                    }
                }, Response.ErrorListener { error ->
                    if (isAdded) {
                        try {
                            error.printStackTrace()
                            if (bitacorasList.size == 0) {
                                textEmpty.visibility = View.VISIBLE
                                progressView.visibility = View.GONE
                                contentView.visibility = View.VISIBLE
                                swipeRefreshLayout.isRefreshing = false
                                swipeRefreshLayout.visibility = View.GONE
                            } else {
                                recyclerView.recycledViewPool.clear()
                                bitacoraAdapter.notifyDataSetChanged()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@AlarmasListaFragment.context, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
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

interface AlarmasUIObserver{
    fun onElementClicked(temp: JsonObject)
}