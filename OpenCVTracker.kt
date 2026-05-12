package com.dji.mini3activetrack.tracking

import android.graphics.Bitmap
import android.graphics.RectF
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Rect2d
import org.opencv.tracking.TrackerKCF
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class OpenCVTracker {

    private var tracker: TrackerKCF? = null
    private var isInitialized = AtomicBoolean(false)
    private var currentState = TrackingState.IDLE
    private var lostFrameCount = 0
    private val MAX_LOST_FRAMES = 10

    fun initTracker(frame: Bitmap, boundingBox: RectF): Boolean {
        return try {
            val mat = Mat()
            Utils.bitmapToMat(frame, mat)
            
            val rect2d = Rect2d(
                boundingBox.left.toDouble(),
                boundingBox.top.toDouble(),
                boundingBox.width().toDouble(),
                boundingBox.height().toDouble()
            )

            tracker = TrackerKCF.create()
            val success = tracker?.init(mat, rect2d) ?: false
            
            if (success) {
                isInitialized.set(true)
                currentState = TrackingState.TRACKING
                lostFrameCount = 0
            }
            mat.release()
            success
        } catch (e: Exception) {
            Timber.e(e, "Tracker init failed")
            false
        }
    }

    fun processFrame(frame: Bitmap): TrackingResult {
        if (!isInitialized.get() || tracker == null) {
            return TrackingResult(state = TrackingState.IDLE)
        }

        return try {
            val mat = Mat()
            Utils.bitmapToMat(frame, mat)
            
            val boundingBox = Rect2d()
            val success = tracker?.update(mat, boundingBox) ?: false
            mat.release()

            if (success) {
                lostFrameCount = 0
                currentState = TrackingState.TRACKING
                val target = createTrackingTarget(boundingBox, frame.width, frame.height)
                TrackingResult(state = TrackingState.TRACKING, target = target)
            } else {
                lostFrameCount++
                if (lostFrameCount >= MAX_LOST_FRAMES) {
                    currentState = TrackingState.LOST
                    TrackingResult(state = TrackingState.LOST)
                } else {
                    TrackingResult(state = TrackingState.TRACKING)
                }
            }
        } catch (e: Exception) {
            TrackingResult(state = TrackingState.LOST)
        }
    }

    private fun createTrackingTarget(rect: Rect2d, width: Int, height: Int): TrackingTarget {
        val left = rect.x.toFloat()
        val top = rect.y.toFloat()
        val right = (rect.x + rect.width).toFloat()
        val bottom = (rect.y + rect.height).toFloat()
        
        return TrackingTarget(
            boundingBox = RectF(left, top, right, bottom),
            centerX = (left + right) / 2f / width,
            centerY = (top + bottom) / 2f / height,
            normalizedWidth = rect.width.toFloat() / width,
            normalizedHeight = rect.height.toFloat() / height,
            confidence = 1.0f,
            frameWidth = width,
            frameHeight = height
        )
    }

    fun reset() {
        tracker = null
        isInitialized.set(false)
        currentState = TrackingState.IDLE
    }

    fun getState(): TrackingState = currentState
    fun isTracking(): Boolean = isInitialized.get() && currentState == TrackingState.TRACKING
}
