package com.example.vexel_salida

import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

import java.io.File
import java.io.IOException

class Respuesta : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var layout: ConstraintLayout
    private lateinit var cancelButton: Button
    private lateinit var imageView: ImageView
    private val urlOTP = "https://accesototp-668387496305.us-central1.run.app/accesototp"
    private val urlFace = "https://accesorf-668387496305.us-central1.run.app/procesar_foto"
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_respuesta)

        statusText = findViewById(R.id.statusText)
        layout = findViewById(R.id.main)
        imageView = findViewById(R.id.responseImage)
        cancelButton = findViewById(R.id.cancel_button)

        when(intent.getStringExtra("modo")){
            "foto" -> {
                val ruta = intent.getStringExtra("rutaFoto")
                if (ruta != null) {
                    enviarFoto(File(ruta),"salida")
                }
            }
            "qr" -> {
                val palabra = intent.getStringExtra("palabra")
                if (palabra != null) {
                    enviarPalabra(palabra, "salida")
                }
            }
        }

        cancelButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

    }


    private fun enviarPalabra(palabra: String, tipoRegistro: String) {
        try {
            val partes = palabra.split("//")
            if (partes.size != 2) {
                Log.e("Error", "Formato inválido")
                return
            }

            val userId = partes[0]
            val otp = partes[1]

            val json = JSONObject().apply {
                put("user_id", userId)
                put("otp", otp)
                put("tipo_registro", tipoRegistro)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(urlOTP)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        layout.setBackgroundColor(Color.RED)
                        statusText.text = "Error al enviar la OTP"
                        cerrarVentana()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        if (response.code == 200) {
                            val json = JSONObject(response.body?.string())
                            val usuario = json.getString("mensaje")
                            layout.setBackgroundColor(Color.parseColor("#1F693C"))
                            imageView.setImageResource(R.drawable.autorizo)
                            statusText.text = "Salida registrada \n\n Vuelva pronto: \n$usuario"
                        } else {
                            val json = JSONObject(response.body?.string())
                            val mensaje = json.getString("mensaje")
                            layout.setBackgroundColor(Color.parseColor("#F03A47"))
                            imageView.setImageResource(R.drawable.denegado)
                            statusText.text = "Salida no registrada: \n\n$mensaje"
                        }
                        cerrarVentana()
                    }
                }
            })
        } catch (e: Exception) {
            logError("Error general: ${e.message}")
        }
    }


    private fun enviarFoto(foto: File, tipoRegistro: String) {
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("foto", foto.name, foto.asRequestBody("image/jpeg".toMediaTypeOrNull()))
            .addFormDataPart("tipo_registro", tipoRegistro)
            .build()

        val request = Request.Builder()
            .url(urlFace)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    layout.setBackgroundColor(Color.RED)
                    statusText.text = "Error al procesar solicitud"
                    cerrarVentana()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.code == 200) {
                        val json = JSONObject(response.body?.string())
                        val usuario = json.getString("usuario")
                        layout.setBackgroundColor(Color.parseColor("#1F693C"))
                        imageView.setImageResource(R.drawable.autorizo)
                        statusText.text = "Salida registrada \n\n Vuelva pronto: \n$usuario"
                        val mp = MediaPlayer.create(this@Respuesta, R.raw.pass)
                        mp.start()
                    } else {
                        val json = JSONObject(response.body?.string())
                        val mensaje = json.optString("razon", "Error desconocido")
                        layout.setBackgroundColor(Color.parseColor("#F03A47"))
                        imageView.setImageResource(R.drawable.denegado)
                        statusText.text = "Salida no registrada \n\n$mensaje"
                        val mp = MediaPlayer.create(this@Respuesta, R.raw.error)
                        mp.start()
                    }
                    cerrarVentana()
                }
            }
        })
    }


    private fun cerrarVentana() {
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this@Respuesta, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
    private fun logError(message: String) {
        Log.e("API_ERROR", message)
        runOnUiThread {
            Toast.makeText(this@Respuesta, message, Toast.LENGTH_LONG).show()
        }
    }


}