package com.reactnativemultipleqrcodescanner

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.TimeUnit

data class ScanResult(val barcodes: List<Barcode>, val imageWidth: Int, val imageHeight: Int)

typealias onDetectListener = (results: ScanResult) -> Unit

class QRCodeAnalyzer(
        private val onQRCodeDetectedListener: onDetectListener
) : ImageAnalysis.Analyzer {
    var lastAnalyzedTimestamp = 0L

    /**
     * Realtime analyze (called by CameraFragment)
     */
    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp >= TimeUnit.MILLISECONDS.toMillis(100)) {
            parse(imageProxy)
            lastAnalyzedTimestamp = currentTimestamp
        } else {
            imageProxy.close()
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    fun parse(imageProxy: ImageProxy) {
        val inputImage = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    onQRCodeDetectedListener(ScanResult(barcodes, inputImage.width, inputImage.height))
                }
                .addOnFailureListener {
                    it.message?.also { Log.e(TAG, it) }
                }.addOnCompleteListener {
                    imageProxy.close()
                }
    }

    companion object {
        private const val TAG = "QRCodeImageAnalysis"
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        val barcodeScanner = BarcodeScanning.getClient(options)
    }
}
