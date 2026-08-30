# ADR-0019 — El `applicationId` identifica a quien publica; los paquetes de Kotlin siguen siendo del producto

- **Estado:** Aceptada
- **Fecha:** 2026-08-30
- **Relacionada:** [ADR-0018](ADR-0018-federar-la-base-y-no-la-marca.md), que decidió federar la base
  del sistema de diseño y dejó abierto el nombre de la organización

## Contexto

WhyScan pasa a publicarse bajo **Faro**, y eso obliga a contestar una pregunta que hasta ahora no
existía: **qué nombre lleva la app en la tienda**.

El `applicationId` no es un detalle de empaquetado. Es la URL de la ficha de Play, la clave con la
que el sistema reconoce una actualización y lo que decide si una instalación es la misma app o una
distinta. **No se puede cambiar después de la primera publicación.** Todavía no hay ninguna, así que
esta es literalmente la última ocasión en que cambiarlo cuesta una línea en lugar de ser imposible.

Hasta ahora era `com.whyscan.app`, elegido cuando el proyecto no tenía editor y el único nombre
disponible era el del producto. El comentario que lo acompañaba decía que coincidir con los paquetes
de Kotlin era una ventaja: un solo nombre que mantener, ninguno que explicar. Eso era cierto — y
deja de serlo en cuanto hay una organización detrás.

Hay además un detalle que parece cosmético y no lo es: `com.faro` **no es un dominio de Faro**.
El dominio es `faro.net.ar`, y el orden inverso de ese dominio es `ar.net.faro`. Usar `com.faro`
sería reclamar un espacio de nombres ajeno, y el día que algo haya que publicar en Maven Central
—que verifica la propiedad del dominio contra el `groupId`— no habría forma de sostenerlo.

## Decisión

**El `applicationId` es `ar.net.faro.whyscan`.** La organización primero, el producto después, en el
orden inverso del dominio que Faro sí posee.

**Los paquetes de Kotlin no se tocan.** Siguen siendo `com.whyscan.*`: los módulos, los `namespace`,
los plugins de convención y los almacenes de datos. Renombrar cientos de archivos no le arregla nada
a nadie, y el `applicationId` no tiene por qué coincidir con ellos — nunca lo exigió Android, lo
hacíamos porque era cómodo.

La regla que queda, y que sirve para lo próximo que se publique: **el espacio de nombres de la
tienda identifica a quien publica; el del código identifica al producto.** Son dos preguntas
distintas y cambian en momentos distintos — el editor puede cambiar, y de hecho acaba de hacerlo,
sin que el producto se entere.

Por coherencia con lo mismo, el grupo Maven de `:core:foundation` (ADR-0018) es **`ar.net.faro`**.

## Consecuencias

- La app se publica bajo la identidad de Faro desde el primer día, sin la migración imposible que
  habría supuesto descubrirlo después de la primera subida.
- **Hay dos espacios de nombres y hay que saber cuál es cuál.** Es el coste, y es exactamente la
  ventaja que el comentario anterior decía tener. Queda escrito donde se ve: en el propio
  `androidApp/build.gradle.kts`, al lado de la línea.
- **`BaselineProfileGenerator` tenía el identificador escrito a mano**, porque instala, lanza y
  concede permisos por `pm grant` usando el `applicationId` y no el paquete del código. Se cambió
  con esto. Es el único acoplamiento que había, y se buscó antes de tocar nada: no hay
  `authorities`, ni `FileProvider`, ni enlaces profundos, ni reglas de R8 que lo nombren.
- Queda pendiente lo de siempre y ahora con el nombre nuevo: **comprobar en Play Console que
  `ar.net.faro.whyscan` está libre**. Sin red en el entorno de desarrollo, eso no se pudo verificar
  aquí.
- El nombre visible de la app en la ficha es una decisión aparte y no la toma este ADR. `WhyScan`
  a secas y `Faro WhyScan` siguen las dos sobre la mesa; a diferencia del `applicationId`, esa se
  puede cambiar después.

## Alternativas descartadas

| Alternativa | Motivo |
|---|---|
| Dejar `com.whyscan.app` | Publicar bajo el nombre del producto cuando ya hay una organización detrás obliga a explicar quién publica, y no se puede arreglar después: el `applicationId` es permanente desde la primera subida |
| `com.faro.whyscan` | Más corto y más habitual, y **reclama un dominio que no es de Faro**. Maven Central verifica la propiedad del dominio contra el `groupId`, así que el día que haya que publicar la base federada no se sostendría. Elegir bien ahora no cuesta nada |
| Renombrar también los paquetes a `ar.net.faro.whyscan.*` | Cientos de archivos tocados, todos los `namespace`, los plugins de convención y los almacenes de datos, en un repositorio donde **no compila nada** para comprobarlo. A cambio de ninguna mejora observable. El dueño del proyecto lo descartó explícitamente |
| Esperar a decidirlo con la ficha de Play delante | Es justo el momento en que ya no se puede. La ficha se crea *con* el `applicationId`, no antes |
| Un `applicationIdSuffix` por variante | Resuelve otro problema —convivir debug y release en el mismo teléfono— y no este. Además rompería el generador del baseline profile, que busca un paquete exacto |
