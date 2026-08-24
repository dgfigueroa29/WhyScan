# ADR-0013 — Baseline profile generado en un emulador declarado, versionado y lanzado a mano

- **Estado:** Aceptada
- **Fecha:** 2026-08-24

## Contexto

La app arranca en Compose Multiplatform, monta un grafo de Koin con cinco motores y abre Room. Todo
eso es código que ART **interpreta** la primera vez que se ejecuta: sin un baseline profile, el
primer arranque tras instalar —y el primero tras cada actualización— paga la traducción entera. Es el
arranque que decide si alguien deja la app instalada, y es además el único que Play mide en Android
vitals.

Un baseline profile es la lista de clases y métodos que ART compila por adelantado al instalar. Play
la distribuye dentro del AAB. No se escribe a mano: se **graba** ejecutando la app y anotando por
dónde pasa.

Ahí está el problema de este proyecto. Grabar exige ejecutar, y este repositorio decidió en la Fase 2
que **no habría tests instrumentados**, porque sin emulador en CI un test que exige dispositivo es un
test que nadie ejecuta (deuda D6). El baseline profile parece caer del mismo lado.

No cae, y la diferencia importa: **un test instrumentado es un criterio, una grabación es un
artefacto.** Un test que nadie ejecuta da una falsa sensación de red; un archivo que se genera cuando
hace falta y se versiona no engaña a nadie sobre lo que cubre. El motivo de D6 era la falsedad, no el
emulador.

## Decisión

Tres decisiones, y las tres son sobre *cuándo* y *dónde*, no sobre *si*.

**1. Se graba sobre un Gradle Managed Device y no sobre un dispositivo enchufado.** El perfil depende
de por dónde pasa el código, así que dos dispositivos distintos dan dos perfiles distintos y ninguno
es "el" perfil. Con un emulador declarado en la build —Pixel 6, API 34, imagen `aosp`— la grabación
es reproducible por cualquiera y no depende de qué tenga nadie sobre la mesa. `useConnectedDevices`
queda en `false` para que usar el propio sea un acto deliberado.

La imagen es `aosp` y no `google`: lo que se quiere grabar es el arranque de **esta** app —Compose,
Koin, Room, la cadena de motores— y no el de los servicios de Google. En esa imagen el Google Code
Scanner se declara no disponible, exactamente igual que en un teléfono sin Play Services, y recorrer
el fallback también forma parte del arranque real.

**2. El perfil se versiona y la build de release lo consume del repositorio.**
`automaticGenerationDuringBuild` queda en `false`. Si `assembleRelease` arrancara un emulador, nadie
podría ensamblar la app sin uno, y el job de Android del CI dejaría de existir tal como está.

**3. La generación vive en un workflow manual, `baseline-profile.yml`.** El mismo trato que iOS y por
el mismo motivo: no es un criterio para aceptar o rechazar un cambio. Generar un perfil no comprueba
nada — arranca un emulador y escribe un archivo. Se relanza cuando cambia el camino que graba (una
pantalla nueva, un motor nuevo, una versión de Compose), no en cada pull request. Lo que `Verify` sí
comprueba en cada cambio es que el cableado del plugin no rompe la build.

## Qué se graba

Dos recorridos, en `:baselineprofile`:

- **`startup`**, marcado con `includeInStartupProfile`. Alimenta además el *startup profile*, que AGP
  usa para reordenar el DEX y poner junto lo que se toca al abrir.
- **`navigation`**: escáner → historial → ajustes → escáner. El comparador de motores no entra: vive
  detrás del modo avanzado y no es camino de nadie que abra la app a leer un código.

Los dos conceden el permiso de cámara antes de arrancar. No es un atajo para esquivar el diálogo del
sistema —que también—: el camino que interesa grabar es el del usuario que ya dijo que sí, que es el
de todos los arranques menos el primero y el único que llega a encender la cámara.

## Consecuencias

**Positivas**

- El primer arranque deja de pagar la interpretación de Compose, Koin y Room. Es la única
  optimización de arranque que este proyecto puede hacer sin tocar arquitectura.
- `androidx.profileinstaller` extiende el efecto a Android 7-11, donde el sistema no instala el
  perfil por su cuenta. Con `minSdk` 24 eso es la mitad del rango que la app soporta, y justo la
  mitad más lenta.
- El emulador declarado abre la puerta —no la cruza— a que algún día se mida el arranque con
  macrobenchmark. La infraestructura es la misma.

**Negativas y su gestión**

- **El perfil caduca en silencio.** Un perfil viejo no rompe nada, simplemente deja de cubrir el
  código nuevo, y nada avisa. Se mitiga escribiéndolo aquí y en el roadmap: relanzar el workflow al
  añadir pantallas o al subir Compose.
- **La grabación depende de los textos de la barra inferior.** `openDestination` busca "Scan",
  "History" y "Settings" por su etiqueta. Si esos textos cambian, la grabación se queda en el
  arranque en vez de fallar ruidosamente. Queda dicho en el propio generador.
- **Un emulador en CI es lento y puede colgarse.** Por eso el workflow lleva `timeout-minutes: 60` y
  no el margen por defecto de seis horas.

## Alternativas descartadas

| Alternativa | Motivo |
|---|---|
| No tener baseline profile | El coste del primer arranque en Compose es real y medible; no tenerlo es una decisión también, y peor |
| Generarlo en cada PR dentro de `Verify` | Un cuarto de hora por cambio para producir un archivo que casi siempre sale igual |
| Generarlo sobre un dispositivo enchufado | El perfil cambiaría según quién lance la tarea; no es reproducible |
| Escribir el perfil a mano | Se puede, y es adivinar: la lista real tiene miles de entradas y cambia con cada versión de Compose |
| Confiar solo en los *cloud profiles* de Play | Llegan después, se construyen con datos de usuarios reales y no cubren el primer arranque de la primera versión, que es justo el que se quiere arreglar |
