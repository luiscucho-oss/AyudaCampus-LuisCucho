package com.tecsup.ayudacampus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.ayudacampus.model.Rol
import com.tecsup.ayudacampus.ui.cliente.SplashScreen
import com.tecsup.ayudacampus.ui.soporte.CategoriasScreen
import com.tecsup.ayudacampus.ui.theme.AyudaTheme
import com.tecsup.ayudacampus.viewmodel.AyudaViewModelFactory
import com.tecsup.ayudacampus.viewmodel.CategoriasViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AyudaApp
        setContent {
            val lista by app.lista.collectAsStateWithLifecycle()
            val error by app.error.collectAsStateWithLifecycle()
            AyudaTheme(rol = Rol.ADMIN) {
                Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                        when {
                            lista -> {
                                val categorias: CategoriasViewModel = viewModel(
                                    factory = AyudaViewModelFactory { CategoriasViewModel(app.repository) },
                                )
                                CategoriasScreen(categorias, onEditar = {}, onNueva = {})
                            }
                            error != null -> Box(
                                Modifier.fillMaxSize().padding(28.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text(error.orEmpty()) }
                            else -> SplashScreen()
                        }
                    }
                }
            }
        }
    }
}
