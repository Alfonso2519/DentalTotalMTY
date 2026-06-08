package com.dental.totalmty.data.model

data class Servicio(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val duracionMinutos: Int = 30,
    val precio: Double = 0.0,
    val icono: String = ""
) {
    // Cuántos slots de 30 min ocupa este servicio (redondeado hacia arriba)
    val duracionSlots: Int get() = maxOf(1, (duracionMinutos + 29) / 30)

    companion object {
        fun getDefaultServices(): List<Servicio> = listOf(
            Servicio("1", "Consulta General",  "Revisión y diagnóstico dental",      30,  300.0),
            Servicio("2", "Limpieza Dental",   "Profilaxis y limpieza profunda",      60,  600.0),
            Servicio("3", "Extracción",        "Extracción dental simple",            45,  800.0),
            Servicio("4", "Relleno / Empaste", "Restauración dental con resina",      60, 1000.0),
            Servicio("5", "Blanqueamiento",    "Blanqueamiento dental profesional",   90, 2500.0),
            Servicio("6", "Ortodoncia",        "Consulta de ortodoncia",              45,  500.0),
            Servicio("7", "Radiografía",       "Radiografía dental panorámica",       15,  400.0)
        )
    }
}
