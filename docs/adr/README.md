# Decisiones de arquitectura (ADR)

Un ADR registra **una decisión que tenía alternativa**, junto con el coste que se aceptó al tomarla.
No son documentación de diseño —eso es [`../SDD.md`](../SDD.md)— ni un registro de cambios: son la
respuesta a *por qué esto es así y no de la otra forma*, escrita cuando todavía se sabía.

> **Un ADR no se reescribe.** Cuando una decisión deja de valer, se escribe uno nuevo que la sustituye
> y en el viejo se cambia **solo** la línea de estado, a `Superada por ADR-NNNN`. Editar el
> razonamiento antiguo para que cuadre con hoy destruye lo único que ese archivo guardaba.

## Índice

| ADR | Decisión | Estado | Fecha |
|---|---|---|---|
| [0001](ADR-0001-compose-multiplatform.md) | Compose Multiplatform en lugar de KMP con UI nativa | Aceptada | 2026-07-30 |
| [0002](ADR-0002-scanner-engine-spi.md) | Motores de escaneo como SPI con capacidades declarativas | Aceptada | 2026-07-30 |
| [0003](ADR-0003-koin-como-di.md) | Koin como contenedor de DI | Aceptada | 2026-07-30 |
| [0004](ADR-0004-flow-como-api-de-sesion.md) | `Flow<ScanEvent>` como API de sesión de escaneo | Aceptada | 2026-07-30 |
| [0005](ADR-0005-navegacion-propia.md) | Navegación propia mínima en la Fase 1 | Aceptada | 2026-07-30 |
| [0006](ADR-0006-reestructuracion-del-build.md) | Reestructurar el build de una sola vez | Aceptada | 2026-07-30 |
| [0007](ADR-0007-preview-como-capacidad-del-motor.md) | El preview de cámara es una capacidad del motor, no de la feature | Aceptada | 2026-07-30 |
| [0008](ADR-0008-baseline-zxing-cpp.md) | El baseline de comparación es zxing-cpp desde artefactos publicados | Aceptada | 2026-07-31 |
| [0009](ADR-0009-play-feature-delivery-aplazado.md) | Play Feature Delivery se aplaza, con condición de entrada | Aceptada | 2026-07-31 |
| [0010](ADR-0010-dos-disposiciones-de-la-pantalla-de-escaneo.md) | Dos disposiciones para la pantalla de escaneo, no una con condicionales | Aceptada | 2026-08-21 |
| [0011](ADR-0011-idioma-de-la-app-por-encima-del-sistema.md) | El idioma de la app se fija por encima del sistema | Aceptada | 2026-08-21 |
| [0012](ADR-0012-la-nota-es-del-historial-no-de-la-deteccion.md) | La nota del usuario es del historial, no de la detección | Aceptada | 2026-08-22 |
| [0013](ADR-0013-baseline-profile.md) | Baseline profile versionado y lanzado a mano | Aceptada | 2026-08-24 |
| [0014](ADR-0014-la-marca-sale-del-objeto.md) | La marca sale del objeto que la app lee | Aceptada | 2026-08-24 |
| [0015](ADR-0015-probar-un-motor-es-un-dialogo.md) | Probar un motor abre un diálogo a pantalla completa | Aceptada | 2026-08-27 |
| [0016](ADR-0016-agents-md-como-contrato-canonico.md) | `AGENTS.md` es el contrato canónico para agentes | Aceptada | 2026-08-30 |
| [0017](ADR-0017-openspec-para-cambios-de-comportamiento.md) | Los cambios de comportamiento se proponen en OpenSpec antes de escribirse | Aceptada | 2026-08-30 |
| [0018](ADR-0018-federar-la-base-y-no-la-marca.md) | Se federa la base del sistema de diseño, no la marca | Aceptada | 2026-08-30 |
| [0019](ADR-0019-el-applicationid-identifica-a-quien-publica.md) | El `applicationId` identifica a quien publica, no al código | Aceptada | 2026-08-30 |

`tools/checks.py` comprueba que esta tabla y los archivos no se separen: cada ADR está indexado, cada
fila del índice apunta a un archivo que existe, y todos llevan sus campos de cabecera.

## Cuándo escribir uno

Cuando se cumplen las tres:

1. **Había alternativa de verdad** — algo que una persona competente habría elegido en su lugar.
2. **Condiciona el trabajo futuro**: es una regla, no un detalle de implementación.
3. **Tiene un coste que merece nombrarse.**

No hace falta para arreglar un defecto, renombrar algo, adoptar la biblioteca obvia o hacer lo que
nadie habría hecho de otra manera. Un ADR sobre una no-decisión diluye a los que sí lo son.

Señales de que sí lo es: dos opciones con mérito real; una restricción que cerró la puerta obvia; un
defecto que obligó a cambiar la estructura; un "deliberadamente no hicimos X".

## Cómo se escribe

Se parte de [`TEMPLATE.md`](TEMPLATE.md). Nombre de archivo
`ADR-NNNN-titulo-en-minusculas-con-guiones.md`, con el número siguiente sin usar — **los números no
se reciclan**, ni siquiera los de un ADR abandonado.

Lo que separa un ADR útil de uno decorativo, mirando los dieciocho que ya hay:

- **El título dice la decisión, no el tema.** "Probar un motor abre un diálogo a pantalla completa,
  no un destino nuevo" se entiende sin abrir el archivo.
- **El contexto nombra la restricción que cerró la puerta obvia.** El contenido real del ADR-0015 es
  que la pantalla de escaneo vive dentro del `Scaffold` y por eso *no puede* ocupar la pantalla
  entera. Sin eso, la decisión parece un capricho.
- **Las consecuencias incluyen al menos un coste.** Un ADR con solo ventajas es publicidad, y esa
  sección es justo la que necesita quien lo lee dentro de dos años.
- **Las alternativas descartadas van en tabla, con el motivo concreto de cada una.** "Peor" no es un
  motivo. Lo que se descarta sin dejar escrito por qué, se vuelve a proponer cada seis meses.
- **El defecto concreto gana al principio abstracto.** El ADR-0003 se recuerda porque la app se murió
  en un teléfono real con el CI en verde; el principio solo, no.

Después: añadir la fila al índice de arriba, enlazarlo desde donde ahora obliga —la sección del SDD,
`ENGINES.md`, la ronda del ROADMAP o el KDoc del tipo que gobierna— y ejecutar
`python3 tools/checks.py`. Un ADR que no enlaza nadie es un ADR que nadie encontrará en el momento en
que hacía falta.

El comando `/adr-new` del harness hace todo esto con el número y el índice ya correctos.
