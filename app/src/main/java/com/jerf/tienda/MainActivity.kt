package com.jerf.tienda

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Ajuste para evitar solapamiento con la barra del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Referencias a los elementos
        val etIp = findViewById<EditText>(R.id.etIp)
        val btnConectar = findViewById<Button>(R.id.btnConectar)
        val btnProductos = findViewById<Button>(R.id.btnProductos)
        val btnClientes = findViewById<Button>(R.id.btnClientes)

        // SharedPreferences para guardar la IP
        val sharedPref = getSharedPreferences("config", Context.MODE_PRIVATE)

        // Cargar IP guardada (si existe)
        val ipGuardada = sharedPref.getString("ip_servidor", "")
        etIp.setText(ipGuardada)

        // Botón conectar → guarda la IP
        btnConectar.setOnClickListener {
            val ip = etIp.text.toString().trim()
            if (ip.isNotEmpty()) {
                with(sharedPref.edit()) {
                    putString("ip_servidor", ip)
                    apply()
                }
                Toast.makeText(this, "IP guardada correctamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Por favor ingresa una IP válida", Toast.LENGTH_SHORT).show()
            }
        }

        // Botones para abrir otras pantallas, pasando la IP guardada
        btnProductos.setOnClickListener {
            val intent = Intent(this, Productos::class.java)
            intent.putExtra("ip_servidor", etIp.text.toString().trim())
            startActivity(intent)
        }

        btnClientes.setOnClickListener {
            val intent = Intent(this, Clientes::class.java)
            intent.putExtra("ip_servidor", etIp.text.toString().trim())
            startActivity(intent)
        }

        val btnVentas = findViewById<Button>(R.id.btnVentas)
        btnVentas.setOnClickListener {
            val intent = Intent(this, Ventas::class.java)
            intent.putExtra("ip_servidor", etIp.text.toString().trim())
            startActivity(intent)
        }

        val btnDetalles = findViewById<Button>(R.id.btnDetalles)

        btnDetalles.setOnClickListener {
            val intent = Intent(this, Detalle::class.java)
            intent.putExtra("ip_servidor", etIp.text.toString().trim())
            startActivity(intent)
        }

    }
}
