package com.dental.totalmty.ui.doctor

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dental.totalmty.R
import com.dental.totalmty.data.model.Cita
import com.dental.totalmty.databinding.FragmentDoctorSettingsBinding
import com.dental.totalmty.viewmodel.CitaState
import com.dental.totalmty.viewmodel.CitasViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class DoctorSettingsFragment : Fragment() {

    private var _binding: FragmentDoctorSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CitasViewModel by viewModels()

    private var fechaSeleccionada: String? = null
    private var horaSeleccionada: String?  = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDoctorSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.btnSeleccionarFechaBloqueo.setOnClickListener {
            val hoy = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                R.style.DentalDatePicker,
                { _, year, month, day ->
                    fechaSeleccionada = String.format("%04d-%02d-%02d", year, month + 1, day)
                    val cal = Calendar.getInstance().apply { set(year, month, day) }
                    binding.tvFechaBloqueo.text = "📅 " + SimpleDateFormat("EEE d 'de' MMMM yyyy", Locale("es"))
                        .format(cal.time).replaceFirstChar { it.uppercase() }
                    cargarSlotsParaFecha()
                },
                hoy.get(Calendar.YEAR), hoy.get(Calendar.MONTH), hoy.get(Calendar.DAY_OF_MONTH)
            ).apply { datePicker.minDate = hoy.timeInMillis }.show()
        }

        binding.btnBloquear.setOnClickListener {
            val fecha = fechaSeleccionada ?: run { showError("Selecciona una fecha primero"); return@setOnClickListener }
            val hora  = horaSeleccionada  ?: run { showError("Selecciona un horario"); return@setOnClickListener }
            showBloquearDialog(fecha, hora)
        }

        binding.btnBloquearDia.setOnClickListener {
            val hoy = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                R.style.DentalDatePicker,
                { _, year, month, day ->
                    val fecha = String.format("%04d-%02d-%02d", year, month + 1, day)
                    val cal   = Calendar.getInstance().apply { set(year, month, day) }
                    val legible = SimpleDateFormat("EEEE d 'de' MMMM yyyy", Locale("es"))
                        .format(cal.time).replaceFirstChar { it.uppercase() }
                    showBloquearDiaDialog(fecha, legible)
                },
                hoy.get(Calendar.YEAR), hoy.get(Calendar.MONTH), hoy.get(Calendar.DAY_OF_MONTH)
            ).apply { datePicker.minDate = hoy.timeInMillis }.show()
        }

        setupBloqueosRecycler()

        viewModel.citaState.observe(viewLifecycleOwner) { state ->
            if (_binding == null) return@observe
            when (state) {
                is CitaState.Success -> {
                    showSuccess(state.message)
                    fechaSeleccionada = null
                    horaSeleccionada  = null
                    binding.tvFechaBloqueo.text = "Toca para seleccionar fecha"
                    binding.flexboxSlotsBloqueo.removeAllViews()
                    viewModel.resetState()
                }
                is CitaState.Error -> { showError(state.message); viewModel.resetState() }
                else -> {}
            }
        }
    }

    private fun cargarSlotsParaFecha() {
        val fecha = fechaSeleccionada ?: return
        horaSeleccionada = null
        binding.flexboxSlotsBloqueo.removeAllViews()
        // cargarHorasOcupadas actualiza el Flow → el observer recibe datos en tiempo real
        viewModel.cargarHorasOcupadas(fecha)
        viewModel.horasOcupadas.removeObservers(viewLifecycleOwner)
        viewModel.horasOcupadas.observe(viewLifecycleOwner) { ocupadas ->
            if (_binding == null) return@observe
            renderSlots(ocupadas)
        }
    }

    private fun renderSlots(ocupadas: List<String>) {
        if (_binding == null) return
        binding.flexboxSlotsBloqueo.removeAllViews()
        Cita.TODOS_LOS_SLOTS.forEach { hora ->
            val isOcupado = hora in ocupadas
            val tv = TextView(requireContext()).apply {
                text     = hora
                textSize = 13f
                setPadding(20, 16, 20, 16)
                isEnabled  = !isOcupado
                isClickable = !isOcupado
                background = ContextCompat.getDrawable(requireContext(),
                    if (isOcupado) R.drawable.bg_slot_occupied else R.drawable.bg_slot_available)
                setTextColor(ContextCompat.getColor(requireContext(),
                    if (isOcupado) R.color.slot_occupied_text else R.color.slot_available_text))
                layoutParams = com.google.android.flexbox.FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(6, 6, 6, 6) }
            }
            if (!isOcupado) {
                tv.setOnClickListener { horaSeleccionada = hora; highlightSlot(hora) }
            }
            binding.flexboxSlotsBloqueo.addView(tv)
        }
    }

    private fun highlightSlot(horaElegida: String) {
        for (i in 0 until binding.flexboxSlotsBloqueo.childCount) {
            val child = binding.flexboxSlotsBloqueo.getChildAt(i) as? TextView ?: continue
            if (!child.isEnabled) continue
            if (child.text.toString() == horaElegida) {
                child.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.cyan_primary))
                child.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                child.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_slot_available)
                child.setTextColor(ContextCompat.getColor(requireContext(), R.color.slot_available_text))
            }
        }
    }

        private fun showBloquearDialog(fecha: String, hora: String) {
        val indexInicio = Cita.TODOS_LOS_SLOTS.indexOf(hora).takeIf { it >= 0 } ?: 0
        val maxSlots    = minOf(Cita.TODOS_LOS_SLOTS.size - indexInicio, 8)
        val opciones    = buildDuracionOpciones(hora, indexInicio, maxSlots)
        if (opciones.isEmpty()) return

        val scroll    = android.widget.ScrollView(requireContext())
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 16, 48, 8)
        }
        scroll.addView(container)

        val group = android.widget.RadioGroup(requireContext()).apply {
            orientation = android.widget.RadioGroup.VERTICAL
        }
        opciones.forEachIndexed { index, opcion ->
            val rb = android.widget.RadioButton(requireContext()).apply {
                text      = opcion
                id        = index
                textSize  = 15f
                setPadding(8, 16, 8, 16)
                isChecked = (index == 0)
                setTextColor(android.graphics.Color.parseColor("#1C2B3A"))
            }
            group.addView(rb)
        }
        container.addView(group)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🔒 ¿Cuánto tiempo bloquear?")
            .setMessage("Fecha: $fecha  |  Inicio: $hora")
            .setView(scroll)
            .setPositiveButton("Siguiente") { _, _ ->
                val elegido = if (group.checkedRadioButtonId >= 0) group.checkedRadioButtonId + 1 else 1
                // Paso 2: motivo opcional
                val input = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
                    hint = "Ej. Almuerzo, Reunión, Descanso..."
                    setPadding(48, 32, 48, 16)
                }
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Motivo (opcional)")
                    .setView(input)
                    .setPositiveButton("Bloquear") { _, _ ->
                        viewModel.bloquearSlot(fecha, hora, elegido, input.text.toString())
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    private fun setupBloqueosRecycler() {
        val bloqueosAdapter = BloqueoAdapter(emptyList()) { citaId ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Desbloquear esta hora?")
                .setPositiveButton("Sí, desbloquear") { _, _ -> viewModel.desbloquearSlot(citaId) }
                .setNegativeButton("Cancelar", null).show()
        }
        binding.rvBloqueos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBloqueos.adapter = bloqueosAdapter

        viewModel.getBloqueos().observe(viewLifecycleOwner) { bloqueos ->
            if (_binding == null) return@observe
            binding.tvBloqueosVacios.visibility = if (bloqueos.isEmpty()) View.VISIBLE else View.GONE
            binding.rvBloqueos.visibility       = if (bloqueos.isEmpty()) View.GONE    else View.VISIBLE
            bloqueosAdapter.updateItems(bloqueos)
        }
    }

    // ── Bloquear / desbloquear día completo ───────────────────────────────
    private fun showBloquearDiaDialog(fecha: String, legible: String) {
        val opcion  = intArrayOf(0)
        val opciones = arrayOf<CharSequence>("🔒 Bloquear todo el día", "🔓 Desbloquear todo el día")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("📅 $legible")
            .setSingleChoiceItems(opciones, 0) { _, which -> opcion[0] = which }
            .setPositiveButton("Continuar") { _, _ ->
                if (opcion[0] == 0) {
                    val input = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
                        hint = "Ej. Vacaciones, Congreso, Día libre..."
                        setPadding(48, 32, 48, 16)
                    }
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("🔒 Motivo (opcional)")
                        .setView(input)
                        .setPositiveButton("Bloquear día completo") { _, _ ->
                            bloquearDiaCompleto(fecha, legible, input.text.toString().trim())
                        }
                        .setNegativeButton("Cancelar", null).show()
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("🔓 Desbloquear $legible")
                        .setMessage("¿Desbloquear todas las horas de este día?\n\nSolo se eliminarán los bloqueos del doctor, no las citas de pacientes.")
                        .setPositiveButton("Desbloquear") { _, _ -> desbloquearDiaCompleto(fecha) }
                        .setNegativeButton("Cancelar", null).show()
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun bloquearDiaCompleto(fecha: String, legible: String, motivo: String) {
        val db      = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val label   = motivo.ifBlank { "Día bloqueado" }
        // Un único documento representa el bloqueo del día completo.
        // ID: "dia_FECHA" → no colisiona con los slots individuales "FECHA_HORA"
        // La agenda lo mostrará como una sola tarjeta "Día bloqueado"
        // El snapshotListener del paciente lo expandirá a todos los slots
        val docId  = "dia_$fecha"
        val docRef = db.collection("citas").document(docId)
        val cita   = Cita(
            id             = docId,
            pacienteId     = Cita.PACIENTE_DIA_BLOQUEADO,
            pacienteNombre = label,
            fecha          = fecha,
            hora           = Cita.TODOS_LOS_SLOTS.first(),
            servicio       = "🚫 $label",
            estado         = Cita.ESTADO_CONFIRMADA,
            duracionSlots  = Cita.TODOS_LOS_SLOTS.size,  // toda la jornada
            notas          = "Día completo bloqueado: $legible"
        )
        docRef.set(cita)
            .addOnSuccessListener { showSuccess("✅ Día completo bloqueado") }
            .addOnFailureListener { showError("Error: ${it.message}") }
    }

    private fun desbloquearDiaCompleto(fecha: String) {
        val db       = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val citasRef = db.collection("citas")
        // Buscar el documento de día completo Y cualquier bloqueo individual
        citasRef.whereEqualTo("fecha", fecha)
            .whereEqualTo("pacienteId", Cita.PACIENTE_DIA_BLOQUEADO)
            .get()
            .addOnSuccessListener { snap1 ->
                citasRef.whereEqualTo("fecha", fecha)
                    .whereEqualTo("pacienteId", "BLOQUEADO")
                    .get()
                    .addOnSuccessListener { snap2 ->
                        val batch = db.batch()
                        snap1.documents.forEach { batch.delete(it.reference) }
                        snap2.documents.forEach { batch.delete(it.reference) }
                        val total = snap1.size() + snap2.size()
                        if (total == 0) { showError("No hay bloqueos en ese día"); return@addOnSuccessListener }
                        batch.commit()
                            .addOnSuccessListener { showSuccess("✅ Día desbloqueado correctamente") }
                            .addOnFailureListener { showError("Error: ${it.message}") }
                    }
            }
            .addOnFailureListener { showError("Error: ${it.message}") }
    }

    private fun showError(msg: String) {
        if (_binding == null) return
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_red)).show()
    }

    private fun showSuccess(msg: String) {
        if (_binding == null) return
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.success_green)).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── Adapter para bloqueos ─────────────────────────────────────────────────────
class BloqueoAdapter(
    private var items: List<Cita>,
    private val onDesbloquear: (String) -> Unit
) : RecyclerView.Adapter<BloqueoAdapter.VH>() {

    fun updateItems(newItems: List<Cita>) { items = newItems; notifyDataSetChanged() }

    inner class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(48, 28, 48, 28)
            textSize = 14f
            setTextColor(ContextCompat.getColor(parent.context, R.color.text_primary))
            background = ContextCompat.getDrawable(parent.context, R.drawable.bg_card_white)
        }
        return VH(tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val b      = items[position]
        val mins   = b.duracionSlots * 30
        val durStr = if (mins < 60) "$mins min" else "${mins / 60}h${if (mins % 60 > 0) " ${mins % 60}min" else ""}"
        holder.tv.text = "🔒  ${b.fecha}  •  ${b.hora}  •  $durStr\n     ${b.servicio.ifBlank { "Hora bloqueada" }}"
        holder.tv.setOnClickListener { onDesbloquear(b.id) }
    }

    override fun getItemCount() = items.size
}
