package com.dental.totalmty.utils

import com.dental.totalmty.data.model.Usuario

object SessionManager {
    var usuario: Usuario? = null

    fun clear() {
        usuario = null
    }
}
