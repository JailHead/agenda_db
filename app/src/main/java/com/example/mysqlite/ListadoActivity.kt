package com.example.mysqlite

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

class ListadoActivity : AppCompatActivity() {
    // Variables simplificadas
    private lateinit var txtDetalle: TextView
    private lateinit var txtContador: TextView
    private lateinit var btnRegresar: Button
    private lateinit var btnActualizar: Button

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listado)

        firestore = Firebase.firestore
        auth = FirebaseAuth.getInstance()

        // Asociar componentes
        txtDetalle = findViewById(R.id.txtDetalle)
        txtContador = findViewById(R.id.txtContador)
        btnRegresar = findViewById(R.id.btnRegresar)
        btnActualizar = findViewById(R.id.btnActualizar)

        // DEBUG: Forzar visibilidad del TextView
        txtDetalle.setBackgroundColor(android.graphics.Color.WHITE)
        txtDetalle.setTextColor(android.graphics.Color.BLACK)
        txtDetalle.textSize = 14f

        // Configurar botones
        btnActualizar.setOnClickListener {
            listarContactos()
        }
        btnRegresar.setOnClickListener {
            finish()
        }

        listarContactos()
    }

    private fun listarContactos() {
        Log.d("ListadoActivity", "listarContactos - INICIADO")

        // Verificar autenticación
        val currentUser = auth.currentUser
        if (currentUser == null) {
            txtDetalle.text = "❌ ERROR DE AUTENTICACIÓN\n\nUsuario no autenticado.\nPor favor inicie sesión."
            txtContador.text = "❌ Sin autenticar"
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_LONG).show()
            return
        }

        // Estado de carga - mantener igual
        txtContador.text = "🔄 Consultando Firebase..."
        txtDetalle.text = "⏳ Cargando contactos desde Firebase...\n\nEspere un momento..."

        // Consultar Firebase Firestore
        firestore.collection("contactos")
            .whereEqualTo("userId", currentUser.uid) // Solo contactos del usuario actual
            .orderBy("createdAt", Query.Direction.DESCENDING) // Más recientes primero
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Contactos encontrados - construir el texto igual que antes
                    var listaContactos = "📱 LISTA DE CONTACTOS\n"
                    listaContactos += "═════════════════════\n\n"

                    documents.forEachIndexed { index, document ->
                        listaContactos += "👤 CONTACTO ${index + 1}\n"
                        listaContactos += "─────────────────\n"
                        listaContactos += "Nombre: ${document.getString("nombre") ?: ""}\n"
                        listaContactos += "Apellidos: ${document.getString("apellidos") ?: ""}\n"

                        // Manejar campos que pueden estar vacíos
                        val telefono = document.getString("telefono")
                        if (!telefono.isNullOrEmpty()) {
                            listaContactos += "Teléfono: $telefono\n"
                        } else {
                            listaContactos += "Teléfono: No registrado\n"
                        }

                        val email = document.getString("email")
                        if (!email.isNullOrEmpty()) {
                            listaContactos += "Email: $email\n"
                        } else {
                            listaContactos += "Email: No registrado\n"
                        }

                        listaContactos += "\n"
                    }

                    listaContactos += "═════════════════════\n"
                    listaContactos += "📊 Total: ${documents.size()} contactos"

                    Log.d("ListadoActivity", "✅ ${documents.size()} contactos cargados")

                    // Actualizar UI - mantener el mismo formato
                    txtDetalle.text = listaContactos
                    txtContador.text = "✅ Cargados: ${documents.size()} contactos"
                    txtContador.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))

                    // Forzar actualización de la vista
                    txtDetalle.invalidate()
                    txtDetalle.requestLayout()

                    Toast.makeText(this, "✅ ${documents.size()} contactos cargados", Toast.LENGTH_SHORT).show()
                } else {
                    // Sin contactos - mantener el mismo mensaje
                    txtDetalle.text = "📭 SIN CONTACTOS\n\nNo hay contactos registrados en Firebase.\n\nAgregue contactos desde la pantalla principal."
                    txtContador.text = "📊 Total: 0 contactos"

                    Log.d("ListadoActivity", "ℹ️ No hay contactos para este usuario")
                    Toast.makeText(this, "No hay contactos registrados", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                // Error en la consulta
                Log.e("ListadoActivity", "Error al listar contactos: ${e.message}", e)

                val errorMessage = when (e) {
                    is FirebaseFirestoreException -> {
                        when (e.code) {
                            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                "❌ ERROR DE PERMISOS\n\nSin permisos para acceder a los contactos.\nVerifique las reglas de seguridad."
                            FirebaseFirestoreException.Code.UNAVAILABLE ->
                                "🌐 ERROR DE SERVICIO\n\nFirebase no disponible.\nIntente más tarde."
                            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                                "🌐 ERROR DE RED\n\nRevise su conexión a internet."
                            else ->
                                "❌ ERROR FIREBASE\n\nDetalles: ${e.message}"
                        }
                    }
                    else -> "❌ ERROR GENERAL\n\nDetalles: ${e.message}"
                }

                txtDetalle.text = errorMessage
                txtContador.text = "❌ Error al cargar"
                Toast.makeText(this, "Error al cargar contactos", Toast.LENGTH_LONG).show()
            }
    }
}