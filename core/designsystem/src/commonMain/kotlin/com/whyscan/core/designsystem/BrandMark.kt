package com.whyscan.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * La marca de WhyScan: **el módulo fugado**.
 *
 * Un *patrón de localización* —los cuadrados anidados que toda esquina de un QR lleva para que un
 * lector sepa dónde empieza el código— con dos cosas mal a propósito: el anillo tiene una brecha en
 * la esquina, y el módulo que debería estar en su centro ya está fuera, atravesándola.
 *
 * ## Por qué esta forma y no un visor
 *
 * Lo que había antes eran cuatro esquinas de encuadre y una línea, que es el símbolo genérico del
 * sector: el mismo dibujo que traen el icono `QrCodeScanner` de Material y otras doscientas apps de
 * la tienda. En una ficha de Play un símbolo así no distingue, agrupa.
 *
 * El patrón de localización, en cambio, es el átomo más reconocible de un código y casi nadie lo
 * usa. Y trae una propiedad que se puede romper: **es siempre concéntrico y siempre cerrado**. Uno
 * que no lo sea llama la atención sin gritar, y dice exactamente lo que hace la app — el dato estaba
 * encerrado en un dibujo que ningún humano lee, y ahora está fuera y sirve para algo.
 *
 * ## Dos cosas que hay que respetar al tocarla
 *
 * **La brecha del anillo termina en 10.4 y el módulo empieza en 13.4.** Esa holgura no es estética:
 * en la capa monocroma de Android 13+ las dos piezas se pintan del **mismo color**, así que si se
 * acercan se funden en una mancha y la idea desaparece. Cualquier ajuste tiene que conservar la
 * separación.
 *
 * **Hay tres copias de esta forma y tienen que decir lo mismo**: este `ImageVector`, el primer plano
 * del icono adaptativo (`ic_launcher_foreground.xml`) y la capa monocroma
 * (`ic_launcher_monochrome.xml`). Viven en dos mundos que no se hablan —Compose y el sistema de
 * recursos de Android— y por eso las tres llevan esta nota. Las de Android van en un lienzo de 108
 * con la marca escalada a 48 centrados; las coordenadas de aquí son la fuente.
 */
// Las coordenadas de un trazado **son** la definición de la forma: darles nombre no aclararía nada
// —¿`ANILLO_ESQUINA_SUPERIOR_DERECHA_X`?— y rompería la correspondencia línea a línea con los dos
// XML, que es lo que hace comprobable que los tres dibujan lo mismo.
@Suppress("MagicNumber")
val WhyScanMark: ImageVector by lazy {
    ImageVector.Builder(
        name = "WhyScanMark",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT,
    ).apply {
        // El anillo, abierto por la esquina inferior derecha. Va en dos subtrayectos y no en uno
        // porque la brecha **es** la forma: un solo trazo cerrado sería un patrón de localización
        // normal, y entonces no habría nada que contar.
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            // Lado superior y bajada por la derecha, hasta el borde de la brecha.
            moveTo(6.4f, 4.4f)
            lineTo(15.4f, 4.4f)
            arcTo(2f, 2f, 0f, false, true, 17.4f, 6.4f)
            lineTo(17.4f, 10.4f)

            // Desde el otro borde de la brecha, todo el resto del anillo.
            moveTo(10.4f, 17.4f)
            lineTo(6.4f, 17.4f)
            arcTo(2f, 2f, 0f, false, true, 4.4f, 15.4f)
            lineTo(4.4f, 6.4f)
            arcTo(2f, 2f, 0f, false, true, 6.4f, 4.4f)
        }

        // El módulo, ya fuera. Macizo frente al anillo en trazo: esa diferencia entre línea y mancha
        // es lo que distingue las dos piezas cuando el sistema las pinta del mismo color.
        path(fill = SolidColor(Color.Black)) {
            moveTo(15.2f, 13.4f)
            lineTo(18.8f, 13.4f)
            arcTo(1.8f, 1.8f, 0f, false, true, 20.6f, 15.2f)
            lineTo(20.6f, 18.8f)
            arcTo(1.8f, 1.8f, 0f, false, true, 18.8f, 20.6f)
            lineTo(15.2f, 20.6f)
            arcTo(1.8f, 1.8f, 0f, false, true, 13.4f, 18.8f)
            lineTo(13.4f, 15.2f)
            arcTo(1.8f, 1.8f, 0f, false, true, 15.2f, 13.4f)
            close()
        }
    }.build()
}

private const val VIEWPORT = 24f
private const val STROKE = 2.2f
