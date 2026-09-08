package com.whyscan.core.designsystem

import ar.net.faro.foundation.Palette

/**
 * La paleta de WhyScan como **datos**, sin Compose de por medio.
 *
 * Está separada de `Theme.kt` por una razón concreta: así el contraste se puede comprobar con
 * aritmética en `commonTest`, sin renderizar nada ni necesitar dispositivo. RNF-05 exige contraste
 * AA, y hasta que existió `ContrastTest` era una intención escrita en un documento.
 *
 * Los colores son ARGB en `Int`, igual que espera el tipo `Color` de Compose.
 */
public object ScannerPalette {

    /** Combinación de un color y el que se pinta encima. La unidad que se mide. */
    public data class ColorPair(val name: String, val foreground: Int, val background: Int)

    /** Exporta la paleta clara como un objeto [Palette] del sistema de diseño federado. */
    public fun light(): Palette = with(Light) {
        Palette(
            primary = PRIMARY,
            onPrimary = ON_PRIMARY,
            primaryContainer = PRIMARY_CONTAINER,
            onPrimaryContainer = ON_PRIMARY_CONTAINER,
            secondary = SECONDARY,
            onSecondary = ON_SECONDARY,
            secondaryContainer = SECONDARY_CONTAINER,
            onSecondaryContainer = ON_SECONDARY_CONTAINER,
            tertiary = TERTIARY,
            onTertiary = ON_TERTIARY,
            tertiaryContainer = TERTIARY_CONTAINER,
            onTertiaryContainer = ON_TERTIARY_CONTAINER,
            error = ERROR,
            onError = ON_ERROR,
            errorContainer = ERROR_CONTAINER,
            onErrorContainer = ON_ERROR_CONTAINER,
            background = BACKGROUND,
            onBackground = ON_BACKGROUND,
            surface = SURFACE,
            onSurface = ON_SURFACE,
            surfaceVariant = SURFACE_VARIANT,
            onSurfaceVariant = ON_SURFACE_VARIANT,
            surfaceContainerLowest = SURFACE_CONTAINER_LOWEST,
            surfaceContainerLow = SURFACE_CONTAINER_LOW,
            surfaceContainer = SURFACE_CONTAINER,
            surfaceContainerHigh = SURFACE_CONTAINER_HIGH,
            surfaceContainerHighest = SURFACE_CONTAINER_HIGHEST,
            outline = OUTLINE,
            outlineVariant = OUTLINE_VARIANT,
            scrim = SCRIM,
            inverseSurface = INVERSE_SURFACE,
            inverseOnSurface = INVERSE_ON_SURFACE,
            inversePrimary = INVERSE_PRIMARY,
        )
    }

    /** Exporta la paleta oscura como un objeto [Palette]. */
    public fun dark(): Palette = with(Dark) {
        Palette(
            primary = PRIMARY,
            onPrimary = ON_PRIMARY,
            primaryContainer = PRIMARY_CONTAINER,
            onPrimaryContainer = ON_PRIMARY_CONTAINER,
            secondary = SECONDARY,
            onSecondary = ON_SECONDARY,
            secondaryContainer = SECONDARY_CONTAINER,
            onSecondaryContainer = ON_SECONDARY_CONTAINER,
            tertiary = TERTIARY,
            onTertiary = ON_TERTIARY,
            tertiaryContainer = TERTIARY_CONTAINER,
            onTertiaryContainer = ON_TERTIARY_CONTAINER,
            error = ERROR,
            onError = ON_ERROR,
            errorContainer = ERROR_CONTAINER,
            onErrorContainer = ON_ERROR_CONTAINER,
            background = BACKGROUND,
            onBackground = ON_BACKGROUND,
            surface = SURFACE,
            onSurface = ON_SURFACE,
            surfaceVariant = SURFACE_VARIANT,
            onSurfaceVariant = ON_SURFACE_VARIANT,
            surfaceContainerLowest = SURFACE_CONTAINER_LOWEST,
            surfaceContainerLow = SURFACE_CONTAINER_LOW,
            surfaceContainer = SURFACE_CONTAINER,
            surfaceContainerHigh = SURFACE_CONTAINER_HIGH,
            surfaceContainerHighest = SURFACE_CONTAINER_HIGHEST,
            outline = OUTLINE,
            outlineVariant = OUTLINE_VARIANT,
            scrim = SCRIM,
            inverseSurface = INVERSE_SURFACE,
            inverseOnSurface = INVERSE_ON_SURFACE,
            inversePrimary = INVERSE_PRIMARY,
        )
    }

    /**
     * Claro: esmeralda de marca sobre neutros de **grafito cálido** — papel y tinta, no acero.
     *
     * El neutro no es gris puro: lleva una pizca de amarillo. Es la decisión que hace que la app se
     * lea como un documento y no como un instrumento, que es lo que corresponde a un producto cuyo
     * trabajo no es escanear sino **decirte qué es lo que acabas de escanear**.
     *
     * **Un solo acento.** Toda la energía cromática está en el esmeralda y el resto es papel y
     * tinta; por eso, cuando el esmeralda aparece, significa algo. `secondary` es el mismo verde
     * apagado —no un segundo color de marca— y `tertiary` es ámbar porque tiene un trabajo
     * **semántico**: avisar. No compite, señala.
     */
    public object Light {
        public const val PRIMARY: Int = 0xFF07704E.toInt()
        public const val ON_PRIMARY: Int = 0xFFFFFFFF.toInt()
        public const val PRIMARY_CONTAINER: Int = 0xFFC6EFDD.toInt()
        public const val ON_PRIMARY_CONTAINER: Int = 0xFF00281A.toInt()

        public const val SECONDARY: Int = 0xFF2C6152.toInt()
        public const val ON_SECONDARY: Int = 0xFFFFFFFF.toInt()
        public const val SECONDARY_CONTAINER: Int = 0xFFD6E8DF.toInt()
        public const val ON_SECONDARY_CONTAINER: Int = 0xFF0B2019.toInt()

        public const val TERTIARY: Int = 0xFF8A5A00.toInt()
        public const val ON_TERTIARY: Int = 0xFFFFFFFF.toInt()
        public const val TERTIARY_CONTAINER: Int = 0xFFFBE7C4.toInt()
        public const val ON_TERTIARY_CONTAINER: Int = 0xFF3A2600.toInt()

        public const val ERROR: Int = 0xFFB3261E.toInt()
        public const val ON_ERROR: Int = 0xFFFFFFFF.toInt()
        public const val ERROR_CONTAINER: Int = 0xFFF9DEDC.toInt()
        public const val ON_ERROR_CONTAINER: Int = 0xFF410E0B.toInt()

        public const val BACKGROUND: Int = 0xFFFAF9F7.toInt()
        public const val ON_BACKGROUND: Int = 0xFF1B1917.toInt()
        public const val SURFACE: Int = 0xFFFFFFFF.toInt()
        public const val ON_SURFACE: Int = 0xFF1B1917.toInt()
        public const val SURFACE_VARIANT: Int = 0xFFE8E5E0.toInt()
        public const val ON_SURFACE_VARIANT: Int = 0xFF4A4744.toInt()

        // Los cinco niveles de contenedor de Material 3. Son la jerarquía de elevación **por color**
        // que sustituyó a las sombras: una tarjeta dentro de otra se distingue por tono y no por
        // sombra, y eso es justo lo que hace legible una pantalla en modo oscuro.
        public const val SURFACE_CONTAINER_LOWEST: Int = 0xFFFFFFFF.toInt()
        public const val SURFACE_CONTAINER_LOW: Int = 0xFFFAF9F7.toInt()
        public const val SURFACE_CONTAINER: Int = 0xFFF1EFEC.toInt()
        public const val SURFACE_CONTAINER_HIGH: Int = 0xFFEAE7E2.toInt()
        public const val SURFACE_CONTAINER_HIGHEST: Int = 0xFFE3DFDA.toInt()

        public const val OUTLINE: Int = 0xFF74706B.toInt()
        public const val OUTLINE_VARIANT: Int = 0xFFCFCAC4.toInt()
        public const val SCRIM: Int = 0xFF000000.toInt()

        // Lo que usa el Snackbar: fondo oscuro en tema claro. Sin declararlos, el mensaje de
        // "Copiado" —el feedback más frecuente de toda la app— salía con la paleta de fábrica.
        public const val INVERSE_SURFACE: Int = 0xFF2A2724.toInt()
        public const val INVERSE_ON_SURFACE: Int = 0xFFF1EFEC.toInt()
        public const val INVERSE_PRIMARY: Int = 0xFF5FD9A5.toInt()
    }

    /**
     * Oscuro: el esmeralda se aclara y los neutros bajan a una tinta casi negra que conserva el
     * sesgo cálido, no a gris neutro.
     *
     * Es el tema en el que esta app vive de verdad: se usa apuntando a algo, casi siempre con poca
     * luz, y encima de una imagen de cámara. El claro no es menos importante, pero es el secundario.
     */
    public object Dark {
        public const val PRIMARY: Int = 0xFF5FD9A5.toInt()
        public const val ON_PRIMARY: Int = 0xFF00301F.toInt()
        public const val PRIMARY_CONTAINER: Int = 0xFF0A5238.toInt()
        public const val ON_PRIMARY_CONTAINER: Int = 0xFFC6EFDD.toInt()

        public const val SECONDARY: Int = 0xFFA9CFBF.toInt()
        public const val ON_SECONDARY: Int = 0xFF12312A.toInt()
        public const val SECONDARY_CONTAINER: Int = 0xFF2A4A40.toInt()
        public const val ON_SECONDARY_CONTAINER: Int = 0xFFC9E8DC.toInt()

        public const val TERTIARY: Int = 0xFFF0C070.toInt()
        public const val ON_TERTIARY: Int = 0xFF3A2600.toInt()
        public const val TERTIARY_CONTAINER: Int = 0xFF6B4A0C.toInt()
        public const val ON_TERTIARY_CONTAINER: Int = 0xFFFBE7C4.toInt()

        public const val ERROR: Int = 0xFFFFB4AB.toInt()
        public const val ON_ERROR: Int = 0xFF601410.toInt()
        public const val ERROR_CONTAINER: Int = 0xFF8C1D18.toInt()
        public const val ON_ERROR_CONTAINER: Int = 0xFFF9DEDC.toInt()

        public const val BACKGROUND: Int = 0xFF121110.toInt()
        public const val ON_BACKGROUND: Int = 0xFFEDEAE6.toInt()
        public const val SURFACE: Int = 0xFF1A1817.toInt()
        public const val ON_SURFACE: Int = 0xFFEDEAE6.toInt()
        public const val SURFACE_VARIANT: Int = 0xFF2B2825.toInt()
        public const val ON_SURFACE_VARIANT: Int = 0xFFC9C4BE.toInt()

        public const val SURFACE_CONTAINER_LOWEST: Int = 0xFF0C0B0A.toInt()
        public const val SURFACE_CONTAINER_LOW: Int = 0xFF1A1817.toInt()
        public const val SURFACE_CONTAINER: Int = 0xFF221F1D.toInt()
        public const val SURFACE_CONTAINER_HIGH: Int = 0xFF2B2825.toInt()
        public const val SURFACE_CONTAINER_HIGHEST: Int = 0xFF35312E.toInt()

        public const val OUTLINE: Int = 0xFF8C877F.toInt()
        public const val OUTLINE_VARIANT: Int = 0xFF45413D.toInt()
        public const val SCRIM: Int = 0xFF000000.toInt()

        public const val INVERSE_SURFACE: Int = 0xFFEDEAE6.toInt()
        public const val INVERSE_ON_SURFACE: Int = 0xFF1A1817.toInt()

        // Un verde bastante más oscuro que el `primary` del tema claro: sobre el `inverseSurface`
        // de este tema —que es casi blanco— aquel se quedaba corto y no llegaba a AA. Es
        // exactamente el par que nadie mira: el botón de acción de un Snackbar en modo oscuro.
        public const val INVERSE_PRIMARY: Int = 0xFF08643F.toInt()
    }

    /**
     * Lo que se pinta **encima del vídeo**, donde no hay tema que valga.
     *
     * El visor dibuja sobre el preview de la cámara, y ahí el fondo no es una superficie del tema:
     * es la escena que el usuario esté enfocando. `MaterialTheme.colorScheme` no significa nada
     * sobre una pared blanca o sobre una caja negra, así que estos colores **no** salen de [Light]
     * ni de [Dark] — son los mismos en los dos temas, a propósito.
     *
     * Estaban escritos a mano dentro de `ScanOverlay`, que es exactamente la clase de fuga que un
     * sistema de diseño existe para impedir: un color de marca fuera de la paleta, en el único
     * sitio donde nadie lo iba a buscar. El valor no cambia — cambia dónde vive y quién lo puede
     * medir.
     *
     * **Lo que aquí no se puede garantizar, y hay que decirlo:** estos pares no entran en
     * [measuredPairs] porque no tienen fondo conocido contra el que medirlos. El contraste sobre
     * vídeo arbitrario no es una propiedad de la paleta y no lo decide ningún test — lo decide un
     * dispositivo, apuntando a una escena real. Lo que sí se hace por diseño es no depender solo
     * del color: la retícula lleva su propio trazo y las detecciones se dibujan con un contorno
     * cerrado, así que la forma sigue leyéndose aunque el color se pierda contra el fondo.
     */
    public object Overlay {

        /** Contorno de un código detectado. Verde de marca, más luminoso que el `primary` claro. */
        public const val DETECTION: Int = 0xFF34D399.toInt()

        /** Esquinas del marco de encuadre. Blanco, que es lo único legible sobre cualquier escena. */
        public const val RETICLE: Int = 0xFFFFFFFF.toInt()
    }

    /**
     * Los pares que hay que medir a 4.5:1: no solo los de Material, también los que **la UI usa de
     * hecho**.
     *
     * La distinción importa. Material garantiza que `onPrimary` se lee sobre `primary`, pero las
     * pantallas usan además `primary`, `tertiary` y `error` como **color de texto sobre la
     * tarjeta** —la disponibilidad del motor, el aviso de degradación, el error de sesión—, y esos
     * pares no los cubre ninguna convención. Son justo los que se pueden romper sin que nadie note
     * nada hasta que alguien no los pueda leer.
     */
    public fun measuredPairs(): List<ColorPair> =
        pairs("claro", lightRoles(), TEXT_PAIRS) + pairs("oscuro", darkRoles(), TEXT_PAIRS)

    /**
     * Pares que solo tienen que llegar a 3.0:1, el umbral de WCAG para **componentes no textuales**.
     *
     * Aquí vive `outline`, que es el borde de un `OutlinedButton` o de un `OutlinedTextField`: es
     * información —dónde termina el control— pero no es texto, y exigirle 4.5 obligaría a un borde
     * tan oscuro que la UI parecería un formulario de los noventa.
     */
    public fun measuredNonTextPairs(): List<ColorPair> =
        pairs("claro", lightRoles(), NON_TEXT_PAIRS) + pairs("oscuro", darkRoles(), NON_TEXT_PAIRS)

    /**
     * Los pares se declaran **una vez** y se aplican a los dos esquemas.
     *
     * Antes la lista estaba escrita dos veces y `los_dos_temas_miden_los_mismos_pares` existía justo
     * para cazar el día en que una de las dos copias se quedara corta. Ahora ese caso no puede
     * ocurrir, y el test sigue valiendo la pena: protege de que alguien vuelva a separarlas.
     *
     * Los nombres son los de Material y no cadenas libres: se resuelven contra el mapa de roles con
     * `getValue`, así que un rol mal escrito revienta el test en vez de saltarse el par en silencio.
     */
    private val TEXT_PAIRS = listOf(
        "onPrimary" to "primary",
        "onPrimaryContainer" to "primaryContainer",
        "onSecondary" to "secondary",
        "onSecondaryContainer" to "secondaryContainer",
        "onTertiary" to "tertiary",
        "onTertiaryContainer" to "tertiaryContainer",
        "onError" to "error",
        "onErrorContainer" to "errorContainer",
        "onBackground" to "background",
        "onSurface" to "surface",
        "onSurfaceVariant" to "surface",
        "onSurfaceVariant" to "surfaceVariant",
        "onSurface" to "surfaceContainerLowest",
        "onSurface" to "surfaceContainerLow",
        "onSurface" to "surfaceContainer",
        "onSurface" to "surfaceContainerHigh",
        "onSurface" to "surfaceContainerHighest",
        "onSurfaceVariant" to "surfaceContainer",
        "onSurfaceVariant" to "surfaceContainerHighest",
        // Los cuatro que ninguna convención de Material cubre: colores de acento usados como
        // **texto** sobre una superficie.
        "primary" to "surface",
        "primary" to "surfaceContainer",
        "tertiary" to "surface",
        "error" to "surface",
        // El Snackbar, que en tema claro es oscuro y al revés.
        "inverseOnSurface" to "inverseSurface",
        "inversePrimary" to "inverseSurface",
    )

    private val NON_TEXT_PAIRS = listOf(
        "outline" to "surface",
        "outline" to "surfaceContainer",
        "outline" to "background",
    )

    private fun pairs(
        theme: String,
        roles: Map<String, Int>,
        spec: List<Pair<String, String>>,
    ): List<ColorPair> = spec.map { (foreground, background) ->
        ColorPair(
            name = "$theme: $foreground sobre $background",
            foreground = roles.getValue(foreground),
            background = roles.getValue(background),
        )
    }

    private fun lightRoles(): Map<String, Int> = with(Light) {
        mapOf(
            "primary" to PRIMARY,
            "onPrimary" to ON_PRIMARY,
            "primaryContainer" to PRIMARY_CONTAINER,
            "onPrimaryContainer" to ON_PRIMARY_CONTAINER,
            "secondary" to SECONDARY,
            "onSecondary" to ON_SECONDARY,
            "secondaryContainer" to SECONDARY_CONTAINER,
            "onSecondaryContainer" to ON_SECONDARY_CONTAINER,
            "tertiary" to TERTIARY,
            "onTertiary" to ON_TERTIARY,
            "tertiaryContainer" to TERTIARY_CONTAINER,
            "onTertiaryContainer" to ON_TERTIARY_CONTAINER,
            "error" to ERROR,
            "onError" to ON_ERROR,
            "errorContainer" to ERROR_CONTAINER,
            "onErrorContainer" to ON_ERROR_CONTAINER,
            "background" to BACKGROUND,
            "onBackground" to ON_BACKGROUND,
            "surface" to SURFACE,
            "onSurface" to ON_SURFACE,
            "surfaceVariant" to SURFACE_VARIANT,
            "onSurfaceVariant" to ON_SURFACE_VARIANT,
            "surfaceContainerLowest" to SURFACE_CONTAINER_LOWEST,
            "surfaceContainerLow" to SURFACE_CONTAINER_LOW,
            "surfaceContainer" to SURFACE_CONTAINER,
            "surfaceContainerHigh" to SURFACE_CONTAINER_HIGH,
            "surfaceContainerHighest" to SURFACE_CONTAINER_HIGHEST,
            "outline" to OUTLINE,
            "outlineVariant" to OUTLINE_VARIANT,
            "inverseSurface" to INVERSE_SURFACE,
            "inverseOnSurface" to INVERSE_ON_SURFACE,
            "inversePrimary" to INVERSE_PRIMARY,
        )
    }

    private fun darkRoles(): Map<String, Int> = with(Dark) {
        mapOf(
            "primary" to PRIMARY,
            "onPrimary" to ON_PRIMARY,
            "primaryContainer" to PRIMARY_CONTAINER,
            "onPrimaryContainer" to ON_PRIMARY_CONTAINER,
            "secondary" to SECONDARY,
            "onSecondary" to ON_SECONDARY,
            "secondaryContainer" to SECONDARY_CONTAINER,
            "onSecondaryContainer" to ON_SECONDARY_CONTAINER,
            "tertiary" to TERTIARY,
            "onTertiary" to ON_TERTIARY,
            "tertiaryContainer" to TERTIARY_CONTAINER,
            "onTertiaryContainer" to ON_TERTIARY_CONTAINER,
            "error" to ERROR,
            "onError" to ON_ERROR,
            "errorContainer" to ERROR_CONTAINER,
            "onErrorContainer" to ON_ERROR_CONTAINER,
            "background" to BACKGROUND,
            "onBackground" to ON_BACKGROUND,
            "surface" to SURFACE,
            "onSurface" to ON_SURFACE,
            "surfaceVariant" to SURFACE_VARIANT,
            "onSurfaceVariant" to ON_SURFACE_VARIANT,
            "surfaceContainerLowest" to SURFACE_CONTAINER_LOWEST,
            "surfaceContainerLow" to SURFACE_CONTAINER_LOW,
            "surfaceContainer" to SURFACE_CONTAINER,
            "surfaceContainerHigh" to SURFACE_CONTAINER_HIGH,
            "surfaceContainerHighest" to SURFACE_CONTAINER_HIGHEST,
            "outline" to OUTLINE,
            "outlineVariant" to OUTLINE_VARIANT,
            "inverseSurface" to INVERSE_SURFACE,
            "inverseOnSurface" to INVERSE_ON_SURFACE,
            "inversePrimary" to INVERSE_PRIMARY,
        )
    }
}
