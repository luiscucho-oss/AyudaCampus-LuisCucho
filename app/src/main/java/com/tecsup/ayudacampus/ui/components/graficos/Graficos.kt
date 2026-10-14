package com.tecsup.ayudacampus.ui.components.graficos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.ayudacampus.model.DatoGrafico
import kotlin.math.max

@Composable
fun GraficoDona(datos: List<DatoGrafico>, modifier: Modifier = Modifier) {
    val total = datos.sumOf { it.valor.toDouble() }.toFloat()
    val empty = MaterialTheme.colorScheme.outline
    Row(modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        Box(Modifier.size(125.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().semantics { contentDescription = datos.joinToString { "${it.etiqueta}: ${it.valor.toInt()}" } }) {
                val stroke = 22.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                if (total <= 0f) drawArc(empty, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                else {
                    var start = -90f
                    datos.forEach { d ->
                        val sweep = d.valor / total * 360f
                        if (sweep > 0f) drawArc(Color(d.color), start, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                        start += sweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(total.toInt().toString(), style = MaterialTheme.typography.headlineSmall)
                Text("Tickets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            datos.forEach { d ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.size(8.dp).background(Color(d.color), RoundedCornerShape(2.dp)))
                    Text(d.etiqueta, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    Text(d.valor.toInt().toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GraficoBarras(datos: List<DatoGrafico>, modifier: Modifier = Modifier) {
    if (datos.isEmpty()) {
        Text("Aún no hay categorías", modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val maxValue = max(1f, datos.maxOf { it.valor })
    Row(modifier.fillMaxWidth().height(158.dp).padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        datos.forEach { d ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Text(d.valor.toInt().toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height((d.valor / maxValue * 88).coerceAtLeast(3f).dp).background(Color(d.color), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)))
                Spacer(Modifier.height(6.dp))
                Text(when (d.etiqueta) { "Red / Wi-Fi" -> "Red"; "Impresión" -> "Impresión"; else -> d.etiqueta }, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}
