package com.tecsup.ayudacampus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AppTopBar(titulo: String, subtitulo: String? = null, onVolver: (() -> Unit)? = null, acciones: @Composable RowScope.() -> Unit = {}) {
    if (onVolver != null) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).heightIn(min = 40.dp)) {
            IconButton(onClick = onVolver, modifier = Modifier.align(Alignment.CenterStart).size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", modifier = Modifier.size(20.dp))
            }
            Column(Modifier.align(Alignment.Center).padding(horizontal = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                if (subtitulo != null) Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.align(Alignment.CenterEnd), content = acciones)
        }
    } else Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge)
            if (subtitulo != null) Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        acciones()
    }
}

@Composable
fun AyudaButton(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
        Text(texto, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AyudaTextField(valor: String, onValor: (String) -> Unit, etiqueta: String, modifier: Modifier = Modifier, error: String? = null, multilinea: Boolean = false, password: Boolean = false, icono: ImageVector? = null) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = valor, onValueChange = onValor, label = { Text(etiqueta) },
        modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        singleLine = !multilinea, minLines = if (multilinea) 3 else 1,
        isError = error != null,
        supportingText = if (error != null) ({ Text(error) }) else null,
        leadingIcon = if (icono != null) ({ Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }) else null,
        trailingIcon = if (password) ({ IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (visible) "Ocultar contraseña" else "Mostrar contraseña") } }) else null,
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
