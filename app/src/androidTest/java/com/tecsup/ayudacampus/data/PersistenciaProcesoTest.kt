package com.tecsup.ayudacampus.data

import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tecsup.ayudacampus.AyudaApp
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.model.Prioridad
import com.tecsup.ayudacampus.model.Rol
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenciaProcesoTest {
    private val fase = InstrumentationRegistry.getArguments().getString("fase").orEmpty()
    private val app get() = InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as AyudaApp

    @Test
    fun conservaTicketsConversacionYSesionAlAbrirOtroProceso() = runBlocking {
        withTimeout(30_000) { app.lista.first { it } }
        when (fase) {
            "preparar" -> preparar()
            "verificar" -> verificar()
            "" -> { preparar(); verificar() }
            else -> error("Fase desconocida: $fase")
        }
    }

    private suspend fun preparar() {
        borrarFixture()
        val estudiante = requireNotNull(app.repository.obtenerUsuarioPorRol(Rol.CLIENTE))
        val soporte = requireNotNull(app.repository.obtenerUsuarioPorRol(Rol.ADMIN))
        val categoria = app.repository.observarCategorias().first().first()
        val id = app.repository.crearTicket(estudiante.id, categoria.id, TITULO,
            "Incidencia de prueba que debe conservarse al cerrar el proceso.",
            "Laboratorio de verificación", Prioridad.ALTA).toInt()
        app.repository.agregarComentario(id, soporte, COMENTARIO)
        app.repository.cambiarEstado(id, EstadoTicket.EN_PROCESO)
        app.preferencias.guardarSesion(Rol.CLIENTE, estudiante.id)
        app.preferencias.guardarModoOscuro(true)
        app.preferencias.guardarNotificaciones(false)
        app.preferencias.guardarRecibirTickets(false)
        verificar()
    }

    private suspend fun verificar() {
        val estudiante = requireNotNull(app.repository.obtenerUsuarioPorRol(Rol.CLIENTE))
        val soporte = requireNotNull(app.repository.obtenerUsuarioPorRol(Rol.ADMIN))
        val detalle = app.repository.observarTodosLosTickets().first()
            .single { it.ticket.titulo == TITULO }
        assertEquals(estudiante.id, detalle.ticket.clienteId)
        assertEquals(EstadoTicket.EN_PROCESO.name, detalle.ticket.estado)
        assertEquals(Prioridad.ALTA.name, detalle.ticket.prioridad)
        val comentario = detalle.comentarios.single { it.texto == COMENTARIO }
        assertEquals(soporte.id, comentario.autorId)
        assertEquals(soporte.nombre, comentario.autorNombre)
        assertEquals(Rol.ADMIN.name, comentario.autorRol)
        assertEquals(Rol.CLIENTE.name, app.preferencias.perfilActivo.first())
        assertEquals(estudiante.id, app.preferencias.usuarioId.first())
        assertTrue(app.preferencias.modoOscuro.first())
        assertFalse(app.preferencias.notificaciones.first())
        assertFalse(app.preferencias.recibirTickets.first())
    }

    @After
    fun limpiarCuandoCorresponde() = runBlocking {
        if (fase == "preparar") return@runBlocking
        withTimeout(30_000) { app.lista.first { it } }
        borrarFixture()
        app.preferencias.guardarModoOscuro(false)
        app.preferencias.guardarNotificaciones(true)
        app.preferencias.guardarRecibirTickets(true)
        app.preferencias.cerrarSesion()
    }

    private suspend fun borrarFixture() {
        val ids = app.repository.observarTodosLosTickets().first()
            .filter { it.ticket.titulo == TITULO }.map { it.ticket.id }
        app.database.withTransaction {
            ids.forEach { id ->
                app.database.openHelper.writableDatabase.execSQL(
                    "DELETE FROM tickets WHERE id = ?", arrayOf<Any>(id),
                )
            }
        }
    }

    private companion object {
        const val TITULO = "Verificación de persistencia"
        const val COMENTARIO = "La conversación se conserva después de cerrar y abrir la aplicación."
    }
}
