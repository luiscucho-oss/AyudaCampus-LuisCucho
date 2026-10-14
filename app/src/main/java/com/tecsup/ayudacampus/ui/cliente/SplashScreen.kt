package com.tecsup.ayudacampus.ui.cliente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.SupportAgent, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(22.dp).size(56.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("AyudaCampus", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Tu campus, conectado", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            CircularProgressIndicator(Modifier.padding(top = 32.dp).size(28.dp), strokeWidth = 3.dp)
        }
    }
}
