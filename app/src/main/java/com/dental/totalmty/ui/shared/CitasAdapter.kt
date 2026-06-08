package com.dental.totalmty.ui.shared

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dental.totalmty.R
import com.dental.totalmty.data.model.Cita
import com.dental.totalmty.databinding.ItemCitaBinding
import java.text.SimpleDateFormat
import java.util.*

class CitasAdapter(
    private val onCitaClick: (Cita) -> Unit,
    private val showDoctorName: Boolean = false
) : ListAdapter<Cita, CitasAdapter.CitaViewHolder>(CitaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitaViewHolder {
        val binding = ItemCitaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CitaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CitaViewHolder(private val binding: ItemCitaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(cita: Cita) {
            val esDiaBloqueado = cita.pacienteId == Cita.PACIENTE_DIA_BLOQUEADO
            val esBloqueoHora  = cita.pacienteId == "BLOQUEADO"

            binding.tvServicio.text = when {
                esDiaBloqueado -> "🚫 Día completo bloqueado"
                esBloqueoHora  -> "🔒 ${cita.servicio}"
                else           -> cita.servicio
            }

            val fechaFormateada = try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(cita.fecha)
                SimpleDateFormat("EEE d MMM", Locale("es")).format(date ?: Date())
                    .replaceFirstChar { it.uppercase() }
            } catch (e: Exception) { cita.fecha }

            binding.tvFechaHora.text = when {
                esDiaBloqueado -> "$fechaFormateada · Todo el día"
                else           -> "$fechaFormateada · ${cita.hora}"
            }

            if (esDiaBloqueado || esBloqueoHora) {
                binding.tvPacienteNombre.visibility = View.VISIBLE
                binding.tvPacienteNombre.text = "📝 ${cita.pacienteNombre}"
            } else if (showDoctorName && cita.pacienteNombre.isNotEmpty()) {
                binding.tvPacienteNombre.visibility = View.VISIBLE
                binding.tvPacienteNombre.text = "👤 ${cita.pacienteNombre}"
            } else {
                binding.tvPacienteNombre.visibility = View.GONE
            }

            // Estado: texto + color de texto + fondo del chip + barra lateral
            val ctx = binding.root.context
            data class Style(val texto: String, val textRes: Int, val bgRes: Int, val barRes: Int)
            val s = when {
                esDiaBloqueado -> Style("Bloqueado", R.color.gray_dark, R.color.gray_light, R.color.gray_medium)
                esBloqueoHora  -> Style("Bloqueado", R.color.gray_dark, R.color.gray_light, R.color.gray_medium)
                cita.estado == Cita.ESTADO_CONFIRMADA -> Style("Confirmada", R.color.status_confirmed, R.color.status_confirmed_bg, R.color.status_confirmed)
                cita.estado == Cita.ESTADO_CANCELADA  -> Style("Cancelada",  R.color.status_cancelled, R.color.status_cancelled_bg, R.color.status_cancelled)
                cita.estado == Cita.ESTADO_COMPLETADA -> Style("Completada", R.color.status_completed, R.color.status_completed_bg, R.color.status_completed)
                else -> Style("Pendiente", R.color.status_pending, R.color.status_pending_bg, R.color.status_pending)
            }
            binding.tvEstado.text = s.texto
            binding.tvEstado.setTextColor(ContextCompat.getColor(ctx, s.textRes))
            binding.tvEstado.setBackgroundColor(ContextCompat.getColor(ctx, s.bgRes))
            binding.viewStatusBar.setBackgroundColor(ContextCompat.getColor(ctx, s.barRes))

            binding.root.setOnClickListener {
                it.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
                    .withEndAction { it.animate().scaleX(1f).scaleY(1f).setDuration(80).start() }
                    .start()
                onCitaClick(cita)
            }
        }
    }

    fun attachSwipeToCancel(recyclerView: RecyclerView, onSwipe: (Cita) -> Unit) {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                if (pos == RecyclerView.NO_ID.toInt()) return
                val cita = getItem(pos)
                if (cita.estado == Cita.ESTADO_CANCELADA || cita.estado == Cita.ESTADO_COMPLETADA) {
                    notifyItemChanged(pos); return
                }
                onSwipe(cita)
            }

            override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int {
                val pos = vh.bindingAdapterPosition
                if (pos == RecyclerView.NO_ID.toInt()) return 0
                val cita = getItem(pos)
                return if (cita.estado == Cita.ESTADO_CANCELADA || cita.estado == Cita.ESTADO_COMPLETADA) 0
                else super.getSwipeDirs(rv, vh)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }
}

class CitaDiffCallback : DiffUtil.ItemCallback<Cita>() {
    override fun areItemsTheSame(oldItem: Cita, newItem: Cita) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Cita, newItem: Cita) = oldItem == newItem
}
