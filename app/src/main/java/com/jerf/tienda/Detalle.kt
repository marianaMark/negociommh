package com.jerf.tienda

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray

class Detalle : AppCompatActivity() {

    data class Producto(val nombre: String, val cantidad: Int, val subtotal: Double)
    data class Venta(
        val id: String,
        val cliente: String,
        val fecha: String,
        val direccion: String,
        val total: Double,
        val productos: List<Producto>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detalle)

        val llVentas = findViewById<LinearLayout>(R.id.llVentas)
        val queue = Volley.newRequestQueue(this)

        val prefs = getSharedPreferences("config", MODE_PRIVATE)
        val ip = prefs.getString("ip_servidor", "") ?: ""
        if (ip.isEmpty()) {
            Toast.makeText(this, "No se ha configurado la IP.", Toast.LENGTH_LONG).show()
            return
        }

        val url = "http://$ip/practica/listarventasjson.php"

        val request = StringRequest(url,
            { response ->
                llVentas.removeAllViews()
                val array = JSONArray(response)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)

                    // Productos
                    val productosJson = item.getJSONArray("productos")
                    val productos = mutableListOf<Producto>()
                    for (j in 0 until productosJson.length()) {
                        val p = productosJson.getJSONObject(j)
                        productos.add(
                            Producto(
                                p.getString("nombre"),
                                p.getInt("cantidad"),
                                p.getDouble("subtotal")
                            )
                        )
                    }

                    // Cliente: Nombre + Apellido
                    val nombreCliente = item.getString("nombre_cliente")
                    val apellidoCliente = item.getString("apellido_cliente")
                    val cliente = "$nombreCliente $apellidoCliente"

                    val venta = Venta(
                        id = item.getString("id"),
                        cliente = cliente,
                        fecha = item.getString("fecha"),
                        direccion = item.getString("direccion"),
                        total = item.getDouble("total"),
                        productos = productos
                    )

                    // Tarjeta de venta
                    val tarjeta = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(24, 24, 24, 24)
                        setBackgroundColor(Color.WHITE)
                        elevation = 8f
                        val params = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(0, 0, 0, 16)
                        layoutParams = params
                        background = resources.getDrawable(R.drawable.card_background, null)
                    }

                    // Función para combinar título rojo y valor negro
                    fun formatLine(titulo: String, valor: String): SpannableString {
                        val texto = "$titulo $valor"
                        val spannable = SpannableString(texto)
                        spannable.setSpan(
                            ForegroundColorSpan(Color.RED),
                            0, titulo.length + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        return spannable
                    }

                    // Info general: Venta ID, Cliente, Dirección
                    val tvId = TextView(this).apply {
                        text = formatLine("Venta ID:", venta.id)
                        setTypeface(null, Typeface.BOLD)
                        textSize = 16f
                    }
                    val tvCliente = TextView(this).apply {
                        text = formatLine("Cliente:", venta.cliente)
                        setTypeface(null, Typeface.BOLD)
                        textSize = 16f
                    }
                    val tvDireccion = TextView(this).apply {
                        text = formatLine("Dirección:", venta.direccion)
                        setTypeface(null, Typeface.BOLD)
                        textSize = 16f
                    }

                    tarjeta.addView(tvId)
                    tarjeta.addView(tvCliente)
                    tarjeta.addView(tvDireccion)

                    // Productos
                    venta.productos.forEach { p ->
                        val tvProd = TextView(this).apply {
                            text = "• ${p.nombre} x${p.cantidad} = ${p.subtotal}"
                            setPadding(16, 4, 0, 4)
                            setTextColor(Color.BLACK)
                        }
                        tarjeta.addView(tvProd)
                    }

                    // Total
                    val tvTotal = TextView(this).apply {
                        text = formatLine("Total:", venta.total.toString())
                        setTypeface(null, Typeface.BOLD)
                        textSize = 16f
                        setPadding(0, 8, 0, 0)
                    }
                    tarjeta.addView(tvTotal)

                    // Fecha al final
                    val tvFecha = TextView(this).apply {
                        text = formatLine("Fecha:", venta.fecha)
                        setTypeface(null, Typeface.BOLD)
                        textSize = 16f
                        setPadding(0, 4, 0, 0)
                    }
                    tarjeta.addView(tvFecha)

                    llVentas.addView(tarjeta)
                }
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }
}
