package com.dji.mini3activetrack.tracking

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object VisionTrackingManager {

    private val _trackingResult = MutableLiveData<TrackingResult>()
    val trackingResult: LiveData<TrackingResult> = _trackingResult

    private val _trackingState = MutableLiveData<TrackingState>(TrackingState.IDLE)
    val trackingState: LiveData<TrackingState> = _trackingState

    private val tracker = OpenCVTracker()
    private val isActive = AtomicBoolean(false)

    private val processingExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "opencv-tracker-thread").apply { isDaemon = true }
    }
    private val processingScope = CoroutineScope(
        processingExecutor.asCoroutineDispatcher() + SupervisorJob()
    )

    private var lastFrameTime = 0L
    private val MIN_FRAME_INTERVAL_MS = 50L // ~20 FPS

    fun selectTarget(frame: Bitmap, boundingBox: RectF) {
        processingScope.launch {
            _trackingState.postValue(TrackingState.SELECTING)
            val success = tracker.initTracker(frame, boundingBox)
            if (success) {
                isActive.set(true)
                _trackingState.postValue(TrackingState.TRACKING)
            } else {
                _trackingState.postValue(TrackingState.IDLE)
            }
        }
    }

    fun processFrame(frame: Bitmap) {
        if (!isActive.get()) return
        val now = System.currentTimeMillis()
        if (now - lastFrameTime < MIN_FRAME_INTERVAL_MS) return
        lastFrameTime = now

        processingScope.launch {
            val result = tracker.processFrame(frame)
            _trackingResult.postValue(result)
            _trackingState.postValue(result.state)

            if (result.state == TrackingState.LOST) {
                isActive.set(false)
            }
        }
    }

    fun stopTracking() {
        isActive.set(false)
        processingScope.launch {
            tracker.reset()
            _trackingState.postValue(TrackingState.IDLE)
        }
    }

    fun isTracking(): Boolean = isActive.get()

    fun destroy() {
        stopTracking()
        processingScope.cancel()
        processingExecutor.shutdown()
    }
}
