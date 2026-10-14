package com.tecsup.ayudacampus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tecsup.ayudacampus.model.Rol

@Composable
fun AyudaTheme(rol: Rol = Rol.CLIENTE, oscuro: Boolean = false, content: @Composable () -> Unit) {
    val soporte = rol == Rol.ADMIN
    val primary = if (soporte) Violeta else Cian
    val colors = if (oscuro) darkColorScheme(
        primary = if (soporte) Color(0xFFC4B5FD) else Color(0xFF67E8F9),
        onPrimary = Color(0xFF111827),
        primaryContainer = if (soporte) Color(0xFF352458) else Color(0xFF134552),
        onPrimaryContainer = Color(0xFFF3F4F6),
        secondaryContainer = Color(0xFF293342),
        background = Color(0xFF111827), surface = Color(0xFF1F2937),
        surfaceVariant = Color(0xFF293342), onBackground = Color(0xFFF3F4F6),
        onSurface = Color(0xFFF3F4F6), onSurfaceVariant = Color(0xFFCBD5E1),
        outline = Color(0xFF475569)
    ) else lightColorScheme(
        primary = primary, onPrimary = Color.White,
        primaryContainer = if (soporte) Color(0xFFEDE4FF) else Color(0xFFCFFAFE),
        onPrimaryContainer = primary,
        secondaryContainer = Color(0xFFF3F4F6), onSecondaryContainer = TextoSecundario,
        background = if (soporte) Color(0xFFFAF8FF) else Color(0xFFF7FDFE),
        onBackground = Texto, surface = Color.White, onSurface = Texto,
        surfaceVariant = Color(0xFFF3F4F6), onSurfaceVariant = TextoSecundario,
        outline = Color(0xFFE5E7EB), outlineVariant = Color(0xFFE5E7EB)
    )
    MaterialTheme(colorScheme = colors, typography = AyudaTypography, content = content)
}
