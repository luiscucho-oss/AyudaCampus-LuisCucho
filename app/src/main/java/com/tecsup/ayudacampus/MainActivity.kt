package com.tecsup.ayudacampus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.ayudacampus.model.Rol
import com.tecsup.ayudacampus.ui.cliente.SplashScreen
import com.tecsup.ayudacampus.ui.navigation.NavAyuda
import com.tecsup.ayudacampus.ui.theme.AyudaTheme
import com.tecsup.ayudacampus.viewmodel.ModelosAyuda
import com.tecsup.ayudacampus.viewmodel.SesionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AyudaApp
        val modelos = ModelosAyuda(app.repository, app.preferencias)
        setContent {
            val lista by app.lista.collectAsStateWithLifecycle()
            val error by app.error.collectAsStateWithLifecycle()
            if (lista) {
                val sesion: SesionViewModel = viewModel(factory = modelos.sesion())
                val usuario by sesion.usuarioActual.collectAsStateWithLifecycle()
                val oscuro by sesion.modoOscuro.collectAsStateWithLifecycle()
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !oscuro
                        isAppearanceLightNavigationBars = !oscuro
                    }
                }
                AyudaTheme(if (usuario?.rol == Rol.ADMIN.name) Rol.ADMIN else Rol.CLIENTE, oscuro) {
                    NavAyuda(sesion, modelos)
                }
            } else AyudaTheme {
                if (error == null) SplashScreen()
                else Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) { Text(error.orEmpty()) }
                }
            }
        }
    }
}
