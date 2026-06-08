package com.dental.totalmty.ui.doctor

import com.dental.totalmty.data.model.Cita
import java.text.SimpleDateFormat
import java.util.*

// Genera opciones de duración desde 30 min hasta maxSlots*30 min.
// Calcula la hora de fin sumando minutos a horaInicio, sin depender del array de slots.
fun buildDuracionOpciones(hora: String, indexInicio: Int, maxSlots: Int): Array<CharSequence> {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val base = try { sdf.parse(hora) ?: return emptyArray() } catch (e: Exception) { return emptyArray() }

    return (1..maxSlots).map { slots ->
        val minutos = slots * 30
        val cal = Calendar.getInstance().apply {
            time = base
            add(Calendar.MINUTE, minutos)
        }
        val horaFin = sdf.format(cal.time)
        when {
            minutos < 60  -> "$minutos min  ($hora – $horaFin)"
            minutos == 60 -> "1 hora  ($hora – $horaFin)"
            minutos % 60 == 0 -> "${minutos / 60} horas  ($hora – $horaFin)"
            else          -> "${minutos / 60}h ${minutos % 60}min  ($hora – $horaFin)"
        } as CharSequence
    }.toTypedArray()
}
