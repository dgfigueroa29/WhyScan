# Política de privacidad de WhyScan

**Última actualización: 27 de agosto de 2026.**

WhyScan es un lector de códigos de barras y QR. Esta política explica qué datos maneja, dónde se
quedan y qué puede hacer con ellos quien lo use. Está escrita para que se pueda comprobar: cada
afirmación de aquí se corresponde con algo que se puede verificar en el código fuente de la app, y
donde hay un matiz, está dicho.

## Lo corto

**WhyScan no recoge, no guarda en ningún servidor y no transmite nada de lo que escaneás.** No hay
cuenta, no hay registro, no hay publicidad, no hay analítica y no hay informes de fallos.

La app **no pide el permiso de internet**. En Android eso no es una promesa nuestra: es el sistema
operativo el que impide que un proceso sin ese permiso abra una conexión de red. Se puede comprobar
desde el propio teléfono, en la lista de permisos que muestran los ajustes del sistema.

Un detalle, por si mirás el código y te confunde: el manifiesto sí menciona `INTERNET` — con
`tools:node="remove"`, que es la instrucción que lo **quita**. Está ahí porque las librerías de
escaneo de Google declaran ese permiso en sus propios manifiestos y la build de Android los fusiona
con el de la app salvo que se le diga que no. Hasta el 31 de agosto de 2026 nadie se lo decía, así
que las builds anteriores a esa fecha sí llevaban el permiso. Lo encontró una comprobación escrita
justo para ese caso, y la decisión está en el
[ADR-0020](../adr/ADR-0020-el-permiso-de-internet-se-quita-no-solo-se-omite.md).

## Qué datos existen, y dónde

### Lo que escaneás

Cada lectura queda guardada en el **historial**, que es una base de datos dentro del
almacenamiento privado de la app, en tu dispositivo. De cada lectura se guarda:

- el contenido del código, tal cual venía;
- el formato (QR, EAN-13, Code 128…);
- el momento en que se leyó y qué motor de lectura lo leyó;
- la nota que le hayas escrito, si le escribiste alguna.

Ese contenido puede ser cualquier cosa que quepa en un código, **incluida información sensible**: un
QR de WiFi lleva la contraseña de la red dentro, y un código de una entrada o de un envío puede
identificarte. Se guarda porque el historial es la función; no sale de ahí.

La **copia de seguridad automática del sistema está desactivada** (`allowBackup="false"` y
`dataExtractionRules`). Esto importa más de lo que parece: sin esa desactivación, Android copiaría
la base de datos del historial a la cuenta de Google Drive del usuario, y lo haría un proceso del
sistema, para el que nuestra ausencia de permiso de internet no significa nada.

La contrapartida está dicha y aceptada: **al cambiar de teléfono, el historial no viaja.** Si querés
conservarlo, exportalo vos.

### La cámara

WhyScan pide permiso de cámara para leer códigos. Las imágenes se analizan **en el dispositivo**,
fotograma a fotograma, y no se guardan en ninguna parte: la app no toma fotos, no graba vídeo y no
conserva ningún fotograma después de analizarlo.

Si preferís no dar el permiso, la app sigue sirviendo: podés escanear una imagen que ya tengas o
escribir un código a mano.

### Las imágenes que elegís

Al escanear desde una imagen, la app usa el selector del sistema —el *photo picker* de Android—, que
corre fuera de la app y solo le entrega el archivo que elegiste. Por eso WhyScan **no pide permiso
para leer tus fotos**: no tiene acceso a tu galería, solo a lo que le pasás. La imagen se decodifica
y se descarta; no se copia ni se guarda.

### Tus ajustes

El tema, el idioma, el modo dislexia y el modo avanzado se guardan en el almacenamiento privado de
la app. Nada más.

## Terceros

Aquí está el único matiz de todo el documento, y por eso va con nombre y apellido.

Uno de los motores de lectura de WhyScan en Android es el **escáner de códigos de Google Play
Services** (*Google Code Scanner*). Cuando se usa ese motor, la cámara la abre Play Services —no
WhyScan— en su propia pantalla, y lo único que vuelve a la app es el texto del código leído. Ese
componente es de Google y su tratamiento de datos se rige por la [política de privacidad de
Google](https://policies.google.com/privacy).

WhyScan no le envía nada por su cuenta y no recibe de él más que el resultado de la lectura. Si
preferís no usarlo, en **Ajustes → Avanzado** podés activar el banco de motores y elegir a mano
cualquiera de los que se ejecutan enteros dentro de la app.

Fuera de eso, WhyScan **no integra ningún SDK de analítica, publicidad, atribución ni informe de
fallos**.

## Cuándo sale algo del dispositivo

Solo cuando vos lo mandáis, y siempre con un gesto explícito:

- **Copiar** pone el contenido en el portapapeles del sistema. WhyScan lo marca como sensible para
  que Android no lo muestre en la previsualización flotante que enseña encima de cualquier app.
- **Compartir** abre la hoja del sistema y vos elegís a dónde va. A partir de ahí manda la app que
  elijas, no esta.
- **Exportar** el historial a CSV, JSON o texto escribe un archivo donde vos digas.
- **Abrir** un enlace de un código escaneado te lleva al navegador o a la app que corresponda.
  WhyScan solo abre `http`, `https`, `mailto`, `tel`, `sms` y `geo`; cualquier otro esquema se
  rechaza. Lo que pase después ocurre en esa otra app y bajo su política, no la nuestra.

## Tus derechos, en la práctica

No hace falta escribirnos para ejercerlos, porque no tenemos nada tuyo:

- **Ver** tus datos: están en la pantalla de Historial.
- **Exportarlos**: el botón de exportar, en esa misma pantalla.
- **Borrar uno**: el botón de eliminar de cada fila, con opción de deshacer.
- **Borrarlos todos**: "Borrar" en la barra del historial. Es definitivo y no hay copia en ninguna
  parte.
- **Borrarlo todo del todo**: desinstalar la app. Se lleva la base de datos y los ajustes con ella.

## Menores

WhyScan no está dirigida a menores de 13 años y no recoge datos de nadie, de ninguna edad.

## Cambios en esta política

Si alguna vez cambia lo que la app hace con los datos, este documento cambia con ello y la fecha de
arriba lo dice. El historial de cambios es público: vive en el mismo repositorio que el código.

## Contacto

Para cualquier pregunta sobre privacidad, o para ejercer los derechos de la sección anterior,
escribí a **<david@faro.net.ar>**.

WhyScan es además un proyecto de código abierto: si lo tuyo es un defecto y no un dato personal,
una incidencia pública en <https://github.com/dgfigueroa29/WhyScan/issues> llega antes y le sirve a
más gente. Lo que **no** va en una incidencia pública es nada que te identifique.
