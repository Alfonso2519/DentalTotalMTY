package com.dental.totalmty.ui.paciente

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dental.totalmty.R
import com.dental.totalmty.data.model.Cita
import com.dental.totalmty.data.model.Servicio
import com.dental.totalmty.databinding.FragmentNuevaCitaBinding
import com.dental.totalmty.utils.SessionManager
import com.dental.totalmty.viewmodel.CitaState
import com.dental.totalmty.viewmodel.CitasViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class NuevaCitaFragment : Fragment() {

    private var _binding: FragmentNuevaCitaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CitasViewModel by viewModels()

    private val servicios = Servicio.getDefaultServices()
    private var selectedServicio: Servicio? = null
    private var selectedDate: String?       = null
    private var selectedHora: String?       = null
    private var citaEnviada                 = false
    private var slotsOcupados: List<String> = emptyList()
    // Fechas con día completo bloqueado — se usa para mostrar error si el usuario las selecciona
    private var fechasConDiaBloqueado: Set<String> = emptySet()

    // Listener directo a Firestore — sin Flow, sin StateFlow, sin intermediarios
    private var slotListener: ListenerRegistration? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNuevaCitaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupServicioSpinner()
        setupFechaButton()
        setupConfirmarButton()
        observeCitaState()
        cargarFechasBloqueadas()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
    }

    private fun setupServicioSpinner() {
        val nombres = servicios.map { it.nombre }
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_servicio, nombres)
        binding.spinnerServicio.setAdapter(adapter)

        binding.spinnerServicio.setOnItemClickListener { _, _, position, _ ->
            selectedServicio = servicios[position]
            binding.spinnerServicio.setText(selectedServicio!!.nombre, false)
            binding.spinnerServicio.clearFocus()
            binding.tilServicio.boxStrokeColor = ContextCompat.getColor(requireContext(), R.color.success_green)
            binding.tilServicio.hintTextColor  = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.success_green))

            // Mostrar duración estimada del servicio
            val srv = selectedServicio!!
            val mins = srv.duracionMinutos
            val durTxt = when {
                mins < 60  -> "⏱ Duración estimada: $mins min"
                mins == 60 -> "⏱ Duración estimada: 1 hora"
                mins % 60 == 0 -> "⏱ Duración estimada: ${mins/60} horas"
                else -> "⏱ Duración estimada: ${mins/60}h ${mins%60}min"
            }
            binding.tvDuracionServicio.text      = durTxt
            binding.tvDuracionServicio.visibility = View.VISIBLE

            if (selectedDate != null) {
                // Si ya hay fecha seleccionada, mostrar el panel de horarios y re-renderizar
                // (necesario cuando el usuario elige servicio DESPUÉS de la fecha)
                binding.cardHorarios.visibility = View.VISIBLE
                binding.cardNotas.visibility    = View.VISIBLE
                selectedHora = null
                renderTimeSlots(slotsOcupados)
                updateResumen()
            }
        }
        binding.spinnerServicio.setOnClickListener      { binding.spinnerServicio.showDropDown() }
        binding.spinnerServicio.setOnFocusChangeListener { _, has -> if (has) binding.spinnerServicio.showDropDown() }
        binding.tilServicio.setOnClickListener          { binding.spinnerServicio.requestFocus(); binding.spinnerServicio.showDropDown() }
    }

    private fun setupFechaButton() {
        binding.btnSeleccionarFecha.setOnClickListener { mostrarDatePicker() }
    }

    private fun mostrarDatePicker() {
        val hoy = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            R.style.DentalDatePicker,
            { _, year, month, day ->
                if (_binding == null) return@DatePickerDialog
                val cal = Calendar.getInstance().apply { set(year, month, day) }
                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    showError("No hay citas los domingos"); return@DatePickerDialog
                }
                val fechaElegida = String.format("%04d-%02d-%02d", year, month + 1, day)
                if (fechaElegida in fechasConDiaBloqueado) {
                    showError("El día ${day}/${month+1}/$year no tiene citas disponibles")
                    return@DatePickerDialog
                }
                selectedDate = fechaElegida
                selectedHora = null
                citaEnviada  = false
                val legible = try {
                    SimpleDateFormat("EEEE d 'de' MMMM yyyy", Locale("es")).format(cal.time)
                        .replaceFirstChar { it.uppercase() }
                } catch (e: Exception) { selectedDate ?: "" }
                binding.tvFechaSeleccionada.text = "📅 $legible"
                binding.tvFechaSeleccionada.setTextColor(ContextCompat.getColor(requireContext(), R.color.cyan_dark))
                binding.cardHorarios.visibility = View.VISIBLE
                binding.cardNotas.visibility    = View.VISIBLE
                binding.cardResumen.visibility  = View.GONE
                iniciarListenerSlots(selectedDate!!)
            },
            hoy.get(Calendar.YEAR), hoy.get(Calendar.MONTH), hoy.get(Calendar.DAY_OF_MONTH)
        ).apply { datePicker.minDate = hoy.timeInMillis }.show()
    }

    // ── Listener DIRECTO a Firestore — sin Flow ni ViewModel intermediarios ──
    private fun iniciarListenerSlots(fecha: String) {
        // Cancelar listener anterior
        slotListener?.remove()
        slotsOcupados = emptyList()
        binding.flexboxHorarios.removeAllViews()
        binding.progressSlots.visibility = View.VISIBLE

        slotListener = db.collection("citas")
            .whereEqualTo("fecha", fecha)
            .addSnapshotListener { snap, error ->
                if (_binding == null) return@addSnapshotListener
                if (error != null || snap == null) {
                    binding.progressSlots.visibility = View.GONE
                    return@addSnapshotListener
                }
                val ocupadas = mutableListOf<String>()
                for (doc in snap.documents) {
                    val estado     = doc.getString("estado") ?: continue
                    if (estado != Cita.ESTADO_PENDIENTE && estado != Cita.ESTADO_CONFIRMADA) continue
                    val pacienteId = doc.getString("pacienteId") ?: ""
                    val hora       = doc.getString("hora")       ?: continue
                    val duracion   = when (pacienteId) {
                        Cita.PACIENTE_DIA_BLOQUEADO -> Cita.TODOS_LOS_SLOTS.size
                        else -> doc.getLong("duracionSlots")?.toInt() ?: 1
                    }
                    ocupadas.addAll(Cita.calcularSlotsOcupados(hora, duracion))
                }
                slotsOcupados = ocupadas.distinct()
                renderTimeSlots(slotsOcupados)

                // Si el slot seleccionado quedó ocupado, limpiarlo
                val hora = selectedHora
                if (hora != null) {
                    val dur = selectedServicio?.duracionSlots ?: 1
                    if (Cita.calcularSlotsOcupados(hora, dur).any { it in slotsOcupados }) {
                        selectedHora = null
                        updateResumen()
                        showError("El horario $hora acaba de ser ocupado. Elige otro.")
                    } else {
                        highlightSelectedSlot()
                    }
                }
            }
    }

    private fun renderTimeSlots(horasOcupadas: List<String>) {
        if (_binding == null) return
        binding.progressSlots.visibility = View.GONE
        binding.flexboxHorarios.removeAllViews()

        val slotsNecesarios = selectedServicio?.duracionSlots ?: 1

        Cita.TODOS_LOS_SLOTS.forEach { hora ->
            val isOcupado = hora in horasOcupadas ||
                Cita.calcularSlotsOcupados(hora, slotsNecesarios).any { it in horasOcupadas }

            val tv = TextView(requireContext()).apply {
                text     = hora
                textSize = 13f
                gravity  = android.view.Gravity.CENTER
                setPadding(24, 18, 24, 18)

                if (isOcupado) {
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_slot_occupied)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.slot_occupied_text))
                    isEnabled       = false
                    isClickable     = false
                    isFocusable     = false
                    isLongClickable = false
                    alpha           = 0.5f
                } else {
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_slot_available)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.slot_available_text))
                    isEnabled = true
                    isClickable = true
                    isFocusable = true
                    alpha = 1.0f
                    setOnClickListener {
                        if (_binding == null) return@setOnClickListener
                        // Guard final contra race condition
                        if (Cita.calcularSlotsOcupados(hora, slotsNecesarios).any { it in slotsOcupados }) {
                            showError("Ese horario ya fue tomado, elige otro")
                            return@setOnClickListener
                        }
                        selectedHora = hora
                        highlightSelectedSlot()
                        updateResumen()
                    }
                }

                layoutParams = com.google.android.flexbox.FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(6, 6, 6, 6) }
            }
            binding.flexboxHorarios.addView(tv)
        }
    }

    private fun highlightSelectedSlot() {
        if (_binding == null) return
        for (i in 0 until binding.flexboxHorarios.childCount) {
            val child = binding.flexboxHorarios.getChildAt(i) as? TextView ?: continue
            if (!child.isEnabled) continue
            if (child.text.toString() == selectedHora) {
                child.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.cyan_primary))
                child.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                child.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_slot_available)
                child.setTextColor(ContextCompat.getColor(requireContext(), R.color.slot_available_text))
            }
        }
    }

    private fun updateResumen() {
        if (_binding == null) return
        if (selectedServicio != null && selectedDate != null && selectedHora != null) {
            binding.cardResumen.visibility = View.VISIBLE
            val fechaFormat = try {
                SimpleDateFormat("EEEE d 'de' MMMM yyyy", Locale("es"))
                    .format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate!!) ?: Date())
                    .replaceFirstChar { it.uppercase() }
            } catch (e: Exception) { selectedDate ?: "" }
            binding.tvResumenServicio.text = "📋 ${selectedServicio!!.nombre}"
            binding.tvResumenFecha.text    = "📅 $fechaFormat"
            binding.tvResumenHora.text     = "🕐 $selectedHora"
        } else {
            binding.cardResumen.visibility = View.GONE
        }
    }

    private fun setupConfirmarButton() {
        binding.btnConfirmar.setOnClickListener {
            if (citaEnviada) return@setOnClickListener
            when {
                selectedServicio == null -> showError("Selecciona un servicio")
                selectedDate     == null -> showError("Selecciona una fecha")
                selectedHora     == null -> showError("Selecciona un horario disponible")
                else -> {
                    val slotsNecesarios  = selectedServicio!!.duracionSlots
                    val slotsQueOcuparía = Cita.calcularSlotsOcupados(selectedHora!!, slotsNecesarios)
                    if (slotsQueOcuparía.any { it in slotsOcupados }) {
                        showError("Ese horario ya no está disponible, elige otro")
                        selectedHora = null
                        renderTimeSlots(slotsOcupados)
                        updateResumen()
                        return@setOnClickListener
                    }
                    enviarCita()
                }
            }
        }
    }

    private fun enviarCita() {
        val usuario = SessionManager.usuario ?: run { showError("No hay sesión activa"); return }
        citaEnviada = true
        viewModel.agendarCita(Cita(
            pacienteId       = usuario.uid,
            pacienteNombre   = usuario.nombre,
            pacienteTelefono = usuario.telefono,
            fecha            = selectedDate!!,
            hora             = selectedHora!!,
            servicio         = selectedServicio!!.nombre,
            duracionSlots    = selectedServicio!!.duracionSlots,
            notas            = binding.etNotas.text.toString().trim()
        ))
    }

    private fun observeCitaState() {
        viewModel.citaState.observe(viewLifecycleOwner) { state ->
            if (_binding == null) return@observe
            when (state) {
                is CitaState.Loading -> {
                    binding.btnConfirmar.isEnabled = false
                    binding.btnConfirmar.text      = "Agendando..."
                }
                is CitaState.Success -> {
                    binding.btnConfirmar.isEnabled = true
                    binding.btnConfirmar.text      = "Confirmar Cita"
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("¡Cita Agendada!")
                        .setMessage("Tu cita de ${selectedServicio?.nombre} fue agendada para el $selectedDate a las $selectedHora.")
                        .setPositiveButton("Entendido") { _, _ -> findNavController().navigateUp() }
                        .show()
                    viewModel.resetState()
                }
                is CitaState.Error -> {
                    citaEnviada                    = false
                    binding.btnConfirmar.isEnabled = true
                    binding.btnConfirmar.text      = "Confirmar Cita"
                    showError(state.message)
                    viewModel.resetState()
                }
                else -> {
                    binding.btnConfirmar.isEnabled = true
                    binding.btnConfirmar.text      = "Confirmar Cita"
                }
            }
        }
    }

    private fun showError(msg: String) {
        if (_binding == null) return
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_red)).show()
    }

    // Carga las fechas que tienen día completo bloqueado por el doctor
    private fun cargarFechasBloqueadas() {
        db.collection("citas")
            .whereEqualTo("pacienteId", Cita.PACIENTE_DIA_BLOQUEADO)
            .whereEqualTo("estado", Cita.ESTADO_CONFIRMADA)
            .get()
            .addOnSuccessListener { snap ->
                fechasConDiaBloqueado = snap.documents
                    .mapNotNull { it.getString("fecha") }
                    .toSet()
            }
    }

    override fun onDestroyView() {
        slotListener?.remove()
        slotListener = null
        _binding = null
        super.onDestroyView()
    }
}
