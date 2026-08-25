package com.whyscan.engines.ocr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.whyscan.core.model.Barcode
import com.whyscan.core.model.Detection
import com.whyscan.core.model.Permission
import com.whyscan.core.model.ScanError
import com.whyscan.core.model.ScanImage
import com.whyscan.core.model.ScanRequest
import com.whyscan.core.model.ScannerEngineId
import com.whyscan.core.scanner.BarcodeScannerEngine
import com.whyscan.core.scanner.CameraControlEngine
import com.whyscan.core.scanner.EngineAvailability
import com.whyscan.core.scanner.ImageDecodingEngine
import com.whyscan.core.scanner.ScanEvent
import com.whyscan.core.scanner.ScannerEngineDescriptor
import com.whyscan.core.scanner.SystemTimeProvider
import com.whyscan.core.scanner.TimeProvider
import com.whyscan.core.scanner.catalog.ScannerEngineCatalog
import com.whyscan.core.scanner.ui.CameraPreviewEngine
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.CVPixelBufferRef
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.Vision.VNImageRequestHandler
import platform.darwin.NSObject
import platform.darwin.dispatch_queue_create

/**
 * El OCR de iOS: lee el número impreso bajo el código (RF-12) con el reconocedor del sistema.
 *
 * Cierra la cadena de fallback de iOS por delante de la entrada manual, igual que `MlKitOcrEngine`
 * la de Android, y **comparte con él todo lo que decide algo**: [OcrCodeInterpreter] vive en
 * `commonMain` de este mismo módulo y es quien valida el dígito de control. Lo que cambia es el
 * reconocedor, y por eso son dos motores del catálogo y no uno con dos implementaciones — ver
 * [ScannerEngineId.VisionOcr].
 *
 * ### Por qué Vision y no ML Kit
 * ML Kit se distribuye para iOS por CocoaPods, que este proyecto no usa y que arrastraría el modelo
 * de texto dentro del binario. `VNRecognizeTextRequest` viene con el sistema: el motor no añade una
 * sola dependencia ni un solo byte.
 *
 * ### Por qué `AVCaptureVideoDataOutput` y no la salida de metadatos
 * Por lo mismo que en `:engines:zxing-cpp`: la salida de metadatos entrega **códigos ya
 * decodificados** por AVFoundation, y aquí hacen falta los píxeles — el objeto de este motor es
 * precisamente el código que AVFoundation no consiguió decodificar.
 */
@OptIn(ExperimentalForeignApi::class)
class VisionOcrEngine(
    private val time: TimeProvider = SystemTimeProvider,
) : BarcodeScannerEngine, CameraControlEngine, ImageDecodingEngine, CameraPreviewEngine {

    internal val sessionHolder = OcrCameraSessionHolder()

    override val id: ScannerEngineId = ScannerEngineId.VisionOcr

    override val descriptor: ScannerEngineDescriptor = ScannerEngineCatalog.visionOcr

    override suspend fun availability(): EngineAvailability =
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> EngineAvailability.Available

            AVAuthorizationStatusNotDetermined,
            AVAuthorizationStatusDenied, -> EngineAvailability.RequiresPermission(Permission.Camera)

            AVAuthorizationStatusRestricted -> EngineAvailability.Unsupported(
                "El acceso a la cámara está restringido por el dispositivo",
            )

            else -> EngineAvailability.Unsupported("Estado de autorización desconocido")
        }

    // Montar una AVCaptureSession es una secuencia lineal —dispositivo, entrada, salida, cola,
    // arranque— y partirla en trozos que solo se llaman una vez, en orden, dispersa el orden de
    // liberación sin ganar nada. Mismo criterio que en `:engines:zxing-cpp`.
    @Suppress("LongMethod")
    override fun scan(request: ScanRequest): Flow<ScanEvent> = callbackFlow {
        val startedAtMillis = time.nowMillis()

        val camera = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        if (camera == null) {
            trySend(ScanEvent.Failed(ScanError.CameraUnavailable("No hay cámara disponible"), id))
            trySend(ScanEvent.SessionEnded(id))
            close()
            return@callbackFlow
        }

        val session = AVCaptureSession().apply { sessionPreset = AVCaptureSessionPresetHigh }

        val input = AVCaptureDeviceInput.deviceInputWithDevice(camera, null)
        if (input == null || !session.canAddInput(input)) {
            trySend(
                ScanEvent.Failed(ScanError.CameraUnavailable("No se pudo abrir la cámara"), id),
            )
            trySend(ScanEvent.SessionEnded(id))
            close()
            return@callbackFlow
        }
        session.addInput(input)

        val output = AVCaptureVideoDataOutput().apply {
            // Sin esto la cola de frames crece cuando el reconocimiento no llega, y el preview se
            // va retrasando hasta que la app parece colgada. Aquí importa más que en ningún otro
            // motor: reconocer texto en modo `accurate` cuesta un orden de magnitud más que
            // decodificar unas barras, así que se descartan muchos frames a propósito.
            alwaysDiscardsLateVideoFrames = true
        }
        if (!session.canAddOutput(output)) {
            trySend(
                ScanEvent.Failed(ScanError.CameraUnavailable("No se pudo instalar la salida"), id),
            )
            trySend(ScanEvent.SessionEnded(id))
            close()
            return@callbackFlow
        }
        session.addOutput(output)

        // Se lee aquí, en el hilo del que arranca la sesión, y no dentro del delegate: ver
        // `currentCaptureOrientation`.
        val orientation = currentCaptureOrientation()

        val delegate = FrameDelegate { pixelBuffer ->
            val now = time.nowMillis()

            runCatching {
                val handler = VNImageRequestHandler(
                    cVPixelBuffer = pixelBuffer,
                    orientation = orientation,
                    options = emptyMap<Any?, Any?>(),
                )
                OcrCodeInterpreter.interpret(VisionTextRecognizer.linesIn(handler))
            }
                .onSuccess { codes ->
                    // Un frame con texto pero sin ningún checksum válido es un frame analizado sin
                    // resultado, no un fallo: es el caso normal mientras se enfoca la etiqueta.
                    if (codes.isEmpty()) {
                        trySend(ScanEvent.FrameAnalyzed(id, now))
                    } else {
                        trySend(
                            ScanEvent.Detected(
                                codes.map { barcode ->
                                    Detection.of(
                                        barcode = barcode,
                                        engineId = id,
                                        detectedAtMillis = now,
                                        latencyMillis = now - startedAtMillis,
                                    )
                                },
                            ),
                        )
                    }
                }
                .onFailure { failure ->
                    // Un frame ilegible es transitorio: no apaga la cámara ni degrada de motor.
                    trySend(
                        ScanEvent.Failed(ScanError.DecodeFailed(failure.message.orEmpty()), id),
                    )
                }
        }

        // Cola propia y no la principal: aquí se reconoce texto en cada frame, y hacerlo en el hilo
        // de UI congelaría el preview.
        val queue = dispatch_queue_create("com.whyscan.visionocr.frames", null)
        output.setSampleBufferDelegate(delegate, queue)

        sessionHolder.attach(session, camera)
        session.startRunning()
        trySend(ScanEvent.SessionStarted(id))

        awaitClose {
            session.stopRunning()
            output.setSampleBufferDelegate(null, null)
            sessionHolder.detach()
        }
    }

    @Composable
    override fun CameraPreview(modifier: Modifier) {
        RenderVisionOcrPreview(sessionHolder, modifier)
    }

    override suspend fun setTorch(enabled: Boolean) = sessionHolder.setTorch(enabled)

    override suspend fun setZoomRatio(ratio: Float) = sessionHolder.setZoom(ratio)

    /**
     * Es la fuente donde este motor rinde de verdad: una foto quieta y bien enfocada de una etiqueta
     * dañada, que es justo el caso en el que los decodificadores fallan.
     *
     * No hace falta rasterizar a mano como en `:engines:zxing-cpp` —que necesita un búfer de píxeles
     * suyo—: Vision acepta el `CGImage` directamente.
     */
    override suspend fun decode(
        image: ScanImage,
        request: ScanRequest,
    ): Result<List<Barcode>> = runCatching {
        val uiImage = image.encoded.toUIImage()
            ?: error("No se pudo decodificar la imagen (${image.mimeType})")
        val cgImage = uiImage.CGImage ?: error("La imagen no tiene mapa de bits que reconocer")

        val handler = VNImageRequestHandler(
            cGImage = cgImage,
            orientation = uiImage.captureOrientation(),
            options = emptyMap<Any?, Any?>(),
        )
        OcrCodeInterpreter.interpret(VisionTextRecognizer.linesIn(handler))
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toUIImage(): UIImage? = usePinned { pinned ->
    UIImage.imageWithData(NSData.create(bytes = pinned.addressOf(0), length = size.convert()))
}

/**
 * Delegate de Objective-C que recibe cada frame de vídeo.
 *
 * Tiene que ser un `NSObject` que implemente el protocolo; una lambda de Kotlin no vale.
 */
@OptIn(ExperimentalForeignApi::class)
private class FrameDelegate(
    private val onFrame: (CVPixelBufferRef) -> Unit,
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection,
    ) {
        val pixelBuffer = didOutputSampleBuffer?.let(::CMSampleBufferGetImageBuffer) ?: return
        onFrame(pixelBuffer)
    }
}
