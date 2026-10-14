# Tres prompts para Gemini en Android Studio

Proyecto: AyudaCampus · Luis Cucho

Úsalos en orden, uno por uno. Antes de empezar, verifica que `main` esté limpio y crea la rama:

```bash
git switch main
git pull --ff-only origin main
git switch -c mejora-ia
```

Después de cada mejora, revisa los archivos, compila y prueba la aplicación. `PROMPTS.md` debe registrar lo que ocurrió realmente: texto enviado, resultado, correcciones y respeto de MVVM. No afirmes que una prueba pasó si no se ejecutó.

## Prompt 1 · Tiempo promedio de resolución

```text
Trabaja sobre mi proyecto Android AyudaCampus, en la rama mejora-ia. Antes de modificar archivos, inspecciona su estructura y firmas existentes. Usa Kotlin, Jetpack Compose, Material 3, Room 2.6.1, corrutinas y MVVM + Repository. No uses XML para pantallas ni cambies las dependencias si no es necesario.

Agrega al panel de Soporte TI el tiempo promedio de resolución de los tickets CERRADOS, calculado según la guía como fechaCierre - fecha. Incluye únicamente registros con fechaCierre no nula y fechaCierre >= fecha. Si no hay tickets cerrados, muestra “Sin tickets cerrados”; no muestres cero como si fuera una medición.

Distribuye el cambio así:
1. TicketDao: una consulta SQL AVG(fechaCierre - fecha), reactiva, que devuelva Flow<Double?> en milisegundos.
2. AyudaRepository: declara primero el nuevo método en la interface.
3. AyudaRepositoryRoom: implementa el método delegando en el DAO.
4. PanelViewModel: convierte el Flow en StateFlow y prepara el texto de horas/minutos, conservando un estado para ausencia de datos.
5. PanelScreen: muestra una tarjeta con el indicador, con el mismo diseño violeta, márgenes, tipografía y colores del tema existente. La pantalla solo consume el ViewModel.

No agregues reglas ni acceso a Room/Repository en los Composables. No cambies la estructura de las entidades ni la versión de Room porque esta mejora solo agrega una consulta. El valor debe actualizarse automáticamente cuando el estudiante cierra un ticket.

Añade pruebas con casos de varios cierres, cero tickets cerrados y registros sin fechaCierre. Ejecuta la compilación y las pruebas disponibles. Explica únicamente los resultados verificados y los pendientes.

Muéstrame los archivos modificados y una explicación breve de cómo respeta MVVM. Añade en PROMPTS.md una entrada con este prompt, lo que generaste, las correcciones necesarias y “¿Respeta MVVM?” con explicación. No inventes correcciones que yo no haya realizado.

Cuando la mejora esté probada, prepara el commit: ia: agrega tiempo promedio de resolucion al panel
```

## Prompt 2 · Contador de SLA por prioridad

```text
Continúa en la rama mejora-ia de mi proyecto AyudaCampus. Inspecciona el código actual y conserva la mejora del promedio de resolución. Implementa un contador de SLA en las tarjetas de tickets y en los detalles de Estudiante y Soporte TI.

El límite se calcula desde ticket.fecha: prioridad ALTA = 4 horas, MEDIA = 24 horas y BAJA = 72 horas. Para estados ABIERTO y EN_PROCESO, muestra “Quedan X h Y min” o “SLA vencido hace X h Y min”. No muestres tiempo restante negativo. Para RESUELTO y CERRADO muestra “Atención resuelta” y “Ticket cerrado”, respectivamente, sin seguir contando el SLA.

El cálculo y la actualización periódica deben vivir en los ViewModels o en una función de dominio reutilizable llamada desde ellos. Usa un flujo con un intervalo de un minuto, ligado a viewModelScope y al ciclo de observación de la pantalla; evita bucles o corrutinas en los Composables y evita un temporizador distinto por cada tarjeta. Al cambiar la prioridad, recalcula el límite con la nueva prioridad y la misma fecha de creación.

Las Views solo reciben el estado o texto preparado y lo dibujan. Si extiendes TicketCard, actualiza todos sus usos sin darle acceso al Repository. Conserva Room y DataStore, el diseño cian/violeta y la arquitectura MVVM + Repository. No agregues columnas ni dependencias para esta mejora. No cambies las reglas de cierre, reapertura o comentarios.

Prueba las tres prioridades, el instante exacto del vencimiento, un ticket vencido, el cambio de prioridad y los estados RESUELTO/CERRADO. Haz el cálculo comprobable con un reloj o un parámetro de tiempo para que las pruebas no dependan de esperar realmente. Ejecuta la compilación y las pruebas disponibles y reporta los resultados reales.

Añade la segunda entrada a PROMPTS.md con el prompt, resultado, correcciones y explicación del respeto de MVVM. Conserva la entrada anterior. Muéstrame los archivos modificados y prepara el commit: ia: agrega contador de SLA por prioridad
```

## Prompt 3 · Búsqueda avanzada en la bandeja

```text
Continúa en la rama mejora-ia de AyudaCampus y conserva las dos mejoras anteriores. Agrega búsqueda avanzada únicamente a la bandeja de Soporte TI.

Soporte debe combinar simultáneamente:
- El filtro de estado existente: Todos, Abiertos, En proceso y Resueltos. Todos también incluye CERRADOS.
- Prioridad: Todas, Alta, Media o Baja.
- Categoría: Todas o una categoría concreta. Incluye categorías inactivas que conservan tickets históricos.
- Texto libre por título, número visible del ticket (por ejemplo #0012 o 12) o nombre del estudiante, sin distinguir mayúsculas y aceptando espacios al inicio/final.

Implementa los estados y setters en BandejaViewModel. Usa combine con el Flow de tickets y los flujos de filtros para exponer una única lista ya filtrada como StateFlow. Conserva el orden SQL del DAO: ALTA primero, luego MEDIA y BAJA, y dentro de cada prioridad la fecha más reciente. Filtrar no debe reordenar los resultados ni crear copias inconsistentes.

Carga las categorías mediante AyudaRepository. Si necesitas agregar un método, decláralo primero en la interface y luego en AyudaRepositoryRoom; usa los métodos existentes cuando sea posible. La View no debe conocer DAO ni Repository.

En BandejaScreen agrega un buscador y selectores Material 3 compactos para prioridad y categoría, manteniendo los chips de estado y el diseño violeta existente. Muestra la cantidad de resultados, el estado vacío “No hay tickets con estos filtros” y una acción “Limpiar filtros” que restablezca todos los filtros. Mantén el acceso a Atender ticket y la navegación actual.

Añade pruebas de filtros combinados, número con ceros, búsqueda por estudiante, categorías inactivas, lista vacía, restablecimiento y conservación del orden por prioridad. Compila y ejecuta las pruebas disponibles; informa qué pasó y qué no se pudo verificar.

Añade la tercera entrada a PROMPTS.md sin perder las anteriores. Después de que yo revise los resultados, ayúdame a redactar la reflexión final de cinco líneas sobre las mejoras, errores y aprendizaje de MVVM/Room. No inventes experiencias, resultados ni correcciones personales.

Muéstrame los archivos modificados y prepara el commit: ia: agrega busqueda avanzada en la bandeja
```

## Cierre de la segunda fase

Cuando las tres mejoras y el registro de prompts estén revisados:

```bash
git add PROMPTS.md
git commit -m "docs: agrega PROMPTS.md"
git push -u origin mejora-ia
```
