package com.tecsup.ayudacampus.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tecsup.ayudacampus.data.local.AyudaDatabase
import com.tecsup.ayudacampus.data.local.DatosIniciales
import com.tecsup.ayudacampus.data.local.entity.Usuario
import com.tecsup.ayudacampus.data.prefs.PreferenciasStore
import com.tecsup.ayudacampus.data.repository.AyudaRepositoryRoom
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.model.Prioridad
import com.tecsup.ayudacampus.model.Rol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Verifica ViewModels con Room y el DataStore reales del dispositivo. */
@RunWith(AndroidJUnit4::class)
class SesionYFormularioTest {
    private lateinit var db: AyudaDatabase
    private lateinit var repo: AyudaRepositoryRoom
    private lateinit var prefs: PreferenciasStore
    private lateinit var estudiante: Usuario
    private lateinit var soporte: Usuario
    private val viewModels = ViewModelStore()
    private var siguienteVm = 0

    @Before fun preparar() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AyudaDatabase::class.java).build()
        DatosIniciales.sembrar(db)
        repo = AyudaRepositoryRoom(db)
        prefs = PreferenciasStore(context)
        estudiante = requireNotNull(repo.obtenerUsuarioPorRol(Rol.CLIENTE))
        soporte = requireNotNull(repo.obtenerUsuarioPorRol(Rol.ADMIN))
        restablecerPreferencias()
    }

    @After fun limpiar() = runBlocking {
        withContext(Dispatchers.Main) { viewModels.clear() }
        restablecerPreferencias()
        db.close()
    }

    private suspend fun restablecerPreferencias() {
        prefs.cerrarSesion()
        prefs.guardarModoOscuro(false)
        prefs.guardarNotificaciones(true)
        prefs.guardarRecibirTickets(true)
    }

    private suspend fun <T : ViewModel> crearVm(crear: () -> T): T = withContext(Dispatchers.Main) {
        crear().also { viewModels.put("vm-${siguienteVm++}", it) }
    }

    private suspend fun formularioValido(vm: NuevoTicketViewModel) {
        val categoria = repo.observarCategoriasActivas().first().first()
        withContext(Dispatchers.Main) {
            vm.categoria(categoria.id)
            vm.titulo("  Falla en el laboratorio  ")
            vm.descripcion("  El equipo no permite iniciar sesión desde esta mañana  ")
            vm.ubicacion("  Laboratorio 204  ")
        }
    }

    private suspend fun <T> esperarSesion(
        etapa: String,
        vm: SesionViewModel,
        accion: suspend () -> T,
    ): T {
        val inicio = android.os.SystemClock.elapsedRealtime()
        return try {
            withTimeout(15_000) { accion() }.also {
                android.util.Log.i("SesionYFormularioTest", "$etapa: ${android.os.SystemClock.elapsedRealtime() - inicio} ms")
            }
        } catch (error: TimeoutCancellationException) {
            val guardada = withTimeoutOrNull(2_000) { prefs.sesion.first() }
            val actual = vm.usuarioActual.value
            throw AssertionError(
                "$etapa no terminó: sesión guardada=$guardada, usuario=${actual?.id}, " +
                    "rol=${actual?.rol}, listo=${vm.listo.value}, error=${vm.error.value}",
                error,
            )
        }
    }

    @Test fun formularioVacioMuestraErroresPorCampoSinCrearTicket() = runBlocking {
        val antes = repo.observarTodosLosTickets().first().size
        val vm = crearVm { NuevoTicketViewModel(repo, prefs, estudiante) }
        withContext(Dispatchers.Main) { vm.enviar() }
        assertNotNull(vm.formulario.value.errorTitulo)
        assertNotNull(vm.formulario.value.errorDescripcion)
        assertNotNull(vm.formulario.value.errorCategoria)
        assertEquals(antes, repo.observarTodosLosTickets().first().size)
    }

    @Test fun validacionUsaTextoRecortadoYLongitudesMinimas() = runBlocking {
        val vm = crearVm { NuevoTicketViewModel(repo, prefs, estudiante) }
        formularioValido(vm)
        withContext(Dispatchers.Main) {
            vm.titulo("  abcd  ")
            vm.descripcion("  123456789  ")
            vm.enviar()
        }
        assertNotNull(vm.formulario.value.errorTitulo)
        assertNotNull(vm.formulario.value.errorDescripcion)
        assertFalse(vm.formulario.value.enviando)
        assertNull(vm.formulario.value.enviadoId)
    }

    @Test fun ticketValidoSeGuardaAbiertoConPrioridadElegidaYEvitaDobleEnvio() = runBlocking {
        val antes = repo.observarTodosLosTickets().first().size
        val vm = crearVm { NuevoTicketViewModel(repo, prefs, estudiante) }
        assertEquals(Prioridad.MEDIA, vm.formulario.value.prioridad)
        formularioValido(vm)
        withContext(Dispatchers.Main) {
            vm.prioridad(Prioridad.ALTA)
            vm.enviar()
            vm.enviar()
        }
        val enviado = withTimeout(5_000) { vm.formulario.first { it.enviadoId != null } }
        val detalle = repo.observarTicket(requireNotNull(enviado.enviadoId).toInt()).first()
        assertNotNull(detalle)
        assertEquals(EstadoTicket.ABIERTO.name, detalle?.ticket?.estado)
        assertEquals(Prioridad.ALTA.name, detalle?.ticket?.prioridad)
        assertEquals(estudiante.id, detalle?.ticket?.clienteId)
        assertEquals("Falla en el laboratorio", detalle?.ticket?.titulo)
        assertEquals("Laboratorio 204", detalle?.ticket?.ubicacion)
        assertEquals(antes + 1, repo.observarTodosLosTickets().first().size)
        withContext(Dispatchers.Main) { vm.consumirEnvio() }
        assertEquals(NuevoTicketUi(), vm.formulario.value)
    }

    @Test fun longitudesMinimasExactasSonValidasYUbicacionEsOpcional() = runBlocking {
        val vm = crearVm { NuevoTicketViewModel(repo, prefs, estudiante) }
        formularioValido(vm)
        withContext(Dispatchers.Main) {
            vm.titulo("12345")
            vm.descripcion("1234567890")
            vm.ubicacion("")
            vm.enviar()
        }
        val enviado = withTimeout(5_000) { vm.formulario.first { it.enviadoId != null } }
        val guardado = repo.observarTicket(requireNotNull(enviado.enviadoId).toInt()).first()
        assertEquals("12345", guardado?.ticket?.titulo)
        assertEquals("1234567890", guardado?.ticket?.descripcion)
        assertEquals("", guardado?.ticket?.ubicacion)
        assertEquals(Prioridad.MEDIA.name, guardado?.ticket?.prioridad)
    }

    @Test fun categoriaDesactivadaDespuesDeElegirlaImpideElEnvio() = runBlocking {
        val antes = repo.observarTodosLosTickets().first().size
        val vm = crearVm { NuevoTicketViewModel(repo, prefs, estudiante) }
        formularioValido(vm)
        repo.cambiarActiva(requireNotNull(vm.formulario.value.categoriaId), false)
        withContext(Dispatchers.Main) { vm.enviar() }
        val error = withTimeout(5_000) { vm.formulario.first { !it.enviando && it.errorCategoria != null } }
        assertTrue(error.errorCategoria.orEmpty().contains("activa"))
        assertEquals(antes, repo.observarTodosLosTickets().first().size)
    }

    @Test fun mesaCerradaMientrasSeCompletaFormularioImpideCrearTicket() = runBlocking {
        val antes = repo.observarTodosLosTickets().first().size
        val vm = crearVm { NuevoTicketViewModel(repo, prefs, estudiante) }
        formularioValido(vm)
        prefs.guardarRecibirTickets(false)
        withContext(Dispatchers.Main) { vm.enviar() }
        val error = withTimeout(5_000) { vm.formulario.first { !it.enviando && it.errorGeneral != null } }
        assertEquals("Mesa de ayuda cerrada", error.errorGeneral)
        assertEquals(antes, repo.observarTodosLosTickets().first().size)
        prefs.guardarRecibirTickets(true)
        withContext(Dispatchers.Main) { vm.enviar() }
        withTimeout(5_000) { vm.formulario.first { it.enviadoId != null } }
        assertEquals(antes + 1, repo.observarTodosLosTickets().first().size)
    }

    @Test fun perfilSoporteNoPuedeCrearTicketDesdeFormulario() = runBlocking {
        val antes = repo.observarTodosLosTickets().first().size
        val vm = crearVm { NuevoTicketViewModel(repo, prefs, soporte) }
        formularioValido(vm)
        withContext(Dispatchers.Main) { vm.enviar() }
        assertNotNull(vm.formulario.value.errorGeneral)
        assertEquals(antes, repo.observarTodosLosTickets().first().size)
    }

    @Test fun loginValidaCamposCredencialesYPerfilSinGuardarSesionIncorrecta() = runBlocking {
        val vm = crearVm { SesionViewModel(repo, prefs) }
        withTimeout(5_000) { vm.listo.first { it } }
        withContext(Dispatchers.Main) { vm.login("", "", Rol.CLIENTE) }
        assertNotNull(vm.error.value)
        withContext(Dispatchers.Main) {
            vm.limpiarError()
            vm.login(estudiante.correo, "clave incorrecta", Rol.CLIENTE)
        }
        withTimeout(5_000) { vm.error.first { it == "Correo o contraseña incorrectos" } }
        assertNull(vm.usuarioActual.value)
        withContext(Dispatchers.Main) {
            vm.limpiarError()
            vm.login(estudiante.correo, estudiante.clave, Rol.ADMIN)
        }
        withTimeout(5_000) { vm.error.first { it.orEmpty().contains("perfil seleccionado") } }
        assertEquals("" to 0, prefs.sesion.first())
        withContext(Dispatchers.Main) { vm.login(estudiante.correo, estudiante.clave, Rol.CLIENTE) }
        withTimeout(5_000) { vm.usuarioActual.first { it?.id == estudiante.id } }
        assertEquals(Rol.CLIENTE.name to estudiante.id, prefs.sesion.first())
    }

    @Test fun cambioDePerfilYPreferenciasSeRestauranEnNuevoViewModel() = runBlocking {
        prefs.guardarSesion(Rol.CLIENTE, estudiante.id)
        val vm = crearVm { SesionViewModel(repo, prefs) }
        withTimeout(5_000) { vm.usuarioActual.first { it?.id == estudiante.id } }
        withContext(Dispatchers.Main) {
            vm.guardarModoOscuro(true)
            vm.guardarNotificaciones(false)
            vm.cambiarPerfil()
        }
        withTimeout(5_000) { prefs.modoOscuro.first { it } }
        withTimeout(5_000) { prefs.notificaciones.first { !it } }
        withTimeout(5_000) { vm.usuarioActual.first { it?.id == soporte.id } }
        assertEquals(Rol.ADMIN.name to soporte.id, prefs.sesion.first())
        withContext(Dispatchers.Main) { vm.guardarRecibirTickets(false) }
        withTimeout(5_000) { prefs.recibirTickets.first { !it } }
        withContext(Dispatchers.Main) { viewModels.clear() }
        val nuevo = crearVm { SesionViewModel(repo, PreferenciasStore(ApplicationProvider.getApplicationContext())) }
        withTimeout(5_000) { nuevo.listo.first { it } }
        assertEquals(soporte.id, nuevo.usuarioActual.value?.id)
        withTimeout(5_000) { nuevo.modoOscuro.first { it } }
        withTimeout(5_000) { nuevo.notificaciones.first { !it } }
        withTimeout(5_000) { nuevo.recibirTickets.first { !it } }
        assertFalse(nuevo.recibirTickets.value)
    }

    @Test fun cerrarSesionBorraIdentidadYConservaModoOscuro() = runBlocking {
        prefs.guardarSesion(Rol.CLIENTE, estudiante.id)
        prefs.guardarModoOscuro(true)
        assertEquals(Rol.CLIENTE.name to estudiante.id, prefs.sesion.first())
        val vm = crearVm { SesionViewModel(repo, prefs) }
        esperarSesion("Restaurar estudiante antes de cerrar", vm) {
            vm.usuarioActual.first { it?.id == estudiante.id }
        }
        withContext(Dispatchers.Main) { vm.cerrarSesion() }
        esperarSesion("Borrar perfil e id de DataStore", vm) {
            prefs.sesion.first { it == ("" to 0) }
        }
        esperarSesion("Quitar la identidad del ViewModel", vm) {
            vm.usuarioActual.first { it == null }
        }
        assertTrue("Cerrar sesión debe conservar el modo oscuro", prefs.modoOscuro.first())
    }

    @Test fun perfilCuentaSoloTicketsDelEstudianteActivo() = runBlocking {
        prefs.guardarSesion(Rol.CLIENTE, estudiante.id)
        val propios = repo.observarTicketsCliente(estudiante.id).first()
        val vm = crearVm { SesionViewModel(repo, prefs) }
        val conteos = withTimeout(5_000) { vm.estadisticas.first { it.total == propios.size } }
        assertEquals(propios.count { it.ticket.estado == EstadoTicket.ABIERTO.name }, conteos.abiertos)
        assertEquals(propios.count { it.ticket.estado == EstadoTicket.RESUELTO.name }, conteos.resueltos)
        assertEquals(propios.size, conteos.total)
    }

    @Test fun restauracionConRolAjenoLimpiaSesionYTerminaSplash() = runBlocking {
        prefs.guardarSesion(Rol.ADMIN, estudiante.id)
        val vm = crearVm { SesionViewModel(repo, prefs) }
        withTimeout(5_000) { vm.listo.first { it } }
        withTimeout(5_000) { prefs.sesion.first { it == ("" to 0) } }
        assertNull(vm.usuarioActual.value)
        assertNotNull(vm.error.value)
    }

    @Test fun restauracionConUsuarioInexistenteNoDejaIdentidadAnterior() = runBlocking {
        prefs.guardarSesion(Rol.CLIENTE, estudiante.id)
        val vm = crearVm { SesionViewModel(repo, prefs) }
        withTimeout(5_000) { vm.usuarioActual.first { it != null } }
        prefs.guardarSesion(Rol.ADMIN, Int.MAX_VALUE)
        withTimeout(5_000) { prefs.sesion.first { it == ("" to 0) } }
        assertNull(vm.usuarioActual.value)
        assertTrue(vm.listo.value)
    }

    @Test fun estudianteNoPuedeModificarRecepcionDeTickets() = runBlocking {
        prefs.guardarSesion(Rol.CLIENTE, estudiante.id)
        val vm = crearVm { SesionViewModel(repo, prefs) }
        withTimeout(5_000) { vm.usuarioActual.first { it != null } }
        withContext(Dispatchers.Main) { vm.guardarRecibirTickets(false) }
        assertTrue(prefs.recibirTickets.first())
        assertNotNull(vm.error.value)
    }
}
