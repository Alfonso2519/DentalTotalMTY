package com.dental.totalmty.ui.paciente

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dental.totalmty.data.model.Cita
import com.dental.totalmty.databinding.FragmentHistorialBinding
import com.dental.totalmty.ui.shared.CitasAdapter
import com.dental.totalmty.utils.SessionManager
import com.dental.totalmty.viewmodel.CitasViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HistorialFragment : Fragment() {
    private var _binding: FragmentHistorialBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CitasViewModel by viewModels()
    private lateinit var adapter: CitasAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Al tocar, mostrar notas del doctor si las tiene
        adapter = CitasAdapter(onCitaClick = { cita ->
            if (cita.notas.isNotEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("${cita.servicio}  —  ${cita.fecha}")
                    .setMessage("Estado: ${estadoLabel(cita.estado)}\n\n📝 Notas:\n${cita.notas}")
                    .setPositiveButton("Cerrar", null)
                    .show()
            }
        })
        binding.rvHistorial.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistorial.adapter = adapter

        val uid = SessionManager.usuario?.uid ?: return
        viewModel.getCitasPaciente(uid).observe(viewLifecycleOwner) { citas ->
            if (_binding == null) return@observe
            val historial = citas
                .filter { it.estado == Cita.ESTADO_COMPLETADA || it.estado == Cita.ESTADO_CANCELADA }
                .filter { it.pacienteId != "BLOQUEADO" }
                .sortedWith(compareByDescending<Cita> { it.fecha }.thenByDescending { it.hora })
            binding.tvEmpty.visibility = if (historial.isEmpty()) View.VISIBLE else View.GONE
            adapter.submitList(historial)
        }
    }

    private fun estadoLabel(estado: String) = when (estado) {
        Cita.ESTADO_COMPLETADA -> "🏁 Completada"
        Cita.ESTADO_CANCELADA  -> "❌ Cancelada"
        else -> estado
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
