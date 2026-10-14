package com.tecsup.ayudacampus.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tecsup.ayudacampus.data.local.AyudaDatabase
import com.tecsup.ayudacampus.data.local.DatosIniciales
import com.tecsup.ayudacampus.data.local.entity.Categoria
import com.tecsup.ayudacampus.data.local.entity.Ticket
import com.tecsup.ayudacampus.data.local.relation.TicketConDetalle
import com.tecsup.ayudacampus.data.repository.AyudaRepositoryRoom
import com.tecsup.ayudacampus.data.repository.CategoriaEnUsoException
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.model.Prioridad
import com.tecsup.ayudacampus.model.Rol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {
    private lateinit var db: AyudaDatabase
    private lateinit var repo: AyudaRepositoryRoom

    @Before
    fun preparar() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AyudaDatabase::class.java,
        ).build()
        repo = AyudaRepositoryRoom(db)
        DatosIniciales.sembrar(db)
    }

    @After
    fun cerrarBase() {
        db.close()
    }

    @Test
    fun siembraEsCompletaEIdempotente() = runBlocking {
        val antes = repo.observarTodosLosTickets().first()
        DatosIniciales.sembrar(db)
        DatosIniciales.sembrar(db)
        val despues = repo.observarTodosLosTickets().first()

        assertEquals(2, db.usuarioDao().contar())
        assertEquals(5, db.categoriaDao().contar())
        assertEquals(6, despues.size)
        assertEquals(4, despues.sumOf { it.comentarios.size })
        assertEquals(antes, despues)
        assertEquals(EstadoTicket.entries.map { it.name }.toSet(), despues.map { it.ticket.estado }.toSet())
        assertEquals(Prioridad.entries.map { it.name }.toSet(), despues.map { it.ticket.prioridad }.toSet())
        assertEquals("Juan Pérez", repo.login("juan@campus.test", "1234")?.nombre)
        assertEquals(Rol.ADMIN.name, repo.login("soporte@campus.test", "admin")?.rol)
        assertNull(repo.login("juan@campus.test", "incorrecta"))
    }

    @Test
    fun clavesForaneasRechazanTicketsHuerfanos() = runBlocking {
        for (ticket in listOf(
            ticketPrueba(clienteId = 999, categoriaId = 1),
            ticketPrueba(clienteId = 1, categoriaId = 999),
        )) {
            try {
                db.ticketDao().insertarTicket(ticket)
                fail("Se permitió guardar un ticket sin cliente o categoría existente")
            } catch (_: SQLiteConstraintException) {
                // La restricción de SQLite mantiene la integridad de las relaciones.
            }
        }
        assertEquals(6, repo.observarTodosLosTickets().first().size)
    }

    @Test
    fun categoriaConTicketsNoSeEliminaPeroPuedeDesactivarse() = runBlocking {
        val categoria = requireNotNull(repo.obtenerCategoria(1))
        try {
            repo.eliminarCategoria(categoria)
            fail("Se eliminó una categoría que todavía tiene tickets")
        } catch (error: CategoriaEnUsoException) {
            assertTrue(error.message.orEmpty().contains("tickets asociados"))
        }

        assertNotNull(repo.obtenerCategoria(1))
        repo.cambiarActiva(1, false)
        assertFalse(requireNotNull(repo.obtenerCategoria(1)).activa)
        assertFalse(repo.observarCategoriasActivas().first().any { it.id == 1 })
        assertTrue(repo.observarTodosLosTickets().first().any { it.ticket.categoriaId == 1 })

        repo.guardarCategoria(Categoria(nombre = "Sin tickets", icono = "tag"))
        val nueva = repo.observarCategorias().first().single { it.nombre == "Sin tickets" }
        repo.eliminarCategoria(nueva)
        assertNull(repo.obtenerCategoria(nueva.id))
    }

    @Test
    fun borrarTicketEliminaComentariosEnCascada() = runBlocking {
        assertEquals(3, requireNotNull(repo.observarTicket(12).first()).comentarios.size)
        db.openHelper.writableDatabase.execSQL("DELETE FROM tickets WHERE id = 12")

        assertNull(repo.observarTicket(12).first())
        db.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM comentarios WHERE ticketId = 12",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        assertEquals(1, repo.observarTodosLosTickets().first().sumOf { it.comentarios.size })
        assertEquals(5, repo.observarTodosLosTickets().first().size)
    }

    @Test
    fun panelIncluyeCategoriasSinTicketsYCuentaAlertasPorAntiguedad() = runBlocking {
        repo.guardarCategoria(Categoria(nombre = "Proyectores", icono = "pc"))
        val conteos = repo.observarCantidadPorCategoria().first()
        assertEquals(6, conteos.size)
        assertEquals(0, conteos.single { it.nombre == "Proyectores" }.cantidad)
        assertEquals(2, conteos.single { it.nombre == "Hardware" }.cantidad)
        assertEquals(6, conteos.sumOf { it.cantidad })
        assertEquals(3, repo.observarCantidadPorEstado(EstadoTicket.ABIERTO).first())
        assertEquals(1, repo.observarCantidadPorEstado(EstadoTicket.EN_PROCESO).first())
        assertEquals(1, repo.observarCantidadPorEstado(EstadoTicket.RESUELTO).first())
        assertEquals(1, repo.observarCantidadPorEstado(EstadoTicket.CERRADO).first())
        assertEquals(1, repo.observarAltasSinAtender(System.currentTimeMillis() - 3_600_000L).first())
    }

    @Test
    fun bandejaOrdenaPrioridadYLuegoFechaDescendente() = runBlocking {
        val ahora = System.currentTimeMillis()
        val altaAntigua = db.ticketDao().insertarTicket(
            ticketPrueba(1, 1).copy(prioridad = "ALTA", fecha = ahora - 96 * 3_600_000L),
        ).toInt()
        val bajaReciente = db.ticketDao().insertarTicket(
            ticketPrueba(1, 1).copy(prioridad = "BAJA", fecha = ahora),
        ).toInt()
        val bandeja = repo.observarTodosLosTickets().first().map { it.ticket }
        assertEquals(listOf(12, 14, altaAntigua, 13, 11, bajaReciente, 10, 9), bandeja.map { it.id })
        assertEquals(listOf("ALTA", "ALTA", "ALTA", "MEDIA", "MEDIA", "BAJA", "BAJA", "BAJA"), bandeja.map { it.prioridad })
        val cliente = repo.observarTicketsCliente(1).first().map { it.ticket }
        assertEquals(listOf(bajaReciente, 12, 14, 13, 11, 10, 9, altaAntigua), cliente.map { it.id })
        assertTrue(cliente.zipWithNext().all { (a, b) -> a.fecha >= b.fecha })
        assertTrue(repo.observarTicketsCliente(999).first().isEmpty())
    }

    @Test
    fun cierreGuardaCalificacionYFechaYReaperturaLasLimpia() = runBlocking {
        val antes = System.currentTimeMillis()
        repo.cerrarConCalificacion(9, 4)
        val cerrado = requireNotNull(repo.observarTicket(9).first()).ticket
        assertEquals(EstadoTicket.CERRADO.name, cerrado.estado)
        assertEquals(4, cerrado.calificacion ?: -1)
        assertTrue(requireNotNull(cerrado.fechaCierre) in antes..System.currentTimeMillis())

        repo.reabrir(9)
        val reabierto = requireNotNull(repo.observarTicket(9).first()).ticket
        assertEquals(EstadoTicket.ABIERTO.name, reabierto.estado)
        assertNull(reabierto.calificacion)
        assertNull(reabierto.fechaCierre)
    }

    @Test
    fun crearTicketYEditarCategoriaConservaRelaciones() = runBlocking {
        val antes = System.currentTimeMillis()
        val id = repo.crearTicket(1, 1, "Monitor sin imagen", "El monitor no muestra ninguna imagen.",
            "Laboratorio C", Prioridad.MEDIA).toInt()
        val creado = requireNotNull(repo.observarTicket(id).first())
        assertEquals(EstadoTicket.ABIERTO.name, creado.ticket.estado)
        assertEquals(Prioridad.MEDIA.name, creado.ticket.prioridad)
        assertTrue(creado.ticket.fecha in antes..System.currentTimeMillis())
        assertNull(creado.ticket.fechaCierre)
        assertNull(creado.ticket.calificacion)

        repo.guardarCategoria(creado.categoria.copy(nombre = "Equipos"))
        val editado = requireNotNull(repo.observarTicket(id).first())
        assertEquals("Equipos", editado.categoria.nombre)
        assertEquals(id, editado.ticket.id)
        assertEquals(7, repo.observarTodosLosTickets().first().size)
    }

    @Test
    fun comentarioEmiteActualizacionEnElFlowDeLaRelacion() = runBlocking {
        val emisiones = Channel<TicketConDetalle?>(Channel.UNLIMITED)
        val observador = launch(Dispatchers.IO) {
            repo.observarTicket(12).collect { emisiones.send(it) }
        }
        try {
            val inicial = requireNotNull(esperarEmision("la lectura inicial del ticket") {
                emisiones.receive()
            })
            val autor = requireNotNull(repo.obtenerUsuarioPorRol(Rol.ADMIN))
            val texto = "Revisaremos la fuente de energía del equipo."
            repo.agregarComentario(12, autor, texto)

            val actualizado = requireNotNull(esperarEmision("la actualización del comentario insertado") {
                emisiones.receiveAsFlow().first { detalle ->
                    detalle?.comentarios?.any { it.texto == texto } == true
                }
            })
            assertEquals(inicial.comentarios.size + 1, actualizado.comentarios.size)
            val nuevo = actualizado.comentarios.single { it.texto == texto }
            assertEquals(autor.id, nuevo.autorId)
            assertEquals(autor.nombre, nuevo.autorNombre)
            assertEquals(autor.rol, nuevo.autorRol)
        } finally {
            observador.cancelAndJoin()
            emisiones.close()
        }
    }

    private suspend fun <T> esperarEmision(etapa: String, recibir: suspend () -> T): T =
        try {
            // El arranque del emulador y la primera preparación de Room pueden superar cinco segundos.
            withTimeout(30_000) { recibir() }
        } catch (error: TimeoutCancellationException) {
            throw AssertionError("Room no emitió $etapa en 30 segundos", error)
        }

    private fun ticketPrueba(clienteId: Int, categoriaId: Int) = Ticket(
        clienteId = clienteId,
        categoriaId = categoriaId,
        titulo = "Ticket de prueba",
        descripcion = "Descripción de prueba para comprobar las relaciones.",
        ubicacion = "Laboratorio",
    )
}
