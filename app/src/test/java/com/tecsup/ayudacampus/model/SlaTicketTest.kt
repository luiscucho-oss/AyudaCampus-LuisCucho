package com.tecsup.ayudacampus.model

import com.tecsup.ayudacampus.data.local.entity.Ticket
import org.junit.Assert.*
import org.junit.Test

class SlaTicketTest {
    private val ticket = Ticket(id = 1, clienteId = 1, categoriaId = 1, titulo = "Prueba", descripcion = "Prueba",
        ubicacion = "Lab", prioridad = "ALTA", fecha = 1_000_000)

    @Test fun prioridadesUsanCuatroVeinticuatroYSetentaYDosHoras() {
        for ((prioridad, horas) in listOf("ALTA" to 4, "MEDIA" to 24, "BAJA" to 72)) {
            val sla = SlaTicket.calcular(ticket.copy(prioridad = prioridad), ticket.fecha)
            assertEquals("Quedan $horas h 0 min", sla.texto)
            assertEquals(1f, requireNotNull(sla.fraccionRestante), .001f)
        }
    }
    @Test fun vencimientoExactoYTiempoVencidoNoMuestranNegativos() {
        assertEquals(NivelSla.VENCIDO, SlaTicket.calcular(ticket, ticket.fecha + 4 * 3_600_000).nivel)
        val vencido = SlaTicket.calcular(ticket, ticket.fecha + 5 * 3_600_000 + 120_000)
        assertEquals("SLA vencido hace 1 h 2 min", vencido.texto)
        assertEquals(0f, requireNotNull(vencido.fraccionRestante), .001f)
    }
    @Test fun ultimoCuartoAdvierteYFraccionesDeMinutoSeRedondeanHaciaArriba() {
        assertEquals(NivelSla.POR_VENCER, SlaTicket.calcular(ticket, ticket.fecha + 3 * 3_600_000).nivel)
        assertEquals("Quedan 0 h 1 min", SlaTicket.calcular(ticket, ticket.fecha + 4 * 3_600_000 - 1).texto)
    }
    @Test fun cambiarPrioridadConservaLaFechaOriginal() {
        val ahora = ticket.fecha + 5 * 3_600_000
        assertEquals(NivelSla.VENCIDO, SlaTicket.calcular(ticket, ahora).nivel)
        assertEquals("Quedan 19 h 0 min", SlaTicket.calcular(ticket.copy(prioridad = "MEDIA"), ahora).texto)
    }
    @Test fun resueltosYCerradosNoCuentanYReabiertoRetomaFechaOriginal() {
        for ((estado, texto) in listOf("RESUELTO" to "Atención resuelta", "CERRADO" to "Ticket cerrado")) {
            val sla = SlaTicket.calcular(ticket.copy(estado = estado), Long.MAX_VALUE / 2)
            assertEquals(texto, sla.texto)
            assertNull(sla.fraccionRestante)
        }
        assertEquals(NivelSla.VENCIDO, SlaTicket.calcular(ticket.copy(estado = "ABIERTO"), ticket.fecha + 5 * 3_600_000).nivel)
    }
}
