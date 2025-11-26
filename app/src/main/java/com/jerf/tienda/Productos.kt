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

class Productos : AppCompatActivity() {

    data class Producto(val id: String, val nombre: String, val precio: String, val stock: String, val categoria: String)
    data class Categoria(val id: String, val nombre: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.productos)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etPrecio = findViewById<EditText>(R.id.etPrecio)
        val etStock = findViewById<EditText>(R.id.etStock)
        val btnInsertar = findViewById<Button>(R.id.btnInsertar)
        val lvProductos = findViewById<ListView>(R.id.lvProductos)
        val spCategoria = findViewById<Spinner>(R.id.spCategoria)

        val queue = Volley.newRequestQueue(this)
        val productos = mutableListOf<Producto>()
        val categorias = mutableListOf<Categoria>()

        // Recuperar IP
        val prefs = getSharedPreferences("config", MODE_PRIVATE)
        val ip = prefs.getString("ip_servidor", "") ?: ""
        if (ip.isEmpty()) {
            Toast.makeText(this, "No se ha configurado la IP. Ve a la pantalla principal.", Toast.LENGTH_LONG).show()
            return
        }

        val urlBase = "http://$ip/practica/listarproductosjson.php"
        val urlCategorias = "http://$ip/practica/listarcategoriasjson.php"

        // --- FUNCION LISTAR CATEGORIAS ---
        fun listarCategorias() {
            val request = StringRequest(Request.Method.GET, urlCategorias,
                { response ->
                    categorias.clear()
                    val jsonArray = JSONArray(response)
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        categorias.add(Categoria(item.getString("id"), item.getString("nombre")))
                    }
                    val adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        categorias.map { it.nombre }
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spCategoria.adapter = adapter
                },
                { error ->
                    Toast.makeText(this, "Error al cargar categorías: ${error.message}", Toast.LENGTH_SHORT).show()
                })
            queue.add(request)
        }

        // --- FUNCION LISTAR PRODUCTOS ---
        fun listarProductos() {
            val stringRequest = StringRequest(Request.Method.GET, urlBase,
                { response ->
                    productos.clear()
                    val jsonArray = JSONArray(response)
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        productos.add(
                            Producto(
                                id = item.getString("id"),
                                nombre = item.getString("nombre"),
                                precio = item.getString("precio"),
                                stock = item.getString("stock"),
                                categoria = item.getString("categoria")
                            )
                        )
                    }

                    val adapter = object : ArrayAdapter<Producto>(this, R.layout.item_producto, R.id.tvProducto, productos) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getView(position, convertView, parent)
                            val producto = getItem(position)!!
                            val tvProducto = view.findViewById<TextView>(R.id.tvProducto)
                            val btnBorrar = view.findViewById<Button>(R.id.btnBorrar)
                            val btnEditar = view.findViewById<Button>(R.id.btnEditar)

                            val text = "ID: ${producto.id}\nNombre: ${producto.nombre}\nPrecio: ${producto.precio}\nStock: ${producto.stock}\nCategoria: ${producto.categoria}"
                            val spannable = SpannableString(text)
                            val columnas = listOf("ID:", "Nombre:", "Precio:", "Stock:", "Categoria:")
                            columnas.forEach { col ->
                                val start = text.indexOf(col)
                                if (start >= 0) {
                                    spannable.setSpan(ForegroundColorSpan(Color.RED), start, start + col.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                }
                            }
                            tvProducto.text = spannable

                            // --- BORRAR ---
                            btnBorrar.setOnClickListener {
                                val deleteRequest = object : StringRequest(Method.POST, urlBase,
                                    { response ->
                                        Toast.makeText(this@Productos, "Producto eliminado", Toast.LENGTH_SHORT).show()
                                        listarProductos()
                                    },
                                    { error ->
                                        Toast.makeText(this@Productos, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }) {
                                    override fun getParams(): MutableMap<String, String> {
                                        return hashMapOf("id" to producto.id)
                                    }
                                }
                                queue.add(deleteRequest)
                            }

                            // --- EDITAR ---
                            btnEditar.setOnClickListener {
                                val dialogView = layoutInflater.inflate(R.layout.dialog_editar_producto, null)
                                val etNombreEdit = dialogView.findViewById<EditText>(R.id.etNombreEdit)
                                val etPrecioEdit = dialogView.findViewById<EditText>(R.id.etPrecioEdit)
                                val etStockEdit = dialogView.findViewById<EditText>(R.id.etStockEdit)
                                val spCategoriaEdit = dialogView.findViewById<Spinner>(R.id.spCategoriaEdit)

                                etNombreEdit.setText(producto.nombre)
                                etPrecioEdit.setText(producto.precio)
                                etStockEdit.setText(producto.stock)

                                val adapterCategorias = ArrayAdapter(
                                    this@Productos,
                                    android.R.layout.simple_spinner_item,
                                    categorias.map { it.nombre }
                                )
                                adapterCategorias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                spCategoriaEdit.adapter = adapterCategorias
                                spCategoriaEdit.setSelection(categorias.indexOfFirst { it.id == producto.categoria })

                                val dialog = android.app.AlertDialog.Builder(this@Productos)
                                    .setTitle("Editar Producto")
                                    .setView(dialogView)
                                    .setPositiveButton("Guardar") { _, _ ->
                                        val nombreNuevo = etNombreEdit.text.toString().trim()
                                        val precioNuevo = etPrecioEdit.text.toString().trim()
                                        val stockNuevo = etStockEdit.text.toString().trim()
                                        val categoriaNueva = categorias[spCategoriaEdit.selectedItemPosition].id

                                        if (nombreNuevo.isEmpty() || precioNuevo.isEmpty() || stockNuevo.isEmpty()) {
                                            Toast.makeText(this@Productos, "Completa todos los campos.", Toast.LENGTH_SHORT).show()
                                            return@setPositiveButton
                                        }

                                        val updateRequest = object : StringRequest(Method.POST, urlBase,
                                            { response ->
                                                Toast.makeText(this@Productos, "Producto actualizado", Toast.LENGTH_SHORT).show()
                                                listarProductos()
                                            },
                                            { error ->
                                                Toast.makeText(this@Productos, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                                            }) {
                                            override fun getParams(): MutableMap<String, String> {
                                                return hashMapOf(
                                                    "id" to producto.id,
                                                    "nombre" to nombreNuevo,
                                                    "precio" to precioNuevo,
                                                    "stock" to stockNuevo,
                                                    "categoria_id" to categoriaNueva
                                                )
                                            }
                                        }
                                        queue.add(updateRequest)
                                    }
                                    .setNegativeButton("Cancelar", null)
                                    .create()
                                dialog.show()
                            }

                            return view
                        }
                    }
                    lvProductos.adapter = adapter
                },
                { error ->
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                })
            queue.add(stringRequest)
        }

        // --- INSERTAR PRODUCTO ---
        btnInsertar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val precio = etPrecio.text.toString().trim()
            val stock = etStock.text.toString().trim()
            val categoriaSeleccionada = categorias[spCategoria.selectedItemPosition].id

            if (nombre.isEmpty() || precio.isEmpty() || stock.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val insertRequest = object : StringRequest(Method.POST, urlBase,
                { response ->
                    Toast.makeText(this, "Producto insertado", Toast.LENGTH_SHORT).show()
                    etNombre.text.clear()
                    etPrecio.text.clear()
                    etStock.text.clear()
                    listarProductos()
                },
                { error ->
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }) {
                override fun getParams(): MutableMap<String, String> {
                    return hashMapOf(
                        "nombre" to nombre,
                        "precio" to precio,
                        "stock" to stock,
                        "categoria_id" to categoriaSeleccionada
                    )
                }
            }
            queue.add(insertRequest)
        }

        listarCategorias()
        listarProductos()
    }
}
