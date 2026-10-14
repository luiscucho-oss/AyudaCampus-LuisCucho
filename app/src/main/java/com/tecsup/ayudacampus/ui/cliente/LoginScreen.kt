package com.tecsup.ayudacampus.ui.cliente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.ayudacampus.model.Rol
import com.tecsup.ayudacampus.ui.components.AyudaButton
import com.tecsup.ayudacampus.ui.components.AyudaTextField
import com.tecsup.ayudacampus.ui.theme.AyudaTheme
import com.tecsup.ayudacampus.viewmodel.SesionViewModel

@Composable
fun LoginScreen(vm: SesionViewModel) {
    var rol by rememberSaveable { mutableStateOf(Rol.CLIENTE) }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var ayuda by rememberSaveable { mutableStateOf(false) }
    val error by vm.error.collectAsState()
    val oscuro by vm.modoOscuro.collectAsState()
    AyudaTheme(rol = rol, oscuro = oscuro) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 22.dp, vertical = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Spacer(Modifier.height(12.dp))
                Text("Bienvenido", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Ingresa para continuar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf(Rol.CLIENTE to "Estudiante", Rol.ADMIN to "Soporte TI").forEach { (opcion, etiqueta) ->
                            val seleccionado = rol == opcion
                            Surface(shape = RoundedCornerShape(11.dp),
                                color = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.weight(1f).height(38.dp).selectable(selected = seleccionado, role = Role.RadioButton,
                                    onClick = { rol = opcion; vm.limpiarError() })) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(etiqueta, style = MaterialTheme.typography.labelMedium,
                                        color = if (seleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                AyudaTextField(correo, { correo = it; vm.limpiarError() }, "Correo", Modifier.fillMaxWidth().testTag("login-correo"), icono = Icons.Outlined.Email)
                AyudaTextField(clave, { clave = it; vm.limpiarError() }, "Contraseña", Modifier.fillMaxWidth().testTag("login-clave"), password = true, icono = Icons.Outlined.Lock)
                TextButton(onClick = { ayuda = true }, modifier = Modifier.fillMaxWidth()) { Text("¿Olvidaste tu contraseña?") }
                AvisoError(error)
                AyudaButton("Ingresar", { vm.login(correo.trim(), clave, rol) }, Modifier.fillMaxWidth().testTag("login-ingresar"))
                if (rol == Rol.CLIENTE) {
                    TextButton(onClick = { ayuda = true }, modifier = Modifier.fillMaxWidth()) { Text("¿No tienes cuenta?  Regístrate") }
                } else {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Modo práctica", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text("Puedes cambiar de perfil desde Ajustes para probar ambos roles.", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (ayuda) AlertDialog(onDismissRequest = { ayuda = false }, title = { Text("Cuentas de práctica") },
                text = { Text("Estudiante: juan@campus.test\nContraseña: 1234\n\nSoporte TI: soporte@campus.test\nContraseña: admin\n\nLa actividad utiliza estas cuentas locales. Puedes cambiar de perfil al ingresar.") },
                confirmButton = { TextButton(onClick = { ayuda = false }) { Text("Entendido") } })
        }
    }
}
