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
import com.tecsup.ayudacampus.model.EstadoTicket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/** Verifica el ciclo completo en un dispositivo y conserva seis capturas de las pantallas reales. */
@RunWith(AndroidJUnit4::class)
class FlujoAceptacionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app: AyudaApp get() = compose.activity.application as AyudaApp

    @Before
    fun prepararSesion() {
        runBlocking {
            withTimeout(30_000) { app.lista.first { it } }
            app.preferencias.guardarModoOscuro(false)
            app.preferencias.guardarNotificaciones(true)
            app.preferencias.guardarRecibirTickets(true)
            app.preferencias.cerrarSesion()
        }
        esperarTag("login-correo")
        runBlocking {
            withContext(Dispatchers.IO) {
                // Base exclusiva de esta aplicación de pruebas: cada ejecución parte del mismo ejemplo.
                app.database.clearAllTables()
                DatosIniciales.sembrar(app.database)
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun cicloDeAtencionCategoriasYPreferencias() {
        val sufijo = System.currentTimeMillis().toString().takeLast(6)
        val titulo = "Sin Wi-Fi en el laboratorio 3"
        val categoriaNueva = "Laboratorios $sufijo"
        val comentario = "La conexión se pierde durante la clase."
        val respuesta = "Reiniciamos el equipo. ¿Ya tienes señal?"

        // Una clave incorrecta conserva el login y muestra un error legible.
        escribir("login-correo", "juan@campus.test")
        escribir("login-clave", "incorrecta")
        pulsarTag("login-ingresar")
        esperarTexto("Correo o contraseña incorrectos")
        compose.onNodeWithTag("login-correo").assertExists()
        escribir("login-clave", "1234")
        pulsarTag("login-ingresar")
        esperarTexto("Mis tickets")
        compose.waitUntil(10_000) { compose.onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag("lista-tickets"))).fetchSemanticsNodes().isNotEmpty() }
        capturar("01-cliente-mis-tickets")

        // Los tres errores aparecen junto a sus campos; no se crea un ticket vacío.
        pulsarTag("nav-nuevo")
        esperarTag("ticket-enviar")
        pulsarTag("ticket-enviar")
        esperarTexto("Elige una categoría")
        compose.onNodeWithText("Escribe al menos 5 caracteres").assertExists()
        compose.onNodeWithText("Escribe al menos 10 caracteres").assertExists()
        compose.onNodeWithText("Red / Wi-Fi").performClick()
        escribir("ticket-titulo", titulo)
        escribir("ticket-ubicacion", "Lab 3 · Pabellón B")
        compose.onNodeWithText("Alta").performScrollTo().performClick()
        escribir("ticket-descripcion", "Desde ayer no hay señal de Wi-Fi en todo el laboratorio. Afecta a 30 alumnos durante la clase.")
        ocultarTeclado()
        compose.onNodeWithTag("ticket-enviar").performScrollTo()
        capturar("02-cliente-nuevo-ticket")
        pulsarTag("ticket-enviar")
        esperarTexto("Mis tickets")
        abrirTicket(titulo, "lista-tickets")
        esperarTag("comentario-texto")
        val creado = runBlocking {
            withTimeout(10_000) { app.repository.observarTodosLosTickets().first { lista -> lista.any { it.ticket.titulo == titulo } }.first { it.ticket.titulo == titulo } }
        }
        assertEquals(EstadoTicket.ABIERTO.name, creado.ticket.estado)
        assertEquals("ALTA", creado.ticket.prioridad)
        escribir("comentario-texto", comentario)
        compose.onNodeWithContentDescription("Enviar comentario").performClick()
        esperarTexto(comentario)
        ocultarTeclado()
        capturar("03-cliente-detalle-ticket")

        cambiarASoporte()
        val datosPanel = runBlocking { app.repository.observarTodosLosTickets().first() }
        val descripcionGrafico = listOf(
            "Abiertos" to EstadoTicket.ABIERTO, "En proceso" to EstadoTicket.EN_PROCESO,
            "Resueltos" to EstadoTicket.RESUELTO, "Cerrados" to EstadoTicket.CERRADO,
        ).joinToString { (etiqueta, estado) -> "$etiqueta: ${datosPanel.count { it.ticket.estado == estado.name }}" }
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription(descripcionGrafico).fetchSemanticsNodes().isNotEmpty() }
        esperarTexto("Hardware")
        listOf("abiertos" to EstadoTicket.ABIERTO, "proceso" to EstadoTicket.EN_PROCESO, "resueltos" to EstadoTicket.RESUELTO).forEach { (tag, estado) ->
            val esperado = datosPanel.count { it.ticket.estado == estado.name }.toString()
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasText(esperado) and hasAnyAncestor(hasTestTag("panel-$tag")), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
        }
        capturar("04-soporte-panel")
        pulsarTag("nav-bandeja")
        esperarTag("lista-bandeja")
        compose.onNodeWithTag("lista-bandeja").performScrollToNode(hasText(titulo))
        capturar("05-soporte-bandeja")
        abrirTicket(titulo, "lista-bandeja")
        esperarTexto("Guardar cambios")
        compose.onNodeWithText("Cerrado").assertIsNotEnabled()
        capturar("06-soporte-atender-ticket")
        compose.onNodeWithText("En proceso").performClick()
        escribir("comentario-texto", respuesta)
        compose.onNodeWithContentDescription("Enviar comentario").performClick()
        esperarTexto(respuesta)
        pulsarTexto("Guardar cambios")
        esperarEstado(creado.ticket.id, EstadoTicket.EN_PROCESO)

        // El estudiante recibe la respuesta y el estado sin reiniciar la aplicación.
        cambiarAEstudiante()
        abrirTicket(titulo, "lista-tickets")
        esperarTexto(respuesta)
        compose.onAllNodesWithText("En proceso")[0].assertExists()
        cambiarASoporte()
        resolver(titulo, creado.ticket.id)

        // Reabrir borra la calificación; posteriormente el estudiante cierra con cuatro estrellas.
        cambiarAEstudiante()
        abrirTicket(titulo, "lista-tickets")
        esperarTexto("resuelto. ¿El problema se solucionó?")
        pulsarTexto("Reabrir ticket")
        esperarEstado(creado.ticket.id, EstadoTicket.ABIERTO)
        assertEquals(null, runBlocking { app.repository.observarTicket(creado.ticket.id).first()?.ticket?.calificacion })
        cambiarASoporte()
        resolver(titulo, creado.ticket.id)
        cambiarAEstudiante()
        abrirTicket(titulo, "lista-tickets")
        esperarTexto("Califica la atención")
        compose.onNodeWithTag("ticket-cerrar").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("4 estrellas").performScrollTo().performClick()
        escribir("comentario-final", "Atención rápida, gracias.")
        pulsarTag("ticket-cerrar")
        esperarEstado(creado.ticket.id, EstadoTicket.CERRADO)
        esperarTexto("Ticket cerrado")
        val cerrado = runBlocking { app.repository.observarTicket(creado.ticket.id).first() }
        assertEquals(4, cerrado?.ticket?.calificacion)
        assertNotNull(cerrado?.ticket?.fechaCierre)
        compose.onNodeWithTag("comentario-texto").assertDoesNotExist()

        // Una categoría usada se conserva; una nueva admite desactivación y desaparece del formulario.
        cambiarASoporte()
        pulsarTag("nav-categorias")
        esperarTag("lista-categorias")
        val borrarUsada = "categoria-eliminar-${creado.ticket.categoriaId}"
        compose.onNodeWithTag("lista-categorias").performScrollToNode(hasTestTag(borrarUsada))
        pulsarTag(borrarUsada)
        compose.onNode(hasText("Eliminar") and hasAnyAncestor(isDialog()), useUnmergedTree = true).performClick()
        esperarTexto("No se puede eliminar una categoría que ya tiene tickets. Puedes desactivarla.")
        compose.onNodeWithContentDescription("Nueva categoría").performClick()
        esperarTag("categoria-nombre")
        pulsarTag("categoria-guardar")
        esperarTexto("El nombre es obligatorio")
        escribir("categoria-nombre", categoriaNueva)
        compose.onNodeWithContentDescription("Etiqueta").performClick()
        pulsarTag("categoria-guardar")
        esperarTag("lista-categorias")
        val nueva = runBlocking { app.repository.observarCategorias().first { lista -> lista.any { it.nombre == categoriaNueva } }.first { it.nombre == categoriaNueva } }
        val tagSwitch = "categoria-switch-${nueva.id}"
        compose.onNodeWithTag("lista-categorias").performScrollToNode(hasTestTag(tagSwitch))
        pulsarTag(tagSwitch)
        compose.waitUntil(10_000) { runBlocking { app.repository.obtenerCategoria(nueva.id)?.activa == false } }
        compose.onNodeWithTag(tagSwitch).assertIsOff()
        cambiarAEstudiante()
        pulsarTag("nav-nuevo")
        esperarTag("ticket-enviar")
        compose.onNodeWithText(categoriaNueva).assertDoesNotExist()

        // Recrear Activity comprueba que perfil, modo oscuro y notificaciones sobreviven.
        cambiarASoporte()
        pulsarTag("nav-ajustes")
        esperarTag("preferencia-oscuro")
        pulsarTag("preferencia-oscuro")
        pulsarTag("preferencia-notificaciones")
        esperarPreferencias(oscuro = true, notificaciones = false)
        compose.activityRule.scenario.recreate()
        esperarTag("nav-ajustes")
        pulsarTag("nav-ajustes")
        esperarTag("preferencia-oscuro")
        compose.onNodeWithTag("preferencia-oscuro").assertIsOn()
        compose.onNodeWithTag("preferencia-notificaciones").assertIsOff()
        pulsarTag("preferencia-recibir")
        cambiarAEstudiante()
        pulsarTag("nav-nuevo")
        esperarTexto("Mesa de ayuda cerrada", substring = true)
        compose.onNodeWithTag("ticket-enviar").performScrollTo().assertIsNotEnabled()

        // Deja la mesa de ayuda disponible y el tema claro tras la verificación.
        cambiarASoporte()
        pulsarTag("nav-ajustes")
        esperarTag("preferencia-recibir")
        pulsarTag("preferencia-recibir")
        pulsarTag("preferencia-oscuro")
        pulsarTag("preferencia-notificaciones")
        esperarPreferencias(oscuro = false, notificaciones = true)
        assertTrue(runBlocking { app.preferencias.recibirTickets.first() })
    }

    private fun resolver(titulo: String, id: Int) {
        pulsarTag("nav-bandeja")
        esperarTag("lista-bandeja")
        abrirTicket(titulo, "lista-bandeja")
        esperarTexto("Guardar cambios")
        compose.onNodeWithText("Resuelto").performClick()
        pulsarTexto("Guardar cambios")
        esperarEstado(id, EstadoTicket.RESUELTO)
    }

    private fun cambiarASoporte() {
        pulsarTag("nav-perfil")
        esperarTexto("Cambiar de perfil")
        pulsarTexto("Cambiar de perfil")
        esperarTexto("Panel de soporte")
    }

    private fun cambiarAEstudiante() {
        pulsarTag("nav-ajustes")
        esperarTexto("Cambiar de perfil")
        pulsarTexto("Cambiar de perfil")
        esperarTexto("Mis tickets")
    }

    private fun abrirTicket(titulo: String, lista: String) {
        esperarTag(lista)
        compose.onNodeWithTag(lista).performScrollToNode(hasText(titulo))
        compose.onAllNodesWithText(titulo)[0].performClick()
    }

    private fun escribir(tag: String, texto: String) {
        esperarTag(tag)
        hacerVisible(compose.onNodeWithTag(tag)).performTextReplacement(texto)
    }

    private fun pulsarTag(tag: String) {
        esperarTag(tag)
        val nodo = hacerVisible(compose.onNodeWithTag(tag))
        nodo.performClick()
    }

    private fun pulsarTexto(texto: String) {
        esperarTexto(texto)
        hacerVisible(compose.onNodeWithText(texto)).performClick()
    }

    private fun hacerVisible(nodo: SemanticsNodeInteraction): SemanticsNodeInteraction {
        try { nodo.assertIsDisplayed() } catch (_: AssertionError) { nodo.performScrollTo() }
        return nodo
    }

    private fun esperarTag(tag: String) {
        compose.waitUntil(20_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun esperarTexto(texto: String, substring: Boolean = false) {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(texto, substring = substring).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun esperarEstado(id: Int, esperado: EstadoTicket) {
        compose.waitUntil(20_000) { runBlocking { app.repository.observarTicket(id).first()?.ticket?.estado == esperado.name } }
        compose.waitForIdle()
    }

    private fun esperarPreferencias(oscuro: Boolean, notificaciones: Boolean) {
        compose.waitUntil(10_000) { runBlocking { app.preferencias.modoOscuro.first() == oscuro && app.preferencias.notificaciones.first() == notificaciones } }
    }

    private fun ocultarTeclado() {
        compose.runOnIdle {
            val servicio = compose.activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            servicio.hideSoftInputFromWindow(compose.activity.window.decorView.windowToken, 0)
            compose.activity.currentFocus?.clearFocus()
        }
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) != true
        }
        compose.waitForIdle()
    }

    private fun capturar(nombre: String) {
        ocultarTeclado()
        compose.waitForIdle()
        val instrumentacion = InstrumentationRegistry.getInstrumentation()
        instrumentacion.waitForIdleSync()
        instrumentacion.uiAutomation.waitForIdle(150, 5_000)
        val captura = requireNotNull(instrumentacion.uiAutomation.takeScreenshot())
        var muestras = 0
        var muestrasVisibles = 0
        val pasoX = (captura.width / 20).coerceAtLeast(1)
        val pasoY = (captura.height / 30).coerceAtLeast(1)
        for (x in captura.width / 10 until captura.width * 9 / 10 step pasoX) {
            for (y in captura.height / 10 until captura.height * 9 / 10 step pasoY) {
                val pixel = captura.getPixel(x, y)
                muestras++
                if (android.graphics.Color.red(pixel) + android.graphics.Color.green(pixel) + android.graphics.Color.blue(pixel) > 90) muestrasVisibles++
            }
        }
        assertTrue("La captura $nombre debe contener la pantalla visible de la aplicación", muestrasVisibles > muestras * .35f)
        val directorio = File(compose.activity.getExternalFilesDir(null), "capturas").apply { mkdirs() }
        FileOutputStream(File(directorio, "$nombre.png")).use { captura.compress(Bitmap.CompressFormat.PNG, 100, it) }
        captura.recycle()
    }
}
