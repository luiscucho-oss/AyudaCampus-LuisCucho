# Validación de mejora-ia

Autor: Luis Cucho · Fecha: 14 de octubre de 2026

## Resultado ejecutado

| Comprobación | Resultado |
|---|---|
| APK de depuración (`assembleDebug`) | Correcto |
| Pruebas JVM (`testDebugUnitTest`) | 25 correctas, 0 fallos |
| APK de pruebas (`assembleDebugAndroidTest`) | Correcto |
| Instrumentación real en Pixel 4, Android 16 / API 36 | 28 correctas, 0 fallos |
| Android Lint (`lintDebug`) | 0 errores, 16 advertencias |
| Total de casos | 53 correctos, 0 fallos |

El listado individual está en [RESULTADOS-MEJORA-IA.json](RESULTADOS-MEJORA-IA.json). Las advertencias de Lint corresponden a versiones de dependencias fijadas para la práctica, target SDK, verificadores de Navigation y metadatos de copia de seguridad. Se conservaron las dependencias y el esquema de Room de la primera fase.

## Casos relevantes de las tres mejoras

- **Promedio:** varios cierres válidos, ausencia de datos, fecha nula, fecha anterior a creación y exclusión de estados distintos de CERRADO. Un observador real de Room recibe el cambio al cerrar y reabrir. La interfaz pasa de 24 h a 48 h y vuelve a 24 h sin reiniciar.
- **SLA:** 4/24/72 horas, vencimiento exacto, tiempo vencido sin negativos, aviso en el último cuarto, fracciones de minuto, cambio de prioridad con fecha original, resuelto/cerrado y reapertura. Un reloj virtual comprueba que todas las tarjetas comparten una actualización y que el flujo se detiene sin observadores.
- **Bandeja:** estado, prioridad, categoría y texto simultáneos; #0012, número sin ceros, nombre sin tildes y espacios; categorías inactivas, resultados vacíos, limpieza y conservación del orden del Repository. También reacciona a modificaciones de los datos con los filtros activos.
- **Interfaz:** el recorrido completo de la primera fase sigue pasando. El recorrido de mejoras usa los filtros, abre el resultado, cambia la prioridad y comprueba el nuevo SLA en los detalles de ambos perfiles. Se revisó la presentación clara y oscura con capturas reales.

## Ejecución reproducible

```bash
./gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell am instrument -w -r -e disableAnalytics true com.tecsup.ayudacampus.test/androidx.test.runner.AndroidJUnitRunner
```

Se utilizó AndroidJUnitRunner directamente, porque el lanzador `connectedDebugAndroidTest` había quedado detenido al iniciar UTP en este entorno durante la primera fase. No se cambió el alcance de la suite.

## Ajustes de la verificación

Los primeros intentos de interfaz fallaron porque el emulador estaba en reposo. Se activó la pantalla y se hicieron tolerantes las esperas al arranque de Compose. El nuevo test también necesitó consultar el árbol semántico sin fusionar para encontrar el indicador dentro de una tarjeta clicable. Después de estos ajustes, la suite completa terminó con 28 pruebas instrumentadas correctas. Estos ajustes fueron de preparación y consultas de prueba; no alteraron las reglas de la aplicación.

## Capturas y alcance

Las cinco imágenes de [capturas-mejora-ia](capturas-mejora-ia/) proceden de la aplicación ejecutada en el emulador, sin recrear la interfaz. El test utiliza los datos de ejemplo y restablece la base al terminar. La primera fase conserva sus capturas y registro en [VALIDACION.md](VALIDACION.md).

La rama añade únicamente promedio de resolución, SLA y búsqueda avanzada. Se mantiene MVVM + Repository: las Views no acceden al DAO ni al Repository. No se añadieron entidades, columnas, migraciones ni dependencias.
