# openspec/ — spec-driven change

**English below · [Castellano abajo](#castellano)**

This directory holds what the system does today (`specs/`) and what should become true next
(`changes/`). The rules an agent follows are in [`AGENTS.md`](AGENTS.md); the context it needs before
proposing anything is in [`project.md`](project.md). This file is the short human version of both.

## Why it exists

An agent given a one-line request writes something plausible. An agent that first has to write down
*what must become true, and how anyone would know* writes something reviewable — and the review
happens before the code, when changing your mind is free.

Three documents already existed for three different questions: `docs/SDD.md` answers **how** it is
built, `docs/adr/` answers **why** a decision was taken, `docs/ROADMAP.md` answers **when**. What was
missing was **what**, stated as checkable requirements. That is this directory.

## The loop

```
/spec-propose  →  spec-reviewer  →  owner accepts  →  implement tasks.md  →  /spec-apply  →  archive
```

A change is not done when the code merges. It is done when its requirements have been folded into
`specs/` and the proposal has moved to `changes/archive/`, where it stays verbatim as the record of
why the spec reads as it does.

`tools/checks.py` validates the structure offline — required files, delta headings, and that every
requirement carries at least one scenario — and CI runs the same script. The
[OpenSpec](https://github.com/Fission-AI/OpenSpec) CLI understands this layout too, but nothing here
depends on it: the development environment has no network.

---

## Castellano

Este directorio guarda lo que el sistema hace hoy (`specs/`) y lo que debería pasar a ser cierto
(`changes/`). Las reglas para agentes están en [`AGENTS.md`](AGENTS.md) y el contexto previo a
cualquier propuesta, en [`project.md`](project.md). Los dos están en inglés a propósito: son
superficies que leen las herramientas.

### Por qué existe

A un agente con una petición de una línea le sale algo plausible. A un agente obligado a escribir
antes **qué tiene que pasar a ser cierto y cómo se sabría**, le sale algo revisable — y la revisión
ocurre antes del código, cuando cambiar de idea todavía es gratis.

Ya había tres documentos para tres preguntas: `docs/SDD.md` contesta **cómo** está construido,
`docs/adr/` **por qué** se decidió algo, y `docs/ROADMAP.md` **cuándo**. Faltaba **qué**, dicho como
requisitos comprobables. Eso es este directorio.

### El ciclo

`/spec-propose` → revisión → aceptación → implementar `tasks.md` → `/spec-apply` → archivo.

Un cambio no termina cuando el código entra: termina cuando sus requisitos están integrados en
`specs/` y la propuesta se ha movido a `changes/archive/`, donde se queda **tal cual**. Una propuesta
retocada después para que cuadre con lo que se acabó construyendo no le sirve de nada a quien
reabra la pregunta dentro de un año.

`tools/checks.py` valida la estructura sin red y sin compilador, y CI ejecuta ese mismo script.
