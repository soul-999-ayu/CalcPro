package com.devayu.calcpro

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs

class ZoomImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs), ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    private var mScaleDetector: ScaleGestureDetector? = null
    private var mGestureDetector: GestureDetector? = null
    private val mMatrix = Matrix()
    private var mMatrixValues = FloatArray(9)
    var isZoomEnabled = true

    // Zoom Constraints
    private var minScale = 1f
    private var maxScale = 4f
    private var saveScale = 1f

    init {
        scaleType = ScaleType.MATRIX
        mScaleDetector = ScaleGestureDetector(context, this)
        mGestureDetector = GestureDetector(context, this)
        mGestureDetector?.setOnDoubleTapListener(this)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isZoomEnabled) return super.onTouchEvent(event)
        mScaleDetector?.onTouchEvent(event)
        mGestureDetector?.onTouchEvent(event)
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = true

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        var scaleFactor = detector.scaleFactor
        val origScale = saveScale
        saveScale *= scaleFactor

        if (saveScale > maxScale) {
            saveScale = maxScale
            scaleFactor = maxScale / origScale
        } else if (saveScale < minScale) {
            saveScale = minScale
            scaleFactor = minScale / origScale
        }

        if (origScale * scaleFactor < minScale) {
            mMatrix.postScale(minScale / origScale, minScale / origScale, width / 2f, height / 2f)
        } else {
            mMatrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
        }
        fixTranslation()
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {}

    private fun fixTranslation() {
        mMatrix.getValues(mMatrixValues)
        val transX = mMatrixValues[Matrix.MTRANS_X]
        val transY = mMatrixValues[Matrix.MTRANS_Y]
        val fixTransX = getFixTranslation(transX, width.toFloat(), drawable.intrinsicWidth * saveScale)
        val fixTransY = getFixTranslation(transY, height.toFloat(), drawable.intrinsicHeight * saveScale)
        if (fixTransX != 0f || fixTransY != 0f) mMatrix.postTranslate(fixTransX, fixTransY)
        imageMatrix = mMatrix
    }

    private fun getFixTranslation(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }
        if (trans < minTrans) return -trans + minTrans
        if (trans > maxTrans) return -trans + maxTrans
        return 0f
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        val origScale = saveScale
        val targetScale: Float
        targetScale = if (saveScale == minScale) maxScale else minScale
        val scaleFactor = targetScale / origScale
        mMatrix.postScale(scaleFactor, scaleFactor, e.x, e.y)
        saveScale = targetScale
        fixTranslation()
        return true
    }

    // Required Interface Methods (Unused)
    override fun onDown(e: MotionEvent): Boolean = true
    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean = false
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (saveScale == minScale) return false
        mMatrix.postTranslate(-distanceX, -distanceY)
        fixTranslation()
        return true
    }
    override fun onLongPress(e: MotionEvent) {}
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false
    override fun onSingleTapConfirmed(e: MotionEvent): Boolean { return callOnClick() }
    override fun onDoubleTapEvent(e: MotionEvent): Boolean = false

    override fun setImageDrawable(drawable: android.graphics.drawable.Drawable?) {
        super.setImageDrawable(drawable)
        if (drawable == null) return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth == 0f || viewHeight == 0f) return

        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()

        val scaleX = viewWidth / drawableWidth
        val scaleY = viewHeight / drawableHeight
        val scale = kotlin.math.min(scaleX, scaleY)

        mMatrix.setScale(scale, scale)

        // Center the image
        val redundantYSpace = viewHeight - (scale * drawableHeight)
        val redundantXSpace = viewWidth - (scale * drawableWidth)
        mMatrix.postTranslate(redundantXSpace / 2, redundantYSpace / 2)

        saveScale = 1f
        imageMatrix = mMatrix
    }
}