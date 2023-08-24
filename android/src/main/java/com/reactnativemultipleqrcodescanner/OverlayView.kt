package com.reactnativemultipleqrcodescanner

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import com.facebook.react.bridge.ReadableMap
import com.google.mlkit.vision.barcode.Barcode
import java.nio.charset.Charset
import java.time.LocalDateTime

class OverlayView : View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var colorMap: ReadableMap? = null
    var colorMapForAlreadyRead: ReadableMap? = null
    var labelMap: ReadableMap? = null
    var overlayAlpha = 100
    var labelAlpha = 100
    var labelFontSize = 40
    var labelDirection = "left"
    var labelColor: String? = null
    var labelColorMap: ReadableMap? = null
    var labeledOnlyPatternMatched = true
    var charset: String? = null
    var barcodes = listOf<Barcode>()
    var barcodesNowReading = mutableMapOf<String, LocalDateTime>()
    var barcodesAlreadyRead = mutableSetOf<String>()

    private var scaleFactorX = 1.0f
    private var scaleFactorY = 1.0f
    private var paint = Paint()
    private val rectF = RectF(0f,0f,0f,0f)
    private var label = ""
    private var offsetX = 0F
    private var offsetY = 0F

    private var areaRect = Rect(0, 0, width, height)

    // dpiを取得してpointから実際のピクセルを計算
    private fun labelFontSizePixel() = labelFontSize.toFloat() / 72.0f * context.resources.displayMetrics.xdpi

    private fun setLabelWithPattern(pattern: String) {
        labelMap?.let {
            label = it.getString(pattern).orEmpty()
        }
    }

    private fun setLabel(code: String) {
        labelMap?.also {
            it.entryIterator.forEach { entry ->
                val pattern = entry.key.toRegex(option = RegexOption.DOT_MATCHES_ALL)
                if (pattern.containsMatchIn(code)) {
                    label = entry.value.toString()
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
          setLabel(code)
        }
        setPaintColor(Color.RED, paint, overlayAlpha)
    }

    private fun setPaintStyleWithColorMap(code: String, paint: Paint): Boolean {
        return run loop@ {
            colorMap?.also {
                it.entryIterator.forEach { entry ->
                    val pattern = entry.key.toRegex(option = RegexOption.DOT_MATCHES_ALL)
                    if (pattern.containsMatchIn(code)) {
                        barcodesNowReading[code] = LocalDateTime.now()
                        if (labeledOnlyPatternMatched) {
                            setLabelWithPattern(entry.key)
                        } else {
                            setLabel(code)
                        }
                        setPaintColor(Color.parseColor(entry.value.toString()), paint, overlayAlpha)
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
                    val pattern = entry.key.toRegex(option = RegexOption.DOT_MATCHES_ALL)
                    if (pattern.containsMatchIn(code)) {
                        if (labeledOnlyPatternMatched) {
                            setLabelWithPattern(entry.key)
                        } else {
                            setLabel(code)
                        }
                        setPaintColor(Color.parseColor(entry.value.toString()), paint, overlayAlpha)
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

    private fun setPaintColor(@ColorInt color: Int, paint: Paint, alpha: Int) {
        paint.apply {
            this.color = color
            this.alpha = alpha
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
        val actualLabelAlpha = if(labelAlpha > 0)  labelAlpha else overlayAlpha
        canvas?.also {c ->
            barcodes.forEach {barcode ->
                barcode.boundingBox?.let { boundingBox ->
                  val rawString = if (charset == null) {
                    barcode.rawValue
                  } else {
                    barcode.rawBytes?.toString(Charset.forName(charset))
                  }
                  rawString?.let {code ->
                        translateRect(boundingBox, rectF)
                        setPaintStyle(code, paint)
                        c.drawRect(rectF, paint)
                        if (labelColor != null) {
                            setPaintColor(Color.parseColor(labelColor), paint, actualLabelAlpha)
                        } else {
                            var found = false
                            labelColorMap?.also {
                                it.entryIterator.forEach inner@{ entry ->
                                    Log.d("color", entry.key + ":" + code)
                                    val pattern = entry.key.toRegex(option = RegexOption.DOT_MATCHES_ALL)
                                    if (pattern.containsMatchIn(code)) {
                                        setPaintColor(Color.parseColor(entry.value.toString()), paint, actualLabelAlpha)

                                        found = true
                                        return@inner
                                    }
                                }
                            }
                            if (!found) {
                                setPaintColor(paint.color, paint, actualLabelAlpha)
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

    // TODO なぜscaleFactorYをかけるのかわからない
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
        Log.d("offset", "(" + offsetX.toString() + "," + offsetY.toString() + ")")

        // dignoではこれをしないとずれてしまう。理由はわからない。
        offsetY = 1300f
        rectF.apply {
            this.left = left - offsetX
            this.right = left + size - offsetX
            this.top = top - offsetY
            this.bottom = top + size - offsetY
        }
    }

    fun update(scanResult: ScanResult) {
        // TODO react nativeからではorientationがわからない
        // TODO 今回のアプリではとりあえずorientationはportrait固定
        if (isPortraitMode()) {
            scaleFactorY = height.toFloat() / scanResult.imageWidth
            scaleFactorX = width.toFloat() / scanResult.imageHeight
        } else {
            scaleFactorY = height.toFloat() / scanResult.imageHeight
            scaleFactorX = width.toFloat() / scanResult.imageWidth
        }
        // TODO　どうしてこの計算で正しいoffsetが出るのかわからない！
        // dignoだと
        // heightは2179
        // widthは1080
        // scanResult.imageWidthは640
        // scanResult.imageHeightは480
        // なぜここでimageHeightとscaleFactorYで計算するとうまくいくのかわからない
        val scaledWidth = scanResult.imageHeight * scaleFactorY
        val diffX = scaledWidth - width
        offsetX = diffX / 2

        barcodes = scanResult.barcodes
        invalidate()
    }

    val rectFForTouch = RectF(0F,0F,0F,0F)
    public fun inRect(x: Float, y:Float, rect: Rect): Boolean {
        translateRect(rect, rectFForTouch)
        return rectFForTouch.contains(x, y)
    }
}
