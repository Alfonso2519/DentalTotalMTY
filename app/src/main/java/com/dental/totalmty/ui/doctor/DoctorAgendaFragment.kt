package com.dental.totalmty.ui.doctor

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dental.totalmty.data.model.Cita
import com.dental.totalmty.databinding.FragmentDoctorAgendaBinding
import com.dental.totalmty.ui.shared.CitasAdapter
import com.dental.totalmty.viewmodel.CitasViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class DoctorAgendaFragment : Fragment() {
    private var _binding: FragmentDoctorAgendaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CitasViewModel by viewModels()
    private lateinit var adapter: CitasAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDoctorAgendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = CitasAdapter(onCitaClick = { showOptionsDialog(it) }, showDoctorName = true)
        binding.rvTodasCitas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTodasCitas.adapter = adapter
        viewModel.getAllCitas().observe(viewLifecycleOwner) { citas ->
            if (_binding == null) return@observe
            binding.tvNoCitas.visibility = if (citas.isEmpty()) View.VISIBLE else View.GONE
            adapter.submitList(citas)
        }
    }

    private fun showOptionsDialog(cita: Cita) {
        val esBloqueo = cita.pacienteId == "BLOQUEADO"
        val titulo    = if (esBloqueo) "🔒 ${cita.servicio}" else "${cita.servicio}\n👤 ${cita.pacienteNombre}"
        val notasTxt  = if (cita.notas.isNotEmpty()) "\n📝 ${cita.notas}" else ""
        val detalle   = "📅 ${cita.fecha}  🕐 ${cita.hora}\nEstado: ${estadoLabel(cita.estado)}$notasTxt"
        val builder   = MaterialAlertDialogBuilder(requireContext()).setTitle(titulo).setMessage(detalle)

        when (cita.estado) {
            Cita.ESTADO_PENDIENTE  -> builder
                .setPositiveButton("✅ Confirmar")    { _, _ -> showDuracionDialog(cita) }
                .setNeutralButton("✏️ Editar")         { _, _ -> showEditarDialog(cita) }
                .setNegativeButton("❌ Cancelar")      { _, _ -> viewModel.cancelarCita(cita.id) }
            Cita.ESTADO_CONFIRMADA -> builder
                .setPositiveButton("🏁 Completar")    { _, _ -> showCompletarDialog(cita) }
                .setNeutralButton("✏️ Editar")         { _, _ -> showEditarDialog(cita) }
                .setNegativeButton("❌ Cancelar")      { _, _ -> viewModel.cancelarCita(cita.id) }
            Cita.ESTADO_CANCELADA  -> builder
                .setPositiveButton("🔄 Reactivar")    { _, _ -> viewModel.reactivarCita(cita.id) }
                .setNeutralButton("✏️ Editar notas")   { _, _ -> showEditarDialog(cita) }
                .setNegativeButton("🗑 Eliminar")      { _, _ -> confirmarEliminar(cita) }
            Cita.ESTADO_COMPLETADA -> builder
                .setPositiveButton("✏️ Editar notas")  { _, _ -> showEditarDialog(cita) }
                .setNegativeButton("🗑 Eliminar")      { _, _ -> confirmarEliminar(cita) }
            else -> builder.setNegativeButton("Cerrar", null)
        }
        builder.show()
    }

        private fun showDuracionDialog(cita: Cita) {
        val maxSlots = 8
        val idx      = Cita.TODOS_LOS_SLOTS.indexOf(cita.hora).takeIf { it >= 0 } ?: 0
        val opciones = buildDuracionOpciones(cita.hora, idx, maxSlots)
        if (opciones.isEmpty()) return

        val scroll    = android.widget.ScrollView(requireContext())
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 16, 48, 8)
        }
        scroll.addView(container)

        val group      = android.widget.RadioGroup(requireContext()).apply {
            orientation = android.widget.RadioGroup.VERTICAL
        }
        val preselected = (cita.duracionSlots - 1).coerceIn(0, maxSlots - 1)
        opciones.forEachIndexed { index, opcion ->
            val rb = android.widget.RadioButton(requireContext()).apply {
                text     = opcion
                id       = index
                textSize = 15f
                setPadding(8, 16, 8, 16)
                isChecked = (index == preselected)
                setTextColor(android.graphics.Color.parseColor("#1C2B3A"))
            }
            group.addView(rb)
        }
        container.addView(group)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⏱ Duración de la cita")
            .setMessage("👤 ${cita.pacienteNombre}\n📋 ${cita.servicio}")
            .setView(scroll)
            .setPositiveButton("Confirmar") { _, _ ->
                val duracion = if (group.checkedRadioButtonId >= 0) group.checkedRadioButtonId + 1 else cita.duracionSlots
                viewModel.confirmarCita(cita.id, duracion)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    private fun showCompletarDialog(cita: Cita) {
        val input = TextInputEditText(requireContext()).apply {
            hint = "Notas de la consulta (opcional)"
            setText(cita.notas)
            setPadding(48, 32, 48, 16)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🏁 Completar cita")
            .setMessage("${cita.servicio} — ${cita.pacienteNombre}")
            .setView(input)
            .setPositiveButton("Completar") { _, _ ->
                viewModel.completarCita(cita.id, input.text.toString().trim())
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun showEditarDialog(cita: Cita) {
        val estados      = arrayOf("⏳ Pendiente", "✅ Confirmada", "🏁 Completada", "❌ Cancelada")
        val estadosCodigo = arrayOf(Cita.ESTADO_PENDIENTE, Cita.ESTADO_CONFIRMADA, Cita.ESTADO_COMPLETADA, Cita.ESTADO_CANCELADA)
        val actual       = estadosCodigo.indexOf(cita.estado).coerceAtLeast(0)
        var nuevoEstado  = cita.estado

        val notasInput = TextInputEditText(requireContext()).apply {
            hint = "Notas (visibles para el paciente)"
            setText(cita.notas)
            setPadding(48, 32, 48, 16)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("✏️ Editar cita")
            .setMessage("${cita.servicio} — ${cita.pacienteNombre}\n${cita.fecha} ${cita.hora}")
            .setSingleChoiceItems(estados as Array<CharSequence>, actual) { _: android.content.DialogInterface, which: Int -> nuevoEstado = estadosCodigo[which] }
            .setView(notasInput)
            .setPositiveButton("Guardar") { _, _ ->
                if (nuevoEstado != cita.estado) viewModel.cambiarEstado(cita.id, nuevoEstado)
                val nuevasNotas = notasInput.text.toString().trim()
                if (nuevasNotas != cita.notas) viewModel.actualizarNotas(cita.id, nuevasNotas)
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun confirmarEliminar(cita: Cita) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🗑 ¿Eliminar?")
            .setMessage("Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.eliminarCita(cita.id) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun estadoLabel(estado: String) = when (estado) {
        Cita.ESTADO_PENDIENTE  -> "⏳ Pendiente"
        Cita.ESTADO_CONFIRMADA -> "✅ Confirmada"
        Cita.ESTADO_CANCELADA  -> "❌ Cancelada"
        Cita.ESTADO_COMPLETADA -> "🏁 Completada"
        else -> estado
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
