package com.reactnativemultipleqrcodescanner

import android.util.Log
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.annotation.NonNull
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.lixil_qr_code_android.OverlayView
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class QRCodeScannerViewManager() : SimpleViewManager<QRCodeScannerView>() {
    val REACT_CLASS = "QRCodeScannerView"

    @NonNull
    override fun getName(): String {
        return REACT_CLASS
    }

    @NonNull
    override fun createViewInstance(@NonNull context: ThemedReactContext): QRCodeScannerView {
        val previewView = PreviewView(context)
        val overlayView = OverlayView(context)
        val qrCodeScannerView = QRCodeScannerView(context)
        qrCodeScannerView.addView(previewView)
        qrCodeScannerView.previewView = previewView
        qrCodeScannerView.addView(overlayView)
        qrCodeScannerView.overlayView = overlayView
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val cameraExecutor = Executors.newSingleThreadExecutor()
            // Select the back facing lens
            val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            /* Setup Preview */
            val preview = Preview.Builder().build()

            /* Setup Pickinglist Analyzer */
            val imageAnalyzer = ImageAnalysis.Builder().build()
                    .also {
                        it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { scanResult ->
                            run {
                                overlayView.update(scanResult)
                                scanResult.barcodes.forEach { barcode ->
                                    val event: WritableMap = Arguments.createMap()
                                    event.putString("code", barcode.rawValue)
                                    val reactContext = qrCodeScannerView.context as ReactContext
                                    reactContext.getJSModule(RCTEventEmitter::class.java)
                                        .receiveEvent(
                                            qrCodeScannerView.getId(),
                                            "onQRCodeRead",
                                            event
                                        )
                                }
                            }
                        })
                    }

            /* Unbind use cases before rebinding */
            cameraProvider.unbindAll()

            /* Bind camera */
            try {
                val camera = cameraProvider.bindToLifecycle(context.currentActivity as LifecycleOwner, cameraSelector, preview, imageAnalyzer)
                preview.setSurfaceProvider(previewView.surfaceProvider)
                // autofocus
                qrCodeScannerView.afterMeasured {
                    val autoFocusPoint = SurfaceOrientedMeteringPointFactory(1f, 1f)
                            .createPoint(.5f, .5f)
                    try {
                        val autoFocusAction = FocusMeteringAction.Builder(
                                autoFocusPoint,
                                FocusMeteringAction.FLAG_AF
                        ).apply {
                            //start auto-focusing after 2 seconds
                            setAutoCancelDuration(10, TimeUnit.SECONDS)
                        }.build()
                        camera.cameraControl.startFocusAndMetering(autoFocusAction)
                    } catch (e: CameraInfoUnavailableException) {
                        Log.d("ERROR", "cannot access camera", e)
                    }
                }
                previewView.setOnTouchListener (View.OnTouchListener { view: View, motionEvent: MotionEvent ->
                    when (motionEvent.action) {
                        // touch qrcode
                        MotionEvent.ACTION_DOWN -> {
                            overlayView.barcodes.forEach { barcode ->
                                barcode.boundingBox?.let { boundingBox ->
                                    barcode.rawValue?.let { code ->
                                        if (overlayView.inRect(motionEvent.x, motionEvent.y, boundingBox)) {
                                            val event: WritableMap = Arguments.createMap()
                                            event.putString("code", code)
                                            val reactContext = qrCodeScannerView.context as ReactContext
                                            reactContext.getJSModule(RCTEventEmitter::class.java)
                                                .receiveEvent(
                                                    qrCodeScannerView.getId(),
                                                    "onQRCodeTouch",
                                                    event
                                                )
                                        }
                                    }
                                }
                            }

                            true
                        }
                        // touch focus
                        MotionEvent.ACTION_UP -> {
                            view.performClick()
                            val factory = previewView.getMeteringPointFactory()
                            val point = factory.createPoint(motionEvent.x, motionEvent.y)
                            val action = FocusMeteringAction.Builder(point).build()
                            camera.cameraControl.startFocusAndMetering(action)
                            true
                        }
                        else -> false
                    }
                })
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        },
                ContextCompat.getMainExecutor(context))

        setupLayoutHack(qrCodeScannerView)
        return qrCodeScannerView
    }

    inline fun View.afterMeasured(crossinline block: () -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    block()
                }
            }
        })
    }

    companion object {
        private val TAG = "QRCodeScannerViewManager"
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
        return MapBuilder.builder<String, Any>()
                .put("onQRCodeRead", MapBuilder.of("registrationName", "onQRCodeRead"))
                .put("onQRCodeTouch", MapBuilder.of("registrationName", "onQRCodeTouch"))
                .build()
    }

    @ReactProp(name = "colorMap")
    fun setColorMap(view: QRCodeScannerView, colorMap: ReadableMap?) {
        view.overlayView?.also {
            it.colorMap = colorMap
            it.barcodesAlreadyRead = mutableSetOf();
            it.barcodesNowReading = mutableMapOf()
        }
    }

    @ReactProp(name = "colorMapForAlreadyRead")
    fun setColorMapForAlreadyRead(view: QRCodeScannerView, colorMapForAlreadyRead: ReadableMap?) {
        view.overlayView?.also {
            it.colorMapForAlreadyRead = colorMapForAlreadyRead
            it.barcodesAlreadyRead = mutableSetOf();
            it.barcodesNowReading = mutableMapOf()
        }
    }

    @ReactProp(name = "labelMap")
    fun setLabelMap(view: QRCodeScannerView, labelMap: ReadableMap?) {
        view.overlayView?.also {
            it.labelMap = labelMap
        }
    }

    @ReactProp(name = "alpha")
    fun setAlpha(view: QRCodeScannerView, alpha: Int?) {
        view.overlayView?.also {
            if (alpha != null) {
                it.overlayAlpha = alpha
            }

        }
    }

    @ReactProp(name = "labelFontSize")
    fun setLabelFontSize(view: QRCodeScannerView, labelFontSize: Int?) {
        view.overlayView?.also {
            if (labelFontSize != null) {
                it.labelFontSize = labelFontSize
            }
        }
    }

    @ReactProp(name = "labelDirection")
    fun setLabelDirection(view: QRCodeScannerView, labelDirection: String?) {
        view.overlayView?.also {
            if (labelDirection == "right" || labelDirection == "bottom") {
                it.labelDirection = labelDirection;
            }
        }
    }

    @ReactProp(name = "labelColor")
    fun setLabelColor(view: QRCodeScannerView, labelColor: String?) {
      view.overlayView?.also {
        it.labelColor = labelColor
      }
    }

    @ReactProp(name = "labelColorMap")
    fun setLabelColorMap(view: QRCodeScannerView, labelColorMap: ReadableMap?) {
        view.overlayView?.also {
            it.labelColorMap = labelColorMap
        }
    }

    @ReactProp(name = "labeledOnlyPatternMatched")
    fun setLabeledOnlyPatternMatched(view: QRCodeScannerView, labeledOnlyPatternMatched: Boolean) {
        view.overlayView?.also {
            it.labeledOnlyPatternMatched = labeledOnlyPatternMatched;
        }
    }
    /**
     * React Nativeから呼び出すと、カメラが接続したタイミングで画面のrerenderingが走らない。
     * なので手動でレイアウトする
     */
    fun setupLayoutHack(layout: FrameLayout) {
        Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
//                Log.d(TAG, "setupLayoutHack");
                manuallyLayoutChildren(layout)
                layout.getViewTreeObserver().dispatchOnGlobalLayout()
                Choreographer.getInstance().postFrameCallback(this)
            }
        })
    }

    fun manuallyLayoutChildren(layout: FrameLayout) {
        for (i in 0 until layout.getChildCount()) {
            Log.d(TAG, "manuallyLayoutChildren")
            val child: View = layout.getChildAt(i)
            child.measure(MeasureSpec.makeMeasureSpec(layout.getMeasuredWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(layout.getMeasuredHeight(), MeasureSpec.EXACTLY))
            child.layout(0, 0, child.measuredWidth, child.measuredHeight)
        }
    }
}
