package com.whyscan.engines.ocr

import com.whyscan.core.model.Point
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.ImageIO.CGImagePropertyOrientation
import platform.ImageIO.kCGImagePropertyOrientationDown
import platform.ImageIO.kCGImagePropertyOrientationDownMirrored
import platform.ImageIO.kCGImagePropertyOrientationLeft
import platform.ImageIO.kCGImagePropertyOrientationLeftMirrored
import platform.ImageIO.kCGImagePropertyOrientationRight
import platform.ImageIO.kCGImagePropertyOrientationRightMirrored
import platform.ImageIO.kCGImagePropertyOrientationUp
import platform.ImageIO.kCGImagePropertyOrientationUpMirrored
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIImage
import platform.UIKit.UIImageOrientation
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate

/**
 * El puente entre el framework Vision y [OcrCodeInterpreter], que es quien decide de verdad.
 *
 * Aquí no se interpreta nada: se pide texto y se entregan líneas. Es la misma división que en
 * Android, donde `MlKitOcrEngine` aplana el resultado de ML Kit a [OcrLine] y no toma ninguna
 * decisión sobre lo leído.
 */
@OptIn(ExperimentalForeignApi::class)
internal object VisionTextRecognizer {

    /** Cuántas alternativas se piden por línea. Solo interesa la mejor: el checksum es el filtro. */
    private const val TOP_CANDIDATES = 1uL

    /**
     * Reconoce texto y devuelve una línea por observación.
     *
     * `performRequests` es **síncrono**: cuando vuelve, `results` ya está poblado. Por eso el
     * llamador se encarga de invocarlo fuera del hilo principal.
     */
    fun linesIn(handler: VNImageRequestHandler): List<OcrLine> {
        val request = VNRecognizeTextRequest().apply {
            // `accurate` y no `fast`: lo que se busca son dígitos pequeños impresos bajo unas
            // barras, que es justo donde el modelo rápido se equivoca. Con el checksum delante, un
            // reconocimiento peor no produce lecturas falsas — produce **ninguna** lectura, y el
            // motor parecería no hacer nada.
            recognitionLevel = VNRequestTextRecognitionLevelAccurate
            // Un EAN no es una palabra. La corrección lingüística está entrenada para arreglar
            // texto, y sobre una tirada de trece cifras solo puede inventar.
            usesLanguageCorrection = false
        }

        handler.performRequests(listOf(request), null)

        return request.results.orEmpty()
            .filterIsInstance<VNRecognizedTextObservation>()
            .mapNotNull { it.toOcrLine() }
    }

    private fun VNRecognizedTextObservation.toOcrLine(): OcrLine? {
        val candidate = topCandidates(TOP_CANDIDATES).firstOrNull() as? VNRecognizedText ?: return null

        return OcrLine(
            text = candidate.string,
            confidence = candidate.confidence,
            cornerPoints = listOf(topLeft, topRight, bottomRight, bottomLeft).map { it.toPoint() },
        )
    }

    /**
     * Vision normaliza a `[0, 1]` con el origen **abajo a la izquierda**; [Point] lo quiere arriba,
     * que es donde lo tienen tanto el overlay como el motor de Android.
     *
     * Solo hay que invertir la `y`: el nombre de cada esquina sigue valiendo después de invertir
     * —lo que Vision llama `topLeft` tiene la `y` más alta y pasa a ser la más baja—, así que el
     * orden de recorrido del cuadrilátero se conserva.
     */
    private fun CValue<CGPoint>.toPoint(): Point =
        useContents { Point(x.toFloat(), (1.0 - y).toFloat()) }
}

/**
 * Orientación con la que interpretar los frames de la cámara trasera.
 *
 * Se lee **una vez al arrancar la sesión** y desde el hilo principal, no en cada frame: el delegate
 * de vídeo corre en una cola propia y consultar UIKit desde ahí no es correcto. El precio está
 * dicho: si el dispositivo gira a mitad de sesión, el OCR sigue leyendo con la orientación con la
 * que empezó. El preview sí rota, porque de eso se encarga la capa de Core Animation.
 *
 * Cuando el sistema no está generando notificaciones de orientación, `orientation` responde
 * "desconocida"; ahí se asume vertical, que es como se sujeta un teléfono para leer una etiqueta.
 *
 * Las constantes se cualifican por su tipo y no se importan sueltas, al revés que las de
 * AVFoundation que usa el resto del proyecto: cinterop no traduce igual todos los `NS_ENUM`, y las
 * de UIKit no existen como declaraciones de nivel superior. Las de ImageIO sí, y por eso van
 * importadas. Está escrito porque es exactamente la clase de asimetría que solo se descubre
 * compilando, y compilar iOS aquí cuesta un workflow entero.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun currentCaptureOrientation(): CGImagePropertyOrientation =
    when (UIDevice.currentDevice.orientation) {
        UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown ->
            kCGImagePropertyOrientationLeft

        UIDeviceOrientation.UIDeviceOrientationLandscapeLeft ->
            kCGImagePropertyOrientationUp

        UIDeviceOrientation.UIDeviceOrientationLandscapeRight ->
            kCGImagePropertyOrientationDown

        else -> kCGImagePropertyOrientationRight
    }

/**
 * Orientación de una imagen ya capturada (RF-07).
 *
 * `UIImage` guarda la suya aparte de los píxeles —una foto vertical de un iPhone es un mapa de bits
 * apaisado más una etiqueta que dice cómo girarlo—, y `CGImage` no la lleva dentro. Pasarla por
 * separado es lo que evita que el texto llegue a Vision de lado.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun UIImage.captureOrientation(): CGImagePropertyOrientation =
    when (imageOrientation) {
        UIImageOrientation.UIImageOrientationDown -> kCGImagePropertyOrientationDown
        UIImageOrientation.UIImageOrientationLeft -> kCGImagePropertyOrientationLeft
        UIImageOrientation.UIImageOrientationRight -> kCGImagePropertyOrientationRight
        UIImageOrientation.UIImageOrientationUpMirrored -> kCGImagePropertyOrientationUpMirrored
        UIImageOrientation.UIImageOrientationDownMirrored -> kCGImagePropertyOrientationDownMirrored
        UIImageOrientation.UIImageOrientationLeftMirrored -> kCGImagePropertyOrientationLeftMirrored
        UIImageOrientation.UIImageOrientationRightMirrored ->
            kCGImagePropertyOrientationRightMirrored

        else -> kCGImagePropertyOrientationUp
    }
