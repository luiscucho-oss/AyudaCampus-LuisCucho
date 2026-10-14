package com.tecsup.ayudacampus

import android.app.Application
import com.tecsup.ayudacampus.data.local.AyudaDatabase
import com.tecsup.ayudacampus.data.local.DatosIniciales
import com.tecsup.ayudacampus.data.repository.AyudaRepository
import com.tecsup.ayudacampus.data.repository.AyudaRepositoryRoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AyudaApp : Application() {
    val database by lazy { AyudaDatabase.obtener(this) }
    val repository: AyudaRepository by lazy { AyudaRepositoryRoom(database) }
    private val _lista = MutableStateFlow(false)
    val lista = _lista.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            try {
                DatosIniciales.sembrar(database)
                _lista.value = true
            } catch (_: Exception) {
                _error.value = "No se pudo preparar la base de datos. Vuelve a abrir la aplicación."
            }
        }
    }
}
