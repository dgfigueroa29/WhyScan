# Catálogo de motores de escaneo

Fuente de verdad del catálogo. El registro en código (`ScannerEngineCatalog`) debe coincidir con
esta tabla — `check_engine_catalog()` en `tools/checks.py` verifica que los identificadores, las
fases y las plataformas no divergen, y corre antes que Gradle en cada PR.

---

## Tabla maestra

| ID                 | Nombre                  | Plataformas  | Fuente             |    Fase     | Dependencia                                                                                                       |
|--------------------|-------------------------|--------------|--------------------|:-----------:|-------------------------------------------------------------------------------------------------------------------|
| `GMS_CODE_SCANNER` | Google Code Scanner     | Android      | Cámara (UI propia) |     2 ✅     | `com.google.android.gms:play-services-code-scanner`                                                               |
| `MLKIT_CAMERAX`    | ML Kit + CameraX        | Android      | Cámara             |     2 ✅     | `com.google.mlkit:barcode-scanning` + `androidx.camera:*`                                                         |
| `VISION_IOS`       | Vision / AVFoundation   | iOS          | Cámara             |     3 ✅     | Framework del sistema                                                                                             |
| `ZXING_CPP`        | ZXing-cpp               | Android, iOS | Cámara + imagen    |     3 ✅     | `io.github.zxing-cpp:android` (Android) y `:kotlin-native` (iOS) — [ADR-0008](adr/ADR-0008-baseline-zxing-cpp.md) |
| `ZXING_JAVA`       | ZXing (Java)            | Desktop      | Imagen             |     5 ✅     | `com.google.zxing:core`                                                                                           |
| `BROWSER_DETECTOR` | BarcodeDetector API     | Web          | Cámara + imagen    |     4 ✅     | API del navegador                                                                                                 |
| `MLKIT_OCR`        | ML Kit Text Recognition | Android      | Cámara + imagen    |     4 ✅     | `com.google.mlkit:text-recognition`                                                                               |
| `VISION_OCR`       | Vision Text Recognition | iOS          | Cámara + imagen    |     4 ✅     | Framework del sistema                                                                                             |
| `MANUAL_INPUT`     | Entrada manual          | Todas        | Teclado            |    **1**    | Ninguna                                                                                                           |

**Los dos OCR son dos motores y no uno con dos implementaciones.** Hacen el mismo oficio —leer el
número impreso bajo el código— y comparten `OcrCodeInterpreter`, que es quien valida el dígito de
control y quien tiene los tests. Lo que no comparten es el reconocedor: ML Kit en Android,
`VNRecognizeTextRequest` en iOS. Es el criterio de `ZXING_JAVA` frente a `ZXING_CPP` (D13):
atribuirle a uno lo que leyó el otro falsearía justo la comparación que la app existe para hacer.
Viven en el mismo módulo `engines/ocr/` porque el intérprete es común y porque así quitar el OCR del
producto sigue siendo borrar una línea de `settings.gradle.kts`.

---

## Matriz de formatos por motor

Leyenda: ✅ soportado · ⚠️ parcial o dependiente de versión · ❌ no soportado

| Formato       | GMS | ML Kit | Vision | ZXing-cpp | ZXing Java | Browser | OCR | Manual |
|---------------|:---:|:------:|:------:|:---------:|:----------:|:-------:|:---:|:------:|
| QR Code       |  ✅  |   ✅    |   ✅    |     ✅     |     ✅      |    ✅    |  ❌  |   ✅    |
| Data Matrix   |  ✅  |   ✅    |   ✅    |     ✅     |     ✅      |   ⚠️    |  ❌  |   ✅    |
| Aztec         |  ✅  |   ✅    |   ✅    |     ✅     |     ✅      |   ⚠️    |  ❌  |   ✅    |
| PDF417        |  ✅  |   ✅    |   ✅    |     ✅     |     ✅      |   ⚠️    |  ❌  |   ✅    |
| EAN-13        |  ✅  |   ✅    |   ✅    |     ✅     |     ✅      |    ✅    | ⚠️  |   ✅    |
| EAN-8         |  ✅  |   ✅    |   ✅    |     ✅     |     ✅      |    ✅    | ⚠️  |   ✅    |
| UPC-A         |  ✅  |   ✅    |   ❌¹   |     ✅     |     ✅      |    ✅    | ⚠️  |   ✅    |
| UPC-E         |  ✅  |   ✅    |   ✅    |     ✅     |     ✅      |    ✅    | ⚠️  |   ✅    |
| Code 39       |  ✅  |   ✅    |   ✅    |     ✅     |     ✅      |    ✅    | ⚠️  |   ✅    |
| Code 93       |  ✅  |   ✅    |   ✅    |     ✅     |     ✅      |   ⚠️    | ⚠️  |   ✅    |
| Code 128      |  ✅  |   ✅    |   ✅    |     ✅     |     ✅      |    ✅    | ⚠️  |   ✅    |
| Codabar       |  ✅  |   ✅    |   ⚠️   |     ✅     |     ✅      |   ⚠️    | ⚠️⁸ |   ✅    |
| ITF           |  ✅  |   ✅    |   ✅    |     ✅     |     ✅      |   ⚠️    | ⚠️  |   ✅    |
| DataBar / RSS |  ❌  |   ❌    |   ❌    |     ✅     |     ✅      |    ❌    |  ❌  |   ✅    |
| MaxiCode      |  ❌  |   ❌    |   ❌    |     ✅     |     ✅      |    ❌    |  ❌  |   ✅    |
| Micro QR      |  ❌  |   ❌    |   ✅    |     ✅     |     ❌⁶     |    ❌    |  ❌  |   ✅    |
| rMQR          |  ❌  |   ❌    |   ❌    |     ✅     |     ❌⁶     |    ❌    |  ❌  |   ✅    |

¹ AVFoundation no tiene un tipo UPC-A: devuelve esos códigos como EAN-13 con un cero delante, que
es lo que son. Declararlo como soportado sería prometer una distinción que el sistema no hace.

² El motor de iOS usa `AVCaptureMetadataOutput`, que solo trabaja sobre vídeo en vivo. La imagen
estática llegará con RF-07 usando `VNDetectBarcodesRequest` del framework Vision.

³ La `BarcodeDetector` API no controla la cámara: solo recibe imágenes. La linterna se podría pedir
por constraints de `MediaStreamTrack`, pero solo la soportan algunos navegadores de Android; hasta
que se implemente, el motor no declara la capacidad y la UI no muestra los controles.

⁴ En Web el visor no está *dentro* del árbol de Compose: no hay equivalente de `AndroidView` ni de
`UIKitView`, así que el `<video>` vive en el documento y el composable solo le dice qué rectángulo
ocupar. Va **encima** del canvas —el tema pinta su fondo en toda la superficie, así que detrás no se
vería—, y por eso tapa el overlay de detección. El motor lo declara con `occludesOverlay` y la
pantalla deja de pintarlo, en lugar de ejecutar un dibujo que nadie ve.

⁵ `com.google.mlkit:text-recognition` es la variante *bundled*: el modelo latino viaja en el APK, a
diferencia del detector de códigos, que sí se descarga en el primer uso.

⁶ `com.google.zxing.BarcodeFormat` no tiene constantes para Micro QR ni rMQR. El port a C++ sí las
lee, y esa diferencia entre dos motores del mismo linaje es justo el tipo de dato que la app existe
para producir.

⁷ ZXing devuelve dos puntos en los códigos lineales y cuatro en los 2D, así que no son las esquinas
que espera el overlay. Se declara `reportsCornerPoints = false` y no se reportan: construir un
rectángulo a partir de dos puntos sería dibujar una suposición.

⁸ **La columna OCR dice lo que el motor *declara*, no lo que puede llegar a producir**, y la
distinción importa aquí más que en ninguna otra. Los dos motores de OCR declaran
`BarcodeFormat.oneDimensional` entero —de ahí que Codabar pase de ❌ a ⚠️, porque estaba declarado y
la tabla decía que no—, pero `OcrCodeInterpreter` solo sabe emitir cuatro simbologías: infiere por
longitud y dígito de control, así que produce EAN-8, UPC-A, EAN-13 e ITF y **nunca** Code 39, Code 93,
Code 128, UPC-E ni Codabar. Un número de ocho dígitos válido sale como EAN-8, no como UPC-E.

Se deja declarado el conjunto ancho a propósito: el selector usa lo declarado para decidir si el
motor entra en la cadena, y estrecharlo dejaría al OCR fuera de peticiones que sí sabe atender por la
vía del dígito de control. Lo que había que arreglar era la tabla, que prometía una correspondencia
uno a uno que nunca existió.

Las marcas ⚠️ del OCR reflejan que el motor no decodifica la simbología: **lee el número impreso
bajo el código** y el dominio infiere el formato validando su checksum. Solo funciona con
simbologías cuyo valor va impreso en texto (típicamente 1D de producto).

La columna **OCR** vale para los dos, `MLKIT_OCR` y `VISION_OCR`: declaran lo mismo porque lo que
determina qué formatos salen no es el reconocedor sino `OcrCodeInterpreter`, que es común. Dónde se
diferencian de verdad —qué dígitos acierta cada uno sobre una etiqueta arrugada— no se puede
declarar en una tabla, y es exactamente lo que la pantalla "Comparar" existe para medir.

---

## Capacidades por motor

| Capacidad                        | GMS | ML Kit | Vision | ZXing-cpp | ZXing Java | Browser | OCR | Manual |
|----------------------------------|:---:|:------:|:------:|:---------:|:----------:|:-------:|:---:|:------:|
| Cámara en vivo                   |  ✅  |   ✅    |   ✅    |     ✅     |     ❌      |    ✅    |  ✅  |   ❌    |
| Imagen estática                  |  ❌  |   ✅    |   ⏳²   |     ✅     |     ✅      |    ✅    |  ✅  |   ❌    |
| Múltiples códigos a la vez       |  ❌  |   ✅    |   ✅    |     ✅     |     ✅      |    ✅    |  ✅  |   ❌    |
| Escaneo continuo                 |  ❌  |   ✅    |   ✅    |     ✅     |     ❌      |    ✅    |  ✅  |   ✅    |
| UI propia del motor              |  ✅  |   ❌    |   ❌    |     ❌     |     ❌      |    ❌    |  ❌  |   ❌    |
| Linterna                         |  ❌  |   ✅    |   ✅    |     ✅     |     ❌      |   ❌³    |  ✅  |   ❌    |
| Zoom                             |  ❌  |   ✅    |   ✅    |     ✅     |     ❌      |   ❌³    |  ✅  |   ❌    |
| Puntos de esquina (normalizados) |  ❌  |   ✅    |   ✅    |     ✅     |     ❌⁷     |    ✅    |  ✅  |   ❌    |
| Superficie de preview propia     |  ❌  |   ✅    |   ✅    |     ✅     |     ❌      |   ✅⁴    |  ✅  |   ❌    |
| Confianza reportada              |  ❌  |   ❌    |   ❌    |     ❌     |     ❌      |    ❌    |  ✅  |   ❌    |
| Requiere permiso de cámara       |  ❌  |   ✅    |   ✅    |     ✅     |     ❌      |    ✅    |  ✅  |   ❌    |
| Requiere red                     |  ❌  |   ❌    |   ❌    |     ❌     |     ❌      |    ❌    |  ❌  |   ❌    |
| Descarga en tiempo de ejecución  |  ✅  |   ⚠️   |   ❌    |     ❌     |     ❌      |    ❌    | ❌⁵  |   ❌    |

`GMS_CODE_SCANNER` no requiere permiso de cámara porque el escaneo ocurre en un proceso de Google
Play Services, fuera de la app. Es su ventaja distintiva y la razón de que encabece la prioridad
en Android para escaneos puntuales.

**`UI propia del motor` tiene una consecuencia que ninguna otra capacidad tiene: el usuario puede
cerrar esa pantalla.** Al hacerlo, el motor emite `ScanError.Cancelled`. Ese error es fatal —la
sesión no puede continuar— pero **no admite degradación**: si la cadena pasara al motor siguiente,
cerrar la cámara la volvería a abrir, que es exactamente el defecto que se corrigió en la Ronda 16.
Lo declara `ScanError.allowsFallback` y lo respeta `FallbackScannerEngine` (§7.5 del SDD). Cualquier
motor futuro con UI propia hereda esa regla sin tocar nada.

---

## Prioridad de selección automática (RF-04)

Orden por defecto cuando el usuario no ha fijado un motor. El selector recorre la lista, descarta
los que no están `Available` y los que no cubren los formatos pedidos, y devuelve la cadena
resultante como preferido + fallbacks.

| Plataforma | Cadena por defecto                                                                |
|------------|-----------------------------------------------------------------------------------|
| Android    | `GMS_CODE_SCANNER` → `MLKIT_CAMERAX` → `ZXING_CPP` → `MLKIT_OCR` → `MANUAL_INPUT` |
| iOS        | `VISION_IOS` → `ZXING_CPP` → `VISION_OCR` → `MANUAL_INPUT`                        |
| Desktop    | `ZXING_JAVA` (solo imagen) → `MANUAL_INPUT`                                       |
| Web        | `BROWSER_DETECTOR` → `MANUAL_INPUT`                                               |

La cadena resultante no llega cruda al ViewModel: pasa por los decoradores del dominio. Por motor se
filtran los formatos, se aplican los límites del `ScanRequest` y se interpretan los valores; sobre
la
cadena entera se aplican el plazo y **la supresión de lecturas repetidas**. Ese último importa para
entender qué reporta un motor y qué no:

> Ningún motor evita repetir un código, y no es un defecto suyo — para ML Kit o Vision, un código
> que
> sigue delante de la lente es un código que sigue ahí. A treinta frames por segundo eso son decenas
> de lecturas idénticas. `DistinctDetectionsScannerEngine` suprime la repetición del mismo par
> (formato, valor) dentro de una ventana de dos segundos.
>
> **El comparador no lo lleva**, y es esencial que no lo lleve: su razón de ser es que *todos* los
> motores reporten el mismo código. Tampoco lo lleva la decodificación de imagen, donde los códigos
> aparecen una sola vez. Ver §7.5 del SDD.

Excepciones de la política:

- Si el `ScanRequest` pide **escaneo continuo** o **múltiples códigos**, `GMS_CODE_SCANNER` queda
  descartado por capacidades y `MLKIT_CAMERAX` encabeza la cadena en Android.
- Si el `ScanRequest` pide **imagen estática**, solo entran motores con `ScanSource.StaticImage`.
- `MANUAL_INPUT` **debería** cerrar siempre la cadena, y hoy no lo hace: declara solo
  `ScanSource.ManualInput`, así que el selector lo descarta ante cualquier petición de cámara y la
  cadena se queda vacía. Ver el cambio `close-the-chain-with-manual-entry`.
- **En escritorio, una petición de cámara en vivo cae directamente a `MANUAL_INPUT`**: `ZXING_JAVA`
  no declara esa fuente, así que el selector lo descarta antes de elegirlo. El decodificador está
  ahí, pero no hay captura de webcam que lo alimente.
- **Web no tiene respaldo tras el navegador**: zxing-cpp no publica artefacto wasmJs (ADR-0008), así
  que listarlo en esa cadena sería una entrada muerta. Lo que la cierra es la entrada manual.

---

## Cómo añadir un motor

1. Crear el módulo `engines/<nombre>/` con target(s) de la(s) plataforma(s) que soporte.
2. Depender únicamente de `:core:scanner-api`, `:core:model` y del SDK correspondiente.
3. Implementar `BarcodeScannerEngine` y, si aplica, las capacidades opcionales:
   `ImageDecodingEngine`, `CameraControlEngine`, `TextInputEngine` y `CameraPreviewEngine`
   (esta última si el motor aporta superficie de vídeo — ver ADR-0007).
4. Declarar un `ScannerEngineDescriptor` honesto — las capacidades declaradas se contrastan con el
   comportamiento real en la suite de contrato.
5. Añadir el ID a `ScannerEngineId`, la fila a este documento y la entrada a
   `ScannerEngineCatalog`.
6. Registrarlo en el `platformModule()` del target correspondiente en `:composeApp`.
7. Heredar `BarcodeScannerEngineContractTest` aportando la factory del motor.
8. Añadir el módulo a `settings.gradle.kts`.
9. Actualizar `docs/ROADMAP.md`, y la cadena por defecto de este documento si el motor entra en
   alguna. Un cambio de comportamiento que no llega al ROADMAP está a medias, y el paso faltaba
   aquí mientras el resto de las guías ya hablaban de nueve.

Sobre el paso 6 conviene saber lo que costó una app muerta al arrancar: **Koin resuelve por igualdad
exacta de tipo y no recorre supertipos**, así que hay que declarar cada dependencia con el tipo que
el motor *consume* y no con el que devuelve la fábrica. Eso lo comprueban `KoinGraphTest` para el
grafo de escritorio y los módulos comunes y `AndroidKoinGraphTest` —con un `Context` real que da
Robolectric en la JVM— para el de Android, que es el más grande de los cuatro y el único donde
ocurrió el crash que abrió D18 (§10 del SDD). Ya no queda ningún grafo sin comprobar.

Ningún paso toca `:feature:scanner` ni `:core:domain`. Si un motor nuevo obliga a modificarlos, es
señal de que el SPI se quedó corto y hay que extenderlo de forma explícita — no a parchear la UI.

El paso 7 obliga a **todo motor que se pueda instanciar sin dispositivo** —hoy la entrada manual y
ZXing en Java— y a los decoradores del dominio. Los de cámara **no la heredan, y es una decisión y no
un olvido**: construirlos exige un emulador, y un test que nunca se ejecuta es peor que no tenerlo
(D6). A ellos los cubre lo que declaran y los decoradores que los envuelven.

La suite es lo que impide que un motor declare capacidades que
luego no cumple, y las capacidades declaradas son de lo que dependen el selector y la UI entera.
