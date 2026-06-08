package com.dental.totalmty.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dental.totalmty.R
import com.dental.totalmty.data.model.Usuario
import com.dental.totalmty.databinding.ActivityAuthBinding
import com.dental.totalmty.ui.doctor.DoctorActivity
import com.dental.totalmty.ui.paciente.MainActivity
import com.dental.totalmty.utils.SessionManager

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /**
     * Llamado desde LoginFragment/RegisterFragment cuando el login es exitoso.
     * Guarda el usuario en SessionManager ANTES de navegar, evitando crash en
     * HomeFragment u otros fragments que dependen de SessionManager.usuario.
     */
    fun onLoginSuccess(usuario: Usuario) {
        SessionManager.usuario = usuario
        val intent = if (usuario.rol == Usuario.ROL_DOCTOR) {
            Intent(this, DoctorActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}
