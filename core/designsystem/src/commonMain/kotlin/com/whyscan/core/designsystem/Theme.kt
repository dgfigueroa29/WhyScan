package com.whyscan.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ar.net.faro.foundation.LocalCodeValueStyle
import ar.net.faro.foundation.colorSchemeFrom

/**
 * Tema propio de WhyScan.
 *
 * Sustituye al `dynamicColorScheme` que traía la plantilla de Android: un tema que cambia con el
 * fondo de pantalla del usuario es incompatible con una app cuya UI se superpone a un preview de
 * cámara, donde el contraste tiene que estar garantizado (RNF-05). Esa decisión se mantiene ahora
 * que hay marca: el esmeralda de WhyScan **es** parte del producto, no un acento negociable.
 */
// Los colores viven en `ScannerPalette`, que no depende de Compose. Así el contraste se mide con
// aritmética en `commonTest` (`ContrastTest`) en lugar de quedar como una intención del documento.

/** Espaciados del sistema de diseño. Evita `dp` sueltos repartidos por las pantallas. */
public object Spacing {
    /** Separación entre un icono y su etiqueta, o entre dos chips. */
    public val xxs: Dp = 2.dp
    public val xs: Dp = 4.dp
    public val sm: Dp = 8.dp

    /** El margen de contenido por defecto. */
    public val md: Dp = 16.dp
    public val lg: Dp = 24.dp
    public val xl: Dp = 32.dp

    /** Huecos de estado vacío y separaciones entre bloques de una pantalla de ajustes. */
    public val xxl: Dp = 48.dp
}

/**
 * Envuelve el contenido en el tema de WhyScan.
 *
 * Recibe booleanos y no los enums del dominio: resolver "sistema" contra lo que el sistema dice
 * **ahora** es cosa de quien tiene el estado de la app delante, y así este módulo no depende del
 * dominio. Los valores por defecto dejan que cualquier `@Preview` o punto de entrada que no quiera
 * saber de preferencias siga funcionando.
 *
 * @param easierReading el modo dislexia. Cambia la escala tipográfica entera —espaciado entre
 *   letras, interlínea y tamaño— y también el estilo del valor de un código, que viaja aparte por
 *   [LocalCodeValueStyle] porque no es un rol de Material. Que las dos cosas salgan de aquí es lo
 *   que evita que una pantalla tenga que preguntarse si el modo está encendido.
 */
@Composable
public fun WhyScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    easierReading: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) ScannerPalette.dark() else ScannerPalette.light()

    CompositionLocalProvider(LocalCodeValueStyle provides codeValueStyle(easierReading)) {
        MaterialTheme(
            colorScheme = colorSchemeFrom(palette, darkTheme),
            typography = whyScanTypography(easierReading),
            shapes = WhyScanShapes,
            content = content,
        )
    }
}
