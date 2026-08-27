package com.whyscan.engines.ocr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIColor
import platform.UIKit.UIView

/**
 * Superficie de vídeo del OCR de iOS.
 *
 * La capa de Core Animation y el reconocedor de texto son dos consumidores de la misma
 * `AVCaptureSession`, no uno alimentando al otro: la capa pinta los frames y
 * `AVCaptureVideoDataOutput` los entrega al motor.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
internal fun RenderVisionOcrPreview(holder: OcrCameraSessionHolder, modifier: Modifier) {
    val view = remember { OcrPreviewUIView() }

    DisposableEffect(holder) {
        val listener: (AVCaptureSession?) -> Unit = { session -> view.bind(session) }
        holder.addListener(listener)
        onDispose {
            holder.removeListener(listener)
            view.bind(null)
        }
    }

    UIKitView(factory = { view }, modifier = modifier)
}

/**
 * `UIView` cuya capa de preview sigue el tamaño de la vista: un `CALayer` no se redimensiona solo.
 */
@OptIn(ExperimentalForeignApi::class)
private class OcrPreviewUIView : UIView(frame = CGRectZero.readValue()) {

    private val previewLayer = AVCaptureVideoPreviewLayer().apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    init {
        backgroundColor = UIColor.blackColor
        layer.addSublayer(previewLayer)
    }

    fun bind(session: AVCaptureSession?) {
        previewLayer.session = session
        setNeedsLayout()
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        // Sin desactivar las acciones implícitas, cada cambio de tamaño se anima y el preview
        // "salta" al rotar.
        CATransaction.begin()
        CATransaction.setValue(true, kCATransactionDisableActions)
        previewLayer.setFrame(currentBounds())
        CATransaction.commit()
    }

    private fun currentBounds(): CValue<CGRect> = bounds
}
