# ADR-0018 — Se federa la base del sistema de diseño, no la marca, y se publica desde este repositorio

- **Estado:** Aceptada
- **Fecha:** 2026-08-30

## Contexto

Otras apps de la empresa quieren reutilizar el sistema de diseño de WhyScan y algunos componentes
comunes. La petición es razonable y lo que hay hoy no la puede atender, por tres motivos distintos
que conviene no mezclar.

**El primero es que `:core:designsystem` no es un sistema de diseño: es el tema de WhyScan.** De sus
930 líneas, `ScannerPalette` (330) y `BrandMark` (95) **son la marca** —el esmeralda, el grafito
cálido, el patrón de localización con el módulo fugado del ADR-0014—, y compartirlas no es
compartir, es que todas las apps de la empresa se llamen WhyScan. `Theme.kt`, `Radius.kt` y
`Typography.kt` son mecánica genérica con **valores** de WhyScan. Solo tres archivos —`Contrast.kt`,
`AppLanguage.kt` y `LocalSnackbarHostState.kt`— no dependen de la marca hoy, y eso está comprobado:
`check_design_system()` en `tools/checks.py` lo mantiene cierto.

**El segundo es que no hay ninguna superficie de API.** No hay `explicitApi()`, así que cada
`internal` que a alguien se le olvidó marcar es API pública por accidente. No hay validador de
compatibilidad binaria, así que romper a un consumidor no se ve en ninguna revisión. No hay
publicación Maven, ni versionado, ni documentación generada. Un módulo así no se consume: se copia,
y a los seis meses hay cuatro versiones divergentes que ya nadie une.

**El tercero es que el paquete es `com.whyscan`.** Código compartido bajo el espacio de nombres de un
producto es una promesa que se rompe el día que WhyScan cambie de nombre o se archive.

Y hay una restricción que atraviesa todo lo anterior: **aquí no compila nada**. Esta decisión se
puede tomar y escribir; la configuración de Gradle que la implementa no se puede verificar en este
entorno, y por eso va secuenciada en un cambio de OpenSpec y no en este ADR.

## Decisión

**Se separa la base de la marca, y se publica solo la base, desde este mismo repositorio.**

Tres partes:

**1. El corte.** Nace `:core:foundation`, sin marca y publicable. Se queda `:core:designsystem`, que
**es** WhyScan y no se publica nunca.

| Va a `:core:foundation` | Se queda en `:core:designsystem` |
|---|---|
| `Contrast` — aritmética WCAG sobre ARGB, sin Compose | `ScannerPalette` — el esmeralda y el grafito |
| `AppLanguage` y sus cuatro `actual` — el idioma de la app por encima del sistema (ADR-0011) | `BrandMark` — el patrón con el módulo fugado (ADR-0014) |
| `LocalSnackbarHostState` | Los **valores** de radio y de la escala tipográfica |
| La *mecánica* de las escalas: un tipo que recibe valores y produce `Shapes` y `Typography` | `WhyScanTheme`, que ata lo anterior a esta marca |
| `themeFrom(palette)`: declarar los ~34 roles de Material a partir de una paleta | |

El criterio del corte, dicho de una vez: **lo reutilizable nunca fueron los colores, son las
reglas.** Que haya que declarar los treinta y cuatro roles o Material los rellena con su morado de
fábrica; que el contraste sea aritmética comprobable sin dispositivo; que lo que se pinta encima del
vídeo viva fuera del tema porque ahí no hay tema que valga. Eso le sirve a cualquier app de la
empresa. El esmeralda, no.

**2. La publicación sale de este repositorio, no de uno nuevo.** WhyScan sigue consumiendo
`project(":core:foundation")` y las demás apps consumen el artefacto publicado. El grupo Maven **no**
lleva `whyscan` en el nombre — el nombre concreto lo decide el dueño del proyecto, y hasta que lo
haga esto está bloqueado, no supuesto.

**3. Publicar exige cinco cosas, y sin las cinco no se publica.** Son la diferencia entre una
biblioteca y una carpeta que otros copian:

- **`explicitApi()` en modo estricto** sobre `:core:foundation`. Sin esto, la API pública es "lo que
  nadie marcó como interno", que no es una API: es un accidente.
- **Validador de compatibilidad binaria**, con el volcado `.api` versionado. Romper a un consumidor
  pasa a ser un diff que se ve en la revisión en lugar de una llamada de teléfono.
- **Versionado semántico con la política escrita**, porque en Compose no es obvia: cambiar el valor
  por defecto de un parámetro es compatible en fuente y **rompe** en binario, y añadir un parámetro
  a un `@Composable` público rompe las dos.
- **Documentación generada** (Dokka). Un consumidor que tiene que leerte el código no es un
  consumidor, es un fork.
- **Un consumidor que no sea WhyScan.** Un módulo `samples/` que dependa **solo** de la API pública
  de `:core:foundation` y que compile en `Verify`. Es el único de los cinco que detecta el
  acoplamiento de verdad: mientras el único consumidor sea la app que lo escribió, "es reutilizable"
  es una opinión.

Los cinco están secuenciados en `openspec/changes/federate-design-system/`, con la pregunta de
verificación contestada tarea por tarea. Ninguno se puede ejecutar aquí.

## Consecuencias

- Otra app de la empresa puede tomar la base y ponerle **su** paleta, y hereda con ella lo que costó
  descubrir: los treinta y cuatro roles, el contraste medido y la regla del overlay. Eso es lo que
  se quería compartir.
- **Este repositorio pasa a ser el hogar de una biblioteca compartida, y eso lo constriñe.** La
  cadencia de entrega y la estabilidad de API de WhyScan dejan de ser solo asunto de WhyScan: un
  refactor interno que antes era libre ahora puede romper a un consumidor. Es el coste principal y
  se acepta a sabiendas — la alternativa, un repositorio aparte, cobra ese mismo peaje antes y en
  cada cambio.
- **El sistema de diseño todavía se mueve.** Nació en la Ronda 1 y cambió en las Rondas 8, 10 y 15.
  Congelar su API ahora es prematuro, y por eso `:core:foundation` sale con versión `0.x`, donde el
  contrato es explícitamente "puede romper", y sube a `1.0` cuando haya un segundo consumidor real.
- Aparece un `internal` de verdad: con `explicitApi()`, todo lo que hoy es público por descuido hay
  que decidirlo uno a uno. Es trabajo aburrido y es exactamente el trabajo.
- **La marca deja de poder fugarse sin que se note.** `check_design_system()` ya impide que
  `Contrast`, `AppLanguage` o `LocalSnackbarHostState` toquen la paleta, y que aparezca un color
  literal fuera de `ScannerPalette` — que es como se coló el verde del contorno de las detecciones
  dentro de `ScanOverlay`, encima del vídeo, donde nadie lo iba a buscar.
- Queda **una decisión que no es técnica y no se toma aquí**: el nombre del grupo Maven y del
  paquete de la base. Hasta que exista, la federación no arranca.

## Alternativas descartadas

| Alternativa | Motivo |
|---|---|
| Copiar y pegar en cada app | Es el estado por defecto y el que se quiere evitar. A los seis meses hay cuatro versiones divergentes, y el arreglo de contraste que se hizo en una no llega a las otras. Ni siquiera es barato: es caro más tarde |
| Extraer a un repositorio aparte desde ya | Cobra el peaje antes de tener el beneficio. Cada cambio que toque la base y la app pasa a ser dos PR coordinados, y hoy **hay un solo consumidor**. Se reconsidera cuando haya dos apps de verdad tirando del artefacto, que es cuando el repositorio aparte empieza a pagar |
| Publicar `:core:designsystem` entero, con marca incluida | Compartir el esmeralda no es compartir un sistema: es que todas las apps de la empresa parezcan WhyScan. Y ata el sistema al ADR-0014, que es una decisión **de este producto** |
| Build compuesto o submódulo de git | Evita la infraestructura de publicación y la sustituye por otra peor: cada consumidor compila el fuente —Compose Multiplatform, que no es rápido— y "la versión" pasa a ser un SHA. El pin deja de ser legible en el momento en que hace falta leerlo |
| Publicar ya y añadir las garantías después | Es el orden que garantiza no añadirlas. En cuanto hay un consumidor, `explicitApi()` deja de ser una limpieza y pasa a ser un cambio incompatible, y el validador de compatibilidad nace con una línea base que ya incluye lo que nunca quisimos publicar |
| Empezar en `1.0` | El sistema todavía se mueve y no hay un segundo consumidor. `1.0` promete estabilidad que nadie puede sostener, y la promesa rota cuesta más que el `0.x` |
