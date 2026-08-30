# Security policy

**English · [Castellano](#política-de-seguridad)**

## What counts as a security issue here

WhyScan has no account, no server and no network access, so the usual attack surface is mostly
absent. What remains is the part that matters most: **the data on the device**.

Treat any of these as a security issue, and report it privately:

- Anything that could let scanned values leave the device. That includes paths the application does
  not control — a system backup, a device-to-device transfer, an exported file landing somewhere
  world-readable.
- The `INTERNET` permission or a network client appearing in a build, whether declared by the
  application or merged in from a dependency.
- History readable by another application, or written outside application-private storage.
- Camera access continuing after a session ends.
- A dependency with a known vulnerability that reaches a shipped artefact.
- Anything that makes the privacy claims in the README, in Settings → About, or in
  [`docs/legal/privacy.md`](docs/legal/privacy.md) untrue.

That last one is the general rule. **A privacy defect is a security defect** in an application whose
entire promise is that what you scan stays with you.

## How to report

Use GitHub's private vulnerability reporting: **Security → Report a vulnerability** on this
repository. That channel is private and reaches the maintainer directly.

Please do not open a public issue for a vulnerability, and do not report it in a pull request — a
pull request is public from the moment it opens.

Include, as far as you can: what you observed, how to reproduce it, the platform and version, and
what an attacker gains. A proof of concept helps; a working exploit is not required.

## What to expect

This is a single-maintainer project, so the honest commitment is modest and kept rather than
generous and broken:

| Stage | Target |
|---|---|
| Acknowledgement | Within 7 days |
| First assessment | Within 14 days |
| Fix or a stated plan | Depends on severity; you will be told which |

You will be credited in the release notes unless you prefer otherwise. If a report turns out not to
be a vulnerability, you will get the reasoning rather than silence.

## Supported versions

The latest release on the default branch. There are no long-term support branches.

## Scope

**In scope:** the application, the modules in this repository, the CI workflows, and the build
configuration.

**Out of scope:** vulnerabilities in Android, iOS, browsers or third-party libraries themselves —
report those upstream. Also out of scope: findings from automated scanners submitted without a
demonstrated impact on this application.

---

# Política de seguridad

## Qué cuenta aquí como problema de seguridad

WhyScan no tiene cuenta, ni servidor, ni acceso a la red, así que la superficie de ataque habitual
casi no existe. Lo que queda es lo que más importa: **los datos del dispositivo**.

Trata como problema de seguridad —y repórtalo en privado— cualquiera de estas cosas:

- Todo lo que permita que los valores escaneados salgan del dispositivo. Incluye caminos que la app
  no controla: una copia de seguridad del sistema, una transferencia entre dispositivos, un archivo
  exportado que acabe en un sitio legible por cualquiera.
- El permiso `INTERNET` o un cliente de red apareciendo en una build, lo declare la app o lo aporte
  una dependencia al fusionar manifiestos.
- Historial legible por otra aplicación, o escrito fuera del almacenamiento privado de la app.
- Acceso a la cámara que siga vivo después de terminar la sesión.
- Una dependencia con vulnerabilidad conocida que llegue a un artefacto publicado.
- Cualquier cosa que haga falsas las promesas de privacidad del README, de Ajustes → Acerca de o de
  [`docs/legal/privacidad.md`](docs/legal/privacidad.md).

Esa última es la regla general. **Un defecto de privacidad es un defecto de seguridad** en una app
cuya promesa entera es que lo que escaneas se queda contigo.

## Cómo reportar

Usa el reporte privado de GitHub: **Security → Report a vulnerability** en este repositorio. Ese
canal es privado y llega directo al mantenedor.

No abras un issue público para una vulnerabilidad, ni la reportes en un pull request: un pull
request es público desde el segundo en que se abre.

Incluye, en la medida en que puedas: qué observaste, cómo reproducirlo, la plataforma y la versión, y
qué gana quien lo explote. Una prueba de concepto ayuda; un exploit funcionando no hace falta.

## Qué esperar

Esto lo mantiene una sola persona, así que el compromiso es modesto y se cumple, en lugar de generoso
y roto:

| Etapa | Plazo |
|---|---|
| Acuse de recibo | 7 días |
| Primera valoración | 14 días |
| Arreglo o plan explícito | Según la gravedad; se te dice cuál |

Se te acredita en las notas de la versión salvo que prefieras que no. Si resulta que no es una
vulnerabilidad, recibirás el razonamiento y no el silencio.

## Versiones cubiertas

La última publicada en la rama principal. No hay ramas de soporte prolongado.

## Alcance

**Dentro:** la aplicación, los módulos de este repositorio, los workflows de CI y la configuración
del build.

**Fuera:** vulnerabilidades de Android, iOS, los navegadores o las bibliotecas de terceros en sí —
esas van a su propio proyecto. También quedan fuera los hallazgos de escáneres automáticos enviados
sin demostrar impacto sobre esta aplicación.
