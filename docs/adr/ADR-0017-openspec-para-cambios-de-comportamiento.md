# ADR-0017 — Un cambio de comportamiento se propone en `openspec/` antes de escribirse

- **Estado:** Aceptada
- **Fecha:** 2026-08-30

## Contexto

La documentación de este proyecto contesta tres preguntas y le falta una cuarta:

| Documento | Contesta |
|---|---|
| `docs/SDD.md` | **Cómo** está construido |
| `docs/adr/` | **Por qué** se decidió así |
| `docs/ROADMAP.md` | **Cuándo**, y qué queda |
| *(nada)* | **Qué** hace el sistema, dicho como requisitos comprobables |

El SDD se acerca, pero no es eso: describe estructura, tipos y contratos, y para saber si una
conducta concreta sigue siendo cierta hay que leer el código. Los requisitos formales —RF-04, RNF-03
y los demás— existen como identificadores repartidos por el texto, sin un sitio donde consultarlos ni
nada que compruebe que se siguen cumpliendo.

Eso ya ha costado. La casilla de la deuda D20 estuvo abierta meses después de que el trabajo
estuviera hecho, "porque lo mismo se apuntaba en dos sitios". Y el suelo de cobertura del §13.1 se
prometía desde hacía tiempo **sin que nada lo midiera**: cincuenta y un archivos de test y nadie
sabía el número.

Y hay una razón nueva, que es la que empuja de verdad. Cuando quien escribe el código es un agente,
el punto de control barato se mueve hacia adelante: una petición de una línea produce algo
plausible en minutos, y revisarlo después cuesta más que haber acordado antes qué tenía que pasar a
ser cierto. La revisión tiene que ocurrir **cuando cambiar de idea todavía es gratis**.

## Decisión

Todo cambio que altere **comportamiento observable**, añada o quite una capacidad, mueva una
frontera de módulos o cambie el esquema de persistencia o el formato de exportación se propone
primero bajo `openspec/changes/<id>/`, siguiendo la convención de
[OpenSpec](https://github.com/Fission-AI/OpenSpec):

- `proposal.md` — el problema con evidencia, qué cambia, qué **no** cambia, y cómo se verifica.
- `tasks.md` — pasos ordenados y marcables, cada uno un commit coherente.
- `design.md` — solo si hay un compromiso real que decidir.
- `specs/<capacidad>/spec.md` — el delta, con `## ADDED`, `## MODIFIED` o `## REMOVED Requirements`.

`openspec/specs/` guarda la verdad actual: lo que el sistema hace hoy, en requisitos `SHALL` con
escenarios `WHEN`/`THEN`. Un cambio no termina cuando entra el código: termina cuando su delta se
integra en `specs/` y la propuesta se archiva **tal cual** en `changes/archive/`.

**No hace falta propuesta** para un arreglo que restaura comportamiento ya documentado, un
renombrado, un refactor, un test sobre conducta existente o una corrección de documentación. Pedir
una propuesta para un arreglo de una línea es burocracia; saltársela para una capacidad nueva es cómo
un directorio de especificaciones acaba siendo decorado.

**Todo requisito tiene que responder a una pregunta**: qué lo demuestra, y ¿eso se ejecuta en cada
pull request? Con las respuestas de este proyecto: un test de JVM, sí; uno instrumentado, **no
existe** (deuda D6), así que el requisito se replantea para que algo en la JVM lo pruebe o se
reconoce bloqueado por hardware y se va a la lista del ROADMAP, no a un cambio en curso.

La estructura la valida `tools/checks.py` sin red y sin compilador, y CI ejecuta ese mismo script.
La CLI de OpenSpec entiende este formato, pero **nada aquí depende de ella**: el entorno de
desarrollo no tiene red.

## Consecuencias

- Los requisitos dejan de ser identificadores sueltos por el SDD y pasan a ser texto comprobable con
  escenarios concretos. Lo que estaba implícito —que `MANUAL_INPUT` cierra siempre la cadena, que la
  poda no se lleva lo anotado, que el comparador **no** suprime repeticiones— queda escrito donde se
  puede contrastar.
- La pregunta de verificación se hace **antes** de implementar. Es la única pieza que ataca de
  frente el fallo más caro de este repositorio: dar por probado lo que no se pudo probar.
- **Un cuarto lugar donde escribir.** Ya había tres, y ahora hay que saber cuándo toca cada uno.
  Está en la tabla de `AGENTS.md` y en `openspec/AGENTS.md`, y aun así es coste real: la primera vez
  que alguien dude, dudará.
- **Un cambio archivado sin integrar deja `specs/` describiendo un sistema que ya no existe**, que es
  peor que no tener especificaciones: una fuente de verdad que miente. Por eso `/spec-apply` es un
  paso del checklist de "terminado" y no una buena costumbre.
- La convención es de una herramienta de terceros. Si OpenSpec cambia de formato o se abandona, lo
  que queda son archivos Markdown con una estructura conocida y un script propio que la valida — el
  coste de salida es reescribir ese script.

## Alternativas descartadas

| Alternativa | Motivo |
|---|---|
| Seguir solo con el SDD | Describe cómo está construido, no qué debe ser cierto. Para saber si una conducta se cumple hay que leer el código, que es justo lo que una especificación evita |
| Requisitos como issues de GitHub | Se cierran y desaparecen de la vista. La verdad actual del sistema no puede vivir en una lista de cosas cerradas, y sin red aquí tampoco se consultan |
| Un ADR por cada cambio de comportamiento | Un ADR registra una decisión con alternativa y coste. Añadir una columna a la exportación no es eso, y hacerlo diluiría los diecisiete que sí lo son |
| Tests como única especificación | Un test dice que algo funciona hoy; no dice qué se prometió ni por qué. Y aquí hay conductas que ningún test cubre sin dispositivo: la especificación tiene que poder nombrarlas y decir que están descubiertas |
| Escribir la propuesta después de implementar | Es documentación, no especificación. El valor entero está en decidir el alcance y la verificación cuando cambiar de idea todavía es gratis |
| Depender de la CLI de OpenSpec para validar | El entorno de desarrollo no tiene red, así que la validación no se ejecutaría donde hace falta. Un script propio en `tools/checks.py` corre en segundos y ya está en CI |
