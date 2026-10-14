package com.tecsup.ayudacampus.model

import com.tecsup.ayudacampus.data.local.relation.TicketConDetalle
import java.text.Normalizer
import java.util.Locale

data class FiltrosBandeja(
    val estado: EstadoTicket? = null,
    val prioridad: Prioridad? = null,
    val categoriaId: Int? = null,
    val busqueda: String = "",
) {
    val activos: Int get() = listOf(estado != null, prioridad != null, categoriaId != null, busqueda.isNotBlank()).count { it }

    fun aplicar(lista: List<TicketConDetalle>): List<TicketConDetalle> {
        val consulta = normalizar(busqueda.trim().removePrefix("#"))
        return lista.filter { detalle ->
            val t = detalle.ticket
            (estado == null || t.estado == estado.name) &&
                (prioridad == null || t.prioridad == prioridad.name) &&
                (categoriaId == null || t.categoriaId == categoriaId) &&
                (consulta.isBlank() || normalizar(t.titulo).contains(consulta) ||
                    normalizar(detalle.cliente.nombre).contains(consulta) ||
                    t.id.toString().contains(consulta) || t.id.toString().padStart(4, '0').contains(consulta))
        }
    }

    private fun normalizar(texto: String): String = Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
}
