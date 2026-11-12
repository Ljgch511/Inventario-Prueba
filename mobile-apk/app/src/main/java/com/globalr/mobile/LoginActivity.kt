package com.globalr.mobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ApiClient.getToken(this) != null && !intent.getBooleanExtra("force_config", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val etServerUrl = findViewById<EditText>(R.id.etServerUrl)
        val btnTest = findViewById<Button>(R.id.btnTestConnection)
        val tvConnStatus = findViewById<TextView>(R.id.tvConnectionStatus)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvError = findViewById<TextView>(R.id.tvLoginError)

        etServerUrl.setText(ApiClient.getServerUrl(this))

        btnTest.setOnClickListener {
            ApiClient.setServerUrl(this, etServerUrl.text.toString())
            tvConnStatus.text = "Probando..."
            thread {
                try {
                    val res = ApiClient.testHealth(this)
                    runOnUiThread { tvConnStatus.text = "Conectado: $res" }
                } catch (e: Exception) {
                    runOnUiThread { tvConnStatus.text = "Error: ${e.message}" }
                }
            }
        }

        btnLogin.setOnClickListener {
            ApiClient.setServerUrl(this, etServerUrl.text.toString())
            tvError.visibility = View.GONE
            btnLogin.isEnabled = false
            val user = etUsername.text.toString().trim()
            val pass = etPassword.text.toString()

            thread {
                try {
                    ApiClient.login(this, user, pass)
                    runOnUiThread {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        btnLogin.isEnabled = true
                        tvError.text = e.message ?: "Error al iniciar sesión"
                        tvError.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}
