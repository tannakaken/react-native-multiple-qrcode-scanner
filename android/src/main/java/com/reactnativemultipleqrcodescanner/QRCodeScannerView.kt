package com.reactnativemultipleqrcodescanner

import android.content.Context
import android.widget.FrameLayout
import androidx.camera.view.PreviewView
import com.lixil_qr_code_android.OverlayView

class QRCodeScannerView(context: Context): FrameLayout(context) {
    public var previewView: PreviewView? = null
    public var overlayView: OverlayView? = null
}
