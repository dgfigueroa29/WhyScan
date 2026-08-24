package com.whyscan.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Escala tipográfica de WhyScan.
//
// Hasta ahora no había ninguna: `MaterialTheme` se quedaba con la de fábrica, que está pensada para
// el catálogo de Material y no para esta app. Dos consecuencias concretas que se veían en pantalla:
// los títulos venían en `Normal` donde esta app quiere `SemiBold`, y el `bodyMedium` con el que se
// pinta **el valor de un código leído** traía `letterSpacing` positivo, que es lo peor posible para
// una tirada de dígitos que alguien va a comparar a ojo con la etiqueta que tiene delante.

/**
 * La familia del sistema, a conciencia, y no una fuente de marca empaquetada.
 *
 * Roboto en Android, San Francisco en iOS y la del navegador en Web ya están optimizadas para cada
 * plataforma, pesan cero en el binario y respetan los ajustes de accesibilidad del usuario.
 * Empaquetar una fuente propia es una decisión de marca que cuesta unos 300 KB por peso y que
 * conviene tomar con la ficha de Play delante, no de pasada.
 */
private val BodyFontFamily = FontFamily.Default

private fun whyScanStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    letterSpacing: Double,
    family: FontFamily = BodyFontFamily,
): TextStyle = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/**
 * Monoespaciada para datos: el valor de un código y las latencias.
 *
 * Que `1`, `l` e `I` se distingan no es estética. Quien escanea un lote compara lo que ve en
 * pantalla con lo que hay impreso en la caja, y en una proporcional esos tres glifos se parecen.
 */
val MonoNumbers: FontFamily = FontFamily.Monospace

/**
 * Estilo de los valores leídos. No está en [Typography] porque no es un rol de Material: es un
 * estilo de dominio, y meterlo en `bodyMedium` obligaría a que **todo** el cuerpo fuese mono.
 *
 * Se lee a través de [LocalCodeValueStyle] y no directamente, porque el modo dislexia también lo
 * ajusta y una pantalla no tiene por qué saber si ese modo está encendido.
 */
val CodeValueStyle: TextStyle = whyScanStyle(
    size = 16,
    lineHeight = 24,
    weight = FontWeight.Medium,
    letterSpacing = 0.0,
    family = MonoNumbers,
)

/**
 * El estilo del valor de un código, tal y como lo deja el tema en vigor.
 *
 * Es un `CompositionLocal` y no una constante importada porque el modo dislexia cambia el mismo
 * dato en dos sitios —la escala tipográfica y este estilo— y con una constante suelta las pantallas
 * se habrían quedado con la versión sin ajustar.
 */
val LocalCodeValueStyle = staticCompositionLocalOf { CodeValueStyle }

/**
 * La misma escala, ajustada para leerse con menos esfuerzo.
 *
 * ## Qué cambia, y por qué esas tres cosas
 *
 * **Más espacio entre letras.** Es lo único de todo esto con evidencia sólida: separar las letras
 * mejora la velocidad y la precisión de lectura en personas con dislexia sin ningún entrenamiento
 * previo (Zorzi et al., PNAS 2012). Además se **eliminan los tracking negativos** de los estilos
 * grandes, que existen por estética tipográfica y aprietan justo lo que aquí conviene separar.
 *
 * **Más aire entre líneas.** Una interlínea generosa reduce el salto de renglón equivocado, que es
 * uno de los errores que más cuesta al releer.
 *
 * **Texto más grande.** No sustituye al ajuste de tamaño de fuente del sistema —que la app respeta
 * porque usa `sp`— sino que se suma a él, para quien lo necesita en esta app y no en todas.
 *
 * ## Y por qué **no** empaqueta una fuente "para dislexia"
 *
 * Es lo que primero se espera de un modo así, y conviene decir por qué no está. Los estudios
 * controlados sobre OpenDyslexic y Dyslexie **no encuentran mejora** frente a una sans-serif normal
 * bien espaciada: lo que ayudaba en esas pruebas era el espaciado que esas fuentes traen de fábrica,
 * no la forma de sus letras (Kuster et al., 2018; Wery y Diliberto, 2017). Empaquetar una fuente
 * costaría unos 300 KB por peso y prometería algo que la evidencia no sostiene, así que se aplica
 * directamente lo que sí funciona.
 *
 * Lo que sí se hace es **fijar `SansSerif` explícitamente** en vez de dejar `Default`: quita de en
 * medio cualquier fuente con serifas o condensada que una plataforma pudiera elegir por su cuenta.
 * Si algún día se decide empaquetar una fuente, entra por [family] y no toca nada más.
 */
private fun TextStyle.forEasierReading(family: FontFamily): TextStyle {
    val grownSize = fontSize.value * SIZE_FACTOR

    return copy(
        fontFamily = family,
        fontSize = grownSize.sp,
        // La interlínea se recalcula sobre el tamaño ya crecido y con una proporción fija, en vez de
        // escalar la que había: los estilos de display venían con proporciones apretadas a propósito,
        // y multiplicarlas habría conservado justo lo que este modo quiere deshacer.
        lineHeight = (grownSize * LINE_HEIGHT_RATIO).sp,
        letterSpacing = (letterSpacing.value.coerceAtLeast(NO_TRACKING) + EXTRA_LETTER_SPACING).sp,
    )
}

/** La escala tipográfica del tema, normal o ajustada. */
internal fun whyScanTypography(easierReading: Boolean): Typography =
    if (easierReading) WhyScanTypography.forEasierReading() else WhyScanTypography

/** El estilo del valor de un código, normal o ajustado. Sigue siendo monoespaciado siempre. */
internal fun codeValueStyle(easierReading: Boolean): TextStyle =
    if (easierReading) {
        // La monoespaciada **no** se negocia ni en este modo: el valor de un código se coteja
        // carácter a carácter contra una etiqueta impresa, y en proporcional `1`, `l` e `I` se
        // parecen. Cambiarla por legibilidad de prosa haría el dato menos legible como dato.
        CodeValueStyle.forEasierReading(MonoNumbers)
    } else {
        CodeValueStyle
    }

private fun Typography.forEasierReading(): Typography = Typography(
    displayLarge = displayLarge.forEasierReading(ReadingFontFamily),
    displayMedium = displayMedium.forEasierReading(ReadingFontFamily),
    displaySmall = displaySmall.forEasierReading(ReadingFontFamily),
    headlineLarge = headlineLarge.forEasierReading(ReadingFontFamily),
    headlineMedium = headlineMedium.forEasierReading(ReadingFontFamily),
    headlineSmall = headlineSmall.forEasierReading(ReadingFontFamily),
    titleLarge = titleLarge.forEasierReading(ReadingFontFamily),
    titleMedium = titleMedium.forEasierReading(ReadingFontFamily),
    titleSmall = titleSmall.forEasierReading(ReadingFontFamily),
    bodyLarge = bodyLarge.forEasierReading(ReadingFontFamily),
    bodyMedium = bodyMedium.forEasierReading(ReadingFontFamily),
    bodySmall = bodySmall.forEasierReading(ReadingFontFamily),
    labelLarge = labelLarge.forEasierReading(ReadingFontFamily),
    labelMedium = labelMedium.forEasierReading(ReadingFontFamily),
    labelSmall = labelSmall.forEasierReading(ReadingFontFamily),
)

/** Sin serifas y sin condensar, dicho explícitamente y no heredado de lo que traiga la plataforma. */
private val ReadingFontFamily: FontFamily = FontFamily.SansSerif

private const val SIZE_FACTOR = 1.15f
private const val LINE_HEIGHT_RATIO = 1.75f
private const val EXTRA_LETTER_SPACING = 0.75f

/** El suelo del tracking en este modo: los negativos de los estilos grandes se descartan. */
private const val NO_TRACKING = 0f

internal val WhyScanTypography = Typography(
    // Display: solo la usan los estados vacíos y la pantalla "acerca de". Apretada de tracking,
    // que es lo que hace que un texto grande parezca diseñado y no ampliado.
    displayLarge = whyScanStyle(size = 57, lineHeight = 64, weight = FontWeight.SemiBold, letterSpacing = -0.5),
    displayMedium = whyScanStyle(size = 45, lineHeight = 52, weight = FontWeight.SemiBold, letterSpacing = -0.4),
    displaySmall = whyScanStyle(size = 36, lineHeight = 44, weight = FontWeight.SemiBold, letterSpacing = -0.3),

    headlineLarge = whyScanStyle(size = 32, lineHeight = 40, weight = FontWeight.SemiBold, letterSpacing = -0.2),
    headlineMedium = whyScanStyle(size = 28, lineHeight = 36, weight = FontWeight.SemiBold, letterSpacing = -0.2),
    headlineSmall = whyScanStyle(size = 24, lineHeight = 32, weight = FontWeight.SemiBold, letterSpacing = -0.1),

    // Title: cabeceras de sección y título de la barra superior. `SemiBold` y no `Medium`: con el
    // peso de Material la jerarquía entre un título de sección y el cuerpo casi no se leía.
    titleLarge = whyScanStyle(size = 22, lineHeight = 28, weight = FontWeight.SemiBold, letterSpacing = 0.0),
    titleMedium = whyScanStyle(size = 17, lineHeight = 24, weight = FontWeight.SemiBold, letterSpacing = 0.1),
    titleSmall = whyScanStyle(size = 15, lineHeight = 20, weight = FontWeight.SemiBold, letterSpacing = 0.1),

    // Body: prosa. `letterSpacing` a cero o casi; el 0.5 de Material está pensado para Roboto a
    // tamaños pequeños y aquí solo separaba las palabras sin ganar nada.
    bodyLarge = whyScanStyle(size = 16, lineHeight = 24, weight = FontWeight.Normal, letterSpacing = 0.0),
    bodyMedium = whyScanStyle(size = 14, lineHeight = 20, weight = FontWeight.Normal, letterSpacing = 0.1),
    bodySmall = whyScanStyle(size = 13, lineHeight = 18, weight = FontWeight.Normal, letterSpacing = 0.1),

    // Label: botones, chips y metadatos. Aquí el tracking positivo **sí** ayuda: son textos cortos
    // en mayúscula o casi, donde separar las letras mejora la lectura de golpe.
    labelLarge = whyScanStyle(size = 14, lineHeight = 20, weight = FontWeight.SemiBold, letterSpacing = 0.1),
    labelMedium = whyScanStyle(size = 12, lineHeight = 16, weight = FontWeight.Medium, letterSpacing = 0.4),
    labelSmall = whyScanStyle(size = 11, lineHeight = 16, weight = FontWeight.Medium, letterSpacing = 0.4),
)
