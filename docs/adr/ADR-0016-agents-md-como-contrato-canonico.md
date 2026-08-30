# ADR-0016 — `AGENTS.md` es el contrato canónico para agentes, y `CLAUDE.md` su espejo

- **Estado:** Aceptada
- **Fecha:** 2026-08-30

## Contexto

Buena parte de este repositorio la ha escrito un agente leyendo `CLAUDE.md`, y hasta ahora ese
archivo era la única fuente de reglas: prioridad de plataformas, qué se puede verificar aquí, cómo
se escribe. Funcionó mientras la herramienta fue una sola.

Ya no lo es. `AGENTS.md` se ha asentado como el archivo que leen las herramientas de agentes en
general, y este proyecto es **público**: lo abre gente que usa otra cosa, y quien no encuentra las
reglas en el sitio esperado trabaja sin ellas. Un archivo con nombre de producto es además una mala
señal en un repositorio que quiere enseñar cómo se trabaja con agentes, no con uno concreto.

Hay dos tensiones de verdad, y son las que decide este ADR:

**El idioma.** El repositorio está en castellano por decisión del dueño, y esa decisión no está en
discusión para el código ni para `docs/`. Pero un contrato para agentes es texto que interpretan
modelos y herramientas, y ahí el inglés es lo que se procesa con menos ambigüedad. Escribir las
reglas en castellano tenía un coste que no se ve: cada instrucción sutil —"lo mínimo y se dice",
"poder hacerlo no lo convierte en prioridad"— viaja peor.

**La duplicación.** Tener el contrato en dos archivos garantiza que un día digan cosas distintas.
Eso es exactamente lo que este proyecto no tolera en `docs/`, donde hay tests que impiden que
`ENGINES.md` y el catálogo se separen.

## Decisión

**`AGENTS.md`, en la raíz y en inglés, es el contrato normativo.** Contiene las reglas completas: las
inquebrantables, el mapa del repositorio, la matriz de lo que se puede verificar aquí, el ciclo de
trabajo, la tabla de dónde va cada cosa, el checklist de "terminado" y los frenos de mano.

**`CLAUDE.md` es su espejo en castellano y no es normativo.** Recoge las reglas de cabecera con la
voz del dueño del proyecto, para quien prefiera leerlas en su idioma, y lo dice en su primera línea:
donde los dos discrepen, manda `AGENTS.md`.

La regla de idioma que se deriva, y que queda escrita en los dos archivos: **el código y `docs/` en
castellano; las superficies que leen los agentes —`AGENTS.md`, `.claude/`, `openspec/`, `docs/ai/`—
en inglés; las guías para personas, en los dos.**

La divergencia se ataja con dos cosas, no con buena voluntad:

- `CLAUDE.md` se queda en un resumen corto. Cuanto menos texto duplicado, menos superficie donde
  separarse.
- `tools/checks.py` comprueba que los dos archivos existen y se enlazan entre sí. Es poco —no
  compara el contenido, que no se puede— pero cierra el caso real: que alguien mueva o borre uno y
  el otro quede apuntando al vacío.

## Consecuencias

- Cualquier herramienta de agentes que abra este repositorio encuentra las reglas donde espera, sin
  que nadie se las tenga que explicar en el prompt.
- **Hay dos archivos que pueden separarse, y ninguna comprobación lee lo que dicen.** Es el coste
  aceptado. La mitigación es que uno de los dos es explícitamente el resumen del otro, así que la
  discrepancia tiene una respuesta obvia en lugar de convertirse en una discusión.
- El dueño del proyecto lee sus propias reglas en castellano y las mantiene en castellano. Quien las
  cambie tiene que tocar los dos archivos en el mismo commit — está escrito en `AGENTS.md` como
  parte del checklist.
- Aparece una asimetría de idioma dentro del repositorio que hay que saber leer: un `.md` en inglés
  bajo `docs/ai/` no es un descuido, es la regla.

## Alternativas descartadas

| Alternativa | Motivo |
|---|---|
| Dejar solo `CLAUDE.md` | Un repositorio público que quiere enseñar cómo se trabaja con agentes no puede tener las reglas en un archivo con nombre de producto. Quien use otra herramienta trabaja sin ellas |
| Dejar solo `AGENTS.md` y borrar `CLAUDE.md` | Claude Code carga `CLAUDE.md` automáticamente, y el dueño del proyecto lee y escribe estas reglas en castellano. Quitarlo cambia dos cosas a la vez y no arregla ninguna |
| `CLAUDE.md` como enlace de una línea a `AGENTS.md` | Elimina la duplicación y también la utilidad: el archivo que se carga primero pasaría a no decir nada. Las tres reglas que más veces se incumplen —Android primero, aquí no compila nada, no digas que probaste lo que no probaste— tienen que estar donde el agente ya está mirando |
| Todo en castellano, incluido `AGENTS.md` | Es el idioma del proyecto, pero no el de las herramientas. Las instrucciones matizadas, que son las que más importan, se interpretan peor |
| Todo en inglés, `docs/` incluido | Reescribir doscientas mil palabras de `SDD.md` y `ROADMAP.md` que ya son fuente de verdad, para servir a una herramienta. El coste no guarda ninguna proporción con el beneficio |
| Generar `CLAUDE.md` desde `AGENTS.md` con un script | Cierra la divergencia y abre un traductor automático dentro del repositorio. Ni la traducción sale bien sin criterio, ni la voz del dueño sobrevive a una plantilla |
