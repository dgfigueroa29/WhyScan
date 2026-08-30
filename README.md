# WhyScan

Lector de **códigos de barras y QR** en Compose Multiplatform, sin cuenta, sin rastreo y sin red.

Debajo hay un **banco de pruebas de motores de escaneo**: la app no lee un código de una sola
manera, sino que elige entre varias alternativas, las compara y degrada con elegancia cuando una no
está disponible — sobre Android, iOS, Desktop y Web con un único código base.

Las dos cosas conviven porque son la misma app en dos modos. Por defecto WhyScan es un lector: se
apunta y se lee. El **modo avanzado** (Ajustes → Avanzado) devuelve el catálogo de los nueve motores,
el comparador en paralelo y las latencias por lectura.

> **Un solo nombre.** WhyScan es el nombre del producto, el del proyecto Gradle, el de los paquetes
> de Kotlin (`com.whyscan.*`), el del `applicationId` de Play (`com.whyscan.app`) y el de los
> plugins de convención. Se escribe siempre como una sola palabra —`WhyScan`, `whyScan`,
> `whyscan`—, nunca separado.

---

## Estado actual — arranca en Android; compila en las cuatro plataformas

|                                                  |                                                                                                                                                                                                                                                                                                                                               |
|--------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Arquitectura y SPI de motores                    | ✅ completos                                                                                                                                                                                                                                                                                                                                   |
| Catálogo de los 9 motores con capacidades        | ✅ declarado                                                                                                                                                                                                                                                                                                                                   |
| Selección automática + cadena de fallback        | ✅ implementados y testeados                                                                                                                                                                                                                                                                                                                   |
| Suite de contrato que todo motor debe pasar      | ✅ implementada, y aplicada a los decoradores y a la cadena completa                                                                                                                                                                                                                                                                           |
| Comparador de motores con marcador en vivo (G5)  | ✅ implementado y en la UI                                                                                                                                                                                                                                                                                                                     |
| Motor de entrada manual                          | ✅ funcional en las 4 plataformas                                                                                                                                                                                                                                                                                                              |
| Google Code Scanner y ML Kit + CameraX (Android) | ✅ implementados y compilando                                                                                                                                                                                                                                                                                                                  |
| Historial persistente                            | ✅ Room en Android, iOS y Desktop; en Web, JSON en el almacén del navegador. Con **migración de verdad**: hasta esta versión, el primer cambio de esquema lo habría borrado entero                                                                                                                                                             |
| Preferencias persistentes                        | ✅ las cuatro plataformas                                                                                                                                                                                                                                                                                                                      |
| CI en GitHub Actions                             | ✅ **en verde**: detekt, tests, Android (con R8), Desktop y Web                                                                                                                                                                                                                                                                                |
| Vision / AVFoundation (iOS)                      | ✅ implementado; **todo el Kotlin de iOS enlaza**, a demanda en el workflow `iOS (manual)`. Falta el dispositivo, no la compilación                                                                                                                                                                                                            |
| Arranque en un dispositivo real                  | ✅ **primera vez en agosto de 2026**, y encontró un defecto de DI que el CI no podía ver (D18)                                                                                                                                                                                                                                                 |
| `targetSdk` 36 (requisito de Play)               | ✅ con el atrás adaptado al *predictive back*                                                                                                                                                                                                                                                                                                  |
| BarcodeDetector del navegador (Web)              | ✅ implementado, con visor sobre el canvas                                                                                                                                                                                                                                                                                                     |
| OCR con ML Kit Text Recognition (Android)        | ✅ implementado; en iOS irá con Vision, no con ML Kit                                                                                                                                                                                                                                                                                          |
| Escaneo desde imagen (RF-07)                     | ✅ selector en las cuatro plataformas, sin pedir permisos                                                                                                                                                                                                                                                                                      |
| ZXing-cpp (Android + iOS)                        | ✅ implementado — el mismo decodificador C++ en ambas, que es lo que hace comparables las lecturas                                                                                                                                                                                                                                             |
| Acciones sobre el resultado (RF-13)              | ✅ copiar, compartir y abrir, según el significado del código                                                                                                                                                                                                                                                                                  |
| Navegación                                       | ✅ propia, con backstack que sobrevive a que el sistema mate el proceso                                                                                                                                                                                                                                                                        |
| Build de release con R8                          | ✅ `minify` y `shrinkResources`, con `assembleRelease` en CI                                                                                                                                                                                                                                                                                   |
| Tamaño del binario                               | ✅ medido en cada PR y repartido por cubos —código, nativas por ABI, assets—, con la tercera razón de [ADR-0009](docs/adr/ADR-0009-play-feature-delivery-aplazado.md) por fin instrumentada. Falta grabar la primera línea base, que solo produce CI |
| Marca, icono y tema                              | ✅ **El módulo fugado**: el patrón de localización de un QR con el anillo abierto y su módulo central ya fuera ([ADR-0014](docs/adr/ADR-0014-la-marca-sale-del-objeto.md)). Grafito cálido con un único acento esmeralda, los ~30 roles de Material 3 declarados, icono adaptativo con capa monocroma y escala tipográfica y de formas propias |
| Selector de tema claro/oscuro                    | ✅ Sistema / Claro / Oscuro, persistido, con las barras del sistema siguiendo al tema **de la app**                                                                                                                                                                                                                                            |
| Idiomas inglés y español                         | ✅ los cuatro catálogos en `values/` (inglés, respaldo de cualquier idioma) y `values-es/`, con selector propio ([ADR-0011](docs/adr/ADR-0011-idioma-de-la-app-por-encima-del-sistema.md)) y `localeConfig` para el selector por app de Android 13+                                                                                            |
| Pantalla de escaneo                              | ✅ cámara a pantalla completa con el resultado en una hoja que la empuja, no que la tapa; la sesión arranca sola y se apaga al salir ([ADR-0010](docs/adr/ADR-0010-dos-disposiciones-de-la-pantalla-de-escaneo.md))                                                                                                                            |
| Lecturas repetidas                               | ✅ suprimidas en el dominio con ventana de dos segundos. Antes, tres segundos apuntando a un QR escribían noventa filas en el historial                                                                                                                                                                                                        |
| Notas en el historial                            | ✅ texto de referencia por lectura, escribible desde el historial **y** desde el escáner, con buscador que mira valor **y** nota ([ADR-0012](docs/adr/ADR-0012-la-nota-es-del-historial-no-de-la-deteccion.md)). La poda no se lleva lo anotado                                                                                                |
| Historial agrupado por día                       | ✅ cabeceras pegajosas con "Hoy" y "Ayer", que es lo que una persona reconoce sin leer                                                                                                                                                                                                                                                         |
| Borrado del historial                            | ✅ una lectura suelta **con deshacer**, o todo con confirmación que dice cuántas se pierden                                                                                                                                                                                                                                                    |
| Exportación del historial                        | ✅ CSV, JSON y texto plano, guardado en las cuatro plataformas                                                                                                                                                                                                                                                                                 |
| Migraciones de la base                           | ✅ `@AutoMigration`, y un test que abre una base v1 con datos y comprueba que siguen ahí                                                                                                                                                                                                                                                       |
| Que el grafo de Koin resuelva                    | ✅ los módulos comunes, escritorio **y Android** (este con Robolectric, en la JVM y sin emulador). D18 cerrada                                                                                                                                                                                                                                 |
| Que la app **se monte**                          | ✅ `AppCompositionTest` compone `App()` entera con el grafo real y cambia de destino, sin emulador ni ventana                                                                                                                                                                                                                                  |
| Transiciones y movimiento                        | ✅ *fade through* entre destinos, salida animada de la pantalla de arranque, entrada de la pantalla completa y llegada de una lectura nueva. Lo que **no** se anima está decidido y escrito (§9.12 del SDD) |
| Pantalla de arranque                             | ✅ la marca del lanzador con `core-splashscreen`, sujeta hasta que la primera composición resuelve el tema — cierra el destello claro de quien tiene el tema oscuro forzado |
| Probar un motor sin salir del catálogo           | ✅ "Probar ahora" junto a "Elegir": elige el motor, reinicia la sesión con él y abre el visor a pantalla completa ([ADR-0015](docs/adr/ADR-0015-probar-un-motor-es-un-dialogo.md)) |
| Acciones que caben en cualquier pantalla         | ✅ copiar, compartir, anotar y borrar son iconos; abrir conserva la palabra, porque son cinco acciones distintas que ningún icono separa. La descripción hablada sigue llevando el valor dentro |
| Política de privacidad y términos de uso         | ✅ escritos, comprobables contra el código y enlazados desde Ajustes → Acerca de, en los dos idiomas (`docs/legal/`) |
| Baseline profile                                 | ✅ grabado y versionado ([ADR-0013](docs/adr/ADR-0013-baseline-profile.md)); se regenera desde el workflow `Baseline profile (manual)`. Lo que sigue sin saberse es **cuánto** mejora el arranque: eso exige un dispositivo |
| Qué hay de nuevo                                 | ✅ una vez tras cada actualización, y siempre accesible desde Ajustes. A quien acaba de instalar no se le estrena nada                                                                                                                                                                                                                         |
| Accesibilidad (RNF-05)                           | ✅ contraste AA **verificado por test** (56 pares, los dos temas), semántica para lectores de pantalla y **modo dislexia** que ajusta la escala tipográfica entera                                                                                                                                                                             |
| Privacidad (RNF-03)                              | ✅ auditada: sin trazas, sin cliente HTTP, sin analítica, sin permiso `INTERNET` y **sin copia de seguridad del sistema** — esa última era la puerta que no pasaba por la app, y la vigila un chequeo en CI                                                                                                                                    |
| ZXing en Java (Desktop)                          | ✅ el único decodificador de escritorio; **verificado de verdad**, decodificando imágenes generadas en el test                                                                                                                                                                                                                                 |

El catálogo muestra las nueve alternativas con su estado real; los motores aún no implementados se
declaran como tales, con la fase en la que llegan. Ver `docs/ROADMAP.md`.

Lo que queda fuera por ahora, y por qué:

- **iOS está despriorizado**, no abandonado. Probarlo exige un dispositivo, que no lo hay; lo que sí
  se puede es **compilarlo**, y para eso está el workflow `iOS (manual)` — Actions → Run workflow.
  Está fuera de `Verify` a propósito: compilar no es probar, y un check que nadie puede satisfacer
  con una prueba real solo servía para dejar `main` en rojo de forma permanente. Ese trabajo **ya
  está terminado**: los errores estuvieron concentrados en los dos motores de AVFoundation y en el
  `import kotlinx.coroutines.IO` que en Kotlin/Native no viaja con el receptor, y con eso el
  framework entero enlaza. Falta el `iosApp.xcodeproj`, que solo se crea desde Xcode, y un iPhone.
- **No hay tests instrumentados y no los va a haber.** Sin emulador en CI, un test que exija
  dispositivo nunca se ejecuta y da una falsa sensación de red. El ROADMAP dice exactamente qué
  queda
  cubierto sin dispositivo y qué no. La regla, dicha con precisión, es **que todo lo que se
  comprueba
  se pueda ejecutar en cada PR**: lo que la incumple es el hardware, no el nombre de la plataforma —
  por eso el grafo de Android sí tiene test, con Robolectric, en la misma JVM que el resto.
  La única excepción es `:baselineprofile`, que sí arranca un emulador — y no contradice la regla
  porque **no es un test, es una grabación**: no afirma nada, no puede fallar por lo que la app haga
  y su resultado es un archivo. Por eso vive en un workflow manual y no en `Verify`.
- **Escritorio lee archivos pero no cámara**: hay decodificador (ZXing en Java) y no hay captura de
  webcam, así que una sesión en vivo cae a la entrada manual.
- **El APK de Android carga con los cuatro motores de la plataforma.** RNF-06 se cumple entre
  plataformas y no dentro de Android; Play Feature Delivery se aplazó a conciencia y con condición
  de entrada ([ADR-0009](docs/adr/ADR-0009-play-feature-delivery-aplazado.md)).
- **Web no tiene respaldo tras el navegador**: zxing-cpp no publica artefacto wasmJs, así que quien
  cierra esa cadena es la entrada manual.

> **Verificado en CI.** El proyecto compila entero: Android (debug, lint y release con R8),
> Escritorio y Web, más detekt y los tests en cada PR. iOS se enlaza a demanda, en el workflow
> `iOS (manual)`.
>
> Lo que el CI **no** comprueba es que la app arranque: sin tests instrumentados nadie ejecuta la
> `MainActivity`, así que un fallo de arranque no lo detecta ningún check. No es teórico — el primer
> arranque en un dispositivo real murió por un `Executor` registrado en Koin con el tipo equivocado,
> con el CI en verde todo el tiempo. Está contado en `docs/adr/ADR-0003`.
>
> **Parte de ese hueco ya está tapado, y hay que decir cuál.** `KoinGraphTest` arranca el grafo real
> y resuelve cada tipo que la raíz de la app consume; corre en un test JVM normal, sin emulador. En
> su primera ejecución encontró un defecto que llevaba meses en producción y que ningún check veía:
>
> - **La base de datos nunca recibía su driver.** `:core:database` declaraba una *extensión*
    > `build()` sobre `RoomDatabase.Builder` para configurar el driver bundled, y en Kotlin **un
    > miembro siempre gana a una extensión**: los tres `platformModule` llamaban al `build()` de
    Room y
    > esa configuración no se ejecutó nunca. Escritorio e iOS reventaban al abrir la primera
    pantalla;
    > Android funcionaba cayendo al SQLite del framework — justo el driver que ese código existe
    para
    > evitar, así que la garantía de "la misma versión de SQLite en las cuatro plataformas" llevaba
    > siendo falsa desde que se escribió.
> - **El compilador lo avisaba en cada build** (`This extension is shadowed by a member`) y nadie
    > leía el aviso. Queda registrado como deuda D19: o se limpian todos los avisos, o se acepta el
    > ruido explícitamente.
> - Encontrarlo exigió antes arreglar otra cosa: **un test que fallaba en CI no decía por qué**. La
    > salida por defecto de Gradle daba el tipo de excepción y la línea, sin mensaje ni causa. El
    > `build.gradle.kts` raíz configura ahora `testLogging` con `exceptionFormat = FULL`.
>
> **Ese hueco ya está cerrado del todo.** `AndroidKoinGraphTest` monta el `platformModule` de
> Android —el más grande de los cuatro y el único donde ocurrió el crash— con un `Context` real que
> da Robolectric en la JVM. Lo que sigue sin cubrir es que la app **se abra y lea un código**, que
> necesita un dispositivo y siempre lo va a necesitar.
>
> En el mismo pase se cerró **D20**, y merece la pena por cómo: lo que la mantenía abierta era creer
> que quitar el `KoinContext { }` de `App.kt` no se podía comprobar sin instalar la app. Sí se
> puede.
> `koinInject` no es UI —lee un `CompositionLocal` y llama a `remember`—, así que basta el **runtime
**
> de Compose, que es Kotlin puro: `ComposeKoinContextTest` monta una `Composition` con un `Applier`
> que no aplica nada y comprueba que sale la misma instancia que del grafo.
>
> Hasta que se activó Actions nada de esto se había compilado nunca —el entorno de desarrollo no
> alcanza el maven de Google—, y el primer CI encontró **doce fallos encadenados**, desde el
> `build-logic` que no resolvía sus plugins hasta un `ScanError` construido sin argumentos en el
> motor de Web. Están todos arreglados y cada uno explicado en su commit.

---

## Documentación

| Documento                            | Contenido                                                                      |
|--------------------------------------|--------------------------------------------------------------------------------|
| [`docs/SDD.md`](docs/SDD.md)         | Documento de diseño: requisitos, arquitectura, SPI, calidad, plan de migración |
| [`docs/ENGINES.md`](docs/ENGINES.md) | Catálogo de motores: formatos, capacidades y prioridad por plataforma          |
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | Fases, criterios de salida y deuda técnica aceptada                            |
| [`docs/adr/`](docs/adr/)             | Decisiones de arquitectura con su contexto y sus consecuencias                 |
| [`docs/legal/`](docs/legal/)         | Política de privacidad y términos de uso, en español e inglés                  |

Lectura mínima para tocar código: **§7 del SDD** (el Scanner Engine SPI) y **ADR-0002**.

**Para retomar el proyecto**, lo primero es
[«Por dónde seguir»](docs/ROADMAP.md#por-dónde-seguir), al principio del ROADMAP: dice qué queda
abierto, qué lo bloquea y dónde vive el detalle de cada cosa. Está separado en tres grupos porque el
bloqueo no es el mismo — lo que se puede hacer ya, lo que espera un número que solo produce CI, y lo
que necesita un teléfono.

---

## Estructura

```
core/model          modelo puro: Barcode, BarcodeFormat, Detection, ScanRequest
core/scanner-api    el SPI + el catálogo declarativo de motores
core/scanner-ui     capacidad de UI del motor: CameraPreviewEngine
core/scanner-testing suite de contrato que todo motor hereda
core/domain         casos de uso, políticas de selección y decoradores del SPI
core/data           registro de motores, preferencias e historial
core/designsystem   tema, paleta, tipografía, formas, marca y cambio de idioma en caliente
core/permissions    abstracción de permisos por plataforma
core/platform       acciones del sistema: copiar, compartir, abrir, elegir imagen, guardar archivo
core/database       Room KMP: historial persistente (sin target wasmJs)
engines/*           un módulo por alternativa de escaneo
feature/scanner     MVI, pantalla de escaneo y comparador de motores
feature/history     historial filtrable por motor
feature/settings    tema, idioma y modo avanzado
composeApp          raíz Compose Multiplatform y composition root de la DI
androidApp          shell de Android
baselineprofile     graba el baseline profile de Android; no entra en ningún binario
iosApp              shell de iOS (Xcode)
playstore/          material de la ficha de Play (icono 512×512)
```

La regla de dependencias es estricta: un módulo `engines/*` depende solo de `:core:scanner-api`
y de su SDK nativo. Nunca de `:feature:*`, ni de `:core:data`, ni de otro motor.

---

## Marca, tema e idiomas

**El tema.** `WhyScanTheme` declara los ~30 roles de color de Material 3, y no solo los seis
habituales. No es exhaustividad por gusto: `lightColorScheme()` rellena con su paleta de fábrica
todo
lo que no se le pase, así que un `FilterChip` seleccionado o el indicador del ítem activo de la
barra
salían **morados** en una app cuya marca es verde. `ContrastTest` mide 50 pares de color a 4.5:1 y 6
más a 3.0:1, sobre los dos esquemas, con aritmética de WCAG en `commonTest`: sin dispositivo y sin
renderizar nada.

**El selector claro/oscuro** vive en Ajustes y persiste. En Android hay una segunda mitad que no
pinta Compose: los iconos de las barras del sistema. Con `enableEdgeToEdge()` a secas siguen al modo
oscuro *del sistema*, y en cuanto el usuario elige un tema distinto dejan de coincidir — teléfono en
claro y app en oscuro daba iconos oscuros sobre fondo oscuro. `MainActivity` recibe el valor ya
resuelto y reajusta el estilo de las barras.

**Los idiomas.** Los cuatro catálogos de textos viven en `values/` (inglés) y `values-es/`. El
inglés está en la carpeta **sin calificador** a propósito: es el respaldo de cualquier idioma que no
sea español, así que un teléfono en alemán ve inglés y no castellano. El selector propio va por
encima del idioma del sistema cambiando el locale de la plataforma y tirando el subárbol de
Compose con `key(tag)`, y `androidApp` declara `localeConfig` para que WhyScan aparezca además en el
selector de idioma por app de Android 13+.

Ese mecanismo es el segundo intento. El primero sustituía el entorno de recursos con
`LocalComposeEnvironment`, que es lo que documentan varios ejemplos y **no compila con Compose
Multiplatform 1.11.1**: esa interfaz y su `CompositionLocal` son `internal` a la librería. Lo dice
`AppLanguage.kt` con el error exacto al lado, para que nadie lo vuelva a intentar.

En Web el selector **no se muestra**: el idioma sale de `navigator.language`, que una página no
puede escribir. Preferimos no ofrecer el control a ofrecerlo roto —
`PlatformSupportsLanguageOverride` es lo que lo decide, y es `false` solo ahí.

**El icono** se dibuja dos veces, y las dos copias lo dicen: `WhyScanMark` como `ImageVector` para
la
UI y `ic_launcher_foreground.xml` para el lanzador, con las mismas coordenadas escaladas. Lleva capa
`monochrome`, así que se tiñe con los iconos temáticos de Android 13+, y hay PNG de respaldo para
API 24 y 25, que no entienden iconos adaptativos. Antes de esto **no había icono en absoluto**: el
manifiesto no declaraba `android:icon` y Android ponía su robot por defecto.

El razonamiento completo del idioma está en
[ADR-0011](docs/adr/ADR-0011-idioma-de-la-app-por-encima-del-sistema.md).

---

## La pantalla de escaneo

Hay **dos disposiciones**, no una con condicionales, y el motivo está en
[ADR-0010](docs/adr/ADR-0010-dos-disposiciones-de-la-pantalla-de-escaneo.md): leer un código y
comparar motores son preguntas distintas y quieren jerarquías distintas.

En el modo por defecto el visor ocupa todo el alto y los resultados llegan en una hoja que **lo
empuja hacia arriba en lugar de taparlo** — por eso no es un `ModalBottomSheet`: quien escanea en
serie mira el resultado y apunta al siguiente sin tocar la pantalla. Antes el visor era el primer
elemento de un `LazyColumn` y se iba de la pantalla en cuanto llegaba el segundo resultado.

**La sesión arranca sola** al aparecer la pantalla y se apaga al salir. Lo segundo no es una
optimización: el ViewModel sobrevive a la navegación, así que la cámara seguía capturando mientras
el
usuario miraba el historial. El arranque automático **no** dispara la petición de permiso — pedirlo
sin que el usuario haya tocado nada es la forma más rápida de que lo deniegue para siempre; en su
lugar la pantalla explica para qué se usa la cámara y ofrece el botón.

**Las lecturas repetidas se suprimen en el dominio**, no en la UI, y eso importa: a treinta frames
por segundo, tres segundos apuntando a un QR emitían noventa lecturas idénticas que se escribían
**una a una en el historial persistente**. No era ruido visual sino corrupción de los datos del
usuario. La regla es una ventana de dos segundos y no "una vez por sesión", porque volver a leer el
mismo código es un caso de uso real — contar unidades iguales en un inventario.

**Cerrar la cámara la hacía aparecer otra vez**, y el defecto llevaba ahí desde la Fase 2. El Google
Code Scanner encabeza la cadena en Android y abre **su propia pantalla** a pantalla completa; al
cerrarla con el botón atrás emitía `Cancelled`, que es un error fatal, y la cadena de fallback hacía
con él lo que hace con cualquier fallo fatal: pasar al motor siguiente, que vuelve a abrir la
cámara. El fallo de fondo era conceptual — `isFatal` contestaba *"¿puede seguir esta sesión?"* y se
le estaba pidiendo además *"¿merece la pena probar otro motor?"*. Ahora son dos preguntas: cancelar
no es una avería, es el usuario diciendo que no quiere seguir (§7.5 del SDD).

**Se puede probar un motor sin salir del catálogo.** "Elegir" guardaba una preferencia y devolvía al
usuario a la misma lista de fichas, donde a la vista no cambiaba nada; la pregunta que uno se hace
delante de ese catálogo es *qué tal lee **este***, y esa solo la contesta la cámara abierta.
"Probar ahora" elige el motor, reinicia la sesión con él y abre el visor a pantalla completa —un
`Dialog` y no un destino, porque la pantalla vive dentro del `Scaffold` y no puede quitarse el
recorte que ese `Scaffold` le impone
([ADR-0015](docs/adr/ADR-0015-probar-un-motor-es-un-dialogo.md)).

**Pausado es un estado con nombre.** Lo era en la píldora de estado y no en el visor: al pausar
desaparece la superficie de preview, y el `when` que decide qué ocupa ese hueco no tenía un caso
para
eso, así que caía en la rama final y dejaba **un spinner girando indefinidamente**. La pantalla
llegaba a contradecirse — "Pausado" escrito encima de una señal de que algo está cargando. Ahora el
spinner solo sale mientras la cámara se abre de verdad.

---

## El historial

Cada lectura admite **una nota**: un texto de referencia que escribe el usuario. Sin ella,
`7501234567893` es exacto y completamente inútil dentro de una lista de doscientas filas cuando lo
que uno recuerda es "el del pedido de marzo". El buscador mira el valor **y** la nota, que es media
razón de que la nota exista.

Se escribe desde las dos pantallas, y no es duplicación: **el momento en que uno sabe para qué es un
código es justo cuando lo acaba de leer**, así que la lectura recién hecha tiene su "Agregar nota" a
mano en el escáner. Lo que no hace el escáner es guardarlas — las lee del historial y las escribe
allí. Recordarlas en la pantalla habría sido más corto y habría abierto un agujero: el id de una
detección es determinista, así que releer un código ya anotado devuelve la misma fila, el campo se
habría abierto vacío y guardar habría borrado lo que hubiera.

La nota vive en un tipo aparte (`HistoryEntry`) y **no** dentro de `Detection`
([ADR-0012](docs/adr/ADR-0012-la-nota-es-del-historial-no-de-la-deteccion.md)): `Detection` la
producen los motores y la atraviesan seis decoradores, el comparador y el marcador, así que un campo
que escribe una persona más tarde no pinta nada ahí.

Añadirla obligó a mirar cómo se comportaba la base de datos ante un cambio de esquema, y ahí había
**tres defectos que nunca se habían disparado** porque nunca había habido una versión 2:

- **La primera migración habría borrado el historial de todo el mundo.** La base se construía con
  `fallbackToDestructiveMigration(dropAllTables = true)`. En una app sin cuenta, sin nube y sin
  papelera, ese historial es el único sitio donde esos datos existen. Ahora sube con
  `@AutoMigration`
  y lo destructivo queda solo para las bajadas de versión, donde no hay alternativa.
- **Reinsertar una lectura borraba su nota.** El id de una detección es determinista, y el `upsert`
  usaba `REPLACE`, que en SQLite es un borrado más un alta. Pasa a `INSERT OR IGNORE`.
- **La poda borraba por antigüedad sin mirar si la fila estaba anotada.** Una nota es la señal más
  clara de que esa lectura le importa a alguien; el techo existe para acotar lo que genera una
  sesión
  continua, no para borrar lo que alguien escribió a mano.

Y uno más, que apareció después al tirar del mismo hilo: **el id resumía el valor con
`rawValue.hashCode()`**, treinta y dos bits. Dos valores distintos que colisionen y se lean en el
mismo milisegundo dan el mismo id, y con `INSERT OR IGNORE` el segundo se descarta en silencio. La
probabilidad siempre fue ínfima; lo que cambió es la consecuencia, porque de ese id cuelga ahora la
nota. Pasa a FNV-1a de 64 bits, escrito a mano en diez líneas.

También se puede **borrar una lectura suelta** —antes era todo o nada—. Ese borrado no pregunta y
por
eso **se puede deshacer**: son las dos caras de la misma decisión, porque un diálogo por cada fila
convierte limpiar veinte lecturas en veinte interrupciones. Vaciar el historial entero sí pregunta,
y
dice cuántas lecturas se pierden, porque ahí no hay nada que devolver.

El buscador **ignora los acentos**: en español media gente escribe "factura" buscando lo que guardó
como "Factúra", y desde un teclado sin tildes no hay otra opción. La eñe no se pliega — es una letra
distinta, y que "ano" encontrara "año" sería desconcertante.

Y hay un tercer formato de exportación, **texto plano**, una lectura por línea sin cabecera ni
comillas. CSV y JSON son para herramientas; lo que la gente hace con treinta códigos es pegarlos en
un correo. Ese formato es el único que **no** neutraliza fórmulas, a propósito: no lo abre una hoja
de cálculo, y una comilla delante rompería justo lo que existe para dar.

---

## Cómo construir

```bash
python3 tools/checks.py                              # comprobaciones sin compilador (segundos)
./gradlew :androidApp:assembleDebug                  # Android
./gradlew :composeApp:run                            # Desktop
./gradlew :composeApp:wasmJsBrowserDevelopmentRun    # Web
./gradlew detekt                                     # análisis estático
./gradlew jvmTest desktopTest                        # tests multiplataforma
./gradlew :composeApp:testDebugUnitTest              # grafo de Koin de Android (Robolectric)
./gradlew check                                      # tests + detekt
```

iOS se construye desde `iosApp/` en Xcode (requiere macOS).

**`tools/checks.py` tarda segundos y no necesita Gradle ni red.** Reproduce lo que detekt exige de
longitud de línea y orden de imports —para poder verlo sin arrancar nada— y además comprueba lo que
**no comprueba nadie más**: que los catálogos de recursos estén parejos entre inglés y español, que
cada `Res.string.X` tenga su `import`, que no queden claves huérfanas y que el `package` de cada
`.kt` siga a su carpeta. Un catálogo desparejado no rompe la compilación: rompe la pantalla de quien
tenga el idioma que falta.

Lo ejecuta también CI, como primer paso y antes incluso de instalar Java. Eso no es solo rapidez: es
lo que impide que estas comprobaciones se desincronicen en silencio de lo que detekt exige de
verdad.

**`tools/binary_size.py` mide el APK y avisa cuando engorda.** RNF-06 dice que el binario no debe
crecer por motores que el usuario no usa y ADR-0009 aplaza la solución diciendo, entre otras cosas,
que **no hay medición con la que decidir qué partir**. Ya la hay: reparte el zip en cubos —código,
nativas **por ABI**, assets, recursos—, compara con la línea base versionada y falla si crece más de
un 2 % o si desaparece una ABI. No es el tamaño de descarga de Play, y el script lo dice: es una
medida estable con la misma metodología, que es lo que hace falta para detectar un salto.

**`tools/check_resolved_versions.py` cruza lo declarado con lo resuelto.** Declarar una versión no
la
impone: si otro punto del grafo pide una superior, Gradle resuelve la mayor para todo el classpath y
lo escrito en `libs.versions.toml` pasa a ser una sugerencia. Eso costó una tanda entera de CI
—`kotlinx-datetime` fijado en 0.6.2, resuelto en 0.7+, donde el tipo que se usaba sobrevive solo
como
typealias: **compilaba y reventaba al ejecutar**—. El chequeo falla cuando lo sustituido es una
versión nuestra e informa de los ascensos entre terceros, que son funcionamiento normal.

**Baseline profile.** Se graba aparte, porque necesita un emulador:

```bash
./gradlew :androidApp:generateBaselineProfile        # arranca el emulador declarado y graba
```

Tarda unos quince minutos y deja el perfil en `androidApp/src/release/generated/baselineProfiles/`,
que se versiona. En CI hay un botón para lo mismo: Actions → "Baseline profile (manual)". No corre
en
cada PR a propósito — ver [ADR-0013](docs/adr/ADR-0013-baseline-profile.md).

---

## Añadir un motor de escaneo

El coste es constante: **un módulo y una entrada en el catálogo**. Ni la UI ni el dominio cambian.
Los ocho pasos están en [`docs/ENGINES.md`](docs/ENGINES.md#cómo-añadir-un-motor).

---

## Cómo se trabaja aquí, y cómo se trabaja con agentes

Casi todo este repositorio lo ha escrito un agente de IA dirigido por el dueño del proyecto. No es
una nota al pie: es la razón de que el repositorio tenga la forma que tiene, y está documentado
entero — incluido lo que el agente **no** pudo hacer.

| Necesitas | Está en |
|---|---|
| Empezar, con el repositorio recién clonado | [`docs/guides/primeros-pasos.md`](docs/guides/primeros-pasos.md) · [English](docs/guides/getting-started.md) |
| El contrato que sigue un agente, completo | [`AGENTS.md`](AGENTS.md) — normativo, en inglés. Su espejo en castellano es [`CLAUDE.md`](CLAUDE.md) |
| Comandos, subagentes, skills y hooks | [`.claude/README.md`](.claude/README.md) |
| Proponer un cambio de comportamiento antes de escribirlo | [`openspec/README.md`](openspec/README.md) |
| Cómo se usa la IA aquí, de punta a punta | [`docs/ai/README.md`](docs/ai/README.md) |
| Las decisiones tomadas, con su coste | [`docs/adr/README.md`](docs/adr/README.md) |
| Contribuir | [`CONTRIBUTING.es.md`](CONTRIBUTING.es.md) · [English](CONTRIBUTING.md) |

La idea que sostiene el resto está en una línea: **un agente produce buen software cuando el entorno
hace barato ver el trabajo malo.** Aquí no compila nada en local, así que cada red de seguridad hubo
que construirla a mano — `python3 tools/checks.py` en segundos, tests que comparan la documentación
contra el código, y una regla escrita de que decir que algo se probó cuando no se pudo probar es el
peor fallo posible. Eso último también vale para las personas.

Los idiomas están repartidos por función: el código y `docs/` en castellano, las superficies que
leen los agentes en inglés, y las guías para personas en los dos ([ADR-0016](docs/adr/ADR-0016-agents-md-como-contrato-canonico.md)).

---

## Licencia

[Apache-2.0](LICENSE).

Se eligió sobre MIT por una diferencia concreta y no por costumbre: **Apache-2.0 incluye una
concesión expresa de patentes** de quien contribuye a quien usa, y la retira automáticamente a quien
demande por patentes. En un lector de códigos de barras —un terreno con patentes vivas sobre
simbologías y sobre técnicas de decodificación— esa cláusula es exactamente la que hace falta. MIT
no dice nada sobre patentes, y el silencio en un tema así no es neutral.

Es además la licencia del ecosistema en el que vive el proyecto: Kotlin, Compose Multiplatform,
AndroidX, ZXing y ML Kit son todas Apache-2.0 o compatibles.

**Antes de esto no había archivo de licencia**, y sin él el código estaba, por defecto, con todos
los derechos reservados — mientras los términos de uso decían que era público. Una de las dos cosas
tenía que ceder, y no iba a ser la del documento que lee el usuario.
