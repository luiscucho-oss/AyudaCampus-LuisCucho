package com.tecsup.ayudacampus.ui.soporte

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.ayudacampus.data.local.entity.Categoria
import com.tecsup.ayudacampus.ui.cliente.AvisoError
import com.tecsup.ayudacampus.ui.cliente.MensajeVacio
import com.tecsup.ayudacampus.ui.components.AppTopBar
import com.tecsup.ayudacampus.ui.components.IconoCategoria
import com.tecsup.ayudacampus.viewmodel.CategoriasViewModel

@Composable
fun CategoriasScreen(vm: CategoriasViewModel, onEditar: (Int) -> Unit, onNueva: () -> Unit) {
    val categorias by vm.categorias.collectAsState()
    val error by vm.error.collectAsState()
    var eliminar by remember { mutableStateOf<Categoria?>(null) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("Categorías", "Organiza las incidencias del campus", acciones = {
            IconButton(onClick = { vm.limpiarError(); vm.consumirGuardado(); onNueva() }) { Icon(Icons.Outlined.Add, "Nueva categoría") }
        })
        LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("lista-categorias"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Activa las categorías que pueden elegir los estudiantes.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (error != null) item { AvisoError(error) }
            if (categorias.isEmpty()) item { MensajeVacio("Aún no hay categorías") }
            items(categorias, key = { it.id }) { categoria ->
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().testTag("categoria-${categoria.id}")) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(46.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(IconoCategoria(categoria.icono), null, tint = MaterialTheme.colorScheme.primary) }
                            }
                            Column(Modifier.weight(1f).clickable { vm.limpiarError(); vm.consumirGuardado(); onEditar(categoria.id) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(categoria.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(if (categoria.activa) "Activa" else "Inactiva", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = categoria.activa, onCheckedChange = { vm.alternar(categoria) }, modifier = Modifier.testTag("categoria-switch-${categoria.id}"))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { vm.limpiarError(); vm.consumirGuardado(); onEditar(categoria.id) }) {
                                Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Editar")
                            }
                            TextButton(onClick = { eliminar = categoria }, modifier = Modifier.testTag("categoria-eliminar-${categoria.id}"), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Eliminar")
                            }
                        }
                    }
                }
            }
            item { OutlinedButton(onClick = { vm.limpiarError(); vm.consumirGuardado(); onNueva() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Nueva categoría")
            } }
        }
    }
    eliminar?.let { categoria ->
        AlertDialog(onDismissRequest = { eliminar = null }, title = { Text("Eliminar categoría") },
            text = { Text("¿Quieres eliminar «${categoria.nombre}»? Si tiene tickets asociados, puedes desactivarla para ocultarla a los estudiantes.") },
            confirmButton = { TextButton(onClick = { vm.eliminar(categoria); eliminar = null }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { eliminar = null }) { Text("Cancelar") } })
    }
}
