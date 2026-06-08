package com.dental.totalmty.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dental.totalmty.databinding.FragmentRegisterBinding
import com.dental.totalmty.viewmodel.AuthState
import com.dental.totalmty.viewmodel.AuthViewModel

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.tvLogin.setOnClickListener  { findNavController().navigateUp() }

        binding.btnRegister.setOnClickListener {
            animatePress(it)
            val nombre   = binding.etNombre.text?.toString()?.trim() ?: ""
            val telefono = binding.etTelefono.text?.toString()?.trim() ?: ""
            val email    = binding.etEmail.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            val confirm  = binding.etConfirmPassword.text?.toString() ?: ""

            if (password != confirm) {
                showError("Las contraseñas no coinciden")
                return@setOnClickListener
            }
            viewModel.register(email, password, nombre, telefono)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnRegister.isEnabled  = false
                    binding.tvError.visibility     = View.GONE
                }
                is AuthState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled  = true
                    (activity as? AuthActivity)?.onLoginSuccess(state.usuario)
                }
                is AuthState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled  = true
                    showError(state.message)
                    viewModel.resetState()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled  = true
                }
            }
        }
    }

    private fun showError(msg: String) {
        binding.tvError.text       = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun animatePress(v: View) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
            .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(80).start() }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
