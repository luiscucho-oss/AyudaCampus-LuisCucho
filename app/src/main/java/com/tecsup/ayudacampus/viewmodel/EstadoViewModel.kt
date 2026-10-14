package com.tecsup.ayudacampus.viewmodel

import kotlinx.coroutines.CancellationException

/** Conserva la cancelación de las corrutinas y muestra mensajes útiles al usuario. */
internal fun Throwable.mensajeUsuario(alternativa: String): String {
    if (this is CancellationException) throw this
    return alternativa
}

data class EstadisticasUsuario(
    val abiertos: Int = 0,
    val resueltos: Int = 0,
    val total: Int = 0,
)
