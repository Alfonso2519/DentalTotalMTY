package com.dental.totalmty.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
data class Cita(
    val id: String = "",
    val pacienteId: String = "",
    val pacienteNombre: String = "",
    val pacienteTelefono: String = "",
    val doctorId: String = "",
    val fecha: String = "",
    val hora: String = "",
    val servicio: String = "",
    val estado: String = ESTADO_PENDIENTE,
    val notas: String = "",
    val fechaCreacion: String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
    val reminderSent: Boolean = false,
    val duracionSlots: Int = 1
) : Parcelable {
    companion object {
        const val ESTADO_PENDIENTE  = "pendiente"
        const val ESTADO_CONFIRMADA = "confirmada"
        const val ESTADO_CANCELADA  = "cancelada"
        const val ESTADO_COMPLETADA = "completada"
        // ID especial para bloqueo de día completo (un solo documento en Firestore)
        const val PACIENTE_DIA_BLOQUEADO = "DIA_COMPLETO_BLOQUEADO"

        val TODOS_LOS_SLOTS = listOf(
            "09:00","09:30","10:00","10:30","11:00","11:30",
            "12:00","12:30","13:00","13:30","14:00","14:30",
            "15:00","15:30","16:00","16:30","17:00","17:30"
        )

        // Retorna los slots que ocupa una cita HACIA ADELANTE desde horaInicio
        // Si horaInicio no existe en el array devuelve solo esa hora (no bloquea nada extra)
        fun calcularSlotsOcupados(horaInicio: String, duracionSlots: Int): List<String> {
            val idx = TODOS_LOS_SLOTS.indexOf(horaInicio)
            if (idx < 0) return listOf(horaInicio)
            // Solo hacia adelante: idx, idx+1, ..., idx+duracionSlots-1
            return TODOS_LOS_SLOTS.subList(idx, minOf(idx + duracionSlots, TODOS_LOS_SLOTS.size))
        }
    }
}
