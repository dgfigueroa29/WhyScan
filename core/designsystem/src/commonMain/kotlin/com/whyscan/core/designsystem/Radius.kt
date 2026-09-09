package com.whyscan.core.designsystem

import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ar.net.faro.foundation.shapesFrom

/**
 * Radios de WhyScan, algo más redondeados que los de fábrica de Material 3.
 *
 * No es capricho: la app pinta su UI **encima o al lado de un visor de cámara**, que es un
 * rectángulo con esquinas muy marcadas. Un radio generoso en tarjetas y hojas separa la interfaz de
 * la imagen sin necesitar bordes ni sombras, que sobre vídeo se ven sucios.
 *
 * Se exponen también sueltos —[Radius]— porque el visor y el overlay no son componentes de Material
 * y aun así tienen que usar los mismos valores. Esa era la razón por la que en `ScannerScreen`
 * aparecía un `RoundedCornerShape(Spacing.md)`: un radio tomado prestado de una escala de
 * espaciados, que es un `dp` suelto con disfraz.
 */
public object Radius {
    /** Chips y campos pequeños. */
    public val xs: Dp = 8.dp

    /** Botones y campos de texto. */
    public val sm: Dp = 12.dp

    /** Tarjetas. El valor más usado de toda la app. */
    public val md: Dp = 16.dp

    /** Visor de cámara, diálogos y contenedores grandes. */
    public val lg: Dp = 22.dp

    /** Hojas inferiores y superficies que nacen del borde de la pantalla. */
    public val xl: Dp = 28.dp

    /** Píldoras: `FilterChip` de formato, indicador de navegación. */
    public val pill: Dp = 999.dp
}

/**
 * El fichero se llama `Radius.kt` y no `Shapes.kt` por la regla `MatchingDeclarationName` de detekt:
 * la única declaración de tipo aquí arriba es [Radius], y el nombre del fichero tiene que ser ese.
 *
 * Llamar `Shapes` al objeto para poder llamar `Shapes.kt` al fichero no era opción: chocaría con
 * `androidx.compose.material3.Shapes`, que se usa tres líneas más abajo.
 */
internal val WhyScanShapes: Shapes = shapesFrom(
    extraSmall = Radius.xs,
    small = Radius.sm,
    medium = Radius.md,
    large = Radius.lg,
    extraLarge = Radius.xl,
)
