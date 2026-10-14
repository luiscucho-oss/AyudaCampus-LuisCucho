package com.tecsup.ayudacampus.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tecsup.ayudacampus.model.Rol
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("ajustes")

class PreferenciasStore(context: Context) {
    private val store = context.applicationContext.dataStore
    private val datos = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    // El perfil y su usuario se leen desde una sola instantánea de DataStore.
    val sesion: Flow<Pair<String, Int>> = datos.map {
        (it[PERFIL_ACTIVO] ?: "") to (it[USUARIO_ID] ?: 0)
    }.distinctUntilChanged()
    val perfilActivo: Flow<String> = datos.map { it[PERFIL_ACTIVO] ?: "" }.distinctUntilChanged()
    val usuarioId: Flow<Int> = datos.map { it[USUARIO_ID] ?: 0 }.distinctUntilChanged()
    val modoOscuro: Flow<Boolean> = datos.map { it[MODO_OSCURO] ?: false }.distinctUntilChanged()
    val notificaciones: Flow<Boolean> = datos.map { it[NOTIFICACIONES] ?: true }.distinctUntilChanged()
    val recibirTickets: Flow<Boolean> = datos.map { it[RECIBIR_TICKETS] ?: true }.distinctUntilChanged()

    suspend fun guardarSesion(rol: Rol, usuarioId: Int) {
        store.edit {
            it[PERFIL_ACTIVO] = rol.name
            it[USUARIO_ID] = usuarioId
        }
    }

    suspend fun guardarModoOscuro(b: Boolean) {
        store.edit { it[MODO_OSCURO] = b }
    }

    suspend fun guardarNotificaciones(b: Boolean) {
        store.edit { it[NOTIFICACIONES] = b }
    }

    suspend fun guardarRecibirTickets(b: Boolean) {
        store.edit { it[RECIBIR_TICKETS] = b }
    }

    suspend fun cerrarSesion() {
        store.edit {
            it.remove(PERFIL_ACTIVO)
            it.remove(USUARIO_ID)
        }
    }

    private companion object {
        val PERFIL_ACTIVO = stringPreferencesKey("perfilActivo")
        val USUARIO_ID = intPreferencesKey("usuarioId")
        val MODO_OSCURO = booleanPreferencesKey("modoOscuro")
        val NOTIFICACIONES = booleanPreferencesKey("notificaciones")
        val RECIBIR_TICKETS = booleanPreferencesKey("recibirTickets")
    }
}
