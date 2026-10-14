package com.tecsup.ayudacampus.ui

import android.content.Context
import android.graphics.Bitmap
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tecsup.ayudacampus.AyudaApp
import com.tecsup.ayudacampus.MainActivity
import com.tecsup.ayudacampus.data.local.DatosIniciales
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class MejorasAceptacionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app: AyudaApp get() = compose.activity.application as AyudaApp

    @Before fun preparar() {
        runBlocking {
            withTimeout(30_000) { app.lista.first { it } }
            app.preferencias.cerrarSesion()
            app.preferencias.guardarModoOscuro(false)
        }
        esperarTag("login-correo")
        reiniciarDatos()
        runBlocking { app.repository.cambiarActiva(1, false) }
    }

    @After fun restaurar() {
        runBlocking { app.preferencias.guardarModoOscuro(false); app.preferencias.cerrarSesion() }
        reiniciarDatos()
    }

    @Test fun indicadoresReactivosFiltrosCombinadosYSlaEnAmbosPerfiles() {
        compose.onNodeWithText("Soporte TI").performClick()
        escribir("login-correo", "soporte@campus.test")
        escribir("login-clave", "admin")
        compose.onNodeWithTag("login-ingresar").performScrollTo().performClick()
        esperarTexto("24 h 0 min")
        capturar("01-panel-promedio")
        runBlocking { app.repository.cerrarConCalificacion(9, 5) }
        esperarTexto("48 h 0 min")
        runBlocking { app.repository.reabrir(9) }
        esperarTexto("24 h 0 min")

        pulsar("nav-bandeja")
        esperarTexto("6 resultados")
        escribir("bandeja-busqueda", "  JUAN PEREZ  ")
        ocultarTeclado()
        compose.onNodeWithText("Abiertos").performClick()
        pulsar("bandeja-prioridad")
        pulsar("bandeja-prioridad-ALTA")
        pulsar("bandeja-categoria")
        esperarTexto("Hardware (inactiva)")
        pulsar("bandeja-categoria-1")
        esperarTexto("1 resultado")
        compose.onNodeWithText("Servidor del aula 305 caído").assertExists()
        esperarTag("sla-14")
        capturar("02-bandeja-filtros")
        escribir("bandeja-busqueda", "no existe este ticket")
        ocultarTeclado()
        esperarTexto("No hay tickets con estos filtros")
        pulsar("bandeja-limpiar")
        esperarTexto("6 resultados")
        escribir("bandeja-busqueda", " #0012 ")
        ocultarTeclado()
        esperarTexto("1 resultado")
        compose.onNodeWithText("Sin Wi-Fi en el laboratorio 3").performClick()
        esperarTag("sla-12")
        esperarTexto("SLA · Alta · 4 h")
        capturar("03-detalle-sla")
        compose.onNodeWithText("Baja").performScrollTo().performClick()
        compose.onNodeWithText("Guardar cambios").performScrollTo().performClick()
        compose.onNodeWithTag("sla-12", useUnmergedTree = true).performScrollTo()
        esperarTexto("SLA · Baja · 72 h")
        compose.onNodeWithContentDescription("Volver").performClick()
        pulsar("nav-ajustes")
        compose.onNodeWithText("Cambiar de perfil").performScrollTo().performClick()
        esperarTexto("Mis tickets")
        esperarTag("sla-12")
        capturar("04-estudiante-sla")
        compose.onNodeWithText("Sin Wi-Fi en el laboratorio 3").performClick()
        esperarTexto("SLA · Baja · 72 h")
        compose.onNodeWithContentDescription("Volver").performClick()
        pulsar("nav-perfil")
        pulsar("preferencia-oscuro")
        compose.waitUntil(10_000) { runBlocking { app.preferencias.modoOscuro.first() } }
        pulsar("nav-tickets")
        esperarTag("sla-12")
        capturar("05-estudiante-oscuro")
    }

    private fun reiniciarDatos() = runBlocking { withContext(Dispatchers.IO) {
        app.database.clearAllTables(); DatosIniciales.sembrar(app.database)
    } }
    private fun esperarTag(tag: String) = compose.waitUntil(20_000) { runCatching { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
    private fun esperarTexto(texto: String) = compose.waitUntil(20_000) { runCatching { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
    private fun pulsar(tag: String) { esperarTag(tag); compose.onNodeWithTag(tag).performClick() }
    private fun escribir(tag: String, texto: String) { esperarTag(tag); compose.onNodeWithTag(tag).performTextReplacement(texto) }
    private fun ocultarTeclado() {
        compose.runOnIdle {
            (compose.activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(compose.activity.window.decorView.windowToken, 0)
            compose.activity.currentFocus?.clearFocus()
        }
        compose.waitUntil(5_000) { ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) != true }
        compose.waitForIdle()
    }
    private fun capturar(nombre: String) {
        ocultarTeclado()
        val instrumentacion = InstrumentationRegistry.getInstrumentation()
        instrumentacion.waitForIdleSync()
        instrumentacion.uiAutomation.waitForIdle(150, 5_000)
        val captura = requireNotNull(instrumentacion.uiAutomation.takeScreenshot())
        val destino = File(compose.activity.getExternalFilesDir(null), "capturas-mejora-ia").apply { mkdirs() }
        FileOutputStream(File(destino, "$nombre.png")).use { captura.compress(Bitmap.CompressFormat.PNG, 100, it) }
        captura.recycle()
    }
}
