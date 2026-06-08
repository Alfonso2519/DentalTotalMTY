package com.dental.totalmty.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Usuario(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val rol: String = ROL_PACIENTE,   // "paciente" o "doctor"
    val fotoPerfil: String = "",
    val fechaRegistro: Long = System.currentTimeMillis(),
    val activo: Boolean = true
) : Parcelable {
    companion object {
        const val ROL_PACIENTE = "paciente"
        const val ROL_DOCTOR = "doctor"
    }
}
