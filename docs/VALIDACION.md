# Validación de AyudaCampus

Autor: Luis Cucho

Fecha: 14 de octubre de 2026

Entorno: Pixel 4, Android 16 (API 36), arquitectura arm64.

## Resultado

**39 pruebas superadas, sin fallos en la verificación final.** La aplicación y su APK de pruebas compilaron correctamente. Lint terminó con 0 errores y 16 advertencias sobre versiones/API y comprobadores de Navigation de las dependencias indicadas en la guía; no impiden compilar ni ejecutar.

| Suite | Tipo | Pruebas | Resultado |
|---|---|---:|---|
| ReglasTicketTest | JVM | 14 | OK |
| DatabaseIntegrationTest | Room en emulador | 9 | OK |
| PersistenciaProcesoTest | Base y preferencias reales | 1 | OK |
| SesionYFormularioTest | ViewModels, Room y DataStore reales | 14 | OK |
| FlujoAceptacionTest | Recorrido de interfaz Compose | 1 | OK |
| **Total** | | **39** | **OK** |

Además, PersistenciaProcesoTest se ejecutó en dos fases separadas por `am force-stop`: preparar y verificar. La segunda ejecución abrió otro proceso y confirmó que se conservaron el ticket, su conversación, estado, prioridad, sesión, modo oscuro, notificaciones y recepción de tickets. Al finalizar se borró únicamente el fixture de esa comprobación y se restauraron las preferencias de práctica.

## Pruebas de aceptación

| Prueba de la guía | Resultado comprobado |
|---|---|
| Login con clave incorrecta | OK: mensaje de error, permanece en el login. |
| Crear ticket vacío | OK: errores junto a los campos y ningún ticket creado. |
| Crear ticket válido | OK: aparece como ABIERTO con la prioridad elegida. |
| Bandeja de Soporte | OK: recibe el ticket nuevo y conserva prioridad ALTA, MEDIA, BAJA y fecha descendente. |
| Pasar a En proceso y responder | OK: el estudiante recibe el estado y el comentario sin reiniciar la app. |
| Soporte intenta cerrar | OK: Cerrado aparece deshabilitado; el ViewModel también rechaza la operación. |
| Marcar Resuelto, calificar y cerrar | OK: aviso de resolución, cuatro estrellas, estado CERRADO y fecha de cierre. |
| Reabrir un ticket resuelto | OK: vuelve a ABIERTO y se limpian calificación y fecha de cierre. |
| Comentar un ticket cerrado | OK: el campo desaparece y la regla impide nuevas escrituras. |
| Panel y alerta | OK: conteos y gráficos corresponden a Room; alerta de ALTA/ABIERTO con más de una hora. |
| Desactivar categoría | OK: desaparece de las opciones para crear tickets. |
| Cerrar y abrir la app | OK: Room, sesión y preferencias se conservan entre procesos. |

También se comprobaron categoría en uso, claves foráneas, cascada de comentarios, categorías sin tickets, observación reactiva, permisos por rol, pertenencia del ticket, cambio de perfil, modo oscuro, notificaciones, recepción cerrada y prevención del doble envío.

## Reproducir las pruebas

```bash
./gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug
./gradlew connectedDebugAndroidTest
```

La verificación final del dispositivo se ejecutó directamente con AndroidJUnitRunner. Este modo también sirve cuando el controlador de pruebas de Gradle no inicia en el entorno:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r com.tecsup.ayudacampus.test/androidx.test.runner.AndroidJUnitRunner
```

Para repetir específicamente la comprobación entre procesos:

```bash
adb shell am instrument -w -e class com.tecsup.ayudacampus.data.PersistenciaProcesoTest -e fase preparar com.tecsup.ayudacampus.test/androidx.test.runner.AndroidJUnitRunner
adb shell am force-stop com.tecsup.ayudacampus
adb shell am instrument -w -e class com.tecsup.ayudacampus.data.PersistenciaProcesoTest -e fase verificar com.tecsup.ayudacampus.test/androidx.test.runner.AndroidJUnitRunner
```

El recorrido de interfaz reconstruye los datos ficticios de esta app para que la prueba sea repetible y guarda seis capturas en el directorio externo privado de la aplicación. Ejecuta estas pruebas en un emulador de práctica.

## Historial de implementación

Los trece commits de implementación se exportaron y compilaron por separado con `assembleDebug`. El paso final añade README, capturas, documentación y pruebas, sobre la aplicación verificada.

| N.º | Commit | Paso | Compila |
|---|---|---|---|
| 1 | `7595c83` | chore: importa proyecto base AyudaCampus | OK |
| 2 | `77db2d5` | feat(data): agrega entidades Usuario, Ticket y Comentario | OK |
| 3 | `68cd318` | feat(data): agrega DAOs y relacion TicketConDetalle | OK |
| 4 | `e1f7e66` | feat(data): registra entidades y siembra datos iniciales en AyudaDatabase | OK |
| 5 | `b06d794` | feat(data): implementa AyudaRepositoryRoom con todas las operaciones | OK |
| 6 | `895bc50` | feat(data): agrega PreferenciasStore con DataStore | OK |
| 7 | `d41c322` | feat(viewmodel): agrega SesionViewModel y login con selector de rol | OK |
| 8 | `fb2a736` | feat(ui): agrega mis tickets con busqueda y filtros por estado | OK |
| 9 | `81d9f85` | feat(ui): agrega nuevo ticket con validaciones | OK |
| 10 | `6b5f47e` | feat(ui): agrega detalle del ticket, conversacion y cierre con calificacion | OK |
| 11 | `4824f86` | feat(soporte): agrega panel con graficos y bandeja de tickets | OK |
| 12 | `5444afa` | feat(soporte): agrega atencion de tickets y formulario de categorias | OK |
| 13 | `15909ab` | feat: agrega perfil, ajustes con DataStore y cambio de perfil | OK |
| 14 | Documentación final | docs: agrega README con estructura y capturas | OK, aplicación y pruebas verificadas |

## Capturas

Las seis imágenes del README son capturas reales de la aplicación, obtenidas durante el recorrido de aceptación. El panel espera los datos de Room antes de capturar y los valores mostrados corresponden a los tickets presentes en ese momento.
