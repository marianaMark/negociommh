package com.jerf.tienda

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import android.text.SpannableString
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.graphics.Color

class Clientes : AppCompatActivity() {

    data class Cliente(val id: String, val nombre: String, val apellido: String, val numero: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.clientes)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellido = findViewById<EditText>(R.id.etApellido)
        val etNumero = findViewById<EditText>(R.id.etNumero)
        val btnInsertar = findViewById<Button>(R.id.btnInsertar)
        val lvClientes = findViewById<ListView>(R.id.lvClientes)

        val queue = Volley.newRequestQueue(this)
        val clientes = mutableListOf<Cliente>()

        // Recuperar IP guardada en SharedPreferences
        val prefs = getSharedPreferences("config", MODE_PRIVATE)
        val ip = prefs.getString("ip_servidor", "") ?: ""
        if (ip.isEmpty()) {
            Toast.makeText(this, "No se ha configurado la IP. Ve a la pantalla principal.", Toast.LENGTH_LONG).show()
            return
        }

        val urlBase = "http://$ip/practica/listarclientesjson.php"

        fun listarClientes() {
            val stringRequest = StringRequest(
                Request.Method.GET, urlBase,
                { response ->
                    clientes.clear()
                    val jsonArray = JSONArray(response)
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        clientes.add(
                            Cliente(
                                id = item.optString("id"),
                                nombre = item.optString("nombre"),
                                apellido = item.optString("apellido"),
                                numero = item.optString("numero")
                            )
                        )
                    }

                    val adapter = object : ArrayAdapter<Cliente>(this, R.layout.item_cliente, R.id.tvCliente, clientes) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getView(position, convertView, parent)
                            val cliente = getItem(position)!!
                            val tvCliente = view.findViewById<TextView>(R.id.tvCliente)
                            val btnBorrar = view.findViewById<Button>(R.id.btnBorrar)

                            val text = "ID: ${cliente.id}\nNombre: ${cliente.nombre}\nApellido: ${cliente.apellido}\nNumero: ${cliente.numero}"
                            val spannable = SpannableString(text)
                            val columnas = listOf("ID:", "Nombre:", "Apellido:", "Numero:")
                            columnas.forEach { col ->
                                val start = text.indexOf(col)
                                if (start >= 0) {
                                    spannable.setSpan(ForegroundColorSpan(Color.RED), start, start + col.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                }
                            }
                            tvCliente.text = spannable

                            btnBorrar.setOnClickListener {
                                val deleteRequest = object : StringRequest(
                                    Method.POST, urlBase,
                                    { response ->
                                        Toast.makeText(this@Clientes, "Cliente eliminado", Toast.LENGTH_SHORT).show()
                                        listarClientes()
                                    },
                                    { error ->
                                        Toast.makeText(this@Clientes, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    override fun getParams(): MutableMap<String, String> {
                                        return hashMapOf("id" to cliente.id)
                                    }
                                }
                                queue.add(deleteRequest)
                            }

                            return view
                        }
                    }

                    lvClientes.adapter = adapter
                },
                { error ->
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
            queue.add(stringRequest)
        }


        btnInsertar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val numero = etNumero.text.toString().trim()

            if (nombre.isEmpty() || apellido.isEmpty() || numero.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val insertRequest = object : StringRequest(
                Method.POST, urlBase,
                { response ->
                    Toast.makeText(this, "Cliente insertado", Toast.LENGTH_SHORT).show()
                    etNombre.text.clear()
                    etApellido.text.clear()
                    etNumero.text.clear()
                    listarClientes()
                },
                { error ->
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            ) {
                override fun getParams(): MutableMap<String, String> {
                    return hashMapOf(
                        "nombre" to nombre,
                        "apellido" to apellido,
                        "numero" to numero
                    )
                }
            }
            queue.add(insertRequest)
        }


        listarClientes()
    }
}
