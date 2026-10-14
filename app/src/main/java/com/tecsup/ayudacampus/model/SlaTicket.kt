package com.tecsup.ayudacampus.model

import com.tecsup.ayudacampus.data.local.entity.Ticket

enum class NivelSla { EN_TIEMPO, POR_VENCER, VENCIDO, FINALIZADO }

data class SlaTicket(
    val ticketId: Int,
    val texto: String,
    val detalle: String,
    val nivel: NivelSla,
    val fraccionRestante: Float?,
) {
    companion object {
        fun calcular(ticket: Ticket, ahora: Long): SlaTicket {
            val prioridad = Prioridad.valueOf(ticket.prioridad)
            val horas = when (prioridad) { Prioridad.ALTA -> 4; Prioridad.MEDIA -> 24; Prioridad.BAJA -> 72 }
            val duracion = horas * 3_600_000L
            val detalle = "SLA · ${prioridad.etiqueta} · $horas h"
            if (ticket.estado == EstadoTicket.RESUELTO.name || ticket.estado == EstadoTicket.CERRADO.name) {
                return SlaTicket(ticket.id, if (ticket.estado == EstadoTicket.RESUELTO.name) "Atención resuelta" else "Ticket cerrado",
                    detalle, NivelSla.FINALIZADO, null)
            }
            val restante = ticket.fecha + duracion - ahora
            val vencido = restante <= 0
            val minutos = if (vencido) (-restante / 60_000L) else (restante + 59_999L) / 60_000L
            val tiempo = "${minutos / 60} h ${minutos % 60} min"
            val fraccion = (restante.toDouble() / duracion).coerceIn(0.0, 1.0).toFloat()
            return SlaTicket(ticket.id, if (vencido) "SLA vencido hace $tiempo" else "Quedan $tiempo", detalle,
                when { vencido -> NivelSla.VENCIDO; fraccion <= .25f -> NivelSla.POR_VENCER; else -> NivelSla.EN_TIEMPO }, fraccion)
        }
    }
}
