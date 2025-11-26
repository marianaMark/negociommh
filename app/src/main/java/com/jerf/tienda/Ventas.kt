package com.jerf.tienda

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class Ventas : AppCompatActivity() {

    data class Cliente(val id: String, val nombre: String, val apellido: String) {
        override fun toString(): String {
            return "$nombre $apellido"
        }
    }

    data class Producto(val id: String, val nombre: String, val precio: Double, val stock: Int)
    data class ProductoSeleccionado(
        val id: String,
        val nombre: String,
        val precio: Double,
        var cantidad: Int,
        var subtotal: Double
    )

    private val productosSeleccionados = mutableListOf<ProductoSeleccionado>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ventas)

        val spCliente = findViewById<Spinner>(R.id.spCliente)
        val etDireccion = findViewById<EditText>(R.id.etDireccion)
        val lvProductosDisponibles = findViewById<ListView>(R.id.lvProductosDisponibles)
        val lvProductosSeleccionados = findViewById<ListView>(R.id.lvProductosSeleccionados)
        val tvTotal = findViewById<TextView>(R.id.tvTotal)
        val btnGuardarVenta = findViewById<Button>(R.id.btnGuardarVenta)

        val queue = Volley.newRequestQueue(this)
        val clientes = mutableListOf<Cliente>()
        val productosDisponibles = mutableListOf<Producto>()

        val prefs = getSharedPreferences("config", MODE_PRIVATE)
        val ip = prefs.getString("ip_servidor", "") ?: ""
        if (ip.isEmpty()) {
            Toast.makeText(this, "No se ha configurado la IP.", Toast.LENGTH_LONG).show()
            return
        }

        val urlClientes = "http://$ip/practica/listarclientesjson.php"
        val urlProductos = "http://$ip/practica/listarproductosjson.php"
        val urlGuardarVenta = "http://$ip/practica/guardarventa.php"

        // --- Cargar clientes ---
        val requestClientes = StringRequest(urlClientes, { response ->
            clientes.clear()
            val array = JSONArray(response)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                clientes.add(
                    Cliente(
                        id = item.getString("id"),
                        nombre = item.getString("nombre"),
                        apellido = item.getString("apellido")
                    )
                )
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, clientes)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spCliente.adapter = adapter
        }, { error ->
            Toast.makeText(this, "Error clientes: ${error.message}", Toast.LENGTH_SHORT).show()
        })
        queue.add(requestClientes)

        // --- Cargar productos disponibles ---
        val requestProductos = StringRequest(urlProductos, { response ->
            productosDisponibles.clear()
            val array = JSONArray(response)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                productosDisponibles.add(
                    Producto(
                        item.getString("id"),
                        item.getString("nombre"),
                        item.getDouble("precio"),
                        item.getInt("stock")
                    )
                )
            }

            val adapter = object : ArrayAdapter<Producto>(
                this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                productosDisponibles
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val producto = getItem(position)!!
                    val tv1 = view.findViewById<TextView>(android.R.id.text1)
                    val tv2 = view.findViewById<TextView>(android.R.id.text2)
                    tv1.text = producto.nombre
                    tv2.text = "Precio: ${producto.precio} | Stock: ${producto.stock}"

                    view.setOnClickListener {
                        val cantidadInput = EditText(this@Ventas)
                        cantidadInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        cantidadInput.hint = "Cantidad"

                        val dialog = android.app.AlertDialog.Builder(this@Ventas)
                            .setTitle("Cantidad de ${producto.nombre}")
                            .setView(cantidadInput)
                            .setPositiveButton("Agregar") { _, _ ->
                                val cantidad = cantidadInput.text.toString().toIntOrNull() ?: 0
                                if (cantidad > 0) {
                                    if (cantidad > producto.stock) {
                                        Toast.makeText(this@Ventas, "No hay suficiente stock disponible. Stock: ${producto.stock}", Toast.LENGTH_SHORT).show()
                                        return@setPositiveButton
                                    }
                                    val subtotal = cantidad * producto.precio
                                    productosSeleccionados.add(
                                        ProductoSeleccionado(
                                            producto.id,
                                            producto.nombre,
                                            producto.precio,
                                            cantidad,
                                            subtotal
                                        )
                                    )
                                    actualizarListaSeleccionados(lvProductosSeleccionados, tvTotal)
                                }
                            }
                            .setNegativeButton("Cancelar", null)
                            .create()
                        dialog.show()
                    }

                    return view
                }
            }
            lvProductosDisponibles.adapter = adapter
        }, { error ->
            Toast.makeText(this, "Error productos: ${error.message}", Toast.LENGTH_SHORT).show()
        })
        queue.add(requestProductos)

        // --- Guardar venta ---
        btnGuardarVenta.setOnClickListener {
            if (spCliente.selectedItemPosition == AdapterView.INVALID_POSITION) {
                Toast.makeText(this, "Seleccione un cliente", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val clienteId = clientes[spCliente.selectedItemPosition].id
            val direccion = etDireccion.text.toString().trim()
            if (direccion.isEmpty()) {
                Toast.makeText(this, "Ingrese dirección", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (productosSeleccionados.isEmpty()) {
                Toast.makeText(this, "No hay productos en la venta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val total = productosSeleccionados.sumOf { it.subtotal }

            val params = HashMap<String, String>()
            params["cliente_id"] = clienteId
            params["direccion"] = direccion
            params["fecha"] = fecha
            params["total"] = total.toString()

            val productosJson = JSONArray()
            for (p in productosSeleccionados) {
                val obj = org.json.JSONObject()
                obj.put("id", p.id)
                obj.put("cantidad", p.cantidad)
                obj.put("subtotal", p.subtotal)
                productosJson.put(obj)
            }
            params["productos"] = productosJson.toString()

            val request = object : StringRequest(
                Method.POST, urlGuardarVenta,
                { response ->
                    Toast.makeText(this, "Venta guardada y stock actualizado", Toast.LENGTH_SHORT).show()
                    productosSeleccionados.clear()
                    actualizarListaSeleccionados(lvProductosSeleccionados, tvTotal)
                    etDireccion.text.clear()
                },
                { error ->
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }) {
                override fun getParams(): MutableMap<String, String> = params
            }
            queue.add(request)
        }
    }

    private fun actualizarListaSeleccionados(lv: ListView, tvTotal: TextView) {
        val adapter = object : ArrayAdapter<ProductoSeleccionado>(this, R.layout.item_producto_seleccionado, R.id.tvProductoSeleccionado, productosSeleccionados) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val producto = getItem(position)!!

                val tv = view.findViewById<TextView>(R.id.tvProductoSeleccionado)
                val btnBorrar = view.findViewById<Button>(R.id.btnBorrarProducto)

                tv.text = "${producto.nombre} x${producto.cantidad} = ${producto.subtotal}"

                btnBorrar.setOnClickListener {
                    productosSeleccionados.removeAt(position)
                    notifyDataSetChanged()
                    val total = productosSeleccionados.sumOf { it.subtotal }
                    tvTotal.text = "Total: $total"
                }

                return view
            }
        }
        lv.adapter = adapter

        val total = productosSeleccionados.sumOf { it.subtotal }
        tvTotal.text = "Total: $total"
    }

}
