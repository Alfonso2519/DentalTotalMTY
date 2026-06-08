package com.dental.totalmty.ui.doctor

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dental.totalmty.R
import com.dental.totalmty.data.model.Cita
import com.dental.totalmty.databinding.FragmentDoctorDashboardBinding
import com.dental.totalmty.ui.auth.AuthActivity
import com.dental.totalmty.ui.shared.CitasAdapter
import com.dental.totalmty.utils.SessionManager
import com.dental.totalmty.viewmodel.CitaState
import com.dental.totalmty.viewmodel.CitasViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class DoctorDashboardFragment : Fragment() {

    private var _binding: FragmentDoctorDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CitasViewModel by viewModels()
    private lateinit var adapter: CitasAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDoctorDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = CitasAdapter(onCitaClick = { showCitaOptionsDialog(it) }, showDoctorName = true)
        binding.rvCitasHoy.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCitasHoy.adapter = adapter
        observeData()
        setupClickListeners()
    }

    private fun observeData() {
        viewModel.getCitasHoy().observe(viewLifecycleOwner) { citas ->
            if (_binding == null) return@observe
            binding.swipeRefresh.isRefreshing = false
            val reales = citas.filter { it.pacienteId != "BLOQUEADO" }
            binding.tvCitasHoyCount.text        = reales.size.toString()
            binding.tvCitasPendientesCount.text = reales.count { it.estado == Cita.ESTADO_PENDIENTE }.toString()
            binding.tvNoCitasHoy.visibility = if (citas.isEmpty()) View.VISIBLE else View.GONE
            binding.rvCitasHoy.visibility   = if (citas.isEmpty()) View.GONE    else View.VISIBLE
            if (citas.isNotEmpty()) adapter.submitList(citas)
        }
        viewModel.getAllCitas().observe(viewLifecycleOwner) { citas ->
            if (_binding == null) return@observe
            binding.tvPacientesCount.text = citas.filter { it.pacienteId != "BLOQUEADO" }
                .map { it.pacienteId }.toSet().size.toString()
        }
        viewModel.citaState.observe(viewLifecycleOwner) { state ->
            if (_binding == null) return@observe
            if (state is CitaState.Success) {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.success_green)).show()
                viewModel.resetState()
            } else if (state is CitaState.Error) {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_red)).show()
                viewModel.resetState()
            }
        }
    }

    private fun setupClickListeners() {
        binding.tvVerTodas.setOnClickListener { findNavController().navigate(R.id.nav_doctor_agenda) }
        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cerrar sesión").setMessage("¿Estás seguro?")
                .setPositiveButton("Cerrar sesión") { _, _ ->
                    FirebaseAuth.getInstance().signOut(); SessionManager.clear()
                    startActivity(Intent(requireContext(), AuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }.setNegativeButton("Cancelar", null).show()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.cyan_primary)
        binding.swipeRefresh.setOnRefreshListener { binding.swipeRefresh.isRefreshing = false }
    }

    private fun showCitaOptionsDialog(cita: Cita) {
        if (_binding == null) return
        val esBloqueo = cita.pacienteId == "BLOQUEADO"
        val titulo    = if (esBloqueo) "🔒 ${cita.servicio}" else "${cita.servicio}\n👤 ${cita.pacienteNombre}"
        val notasTxt  = if (cita.notas.isNotEmpty()) "\n📝 ${cita.notas}" else ""
        val detalle   = "📅 ${cita.fecha}  🕐 ${cita.hora}\nEstado: ${estadoLabel(cita.estado)}$notasTxt"

        val builder = MaterialAlertDialogBuilder(requireContext()).setTitle(titulo).setMessage(detalle)

        when (cita.estado) {
            Cita.ESTADO_PENDIENTE -> builder
                .setPositiveButton("✅ Confirmar")    { _, _ -> showDuracionDialog(cita) }
                .setNeutralButton("✏️ Editar")        { _, _ -> showEditarDialog(cita) }
                .setNegativeButton("❌ Cancelar")     { _, _ -> viewModel.cancelarCita(cita.id) }
            Cita.ESTADO_CONFIRMADA -> builder
                .setPositiveButton("🏁 Completar")   { _, _ -> showCompletarDialog(cita) }
                .setNeutralButton("✏️ Editar")        { _, _ -> showEditarDialog(cita) }
                .setNegativeButton("❌ Cancelar")     { _, _ -> viewModel.cancelarCita(cita.id) }
            Cita.ESTADO_CANCELADA -> builder
                .setPositiveButton("🔄 Reactivar")   { _, _ -> viewModel.reactivarCita(cita.id) }
                .setNeutralButton("✏️ Editar notas")  { _, _ -> showEditarDialog(cita) }
                .setNegativeButton("🗑 Eliminar")     { _, _ -> confirmarEliminar(cita) }
            Cita.ESTADO_COMPLETADA -> builder
                .setPositiveButton("✏️ Editar notas") { _, _ -> showEditarDialog(cita) }
                .setNegativeButton("🗑 Eliminar")     { _, _ -> confirmarEliminar(cita) }
            else -> builder.setNegativeButton("Cerrar", null)
        }
        builder.show()
    }

    // Confirmar + elegir duración — usa botones reales para máxima compatibilidad con Huawei
    private fun showDuracionDialog(cita: Cita) {
        if (_binding == null) return
        val maxSlots = 8  // hasta 4 horas (8 x 30min)
        val idx      = Cita.TODOS_LOS_SLOTS.indexOf(cita.hora).takeIf { it >= 0 } ?: 0
        val opciones = buildDuracionOpciones(cita.hora, idx, maxSlots)
        if (opciones.isEmpty()) return

        // Construir layout con RadioButtons — no usa setSingleChoiceItems
        val scroll = android.widget.ScrollView(requireContext())
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 16, 48, 8)
        }
        scroll.addView(container)

        val group = android.widget.RadioGroup(requireContext()).apply {
            orientation = android.widget.RadioGroup.VERTICAL
        }
        val preselected = (cita.duracionSlots - 1).coerceIn(0, maxSlots - 1)
        opciones.forEachIndexed { index, opcion ->
            val rb = android.widget.RadioButton(requireContext()).apply {
                text    = opcion
                id      = index
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
                val checkedId = group.checkedRadioButtonId
                val duracion  = if (checkedId >= 0) checkedId + 1 else cita.duracionSlots
                viewModel.confirmarCita(cita.id, duracion)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Completar + agregar notas finales
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

    // Editar: cambiar estado libre + editar notas
    private fun showEditarDialog(cita: Cita) {
        val estados  = arrayOf("⏳ Pendiente", "✅ Confirmada", "🏁 Completada", "❌ Cancelada")
        val estadosCodigo = arrayOf(Cita.ESTADO_PENDIENTE, Cita.ESTADO_CONFIRMADA, Cita.ESTADO_COMPLETADA, Cita.ESTADO_CANCELADA)
        val actual   = estadosCodigo.indexOf(cita.estado).coerceAtLeast(0)
        var nuevoEstado = cita.estado

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
