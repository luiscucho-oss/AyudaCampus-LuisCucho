package com.tecsup.ayudacampus.viewmodel

import com.tecsup.ayudacampus.data.local.entity.*
import com.tecsup.ayudacampus.data.local.relation.TicketConDetalle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlujosSlaTest {
    @Test fun relojActualizaTodasLasTarjetasSinEscriturasYSeDetieneSinObservadores() = runTest {
        val detalle = TicketConDetalle(Ticket(id = 1, clienteId = 1, categoriaId = 1, titulo = "Prueba", descripcion = "Prueba",
            ubicacion = "Lab", prioridad = "ALTA", fecha = 0), emptyList(), Categoria(1, "Red", "wifi"),
            Usuario(1, "Alumno", "a@campus.test", "1234", "CLIENTE"))
        var lecturas = 0
        val lista = MutableStateFlow(listOf(detalle, detalle.copy(ticket = detalle.ticket.copy(id = 2))))
        val sla = lista.slaParaLista(backgroundScope, relojSla { lecturas++; testScheduler.currentTime })
        val observador = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sla.collect {} }
        runCurrent()
        assertEquals(1, lecturas)
        assertEquals(2, sla.value.size)
        advanceTimeBy(60_000); runCurrent()
        assertEquals("Quedan 3 h 59 min", sla.value[1]?.texto)
        assertEquals(sla.value[1]?.texto, sla.value[2]?.texto)
        assertEquals(2, lecturas)
        observador.cancel()
        advanceTimeBy(5_001); runCurrent()
        val alDetener = lecturas
        advanceTimeBy(120_000); runCurrent()
        assertEquals(alDetener, lecturas)
    }
}
