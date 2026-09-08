package ar.net.faro.foundation

import androidx.compose.material3.Typography
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Familia monoespaciada del sistema, para datos y códigos.
 */
public val MonoNumbers: FontFamily = FontFamily.Monospace

/**
 * Canal para el estilo de los valores de un código leído.
 */
public val LocalCodeValueStyle: ProvidableCompositionLocal<TextStyle> = staticCompositionLocalOf {
    TextStyle.Default
}

/**
 * Crea un [TextStyle] con parámetros comunes.
 */
public fun textStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    letterSpacing: Double,
    family: FontFamily = FontFamily.Default,
): TextStyle = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/**
 * Ajusta una escala tipográfica para facilitar la lectura (modo dislexia).
 */
public fun Typography.forEasierReading(
    family: FontFamily = FontFamily.SansSerif,
): Typography = Typography(
    displayLarge = displayLarge.forEasierReading(family),
    displayMedium = displayMedium.forEasierReading(family),
    displaySmall = displaySmall.forEasierReading(family),
    headlineLarge = headlineLarge.forEasierReading(family),
    headlineMedium = headlineMedium.forEasierReading(family),
    headlineSmall = headlineSmall.forEasierReading(family),
    titleLarge = titleLarge.forEasierReading(family),
    titleMedium = titleMedium.forEasierReading(family),
    titleSmall = titleSmall.forEasierReading(family),
    bodyLarge = bodyLarge.forEasierReading(family),
    bodyMedium = bodyMedium.forEasierReading(family),
    bodySmall = bodySmall.forEasierReading(family),
    labelLarge = labelLarge.forEasierReading(family),
    labelMedium = labelMedium.forEasierReading(family),
    labelSmall = labelSmall.forEasierReading(family),
)

/**
 * Ajusta un estilo concreto para facilitar la lectura.
 */
public fun TextStyle.forEasierReading(
    family: FontFamily = FontFamily.SansSerif,
    sizeFactor: Float = 1.15f,
    lineHeightRatio: Float = 1.75f,
    extraLetterSpacing: Float = 0.75f,
): TextStyle {
    val grownSize = if (fontSize.isSp) fontSize.value else 16f
    return copy(
        fontFamily = family,
        fontSize = (grownSize * sizeFactor).sp,
        lineHeight = (grownSize * sizeFactor * lineHeightRatio).sp,
        letterSpacing = (letterSpacing.value.coerceAtLeast(0f) + extraLetterSpacing).sp,
    )
}
