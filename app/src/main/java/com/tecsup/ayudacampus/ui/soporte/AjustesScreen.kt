package com.tecsup.ayudacampus.ui.soporte

import androidx.compose.runtime.Composable
import com.tecsup.ayudacampus.ui.cliente.PerfilContenido
import com.tecsup.ayudacampus.viewmodel.SesionViewModel

@Composable
fun AjustesScreen(vm: SesionViewModel) = PerfilContenido(vm, soporte = true)
