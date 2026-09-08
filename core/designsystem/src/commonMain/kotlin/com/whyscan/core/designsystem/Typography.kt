package com.whyscan.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ar.net.faro.foundation.LocalCodeValueStyle
import ar.net.faro.foundation.MonoNumbers
import ar.net.faro.foundation.forEasierReading
import ar.net.faro.foundation.textStyle

// Escala tipográfica de WhyScan.

/**
 * Familia monoespaciada para datos: el valor de un código y las latencias.
 *
 * Reexportada desde foundation por compatibilidad.
 */
public val WhyScanMonoNumbers: FontFamily = MonoNumbers

/**
 * Estilo de los valores leídos. No está en [Typography] porque no es un rol de Material: es un
 * estilo de dominio, y meterlo en `bodyMedium` obligaría a que **todo** el cuerpo fuese mono.
 *
 * Se lee a través de [LocalCodeValueStyle] y no directamente, porque el modo dislexia también lo
 * ajusta y una pantalla no tiene por qué saber si ese modo está encendido.
 */
public val CodeValueStyle: TextStyle = textStyle(
    size = 16,
    lineHeight = 24,
    weight = FontWeight.Medium,
    letterSpacing = 0.0,
    family = MonoNumbers,
)

/**
 * El estilo del valor de un código, tal y como lo deja el tema en vigor.
 *
 * Reexportación de compatibilidad — la implementación vive en :core:foundation.
 */
public val LocalCodeValueStyle: ProvidableCompositionLocal<TextStyle> = LocalCodeValueStyle

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

internal val WhyScanTypography: Typography = Typography(
    // Display: solo la usan los estados vacíos y la pantalla "acerca de". Apretada de tracking,
    // que es lo que hace que un texto grande parezca diseñado y no ampliado.
    displayLarge = textStyle(size = 57, lineHeight = 64, weight = FontWeight.SemiBold, letterSpacing = -0.5),
    displayMedium = textStyle(size = 45, lineHeight = 52, weight = FontWeight.SemiBold, letterSpacing = -0.4),
    displaySmall = textStyle(size = 36, lineHeight = 44, weight = FontWeight.SemiBold, letterSpacing = -0.3),

    headlineLarge = textStyle(size = 32, lineHeight = 40, weight = FontWeight.SemiBold, letterSpacing = -0.2),
    headlineMedium = textStyle(size = 28, lineHeight = 36, weight = FontWeight.SemiBold, letterSpacing = -0.2),
    headlineSmall = textStyle(size = 24, lineHeight = 32, weight = FontWeight.SemiBold, letterSpacing = -0.1),

    // Title: cabeceras de sección y título de la barra superior. `SemiBold` y no `Medium`: con el
    // peso de Material la jerarquía entre un título de sección y el cuerpo casi no se leía.
    titleLarge = textStyle(size = 22, lineHeight = 28, weight = FontWeight.SemiBold, letterSpacing = 0.0),
    titleMedium = textStyle(size = 17, lineHeight = 24, weight = FontWeight.SemiBold, letterSpacing = 0.1),
    titleSmall = textStyle(size = 15, lineHeight = 20, weight = FontWeight.SemiBold, letterSpacing = 0.1),

    // Body: prosa. `letterSpacing` a cero o casi; el 0.5 de Material está pensado para Roboto a
    // tamaños pequeños y aquí solo separaba las palabras sin ganar nada.
    bodyLarge = textStyle(size = 16, lineHeight = 24, weight = FontWeight.Normal, letterSpacing = 0.0),
    bodyMedium = textStyle(size = 14, lineHeight = 20, weight = FontWeight.Normal, letterSpacing = 0.1),
    bodySmall = textStyle(size = 13, lineHeight = 18, weight = FontWeight.Normal, letterSpacing = 0.1),

    // Label: botones, chips y metadatos. Aquí el tracking positivo **sí** ayuda: son textos cortos
    // en mayúscula o casi, donde separar las letras mejora la lectura de golpe.
    labelLarge = textStyle(size = 14, lineHeight = 20, weight = FontWeight.SemiBold, letterSpacing = 0.1),
    labelMedium = textStyle(size = 12, lineHeight = 16, weight = FontWeight.Medium, letterSpacing = 0.4),
    labelSmall = textStyle(size = 11, lineHeight = 16, weight = FontWeight.Medium, letterSpacing = 0.4),
)
