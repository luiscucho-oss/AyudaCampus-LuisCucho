package com.tecsup.ayudacampus.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.tecsup.ayudacampus.model.Rol
import com.tecsup.ayudacampus.ui.cliente.*
import com.tecsup.ayudacampus.ui.soporte.*
import com.tecsup.ayudacampus.viewmodel.*

private data class Destino(val ruta: String, val texto: String, val icono: ImageVector)

@Composable
fun NavAyuda(sesion: SesionViewModel, modelos: ModelosAyuda) {
    val usuario by sesion.usuarioActual.collectAsStateWithLifecycle()
    val listo by sesion.listo.collectAsStateWithLifecycle()
    if (!listo) { SplashScreen(); return }
    val actual = usuario
    if (actual == null) { LoginScreen(sesion); return }
    // Cada sesión tiene su propio grafo y sus ViewModels: cambiar perfil elimina la pila anterior.
    key(actual.id, actual.rol) {
        val nav = rememberNavController()
        val soporte = actual.rol == Rol.ADMIN.name
        val inicio = if (soporte) Rutas.PANEL else Rutas.TICKETS
        val destinos = if (soporte) listOf(
            Destino(Rutas.PANEL, "Panel", Icons.Default.BarChart),
            Destino(Rutas.BANDEJA, "Tickets", Icons.AutoMirrored.Filled.FormatListBulleted),
            Destino(Rutas.CATEGORIAS, "Categorías", Icons.Default.LocalOffer),
            Destino(Rutas.AJUSTES, "Ajustes", Icons.Default.Settings)
        ) else listOf(
            Destino(Rutas.TICKETS, "Tickets", Icons.AutoMirrored.Filled.FormatListBulleted),
            Destino(Rutas.NUEVO, "Nuevo", Icons.Default.Add),
            Destino(Rutas.PERFIL, "Perfil", Icons.Default.PersonOutline)
        )
        val entrada by nav.currentBackStackEntryAsState()
        val ruta = entrada?.destination?.route ?: inicio
        val seleccionada = when (ruta) {
            Rutas.DETALLE -> Rutas.TICKETS
            Rutas.ATENDER -> Rutas.BANDEJA
            Rutas.NUEVA_CATEGORIA, Rutas.EDITAR_CATEGORIA -> Rutas.CATEGORIAS
            else -> ruta
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            bottomBar = {
                if (ruta != Rutas.NUEVA_CATEGORIA && ruta != Rutas.EDITAR_CATEGORIA) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, modifier = Modifier.heightIn(min = 64.dp)) {
                    destinos.forEach { d ->
                        NavigationBarItem(
                            selected = seleccionada == d.ruta,
                            onClick = {
                                nav.navigate(d.ruta) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(d.icono, null, modifier = Modifier.size(23.dp)) },
                            label = { Text(d.texto, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.testTag("nav-${d.ruta}")
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(nav, startDestination = inicio, modifier = Modifier.fillMaxSize().padding(padding)) {
                if (!soporte) {
                    composable(Rutas.TICKETS) { entry ->
                        val vm: MisTicketsViewModel = viewModel(entry, factory = modelos.misTickets(actual))
                        MisTicketsScreen(vm, { nav.navigate(Rutas.detalle(it)) }, { nav.navigate(Rutas.NUEVO) })
                    }
                    composable(Rutas.NUEVO) { entry ->
                        val vm: NuevoTicketViewModel = viewModel(entry, factory = modelos.nuevoTicket(actual))
                        NuevoTicketScreen(vm, { nav.popBackStack() }, {
                            nav.navigate(Rutas.TICKETS) { popUpTo(Rutas.TICKETS) { inclusive = true }; launchSingleTop = true }
                        })
                    }
                    composable(Rutas.DETALLE, arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                        val vm: TicketDetalleViewModel = viewModel(entry, factory = modelos.detalle(actual, entry.arguments!!.getInt("id")))
                        DetalleTicketScreen(vm) { nav.popBackStack() }
                    }
                    composable(Rutas.PERFIL) { PerfilScreen(sesion) }
                } else {
                    composable(Rutas.PANEL) { entry ->
                        val vm: PanelViewModel = viewModel(entry, factory = modelos.panel())
                        PanelScreen(vm, { nav.navigate(Rutas.BANDEJA) }, { nav.navigate(Rutas.atender(it)) })
                    }
                    composable(Rutas.BANDEJA) { entry ->
                        val vm: BandejaViewModel = viewModel(entry, factory = modelos.bandeja())
                        BandejaScreen(vm) { nav.navigate(Rutas.atender(it)) }
                    }
                    composable(Rutas.ATENDER, arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                        val vm: AtenderViewModel = viewModel(entry, factory = modelos.atender(actual, entry.arguments!!.getInt("id")))
                        AtenderTicketScreen(vm) { nav.popBackStack() }
                    }
                    composable(Rutas.CATEGORIAS) { entry ->
                        val vm: CategoriasViewModel = viewModel(entry, factory = modelos.categorias())
                        CategoriasScreen(vm, { nav.navigate(Rutas.editarCategoria(it)) }, { nav.navigate(Rutas.NUEVA_CATEGORIA) })
                    }
                    composable(Rutas.NUEVA_CATEGORIA) { entry ->
                        val vm: CategoriasViewModel = viewModel(entry, factory = modelos.categorias())
                        FormularioCategoriaScreen(vm, null) { nav.popBackStack() }
                    }
                    composable(Rutas.EDITAR_CATEGORIA, arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                        val vm: CategoriasViewModel = viewModel(entry, factory = modelos.categorias())
                        FormularioCategoriaScreen(vm, entry.arguments!!.getInt("id")) { nav.popBackStack() }
                    }
                    composable(Rutas.AJUSTES) { AjustesScreen(sesion) }
                }
            }
        }
    }
}
