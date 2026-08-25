# Instrucciones de trabajo en WhyScan

## Prioridad de plataformas: Android primero, y punto

**El foco es Android.** Es la plataforma que se va a publicar, la única que alguien ha ejecutado de
verdad y la única cuyo trabajo se puede validar de principio a fin desde este repositorio.

**iOS está despriorizado hasta nuevo aviso del dueño del proyecto.** No se toca por iniciativa
propia: ni motores nuevos, ni refactors, ni cerrar pendientes de la Fase 3 o de las partes de iOS de
otras fases, ni lanzar el workflow `iOS (manual)`, ni proponerlo como siguiente paso. Da igual que
un pendiente de iOS sea el que "mejor se pueda hacer sin dispositivo": **poder hacerlo no lo
convierte en prioridad**. Si un cambio de Android o de código común obliga a tocar iOS para que
siga compilando, se hace lo mínimo y se dice; eso no es trabajar en iOS.

El motivo es el que ya está escrito en `docs/ROADMAP.md`: sin dispositivos Apple no se puede
*probar* nada de esa plataforma. Enlazar el framework comprueba que el Kotlin/Native compila y nada
más, y eso no acerca la app a la tienda.

Desktop y Web se mantienen: existen, compilan en `Verify` y no cuestan atención. Tampoco son el
sitio donde invertir de más.

## Qué se puede verificar desde aquí, y qué no

- **Aquí no compila nada.** El entorno de desarrollo no alcanza `dl.google.com` ni `api.foojay.io`,
  así que ninguna tarea de Gradle se puede lanzar en local. Lo que sí corre es
  `python3 tools/checks.py`, en segundos y sin red: paridad de los catálogos de recursos entre
  idiomas, claves huérfanas, longitud de línea, orden de imports y `package` contra carpeta.
  **Ejecutarlo antes de cada commit.**
- La autoridad real es `Verify`, que corre en cada pull request y cubre las tres plataformas que
  este proyecto puede ejecutar: detekt, tests del núcleo, Android (debug, lint y release con R8),
  Desktop y Web.
- `iOS (manual)` y `Baseline profile (manual)` son workflows aparte y **solo manuales**, a
  propósito: lo que hacen no es un criterio para aceptar un cambio.
- No hay tests instrumentados y no los va a haber (D6): sin emulador en CI, un test que exija
  dispositivo es un test que nunca se ejecuta.

## Cómo se escribe aquí

- **En castellano**, comentarios y documentación incluidos.
- Los comentarios explican **por qué**, no qué. Un comentario que repite el código sobra; uno que
  guarda la razón de una decisión —o el defecto que la provocó— vale su sitio.
- `docs/` es fuente de verdad, no un resumen: `ENGINES.md` y `ScannerEngineCatalog` no pueden
  divergir, y hay tests que lo comprueban. Un cambio de comportamiento que no llega al ROADMAP está
  a medias.
- Los ADR son registros de decisiones ya tomadas. **No se reescriben** para que cuadren con hoy.
