# Contribuir a WhyScan

**Castellano · [English](CONTRIBUTING.md)**

Gracias por asomarte. WhyScan es un lector de códigos de barras y QR sin cuenta, sin rastreo y sin
red, y debajo un banco de pruebas que compara nueve motores de escaneo en cuatro plataformas.

Si eres un agente de IA, lee [`AGENTS.md`](AGENTS.md) en lugar de esto: es el contrato normativo y es
más concreto que este archivo.

## Antes de escribir código

**Abre un issue primero** para cualquier cosa que cambie comportamiento. No es burocracia: el diseño
está escrito en `docs/SDD.md` y en dieciocho ADR, y un cambio que contradice una decisión ya
registrada necesita una decisión nueva, no un parche. Enterarse de eso *después* de escribir el
código no le hace gracia a nadie.

Buenas primeras contribuciones: un defecto que puedas reproducir, un test que falta sobre
comportamiento que ya existe, un error de documentación, un arreglo de traducción.

## Lo que conviene saber de este proyecto

**Android es la prioridad.** Es la plataforma que se va a publicar y la única que alguien ha
ejecutado de principio a fin. Escritorio y Web se mantienen porque compilan en CI y no cuestan
atención. **iOS está despriorizado** — no abandonado, pero sin dispositivos Apple no se puede
*probar* nada de esa plataforma, y enlazar el framework solo demuestra que el Kotlin/Native compila.

**No hay tests instrumentados y no los va a haber.** Sin emulador en CI, un test que exige
dispositivo nunca se ejecuta y da una falsa sensación de red. La regla, dicha con precisión, es que
**todo lo que se comprueba se pueda ejecutar en cada pull request** — por eso el grafo de
dependencias de Android sí tiene test, con Robolectric, en la JVM.

**La app no tiene ningún acceso a la red.** Sin permiso `INTERNET`, sin cliente HTTP, sin analítica,
sin informes de fallos y sin copia de seguridad del sistema. Es una promesa hecha a los usuarios en
el README, en Ajustes y en la política de privacidad publicada, y una parte la vigila un chequeo en
CI. Un pull request que la debilite no se acepta.

**`docs/` es fuente de verdad, no un resumen.** `docs/ENGINES.md` y `ScannerEngineCatalog` no pueden
divergir: hay una comprobación en `tools/checks.py` que los compara en cada PR.

## Puesta en marcha

```bash
git clone https://github.com/dgfigueroa29/WhyScan.git
cd WhyScan
./gradlew :androidApp:assembleDebug     # JDK 17, Android SDK 36
```

Se abre con Android Studio (o IntelliJ con el plugin de Kotlin Multiplatform). El módulo
`:composeApp` tiene la carcasa compartida; `:androidApp` es la entrada de Android.

Otros destinos: `./gradlew :composeApp:desktopJar` y
`./gradlew :composeApp:wasmJsBrowserDistribution`.

## Antes de cada commit

```bash
python3 tools/checks.py
```

Corre en segundos, no necesita red y caza lo que no caza nadie más: la paridad de los catálogos de
recursos entre inglés y español, las claves huérfanas, los `Res.string.X` usados sin importar, el
`package` que no coincide con su carpeta, las etiquetas `return@` que no resuelven, la garantía de
privacidad del manifiesto y la propia estructura del repositorio. En CI también se ejecuta el
primero.

Después, si puedes compilar en local:

```bash
./gradlew detekt jvmTest desktopTest
./gradlew :composeApp:testDebugUnitTest    # el grafo de Android, con Robolectric
```

## Convenciones

- **El código, los comentarios y el KDoc van en castellano**, y `docs/` también. Las superficies que
  leen los agentes (`AGENTS.md`, `.claude/`, `openspec/`, `docs/ai/`) van en inglés. Las guías para
  personas, en los dos idiomas. La tabla completa está en
  [`AGENTS.md`](AGENTS.md#language-policy).
- Los comentarios explican **por qué**, no qué. Uno que repite el código sobra; uno que guarda la
  razón de una decisión vale su sitio.
- Líneas de menos de 120 caracteres. El orden de imports es el de ktlint: primero todo lo demás,
  después `java.**`, `javax.**`, `kotlin.**`, y al final los que llevan alias.
- Cada cadena visible existe en `values/` **y** en `values-es/`. El catálogo sin calificador es el
  respaldo de **cualquier** idioma, así que una clave que solo está en español revienta para todos
  los demás.
- Las dependencias apuntan hacia dentro. Un motor depende solo de `:core:scanner-api`, `:core:model`
  y su SDK — nunca de una feature ni de `:core:domain`.

## Añadir un motor de escaneo

Los nueve pasos están en [`docs/ENGINES.md`](docs/ENGINES.md#cómo-añadir-un-motor). Los dos con los
que se tropieza todo el mundo:

- **Declarar un `ScannerEngineDescriptor` honesto.** El selector y la interfaz entera dependen de las
  capacidades declaradas, y `BarcodeScannerEngineContractTest` contrasta lo declarado con el
  comportamiento real. Heredarla es obligatorio para todo motor que se pueda instanciar sin
  dispositivo; los de cámara no lo hacen a propósito, porque construirlos exige un emulador (D6).
- **Koin resuelve por igualdad exacta de tipo y no recorre supertipos.** Hay que declarar cada
  dependencia con el tipo que la *consume*, no con el que devuelve la fábrica. Esto mató la app en su
  primer arranque en un teléfono real, con el CI en verde todo el tiempo.

Si añadir un motor obliga a tocar `:feature:scanner` o `:core:domain`, para y dilo en el pull
request: el SPI se quedó corto, y ampliarlo es una decisión con su propio ADR.

## Pull requests

Rellena la plantilla. La sección que más importa es la última: **qué verificaste y qué no**. Decir
que un test pasó sin haberlo ejecutado es lo único que hace que un pull request se cierre en lugar de
revisarse.

`Verify` corre en cada pull request —detekt, tests del núcleo, Android (debug, lint y release con
R8), Escritorio y Web— y es la autoridad. `iOS (manual)` y `Baseline profile (manual)` van aparte y
a mano a propósito: no son criterio de aceptación.

## Reportar un problema de seguridad o privacidad

Está en [`SECURITY.md`](SECURITY.md). Un defecto de privacidad —cualquier cosa que permita que lo
escaneado salga del dispositivo— se trata como un problema de seguridad, porque lo es.

## Licencia

Al contribuir aceptas que tu contribución quede bajo la [licencia Apache 2.0](LICENSE), la misma que
el resto del proyecto.
