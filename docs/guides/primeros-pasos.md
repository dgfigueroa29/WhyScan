# Primeros pasos

**Castellano · [English](getting-started.md)**

Un recorrido por WhyScan para quien acaba de clonarlo. Veinte minutos, y al final sabes dónde está
cada cosa y por qué.

## Qué estás mirando

Dos cosas que resultan ser una sola app:

- **Un lector de códigos de barras y QR.** Sin cuenta, sin rastreo y sin red. Se apunta y se lee.
- **Un banco de pruebas de motores de escaneo.** Nueve alternativas detrás de una única interfaz,
  elegidas automáticamente, comparadas en paralelo y degradando con elegancia cuando una no está
  disponible — sobre Android, iOS, Escritorio y Web con un solo código base.

Lo segundo solo se ve en **modo avanzado** (Ajustes → Avanzado). Quien instala la app para leer un QR
no ve nunca la palabra "motor", y es a propósito: ese es el criterio de salida de la fase actual.

## Compilarlo

Hace falta **JDK 17** y el **SDK de Android (API 36)**. Android Studio trae los dos.

```bash
git clone https://github.com/dgfigueroa29/WhyScan.git
cd WhyScan

./gradlew :androidApp:assembleDebug                  # Android
./gradlew :composeApp:desktopJar                     # Escritorio
./gradlew :composeApp:wasmJsBrowserDistribution      # Web
```

iOS se enlaza desde el workflow `iOS (manual)` de GitHub Actions. En el repositorio no hay
`iosApp.xcodeproj` —solo se crea desde Xcode— y la plataforma está despriorizada, así que no empieces
por ahí.

## El único comando que hay que recordar

```bash
python3 tools/checks.py
```

Segundos, sin red y sin Gradle. Caza la deriva entre los catálogos de inglés y español, las claves
huérfanas, el orden de imports, el `package` que no coincide con su carpeta, las etiquetas `return@`
que no resuelven, la garantía de privacidad del manifiesto y la propia estructura del repositorio.
Se ejecuta antes de cada commit; en CI también corre el primero.

## Orientarse

Empieza por el SPI, porque todo lo demás orbita a su alrededor:

```
core/scanner-api/       BarcodeScannerEngine — aquí vive el diseño entero
    ↑
engines/<nueve>/        Un módulo por alternativa
    ↓
core/domain/            Política de selección + los decoradores que envuelven a todo motor
    ↓
feature/{scanner,history,settings}      ViewModels e interfaz en Compose
    ↓
composeApp/             App(), navegación y platformModule() por plataforma
```

Después, en este orden:

1. **[`docs/ENGINES.md`](../ENGINES.md)** — los nueve motores, qué sabe hacer cada uno y la cadena de
   selección por plataforma. Es corto, y hace que el resto se entienda.
2. **[`docs/adr/README.md`](../adr/README.md)** — dieciocho decisiones con su coste. Empieza por el
   [ADR-0002](../adr/ADR-0002-scanner-engine-spi.md) (el SPI) y el
   [ADR-0003](../adr/ADR-0003-koin-como-di.md) (la inyección de dependencias, y el defecto que mató a
   la app en su primer arranque en un teléfono real).
3. **[`docs/SDD.md`](../SDD.md)** — el documento de diseño. Unas veinticinco mil palabras: se lee
   la sección que hace falta, no entero. §7 es el SPI, §10 la DI, §11 la persistencia y §13 la
   estrategia de calidad.
4. **[`docs/ROADMAP.md`](../ROADMAP.md)** — qué está hecho, qué queda y qué está bloqueado por no
   tener un dispositivo. También guarda los defectos, en la ronda en la que aparecieron.

## Tres cosas que te van a sorprender

**No hay tests instrumentados.** Sin emulador en CI, un test que exige dispositivo nunca se ejecuta y
da una falsa sensación de red. En su lugar: el grafo de Koin se prueba en la JVM con Robolectric,
`App()` entera se compone en un test sin ventana, y `koinInject` se verifica solo con el runtime de
Compose. La regla es que todo lo que se comprueba se pueda ejecutar en cada pull request.

**La documentación es fuente de verdad, no un resumen.** `docs/ENGINES.md` y `ScannerEngineCatalog`
no pueden divergir, porque hay una comprobación en `tools/checks.py` que los compara. Los suelos de cobertura se miden, no se
prometen.

**Casi todo esto lo escribió un agente de IA**, dirigido por el dueño del proyecto. Está documentado
en [`docs/ai/`](../ai/README.md), incluido lo que el agente **no** pudo hacer: los dos defectos más
valiosos de la historia de este proyecto los encontró una persona poniendo la app en un teléfono de
verdad.

## Por dónde seguir

| Quieres | Vas a |
|---|---|
| Arreglar algo o añadir una funcionalidad | [`CONTRIBUTING.es.md`](../../CONTRIBUTING.es.md) |
| Añadir un motor de escaneo | [`docs/ENGINES.md`](../ENGINES.md#cómo-añadir-un-motor) |
| Trabajar en esto con un agente de IA | [`AGENTS.md`](../../AGENTS.md) y luego [`.claude/README.md`](../../.claude/README.md) |
| Proponer un cambio de comportamiento | [`openspec/README.md`](../../openspec/README.md) |
| Entender una decisión | [`docs/adr/README.md`](../adr/README.md) |
| Reportar un problema de privacidad o seguridad | [`SECURITY.md`](../../SECURITY.md) — en privado |
