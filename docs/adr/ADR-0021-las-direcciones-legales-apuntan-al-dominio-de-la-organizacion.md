# ADR-0021 — Las direcciones legales apuntan al dominio de la organización, no al repositorio

- **Estado:** Aceptada
- **Fecha:** 2026-09-01
- **Relacionada:** [ADR-0019](ADR-0019-el-applicationid-identifica-a-quien-publica.md), que estableció la identidad de Faro y el dominio `faro.net.ar`

## Contexto

La política de privacidad y los términos de uso son documentos vivos que WhyScan debe presentar al usuario en dos idiomas. Hasta ahora, las direcciones (`legal_privacy_url` y `legal_terms_url`) en los catálogos de recursos apuntaban a las versiones en crudo del repositorio en GitHub.

Esto presentaba tres problemas:
1. **Identidad:** Enlazar a GitHub expone la infraestructura de desarrollo al usuario final, en lugar de la marca de la organización que publica la app (Faro).
2. **Requisito de Play Store:** Google Play exige una URL de política de privacidad que sea accesible públicamente y profesional. Una URL de dominio propio (`faro.net.ar`) es la opción preferida y la que mejor transmite confianza.
3. **Mantenibilidad:** Cambiar una coma en el texto legal obligaba a que el enlace en la app (si se usaba un enlace a una rama o commit específico) pudiera quedar desfasado, o a confiar en `main`.

La restricción técnica: la app no tiene permiso de `INTERNET` ([ADR-0020](ADR-0020-el-permiso-de-internet-se-quita-no-solo-se-omite.md)). Sin embargo, abrir una URL externa a través de un `Intent` del sistema hacia el navegador no requiere que la app posea dicho permiso. El navegador es quien accede a la red, no WhyScan.

## Decisión

**Las direcciones legales apuntan a `faro.net.ar`**, el dominio de la organización.

- **Español (Privacidad):** `https://faro.net.ar/whyscan-privacidad`
- **Inglés (Privacy):** `https://faro.net.ar/en/whyscan-privacy`
- **Español (Términos):** `https://faro.net.ar/whyscan-terminos`
- **Inglés (Terms):** `https://faro.net.ar/en/whyscan-terms`

Los documentos locales en `docs/legal/` se mantienen en el repositorio como la **fuente de verdad** que se sincroniza con la web. La garantía de privacidad (RNF-03) se sigue auditando contra estos archivos locales, pero el usuario accede a la versión web para una mejor experiencia y cumplimiento con las tiendas de aplicaciones.

## Consecuencias

- **Mejor percepción de marca:** El usuario ve una dirección propia de Faro.
- **Cumplimiento con Play Store:** Se satisface el requisito de tener una URL de política de privacidad en un dominio profesional.
- **La garantía de privacidad se mantiene intacta:** Abrir el navegador no rompe la promesa de que *la app* no tiene acceso a la red. El usuario es consciente de que sale de la app al navegador.
- **Dependencia de la web de Faro:** Si el sitio cae, el usuario no puede leer los términos desde la app. Se acepta este riesgo porque la disponibilidad de GitHub no es superior a la del dominio propio para este propósito, y porque la alternativa (texto local embebido) aumentaría el tamaño del binario y dificultaría las actualizaciones legales.

## Alternativas descartadas

| Alternativa | Motivo |
|---|---|
| Seguir en GitHub | Menor profesionalismo y no aprovecha la identidad de marca ya establecida en el ADR-0019. |
| Embeber los textos en la app (local) | Aumenta el tamaño del binario y obliga a una actualización de la app en la tienda por cada cambio legal, por pequeño que sea. Además, Play Store sigue pidiendo una URL externa. |
| Usar un dominio de producto (`whyscan.app`) | Coste extra de mantenimiento y renovación de dominio, cuando Faro ya tiene uno que la identifica como organización (coherente con el ADR-0019). |
