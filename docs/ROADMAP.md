# Roadmap de migración

Cada fase es entregable e independientemente verificable. Una fase no se cierra hasta que su
criterio de salida se cumple en CI.

> ## El foco es Android. iOS no se toca hasta nuevo aviso
>
> Esto **manda sobre todo lo que hay más abajo**, incluidas las casillas sin marcar. Un pendiente de
> iOS —de la Fase 3 o de la parte de iOS de cualquier otra fase— no es trabajo disponible: es
> trabajo aplazado. No se retoma por iniciativa propia, ni aunque sea lo que mejor se pueda hacer
> sin dispositivo; **poder hacerlo no lo convierte en prioridad**. Lo desbloquea el dueño del
> proyecto diciéndolo, no el roadmap.
>
> El motivo lleva escrito desde la Fase 3 y sigue valiendo: sin dispositivos Apple no se puede
> *probar* nada de esa plataforma, y enlazar el framework no acerca la app a la tienda. Android es
> lo que se publica.
>
> **Se registró después de saltárselo.** El OCR de iOS de la Fase 4 se implementó en agosto de 2026
> con este documento ya diciendo que iOS estaba despriorizado; el trabajo quedó hecho y en verde,
> pero la elección fue equivocada. Queda aquí y en `CLAUDE.md` para que el criterio no dependa de
> volver a leer la Fase 3.

---

## Por dónde seguir

**Esto es un índice, no una lista aparte.** Cada fila remite a la ronda donde vive el detalle y el
porqué; si algo cambia, se cambia **allí** y aquí solo el enlace. Una segunda copia de lo pendiente
se desincroniza sola — es exactamente lo que acababa de pasar con dos casillas que seguían abiertas
teniendo el trabajo hecho: el cableado de accesibilidad de la Ronda 10 y el `KoinContext` de D20.

### Se puede hacer ahora, sin nada que esperar

| Qué                                                                 | Dónde                        | Nota                                                                       |
|---------------------------------------------------------------------|------------------------------|------------------------------------------------------------------------------|
| **Tests de captura** (Roborazzi u otro)                             | propuestas de la Ronda 16    | Lo más caro y lo que más cierra: "que se vea bien" lleva desde la Fase 1 escrito como incubrible, y **no lo es** — Robolectric ya se usa aquí. Antes de comprometerse, un *spike* de una pantalla: los tests de captura son célebres por volverse ruido |
| **Métricas del compilador de Compose**                              | `openspec/changes/compose-compiler-reports/` | Escrito como propuesta y **no empujado a ciegas**: toca `build-logic`, y si el accesor `composeCompiler` no se genera no cae solo Android, caen los cuatro jobs |
| **`IosPlatformActions.openUrl` sin la guarda de esquemas**          | [Ronda 9](#ronda-9--seguridad-y-privacidad-) | Una línea. Cae dentro de "lo mínimo para que iOS siga compilando", no de trabajar en iOS |
| **Revisión automática en cada PR**                                  | [`docs/ai/state-of-the-art.md`](ai/state-of-the-art.md) §2 | Hoy el harness solo sirve a quien ejecuta un agente en local. Es lo primero que lo haría útil para otra persona |

### Bloqueado por una decisión que no es técnica

No lo bloquea el hardware ni un número: lo bloquea alguien decidiendo. Están juntos porque
arrastrarlos dentro de otras listas los hace parecer trabajo que nadie hace.

| Qué | Dónde | Quién decide |
|---|---|---|
| **Grupo Maven y paquete de `:core:foundation`** — no puede llevar `whyscan` | [ADR-0018](adr/ADR-0018-federar-la-base-y-no-la-marca.md) y los *Blockers* de `openspec/changes/federate-design-system/proposal.md` | El dueño del proyecto. Toda la federación está detrás de esto, y poner un nombre provisional para renombrar después es justo la parte cara |
| **~~Correo de contacto de la ficha de Play~~** | resuelto el 30-08-2026 | <david@faro.net.ar>, ya escrito en los cuatro documentos legales, `SECURITY.md`, el código de conducta y las plantillas de issue |
| **~~El `applicationId`~~** | resuelto el 30-08-2026 | `ar.net.faro.whyscan` ([ADR-0019](adr/ADR-0019-el-applicationid-identifica-a-quien-publica.md)). Los paquetes de Kotlin **no** se tocan. Falta comprobar en Play Console que está libre, que sin red no se pudo hacer aquí |
| **El nombre visible en la ficha** — ¿`WhyScan` o `Faro WhyScan`? | textos de la ficha | El dueño del proyecto. A diferencia del `applicationId`, esta **sí** se puede cambiar después de publicar |
| **Cerrar la cadena con la entrada manual (G4)** | `openspec/changes/close-the-chain-with-manual-entry/` | El dueño del proyecto: arreglar el código o retirar la garantía de los cinco documentos que la prometen. La propuesta recomienda lo primero |

### Bloqueado por un número que solo produce CI

| Qué                                       | Dónde                                              | Qué lo desbloquea                                                    |
|-------------------------------------------|-----------------------------------------------------|-----------------------------------------------------------------------|
| **Suelo de cobertura de las features**    | [Ronda 13](#ronda-13--verificación-), paso dos      | Leer la tabla de cobertura de un run y decidir con ella, incluido si excluir los `@Composable` |
| **Primera línea base del tamaño del binario** | [Ronda 19](#ronda-19--el-tamaño-del-binario-deja-de-ser-una-opinión-) | Bajar el artefacto `tamano-binario` de un run y commitear su JSON en `tools/binary-size.json` |

Las dos tienen la misma forma y el mismo motivo: **aquí no compila nada**, así que la primera
medición la produce CI y no una consola. Adelantarse a ellas es inventarse el dato.

### Necesita un teléfono Android

| Qué                                                    | Dónde                                                                 |
|--------------------------------------------------------|------------------------------------------------------------------------|
| Ver con los ojos lo de las Rondas 15 a 19               | pantalla de arranque, pantalla completa, animaciones, arreglo de cancelar |
| Medir el arranque contra un punto de partida            | [Ronda 14](#ronda-14--rendimiento-sin-baseline-no-hay-ronda-)          |
| Objetivos táctiles y `enableEdgeToEdge` (RNF-05)        | pendiente desde la Fase 5                                              |
| RNF-01 (<500 ms de detección) y RNF-02 (<1 s de cámara) | nunca medidos                                                          |

### Fuera de alcance por decisión, no por olvido

- **iOS**, entero: lo manda `CLAUDE.md` y la Fase 3. Incluye D21, el `.xcodeproj` y que el OCR
  **lea**.
- **Play**: ficha, firma, `bundle`, formulario de seguridad de datos, y ADR-0009 (Play Feature
  Delivery), que además exige distribuir por Play para ejecutarse.

---

## Fase 1 — Fundaciones ✅ (entregable actual)

Convertir el repositorio en un proyecto Compose Multiplatform con la arquitectura de motores
completa, aunque todavía sin motores de cámara reales.

- [x] Build KMP/CMP: Kotlin DSL, version catalog, Gradle, Kotlin, AGP (versiones al día en
  `libs.versions.toml`)
- [x] Targets `android`, `iosArm64/iosSimulatorArm64`, `jvm`, `wasmJs` — sin `iosX64`, el
  simulador de los Mac con Intel: Compose Multiplatform 1.11.1 ya no lo publica
- [x] Estructura de módulos `core/`, `engines/`, `feature/`, `composeApp/`, `androidApp/`
- [x] Modelo de dominio: `Barcode`, `BarcodeFormat` (17 simbologías), `BarcodeValueType`,
  `Detection`
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
> **Eso ya no es del todo cierto, y conviene decir exactamente cuánto.** `AppCompositionTest`
> compone
> `App()` entera con el grafo real —tema, idioma, `CompositionLocal`, `koinViewModel`, los efectos
> de
> arranque de cada pantalla— y cambia de destino, en un test JVM normal sobre escritorio. Es el
> montaje, y es lo más cerca que este proyecto ha estado de su propio criterio de salida sin
> arrancar
> la app.
>
> Lo que ese test **no** dice: que la app arranque en **Android**. `MainActivity`,
`enableEdgeToEdge`
> y el préstamo de los `ActivityResultLauncher` son código de plataforma que sigue sin quien lo
> ejecute; el grafo de Android sí está cubierto, por `AndroidKoinGraphTest`. Y no dice que nada se
> **vea** bien: componer no es dibujar. El criterio de la Fase 1 pasa de no tener ninguna red a
> tenerla en su parte común, que es la mayor.

---

## Fase 2 — Android real ✅

- [x] `:engines:gms-code-scanner` — Google Code Scanner, sin permisos
- [x] `:engines:mlkit-camerax` — ML Kit Barcode + CameraX, con linterna, zoom y decodificación de
  imagen
- [x] `PermissionController` actual de Android + flujo de denegación permanente
- [x] Arranque de Koin por plataforma (`initKoin`) con `Context` en Android
- [x] Preview de Android como capacidad del motor (`CameraPreviewEngine`, ADR-0007)
- [x] Overlay común de detección sobre `cornerPoints` normalizados (`ScanOverlay`)
- [x] Controles de linterna en la UI, derivados de las capacidades declaradas
- [x] Historial persistente con Room KMP (`:core:database` + `:feature:history`)
- [x] Navegación entre escáner e historial, con botón atrás de Android
- [x] CI en GitHub Actions: detekt + tests + Android + Desktop + Web en cada PR. iOS salió de
  `Verify` y vive en el workflow `iOS (manual)`, a demanda — ver la Fase 3
- [x] Higiene del repo: `.editorconfig` alineado con detekt, `.idea/` fuera del control de versiones
- [x] Preferencias persistentes con `multiplatform-settings` (D2) y control de zoom en la UI (D8)
- [x] `ScanRequest.timeoutMillis` implementado (`DeadlineScannerEngine`): estaba en el modelo desde
  la Fase 1 sin que ningún código lo cumpliera
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
- [x] `:engines:ocr` — Text Recognition en Android + `OcrCodeInterpreter`: lee el número
  impreso bajo el código y solo lo emite si el dígito de control cuadra
- [x] Preview de Web (D14): el `<video>` vive en el documento, sobre el canvas, y el composable solo
  le dice qué rectángulo ocupar. Tapa el overlay, y eso pasa a ser una capacidad declarada
  (`occludesOverlay`) en vez de un dibujo invisible
- [x] **OCR en iOS, con el reconocedor del sistema.** ML Kit se distribuye por CocoaPods, que este
  proyecto no usa, así que iOS va por `VNRecognizeTextRequest` del framework Vision — cero
  dependencias nuevas y cero bytes de modelo en el binario. Reutiliza `OcrCodeInterpreter`
  entero: **toda la parte que decide algo es la misma que en Android**, y con ella sus tests.
  Tres decisiones que no eran obvias y que conviene tener escritas:
  **(1)** es un **motor propio del catálogo** (`VISION_OCR`) y no un `actual` de `MLKIT_OCR`. Son
  dos reconocedores de dos fabricantes; presentarlos como uno haría que el comparador atribuyera a
  ML Kit lo que leyó Vision, que es el mismo error que D13 evitó con zxing-java. De paso se
  corrige algo que llevaba desde la Fase 4: `MLKIT_OCR` **declaraba iOS**, así que el catálogo
  prometía en un iPhone un motor que solo podía responder `NotImplemented`;
  **(2)** `AVCaptureVideoDataOutput` y no la salida de metadatos, por lo mismo que zxing-cpp — la
  de metadatos entrega códigos ya decodificados, y el objeto de este motor es precisamente el
  código que nadie consiguió decodificar;
  **(3)** `usesLanguageCorrection = false`. La corrección lingüística está entrenada para arreglar
  texto, y sobre trece cifras solo puede inventar. Con `recognitionLevel = .accurate`, que cuesta
  caro por frame y por eso el motor descarta frames tarde a propósito.
  **Dos tandas de `iOS (manual)`, y la primera dejó algo que merece quedar escrito**: los diez
  errores estaban todos en un archivo y eran todos la misma cosa —**cinterop no traduce igual todos
  los `NS_ENUM`**—. Los de AVFoundation que usa el resto del proyecto (`AVAuthorizationStatus…`)
  son declaraciones de nivel superior; los de UIKit (`UIDeviceOrientation`, `UIImageOrientation`)
  **no existen sueltas** y hay que cualificarlas por su tipo; y los `kCGImagePropertyOrientation*`
  de ImageIO vuelven a ser de nivel superior, aunque su tipo sí se importe. Es la tercera vez en
  este proyecto que el fallo de Kotlin/Native es "importé como una cosa lo que cinterop genera como
  otra" —las dos anteriores fueron los miembros de AVFoundation y el `Dispatchers.IO` de
  `:core:database`—, y ninguna de las tres la puede ver nada que no compile para iOS. Lo demás
  —`performRequests`, `VNImageRequestHandler`, `topCandidates` y las cuatro esquinas de
  `VNRecognizedTextObservation`— compiló a la primera
- [ ] Lo que sigue sin comprobarse del OCR de iOS, dicho aparte del cableado: que **lea**. El job
  `iOS (manual)` enlaza el framework entero con el motor dentro —comprobado, en verde—, y eso cubre
  la superficie de cinterop, que es donde han estado todos los fallos de este proyecto en
  Kotlin/Native. No cubre que Vision encuentre los dígitos, ni que la orientación con la que se le
  pasan los frames sea la correcta. Hace falta un iPhone, igual que el resto de la Fase 3
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
- [x] `ScanRequest.continuous` y `allowMultiple` se respetan de verdad (
  `RequestLimitsScannerEngine`):
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
con sus latencias en la portada, ni una app con nombre de proyecto interno y sin icono.

### Ronda 1 — marca, sistema de diseño, tema e idiomas ✅

- [x] **Nombre y marca.** `applicationId` propio — se cambia ahora porque después de la primera
  publicación en Play ya no se puede. En esta ronda el nombre de producto y el interno todavía
  eran distintos; la Ronda 3 los unifica en **WhyScan**
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
- [x] Estados de permiso y de "aquí no hay cámara" con su motivo y su salida, resueltos con un
  `when`
  sobre cuatro casos excluyentes en vez de condiciones sueltas
- [x] La sesión **arranca sola** al aparecer la pantalla y se apaga al salir. La cámara seguía
  capturando mientras el usuario miraba el historial: el ViewModel sobrevive a la navegación y
  nadie paraba la sesión
- [x] Animación al crecer la hoja, tope de cien resultados vivos, pausa y reanudación sobre el visor
- [x] **D18 saldada para los módulos comunes y el escritorio** (`KoinGraphTest`). El
  `platformModule`
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

### Ronda 3 — cerrada ✅

- [x] **Un solo nombre: WhyScan, en todas partes.** El proyecto convivía con dos —uno de producto y
  uno interno—, y cada documento cargaba con una nota explicando por qué. Se unifican el nombre
  del proyecto Gradle, los paquetes de Kotlin (`com.whyscan.*`), el `namespace` de cada módulo,
  los ids de los plugins de convención (`whyscan.kmp.library`, `whyscan.kmp.compose`,
  `whyscan.android.application`), el `applicationId` (entonces `com.whyscan.app`; hoy
  `ar.net.faro.whyscan`, ADR-0019), los tipos del sistema
  de diseño (`WhyScanTheme`, `WhyScanMark`, `WhyScanTypography`, `WhyScanShapes`), el tema de
  Android (`Theme.WhyScan`), la clase `Application` y los almacenes de datos de las cuatro
  plataformas. **Se escribe siempre como una sola palabra.** Nada que migrar: la app no se ha
  publicado, así que cambiar el nombre del fichero de base de datos y de los almacenes de
  preferencias no deja datos huérfanos a nadie
- [x] **El visor pausado dejaba un spinner girando para siempre.** Al pausar, `activeEngineId` pasa
  a
  `null` y con él desaparece la superficie de preview; el `when` de `ViewfinderArea` no tenía caso
  para eso y lo absorbía por la rama final. La píldora de estado sí decía "Pausado", así que la
  pantalla se contradecía a sí misma. Ahora el spinner solo sale en `Starting` y pausado es un
  estado con nombre, icono y "Reanudar"
- [x] **Notas del usuario en el historial.** Un código leído es exacto y completamente inútil dentro
  de una lista de doscientas filas cuando lo que uno recuerda es "el del pedido de marzo".
  `HistoryEntry(detection, note)` como tercer nivel del modelo, y **no** un campo de `Detection`:
  esa la producen los motores y la atraviesan seis decoradores, el comparador y el marcador
  ([ADR-0012](adr/ADR-0012-la-nota-es-del-historial-no-de-la-deteccion.md))
- [x] **Tres defectos de persistencia que la nota destapó**, y ninguno se había disparado nunca
  porque nunca había habido una versión 2 del esquema:
  **(1)** la base se construía con `fallbackToDestructiveMigration(dropAllTables = true)`, así que
  la primera migración habría borrado el historial de todo el mundo en silencio — ahora va por
  `@AutoMigration` y lo destructivo queda solo para las bajadas de versión;
  **(2)** el `upsert` con `REPLACE` es un borrado más un alta, y como el id de una detección es
  determinista, releer el mismo código se llevaba la nota por delante — pasa a `INSERT OR IGNORE`;
  **(3)** la poda borraba por antigüedad sin mirar si la fila estaba anotada
- [x] **Buscador sobre el valor y la nota.** Media razón de poder anotar: nadie recuerda una tirada
  de dígitos. Con él, un historial lleno cuyo filtro no deja nada deja de decir "todavía no
  escaneaste nada", que con cien lecturas detrás era mentira
- [x] **Borrar una lectura suelta** —antes era todo o nada— y **confirmación antes de vaciar**, con
  el número de lecturas que se pierden. Era la única acción irreversible de la app y se
  disparaba con un toque, sin copia en ninguna parte
- [x] **La lección de D16 aplicada antes de repetirla.** La nota pedía dos casos de uso nuevos de
  una
  línea junto a `ObserveScanHistoryUseCase` y `ClearScanHistoryUseCase`, que ya delegaban sin
  añadir nada. Los dos se borraron y su trabajo vive en `ScanHistory`. De paso se retira
  `findById` del contrato: no lo llamaba nadie desde la Fase 1
- [x] **D18 saldada del todo.** `AndroidKoinGraphTest` monta el `platformModule` de Android —el más
  grande de los cuatro y el único donde ocurrió el crash— con Robolectric, que da un `Context`
  de verdad en la JVM. Incluye el test que le faltaba a la deuda: que el executor de análisis
  resuelva por `Executor` y no por `ExecutorService`. Lo que **no** cubre queda escrito en el
  propio archivo: `sqlite-bundled` trae binarios de las ABI de Android y bajo Robolectric el
  proceso es una JVM de escritorio, así que el historial persistente se queda fuera — y esa
  misma cadena sí se resuelve de verdad en el `KoinGraphTest` de escritorio
- [x] **Transiciones entre destinos:** *fade through* de Material 3. Fundido y no deslizamiento
  porque los cuatro destinos son hermanos y se alcanzan desde la misma barra en cualquier orden;
  deslizar contaría una jerarquía que no existe y obligaría a deducir la dirección del índice en
  la barra, que se rompe al reordenar los ítems o al ocultar el comparador

**Lo que esta ronda no pudo cerrar, y por qué.** Las dos cosas que quedan necesitan ejecutar la app
en un dispositivo, así que se mueven a "Pendiente para publicar" en lugar de arrastrarse de ronda en
ronda como si fueran trabajo pendiente:

- Objetivos táctiles y `enableEdgeToEdge` **mirados con los ojos** (pendiente desde la Fase 5).
- El aviso `KoinContext is not needed anymore` de `App.kt` (D20): quitarlo cambia por dónde
  resuelven
  `koinInject` y `koinViewModel`, y eso no lo comprueba ningún test sin abrir la app.

### Ronda 4 — en curso 🚧

Lo que salió al revisar la app con la vista puesta en "clase mundial". Ordenadas por lo que aportan
frente a lo que cuestan, y con el motivo de cada una escrito para que la decisión de hacerlas o no
sea explícita.

**Hecho**

- [x] **D22 saldada: la migración se ejecuta de verdad.** Room valida `@AutoMigration` en
  compilación
  contra los esquemas exportados, lo que garantiza que el SQL es correcto — pero **un esquema
  correcto es perfectamente compatible con haber borrado la tabla y haberla recreado**, que es lo
  que este proyecto hacía hasta la ronda anterior. Un test de esquema le habría dado el visto
  bueno. Así que `MigrationTest` no mira el esquema: levanta una base v1 con el `createSql`
  literal del `1.json`, le escribe filas, la abre con el código v2 y comprueba que siguen ahí —
  y que la columna nueva acepta datos
- [x] **Deshacer un borrado.** Borrar una fila no pregunta, y por eso ahora se puede deshacer: son
  las dos caras de la misma decisión, porque un diálogo por fila convierte limpiar veinte
  lecturas en veinte interrupciones. Restituir devuelve la nota y coloca la fila **en su sitio
  por fecha**, lo que obligó a que los almacenes de Web y de memoria ordenen como ya ordenaba la
  consulta de Room — una divergencia entre plataformas que llevaba ahí desde la Fase 4
- [x] **La búsqueda ignora los acentos.** En español media gente escribe "factura" buscando lo que
  guardó como "Factúra", y desde un teclado sin tildes no hay otra opción. Que un buscador no
  encuentre algo que está delante no parece un fallo: parece que el dato no existe. La eñe **no**
  se pliega, que es una letra distinta y no una `n` con adorno
- [x] **El historial se agrupa por día**, con cabeceras pegajosas que dicen "Hoy" y "Ayer" en lugar
  de la fecha, que es lo que una persona reconoce sin leer. Es lo que hizo falta traer
  `kotlinx-datetime`, la dependencia que §9.7 había evitado a conciencia: aquel razonamiento
  —no arrastrar una librería de fechas por una columna de un CSV— sigue siendo correcto para
  aquello y deja de serlo aquí, porque agrupar por día **no es aritmética sobre milisegundos**
  (zona horaria, horario de verano y calendario). La agrupación recibe la zona por parámetro
  para ser pura y probable
- [x] **Exportar a texto plano.** Una lectura por línea, sin cabecera y sin comillas. CSV y JSON son
  para herramientas; lo que la gente hace con treinta códigos es pegarlos en un correo. No lleva
  guardado anti-fórmula **a propósito**, y hay un test que lo fija: esto no lo abre una hoja de
  cálculo, y una comilla delante rompería justo lo que el formato existe para dar
- [x] **Anotar desde la pantalla de escaneo**, no solo desde el historial. El momento en que uno
  sabe
  para qué es un código es justo cuando lo acaba de leer; obligar a terminar de escanear, cambiar
  de pantalla y reconocer la lectura entre las demás es pedirle al usuario que recuerde dentro de
  un minuto lo que sabe ahora. **El escáner no guarda notas**: las lee del historial y las
  escribe allí. Recordarlas en el estado de la pantalla parecía más simple y tenía un agujero —
  `Detection.idOf` es determinista, así que releer un código ya anotado devuelve la misma fila, el
  campo se habría abierto vacío y guardar habría borrado lo que hubiera. Es el mismo defecto que
  el `REPLACE` de Room, en otro sitio

- [x] **Modo dislexia**, en Ajustes → Accesibilidad. Ajusta la escala tipográfica entera: más
  espacio
  entre letras —lo único con evidencia sólida detrás, Zorzi et al. PNAS 2012—, más interlínea y
  más tamaño, que se **suma** al del sistema en vez de sustituirlo. **No empaqueta una fuente
  "para dislexia" a propósito:** los estudios controlados sobre OpenDyslexic y Dyslexie no
  encuentran mejora frente a una sans-serif bien espaciada, así que se aplica directamente lo que
  sí funciona en lugar de pagar 300 KB por peso por una promesa que la evidencia no sostiene. El
  valor de un código sigue monoespaciado incluso aquí: se coteja carácter a carácter contra una
  etiqueta impresa, y hacerlo más legible como prosa lo haría menos legible como dato

**Lo que hace falta antes de publicar de verdad**

- [x] **Qué hay de nuevo.** Una función que nadie encuentra es una función que no está: la nota, el
  buscador y la agrupación por día no añaden un botón, cambian lo que hace una pantalla que el
  usuario ya creía conocer. El diálogo sale **una sola vez tras una actualización** y queda
  accesible desde Ajustes → Acerca de, porque quien lo cierra sin leerlo también se merece
  poder volver. Lo que decide si hay algo que contar es una **revisión propia** y no
  `versionName`: esa sube por arreglos que no le importan a nadie, y además solo existe en el
  módulo de Android. **A quien acaba de instalar no se le estrena nada** —para él todo es
  nuevo—, y ese caso es una función pura con su test, no una condición suelta dentro de un
  `LaunchedEffect`. Escribiendo la primera versión apareció una carrera de las que solo se ven
  en un dispositivo: leer la revisión del estado observado la daba por `null` durante la primera
  composición, así que a un usuario con novedades pendientes se le habrían marcado como vistas
  antes de que llegara su valor de disco
- [x] ~~**Baseline Profile.**~~ Se aplazó aquí para decidirlo con datos y se resolvió en la Ronda 7:
  módulo, cableado, ADR-0013 y —desde esta revisión— el perfil generado y versionado

**Deuda de calidad que ya se puede ver**

- [x] **`Detection.idOf` usaba `rawValue.hashCode()`.** Dos valores distintos con el mismo hash,
  leídos por el mismo motor en el mismo milisegundo, colisionaban — y con `INSERT OR IGNORE` la
  segunda lectura se descartaba en silencio, que es la peor forma de perder un dato. La
  probabilidad es ínfima; lo que cambió es la consecuencia, porque desde las notas de este id
  cuelga texto que escribió una persona. Pasa a FNV-1a de 64 bits, diez líneas en `:core:model`
  en lugar de una dependencia de hashing por una función: no es criptográfico y no hace falta
  que lo sea, solo separa lecturas distintas. El test usa la colisión de manual de
  `String.hashCode()` —`"Aa"` y `"BB"` dan los dos 2112— para fijar exactamente lo que se
  arregló. **Efecto de una sola vez:** las filas ya guardadas conservan su id viejo, así que un
  código anotado antes del cambio y releído después crea una fila nueva en vez de reconocerse.
  Se acepta porque la app no está publicada, que es la única ventana en la que esto sale gratis

### Ronda 5 — auditoría de clase mundial 🚧

Revisión contra seguridad, calidad, robustez, estabilidad, arquitectura y buenas prácticas. Lo que
salió, ordenado por lo que costaría equivocarse.

**Hecho**

- [x] **La copia de seguridad del sistema subía el historial a la nube.** Es el hallazgo más grave
  de todo el proyecto y llevaba ahí desde la Fase 1. `allowBackup="true"` hacía que Android
  subiera `databases/` a la cuenta de Drive del usuario, y en iOS pasaba lo mismo sin bandera
  que lo delatara — `Documents` entra en iCloud por defecto. Lo que se subía incluye el
  `rawValue` **literal**: un QR de WiFi se guarda como `WIFI:T:WPA;S:red;P:clave;;`, con la
  contraseña dentro. **La ausencia de `INTERNET` no cubría esto**, y ahí está la lección: el
  backup no lo hace la app sino un proceso del sistema, así que la garantía más fuerte del
  producto tenía una puerta que no pasaba por él. En Android hacen falta **dos** cosas y no una:
  `allowBackup="false"` apaga la nube y `dataExtractionRules` cierra la transferencia entre
  dispositivos, que desde Android 12 es un canal aparte. Coste aceptado y escrito: **al cambiar
  de teléfono se pierde el historial**; la vía que queda es exportarlo, y es del usuario. Queda
  un chequeo en CI que falla si el manifiesto vuelve atrás — no es un error de compilación ni de
  lint, es una promesa de producto que descansa en tres líneas de XML, y sin eso no la vigilaba
  nadie

**Pendiente, por orden de impacto**

- [x] **Treinta `viewModelScope.launch` sin una sola red de seguridad.** `viewModelScope` es
  `SupervisorJob() + Dispatchers.Main.immediate`, y el supervisor solo evita que un hijo que
  falla cancele a sus hermanos: la excepción **sigue subiendo** hasta el manejador por defecto
  del hilo, que mata el proceso. Y como Room abre el archivo de forma perezosa —en la primera
  consulta, no al construir la base— esa primera consulta ocurre siempre dentro de una
  corrutina: **una base corrupta era un cierre en el arranque** que el usuario no podía deshacer
  sin borrar los datos de la app. Lo cierra `launchCatching`, con dos decisiones que son la
  mitad del valor: **la `CancellationException` se relanza** —tragarla rompe la concurrencia
  estructurada y convertiría cada salida de pantalla en un error delante del usuario— y **se
  captura `Exception` y no `Throwable`**, porque un `OutOfMemoryError` dice que el proceso ya no
  está en condiciones de seguir. No es un `CoroutineExceptionHandler` en el ámbito a propósito:
  eso capturaría también los errores de programación y los volvería un mensajito, que con un CI
  que no ejecuta la app los haría invisibles para siempre. Aquí cada sitio se acoge a mano.
  El historial gana además un estado `loadFailed`, porque un fallo de lectura dejaba la lista
  vacía y la pantalla decía "todavía no escaneaste nada" sobre un historial que sí existe
- [x] ~~**El objetivo de cobertura no lo mide nada.**~~ El SDD §13.1 decía "≥ 80 % en `:core:domain`
  y `:core:data`" sobre 51 ficheros de test y **nadie sabía el número**. Kover mide los dos
  módulos y `tools/coverage.py` exige el umbral en CI, listando además **qué paquetes están
  peor** —lo único que Kover no dice, y la pregunta útil cuando la cobertura baja—.
  **Lo que encontró la primera medición no fue falta de tests:** `:core:domain` dio 89,0 % y
  `:core:data` 60,8 %, y la razón del segundo era `InMemoryRepositories.kt`, dos clases que
  ningún módulo de Koin declaraba desde que Room y el almacén de `Settings` las sustituyeron
  (D1 y D3, cerradas hace tiempo). Escribirles tests habría subido el número sin proteger nada;
  se borraron. La otra mitad sí era un hueco: `SettingsAppPreferencesRepository` no tenía un
  solo test pese a guardar dos decisiones que solo vivían en comentarios —los enums por su `id`
  estable, y `null` distinto de cero en `lastSeenNewsRevision`—, y las dos se rompen sin que
  compile nada mal. Resultado: `:core:data` de 60,8 % a **83,7 %**, y `--min 80` exigido en CI
- [x] ~~**Nada compone la raíz.**~~ `AppCompositionTest` monta `App()` con `runComposeUiTest`, en la
  JVM y sin emulador, sobre el grafo **real** de escritorio: un doble ahí comprobaría que Compose
  sabe pintar dobles. Cierra la familia de fallos que `KoinGraphTest` no ve —un
  `CompositionLocal` que falta, un `stringResource` cuya clave se borró, un `remember` que
  lanza—, todos con el mismo final que el defecto original de D18: compila, pasa lint, pasa R8 y
  revienta al abrir la app. Arranca en Ajustes y no en el escáner porque montar el escáner
  pediría cámara, y un test que exige hardware es un test que no corre en cada PR. Fija además
  que sin modo avanzado el comparador **no** esté en la barra, que hasta ahora solo se sostenía
  en `destinationsFor`
- [x] ~~**Nada vigila las dependencias.**~~ Dependabot semanal sobre el catálogo de versiones y
  sobre
  las propias actions —que son código de terceros corriendo con el token del repositorio—, y
  `dependency-review-action` en cada PR. Va agrupado y con pocos PR abiertos a la vez a
  propósito: un Dependabot que abre quince el lunes se ignora en la tercera semana, y entonces
  no vigila nada. Con `gradle/actions/dependency-submission` en `main`, sin lo cual GitHub no
  sabe leer un proyecto Gradle y la revisión pasaría siempre en verde sin mirar una línea
- [x] ~~**`mailto:` se concatena sin escapar.**~~ Y no era solo el `mailto:`: `tel:` y `sms:`
  concatenaban igual. Todo lo que viene del código pasa ahora por `percentEncode`, que conserva
  lo que un destino legítimo necesita —la `@` del buzón, el `+` internacional, los separadores
  visuales de un número— y codifica lo demás sobre UTF-8. Cierra dos cosas: una dirección con
  `?cc=…&body=…` dentro componía un correo entero a nombre de quien solo apuntó la cámara, y una
  `#` en un teléfono partía el URI en un fragmento, así que lo que se marcaba no era lo que el
  usuario estaba leyendo

**Mirado y correcto, para no volver a gastar escrutinio ahí**

- El manejo de URLs es seguro **por construcción**: `parseUrl` solo clasifica como enlace lo que
  empieza por `http://`, `https://` o `www.`, así que `javascript:`, `intent://`, `file://` y
  `content://` no producen acción de abrir. En un lector de códigos, donde el atacante controla el
  contenido entero y la víctima solo apunta la cámara, esa lista blanca implícita es **la** decisión
  de seguridad que importa
- `exported="true"` en `MainActivity` es correcto y obligatorio: es el único `intent-filter` y es
  `MAIN`/`LAUNCHER`. No hay superficie exportada de más
- La poda del historial está acotada de verdad (`dao.trimTo` en cada guardado)

### Ronda 6 — D20, cerrada sin dispositivo ✅

- [x] **Fuera el envoltorio `KoinContext { }` de `App.kt`.** Lo que mantenía abierta la deuda era
  creer que quitarlo no se podía comprobar sin instalar la app. Sí se puede: `koinInject` no es
  UI —lee un `CompositionLocal` y llama a `remember`—, así que basta el **runtime** de Compose,
  que es Kotlin puro. `ComposeKoinContextTest` monta una `Composition` con un `Applier` que no
  aplica nada y comprueba que `koinInject` devuelve la misma instancia que `koin.get()`, para un
  tipo de los módulos comunes y otro que depende del `platformModule`

### Ronda 7 — arranque: el baseline profile ✅

- [x] **Baseline profile** (ver [ADR-0013](adr/ADR-0013-baseline-profile.md)). La app arranca
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
- [x] **El perfil existe.** `baseline-prof.txt` (31.575 líneas) y `startup-prof.txt` (27.831) en
  `androidApp/src/release/generated/baselineProfiles/`, que es de donde los toma la build de
  release. La grabación recorrió la app de verdad y no solo el framework: 2.325 entradas de
  `com/whyscan` —`AppKt`, `Navigator`, el repositorio de preferencias, los destinos— junto a 14.205
  de Compose, 418 de Room y 367 de Koin, que son exactamente las tres cosas que ADR-0013 señala
  como el coste del primer arranque
- [x] **Y el workflow lo commitea solo, que es lo que hacía falta para que existiera.** Lo que lo
  mantuvo pendiente no fue el emulador: fue el último paso, que era manual. El propio workflow
  decía "descarga el artefacto, cópialo a `androidApp/src/…` y haz commit", y la tarea escribe ese
  archivo **dentro del runner**, que se destruye al terminar. Así que cada ejecución producía el
  perfil y lo tiraba, salvo que alguien con el repositorio delante lo rescatara a mano. Un
  artefacto generado que hay que mover a mano es un artefacto que no se mueve — y este llevaba una
  ronda entera sin moverse. Ahora el job hace `git commit` sobre la rama en la que se lanzó, y solo
  si el perfil cambió: que no cambie también es información, quiere decir que el camino de arranque
  sigue siendo el mismo

### Ronda 8 — una marca que distingue en vez de agrupar ✅

Ver [ADR-0014](adr/ADR-0014-la-marca-sale-del-objeto.md). El sistema de diseño estaba bien
construido; lo que no se había examinado era sobre qué se construyó. El símbolo eran **cuatro
esquinas de encuadre y una línea de lectura** —el icono `QrCodeScanner` que Material ya trae y que
usan otras doscientas apps de la tienda— y el color era el azul por defecto de Tailwind. En una
ficha
de Play, junto a sus competidores, eso no distingue: **agrupa**.

- [x] **La marca es el módulo fugado**: el patrón de localización de un QR —los cuadrados anidados
  de
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
- [x] **El color de arranque**, en Android (`values/` y `values-night/`) y en Web (`theme-color` y
  el
  fondo del `body`): tiene que coincidir con el `background` del tema o se ve un destello de otro
  color entre que el sistema pinta la ventana y Compose pinta la primera pantalla

> **Lo que se descartó, que es parte de la decisión.** Una marca hecha con la inicial del nombre —la
> salida cómoda: una letra no dice nada del producto y la copia cualquiera— y un signo de
> interrogación jugando con el nombre, que compite con «ayuda» en una cuadrícula de aplicaciones.
> Cerradas esas dos, la única dirección honesta era mirar el objeto que la app lee.

---

## Auditoría por pilares — rondas 9 a 14 🚧

Una ronda por pilar, abiertas a la vez y cerrables por separado. El criterio para que algo entre
aquí es que **se pueda señalar el archivo y la línea**: lo que solo se puede intuir sin un
dispositivo ya tiene su sitio en "Pendiente para publicar" y no se disfraza de hallazgo.

Conviene decir lo que esta auditoría **no** es. No es una lista de buenas prácticas contrastada
contra la app: es lo que quedó después de mirar el código y descartar lo que ya estaba bien. Esa
segunda parte también se escribe —abajo, en cada ronda— porque saber dónde **no** hace falta gastar
escrutinio vale tanto como saber dónde sí, y porque una auditoría que solo enumera defectos invita a
tocar cosas que funcionan.

### Ronda 9 — seguridad y privacidad ✅

- [x] **El portapapeles ya no enseña lo que se copia.** `copyToClipboard` marca el contenido con el
  extra `android.content.extra.IS_SENSITIVE`, así que Android 13+ deja de pintar la
  **previsualización flotante** con el texto copiado. Sin eso, copiar un QR de WiFi —cuyo `rawValue`
  es literalmente `WIFI:T:WPA;S:red;P:clave;;`— enseñaba la contraseña encima de cualquier app,
  delante de quien estuviera mirando la pantalla.
  **Es exactamente la forma del hallazgo del backup de la Ronda 5**, y por eso importa más que su
  tamaño: la garantía de privacidad de este producto se apoya en no tener `INTERNET`, y esto —igual
  que el backup— **no lo hacía la app**, lo hacía un proceso del sistema al que ese permiso le da
  igual. Van dos puertas del mismo tipo; conviene asumir que hay más y buscarlas por ahí.
  Se marca **siempre** y no solo cuando el valor parece secreto, porque el usuario ya está viendo el
  valor en pantalla justo encima del botón: la previsualización no le informa de nada y su peor caso
  es una credencial a la vista. Con esa asimetría, clasificar qué es sensible añadiría una decisión
  que puede equivocarse a cambio de no ganar nada.
  Y no se afirma: `AndroidPlatformActionsTest` lo comprueba con Robolectric, que da un
  `ClipboardManager` de verdad en la JVM y corre en cada pull request.
- [x] **La lista blanca de esquemas se comprueba en el borde, no solo en el dominio.**
  `isOpenableUri` en `:core:platform` acepta los seis esquemas que `ResultActionsFactory` puede
  producir —`http`, `https`, `mailto`, `tel`, `sms`, `geo`— y rechaza todo lo demás fallando
  cerrado. Lo consultan las implementaciones de Android, Escritorio y Web antes de entregarle nada
  al sistema.
  Lo que había antes no era un agujero abierto: era que **lo único que impedía que llegara un
  `javascript:` o un `intent://` era que nadie llamaba a `openUrl` de otro modo**, y eso es una
  propiedad del grafo de llamadas, no del método. Se cumplía hoy y dejaba de cumplirse el día que
  alguien añadiera un camino, sin que nada avisara.
  **Donde más falta hacía era en Web**, y no se vio hasta escribirlo: `window.open("javascript:…")`
  no abre otra app, **ejecuta** ese código dentro de nuestra página. Es la única de las cuatro
  plataformas donde el esquema equivocado no es un problema de otro.
  La lista queda escrita dos veces, una en cada lado de una separación que debe existir —el dominio
  no sabe que hay un sistema operativo—, así que `OpenableUriDriftTest` ata las dos: comprueba que
  todo lo que el dominio ofrece abrir pasa el borde, y falla en las dos direcciones.
- [ ] **Falta iOS, y es una línea.** `IosPlatformActions.openUrl` sigue sin la guarda. No se tocó
  por la regla de arriba —iOS no se toca por iniciativa propia— y no por falta de importancia; se
  cierra en cuanto la plataforma se desbloquee. Queda escrito para que la asimetría no se descubra
  por sorpresa.

*Sin comprobar en dispositivo:* que la capa del fabricante respete el extra del portapapeles por
debajo de API 33, donde la constante existe desde antes que su documentación.

**Mirado y correcto, para no volver a gastar escrutinio ahí.** El manifiesto está bien y no por
casualidad: sin `INTERNET`, `allowBackup="false"` con `dataExtractionRules`,
`usesCleartextTraffic="false"`, un único `exported="true"` que es el `MAIN`/`LAUNCHER`, y
`uses-feature` de cámara declarado como no obligatorio. `percentEncode` cubre `mailto:`, `tel:` y
`sms:`. El CSV neutraliza fórmulas y el texto plano no, con un test que fija por qué. Nada de esto
hizo falta tocarlo.

### Ronda 10 — accesibilidad ✅

- [x] **El lector de pantalla ya lee el código como un código.** `spokenValue` en `:core:domain`
  devuelve el valor con los caracteres separados cuando lo que hay es un código, y tal cual cuando
  es prosa. Antes las etiquetas habladas interpolaban el `rawValue` crudo, así que TalkBack
  pronunciaba un EAN-13 como *"siete billones quinientos un mil…"*. Para cualquier app eso sería un
  detalle; para **esta** es el producto: el motivo entero de que el valor vaya en monoespaciada es
  que alguien lo coteja **carácter a carácter** contra una etiqueta impresa, y a quien no ve la
  pantalla se le estaba negando justo eso.
  **El valor visible también lo lleva**, y ese era el sitio que faltaba en el hallazgo: se habían
  mirado las etiquetas de los botones, pero el `Text` con el valor no tenía ninguna descripción, así
  que era el primer sitio donde el lector se equivocaba.
- [x] **Deletrear no siempre ayuda, y esa es la mitad de la decisión.** Una URL leída letra a letra
  —`h t t p s d o s p u n t o s…`— no la entiende nadie, y un vCard entero es absurdo. Así que se
  deletrea solo lo que **no es una palabra**: un `Product`, que es un GTIN por definición, y un texto
  corto, sin espacios y mayoritariamente numérico —un número de serie, un lote, un Code 128 que
  ningún motor clasificó—. Se exige *mayoría* de dígitos y no *algún* dígito para que `edificio2` se
  siga leyendo como palabra; con la regla laxa el arreglo habría sido peor que el defecto.
  Y no se toca nada del valor: ni se quitan guiones ni se agrupa de tres en tres, porque lo que se
  anuncia tiene que ser lo mismo que hay en la pantalla y en la etiqueta o el cotejo deja de valer.
- [x] **Vive en el dominio, y conviene decir por qué no contradice a D15.** Lo que se decide ahí no
  es *cómo suena* sino **qué clase de valor es** —código o prosa—, que es una afirmación sobre el
  significado y la misma en los cuatro idiomas. La frase que envuelve al valor ("Copiar %s") la
  sigue poniendo la pantalla con sus recursos.
- [x] **El cableado tampoco lo cubría ningún test, y ya sí.** `spokenValue` tenía ocho casos en
  `commonTest`, pero que las seis llamadas de `ScannerResults` y `HistoryRow` siguieran usándolo no
  lo comprobaba nadie: alguien podía volver a poner `rawValue` y todo seguiría en verde. Lo cierra
  `HistorySemanticsTest` en la Ronda 17, que compone la pantalla y afirma sobre lo que **oye** un
  lector de pantalla. Dejó de ser opcional en la Ronda 15: desde que copiar, compartir, anotar y
  eliminar son iconos, la descripción hablada es lo único que nombra a esos botones.

*Sin comprobar en dispositivo:* que la separación por espacios produzca exactamente la prosodia
esperada en TalkBack y VoiceOver. Es la técnica habitual y no depende del idioma, pero cómo suena de
verdad solo lo dice un teléfono.

**Mirado y correcto.** Los seis `contentDescription = null` son **todos** decorativos y cinco llevan
escrito por qué: el icono repite lo que dice el texto de al lado, y anunciarlo haría que el lector
recorriera cada ítem dos veces. Es la decisión correcta y está tomada a conciencia, no por olvido —
que es el caso habitual cuando se ve un `null` ahí. No hay un solo `Modifier.clickable` propio en
todo el proyecto, así que los 48 dp de objetivo táctil los pone Material y no dependen de nadie.
`ContrastTest` mide 56 pares en los dos temas.

**Lo que esta ronda no puede cerrar:** TalkBack de verdad, los objetivos táctiles medidos y el
comportamiento con la escala tipográfica al máximo. Eso sigue necesitando un teléfono y ya está en
su bloque.

### Ronda 11 — internacionalización ✅

- [x] **Siete cadenas que contaban cosas pasan a `<plurals>`**, y eran siete y no una: el hallazgo
  original solo había mirado `history_clear_body`. Al ir a arreglarlo aparecieron
  `engine_formats_count`, `comparison_frames`, `comparison_failures`, `a11y_detections_in_view`,
  `comparison_needs_two_engines` y `comparison_counts`. Vale la pena decirlo porque es el patrón de
  siempre: la auditoría encuentra el ejemplar visible y el arreglo encuentra la familia.
  El peor era el que estaba a la vista: *"Se van a eliminar **1 lecturas**"*, en el **único diálogo
  irreversible de la app** — justo donde peor sienta que el texto parezca descuidado, porque es
  donde el usuario decide si fiarse.
- [x] **`comparison_counts` no se podía pluralizar tal cual, y ahí estaba lo interesante.** Llevaba
  **dos** contadores —"%1$d códigos distintos · %2$d lecturas"— y `pluralStringResource` elige la
  forma a partir de **una** cantidad. Se parte en dos plurales y la cadena queda como plantilla de
  unión (`%1$s · %2$s`), lo que además conserva el separador dentro del catálogo: juntarlos en
  Kotlin habría metido puntuación traducible en el código.
- [x] **`tools/checks.py` aprende a mirar plurales**, y no solo su paridad entre idiomas: comprueba
  que las dos lenguas declaren **las mismas cantidades**. Una `<item quantity="one">` que falte no
  rompe la compilación — revienta en ejecución y **solo con el número que la usa**, que es la clase
  de fallo que aparece con un elemento en la lista y no con dos. Comprobado borrando una a propósito
  antes de subirlo: la caza y dice qué idioma y qué cantidad.

**Y la fecha del historial se queda como está, que también es una decisión.** El hallazgo decía que
`history_date` fija el orden día-mes-año y que el inglés, al ser el respaldo de todo idioma que no
sea español, se lo impone a todo el mundo. Es cierto, pero **cambiarlo no lo arregla**: sin un
formateador que conozca el locale —y en KMP común no lo hay— cualquier orden fijo se equivoca con
alguien. Y entre los dos, día-mes-año es el que usan más hablantes de inglés y casi toda Europa, así
que como respaldo genérico es la mejor de las dos opciones malas. Se deja escrito para no volver a
"arreglarlo" dando la vuelta a la moneda.

**Mirado y correcto.** 203 claves con paridad inglés/español comprobada en CI antes que ninguna otra
cosa, y el inglés en la carpeta sin calificador para que sea el respaldo real. Eso ya evita el
defecto grande —un idioma con huecos— que es el que rompe pantallas.

### Ronda 12 — resiliencia 🚧

Esta ronda se abre **sin hallazgos bloqueantes**, y decirlo es el resultado: se buscó el fallo y no
está.

- Los quince `launchCatching` cubren lo que toca disco. Quedan dos `viewModelScope.launch` crudos y
  los dos están bien: uno lleva `.catch` **sobre el flujo** —que intercepta lo de aguas arriba y deja
  subir un defecto propio, y está razonado en el código—, y el otro solo emite a un `SharedFlow`.
- Las tres previews de Android atan el controlador de cámara al `LifecycleOwner`, así que irse a
  segundo plano suelta la cámara sin que nadie tenga que acordarse.
- [x] **Cerrado, y no con una comprobación sino quitando la pregunta.** Lo que quedaba era un hueco
  de conocimiento: la *sesión* seguía viva en segundo plano aunque la cámara se desatara, porque
  `DisposableEffect` no se dispara al minimizar. Al mirarlo de cerca, lo que soltaba la cámara era
  que **cada preview de Android** ata su controlador al `LifecycleOwner` por su cuenta — es decir,
  la propiedad se cumplía por debajo, en cada motor, y no en la pantalla. Eso no es una garantía:
  es una coincidencia que se rompe con el primer motor que se olvide. `LifecycleStartEffect` la
  sube al sitio donde se puede afirmar. Y "no se le conoce consecuencia" era precisamente lo que se
  dijo del driver de Room y del `Executor` de Koin.

  **Y este arreglo trajo un defecto peor, que hay que leer junto a él.** Atar la sesión al ciclo de
  vida estuvo bien; lo que estuvo mal fue atar **también** el arranque automático, que responde a
  otra pregunta. El resultado fue una cámara de la que no se podía salir — Ronda 20.

### Ronda 13 — verificación 🚧

El hallazgo era que el suelo de cobertura exigía 80 % en `:core:domain` y `:core:data` mientras los
cuatro ViewModels —scanner (483 líneas), history (231), comparison (140), settings (87)— vivían en
`:feature:*`, **fuera del gate**. Tienen tests, así que nunca fue "no está probado": era que **nadie
sabía el número**, el mismo reproche con el que se abrió la medición en la Ronda 5, un módulo más
allá.

- [x] **Paso uno: medir.** Kover aplicado a los tres módulos de feature y sus informes generados en
  CI. Entran en `coverage.py` **sin suelo**, que es un modo nuevo del script y no un descuido:
  `modulo=80` fija el suyo, `--min` pone el de por defecto, y un módulo a secas se mide y se informa
  sin poder fallar.
  Inventar un umbral antes de la primera medición solo tiene dos finales y los dos son malos: o
  rompe CI el primer día, o se elige tan bajo que no exige nada. Es exactamente cómo se hizo con
  dominio y datos —primero el número, después el suelo—, y aquella primera medición encontró algo
  que ningún umbral habría encontrado: código muerto.
- [ ] **Paso dos: fijar el suelo, con el dato delante.** Y decidir con él una pregunta que ahora
  mismo no se puede contestar a ciegas: si el número de un módulo de Compose significa algo tal
  cual, o si hay que excluir los `@Composable` para que hable de la lógica en vez de la UI. La lista
  de "paquetes con menos cobertura" que imprime el script lo va a decir en la primera ejecución.

  **Lo que bloquea este paso no es trabajo, es un número**, y el número solo lo produce un run de
  CI: la medición ya corre y se publica en el resumen. Se intentó cerrarlo en la Ronda 17 y se
  descartó a conciencia — configurar la exclusión de `@Composable` *antes* de mirar el dato habría
  sido decidir a ciegas, que es exactamente lo que este paso dice que no se haga.
- [x] **Paso tres: el cableado de la Ronda 10.** `HistorySemanticsTest` compone `HistoryContent` con
  datos sembrados y afirma sobre lo que **oye** un lector de pantalla: que un EAN-13 se anuncie cifra
  a cifra, que copiar, compartir y eliminar lleven el valor dentro, que dos filas no se anuncien
  igual y que una URL no se deletree. Corre en la JVM con `runComposeUiTest`, sin ventana. Dejó de
  ser opcional en la Ronda 15: desde que esos botones son iconos, la descripción hablada **es lo
  único que los nombra**.

### Ronda 14 — rendimiento: sin baseline no hay ronda 🚧

- [ ] **No se abre con hallazgos a propósito.** Con el baseline profile ya generado, lo único que
  queda por decir del arranque es **cuánto** mejora, y eso exige medir contra un punto de partida en
  un dispositivo. Cualquier afirmación que se escribiera aquí hoy —recomposiciones de más, tamaño
  del APK, latencia por motor— sería una intuición con formato de dato. El proyecto ya tiene el
  instrumento para lo primero (`:baselineprofile` con macrobenchmark) y para lo último
  (`EngineScoreboard`, que mide latencia por motor **dentro de la app**). Lo que falta es el
  teléfono, y hasta entonces esta ronda existe para que el hueco tenga nombre en vez de parecer que
  nadie lo miró.

### Ronda 15 — botones que caben, y probar un motor de verdad ✅

No es una ronda de la auditoría por pilares: sale de mirar las pantallas en un ancho estrecho y con
el cuerpo de letra subido, que es como las va a ver mucha gente.

- [x] **Las filas de acciones se salían de la pantalla, y la culpa era de las palabras.** Una fila
  del historial llevaba cinco botones de texto seguidos —"Abrir enlace · Copiar · Compartir ·
  Agregar nota · Eliminar"—, y la barra del propio historial otros cuatro con "Borrar" al final. En
  la pantalla de escaneo el problema ya estaba **reconocido y esquivado**: anotar vivía en su propia
  fila justo porque un cuarto botón no cabía. Convertidas en icono las acciones que tienen un
  símbolo que ya no hay que aprender —copiar, compartir, anotar, eliminar, y los tres mandos del
  comparador: comparar, detener, reiniciar—, cabe todo y anotar vuelve con las demás.
- [x] **Abrir conserva la palabra, y esa es la línea.** "Abrir enlace", "Llamar", "Escribir",
  "Enviar SMS" y "Ver en el mapa" son cinco cosas distintas que ningún icono separa; ahí la etiqueta
  es lo único que dice qué va a pasar al tocar. La regla vive en un tipo, `ResultActionLook`, y no
  en un `if` repartido por las pantallas
- [x] **No se pierde nada por el camino (RNF-05).** Cada icono lleva su descripción hablada, y en las
  acciones sobre un resultado es la de siempre —la que ya incluía el valor—, así que un lector de
  pantalla sigue diciendo "Copiar 7 7 0 1…" y no "Copiar" cinco veces. Las cadenas que dejaron de
  dibujarse pero siguen nombrando al botón se quedaron; las que no nombraban nada —`result_copy`,
  `result_share`, `history_delete`— se borraron de los dos catálogos, que es lo que impide que
  alguien las traduzca por gusto
- [x] **"Probar ahora", al lado de "Elegir" en cada ficha de motor.** Elegir guardaba una preferencia
  y devolvía al usuario a la misma lista de fichas, donde a la vista no cambiaba nada; la pregunta
  que uno se hace delante del catálogo es "¿qué tal lee **este**?", y esa solo la contesta la cámara
  abierta. Ahora elige el motor, reinicia la sesión con él y abre el visor a pantalla completa. Solo
  sale en los motores que declaran `LiveCamera`, que es la regla de siempre: la UI no nombra
  motores, lee capacidades
- [x] **La pantalla completa es un `Dialog` y no un destino.** La pantalla de escaneo vive dentro del
  `Scaffold`, entre la barra de navegación y los insets: cualquier cosa dibujada ahí nace recortada.
  Un destino más, además, sería un sitio al que se puede volver con el botón atrás desde donde no
  tiene sentido. Dentro se reutiliza todo —el mismo `ViewfinderArea` y la misma tarjeta de
  resultado—, y el visor de debajo deja de componer su superficie mientras tanto: **dos vistas de
  preview sobre el mismo motor se pelean por la sesión**, y ese es un sexto caso con nombre en el
  `when` del visor, no un `previewEngine = null` que mentiría diciendo "pausada"
- [x] **Cubierto donde se puede:** tres tests nuevos en `ScannerViewModelTest` sobre probar un motor,
  cerrar la prueba sin deshacer la elección, y que salir de la pantalla no deje la pantalla completa
  abierta. Lo que **no** cubre nadie es que se vea bien ni que la cámara se comporte dentro de un
  diálogo: eso sigue necesitando el teléfono de siempre
- [x] **Sin entrada en "Qué hay de nuevo", a propósito.** Los iconos se entienden solos y "Probar
  ahora" vive detrás del modo avanzado, que está apagado por defecto: estrenárselo a todo el mundo
  sería contarle a la mayoría una función que no va a ver

### Ronda 16 — el defecto que atrapaba al usuario, el arranque y lo legal ✅

- [x] **Cerrar la cámara la hacía aparecer otra vez, y llevaba ahí desde la Fase 2.** El Google Code
  Scanner encabeza la cadena en Android y abre su propia pantalla a pantalla completa; al cerrarla
  con el botón atrás emite `ScanError.Cancelled`, que es fatal, y `FallbackScannerEngine` hacía con
  él lo que hace con cualquier fallo fatal: **pasar al motor siguiente y volver a abrir la cámara**.
  El fallo de fondo era conceptual: `isFatal` respondía a una pregunta —"¿puede seguir esta
  sesión?"— y se le estaba pidiendo otra —"¿merece la pena probar otro motor?"—. Ahora son dos, y
  `allowsFallback` dice que cancelar no es una avería sino el usuario diciendo que no quiere seguir.
  Es exactamente la clase de defecto que la tabla de "qué cubre a los motores de cámara sin
  emulador" anuncia: la degradación se testea entera con motores falsos, pero **qué emite el motor
  de Google al cerrarse solo se ve en un teléfono**
- [x] **Pantalla de arranque de verdad, con `androidx.core:core-splashscreen`.** El comentario de
  `themes.xml` llevaba tiempo diciendo que el `windowBackground` cubría el hueco "con un rectángulo
  de color" y que cerrarlo del todo era "una decisión aparte". Se tomó: la marca del lanzador,
  centrada, con relevo al tema normal vía `postSplashScreenTheme` y salida animada. De paso cierra
  el caso que el color solo no podía —sistema en claro con la app forzada a oscuro— sujetándola
  hasta que la primera composición resuelve el tema
- [x] **`observePreferences()` pasa a ser `StateFlow`, y el primer fotograma deja de mentir.** La
  raíz se sembraba con `AppPreferences()` porque `collectAsStateWithLifecycle` exige un valor
  inicial; como el repositorio lee del almacén al construirse, ese valor era una copia falsa que
  duraba un fotograma —y era el fotograma claro de quien tiene el tema oscuro. No es filtrar la
  implementación: estas preferencias **siempre** tienen valor actual, y ahora el tipo lo dice
- [x] **Tres animaciones, y las que no se hicieron.** Se añaden la salida de la pantalla de arranque,
  la entrada de la pantalla completa de "Probar ahora" y la llegada de una lectura nueva a la hoja
  de resultados —que además ya no deja dos tarjetas marcadas como región viva a la vez—. **No** se
  animó el cambio entre los estados del visor: un `AnimatedContent` mantiene compuesto lo que sale,
  así que al degradar de motor habría dos superficies de cámara vivas peleándose por la sesión. Y
  **no** se añadió el latido del contorno de detección: una animación infinita sobre la pantalla de
  la cámara es un bucle de repintado permanente en el sitio donde la batería más importa
- [x] **Política de privacidad y términos de uso, escritos y enlazados** desde Ajustes → Acerca de,
  en los dos idiomas (`docs/legal/`). Las direcciones son cadenas del catálogo y no constantes,
  porque cada una apunta al documento en el idioma que el usuario tiene puesto. Los documentos son
  **comprobables**: cada afirmación se corresponde con algo verificable en el código —el manifiesto
  sin `INTERNET`, `allowBackup="false"`, la lista blanca de esquemas— y el único matiz real, que el
  escáner del sistema es un componente de Google, está dicho con nombre en lugar de escondido
- [x] **Ajustes estrena canal de efectos, y el KDoc que decía que no lo necesitaba se corrigió en vez
  de borrarse.** Era cierto mientras cada cambio se veía en la propia pantalla; abrir un documento
  fuera de la app es la primera acción de Ajustes cuyo fracaso es invisible
- [x] **La documentación se puso al día, y de paso salieron tres cosas que ya estaban mal.** El
  README daba el baseline profile por pendiente cuando lleva versionado desde la Ronda 7; hablaba de
  "las ocho alternativas" con nueve motores en el catálogo, y lo mismo el SDD en tres sitios; y
  `ENGINES.md` decía que el grafo de Android "sigue sin cobertura", que dejó de ser cierto al cerrar
  D18. Añadidos: [ADR-0015](adr/ADR-0015-probar-un-motor-es-un-dialogo.md), §9.12 del SDD (qué se
  anima y **qué no**), §7.5 (`isFatal` frente a `allowsFallback`), §9.5 (símbolo o palabra), §9.10
  (probar un motor), §12 (los documentos legales) y §13.6 (la pantalla de arranque)

### Ronda 17 — cerrar lo que se podía cerrar sin un teléfono ✅

Salió del repaso de la documentación de la Ronda 16: una lista de pendientes que no dependían ni de
iOS ni de la tienda, y que por tanto no tenían excusa.

- [x] **No había `LICENSE`, y los términos de uso decían que el código era público.** Sin archivo de
  licencia, por defecto son *todos los derechos reservados*: la afirmación que lee el usuario era
  falsa. Apache-2.0, y no MIT, por una razón concreta — **incluye concesión expresa de patentes** y
  la retira a quien demande por ellas. En un lector de códigos de barras, con patentes vivas sobre
  simbologías y sobre técnicas de decodificación, esa cláusula es la que hace falta; el silencio de
  MIT en ese tema no es neutral. Es además la licencia de todo lo que hay debajo: Kotlin, Compose,
  AndroidX, ZXing y ML Kit
- [x] **La sesión de escaneo ya no sobrevive a minimizar** (cierra la Ronda 12, ver allí)
- [x] **El cableado de accesibilidad tiene test** (cierra el paso tres de la Ronda 13, ver allí)
- [x] **Se retiró una afirmación falsa que se había escrito una ronda antes.** El §9.12 del SDD decía
  que ninguna animación respetaba *reducir movimiento*. Era una suposición: en Android ese ajuste es
  `ANIMATOR_DURATION_SCALE`, y tanto Compose —vía el `MotionDurationScale` que instala el
  `Recomposer`— como el `ViewPropertyAnimator` de la pantalla de arranque lo leen solos. **Escribir
  un `expect/actual` para eso habría duplicado la plataforma**, que es peor que no hacer nada. Queda
  el mecanismo escrito, que es lo único que permite volver a comprobarlo, y lo que sí falta dicho
  con precisión: Escritorio y Web no tienen equivalente
- [x] Deriva de KDoc: `ScannerViewModel` decía tener "catorce acciones de usuario" y tiene veintitrés

**Lo que se decidió no hacer, y por qué**: el paso dos de la Ronda 13 —fijar el suelo de cobertura—
sigue abierto a propósito. Lo bloquea un número que solo produce un run de CI, y adelantarse a él
habría sido justo el error que ese paso describe.

### Ronda 18 — barrido del parseo semántico ✅

Primer punto de las propuestas de mejora de la Ronda 16, y el que mejor relación tiene entre lo que
cuesta y lo que cubre.

- [x] **`ValueParsingFuzzTest`: cinco invariantes sobre entradas generadas.** El detalle está en
  §13.7 del SDD. Lo que lo justifica es una asimetría del producto: los tests de casos comprueban lo
  que alguien pensó, y aquí **el atacante escribe el valor entero** — no necesita engañar a nadie
  para que su cadena llegue al parser, le basta con imprimirla
- [x] **Los invariantes se ejecutaron antes de subirlos.** Aquí no compila nada, así que se portó el
  parser, `percentEncode`, la fábrica de acciones y la lista blanca a un modelo en Python y se
  corrieron **1,6 millones de casos** sobre la misma gramática, con cinco semillas. Cero
  violaciones. No sustituye a CI —el modelo puede diferir del original justo donde importa— pero
  evita subir un test que se sabía roto, que es lo único que se podía hacer sin compilador
- [x] **Nada nuevo que arreglar en el parser**, y conviene decirlo así en vez de callarlo: el barrido
  **no encontró un defecto**. Lo que aporta es que las cinco propiedades dejen de depender de que a
  alguien se le ocurra el caso — y que el día que alguien toque `parseUrl` o `percentEncode`, se
  entere

### Ronda 19 — el tamaño del binario deja de ser una opinión ✅

Segundo punto de las propuestas de la Ronda 16, y el que desbloquea una decisión que llevaba
aplazada desde ADR-0009.

- [x] **`tools/binary_size.py`, con su paso en `Verify`.** De las tres razones por las que ADR-0009
  aplaza Play Feature Delivery, la tercera —*"no hay ninguna medición del APK con la que decidir qué
  conviene partir"*— era la única que no dependía de tener cuenta de Play, y la más incómoda: **sin
  medir, RNF-06 no dice cuándo se incumple**. Ahora reparte el zip en cubos, y el reparto es lo que
  contesta la pregunta: las nativas van **por ABI**, que es el único trozo del APK atribuible a un
  motor concreto
- [x] **El umbral es un delta y no un nivel, y la distinción se escribió al lado.** Dos rondas antes
  se rechazó fijar un suelo de cobertura sin medir; aquí se fija una tolerancia de crecimiento del
  2 %. No es incoherencia: un suelo es absoluto e inventarlo antes de medir o rompe CI el primer día
  o no exige nada; una tolerancia es relativa a una línea base que se graba de la medición real, así
  que el primer día el delta es cero **por construcción**
- [x] **También falla cuando un cubo desaparece.** Que deje de empaquetarse una ABI no es "pesa
  menos": es que la app dejó de instalarse en esos dispositivos, y el total por sí solo lo aplaudiría
- [x] **Probado antes de subirlo, con APKs sintéticos.** Aquí no se puede construir el binario, así
  que se armaron zips con la forma real de uno —dex, tres ABI, assets, `resources.arsc`— y se
  recorrieron los cinco caminos: sin línea base, sin cambios, con crecimiento, con adelgazamiento y
  con una ABI desaparecida. Además se **extrajo el bloque `run:` del propio workflow** y se ejecutó
  tal cual, en vez de una aproximación a mano: es lo que destapó que un delta negativo se imprimía
  como `-921600 B`, porque el formateador nunca había visto un número por debajo de cero
- [ ] **Falta grabar la primera línea base.** No es trabajo pendiente sino un dato que solo produce
  CI: el paso imprime el JSON y lo sube como artefacto, y grabarlo es descargar ese archivo y
  commitearlo en `tools/binary-size.json`. Hasta entonces el paso mide, informa y **no puede fallar**

### Ronda 20 — la cámara de la que no se podía salir ✅

Reportado desde un dispositivo, que es donde aparecen los defectos que ningún CI ve. Y es **una
regresión de la Ronda 17**: la introduje dos rondas antes, arreglando otra cosa.

- [x] **El síntoma:** con el permiso concedido, la cámara se abre y no hay forma de salir. Ni la X,
  ni atrás, ni el gesto. Escanear un código correctamente tampoco ayudaba
- [x] **La causa: dos preguntas distintas atadas al mismo evento.** La Ronda 12 cambió el
  `DisposableEffect` de la pantalla por un `LifecycleStartEffect`, para que la sesión no siguiera
  viva con la app minimizada. Con eso, "la app volvió al primer plano" pasó a contar como "el
  usuario llegó a la pantalla" — y el arranque automático cuelga de lo segundo.

  El Google Code Scanner abre **su propia pantalla, en otro proceso**. Arrancar la sesión, por
  tanto, **manda WhyScan al fondo**. La secuencia se cierra sobre sí misma: el motor abre su
  pantalla → WhyScan al fondo → el usuario la cierra → WhyScan al primer plano → eso arranca la
  sesión → el motor abre su pantalla otra vez. Y de paso, irse al fondo cancelaba el `sessionJob`,
  así que la lectura que el usuario acababa de hacer moría en una corrutina cancelada
- [x] **El arreglo: que vuelvan a ser dos preguntas.** `ScreenShown`/`ScreenHidden` son navegación —
  llegar a la pantalla y salir de ella— y solo ellas arman el arranque automático.
  `Foregrounded`/`Backgrounded` son ciclo de vida: apagan la cámara al fondo y la devuelven al
  volver **si estaba corriendo**, sin re-armar nada. Si el usuario la había pausado a mano, no
  vuelve sola
- [x] **Y el motor con pantalla propia es un caso con nombre.** Irse al fondo **no** para la sesión
  cuando el motor activo declara `providesOwnUi`: ahí estar en segundo plano no significa que el
  usuario se haya ido, significa que el motor está trabajando porque lo mandamos nosotros. Se lee de
  la capacidad declarada y no de una lista de motores, así que el próximo con pantalla propia lo
  hereda sin tocar el ViewModel (RNF-07)
- [x] **Cinco tests en `ScannerLifecycleTest`**, y uno de ellos es el invariante que se violó: volver
  al primer plano **no arranca una sesión por su cuenta**. Hizo falta que el motor falso pudiera
  quedarse abierto (`keepsScanning`): uno que termina en cuanto emite deja sin sujeto la pregunta
  "¿estaba corriendo la sesión cuando la app se fue al fondo?", y toda aserción sobre pararla y
  reanudarla salía verde sin comprobar nada
- [ ] **Un caso raro que se deja abierto a propósito, y no es este defecto.** Si el sistema se lleva
  la pantalla del motor sin devolver resultado —el usuario la saca de recientes, o la mata una falta
  de memoria—, la sesión se queda esperando un resultado que no llegará. Se podría inferir al volver
  al primer plano, pero el resultado bueno también llega por ahí y el orden entre los dos no está
  garantizado: el arreglo se comería lecturas correctas. **Es anterior a todo esto** y sale de una
  sesión atascada saliendo de la pantalla

**Lo que este defecto dice del proyecto, que es lo que vale para la próxima:** el CI está verde y
seguiría verde con esto dentro. Lo destapó alguien usando la app. La tabla de "qué cubre a los
motores de cámara sin emulador" ya nombraba esta forma de agujero —lo que el motor de Google hace
**fuera del proceso** no lo ve nadie desde aquí— y esta es la segunda vez que muerde por el mismo
sitio: la primera fue la cadena de fallback reabriendo la cámara al cancelar, en la Ronda 16.

### Ronda 21 — la estructura para trabajar con agentes, escrita y comprobada ✅

Casi todo este repositorio lo ha escrito un agente, y hasta ahora las reglas de cómo hacerlo vivían
en un solo archivo con nombre de producto (`CLAUDE.md`), en castellano, y sin nada que las
comprobara. Esta ronda no cambia ni una línea de la app: cambia **cómo se hace el próximo cambio**.

- [x] **`AGENTS.md` es el contrato canónico**, en inglés, con las reglas completas: las
  inquebrantables, el mapa del repositorio, la matriz de qué se puede verificar aquí y qué no, el
  ciclo de trabajo, la tabla de dónde va cada cosa, el checklist de "terminado" y los frenos de
  mano. `CLAUDE.md` se queda como espejo en castellano y **no normativo**
  ([ADR-0016](adr/ADR-0016-agents-md-como-contrato-canonico.md)). El coste está dicho en el propio
  ADR: dos archivos que pueden separarse y ninguna comprobación que lea lo que dicen
- [x] **El harness deja de ser improvisado.** `.claude/` con permisos y hooks versionados, siete
  comandos (`/preflight`, `/pr-ready`, `/adr-new`, `/spec-propose`, `/spec-apply`, `/docs-sync`,
  `/engine-add`), tres subagentes con contexto propio y tres skills. La pieza que más vale es el
  hook: `tools/checks.py` corre **después de cada edición** de `.kt`, `.kts` o `.xml`, que en un
  entorno donde no compila nada es el único bucle de realimentación por debajo del minuto. Es la
  misma lección de la deuda D23 un nivel más arriba — lo que vive fuera del control de versiones se
  pierde entre sesiones
- [x] **OpenSpec para los cambios de comportamiento**
  ([ADR-0017](adr/ADR-0017-openspec-para-cambios-de-comportamiento.md)). Faltaba la cuarta pregunta:
  el SDD dice **cómo**, los ADR **por qué**, el ROADMAP **cuándo**, y nada decía **qué**, en
  requisitos comprobables. Cuatro capacidades escritas —el SPI, la selección, el historial y las
  garantías de privacidad, 23 requisitos con sus escenarios— y un cambio en curso: cubrir el
  `actual` de Android de `DatabaseBuilderFactory`, que sigue siendo lo único de la cadena de Room
  que no ejecuta ningún test. Lo que más cambia del proceso es que la pregunta de verificación —qué
  lo demuestra y ¿eso corre en cada PR?— se hace **antes** de implementar
- [x] **`docs/ai/`**: el modelo de trabajo con IA de punta a punta — el ciclo, el harness, cómo se
  planifica, una biblioteca de prompts con el motivo de cada uno, y la procedencia. Esa última
  incluye lo que el agente **no** hizo: los dos defectos más caros de este proyecto los encontró una
  persona poniendo la app en un teléfono
- [x] **Índice de ADR y plantilla**, con las diecisiete decisiones en una tabla. Los ADR se
  escribían bien y no se encontraban
- [x] **Lo que faltaba de un repositorio público**: contribuir en los dos idiomas, código de
  conducta, política de seguridad —donde un defecto de privacidad **es** un defecto de seguridad—,
  plantillas de issue y de PR, `CODEOWNERS`, y una guía de entrada en castellano e inglés
- [x] **Y, sobre todo, comprobado.** `tools/checks.py` valida ahora las cabeceras de los ADR y su
  paridad con el índice, que `AGENTS.md` y `CLAUDE.md` se enlacen, la forma de cada cambio de
  OpenSpec —incluido que todo `### Requirement:` tenga al menos un `#### Scenario:`— y los enlaces
  relativos entre documentos. Corre en cada PR, en el mismo paso de siempre. **Sin esto la ronda
  entera sería decoración**: una estructura que solo existe mientras alguien se acuerda de seguirla
  dura hasta el primer día con prisa

**Lo que esta ronda dice del proyecto:** el criterio que gobierna el código —lo que se puede
comprobar con un script no depende de que alguien se acuerde— se aplica ahora también a la
documentación y al proceso. Y una regla nueva que sale de aquí: **si algo se puede comprobar
mecánicamente, va a `tools/checks.py` y no a un archivo de instrucciones**. Una regla escrita depende
de que se lea; una comprobada, no.

### Ronda 22 — el sistema de diseño deja de ser el tema de una app 🚧

Sale de una pregunta que no era sobre WhyScan: **otras apps de la empresa quieren reutilizar esto**.
Al mirarlo de cerca, lo que había no se podía compartir, y el motivo no era la infraestructura que
faltaba sino algo anterior.

- [x] **`:core:designsystem` no es un sistema de diseño: es el tema de WhyScan.** De sus 930 líneas,
  `ScannerPalette` y `BrandMark` **son** la marca, y compartirlas no es compartir: es que todas las
  apps de la empresa se llamen WhyScan. `Theme`, `Radius` y `Typography` son mecánica genérica con
  valores de aquí. Solo tres archivos —`Contrast`, `AppLanguage` y `LocalSnackbarHostState`— no
  dependen de la marca, y ahora eso está **comprobado**, no supuesto
- [x] **La fuga que encontró la auditoría.** `ScanOverlay` pintaba el contorno de las detecciones con
  un `Color(0xFF34D399)` escrito a mano: un verde que no estaba en la paleta, que no medía nadie y
  que no cambiaba con el tema. Y estaba en el único sitio donde nadie lo iba a buscar — **encima del
  vídeo**, que es justo donde el contraste importa y donde el tema no llega. Ahora vive en
  `ScannerPalette.Overlay`, con el mismo valor: cambia dónde está y quién lo puede mirar, no cómo se
  ve
- [x] **Y con él, lo que ese caso enseña.** Lo que se pinta sobre la cámara no puede salir del tema,
  porque el fondo es la escena que el usuario esté enfocando y `colorScheme` no significa nada sobre
  una pared blanca. Tampoco se puede medir su contraste: no hay fondo conocido. Lo honesto es
  decirlo y no depender solo del color — la retícula lleva trazo y las detecciones, contorno cerrado
- [x] **Paridad de roles claro/oscuro, comprobada.** Es el defecto que **ya pasó dos veces** y está
  contado en el KDoc de `Theme.kt`: un rol que no se declara no falla, *sale del color de fábrica de
  Material*. Primero los `on*` —texto morado en un botón primario— y después los `*Container`, que
  pintan el `FilterChip` seleccionado, la `Card` y el `NavigationBar`. Ningún test de contraste lo
  caza, porque mide los pares que se le nombran y el rol olvidado no está en ninguna lista.
  Comparar los dos esquemas sí, y son veinte líneas de `check_design_system()`
- [x] **[ADR-0018](adr/ADR-0018-federar-la-base-y-no-la-marca.md): se federa la base, no la marca.**
  Nace `:core:foundation` con lo que sirve a cualquier app —el contraste como aritmética, el idioma
  por encima del sistema, declarar los treinta y cuatro roles a partir de una paleta cualquiera— y
  la marca se queda donde está. El corte, dicho de una vez: **lo reutilizable nunca fueron los
  colores, son las reglas**
- [x] **Las cinco garantías, escritas y secuenciadas** en `openspec/changes/federate-design-system/`:
  `explicitApi()`, validador de compatibilidad binaria, versionado semántico con la política escrita
  —en Compose cambiar un valor por defecto es compatible en fuente y rompe en binario—, documentación
  generada, y **un consumidor que no sea WhyScan**. La quinta es la única que detecta el acoplamiento
  de verdad, y es la que este repositorio no puede cerrar solo
- [ ] **Implementar la federación. Bloqueada por una decisión, no por trabajo:** el grupo Maven y el
  paquete no pueden llevar `whyscan`, y ese nombre lo pone el dueño del proyecto. Nada de esto se
  puede ejecutar aquí de todas formas — es configuración de Gradle y **aquí no compila nada**
- [x] **`docs/ai/state-of-the-art.md`**: dónde está de verdad este repositorio en trabajo con IA,
  escrito para incomodar y no para halagar. La conclusión corta es que la estructura es buena
  práctica actual bien ejecutada, no vanguardia, y que **el hueco más grande es que nada mide si el
  harness sirve**

**Lo que esta ronda dice del proyecto:** la pregunta "¿esto se puede compartir?" resultó ser una
auditoría de diseño disfrazada. No hizo falta infraestructura para encontrar el problema — hizo falta
mirar qué había dentro del módulo y separar lo que es de esta app de lo que es de cualquiera. La
infraestructura viene después, y sin ese corte habría publicado la marca.

### Ronda 23 — la auditoría, y lo que encontró de la propia documentación 🚧

Se auditó **todo** `docs/` y `openspec/` contra el código, requisito por requisito. De los 23
requisitos escritos en la Ronda 21, **nueve eran falsos**. Eso es exactamente el defecto contra el
que avisa el ADR-0017: una fuente de verdad que miente es peor que no tenerla.

- [x] **La garantía más citada del repositorio no existía.** Catorce archivos —`AGENTS.md` entre
  ellos, como regla de cabecera— decían que "un test verifica que `ENGINES.md` y el catálogo no
  divergen". **No lo verificaba nadie**, y no podía: comparar contra un Markdown exige leer del
  disco y un `commonTest` de KMP no tiene sistema de archivos. La respuesta no fue borrar la frase
  de catorce sitios sino **hacerla cierta**: `check_engine_catalog()` compara identificador, fase y
  plataformas, y corre antes que Gradle en cada PR. Probado rompiendo la tabla a propósito
- [ ] **`MANUAL_INPUT` no cierra la cadena, y cinco documentos dicen que sí.** Declara solo
  `ScanSource.ManualInput`, así que `satisfies()` lo descarta ante cualquier petición de cámara: en
  Escritorio —donde no hay captura de webcam— la cadena queda **vacía** y la sesión emite
  `EngineUnavailable`. Es decir, el estado "no se puede escanear" que G4 promete que no existe. Hay
  un test que fija ese comportamiento. Está en `openspec/changes/close-the-chain-with-manual-entry/`
  y **necesita una decisión**: arreglar el código o retirar G4 de los cinco sitios
- [x] **La suite de contrato la heredan dos motores de ocho, y es una decisión.** Los de cámara no
  pueden: construirlos exige un emulador (D6). Seis documentos decían "no es opcional" sin la
  salvedad. Y la suite **no verifica las cuatro garantías**: la tercera —que cancelar libere la
  cámara— no se puede observar sin hardware
- [x] **El SDD §13.3 prometía tres gates de CI que no existen**: reglas de arquitectura verificadas,
  SonarCloud sin regresión, y `allWarningsAsErrors`. Ninguno. El de Sonar se ha borrado —no hay ni
  rastro en el repositorio—, los otros dos quedan dichos como lo que son: convención sin automatizar
  y la deuda D19
- [x] **§7.5 y §8 del SDD llevaban desde la Fase 4 desactualizadas**: daban Desktop a ZXing-cpp, iOS
  a ML Kit OCR, y prometían un fallback a ZXing-cpp/Wasm que se retiró. Siete filas para nueve
  motores. `ENGINES.md` estaba bien; el resumen había dejado de resumir
- [x] **La matriz de formatos mentía sobre el OCR en los dos sentidos**: Codabar salía como no
  soportado estando declarado, y cuatro simbologías salían como parciales cuando
  `OcrCodeInterpreter` **no las puede producir nunca** — infiere por longitud y dígito de control, y
  solo emite EAN-8, UPC-A, EAN-13 e ITF. Corregido con una nota que dice que la columna es lo
  *declarado*, no lo *producible*
- [x] **Y un error mío de la Ronda 21.** La propuesta `cover-android-database-builder` planteaba un
  test de Robolectric que abriera la base de Android — y el KDoc de `AndroidKoinGraphTest`, en el
  mismo módulo, ya decía que `sqlite-bundled` trae binarios nativos que Robolectric no puede cargar.
  Reescrita: cubre las dos decisiones que sí se pueden comprobar —el `applicationContext` y la ruta—
  y **no cierra la casilla**, la estrecha. La lección quedó en `openspec/AGENTS.md`: contestar "qué
  lo demuestra y ¿corre en cada PR?" no basta, falta **"¿puede ese test existir?"**
- [x] Trece correcciones mecánicas más: contadores desfasados (ocho motores donde hay nueve, quince
  ADR donde hay dieciocho, doscientas mil palabras del SDD donde hay veinticinco mil), dos filas de
  la tabla de deuda que se contradecían entre sí, y `engine as? CameraControlEngine` en el §7.1
  cuando el cast directo devuelve `null` a través de los decoradores

**Lo que esta ronda dice del proyecto, y es incómodo:** la estructura de la Ronda 21 se escribió en
un día y **nueve de sus veintitrés requisitos eran falsos**. No por descuido en la redacción, sino
porque se escribieron leyendo el código una sola vez y sin contrastarlos. La auditoría es lo que
convierte esa estructura en algo utilizable, y tiene que ser rutina y no un acto único — está en
`docs/ai/state-of-the-art.md` §3. La otra mitad de la lección es más simple: **la comprobación
mecánica gana a la frase escrita siempre**. Catorce archivos afirmaban una garantía durante dos años
y no costaba nada comprobarla.

### Ronda 24 — tapar el agujero de privacidad, y que el harness deje de poder mentir ✅

Todo lo que la Ronda 23 dejó accionable y no dependía de una decisión ajena.

- [x] **El manifiesto fusionado, que era el hueco más grande de la garantía de privacidad.**
  `check_privacy_guarantee()` mira el manifiesto **fuente**, que es lo único que existe antes de
  Gradle. Una dependencia que declare `INTERNET` en el suyo entra en el APK al fusionar, el
  manifiesto de este repositorio sigue limpio, y la promesa que la app le hace al usuario en Ajustes
  pasa a ser falsa **sin que cambie ni una línea de aquí**. Es el mismo error de forma que
  `allowBackup` y que D18: auditar lo que hace el código propio y no lo que el sistema hace con él.
  Lo cierra `tools/merged_manifest.py` en el job de Android, después de `assembleDebug`
- [x] **Y se niega a pasar si no encuentra el archivo.** La ruta la elige AGP y cambia entre
  versiones; una comprobación que no encuentra su objetivo y sale con cero da por revisado lo que
  nadie revisó. Ya pasó aquí con detekt, que analizaba **cero archivos** y salía en verde
- [x] **Los permisos que no rompen la garantía se imprimen, no fallan.** La app necesita `CAMERA` y
  las dependencias de Google traen los suyos: convertir cualquier permiso nuevo en error rompería el
  build por motivos legítimos, y una comprobación que molesta acaba desactivada. Van al resumen del
  run, que es donde una revisión los ve sin excavar
- [x] **`check_harness()`: que el harness no pueda mentir.** Cada agente, skill y comando con su
  cabecera; el `name` de una skill coincidiendo con su carpeta —si no, no carga y no lo dice nadie—;
  y **cada `XxxTest` y cada `check_xxx()` citado en un archivo que describe la verdad de hoy tiene
  que existir**. Es exactamente lo que habría cazado en 2024 la garantía imaginaria de la Ronda 23.
  Las propuestas de `openspec/changes/` quedan fuera a propósito: nombrar lo que todavía no existe
  es su trabajo
- [x] **Y esto no son evaluaciones, y hay que decirlo.** No mide si el harness *sirve* — mide que no
  mienta, que es una precondición y no lo mismo. Medirlo de verdad exige ejecutar un modelo en CI,
  y eso es una clave de API y un presupuesto, no una tarea. Sigue abierto como hueco nº1 en
  `docs/ai/state-of-the-art.md`
- [x] **La regla de OpenSpec tenía un agujero, y lo encontró la propia comprobación.** Exigía delta
  a *todo* cambio, y uno de herramienta de build no tiene comportamiento observable que
  especificar. Obligarle a inventarse una capacidad habría metido en `specs/` requisitos que el
  usuario no puede observar. Ahora se exime declarando `**Capability:** none` **y diciendo por qué**
  — y declarar las dos cosas a la vez falla, para que la exención no sea una puerta abierta
- [x] **Métricas del compilador de Compose: escritas como propuesta y no empujadas.** Van en
  `build-logic`, y si el accesor `composeCompiler` no se genera para un script precompilado no cae
  solo Android: cae la compilación de `build-logic` y con ella los cuatro jobs. Probablemente
  funcione — `libs.gradlePlugin.composeCompiler` está en el classpath como `implementation`— y
  "probablemente" no es un criterio para empujar configuración de build que aquí no se puede
  ejecutar

**Lo que esta ronda dice del proyecto:** las tres cosas que se cerraron son la misma idea aplicada
tres veces. La garantía de privacidad se comprobaba donde era cómodo y no donde podía romperse; el
harness afirmaba cosas que nadie contrastaba; y la regla de OpenSpec pedía algo que en un caso no
tenía sentido. **Lo que se puede comprobar con un script, se comprueba; lo que no se puede
verificar, no se empuja.** La segunda mitad es la que costó esta semana aprender.

### Ronda 25 — la app pasa a publicarse bajo Faro ✅

- [x] **`applicationId = "ar.net.faro.whyscan"`** ([ADR-0019](adr/ADR-0019-el-applicationid-identifica-a-quien-publica.md)).
  Es la última ocasión en que esto costaba una línea: el `applicationId` es la URL de la ficha y la
  clave con la que el sistema reconoce una actualización, y **después de la primera subida no se
  puede cambiar**. Todavía no hay ninguna
- [x] **`ar.net.faro` y no `com.faro`**, y no es cosmético: el dominio es `faro.net.ar`, así que su
  orden inverso es ese. `com.faro` sería reclamar un dominio ajeno, y el día que haya que publicar
  la base federada en Maven Central —que verifica la propiedad del dominio contra el `groupId`— no
  se sostendría
- [x] **Los paquetes de Kotlin no se tocan.** Siguen siendo `com.whyscan.*`. La regla que queda: el
  espacio de nombres de la tienda identifica a **quien publica**, el del código identifica al
  **producto**. Son dos preguntas distintas y cambian en momentos distintos — el editor acaba de
  cambiar y el producto no se enteró
- [x] **El único acoplamiento del repositorio, buscado antes de tocar nada.**
  `BaselineProfileGenerator` llevaba el identificador escrito a mano, porque instala, lanza y
  concede permisos con `pm grant` usando el `applicationId` y no el paquete del código. Cambiarlo
  allí y no aquí habría dejado la generación del perfil fallando al buscar una pantalla, que es un
  síntoma que no se parece en nada a la causa. No hay `authorities`, ni `FileProvider`, ni enlaces
  profundos, ni reglas de R8 que lo nombren
- [x] Con esto, el grupo Maven de `:core:foundation` queda fijado y **la federación deja de estar
  bloqueada por un nombre**. Sigue bloqueada por el trabajo, que es otra cosa y mejor
- [ ] **Comprobar en Play Console que `ar.net.faro.whyscan` está libre.** Sin red aquí, no se pudo
- [ ] **El nombre visible de la ficha** —`WhyScan` o `Faro WhyScan`— sigue abierto, y a diferencia
  del `applicationId` esa sí se puede cambiar después de publicar

**Lo que esta ronda dice del proyecto:** el comentario que había junto al `applicationId` defendía
que coincidiera con los paquetes —"así no hay dos nombres que mantener sincronizados ni ninguno que
explicar"— y era cierto mientras no hubo editor. La decisión correcta hoy es justo la contraria, y el
comentario viejo no estaba equivocado: estaba **fechado**. Por eso se reescribe con su motivo nuevo
en lugar de borrarse.

### Antes de la ficha de Play, esto va primero

Lo de abajo es trámite de tienda. Lo de esta lista no. Se hizo el repaso a propósito antes de tocar
nada de Play, y salió algo que no estaba en ninguna lista:

- [x] **`allowBackup` estaba en `true`, y la app le dice al usuario lo contrario.** Ajustes afirma
  que
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
- [ ] **Objetivos táctiles y `enableEdgeToEdge`** (RNF-05, pendiente desde la Fase 5). Es la clase
  de
  cosa que no rompe, se ve mal, y se ve mal precisamente en la primera pantalla
- [x] **Generar el baseline profile.** Hecho: el perfil está versionado y `baseline-profile.yml` lo
  regenera y lo commitea solo. Es lo que separa "la app arranca" de "la app arranca rápido la
  primera vez" — con la salvedad de siempre, que **cuánto** más rápido solo lo dice un dispositivo
- [x] **Navegación: no se podía salir de la cámara.** Reportado desde un teléfono: conceder el
  permiso abre la cámara —bien—, pero desde ahí ni la X, ni el botón atrás, ni el gesto sacaban de
  la pantalla, y daba igual que la lectura hubiera funcionado. **Es una regresión de la Ronda 17**
  y está contada en la Ronda 20

Solo después tiene sentido pelearse con la ficha.

### Pendiente para publicar

**Necesita un dispositivo.** Todo lo de este bloque está bloqueado por lo mismo, y por eso vive aquí
y no en una ronda: arrastrarlo de ronda en ronda lo haría parecer trabajo que nadie hace, cuando lo
que falta es un teléfono.

- [ ] Objetivos táctiles y `enableEdgeToEdge` **mirados con los ojos** (pendiente desde la Fase 5)
- [x] Quitar el `KoinContext` de `App.kt` (D20). **Estaba hecho desde la Ronda 6 y esta casilla se
  quedó atrás**, que es justo lo que pasa cuando lo mismo se apunta en dos sitios. Lo que parecía
  imposible de comprobar sin abrir la app resultó no serlo: `koinInject` no es UI, así que
  `ComposeKoinContextTest` monta una `Composition` con el runtime de Compose y compara la instancia
  con la del grafo
- [ ] Verificar el selector de idioma en iOS (D21)
- [ ] Medir el arranque en frío, que es lo que Play reporta en Vitals desde el primer día
- [ ] El `actual` de Android de `DatabaseBuilderFactory`, que es lo único de la cadena de Room que
  no
  ejecuta ningún test

**Trámite de la ficha**

- [ ] Comprobar en Play Console que `ar.net.faro.whyscan` está libre y que "WhyScan" no colisiona con
  una
  ficha existente. **Sin red en el entorno de desarrollo, esto no se pudo verificar aquí**
- [ ] Capturas, gráfico de cabecera 1024×500 y textos de la ficha, en los dos idiomas
- [ ] Política de privacidad **publicada** y formulario de seguridad de datos. El documento ya está
  escrito y enlazado desde Ajustes (`docs/legal/`, Ronda 16); lo que falta es de tienda: pegar la
  dirección en Play Console y rellenar el formulario. Sin `INTERNET`, la respuesta a casi todo es
  "no se recoge nada", con la única salvedad que el propio documento nombra — el escáner del
  sistema es un componente de Google. El **correo de contacto ya está decidido**:
  <david@faro.net.ar>, y figura en los cuatro documentos legales, en `SECURITY.md`, en el código de
  conducta y en las plantillas de issue
- [ ] Firma de release y `bundle` en vez de APK

**Criterio de salida:** alguien que no sabe qué es un motor de escaneo abre la app, lee un código y
lo comparte, sin ver ni una vez la palabra "motor".

---

## Qué cubre a los motores de cámara sin emulador

La decisión de no tener tests instrumentados deja un hueco real y conviene decir exactamente cuál es
y qué lo compensa:

| Se comprueba sin dispositivo                                                                                                                                                                                                                                                              | Sigue sin comprobarse                                                                                                                                                         |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Que el descriptor de los nueve motores es coherente: IDs únicos, fases válidas, sin prometer control de cámara con UI propia (`ScannerEngineCatalogTest`)                                                                                                                                  | Que el motor **lea** un código real                                                                                                                                           |
| Que la selección, el fallback, los límites de petición y el plazo se comportan según el contrato, incluida la cadena completa que llega al ViewModel                                                                                                                                      | Que la cámara arranque, y que se libere al cancelar                                                                                                                           |
| Que lo declarado tenga quien lo cumpla, en todo lo instanciable sin `Context`                                                                                                                                                                                                             | Lo mismo en los motores de Android e iOS, que necesitan `Context` o `AVCaptureSession`                                                                                        |
| Que ZXing (Java) **lea de verdad** un QR y un EAN-13 desde píxeles, filtre por formato y distinga "no hay código" de "no es una imagen"                                                                                                                                                   | Lo mismo en los motores que necesitan cámara                                                                                                                                  |
| Que el **grafo de Koin resuelva** de verdad: `KoinGraphTest` arranca los módulos comunes más el `platformModule` de escritorio y pide cada tipo que la raíz de la app consume                                                                                                             | Lo mismo para el `platformModule` de **Android**, que necesita `androidUnitTest` en `:composeApp`                                                                             |
| Que el proyecto **compile** para Android, Escritorio y Web, incluida la build de release con R8                                                                                                                                                                                           | Que la app **arranque** y lea un código: sigue haciendo falta un dispositivo                                                                                                  |
| Que `koinInject` resuelva **componiendo de verdad**, sin envoltorio y sin UI: `ComposeKoinContextTest` monta una `Composition` con el runtime de Compose y compara la instancia con la del grafo                                                                                          | Que la pantalla se **vea** bien: el runtime compone, no dibuja                                                                                                                |
| Que **`App()` se monte entera**: `AppCompositionTest` compone la raíz con el grafo real —tema, idioma, `CompositionLocal`, `koinViewModel`, los efectos de arranque de cada pantalla— y cambia de destino. Es lo más cerca que este proyecto ha estado de "la app arranca" sin arrancarla | Que se **vea** bien, y que el arranque de **Android** funcione: `MainActivity`, `enableEdgeToEdge` y el préstamo de los `ActivityResultLauncher` siguen sin quien los ejecute |

El riesgo que queda es el de siempre en este tipo de app: el código de cámara solo se prueba
usándola. Lo que sí evita el diseño es que un fallo ahí se lleve por delante al resto — el SPI
mantiene la lógica de selección, degradación y presentación fuera de los motores, y esa parte sí
está cubierta.

---

## D19, los accesores `compose.*` de los scripts de build — cerrados

Cada `implementation(compose.algo)` emitía esto, y eran **27 de los 44 avisos** del inventario:

```
w: composeApp/build.gradle.kts:57:36: 'runtime: String' is deprecated. Specify dependency directly
```

Ahora hay coordenadas explícitas en el catálogo de versiones y **26 sustituciones** en doce scripts.
Lo que no se tocó, porque no estaba deprecado ni es un `String`: `compose.desktop.currentOs`, el
bloque `compose.resources { }` y el bloque `compose.desktop { }`.

**Lo que bloqueaba esto era creer que había que adivinar las coordenadas, y no había que adivinar
nada.** El razonamiento anterior decía —con buen criterio— que acertarlas sin poder compilar
rompería el build entero. La salida no era compilar: era **leerlas**. Están dentro de
`org.jetbrains.compose:compose-gradle-plugin:1.11.1`, en la clase `ComposePlugin$Dependencies`, que
es literalmente lo que los accesores devolvían; y el jar se descarga de Maven Central, que **sí** se
alcanza desde el entorno de desarrollo aunque el maven de Google no. Media hora de mirar en vez de
una tanda de CI a ciegas.

Menos mal que se miró, porque **dos de las ocho no siguen la versión del plugin** y ninguna de las
dos se habría acertado:

- **`material3` va en 1.9.0, no en 1.11.1.** No existe un `org.jetbrains.compose.material3:material3`
  1.11.1 publicado: el artefacto salta de `1.11.0-alpha07` a `1.12.0-alpha01`. Escribir la versión
  del plugin ahí habría roto la resolución, que es exactamente el desastre que se temía.
- **`material-icons-extended` está clavado en 1.7.3** y no recibe más actualizaciones. Eso lo decía
  ya el segundo aviso que D19 dejó vivo; ahora está escrito junto a la coordenada, con la salida que
  propone el propio plugin —migrar a Material Symbols como recursos vectoriales—.

De propina se fue un `@file:OptIn(ExperimentalComposeLibrary::class)` de `:composeApp`: existía solo
para poder escribir `compose.uiTest`, y lo experimental era el accesor, no el artefacto.

Dos cosas que conviene no perder:

- **Nunca fueron errores, aunque Gradle los contara como tales.** El compilador los emite como `w:`;
  cuando *además* hay un error de verdad en el mismo script, el informe de "Script compilation
  errors" los lista todos juntos y suma. Eso es lo que hizo que un fallo por otra cosa pareciera
  ocho.
- **Y el convention plugin no los usaba.** Este documento afirmaba que `whyscan.kmp.compose` también
  los tenía, y no: son once líneas que solo aplican tres plugins. Era una suposición razonable sobre
  un archivo que nadie volvió a abrir, escrita con el mismo tono que los hechos comprobados.

**Lo que queda de D19 es la postura, no la limpieza.** Con estos 27 fuera, el inventario baja a 17
avisos. Activar `allWarningsAsErrors` vuelve a ser una decisión discutible en vez de imposible, y es
lo único que sigue abierto.

---

## Deuda técnica aceptada en la Fase 1

Registrada de forma explícita para que no se olvide:

| #       | Deuda                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | Se salda en                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ~~D1~~  | ~~Sin convention plugins: cada módulo repite su configuración KMP~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | **Saldada**: `build-logic/` con `whyscan.kmp.library`, `.kmp.compose` y `.android.application`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| ~~D2~~  | ~~Preferencias en memoria, no persistidas~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | **Saldada**: `multiplatform-settings` en las cuatro plataformas                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| ~~D3~~  | ~~Historial en memoria~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | **Saldada**: Room KMP en Android, iOS y Desktop. En Web, JSON en el almacén del navegador porque Room no tiene target wasmJs — ver D9, que es la fila que cuenta cómo se cerró. Esta decía "sigue en memoria" y llevaba desde entonces contradiciendo a la otra en la misma tabla                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| ~~D4~~  | ~~Navegación propia sin deep links ni restauración de estado~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | **Saldada**: hecha la revisión de ADR-0005, el umbral (seis destinos o deep links) no se alcanza y la navegación propia se mantiene. Lo que sí era un defecto era la restauración: al recrearse la Activity —muerte del proceso, cambio de idioma o de tamaño de letra; no al rotar, que el manifiesto ya cubre— se volvía al escáner. `Navigator` guarda y restaura el backstack por ids estables y `MainActivity` lo pasa por `onSaveInstanceState`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| ~~D5~~  | ~~Strings hardcodeados en la UI~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | **Saldada**: `composeResources` por módulo. Los ViewModels emiten mensajes semánticos (`ScannerMessage`, `HistoryMessage`) y `ResultAction` dejó de traer etiqueta: el dominio dice qué acción, la UI cómo se llama                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| ~~D6~~  | ~~La suite de contrato no se ejecuta contra motores de cámara reales~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | **Cerrada como no-objetivo**: no habrá emulador en CI, así que ningún test puede exigir dispositivo. Lo cubre lo que sí corre sin él — ver más abajo                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| ~~D8~~  | ~~El zoom se declara como capacidad pero no hay control en la UI~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | **Saldada**: slider derivado de `canControlZoom`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| ~~D10~~ | ~~RF-07 sin UI ni selector de archivos~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | **Saldada**: `ImagePicker` en las cuatro plataformas y `DecodeImageUseCase` recorriendo la cadena de motores. Un motor bloqueado por el permiso de cámara sigue sirviendo para leer un archivo                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| ~~D12~~ | ~~RF-13 (copiar, compartir, abrir enlace) sin implementar~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | **Saldada**: `PlatformActions` en `:core:platform` con implementación en las cuatro plataformas. En escritorio no hay hoja de compartir y el botón no se ofrece (`canShare`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| ~~D11~~ | ~~La comparación necesita dos motores de cámara y solo Android los tenía~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | **Saldada**: con ZXing-cpp, iOS tiene dos (Vision y ZXing-cpp) y Android cuatro                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| ~~D9~~  | ~~El historial de Web es de sesión~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | **Saldada**, aunque no con IndexedDB: se guarda como JSON en el almacén de la plataforma, con los mismos campos que la tabla de Room. Unas cientos de filas de texto no justifican una base de datos, y esto corre en `commonTest` mientras que IndexedDB serían cien líneas de interop que nadie puede probar                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| ~~D7~~  | ~~`:androidApp` sin ProGuard/R8 configurado para release~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | **Saldada**: `minify` y `shrinkResources` activados, con reglas cortas y justificadas, y `assembleRelease` en CI para que R8 se ejecute de verdad                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| ~~D14~~ | ~~El motor de Web escanea pero no muestra visor~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | **Saldada**: el `<video>` se coloca sobre el canvas desde `onGloballyPositioned`. A cambio tapa el overlay, declarado con `occludesOverlay`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| ~~D15~~ | ~~El texto que se copia de un WiFi lo compone el dominio~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | **Saldada**: `shareableContent()` devuelve la estructura y la pantalla la redacta con sus recursos. La acción del ViewModel lleva el texto ya hecho                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| ~~D13~~ | ~~Desktop y Web se quedan sin decodificador: zxing-cpp no publica artefacto JVM ni wasmJs~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | **Saldada en Desktop**: `:engines:zxing-java` sobre `com.google.zxing:core`, en el catálogo **como motor propio** y no con el nombre de zxing-cpp — son proyectos distintos y confundirlos falsearía la comparación. Solo imagen estática: el decodificador está, la captura de webcam no. **Web se queda como está**: no hay artefacto wasmJs y su respaldo sigue siendo la entrada manual                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| ~~D16~~ | ~~`ScannerViewModel` tiene doce colaboradores y veinte funciones~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | **Saldada**: siete dependencias. Los ajustes en `ScanSettings`, la sesión y el guardado en `ScanSessions`, las acciones sobre el resultado en `ResultActionRunner`. Los tres casos de uso de preferencias y el del catálogo se **borraron** en vez de envolverse —delegaban al repositorio sin añadir nada—, y la única regla que había se conservó donde se puede probar. Quince tests nuevos que antes exigían levantar el ViewModel entero. Quedan dos supresiones, ninguna global: `TooManyFunctions` en la clase (veintitrés acciones de usuario, veintitrés funciones) y `CyclomaticComplexMethod` en `onAction`, que es una tabla de despacho sobre un `sealed interface`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| ~~D17~~ | ~~`IosPlatformActions.openUrl` usa `UIApplication.openURL:`, que Apple depreció en iOS 10~~                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | **Saldada**: los dos sitios que lo usaban —ese y `IosPermissionController.openAppSettings`— pasan a `openURL:options:completionHandler:`. No se espera al `completionHandler` a propósito: `openUrl` responde si la app **puede** abrir la URL, cosa que ya contesta `canOpenURL`, y suspender hasta que el sistema termine de cambiar de app no le añade nada a quien llama. Verificado enlazando el framework en el workflow `iOS (manual)`, que es toda la comprobación que cabe sin un iPhone                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| ~~D18~~ | ~~**Nada comprueba que el grafo de Koin resuelva.**~~ El defecto que la abrió lo demostró el primer arranque real en un dispositivo: `platformModule` registraba el executor de análisis como `ExecutorService` mientras los tres motores de cámara lo piden como `Executor`, y Koin resuelve por igualdad exacta de tipo. La app moría al componer la primera pantalla con `NoDefinitionFoundException`. **El compilador no puede verlo** —los `get()` son genéricos que se resuelven en ejecución— y el CI tampoco: compila, pasa lint, pasa R8 y publica un APK que revienta al abrirse. Es el mismo agujero que el criterio de salida de la Fase 1, visto desde el otro lado. **Saldada a medias, y la mitad que falta está dicha.** `KoinGraphTest` (`composeApp/src/desktopTest`) arranca el grafo real y **resuelve** cada tipo que la raíz de la app consume, agrupado por el ViewModel que lo pide. No usa `verify()` sino resolución de verdad, que es más fuerte: instancia en vez de reflexionar. Cubre `dataModule`, `domainModule` y los tres módulos de feature —comunes a las cuatro plataformas— más el `platformModule` de escritorio. En su primera ejecución destapó el defecto del driver de Room que no se aplicaba (SDD §11) | **Saldada del todo.** `AndroidKoinGraphTest` monta el `platformModule` de Android con Robolectric —un `Context` de verdad en la JVM, sin emulador— y lo ejecuta `:composeApp:testDebugUnitTest` en el job de checks. Incluye el test del `Executor` que la abrió                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| ~~D24~~ | ~~**Declarar una versión de dependencia no la impone.**~~ Se fijó `kotlinx-datetime` en 0.6.2 —donde `kotlinx.datetime.Instant` es una clase de verdad— y Gradle resolvió una superior porque otro punto del grafo la pedía. El resultado fue lo peor de los dos mundos: **compilaba y reventaba al ejecutar** con `ClassNotFoundException`, porque en 0.7+ ese nombre sobrevive como typealias y un typealias no existe en el bytecode                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             | **Saldada**: `tools/check_resolved_versions.py` cruza el informe de dependencias de Gradle con el catálogo. **Falla** cuando lo sustituido es una versión **nuestra** —alguien la escribió y el grafo la ignoró, que es exactamente este caso— y solo **informa** de los ascensos entre terceros, que son funcionamiento normal y convertirlos en error dejaría un build que falla por decisiones ajenas. Falla también **cerrado**: si el informe llega vacío devuelve error en vez de aprobar por no haber leído nada. Comprobado con el caso real antes de subirlo — y **en su primera ejecución encontró uno**: el catálogo declaraba `androidx.core:core-ktx` en 1.15.0 y el grafo resolvía 1.18.0, así que lo escrito era una versión que no usaba nadie                                                                                                                                                                                                                                                                                                                                                                                                                |
| ~~D23~~ | ~~**Las comprobaciones locales van por detrás de detekt, y se descubre en CI.**~~ Sin poder compilar aquí, cada ronda se verificaba con chequeos escritos a mano, y cada uno se añadió **después** de que CI rechazara ese caso concreto: la red crecía a golpes. Peor: vivían en una carpeta temporal, así que se perdían entre sesiones y había que reescribirlas — cosa que pasó de verdad                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | **Saldada**: `tools/checks.py`, en el repositorio y ejecutado por CI como **primer paso** del job de checks, antes incluso de instalar Java. Que corra en CI es lo que impide que se desincronice de lo que detekt exige sin que nadie se entere. Cubre lo que detekt ya mira —longitud de línea, orden de imports— más lo que **no comprueba nadie**: paridad de los catálogos entre idiomas, claves huérfanas, `Res.string.X` sin importar y `package` que no sigue a su carpeta. No intenta reimplementar las reglas que necesitan árbol sintáctico, `MagicNumber` incluida: una aproximación con expresiones regulares acabaría fallando donde detekt aprueba, que es la peor forma de tener una comprobación                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| ~~D19~~ | ~~**Los avisos del compilador no los lee nadie, y uno de ellos era un defecto de producción.**~~ `This extension is shadowed by a member` llevaba apareciendo en cada build desde que existe `:core:database`, y señalaba el defecto del driver de Room que reventaba escritorio e iOS (SDD §11): un aviso correcto, visible en cada compilación y leído por nadie durante meses                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | **Postura decidida, con el inventario delante y no de memoria.** Del log de CI salen **44 avisos en 12 mensajes distintos**, y esa cuenta era la condición que faltaba para poder decidir. Se quitan los que son nuestros: `dayOfMonth` → `day` (kotlinx-datetime 0.8), `@OptIn(ExperimentalGetImage::class)` → `@ExperimentalGetImage` —el compilador decía que ese `@OptIn` **no hacía nada**, porque la anotación de CameraX no está marcada con `@RequiresOptIn`— y `-Xexpect-actual-classes`, que es la bandera que el propio mensaje propone y que se lleva 10 de los 44. **No se activa `allWarningsAsErrors`, y el motivo es concreto:** 27 de los 44 son la deprecación de los accesores `compose.ui`, `compose.runtime` y compañía en los ficheros de build, que pide declarar las coordenadas directamente. Es mecánico pero toca los diecinueve módulos y exige acertar coordenadas que **aquí no se pueden comprobar sin compilar**; adivinarlas rompería el build entero. Queda como el siguiente paso, con el trabajo ya acotado. Los otros dos que sobreviven tienen dueño: `KoinContext` es D20 y `materialIconsExtended` avisa de que está clavado en 1.7.3 |
| ~~D20~~ | ~~**El aviso `KoinContext is not needed anymore` en `App.kt`.** Quitarlo cambia por dónde resuelven `koinInject` y `koinViewModel`, y eso no se puede comprobar sin ejecutar la app.~~ **Saldada, y la premisa era falsa.** Sí se puede comprobar sin ejecutar la app, porque `koinInject` no es UI: es una función `@Composable` que lee un `CompositionLocal` y llama a `remember`, y eso lo resuelve el **runtime** de Compose, que es Kotlin puro y no sabe nada de pantallas. `ComposeKoinContextTest` monta una `Composition` con un `Applier` que no aplica nada —no hay árbol de nodos que construir, lo que interesa ocurre *durante* la composición— y comprueba que `koinInject` devuelve la misma instancia que `koin.get()`, para un tipo de los módulos comunes y otro que depende del `platformModule`. Leyendo koin-compose se ve por qué: `LocalKoinScopeContext` declara como valor por defecto `KoinPlatform.getKoin().scopeRegistry.rootScope`, que es exactamente lo que `KoinContext` proveía a mano — el envoltorio era una identidad. Pero eso es leer la librería, y leyendo la librería también estaba bien el `build()` de Room que no se llamaba nunca                                                                  | **Cerrada.** Con ella se va uno de los avisos que D19 señala como ruido                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| D21     | **El selector de idioma en iOS está sin verificar.** El actual escribe `AppleLanguages` en `NSUserDefaults`, que es el mecanismo estándar; si Compose lee `preferredLanguages` el cambio es inmediato, si lee `currentLocale` no lo será hasta reabrir. Ver ADR-0011                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | Cuando haya un iPhone. Es lo primero que hay que mirar de la UI de iOS                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ~~D22~~ | ~~**Nada ejecuta la migración de la base de datos.**~~ Room genera `@AutoMigration` y la valida en compilación contra los esquemas exportados, que cubre que el SQL sea correcto — pero que una base v1 con historial dentro se abra con código v2 y siga teniendo el historial no lo comprueba nadie. Es justo el fallo que la migración existe para evitar, y es un test JVM con un archivo de prueba: no necesita dispositivo                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | **Saldada**: `MigrationTest` levanta una base v1 con el `createSql` literal del `1.json`, le escribe filas y comprueba que siguen ahí tras abrirla con el código v2. No mira el esquema a propósito — un esquema correcto es compatible con haber borrado la tabla, que es justo el fallo que se quería evitar                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
