package com.whyscan.core.platform

/**
 * Los esquemas que este producto abre. Todo lo demás se rechaza en el borde.
 *
 * La lista no es arbitraria: es exactamente lo que puede producir `ResultActionsFactory` del
 * dominio, uno por cada `OpenKind`. `http`/`https` para un enlace, `mailto` para un correo, `tel`
 * para llamar, `sms` para un mensaje y `geo` para un punto en el mapa.
 */
private val ALLOWED_SCHEMES = setOf("http", "https", "mailto", "tel", "sms", "geo")

/**
 * Si [uri] se puede entregar al sistema para que lo abra.
 *
 * ### Por qué existe, si el dominio ya filtra
 * Porque filtrar en el dominio y abrir en la plataforma son dos sitios distintos, y hoy lo único
 * que impide que llegue aquí un `javascript:`, un `intent://` o un `content://` es que **nadie
 * llama a `openUrl` de otro modo**. Eso es una propiedad del grafo de llamadas, no del método: se
 * cumple hoy y deja de cumplirse el día que alguien añada un camino nuevo, sin que nada avise.
 *
 * En un lector de códigos esa distinción no es teórica. El atacante controla el contenido **entero**
 * del código y la víctima solo tiene que apuntar la cámara; la lista blanca de esquemas es *la*
 * decisión de seguridad del producto. Una decisión así se comprueba donde se ejecuta, no solo donde
 * se decide.
 *
 * ### Qué se rechaza, y por qué así
 * Se compara el esquema completo contra una lista blanca en vez de buscar prefijos peligrosos: una
 * lista negra hay que acertarla entera y la blanca solo hay que mantenerla al día con lo que el
 * dominio produce — y hay un test que comprueba justamente eso. Falla **cerrado**: sin `:`, con el
 * `:` en la primera posición o con cualquier esquema desconocido, la respuesta es `false`.
 */
fun isOpenableUri(uri: String): Boolean {
    val separator = uri.indexOf(':')
    if (separator <= 0) return false
    return uri.substring(0, separator).lowercase() in ALLOWED_SCHEMES
}
