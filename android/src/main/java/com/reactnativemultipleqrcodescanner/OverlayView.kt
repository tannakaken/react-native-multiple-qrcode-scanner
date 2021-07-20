package com.lixil_qr_code_android

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import com.facebook.react.bridge.ReadableMap
import com.google.mlkit.vision.barcode.Barcode
import com.reactnativemultipleqrcodescanner.ScanResult
import java.time.LocalDateTime

class OverlayView : View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var colorMap: ReadableMap? = null
    var colorMapForAlreadyRead: ReadableMap? = null
    var labelMap: ReadableMap? = null
    var overlayAlpha = 100
    var labelFontSize = 40
    var labelDirection = "left"
    var labelColor: String? = null
    var labelColorMap: ReadableMap? = null
    var labeledOnlyPatternMatched = true
    var barcodes = listOf<Barcode>()
    var barcodesNowReading = mutableMapOf<String, LocalDateTime>()
    var barcodesAlreadyRead = mutableSetOf<String>()
    private var scaleFactorX = 1.0f
    private var scaleFactorY = 1.0f
    private var paint = Paint()
    private val rectF = RectF(0f,0f,0f,0f)
    private var label = ""
    private var offsetX = 0F

    private var areaRect = Rect(0, 0, width, height)

    // dpiを取得してpointから実際のピクセルを計算
    private fun labelFontSizePixel() = labelFontSize.toFloat() / 72.0f * context.resources.displayMetrics.xdpi

    private fun setLabel(pattern: String, code: String) {
        if (labeledOnlyPatternMatched) {
            labelMap?.let {
                label = it.getString(pattern).orEmpty()
            }
        } else {
            labelMap?.also {
                it.entryIterator.forEach { entry ->
                  val pattern = entry.key.toRegex()
                  if (pattern.matches(code)) {
                      label = entry.value.toString()
                      return
                  }
                }
            }
        }

    }

    private fun setPaintStyle(code: String, paint: Paint) {
        barcodesNowReading[code]?.let {
            if (it.isBefore(LocalDateTime.now().minusSeconds(1))) {
                barcodesAlreadyRead.add(code)
            }
            barcodesNowReading.remove(code)
        }
        if (barcodesAlreadyRead.contains(code)) {
            if (setPaintStyleWithColorMapForAlreadyRead(code, paint)) {
                return
            }
        } else {
            if (setPaintStyleWithColorMap(code, paint)) {
                return
            }
        }
        if (labeledOnlyPatternMatched) {
          label = ""
        } else {
          setLabel("", code)
        }
        setPaintColor(Color.RED, paint)
    }

    private fun setPaintStyleWithColorMap(code: String, paint: Paint): Boolean {
        return run loop@ {
            colorMap?.also {
                it.entryIterator.forEach { entry ->
                    val pattern = entry.key.toRegex()
                    if (pattern.matches(code)) {
                        barcodesNowReading[code] = LocalDateTime.now()
                        setLabel(entry.key, code)
                        setPaintColor(Color.parseColor(entry.value.toString()), paint)
                        return@loop true
                    }
                }
            }
            return@loop false
        }
    }

    private fun setPaintStyleWithColorMapForAlreadyRead(code: String, paint: Paint): Boolean {
        return run loop@ {
            colorMapForAlreadyRead?.also {
                it.entryIterator.forEach { entry ->
                    val pattern = entry.key.toRegex()
                    if (pattern.matches(code)) {
                        setLabel(entry.key, code)
                        setPaintColor(Color.parseColor(entry.value.toString()), paint)
                        return@loop true
                    }
                }
            } ?:run {
                if (setPaintStyleWithColorMap(code, paint)) {
                    return@loop true
                }
            }
            return@loop false
        }
    }

    private fun setPaintColor(@ColorInt color: Int, paint: Paint) {
        paint.apply {
            this.color = color
            alpha = overlayAlpha
            strokeWidth = 5F
            style = Paint.Style.FILL
            textSize = labelFontSizePixel()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        areaRect.set(left, top, right, bottom)
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.also {c ->
            barcodes.forEach {barcode ->
                barcode.boundingBox?.let { boundingBox ->
                    barcode.rawValue?.let {code ->
                        translateRect(boundingBox, rectF)
                        setPaintStyle(code, paint)
                        c.drawRect(rectF, paint)
                        if (labelColor != null) {
                            setPaintColor(Color.parseColor(labelColor), paint)
                        } else {
                            labelColorMap?.also {
                                it.entryIterator.forEach { entry ->
                                    val pattern = entry.key.toRegex()
                                    if (pattern.matches(code)) {
                                        setPaintColor(Color.parseColor(entry.value.toString()), paint)
                                        return@forEach
                                    }
                                }
                            }
                        }
                        if (labelDirection == "right") {
                            c.drawText(label, rectF.right, rectF.top + labelFontSizePixel(), paint)
                        } else if (labelDirection == "bottom") {
                          c.drawText(label, rectF.left, rectF.top + rectF.height() + labelFontSizePixel(), paint)
                        }
                    }
                }
            }
        }
    }

    private fun isPortraitMode(): Boolean {
        val orientation: Int = resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_PORTRAIT
    }

    private fun translateX(x: Float): Float = x * scaleFactorY
    private fun translateY(y: Float): Float = y * scaleFactorY

    private fun translateRect(rect: Rect, rectF: RectF) {
        val left = translateX(rect.left.toFloat())
        val right = translateX(rect.right.toFloat())
        val top = translateY(rect.top.toFloat())
        val bottom = translateY((rect.bottom.toFloat()))
        val rectWidth = right - left
        val rectHeight = bottom - top
        val size = if (rectWidth > rectHeight) rectWidth else rectHeight
        rectF.apply {
            this.left = left - offsetX
            this.right = left + size - offsetX
            this.top = top
            this.bottom = top + size
        }
    }

    fun update(scanResult: ScanResult) {
        // TODO react nativeからではorientationがわからない
        // TODO 今回のアプリではとりあえずorientationは固定
        if (isPortraitMode()) {
            scaleFactorY = height.toFloat() / scanResult.imageWidth
            scaleFactorX = width.toFloat() / scanResult.imageHeight
        } else {
            scaleFactorY = height.toFloat() / scanResult.imageHeight
            scaleFactorX = width.toFloat() / scanResult.imageWidth
        }
        // TODO　どうしてこの計算で正しいoffsetが出るのかわからない！
        val scaledWidth = scanResult.imageHeight * scaleFactorY
        val diff = scaledWidth - width
        offsetX = diff / 2
        barcodes = scanResult.barcodes
        invalidate()
    }

    val rectFForTouch = RectF(0F,0F,0F,0F)
    public fun inRect(x: Float, y:Float, rect: Rect): Boolean {
        translateRect(rect, rectFForTouch)
        return rectFForTouch.contains(x, y)
    }
}
