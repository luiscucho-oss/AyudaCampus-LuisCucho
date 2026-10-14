package com.tecsup.ayudacampus.viewmodel

import com.tecsup.ayudacampus.data.local.entity.Categoria
import com.tecsup.ayudacampus.data.local.entity.Comentario
import com.tecsup.ayudacampus.data.local.entity.Ticket
import com.tecsup.ayudacampus.data.local.entity.Usuario
import com.tecsup.ayudacampus.data.local.relation.TicketConDetalle
import com.tecsup.ayudacampus.data.repository.AyudaRepository
import com.tecsup.ayudacampus.data.repository.CategoriaEnUsoException
import com.tecsup.ayudacampus.model.ConteoCategoria
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.model.Prioridad
import com.tecsup.ayudacampus.model.Rol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ReglasTicketTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun estudianteNoVeNiModificaTicketsAjenos() = runTest {
        val repo = RepositoryPrueba(listOf(detalle(cliente = otroEstudiante)))
        val vm = TicketDetalleViewModel(repo, estudiante, 1)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ticket.collect {} }
        vm.comentar("No debo tener acceso")
        vm.cerrar(4)
        vm.reabrir()
        runCurrent()
        assertNull(vm.ticket.value)
        assertTrue(repo.operaciones.isEmpty())
        assertNotNull(vm.error.value)
    }

    @Test fun cerrarExigeEstadoResueltoYCalificacionValida() = runTest {
        val repo = RepositoryPrueba(listOf(detalle()))
        val vm = TicketDetalleViewModel(repo, estudiante, 1)
        vm.cerrar(4)
        runCurrent()
        assertTrue(repo.operaciones.isEmpty())
        repo.actualizar(1) { it.copy(estado = EstadoTicket.RESUELTO.name) }
        vm.cerrar(0)
        runCurrent()
        vm.cerrar(6)
        runCurrent()
        assertTrue(repo.operaciones.isEmpty())
        assertTrue(vm.error.value.orEmpty().contains("1 a 5"))
    }

    @Test fun cierreGuardaCalificacionFechaYComentarioFinalAntesDelCierre() = runTest {
        val repo = RepositoryPrueba(listOf(detalle(estado = EstadoTicket.RESUELTO)))
        val vm = TicketDetalleViewModel(repo, estudiante, 1)
        vm.cerrar(4, "  Gracias por atender  ")
        runCurrent()
        val guardado = repo.lista.value.single()
        assertEquals(EstadoTicket.CERRADO.name, guardado.ticket.estado)
        assertEquals(4, guardado.ticket.calificacion)
        assertNotNull(guardado.ticket.fechaCierre)
        assertEquals("Gracias por atender", guardado.comentarios.single().texto)
        assertEquals(listOf("comentar", "cerrar"), repo.operaciones)
    }

    @Test fun soporteNoPuedeCerrarNiReabrirComoEstudiante() = runTest {
        val repo = RepositoryPrueba(listOf(detalle(estado = EstadoTicket.RESUELTO)))
        val vm = TicketDetalleViewModel(repo, soporte, 1)
        vm.cerrar(5)
        vm.reabrir()
        runCurrent()
        assertTrue(repo.operaciones.isEmpty())
        assertEquals(EstadoTicket.RESUELTO.name, repo.lista.value.single().ticket.estado)
    }

    @Test fun reabrirResueltoLimpiaCalificacionYFechaCierre() = runTest {
        val original = detalle(estado = EstadoTicket.RESUELTO).let {
            it.copy(ticket = it.ticket.copy(calificacion = 3, fechaCierre = 123L))
        }
        val repo = RepositoryPrueba(listOf(original))
        val vm = TicketDetalleViewModel(repo, estudiante, 1)
        vm.reabrir()
        runCurrent()
        val reabierto = repo.lista.value.single().ticket
        assertEquals(EstadoTicket.ABIERTO.name, reabierto.estado)
        assertNull(reabierto.calificacion)
        assertNull(reabierto.fechaCierre)
    }

    @Test fun cerradoEsInmutableParaAmbosPerfiles() = runTest {
        val repo = RepositoryPrueba(listOf(detalle(estado = EstadoTicket.CERRADO)))
        val clienteVm = TicketDetalleViewModel(repo, estudiante, 1)
        val soporteVm = AtenderViewModel(repo, soporte, 1)
        runCurrent()
        clienteVm.comentar("Comentario después del cierre")
        clienteVm.reabrir()
        clienteVm.cerrar(4)
        soporteVm.cambiarEstado(EstadoTicket.ABIERTO)
        soporteVm.cambiarPrioridad(Prioridad.ALTA)
        soporteVm.responder("Respuesta después del cierre")
        soporteVm.guardar()
        runCurrent()
        assertTrue(repo.operaciones.isEmpty())
        assertEquals(EstadoTicket.CERRADO.name, repo.lista.value.single().ticket.estado)
    }

    @Test fun soporteActualizaEstadoYPrioridadPeroNoEligeCerrado() = runTest {
        val repo = RepositoryPrueba(listOf(detalle()))
        val vm = AtenderViewModel(repo, soporte, 1)
        runCurrent()
        vm.cambiarEstado(EstadoTicket.CERRADO)
        assertEquals(EstadoTicket.ABIERTO, vm.estadoSeleccionado.value)
        assertNotNull(vm.error.value)
        vm.cambiarEstado(EstadoTicket.EN_PROCESO)
        vm.cambiarPrioridad(Prioridad.ALTA)
        vm.responder("  Revisaremos el equipo  ")
        vm.guardar()
        runCurrent()
        assertEquals(EstadoTicket.EN_PROCESO.name, repo.lista.value.single().ticket.estado)
        assertEquals(Prioridad.ALTA.name, repo.lista.value.single().ticket.prioridad)
        assertEquals("Revisaremos el equipo", repo.lista.value.single().comentarios.single().texto)
        assertTrue(vm.guardado.value)
    }

    @Test fun estudianteNoPuedeAtenderDesdeViewModelSoporte() = runTest {
        val repo = RepositoryPrueba(listOf(detalle()))
        val vm = AtenderViewModel(repo, estudiante, 1)
        runCurrent()
        vm.cambiarEstado(EstadoTicket.RESUELTO)
        vm.cambiarPrioridad(Prioridad.ALTA)
        vm.responder("Respuesta sin permiso")
        vm.guardar()
        runCurrent()
        assertNull(vm.ticket.value)
        assertTrue(repo.operaciones.isEmpty())
    }

    @Test fun comentarioNuevoNoBorraSeleccionesPendientesDeSoporte() = runTest {
        val repo = RepositoryPrueba(listOf(detalle()))
        val vm = AtenderViewModel(repo, soporte, 1)
        runCurrent()
        vm.cambiarEstado(EstadoTicket.RESUELTO)
        vm.cambiarPrioridad(Prioridad.ALTA)
        repo.agregarComentario(1, estudiante, "Una actualización del estudiante")
        runCurrent()
        assertEquals(EstadoTicket.RESUELTO, vm.estadoSeleccionado.value)
        assertEquals(Prioridad.ALTA, vm.prioridadSeleccionada.value)
    }

    @Test fun comentariosVaciosSeIgnoranYLaConversacionSeOrdena() = runTest {
        val original = detalle().copy(comentarios = listOf(
            Comentario(2, 1, soporte.id, soporte.nombre, soporte.rol, "Segundo", 20L),
            Comentario(1, 1, estudiante.id, estudiante.nombre, estudiante.rol, "Primero", 10L),
        ))
        val repo = RepositoryPrueba(listOf(original))
        val vm = TicketDetalleViewModel(repo, estudiante, 1)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ticket.collect {} }
        vm.comentar("   ")
        runCurrent()
        assertTrue(repo.operaciones.isEmpty())
        assertEquals(listOf("Primero", "Segundo"), vm.ticket.value?.comentarios?.map { it.texto })
    }

    @Test fun misTicketsCombinaPropiedadBusquedaYEstadoIncluyeCerradosEnTodos() = runTest {
        val repo = RepositoryPrueba(listOf(
            detalle(id = 1, titulo = "Wi-Fi del laboratorio"),
            detalle(id = 2, estado = EstadoTicket.CERRADO, titulo = "Impresora sin tinta"),
            detalle(id = 3, cliente = otroEstudiante, titulo = "Wi-Fi ajeno"),
        ))
        val vm = MisTicketsViewModel(repo, estudiante)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.tickets.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.abiertos.collect {} }
        runCurrent()
        assertEquals(listOf(1, 2), vm.tickets.value.map { it.ticket.id })
        assertEquals(1, vm.abiertos.value)
        vm.buscar("WI-FI")
        runCurrent()
        assertEquals(listOf(1), vm.tickets.value.map { it.ticket.id })
        vm.buscar("#2")
        runCurrent()
        assertEquals(listOf(2), vm.tickets.value.map { it.ticket.id })
        vm.buscar("#0002")
        runCurrent()
        assertEquals(listOf(2), vm.tickets.value.map { it.ticket.id })
        vm.filtrar(EstadoTicket.ABIERTO)
        runCurrent()
        assertTrue(vm.tickets.value.isEmpty())
        assertEquals(1, vm.abiertos.value)
    }

    @Test fun bandejaConservaOrdenDelRepositoryAlFiltrar() = runTest {
        val repo = RepositoryPrueba(listOf(
            detalle(id = 3, prioridad = Prioridad.ALTA),
            detalle(id = 2, estado = EstadoTicket.RESUELTO, prioridad = Prioridad.MEDIA),
            detalle(id = 1, prioridad = Prioridad.BAJA),
        ))
        val vm = BandejaViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.tickets.collect {} }
        runCurrent()
        assertEquals(listOf(3, 2, 1), vm.tickets.value.map { it.ticket.id })
        vm.filtrar(EstadoTicket.ABIERTO)
        runCurrent()
        assertEquals(listOf(3, 1), vm.tickets.value.map { it.ticket.id })
    }

    @Test fun bandejaCombinaEstadoPrioridadCategoriaYTextoYRestableceAtomicamente() = runTest {
        val repo = RepositoryPrueba(listOf(
            detalle(id = 12, prioridad = Prioridad.ALTA, titulo = "Pantalla sin imagen"),
            detalle(id = 11, prioridad = Prioridad.MEDIA),
            detalle(id = 10, estado = EstadoTicket.CERRADO, prioridad = Prioridad.BAJA),
        ))
        val vm = BandejaViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.tickets.collect {} }
        vm.filtrar(EstadoTicket.ABIERTO); vm.filtrarPrioridad(Prioridad.ALTA); vm.filtrarCategoria(1); vm.buscar("  PANTALLA  ")
        runCurrent()
        assertEquals(listOf(12), vm.tickets.value.map { it.ticket.id })
        assertEquals(4, vm.filtros.value.activos)
        vm.filtrarCategoria(999); runCurrent()
        assertTrue(vm.tickets.value.isEmpty())
        vm.limpiarFiltros(); runCurrent()
        assertEquals(listOf(12, 11, 10), vm.tickets.value.map { it.ticket.id })
        assertEquals(0, vm.filtros.value.activos)
    }

    @Test fun bandejaBuscaNumeroConCerosNombreSinAcentosYConservaCategoriasInactivas() = runTest {
        val original = detalle(id = 12, cliente = estudiante.copy(nombre = "Juan Pérez")).copy(categoria = categoria.copy(activa = false))
        val repo = RepositoryPrueba(listOf(original, detalle(id = 5)))
        repo.categorias.value = listOf(categoria.copy(activa = false))
        val vm = BandejaViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.tickets.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.categorias.collect {} }
        for (texto in listOf(" #0012 ", "12", "  JUAN PEREZ  ")) {
            vm.buscar(texto); runCurrent()
            assertEquals(listOf(12), vm.tickets.value.map { it.ticket.id })
        }
        vm.filtrarCategoria(1); runCurrent()
        assertFalse(vm.categorias.value.single().activa)
        assertEquals(listOf(12), vm.tickets.value.map { it.ticket.id })
        vm.buscar("Sin coincidencias"); runCurrent()
        assertTrue(vm.tickets.value.isEmpty())
    }

    @Test fun bandejaReaccionaADatosConFiltrosActivosSinReordenar() = runTest {
        val repo = RepositoryPrueba(listOf(detalle(id = 3, prioridad = Prioridad.ALTA), detalle(id = 2), detalle(id = 1)))
        val vm = BandejaViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.tickets.collect {} }
        vm.filtrarPrioridad(Prioridad.MEDIA); runCurrent()
        assertEquals(listOf(2, 1), vm.tickets.value.map { it.ticket.id })
        repo.actualizar(2) { it.copy(prioridad = "ALTA") }; runCurrent()
        assertEquals(listOf(1), vm.tickets.value.map { it.ticket.id })
    }

    @Test fun categoriasValidaNombreIconoYExplicaCategoriaEnUso() = runTest {
        val repo = RepositoryPrueba(listOf(detalle()))
        val vm = CategoriasViewModel(repo)
        vm.guardar(Categoria(nombre = "   ", icono = "pc"))
        vm.guardar(Categoria(nombre = "Categoría inválida", icono = "invalido"))
        runCurrent()
        assertTrue(repo.operaciones.isEmpty())
        vm.guardar(Categoria(id = 2, nombre = "  Redes  ", icono = "wifi"))
        runCurrent()
        assertEquals("Redes", repo.categorias.value.first { it.id == 2 }.nombre)
        assertTrue(vm.guardado.value)
        vm.eliminar(categoria)
        runCurrent()
        assertTrue(vm.error.value.orEmpty().contains("tickets"))
        vm.alternar(categoria)
        runCurrent()
        assertFalse(repo.categorias.value.first { it.id == categoria.id }.activa)
    }

    @Test fun panelCuentaEstadosYCategoriasYAlertasAltasAbiertasAntiguas() = runTest {
        val ahora = System.currentTimeMillis()
        val repo = RepositoryPrueba(listOf(
            detalle(id = 1, prioridad = Prioridad.ALTA).let { it.copy(ticket = it.ticket.copy(fecha = ahora - 7_200_000L)) },
            detalle(id = 2, prioridad = Prioridad.ALTA).let { it.copy(ticket = it.ticket.copy(fecha = ahora)) },
            detalle(id = 3, estado = EstadoTicket.RESUELTO, prioridad = Prioridad.ALTA).let {
                it.copy(ticket = it.ticket.copy(fecha = ahora - 7_200_000L))
            },
        ))
        val vm = PanelViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.abiertos.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.resueltos.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.porEstado.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.porCategoria.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.altasSinAtender.collect {} }
        runCurrent()
        assertEquals(2, vm.abiertos.value)
        assertEquals(1, vm.resueltos.value)
        assertEquals(1, vm.altasSinAtender.value)
        assertEquals(3f, vm.porEstado.value.sumOf { it.valor.toDouble() }.toFloat())
        assertEquals(3f, vm.porCategoria.value.single().valor)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}

private val estudiante = Usuario(1, "Estudiante Prueba", "estudiante@campus.test", "1234", Rol.CLIENTE.name)
private val otroEstudiante = Usuario(3, "Otro Estudiante", "otro@campus.test", "1234", Rol.CLIENTE.name)
private val soporte = Usuario(2, "Soporte Prueba", "soporte@campus.test", "admin", Rol.ADMIN.name)
private val categoria = Categoria(1, "Hardware", "pc")

private fun detalle(
    id: Int = 1,
    estado: EstadoTicket = EstadoTicket.ABIERTO,
    prioridad: Prioridad = Prioridad.MEDIA,
    cliente: Usuario = estudiante,
    titulo: String = "Equipo no enciende",
): TicketConDetalle = TicketConDetalle(
    ticket = Ticket(id, cliente.id, categoria.id, titulo, "El equipo no enciende desde esta mañana",
        "Laboratorio 2", prioridad.name, estado.name),
    comentarios = emptyList(), categoria = categoria, cliente = cliente,
)

private class RepositoryPrueba(iniciales: List<TicketConDetalle>) : AyudaRepository {
    override fun observarPromedioResolucion(): Flow<Double?> = lista.map { detalles ->
        detalles.mapNotNull { d -> d.ticket.fechaCierre?.takeIf {
            d.ticket.estado == EstadoTicket.CERRADO.name && it >= d.ticket.fecha
        }?.minus(d.ticket.fecha)?.toDouble() }.takeIf { it.isNotEmpty() }?.average()
    }
    val lista = MutableStateFlow(iniciales)
    val categorias = MutableStateFlow(listOf(categoria))
    val operaciones = mutableListOf<String>()
    private val usuarios = listOf(estudiante, otroEstudiante, soporte)
    fun actualizar(id: Int, transformar: (Ticket) -> Ticket) {
        lista.value = lista.value.map { if (it.ticket.id == id) it.copy(ticket = transformar(it.ticket)) else it }
    }
    override suspend fun login(correo: String, clave: String) = usuarios.find { it.correo == correo && it.clave == clave }
    override suspend fun obtenerUsuario(id: Int) = usuarios.find { it.id == id }
    override suspend fun obtenerUsuarioPorRol(rol: Rol) = usuarios.find { it.rol == rol.name }
    override fun observarCategorias(): Flow<List<Categoria>> = categorias
    override fun observarCategoriasActivas(): Flow<List<Categoria>> = categorias.map { it.filter { c -> c.activa } }
    override suspend fun obtenerCategoria(id: Int) = categorias.value.find { it.id == id }
    override suspend fun guardarCategoria(c: Categoria) {
        operaciones += "categoria"
        categorias.value = categorias.value.filterNot { it.id == c.id } + c
    }
    override suspend fun eliminarCategoria(c: Categoria) {
        if (lista.value.any { it.ticket.categoriaId == c.id }) throw CategoriaEnUsoException()
        operaciones += "eliminarCategoria"
        categorias.value = categorias.value.filterNot { it.id == c.id }
    }
    override suspend fun cambiarActiva(id: Int, activa: Boolean) {
        operaciones += "activar"
        categorias.value = categorias.value.map { if (it.id == id) it.copy(activa = activa) else it }
    }
    override suspend fun crearTicket(clienteId: Int, categoriaId: Int, titulo: String, descripcion: String,
        ubicacion: String, prioridad: Prioridad): Long {
        operaciones += "crear"
        val id = (lista.value.maxOfOrNull { it.ticket.id } ?: 0) + 1
        lista.value += TicketConDetalle(Ticket(id, clienteId, categoriaId, titulo, descripcion, ubicacion,
            prioridad.name), emptyList(), requireNotNull(obtenerCategoria(categoriaId)), requireNotNull(obtenerUsuario(clienteId)))
        return id.toLong()
    }
    override fun observarTicketsCliente(clienteId: Int): Flow<List<TicketConDetalle>> =
        lista.map { it.filter { d -> d.ticket.clienteId == clienteId } }
    override fun observarTodosLosTickets(): Flow<List<TicketConDetalle>> = lista
    override fun observarTicket(id: Int): Flow<TicketConDetalle?> = lista.map { it.find { d -> d.ticket.id == id } }
    override suspend fun cambiarEstado(ticketId: Int, estado: EstadoTicket) {
        operaciones += "estado"
        actualizar(ticketId) { it.copy(estado = estado.name) }
    }
    override suspend fun cambiarPrioridad(ticketId: Int, prioridad: Prioridad) {
        operaciones += "prioridad"
        actualizar(ticketId) { it.copy(prioridad = prioridad.name) }
    }
    override suspend fun cerrarConCalificacion(ticketId: Int, calificacion: Int) {
        operaciones += "cerrar"
        actualizar(ticketId) { it.copy(estado = EstadoTicket.CERRADO.name, calificacion = calificacion,
            fechaCierre = System.currentTimeMillis()) }
    }
    override suspend fun reabrir(ticketId: Int) {
        operaciones += "reabrir"
        actualizar(ticketId) { it.copy(estado = EstadoTicket.ABIERTO.name, calificacion = null, fechaCierre = null) }
    }
    override suspend fun agregarComentario(ticketId: Int, autor: Usuario, texto: String) {
        operaciones += "comentar"
        lista.value = lista.value.map {
            if (it.ticket.id == ticketId) it.copy(comentarios = it.comentarios + Comentario(
                id = it.comentarios.size + 1, ticketId = ticketId, autorId = autor.id,
                autorNombre = autor.nombre, autorRol = autor.rol, texto = texto,
            )) else it
        }
    }
    override fun observarCantidadPorEstado(estado: EstadoTicket): Flow<Int> =
        lista.map { it.count { d -> d.ticket.estado == estado.name } }
    override fun observarCantidadPorCategoria(): Flow<List<ConteoCategoria>> = lista.map {
        it.groupBy { d -> d.categoria.nombre }.map { (nombre, tickets) -> ConteoCategoria(nombre, tickets.size) }
    }
    override fun observarAltasSinAtender(limite: Long): Flow<Int> = lista.map {
        it.count { d -> d.ticket.prioridad == Prioridad.ALTA.name &&
            d.ticket.estado == EstadoTicket.ABIERTO.name && d.ticket.fecha < limite }
    }
}
