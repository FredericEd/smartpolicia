package com.smart.hero

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
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.smart.upolicia.R
import com.smart.upolicia.Utils.NetworkUtils
import com.smart.upolicia.Utils.Utils
import com.smart.upolicia.data.model.User
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_alarma_finalizar.*
import org.json.JSONObject
import java.util.HashMap

class AlarmaFinalizarFragment: Fragment() {

    private lateinit var prefs: SharedPreferences
    private lateinit var alarma: JsonObject

    companion object {
        fun newInstance(): AlarmaFinalizarFragment {
            return AlarmaFinalizarFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_alarma_finalizar, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)

        alarma = Parser.default().parse(StringBuilder(arguments!!.getString("EXTRA1"))) as JsonObject
        val usuario = Klaxon().parseFromJsonObject<User>(alarma.obj("usuario")!!)!!
        textNombre.text = "${usuario.nombre1} ${usuario.apellido1}"
        textCedula.text = usuario.cedula
        textCorreo.text = usuario.correo
        textPhone.text = usuario.telefono
        textNacimiento.text = usuario.fecha_nacimiento
        if (usuario.imagen.isNotEmpty())
            Picasso.get().load(Utils.URL_MEDIA + usuario.imagen).error(R.drawable.men).placeholder(R.drawable.men).noFade().into(profilePicture)
        btnFinalizar.setOnClickListener{
            attemptFinalizar()
        }
    }

    fun attemptFinalizar() {
        // Reset errors.
        editDetalles.error = null
        val detalles = editDetalles.text.toString()

        var cancel = false
        var focusView: View? = null
        if (detalles.isEmpty()) {
            editDetalles.error = getString(R.string.error_field_required)
            focusView = editDetalles
            cancel = true
        }
        if (!cancel) {

            val builder = AlertDialog.Builder(context)
            builder.setTitle(getString(R.string.app_name)).setMessage(R.string.alarma_message_finalizar)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.si)) { dialog, _ ->
                    dialog.cancel()
                    finalizarAlarma(alarma.int("id_usuario_alarma")!!.toString(), detalles)
                }
                .setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.cancel() }
            val alert = builder.create()
            alert.show()
        } else focusView!!.requestFocus()
    }

    private fun finalizarAlarma(id_alarma: String, detalle: String) {
        if (!NetworkUtils.isConnected(activity!!.applicationContext)) {
            Toast.makeText(activity, R.string.error_internet2, Toast.LENGTH_LONG).show()
        } else {
            progressView.visibility = View.VISIBLE
            contentView.visibility = View.GONE
            val queue = Volley.newRequestQueue(activity)
            val URL = "${Utils.URL_SERVER}/alarmas/$id_alarma/end"
            Log.wtf("URL", URL)
            val stringRequest = object : StringRequest(Method.POST, URL, Response.Listener<String> { response ->
                if (isAdded) {
                    try {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        Log.wtf("response", response)
                        val json: JsonObject = Parser.default().parse(StringBuilder(response)) as JsonObject
                        Toast.makeText(activity, json.string("message"), Toast.LENGTH_LONG).show()
                        prefs.edit().putString("alarma", "").apply()
                        activity!!.onBackPressed()
                    } catch (e: Exception) {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        e.printStackTrace()
                        Toast.makeText(activity, resources.getString(R.string.error_general), Toast.LENGTH_LONG).show()
                    }
                }
            }, Response.ErrorListener { error ->
                if (isAdded) {
                    try {
                        progressView.visibility = View.GONE
                        contentView.visibility = View.VISIBLE
                        error.printStackTrace()
                        Toast.makeText(
                            activity,
                            JSONObject(String(error.networkResponse.data)).getString("message"),
                            Toast.LENGTH_LONG
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
                    parameters["detalle"] = detalle
                    return parameters
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(180000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(stringRequest)
        }
    }
}