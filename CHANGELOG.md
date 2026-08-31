# Registro de cambios

Formato de [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/), versionado
[semántico](https://semver.org/lang/es/).

> **Este archivo empieza aquí, y no en la Fase 1.** Reconstruir veinticuatro rondas de historia a
> posteriori sería inventarse fechas y agrupaciones que nadie registró en su momento —y este
> proyecto ya tiene un sitio donde esa historia está contada de verdad, con los defectos incluidos:
> [`docs/ROADMAP.md`](docs/ROADMAP.md).
>
> La diferencia entre los dos no es de formato: el ROADMAP cuenta **cómo se llegó**, con lo que se
> rompió por el camino, y este archivo dice **qué cambia para quien usa la app** en cada versión
> publicada. El primero es para quien trabaja en esto; el segundo, para quien la instala.

## [Sin publicar]

Nada publicado todavía. `versionCode = 1` y `versionName = "1.0.0"` siguen sin subir a Play; el
criterio de salida y lo que falta están en `docs/ROADMAP.md`, en "Pendiente para publicar".

Cuando exista la primera versión, esta sección se convierte en `[1.0.0] - AAAA-MM-DD` y se abre una
nueva `[Sin publicar]` encima.

### Cómo se escribe una entrada aquí

En términos de lo que le pasa a una persona con la app instalada, no en términos de módulos:

- **Añadido** — algo que antes no se podía hacer.
- **Cambiado** — algo que se hacía y ahora se hace distinto.
- **Corregido** — algo que estaba mal. Se dice **qué se veía mal**, no qué clase se tocó.
- **Eliminado** — algo que dejó de existir.
- **Seguridad** — cualquier cosa que afecte a lo que sale del dispositivo. Nunca se omite ni se
  suaviza: es la promesa central del producto.

"Refactor de `ScannerViewModel`" no es una entrada. "Salir de la cámara con el gesto atrás vuelve a
funcionar" sí lo es.
