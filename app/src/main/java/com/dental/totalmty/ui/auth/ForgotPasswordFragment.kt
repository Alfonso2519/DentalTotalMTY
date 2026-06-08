package com.dental.totalmty.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dental.totalmty.R
import com.dental.totalmty.viewmodel.AuthState
import com.dental.totalmty.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordFragment : Fragment() {
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val btnSend = view.findViewById<MaterialButton>(R.id.btnSend)
        val btnBack = view.findViewById<android.widget.ImageButton?>(R.id.btnBack)
        val progress = view.findViewById<LinearProgressIndicator>(R.id.progressBar)
        val tvError = view.findViewById<TextView>(R.id.tvError)

        btnBack?.setOnClickListener { findNavController().navigateUp() }

        btnSend.setOnClickListener {
            val email = etEmail.text.toString().trim()
            viewModel.sendPasswordReset(email)
        }

        viewModel.passwordResetState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    progress?.visibility = View.VISIBLE
                    btnSend.isEnabled = false
                }
                is AuthState.Success -> {
                    progress?.visibility = View.GONE
                    Snackbar.make(view, "¡Correo enviado! Revisa tu email.", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(resources.getColor(R.color.success_green, null)).show()
                }
                is AuthState.Error -> {
                    progress?.visibility = View.GONE
                    btnSend.isEnabled = true
                    tvError?.text = state.message
                    tvError?.visibility = View.VISIBLE
                }
                else -> { progress?.visibility = View.GONE; btnSend.isEnabled = true }
            }
        }
    }
}
