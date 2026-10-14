package com.tecsup.ayudacampus.model

import kotlin.math.roundToLong

data class PromedioResolucion(val valor: String, val descripcion: String) {
    companion object {
        val Cargando = PromedioResolucion("—", "Calculando el promedio…")

        fun desde(milisegundos: Double?): PromedioResolucion {
            if (milisegundos == null) return PromedioResolucion("—", "Sin tickets cerrados")
            val minutos = (milisegundos / 60_000).roundToLong().coerceAtLeast(0)
            val texto = when {
                milisegundos == 0.0 -> "0 min"
                minutos == 0L -> "< 1 min"
                minutos < 60 -> "$minutos min"
                else -> "${minutos / 60} h ${minutos % 60} min"
            }
            return PromedioResolucion(texto, "Desde la creación hasta el cierre del ticket")
        }
    }
}
