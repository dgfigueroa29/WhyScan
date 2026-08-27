# ADR-0015 — Probar un motor abre un diálogo a pantalla completa, no un destino nuevo

- **Estado:** Aceptada
- **Fecha:** 2026-08-27

## Contexto

El banco de motores (ADR-0010) lista las nueve alternativas con su disponibilidad, sus capacidades y
un botón: **"Elegir"**. Pulsarlo guarda una preferencia y devuelve al usuario a la misma lista de
fichas, donde **a la vista no ha cambiado nada**.

Eso deja sin contestar la única pregunta que se hace quien está mirando ese catálogo: *¿qué tal lee
**este**?* La respuesta no está en una tabla de capacidades — está en apuntar la cámara con ese
motor cargado y mirar. El comparador (G5) tampoco la contesta: mide varios motores a la vez y
devuelve números, que es otra pregunta.

Hacía falta, entonces, un "**Probar ahora**" que eligiera el motor, reiniciara la sesión con él y
enseñara el visor. Y ahí aparece el problema de dónde ponerlo.

**La pantalla de escaneo no puede dar una pantalla completa.** Vive dentro del `Scaffold` de la app,
entre la barra de navegación inferior y los insets de la barra de estado. Todo lo que se dibuje ahí
nace con un recorte que la propia pantalla no puede quitarse: el `padding` se lo impone el
`Scaffold`, que es de `App.kt`. Y "pantalla completa" para un visor de cámara no es un adorno — es
la diferencia entre encuadrar un código pequeño y no poder.

## Decisión

El visor de "Probar ahora" es un **`Dialog` con `usePlatformDefaultWidth = false`**, montado desde
la propia feature, y **no** un destino más de la navegación.

- Se pinta por encima del `Scaffold` entero, barra de navegación incluida, sin que
  `:feature:scanner` tenga que negociar nada con la raíz de la app.
- El botón atrás lo cierra, que es el comportamiento por defecto de un diálogo y lo que el usuario
  espera de algo que se abrió encima.
- Respeta los insets por su cuenta (`safeDrawingPadding`), porque al pintarse sobre las barras del
  sistema nadie más lo hace por él.
- Dentro **no hay UI nueva**: es el mismo `ViewfinderArea` —con sus estados, su linterna, su zoom y
  su píldora de sesión— y la misma `DetectionCard` de la hoja de resultados.

Mientras está abierto, el visor de debajo **deja de componer su superficie de cámara**. No es
higiene: dos vistas de preview sobre el mismo motor se pelean por la sesión y una de las dos se
queda en negro. Es un caso con nombre en el `when` de `ViewfinderArea` (`previewMoved`) y no un
`previewEngine = null`, que caería en la rama de "pausada" y sería mentira — la cámara está
encendida, solo que en otro sitio.

Se cierra solo en dos casos, y los dos son "aquí ya no hay nada que mirar":

- **La pantalla deja de verse.** El ViewModel sobrevive a la navegación, así que sin esto volver del
  historial devolvía un visor a pantalla completa —y sin cámara detrás, porque la sesión sí se
  paró— tapando el catálogo que el usuario venía a ver.
- **La sesión termina sin ninguna lectura.** Es el caso de cancelar el Google Code Scanner, que
  abre su propia pantalla: al cerrarla, quedarse delante de un visor vacío le pide al usuario un
  segundo atrás para salir de donde ya quiso salir. Con lecturas se queda abierto: ahí sí hay algo
  que leer y qué hacer con ello.

## Consecuencias

- El catálogo de motores pasa de ser una tabla que se consulta a algo que se **prueba**, que era el
  objetivo G5 dicho en la escala de una persona en lugar de en la de un marcador.
- La feature gana la capacidad de ocupar la pantalla entera sin que la raíz de la app sepa nada. El
  precio es que ese poder existe: cualquier feature puede hacerlo, y nada lo impide salvo criterio.
- `ViewfinderArea` tiene un sexto caso excluyente. Su `when` sigue siendo la garantía contra el
  estado imposible, y ese es justamente el motivo de que el caso nuevo entre en el `when` en vez de
  resolverse por fuera.
- **No aplica al modo básico.** "Probar ahora" solo existe en las fichas del banco de motores, que
  solo se ven con el modo avanzado encendido. Quien abre la app a leer un QR no se entera de que
  esto existe, que es lo correcto.

## Alternativas descartadas

| Alternativa                                              | Motivo                                                                                                                                            |
|----------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| Un destino más en el `Navigator`                         | Aparece en la barra inferior o hay que ocultarlo a mano, y queda en el backstack: un sitio al que se vuelve con el botón atrás desde donde no tiene sentido. Además **sigue dentro del `Scaffold`**, así que no resuelve el recorte, que era el problema |
| Que `App.kt` esconda las barras cuando la feature lo pida | Acopla la raíz de la app a un estado interno de una feature, y obliga a inventar el canal para pedirlo. La raíz pasaría a saber qué es "probar un motor" |
| Ampliar el visor dentro de la pantalla, sin taparlo todo  | Es lo que ya hace la disposición del producto (ADR-0010). No añade nada: el recorte del `Scaffold` sigue ahí                                          |
| Que "Elegir" abriera el visor directamente                | Son dos intenciones distintas: fijar la preferencia para siempre y mirar cómo lee ahora. Juntarlas obliga a abrir la cámara a quien solo quería cambiar de motor |
