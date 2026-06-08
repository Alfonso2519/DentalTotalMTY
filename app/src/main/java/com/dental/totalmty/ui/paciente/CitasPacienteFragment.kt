package com.dental.totalmty.ui.paciente

import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dental.totalmty.R
import com.dental.totalmty.data.model.Cita
import com.dental.totalmty.databinding.FragmentCitasPacienteBinding
import com.dental.totalmty.ui.shared.CitasAdapter
import com.dental.totalmty.utils.SessionManager
import com.dental.totalmty.viewmodel.CitaState
import com.dental.totalmty.viewmodel.CitasViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class CitasPacienteFragment : Fragment() {
    private var _binding: FragmentCitasPacienteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CitasViewModel by viewModels()
    private lateinit var adapter: CitasAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCitasPacienteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Al tocar una cita activa, el paciente puede cancelarla
        adapter = CitasAdapter(onCitaClick = { cita -> onCitaTapped(cita) })
        binding.rvCitas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCitas.adapter = adapter

        // También swipe izquierda para cancelar
        adapter.attachSwipeToCancel(binding.rvCitas) { cita ->
            if (_binding == null) return@attachSwipeToCancel
            showCancelarDialog(cita)
        }

        binding.fabNuevaCita.setOnClickListener {
            findNavController().navigate(R.id.action_citas_to_nueva)
        }

        binding.swipeRefresh.setColorSchemeResources(R.color.cyan_primary)
        binding.swipeRefresh.setOnRefreshListener { loadData() }

        loadData()

        viewModel.citaState.observe(viewLifecycleOwner) { state ->
            if (_binding == null) return@observe
            when (state) {
                is CitaState.Success -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.success_green)).show()
                    viewModel.resetState()
                }
                is CitaState.Error -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_red)).show()
                    viewModel.resetState()
                }
                else -> {}
            }
        }
    }

    private fun onCitaTapped(cita: Cita) {
        if (_binding == null) return
        // Solo mostrar opciones en citas que se pueden cancelar
        if (cita.estado == Cita.ESTADO_CANCELADA || cita.estado == Cita.ESTADO_COMPLETADA) {
            // Mostrar notas si las hay
            if (cita.notas.isNotEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("${cita.servicio}  —  ${cita.fecha}")
                    .setMessage("📝 Notas del doctor:\n\n${cita.notas}")
                    .setPositiveButton("Cerrar", null)
                    .show()
            }
            return
        }
        // Cita activa: ofrecer cancelar
        showCancelarDialog(cita)
    }

    private fun showCancelarDialog(cita: Cita) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancelar cita")
            .setMessage("¿Deseas cancelar tu cita de\n${cita.servicio}\nel ${cita.fecha} a las ${cita.hora}?")
            .setPositiveButton("Sí, cancelar") { _, _ -> viewModel.cancelarCita(cita.id) }
            .setNegativeButton("No") { _, _ -> adapter.notifyDataSetChanged() }
            .show()
    }

    private fun loadData() {
        val uid = SessionManager.usuario?.uid ?: run {
            binding.swipeRefresh.isRefreshing = false; return
        }
        viewModel.getCitasPaciente(uid).observe(viewLifecycleOwner) { citas ->
            if (_binding == null) return@observe
            binding.swipeRefresh.isRefreshing = false
            // Solo mostrar citas del paciente, no bloqueos
            val citasPaciente = citas.filter { it.pacienteId != "BLOQUEADO" }
            binding.tvNoCitas.visibility = if (citasPaciente.isEmpty()) View.VISIBLE else View.GONE
            adapter.submitList(citasPaciente)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
