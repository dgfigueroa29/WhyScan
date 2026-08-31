# ADR-0020 — El permiso de internet se **quita**, no solo se omite

- **Estado:** Aceptada
- **Fecha:** 2026-08-31
- **Corrige:** la garantía de privacidad (RNF-03), que era falsa en todos los APK producidos hasta
  hoy
- **Resuelve:** el hueco que la auditoría del 30-08-2026 marcó como el mayor de la garantía — una
  dependencia que aporta `INTERNET` al fusionar manifiestos

## Contexto

La app le dice al usuario, en la pantalla de Ajustes y en la de permiso de cámara, en los dos
idiomas, que **no pide permiso de internet**. El README lo repite, la política de privacidad lo
repite y el SDD §12 lo llama "la garantía más fuerte". `androidApp/src/main/AndroidManifest.xml`
nunca declaró `INTERNET`, y una comprobación de `tools/checks.py` lo vigilaba desde hace meses.

Era falso.

El 31-08-2026, la primera vez que `tools/merged_manifest.py` llegó a ejecutarse en CI —hasta
entonces el job de Android nunca alcanzaba ese paso—, leyó el manifiesto **fusionado** de la build
de debug y encontró esto:

```
android.permission.ACCESS_NETWORK_STATE
android.permission.CAMERA
android.permission.INTERNET
ar.net.faro.whyscan.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
```

Los dos primeros de la lista no los declara este repositorio. Los aportan los manifiestos de las
dependencias de Google —`play-services-code-scanner` en `:engines:gms-code-scanner`, y
`com.google.mlkit:*` en `:engines:mlkit-camerax` y `:engines:ocr`—, y el fusionador de AGP los
incorpora sin preguntar. Ninguna es una dependencia de solo-debug: **el APK de release lleva lo
mismo**.

La restricción que cierra la puerta obvia: no se puede quitar la dependencia. Los motores de Google
son el producto — son los tres que de verdad leen códigos en Android, y el banco de motores existe
para compararlos.

Es el mismo error de forma que ya costó caro dos veces aquí: con `allowBackup`, el proceso de copia
de seguridad; con D18, un tipo mal registrado. **Auditar lo que hace el código propio y no lo que el
sistema hace con él.**

## Decisión

El manifiesto de la aplicación **retira** el permiso explícitamente:

```xml
<uses-permission
    android:name="android.permission.INTERNET"
    tools:node="remove" />
```

Quitarlo no rompe a los motores de Google porque **el nuestro no es el proceso que descarga nada**:
ML Kit sin empaquetar y el Google Code Scanner delegan la obtención del modelo en la app de Google
Play Services, que corre aparte y tiene su propio permiso. Es exactamente lo que `docs/legal/` ya
decía de esos componentes, ahora leído en la dirección contraria: si la red la pone el sistema, la
app no necesita el permiso.

`ACCESS_NETWORK_STATE` **se queda**. Solo permite leer si hay conexión, no abrir ninguna, así que no
toca la garantía; quitarlo arriesgaría las comprobaciones de disponibilidad de GMS a cambio de nada.

`check_privacy_guarantee()` deja de mirar solo si el permiso está declarado y pasa a **exigir que la
retirada esté presente** en el manifiesto fuente. Borrar esa línea devolvería `INTERNET` al APK en
silencio, que es exactamente como llegó la primera vez.

## Consecuencias

Los textos que la app le enseña al usuario siguen siendo ciertos sin tocar ni una palabra, y esa fue
la razón de elegir esta opción y no la de reescribirlos.

**El coste, y no es pequeño: nadie ha comprobado esto en un teléfono.** Que la descarga del modelo
ocurra íntegramente en el proceso de Play Services es lo documentado y lo que explica el diseño de
esas librerías, pero *documentado* no es *ejecutado*. Si en algún camino GMS necesitara la red
dentro de nuestro proceso, el motor afectado fallaría en un dispositivo sin que ningún CI lo viera:
aquí no hay emulador (D6) y esa comprobación **necesita hardware**. Queda delegada y escrita como
pendiente en el ROADMAP, junto al resto de lo que solo un teléfono puede decidir.

Segundo coste, más barato: el manifiesto tiene ahora una línea que parece decir lo contrario de lo
que hace. De ahí el comentario largo que la acompaña y esta decisión escrita.

Lo que **no** cubre: nada de esto impide que una dependencia futura haga red desde su propio
proceso, ni cubre a las otras tres plataformas, que no tienen manifiesto Android. La garantía en Web
y Desktop se apoya en que no hay cliente HTTP, que es más débil y está dicho así.

## Alternativas descartadas

| Alternativa | Motivo |
|---|---|
| Dejarlo como estaba | La app le mentía al usuario en dos pantallas y en cuatro documentos. No es una opción, es el defecto |
| Reescribir los textos para que digan la verdad | Cambia la promesa por otra más débil y más difícil de explicar —"pedimos internet pero no lo usamos"— cuando la promesa fuerte **se puede sostener**. Se reservaría para el día en que quitarlo rompa algo de verdad |
| Quitar los motores de Google | Son los tres que leen códigos en Android. Es cambiar el producto para arreglar una línea de XML |
| Quitar también `ACCESS_NETWORK_STATE` | No aporta privacidad —no abre conexiones— y arriesga las comprobaciones de disponibilidad de GMS. Riesgo sin beneficio |
| Comprobar solo el manifiesto de release | Es el que se publica, sí, pero las dependencias son las mismas en las dos variantes: habría dado el mismo resultado tres minutos más tarde y con un APK ofuscado de por medio |
