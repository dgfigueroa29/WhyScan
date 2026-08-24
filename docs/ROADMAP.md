# Roadmap de migración

Cada fase es entregable e independientemente verificable. Una fase no se cierra hasta que su
criterio de salida se cumple en CI.

---

## Fase 1 — Fundaciones ✅ (entregable actual)

Convertir el repositorio en un proyecto Compose Multiplatform con la arquitectura de motores
completa, aunque todavía sin motores de cámara reales.

- [x] Build KMP/CMP: Kotlin DSL, version catalog, Gradle, Kotlin, AGP (versiones al día en `libs.versions.toml`)
- [x] Targets `android`, `iosArm64/iosSimulatorArm64`, `jvm`, `wasmJs` — sin `iosX64`, el
      simulador de los Mac con Intel: Compose Multiplatform 1.11.1 ya no lo publica
- [x] Estructura de módulos `core/`, `engines/`, `feature/`, `composeApp/`, `androidApp/`
- [x] Modelo de dominio: `Barcode`, `BarcodeFormat` (17 simbologías), `BarcodeValueType`, `Detection`
- [x] Scanner Engine SPI completo: contrato, capacidades, disponibilidad, eventos
- [x] Catálogo de los 8 motores con capacidades declaradas y estado por fase
- [x] Registro, política de selección automática y cadena de fallback
- [x] Motor de entrada manual (100 % `commonMain`) — la app escanea desde el día uno
- [x] Parser semántico de valores (URL, WiFi, vCard, email, teléfono, geo, producto)
- [x] MVI de la feature de escaneo + UI de catálogo y resultados
- [x] Design system propio (tokens, tema claro/oscuro)
- [x] Tests de dominio: selección, fallback, parser, catálogo, comparador y marcador
- [x] Suite de contrato de motores (`BarcodeScannerEngineContractTest`), aplicada también a los
      decoradores y a la cadena completa que llega al ViewModel
- [x] Comparador de motores en paralelo + métricas por motor (objetivo G5)
- [x] `build-logic/` con convention plugins
- [x] SDD, 9 ADRs y catálogo de motores documentados

**Criterio de salida:** la app arranca en Android, Desktop y Web; el catálogo lista los 8 motores
con su estado real; los tests de `:core:domain` y `:core:data` pasan en CI.

> **Este criterio se dio por cumplido sin comprobarlo, y salió caro.** Lo que había en verde era
> *compilación* —`assembleDebug`, `lintDebug`, `assembleRelease` con R8, `desktopJar`,
> `wasmJsBrowserDistribution`— más los tests de dominio. Que la app **arrancase** no lo verificaba
> nada: sin tests instrumentados (D6, sin emulador en CI) nadie ejecuta la `MainActivity`.
>
> **El primer arranque real en un dispositivo fue en agosto de 2026, y la app moría.** Un `Executor`
> registrado en Koin como `ExecutorService` tumbaba el grafo entero al componer la primera pantalla
> — ver D18. El defecto llevaba ahí desde que existen los motores de cámara, con el CI en verde todo
> ese tiempo: compilaba, pasaba lint, pasaba R8 y publicaba un APK que reventaba al abrirse.
>
> Vale la pena quedarse con la forma del fallo y no solo con el fallo: **todo lo que este proyecto
> comprueba son piezas, y nada comprueba el montaje.**
>
> **Eso ya no es del todo cierto, y conviene decir exactamente cuánto.** `AppCompositionTest` compone
> `App()` entera con el grafo real —tema, idioma, `CompositionLocal`, `koinViewModel`, los efectos de
> arranque de cada pantalla— y cambia de destino, en un test JVM normal sobre escritorio. Es el
> montaje, y es lo más cerca que este proyecto ha estado de su propio criterio de salida sin arrancar
> la app.
>
> Lo que ese test **no** dice: que la app arranque en **Android**. `MainActivity`, `enableEdgeToEdge`
> y el préstamo de los `ActivityResultLauncher` son código de plataforma que sigue sin quien lo
> ejecute; el grafo de Android sí está cubierto, por `AndroidKoinGraphTest`. Y no dice que nada se
> **vea** bien: componer no es dibujar. El criterio de la Fase 1 pasa de no tener ninguna red a
> tenerla en su parte común, que es la mayor.

---

## Fase 2 — Android real ✅

- [x] `:engines:gms-code-scanner` — Google Code Scanner, sin permisos
- [x] `:engines:mlkit-camerax` — ML Kit Barcode + CameraX, con linterna, zoom y decodificación de imagen
- [x] `PermissionController` actual de Android + flujo de denegación permanente
- [x] Arranque de Koin por plataforma (`initKoin`) con `Context` en Android
- [x] Preview de Android como capacidad del motor (`CameraPreviewEngine`, ADR-0007)
- [x] Overlay común de detección sobre `cornerPoints` normalizados (`ScanOverlay`)
- [x] Controles de linterna en la UI, derivados de las capacidades declaradas
- [x] Historial persistente con Room KMP (`:core:database` + `:feature:history`)
- [x] Navegación entre escáner e historial, con botón atrás de Android
- [x] CI en GitHub Actions: detekt + tests + Android + Desktop + Web en cada PR, iOS en `main`
- [x] Higiene del repo: `.editorconfig` alineado con detekt, `.idea/` fuera del control de versiones
- [x] Preferencias persistentes con `multiplatform-settings` (D2) y control de zoom en la UI (D8)
- [x] `ScanRequest.timeoutMillis` implementado (`DeadlineScannerEngine`): estaba en el modelo desde la Fase 1 sin que ningún código lo cumpliera
- [x] Decisión sobre los tests instrumentados: **no los va a haber**. Sin emulador en CI, un test
      que exija dispositivo es un test que nunca se ejecuta y que da una falsa sensación de red

**Criterio de salida:** escaneo real en Android alternando dos motores en caliente, con fallback
verificable desactivando Play Services.

> **Compila y pasa CI.** Las APIs de ML Kit y CameraX estuvieron sin compilar hasta que se activó
> Actions, porque el entorno de desarrollo no alcanza `dl.google.com`. Ya no: el job de Android
> ensambla debug, pasa lint y ensambla release con R8.

---

## Fase 3 — iOS ⏸️ despriorizada

> **Sin dispositivos Apple no se puede *probar* nada de esto**, y por eso deja de marcar el ritmo.
> Lo que sí cambió al activar Actions es que **compilarlo ya no exige una Mac propia**: un runner
> macOS enlaza el framework. Eso cubre los errores de compilación de Kotlin/Native —que es donde
> estaba el riesgo grueso, porque ese código no se había compilado jamás— pero no que la cámara
> funcione, que sigue necesitando un iPhone.
>
> **Desde esta revisión, iOS está fuera de la verificación obligatoria.** Vive en su propio
> workflow, `ios.yml`, y **solo se lanza a mano** (Actions → "iOS (manual)" → Run workflow). El
> motivo es que compilar no es probar: mientras el job vivía dentro de `Verify`, una plataforma que
> nadie puede ejecutar dejaba `main` en rojo de forma permanente, y un rojo permanente le quita el
> significado a los checks que sí hablan de algo verificable. `Verify` cubre ahora las tres
> plataformas que este proyecto puede ejecutar; iOS se lanza cuando se vaya a tocar, y de forma
> obligada antes de retomar esta fase.
>
> El coste es real y queda dicho: el Kotlin/Native vuelve a no tener red automática, que es
> exactamente lo que dejó pasar el `Cannot access 'val IO': it is internal` durante una tanda
> entera. Se acepta a cambio de que el verde de `Verify` signifique algo.

- [x] `:engines:vision-ios` — `AVCaptureSession` + `AVCaptureMetadataOutput`, con linterna y zoom
- [x] Preview de iOS (`UIKitView` con `AVCaptureVideoPreviewLayer`) vía `CameraPreviewEngine`
- [x] `IosPermissionController` sobre `AVCaptureDevice`
- [x] `iosApp/` — fuentes Swift e `Info.plist` con `NSCameraUsageDescription`
- [ ] `iosApp.xcodeproj` — se crea en Xcode siguiendo `iosApp/README.md` (requiere macOS)
- [x] Primera compilación de todo el código iOS — **cerrada: enlaza entero**. El stack
      compartido (modelo, dominio, `scanner-api`, `designsystem`, `platform`, `permissions` y el
      motor manual) compiló a la primera; los diez errores estaban todos en los dos motores de
      AVFoundation, y ocho eran la misma confusión repetida: importar como extensión lo que cinterop
      genera como miembro. Arreglados. Los módulos que dependen de esos dos no llegaron a
      compilarse, así que faltan tandas. La tanda siguiente dio **la confusión simétrica**, y por eso
      merece quedar escrita: `Dispatchers.IO` en `:core:database`. Sacarlo de `commonMain` y
      declararlo por plataforma era necesario pero no suficiente — en Kotlin/Native `IO` es una
      *extensión* de `concurrentMain` y el miembro homónimo es `internal`, así que hace falta además
      `import kotlinx.coroutines.IO`. Sin ese import el job de iOS seguía cayendo con
      `Cannot access 'val IO': it is internal`, que es donde estaba `main` hasta esta revisión.
      Con él, el job pasó de morir al minuto y medio a **enlazar el framework completo en 12 min
      48 s**, y no hubo cuarta tanda: `:composeApp` y todas sus dependencias de iOS —que nunca
      habían llegado a compilarse— compilaron sin un solo error. **Todo el código Kotlin de iOS
      compila.** Lo que falta para la fase no es compilar, es un iPhone
- [x] **Motor baseline decidido** (cerraba R9): se consumen los artefactos publicados de zxing-cpp,
      sin cinterop propio — ver [ADR-0008](adr/ADR-0008-baseline-zxing-cpp.md)
- [x] **Kotlin 2.3.20** (cerraba R10): los klibs de zxing-cpp están compilados con 2.2.0 y el
      proyecto estaba en 2.1.21. Se subió a 2.3.20 exacto porque es con la que están compilados CMP
      1.11.1, Koin 4.2.2 y KSP 2.3.10 — emparejar exacto reduce la superficie de fallo. Gradle a
      8.14.5. Room y AGP se quedaron donde estaban por no poder contrastarlos desde aquí; el primer
      CI zanjó la duda y AGP subió a 8.10.0, que es el mínimo que exige KSP 2.3.10 (riesgo R11)
- [x] `:engines:zxing-cpp` — `io.github.zxing-cpp:android` en Android y `:kotlin-native` en iOS.
      Dos adaptadores y ningún `commonMain`: las dos publicaciones no comparten API, solo el núcleo
      C++ — que es lo que hace justa la comparación. En iOS usa `AVCaptureVideoDataOutput` y no la
      salida de metadatos, porque esa ya trae su propio decodificador dentro y el baseline dejaría
      de serlo. Decodifica también imágenes estáticas, lo que da a iOS su primer decodificador de
      archivos
- [x] Revisión de ADR-0005 (cerraba D4): tres destinos y ningún deep link, así que la navegación
      propia se queda. El defecto real era otro y se corrigió: el backstack no sobrevivía a que se
      recreara la Activity. No al rotar —el manifiesto declara `configChanges` para eso— sino a que
      el sistema mate el proceso, o a un cambio de tamaño de letra o de idioma. Ahora se guarda por
      ids estables, escritos a mano porque R8 ofusca los nombres de clase

- [x] CI: `linkDebugFrameworkIosSimulatorArm64` en runner macOS — **disponible a demanda**, en el
      workflow `ios.yml`. Dejó de correr en cada `main`: da su veredicto cuando se le pide, no como
      condición para integrar cambios de las otras tres plataformas

**Criterio de salida:** escaneo real en iOS; ZXing-cpp produce resultados comparables entre
Android e iOS sobre el mismo set de imágenes de referencia.

---

## Fase 4 — Web y OCR

- [x] `:engines:browser-detector` — `BarcodeDetector` con detección de soporte del navegador y de
      contexto seguro, más decodificación de imagen estática vía `createImageBitmap`
- [x] `:engines:mlkit-ocr` — Text Recognition en Android + `OcrCodeInterpreter`: lee el número
      impreso bajo el código y solo lo emite si el dígito de control cuadra
- [x] Preview de Web (D14): el `<video>` vive en el documento, sobre el canvas, y el composable solo
      le dice qué rectángulo ocupar. Tapa el overlay, y eso pasa a ser una capacidad declarada
      (`occludesOverlay`) en vez de un dibujo invisible
- [ ] OCR en iOS: ML Kit se distribuye por CocoaPods, que este proyecto no usa. La alternativa sin
      dependencias es `VNRecognizeTextRequest` del framework Vision, reutilizando `OcrCodeInterpreter`
- [x] Escaneo desde imagen/galería (RF-07) con selector en las cuatro plataformas: *photo picker*
      en Android, `UIImagePickerController` en iOS, `JFileChooser` en escritorio e `<input
      type=file>` en Web. Ninguno pide permisos: los cuatro corren fuera de la app y devuelven solo
      lo elegido
- [x] Suite de contrato contra lo que se puede ejercitar sin dispositivo: los decoradores y la
      cadena completa. Los motores de cámara quedan cubiertos solo por lo declarativo (ver D6)

> El fallback web a ZXing-cpp compilado a Wasm que figuraba aquí se ha retirado: no existe
> publicación wasmJs (ADR-0008). El respaldo del navegador es la entrada manual.

**Criterio de salida:** las cuatro plataformas escanean con al menos dos motores cada una; el OCR
recupera correctamente EAN-13 impresos sobre códigos dañados.

---

## Fase 5 — Producto

- [x] Comparador: ejecutar varios motores sobre la misma petición (`ComparingScannerEngine`)
- [x] Métricas de latencia y acierto por motor (`EngineScoreboard`)
- [x] Pantalla "Comparar" con el marcador en vivo — **cierra G5**
- [x] `ScanRequest.continuous` y `allowMultiple` se respetan de verdad (`RequestLimitsScannerEngine`):
      antes solo los cumplía el motor manual, así que el interruptor de escaneo continuo no tenía
      efecto sobre la cámara y una sesión puntual no terminaba nunca
- [x] Los `ScannerEffect` llegan a la UI vía un `SnackbarHost` único en la raíz: se emitían a un
      flujo que nadie colectaba
- [x] Filtro de formatos en pantalla (RF-06), y `DismissError` / `Refresh` cableados
- [x] Atribución de eventos al motor: `FrameAnalyzed` y `Failed` llevan `engineId`, así que las
      métricas de frames y de fallos por motor dejan de estar siempre en cero
- [x] Acciones sobre el resultado (RF-13): copiar, compartir y abrir, derivadas del **significado**
      del código y no de su formato (`ResultActionsFactory`), ejecutadas por `PlatformActions` en las
      cuatro plataformas. Sin esto, escanear un QR con una URL no llevaba a ningún lado
- [x] Exportación del historial a CSV y JSON (`HistoryExporter` + `FileSaver`). Exporta lo que se
      está viendo, no todo: un archivo que no se parece a la pantalla es una sorpresa. El CSV
      neutraliza los valores que una hoja de cálculo ejecutaría como fórmula — el contenido de un
      código escaneado viene de fuera y no es de fiar
- [x] Play Feature Delivery (RNF-06): **decidido aplazarlo**, no olvidado — ver
      [ADR-0009](adr/ADR-0009-play-feature-delivery-aplazado.md). Un módulo de característica
      dinámica no puede ser un módulo KMP, el mecanismo solo funciona distribuyendo por Play, y no
      hay ninguna medición del APK con la que decidir qué partir. RNF-06 queda **cumplido entre
      plataformas y no cumplido dentro de Android**, dicho en vez de dado por hecho
- [x] Historial de Web persistente (D9) y textos de compartir fuera del dominio (D15)
- [x] `:engines:zxing-java` — decodificador de escritorio (D13). Hasta ahora, elegir un archivo en
      escritorio no llevaba a ninguna parte: el selector existía desde RF-07 y no había quién lo
      leyera. Es además el **primer motor real verificado de extremo a extremo sin dispositivo**: el
      propio ZXing genera los códigos y el test los decodifica de vuelta desde los píxeles
- [x] Accesibilidad (RNF-05): el contraste pasa de intención a **test** — la paleta se extrae a
      `ScannerPalette`, sin Compose, y `ContrastTest` mide WCAG en `commonTest`. De paso apareció que
      los catorce roles `on*` estaban en los morados por defecto de Material. Y semántica donde solo
      había posición: el visor, el estado de sesión como región viva, los botones de acción con el
      valor dentro y el interruptor fusionado con su etiqueta
- [x] Auditoría de privacidad (RNF-03) con dos hallazgos, no solo una lista de garantías: el `fetch`
      del motor de Web resultó ser sobre un data URL local, y aun así se le puso guardia; y la
      ausencia de `INTERNET` en el manifiesto, que es la garantía más fuerte que hay, no estaba
      escrita en ninguna parte
- [ ] Objetivos táctiles medidos sobre un dispositivo: los componentes de Material aplican 48 dp por
      su cuenta y no hay clickables propios, pero comprobarlo exige ejecutar la app

**Criterio de salida:** el usuario puede responder, dentro de la app y con datos, la pregunta
"¿qué motor funciona mejor para este código en este dispositivo?".

---

## Fase 6 — De banco de pruebas a producto de Play 🚧

Hasta aquí el criterio de todas las fases fue *técnico*. Este no: la app tenía que dejar de parecer
lo que es por dentro. Un usuario que abre un lector de códigos no debería encontrarse ocho motores
con sus latencias en la portada, ni una app con nombre de prototipo y sin icono.

### Ronda 1 — marca, sistema de diseño, tema e idiomas ✅

- [x] **Nombre y marca.** `applicationId` a un identificador propio — se cambia ahora porque después
      de la primera publicación en Play ya no se puede. Los paquetes de Kotlin se dejaron como
      estaban a conciencia: doscientos archivos para cambiar algo que nadie ve. **Esa decisión se
      revocó después** — ver la Ronda 5
- [x] **Icono de lanzador, que no existía.** Ni uno: el manifiesto no declaraba `android:icon`, así
      que Android ponía su robot por defecto. Es un bloqueo duro de Play, y de los que no aparecen en
      ningún CI. Adaptativo con capa `monochrome` (iconos temáticos de Android 13+), PNG de respaldo
      para API 24-25 y el 512×512 de la ficha en `playstore/`
- [x] **Los ~30 roles de color de Material 3 declarados.** Estaban los seis de siempre más los
      `on*`; faltaban los `*Container`, que es lo que pinta un `FilterChip` seleccionado, la `Card`,
      el `NavigationBar` y el indicador del ítem activo. Todos ellos salían **morados**, del relleno
      de fábrica de `lightColorScheme()`. Es exactamente el mismo defecto que la Fase 5 arregló para
      los `on*`, en la mitad de los roles que aquella no miró
- [x] **`ContrastTest` pasa de 22 pares a 56**, con un umbral aparte a 3.0:1 para lo que no es texto
      (`outline`). La lista de pares se declara una vez y se aplica a los dos temas, así que la
      simetría entre claro y oscuro deja de depender de mantener dos copias a mano
- [x] **Escala tipográfica y de formas propias.** No había ninguna: `MaterialTheme` usaba las de
      fábrica. El valor de un código leído pasa a monoespaciada — es un dato que alguien coteja
      carácter a carácter contra una etiqueta, y en proporcional `1`, `l` e `I` se confunden
- [x] **Iconos en la barra de navegación.** Estaban en `icon = {}`, literalmente vacíos, y por eso
      no había indicador de ítem activo: Material 3 lo dibuja **alrededor del icono**
- [x] **Selector de tema Sistema / Claro / Oscuro**, persistido. Con él aparece un defecto que antes
      no podía existir: `enableEdgeToEdge()` ata los iconos de las barras del sistema al modo oscuro
      *del sistema*, así que forzar el tema de la app los volvía invisibles. `MainActivity` recibe el
      valor resuelto y reajusta el estilo
- [x] **Inglés y español.** Los cuatro catálogos duplicados en `values/` (inglés) y `values-es/`.
      El inglés va en la carpeta sin calificador porque es el respaldo de todo idioma que no sea
      español: antes, un teléfono en alemán veía castellano. 127 claves con paridad comprobada
- [x] **Selector de idioma propio**, más `localeConfig` para el selector por app de Android 13+.
      En Web no se muestra: `navigator.language` no se puede escribir desde la página, y un control
      inerte es peor que no tenerlo. El mecanismo es el segundo intento: sustituir el entorno de
      recursos con `LocalComposeEnvironment` **no compila** en CMP 1.11.1 —esa interfaz y su
      `CompositionLocal` son `internal`—, así que se cambia el locale de la plataforma y se tira el
      subárbol con `key(tag)`, que es de donde `stringResource` saca el idioma de verdad
- [x] **`:feature:settings`** con su ViewModel y sus tests, y **modo avanzado** como preferencia: el
      catálogo de motores, el comparador, el filtro de formatos y las latencias vuelven con un
      interruptor. `Navigator.pruneTo` saca del backstack lo que deja de estar disponible

### Ronda 2 — la pantalla de escaneo, y dos defectos que salieron debajo ✅

- [x] **Visor a pantalla completa con el resultado en una hoja inferior.** Antes el visor era el
      primer elemento de un `LazyColumn` y se iba de la pantalla en cuanto llegaba el segundo
      resultado, justo cuando el usuario quiere seguir apuntando
- [x] Estados de permiso y de "aquí no hay cámara" con su motivo y su salida, resueltos con un `when`
      sobre cuatro casos excluyentes en vez de condiciones sueltas
- [x] La sesión **arranca sola** al aparecer la pantalla y se apaga al salir. La cámara seguía
      capturando mientras el usuario miraba el historial: el ViewModel sobrevive a la navegación y
      nadie paraba la sesión
- [x] Animación al crecer la hoja, tope de cien resultados vivos, pausa y reanudación sobre el visor
- [x] **D18 saldada para los módulos comunes y el escritorio** (`KoinGraphTest`). El `platformModule`
      de Android lo cubrió la ronda siguiente
- [x] **Lecturas repetidas suprimidas.** Una cámara a 30 fps emitía el mismo código noventa veces en
      tres segundos, y cada repetición **se guardaba en el historial persistente**: no era ruido
      visual, era corrupción de los datos del usuario. Lo arregla `DistinctDetectionsScannerEngine`
- [x] **La base de datos nunca recibía su driver.** `DatabaseBuilderFactory` declaraba una extensión
      `build()` sobre `RoomDatabase.Builder`, y en Kotlin **un miembro siempre gana a una
      extensión**: los tres `platformModule` llamaban al `build()` de Room y la configuración del
      driver bundled no se ejecutaba nunca. Escritorio e iOS reventaban al abrir la primera pantalla;
      Android funcionaba cayendo al SQLite del framework, que es justo el driver que ese archivo
      existe para no usar. **El compilador lo avisaba en cada build** —`This extension is shadowed by
      a member`— y nadie leía el aviso. Lo encontró `KoinGraphTest` en su primera ejecución
- [x] **Un test que falla en CI ahora dice por qué.** Con la salida por defecto de Gradle el fallo
      anterior aparecía como `IllegalArgumentException at KoinGraphTest.kt:189`, sin mensaje ni
      causa; encontrar el defecto de Room exigió configurar `testLogging` primero

### Ronda 3 — el grafo de Android, el movimiento y el arranque ✅

- [x] **`androidUnitTest` en `:composeApp`, y con él se cierra D18 entera.** `AndroidKoinGraphTest`
      arranca el grafo real de Android sobre Robolectric y resuelve cada tipo que la raíz de la app
      pide, incluido el `Executor` que fue el defecto original. Robolectric no contradice la decisión
      de D6 —"no habrá tests instrumentados"—, la esquiva: el grafo de Android no necesita un
      dispositivo, necesita un `Context`. Los motores de cámara guardan la referencia y dejan el
      `LifecycleCameraController` y los clientes de ML Kit detrás de un `by lazy`, así que resolverlos
      no arranca ninguna cámara. `sdk = 34` fijo: Robolectric exige JDK 21 para simular la 36 y el CI
      corre sobre 17
- [x] **Animaciones de transición entre destinos.** `AnimatedContent` con el *fade through* de
      Material 3 en lugar del corte seco que había. No desliza de lado a propósito: la barra inferior
      no es una pila, y un deslizamiento contaría una jerarquía que no existe
- [x] **D20 saldada, y sin dispositivo.** Se quitó el `KoinContext { }` de `App.kt`. Lo que impedía
      cerrarla era no poder comprobar dónde resuelven `koinInject` y `koinViewModel` sin instalar la
      app, y resulta que sí se puede: `koinInject` no es UI, es una función `@Composable` que lee un
      `CompositionLocal` y llama a `remember`, y eso lo resuelve el **runtime** de Compose, que es
      Kotlin puro. `ComposeKoinContextTest` monta una `Composition` con un `Applier` que no aplica
      nada —no hay árbol que construir, interesa el efecto de componer— y comprueba que `koinInject`
      devuelve **la misma instancia** que `koin.get()`. Ni Skiko, ni ventana, ni emulador
- [x] **`AppCompositionTest`: la app se monta, y ahora hay quien lo comprueba.** No estaba en la lista
      de esta ronda —salió de tirar del hilo que abrió D20— y es lo que más lejos llega de todo lo que
      hay. Los otros tests dicen que el grafo resuelve; este dice que `App()` **compone**, con el tema,
      el idioma, los `CompositionLocal`, los `koinViewModel`, los efectos de arranque de cada pantalla
      y el cambio de destino. Es la franja donde vivieron los dos defectos caros del proyecto
- [ ] Objetivos táctiles y `enableEdgeToEdge` **mirados con los ojos** en un dispositivo (queda
      pendiente desde la Fase 5)

### Ronda 4 — arranque 🔜

- [x] **Baseline profile** (ver [ADR-0012](adr/ADR-0012-baseline-profile.md)). La app arranca
      Compose, monta el grafo de Koin con cinco motores y abre Room: todo eso lo **interpreta** ART la
      primera vez, y ese primer arranque es el que Play mide y el que decide si alguien deja la app
      instalada. El módulo `:baselineprofile` graba dos recorridos —arranque y navegación por las tres
      pantallas— sobre un emulador declarado en la build, y `androidx.profileinstaller` instala el
      perfil también en Android 7-11, donde el sistema no lo hace solo: con `minSdk` 24 eso es la
      mitad del rango, y la mitad más lenta
- [x] **El cableado está y `Verify` lo comprueba**: `assembleDebug`, `lintDebug` y `assembleRelease`
      con R8 pasan con el plugin aplicado, y ninguno arranca un emulador — la build de release toma el
      perfil del repositorio cuando lo haya y sigue adelante cuando no. La **grabación** vive en
      `baseline-profile.yml` y se lanza a mano, igual que iOS y por el mismo motivo: no es un criterio
      para aceptar un cambio, es un artefacto
- [ ] **Lanzar el workflow y commitear el perfil generado.** Es lo único de esta ronda que sigue sin
      hacerse, y conviene decir por qué con precisión: el entorno de desarrollo **no alcanza el maven
      de Google**, así que aquí no se puede ni resolver el plugin, y mucho menos arrancar un emulador.
      Un clic en Actions → "Baseline profile (manual)" lo produce. Hasta entonces el perfil no existe
      y la app arranca como arrancaba: el cableado no miente sobre eso

### Ronda 5 — un solo nombre ✅

El proyecto llegó a tener **tres nombres a la vez** y nadie lo había mirado de frente: uno en el
repositorio, otro en `app_name` —lo único que el usuario lee— y un tercero en los paquetes de Kotlin,
en `rootProject.name`, en los convention plugins, en el archivo de la base de datos, en la clave de
preferencias y en el directorio de escritorio.

La divergencia estaba razonada en la Ronda 1, y el razonamiento no era malo: renombrar doscientos
archivos para cambiar algo que ningún usuario ve es mal negocio. Lo que faltaba en esa cuenta es que
**uno de los tres nombres no es reversible**. El `applicationId` queda congelado en la primera
publicación —es la URL de la ficha y la clave con la que el sistema reconoce una actualización—, así
que unificar costaba lo mismo antes que después salvo por ese detalle. Se hizo antes.

- [x] **`com.whyscan` en todo el código**: paquetes, directorios (movidos con `git mv`, para no perder
      el historial), `namespace` de los quince módulos y el paquete de la clase de recursos de Compose
- [x] **`applicationId`, `app_name`, `rootProject.name`**, el `packageName` de la distribución de
      escritorio, el título de la ventana, el `<title>` de la web y el `Info.plist` de iOS
- [x] **Los símbolos de marca**: el tema de Compose, la marca vectorial, el estilo `Theme.*` de
      Android y la clase `Application`
- [x] **Los convention plugins**, renombrados los tres archivos y sus catorce usos
- [x] **Los textos que el usuario lee**: el aviso de privacidad de Ajustes y los dos mensajes de la
      pantalla de escaneo, en los dos idiomas
- [x] **Los identificadores de datos**: archivo de la base de datos, clave de `SharedPreferences`,
      nodo de preferencias de escritorio y directorio `~/.…`

> **Lo que esto rompe, dicho antes de que sorprenda.** Cambiar el nombre del archivo de la base de
> datos, la clave de preferencias y el directorio de escritorio significa que **una instalación
> anterior no encuentra sus datos**: historial y ajustes arrancan vacíos. No se escribió ninguna
> migración a propósito — la app nunca se ha distribuido, así que los únicos afectados son los
> dispositivos de desarrollo, y una migración para eso sería código muerto desde el día uno.

### Ronda 6 — una marca que distingue en vez de agrupar ✅

Ver [ADR-0013](adr/ADR-0013-la-marca-sale-del-objeto.md). El sistema de diseño estaba bien
construido; lo que no se había examinado era sobre qué se construyó. El símbolo eran **cuatro
esquinas de encuadre y una línea de lectura** —el icono `QrCodeScanner` que Material ya trae y que
usan otras doscientas apps de la tienda— y el color era el azul por defecto de Tailwind. En una ficha
de Play, junto a sus competidores, eso no distingue: **agrupa**.

- [x] **La marca es el módulo fugado**: el patrón de localización de un QR —los cuadrados anidados de
      sus esquinas— con el anillo abierto y el módulo central ya fuera. Es el átomo más reconocible de
      un código, casi nadie lo usa, y rompe a propósito una regla real: un patrón de localización es
      siempre concéntrico y siempre cerrado
- [x] **La forma, en sus tres copias**: el `ImageVector` de Compose, el primer plano del icono
      adaptativo y la capa monocroma
- [x] **Los PNG regenerados**: los cinco pares de `mipmap-*` (el icono real en API 24-25, que no
      entienden iconos adaptativos) y el 512×512 de la ficha. Si no, esas tres superficies se habrían
      quedado con la marca vieja sin que nada avisara
- [x] **Paleta nueva: grafito cálido y un único acento esmeralda.** Los 56 pares del `ContrastTest`
      medidos antes de escribir una sola constante — cero fallos
- [x] **El color de arranque**, en Android (`values/` y `values-night/`) y en Web (`theme-color` y el
      fondo del `body`): tiene que coincidir con el `background` del tema o se ve un destello de otro
      color entre que el sistema pinta la ventana y Compose pinta la primera pantalla

> **Lo que se descartó, que es parte de la decisión.** Una marca hecha con la inicial del nombre —la
> salida cómoda: una letra no dice nada del producto y la copia cualquiera— y un signo de
> interrogación jugando con el nombre, que compite con «ayuda» en una cuadrícula de aplicaciones.
> Cerradas esas dos, la única dirección honesta era mirar el objeto que la app lee.

### Antes de la ficha de Play, esto va primero

Lo de abajo es trámite de tienda. Lo de esta lista no. Se hizo el repaso a propósito antes de tocar
nada de Play, y salió algo que no estaba en ninguna lista:

- [x] **`allowBackup` estaba en `true`, y la app le dice al usuario lo contrario.** Ajustes afirma que
      "WhyScan no pide permiso de internet, así que lo que escaneás no puede salir del dispositivo".
      Con Auto Backup encendido eso era **falso**: el historial de Room vive en `databases/` y las
      preferencias en `shared_prefs/`, dos de los directorios que el sistema copia a Google Drive por
      defecto. Y lo hace **el sistema**, desde fuera del proceso, sin necesitar el permiso `INTERNET`
      que la app no declara — que era justamente la garantía en la que se apoyaba la auditoría de
      privacidad (RNF-03). Es el mismo error de forma que D18 y que el driver de Room: **la garantía
      se comprobó en el sitio equivocado**, mirando solo lo que hace el código propio. Corregido con
      `allowBackup="false"` **y** `dataExtractionRules`, que hacen falta los dos: desde Android 12 el
      atributo solo apaga la copia en la nube y la transferencia entre dispositivos se gobierna desde
      el XML. Coste aceptado y dicho: el historial no sobrevive a un cambio de teléfono
- [ ] **Instalar y abrir la app en un dispositivo real con estos cambios.** No es una formalidad: el
      único arranque real que ha tenido este proyecto destapó un defecto que ningún CI podía ver (D18),
      y la revisión siguiente destapó otro de meses en la persistencia. Hay cosas nuevas sin mirar con
      los ojos — la pantalla sin `KoinContext` y la transición entre destinos
- [ ] **Objetivos táctiles y `enableEdgeToEdge`** (RNF-05, pendiente desde la Fase 5). Es la clase de
      cosa que no rompe, se ve mal, y se ve mal precisamente en la primera pantalla
- [ ] **Generar el baseline profile** lanzando `baseline-profile.yml`. Un clic y quince minutos de
      runner, y es lo que separa "la app arranca" de "la app arranca rápido la primera vez"

Solo después tiene sentido pelearse con la ficha.

### Pendiente para publicar

- [ ] Comprobar en Play Console que `com.whyscan.app` está libre y que "WhyScan" no colisiona con una
      ficha existente. **Sin red en el entorno de desarrollo, esto no se pudo verificar aquí**
- [ ] Capturas, gráfico de cabecera 1024×500 y textos de la ficha, en los dos idiomas
- [ ] Política de privacidad publicada y formulario de seguridad de datos. Es el trámite más corto
      de todos: sin `INTERNET`, la respuesta a casi todo es "no se recoge nada"
- [ ] Firma de release y `bundle` en vez de APK

**Criterio de salida:** alguien que no sabe qué es un motor de escaneo abre la app, lee un código y
lo comparte, sin ver ni una vez la palabra "motor".

---

## Qué cubre a los motores de cámara sin emulador

La decisión de no tener tests instrumentados deja un hueco real y conviene decir exactamente cuál es
y qué lo compensa:

| Se comprueba sin dispositivo | Sigue sin comprobarse |
|---|---|
| Que el descriptor de los ocho motores es coherente: IDs únicos, fases válidas, sin prometer control de cámara con UI propia (`ScannerEngineCatalogTest`) | Que el motor **lea** un código real |
| Que la selección, el fallback, los límites de petición y el plazo se comportan según el contrato, incluida la cadena completa que llega al ViewModel | Que la cámara arranque, y que se libere al cancelar |
| Que lo declarado tenga quien lo cumpla, en todo lo instanciable sin `Context` | Lo mismo en los motores de Android e iOS, que necesitan `Context` o `AVCaptureSession` |
| Que ZXing (Java) **lea de verdad** un QR y un EAN-13 desde píxeles, filtre por formato y distinga "no hay código" de "no es una imagen" | Lo mismo en los motores que necesitan cámara |
| Que el **grafo de Koin resuelva** de verdad, en las cuatro plataformas menos iOS y Web: `KoinGraphTest` arranca los módulos comunes más el `platformModule` de escritorio, y `AndroidKoinGraphTest` hace lo mismo con el de **Android** sobre Robolectric | Lo mismo para iOS y Web. iOS necesita ejecutar tests de Kotlin/Native en un runner macOS; Web, el target wasmJs |
| Que `koinInject` resuelva **componiendo de verdad**, sin envoltorio y sin UI: `ComposeKoinContextTest` monta una `Composition` con el runtime de Compose y compara la instancia con la del grafo | Que la pantalla se **vea** bien: el runtime compone, no dibuja |
| Que **`App()` se monte entera**: `AppCompositionTest` compone la raíz con el grafo real —tema, idioma, `CompositionLocal`, `koinViewModel`, los efectos de arranque de cada pantalla— y cambia de destino. Es lo más cerca que este proyecto ha estado de "la app arranca" sin arrancarla | Que se **vea** bien, y que el arranque de **Android** funcione: `MainActivity`, `enableEdgeToEdge` y el préstamo de los `ActivityResultLauncher` siguen sin quien los ejecute |
| Que el proyecto **compile** para Android, Escritorio y Web, incluida la build de release con R8 | Que la app **arranque** y lea un código: sigue haciendo falta un dispositivo |

El riesgo que queda es el de siempre en este tipo de app: el código de cámara solo se prueba
usándola. Lo que sí evita el diseño es que un fallo ahí se lleve por delante al resto — el SPI
mantiene la lógica de selección, degradación y presentación fuera de los motores, y esa parte sí
está cubierta.

---

## D19, primera pieza: los accesores `compose.*` de los scripts de build

Un fallo de CI dejó ver lo que desde el entorno de desarrollo no se puede leer, porque no alcanza el
maven de Google y ninguna tarea de Gradle se puede lanzar. Vale la pena escribirlo mientras está a
mano, que es justo lo que D19 reprocha no haber hecho antes.

Cada `implementation(compose.algo)` de los scripts de build emite:

```
w: composeApp/build.gradle.kts:57:36: 'runtime: String' is deprecated. Specify dependency directly
```

Y así con `foundation`, `animation`, `material3`, `components` y `resources` en `:composeApp`, más
`runtime` en `:androidApp`. Los demás módulos no aparecían en ese log porque la build se cayó antes
de configurarlos, así que la lista completa es más larga: el convention plugin
`whyscan.kmp.compose` los usa también.

Tres cosas que conviene tener claras antes de arreglarlo:

- **No son errores, aunque Gradle los cuente como tales.** El compilador los emite como `w:`; cuando
  *además* hay un error de verdad en el mismo script, el informe de "Script compilation errors" los
  lista todos juntos y suma. Eso es lo que hizo que un fallo por otra cosa pareciera ocho.
- **El arreglo lo dice el propio aviso**: "Specify dependency directly", es decir, coordenadas
  explícitas en vez del accesor del plugin. No es una línea: hay que decidir de dónde sale la versión
  para no dispersar por los módulos lo que hoy centraliza el plugin de Compose.
- **Es una familia, no el total.** D19 sigue abierta: falta el resto del build y falta la postura
  —limpiar y activar `allWarningsAsErrors`, o aceptar el ruido por escrito—.

---

## Deuda técnica aceptada en la Fase 1

Registrada de forma explícita para que no se olvide:

| # | Deuda | Se salda en |
|---|---|---|
| ~~D1~~ | ~~Sin convention plugins: cada módulo repite su configuración KMP~~ | **Saldada**: `build-logic/` con `whyscan.kmp.library`, `.kmp.compose` y `.android.application` |
| ~~D2~~ | ~~Preferencias en memoria, no persistidas~~ | **Saldada**: `multiplatform-settings` en las cuatro plataformas |
| ~~D3~~ | ~~Historial en memoria~~ | **Saldada**: Room KMP en Android, iOS y Desktop. En Web sigue en memoria porque Room no tiene target wasmJs |
| ~~D4~~ | ~~Navegación propia sin deep links ni restauración de estado~~ | **Saldada**: hecha la revisión de ADR-0005, el umbral (seis destinos o deep links) no se alcanza y la navegación propia se mantiene. Lo que sí era un defecto era la restauración: al recrearse la Activity —muerte del proceso, cambio de idioma o de tamaño de letra; no al rotar, que el manifiesto ya cubre— se volvía al escáner. `Navigator` guarda y restaura el backstack por ids estables y `MainActivity` lo pasa por `onSaveInstanceState` |
| ~~D5~~ | ~~Strings hardcodeados en la UI~~ | **Saldada**: `composeResources` por módulo. Los ViewModels emiten mensajes semánticos (`ScannerMessage`, `HistoryMessage`) y `ResultAction` dejó de traer etiqueta: el dominio dice qué acción, la UI cómo se llama |
| ~~D6~~ | ~~La suite de contrato no se ejecuta contra motores de cámara reales~~ | **Cerrada como no-objetivo**: no habrá emulador en CI, así que ningún test puede exigir dispositivo. Lo cubre lo que sí corre sin él — ver más abajo |
| ~~D8~~ | ~~El zoom se declara como capacidad pero no hay control en la UI~~ | **Saldada**: slider derivado de `canControlZoom` |
| ~~D10~~ | ~~RF-07 sin UI ni selector de archivos~~ | **Saldada**: `ImagePicker` en las cuatro plataformas y `DecodeImageUseCase` recorriendo la cadena de motores. Un motor bloqueado por el permiso de cámara sigue sirviendo para leer un archivo |
| ~~D12~~ | ~~RF-13 (copiar, compartir, abrir enlace) sin implementar~~ | **Saldada**: `PlatformActions` en `:core:platform` con implementación en las cuatro plataformas. En escritorio no hay hoja de compartir y el botón no se ofrece (`canShare`) |
| ~~D11~~ | ~~La comparación necesita dos motores de cámara y solo Android los tenía~~ | **Saldada**: con ZXing-cpp, iOS tiene dos (Vision y ZXing-cpp) y Android cuatro |
| ~~D9~~ | ~~El historial de Web es de sesión~~ | **Saldada**, aunque no con IndexedDB: se guarda como JSON en el almacén de la plataforma, con los mismos campos que la tabla de Room. Unas cientos de filas de texto no justifican una base de datos, y esto corre en `commonTest` mientras que IndexedDB serían cien líneas de interop que nadie puede probar |
| ~~D7~~ | ~~`:androidApp` sin ProGuard/R8 configurado para release~~ | **Saldada**: `minify` y `shrinkResources` activados, con reglas cortas y justificadas, y `assembleRelease` en CI para que R8 se ejecute de verdad |
| ~~D14~~ | ~~El motor de Web escanea pero no muestra visor~~ | **Saldada**: el `<video>` se coloca sobre el canvas desde `onGloballyPositioned`. A cambio tapa el overlay, declarado con `occludesOverlay` |
| ~~D15~~ | ~~El texto que se copia de un WiFi lo compone el dominio~~ | **Saldada**: `shareableContent()` devuelve la estructura y la pantalla la redacta con sus recursos. La acción del ViewModel lleva el texto ya hecho |
| ~~D13~~ | ~~Desktop y Web se quedan sin decodificador: zxing-cpp no publica artefacto JVM ni wasmJs~~ | **Saldada en Desktop**: `:engines:zxing-java` sobre `com.google.zxing:core`, en el catálogo **como motor propio** y no con el nombre de zxing-cpp — son proyectos distintos y confundirlos falsearía la comparación. Solo imagen estática: el decodificador está, la captura de webcam no. **Web se queda como está**: no hay artefacto wasmJs y su respaldo sigue siendo la entrada manual |
| ~~D16~~ | ~~`ScannerViewModel` tiene doce colaboradores y veinte funciones~~ | **Saldada**: seis dependencias. Los ajustes en `ScanSettings`, la sesión y el guardado en `ScanSessions`, las acciones sobre el resultado en `ResultActionRunner`. Los tres casos de uso de preferencias y el del catálogo se **borraron** en vez de envolverse —delegaban al repositorio sin añadir nada—, y la única regla que había se conservó donde se puede probar. Quince tests nuevos que antes exigían levantar el ViewModel entero. Quedan dos supresiones, ninguna global: `TooManyFunctions` en la clase (catorce acciones de usuario, catorce funciones) y `CyclomaticComplexMethod` en `onAction`, que es una tabla de despacho sobre un `sealed interface` |
| D17 | `IosPlatformActions.openUrl` usa `UIApplication.openURL:`, que Apple depreció en iOS 10 a favor de `openURL:options:completionHandler:`. Compila —solo es un aviso— pero es API vieja en código nuevo. Salió de auditar el header real al preparar la compilación de iOS, no de un fallo | Cuando iOS enlace en verde y se pueda comprobar el cambio en el runner |
| ~~D18~~ | ~~**Nada comprueba que el grafo de Koin resuelva.** Saldada a medias: falta el `platformModule` de Android.~~ **Saldada entera.** El defecto que la abrió lo demostró el primer arranque real en un dispositivo: `platformModule` registraba el executor de análisis como `ExecutorService` mientras los tres motores de cámara lo piden como `Executor`, y Koin resuelve por igualdad exacta de tipo. La app moría al componer la primera pantalla con `NoDefinitionFoundException`. **El compilador no puede verlo** —los `get()` son genéricos que se resuelven en ejecución— y el CI tampoco: compila, pasa lint, pasa R8 y publica un APK que revienta al abrirse. Es el mismo agujero que el criterio de salida de la Fase 1, visto desde el otro lado. `KoinGraphTest` (`composeApp/src/desktopTest`) arranca el grafo real y **resuelve** cada tipo que la raíz de la app consume, agrupado por el ViewModel que lo pide. No usa `verify()` sino resolución de verdad, que es más fuerte: instancia en vez de reflexionar. Cubre `dataModule`, `domainModule` y los tres módulos de feature —comunes a las cuatro plataformas— más el `platformModule` de escritorio. En su primera ejecución destapó el defecto del driver de Room que no se aplicaba (SDD §11). La mitad que faltaba —el `platformModule` de **Android**, que es justo donde estaba el defecto original— la cubre ahora `AndroidKoinGraphTest` en `composeApp/src/androidUnitTest`, sobre Robolectric, con su tarea en el job de Android del CI | **Cerrada.** Lo que queda fuera es iOS y Web, y no por el mismo motivo: ahí falta ejecutar tests de esos targets, no falta el test |
| D19 | **Los avisos del compilador no los lee nadie, y uno de ellos era un defecto de producción.** `This extension is shadowed by a member` llevaba apareciendo en cada build desde que existe `:core:database`, y señalaba el defecto del driver de Room que reventaba escritorio e iOS (SDD §11): un aviso correcto, visible en cada compilación y leído por nadie durante meses. Hoy el build emite además avisos de deprecación mezclados con ruido de terceros, así que ninguno destaca. **Uno menos:** el `KoinContext is not needed anymore` se fue al cerrar D20, y se fue por el camino largo —comprobando qué pasaba al quitarlo— que es el que esta deuda pide para el resto. **Y ya hay inventario de una familia entera**, leída de un log real de CI (ver abajo) | Decidiendo una postura: o se limpian todos y se activa `allWarningsAsErrors`, o se acepta el ruido explícitamente. Lo primero exige antes saber cuáles vienen de plugins y no se pueden arreglar, y eso exige leer un build completo — que desde el entorno de desarrollo no se puede lanzar, porque no alcanza el maven de Google. Lo que sí se puede es leer el log de CI cuando algo falla, que es como salió el inventario de abajo |
| ~~D20~~ | ~~**El aviso `KoinContext is not needed anymore` en `App.kt`.** Quitarlo cambia por dónde resuelven `koinInject` y `koinViewModel`, y eso no se puede comprobar sin ejecutar la app.~~ **Saldada, y la premisa era falsa.** Sí se puede comprobar sin ejecutar la app, porque `koinInject` no es UI: es una función `@Composable` que lee un `CompositionLocal` y llama a `remember`, y eso lo resuelve el **runtime** de Compose, que es Kotlin puro y no sabe nada de pantallas. `ComposeKoinContextTest` monta una `Composition` con un `Applier` que no aplica nada —no hay árbol de nodos que construir, lo que interesa ocurre *durante* la composición— y comprueba que `koinInject` devuelve la misma instancia que `koin.get()`, para un tipo de los módulos comunes y otro que depende del `platformModule`. Leyendo koin-compose se ve por qué: `LocalKoinScopeContext` declara como valor por defecto `KoinPlatform.getKoin().scopeRegistry.rootScope`, que es exactamente lo que `KoinContext` proveía a mano — el envoltorio era una identidad. Pero eso es leer la librería, y leyendo la librería también estaba bien el `build()` de Room que no se llamaba nunca | **Cerrada.** Con ella se va uno de los avisos que D19 señala como ruido |
| D22 | **Una migración de esquema borra el historial del usuario.** `buildBundled()` cierra con `fallbackToDestructiveMigration(dropAllTables = true)`, que hoy no hace daño —la base va por la versión 1 y nunca ha cambiado— pero es una bomba de relojería en cuanto la app esté instalada en teléfonos ajenos: la primera vez que se añada una columna, Room tirará la tabla en vez de migrarla y el usuario perderá lo que tenía sin enterarse. No se cambia ahora porque quitarlo hoy sustituye una pérdida silenciosa por un cierre inesperado, y no hay ninguna migración que escribir todavía. Lo que sí está: `room { schemaDirectory(...) }` exporta el esquema, que es el requisito para poder escribirla | En el mismo cambio que suba `@Database(version = 2)`: escribir la `Migration` y quitar el fallback. Antes no hay nada que hacer; después es tarde |
| D21 | **El selector de idioma en iOS está sin verificar.** El actual escribe `AppleLanguages` en `NSUserDefaults`, que es el mecanismo estándar; si Compose lee `preferredLanguages` el cambio es inmediato, si lee `currentLocale` no lo será hasta reabrir. Ver ADR-0011 | Cuando haya un iPhone. Es lo primero que hay que mirar de la UI de iOS |
