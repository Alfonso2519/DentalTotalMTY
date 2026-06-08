package com.dental.totalmty.ui.paciente

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.dental.totalmty.R
import com.dental.totalmty.databinding.FragmentPerfilBinding
import com.dental.totalmty.ui.auth.AuthActivity
import com.dental.totalmty.utils.SessionManager
import com.dental.totalmty.viewmodel.AuthState
import com.dental.totalmty.viewmodel.AuthViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class PerfilFragment : Fragment() {
    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val usuario = SessionManager.usuario
        binding.tvNombre.text    = usuario?.nombre   ?: ""
        binding.tvEmail.text     = usuario?.email    ?: ""
        binding.tvTelefono.text  = usuario?.telefono ?: ""
        binding.etNombreEdit.setText(usuario?.nombre   ?: "")
        binding.etTelefonoEdit.setText(usuario?.telefono ?: "")

        // ── Guardar cambios en Firestore ────────────────────────────────
        binding.btnGuardar.setOnClickListener {
            val uid      = SessionManager.usuario?.uid ?: return@setOnClickListener
            val nombre   = binding.etNombreEdit.text.toString()
            val telefono = binding.etTelefonoEdit.text.toString()
            authViewModel.updatePerfil(uid, nombre, telefono)
        }

        authViewModel.perfilState.observe(viewLifecycleOwner) { state ->
            if (_binding == null) return@observe
            when (state) {
                is AuthState.Loading -> {
                    binding.btnGuardar.isEnabled = false
                    binding.btnGuardar.text = "Guardando..."
                }
                is AuthState.Success -> {
                    binding.btnGuardar.isEnabled = true
                    binding.btnGuardar.text = "Guardar Cambios"
                    // Actualizar SessionManager en memoria
                    SessionManager.usuario = SessionManager.usuario?.copy(
                        nombre   = binding.etNombreEdit.text.toString().trim(),
                        telefono = binding.etTelefonoEdit.text.toString().trim()
                    )
                    // Refrescar los TextViews del encabezado
                    binding.tvNombre.text   = SessionManager.usuario?.nombre   ?: ""
                    binding.tvTelefono.text = SessionManager.usuario?.telefono ?: ""
                    Snackbar.make(binding.root, "✅ Cambios guardados", Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.success_green))
                        .show()
                    authViewModel.resetPerfilState()
                }
                is AuthState.Error -> {
                    binding.btnGuardar.isEnabled = true
                    binding.btnGuardar.text = "Guardar Cambios"
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_red))
                        .show()
                    authViewModel.resetPerfilState()
                }
                else -> {
                    binding.btnGuardar.isEnabled = true
                    binding.btnGuardar.text = "Guardar Cambios"
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Deseas cerrar sesión?")
                .setPositiveButton("Sí") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    SessionManager.clear()
                    startActivity(Intent(requireContext(), AuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                .setNegativeButton("No", null).show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
