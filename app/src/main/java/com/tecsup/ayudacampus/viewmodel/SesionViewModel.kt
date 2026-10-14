package com.tecsup.ayudacampus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.ayudacampus.data.local.entity.Usuario
import com.tecsup.ayudacampus.data.prefs.PreferenciasStore
import com.tecsup.ayudacampus.data.repository.AyudaRepository
import com.tecsup.ayudacampus.model.EstadoTicket
import com.tecsup.ayudacampus.model.Rol
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SesionViewModel(
    private val repo: AyudaRepository,
    private val prefs: PreferenciasStore,
) : ViewModel() {
    private val _usuarioActual = MutableStateFlow<Usuario?>(null)
    val usuarioActual: StateFlow<Usuario?> = _usuarioActual.asStateFlow()
    private val _listo = MutableStateFlow(false)
    val listo: StateFlow<Boolean> = _listo.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val modoOscuro = prefs.modoOscuro.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val notificaciones = prefs.notificaciones.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val recibirTickets = prefs.recibirTickets.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val estadisticas: StateFlow<EstadisticasUsuario> = usuarioActual.flatMapLatest { usuario ->
        if (usuario == null) flowOf(EstadisticasUsuario())
        else repo.observarTicketsCliente(usuario.id).map { lista ->
            EstadisticasUsuario(
                abiertos = lista.count { it.ticket.estado == EstadoTicket.ABIERTO.name },
                resueltos = lista.count { it.ticket.estado == EstadoTicket.RESUELTO.name },
                total = lista.size,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadisticasUsuario())

    init {
        viewModelScope.launch {
            try {
                prefs.sesion.collectLatest { (rol, id) ->
                    val rolValido = Rol.entries.any { it.name == rol }
                    val usuario = if (rolValido && id > 0) repo.obtenerUsuario(id) else null
                    val valida = usuario != null && usuario.rol == rol
                    _usuarioActual.value = usuario?.takeIf { valida }
                    _listo.value = true
                    if (!valida && (rol.isNotBlank() || id != 0)) {
                        _error.value = "La sesión guardada ya no es válida. Vuelve a ingresar."
                        prefs.cerrarSesion()
                    }
                }
            } catch (e: Exception) {
                _error.value = e.mensajeUsuario("No se pudo recuperar la sesión. Vuelve a ingresar.")
                _usuarioActual.value = null
                _listo.value = true
            }
        }
    }

    fun login(correo: String, clave: String, rol: Rol) {
        if (correo.isBlank() || clave.isBlank()) {
            _error.value = "Ingresa tu correo y contraseña"
            return
        }
        viewModelScope.launch {
            try {
                val usuario = repo.login(correo.trim(), clave)
                when {
                    usuario == null -> _error.value = "Correo o contraseña incorrectos"
                    usuario.rol != rol.name -> _error.value = "El usuario no corresponde al perfil seleccionado"
                    else -> {
                        prefs.guardarSesion(rol, usuario.id)
                        _usuarioActual.value = usuario
                        _error.value = null
                    }
                }
            } catch (e: Exception) {
                _error.value = e.mensajeUsuario("No se pudo iniciar sesión. Inténtalo nuevamente.")
            }
        }
    }

    fun cambiarPerfil() {
        val actual = usuarioActual.value ?: return
        val otroRol = if (actual.rol == Rol.CLIENTE.name) Rol.ADMIN else Rol.CLIENTE
        viewModelScope.launch {
            try {
                val otro = repo.obtenerUsuarioPorRol(otroRol)
                if (otro == null) _error.value = "No hay un usuario disponible para ese perfil"
                else {
                    prefs.guardarSesion(otroRol, otro.id)
                    _usuarioActual.value = otro
                    _error.value = null
                }
            } catch (e: Exception) {
                _error.value = e.mensajeUsuario("No se pudo cambiar de perfil")
            }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            try {
                prefs.cerrarSesion()
                _usuarioActual.value = null
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.mensajeUsuario("No se pudo cerrar la sesión")
            }
        }
    }

    fun guardarModoOscuro(b: Boolean) = guardarPreferencia { prefs.guardarModoOscuro(b) }
    fun guardarNotificaciones(b: Boolean) = guardarPreferencia { prefs.guardarNotificaciones(b) }
    fun guardarRecibirTickets(b: Boolean) {
        if (usuarioActual.value?.rol != Rol.ADMIN.name) {
            _error.value = "Solo Soporte TI puede cambiar la recepción de tickets"
            return
        }
        guardarPreferencia { prefs.guardarRecibirTickets(b) }
    }

    private fun guardarPreferencia(guardar: suspend () -> Unit) {
        viewModelScope.launch {
            try { guardar() }
            catch (e: Exception) { _error.value = e.mensajeUsuario("No se pudo guardar la preferencia") }
        }
    }

    fun limpiarError() { _error.value = null }
}
