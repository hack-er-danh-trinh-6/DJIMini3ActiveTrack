package com.dji.mini3activetrack.ui

import android.graphics.Bitmap
import android.graphics.RectF
import android.view.TextureView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dji.mini3activetrack.sdk.DJISDKManager
import com.dji.mini3activetrack.sdk.FlightController
import com.dji.mini3activetrack.sdk.VideoStreamManager
import com.dji.mini3activetrack.tracking.TrackingResult
import com.dji.mini3activetrack.tracking.TrackingState
import com.dji.mini3activetrack.tracking.VisionTrackingManager
import com.dji.mini3activetrack.controller.VirtualStickController

class MainViewModel : ViewModel() {

    val connectionState = DJISDKManager.connectionState
    val batteryLevel = DJISDKManager.batteryLevel
    val signalStrength = DJISDKManager.signalStrength
    val productType = DJISDKManager.productType
    
    val isFlying: LiveData<Boolean> = FlightController.isFlying
    val altitude = FlightController.altitude
    val flightMode = FlightController.flightMode

    val trackingState: LiveData<TrackingState> = VisionTrackingManager.trackingState
    val trackingResult: LiveData<TrackingResult> = VisionTrackingManager.trackingResult

    private val _isActiveTrackEnabled = MutableLiveData<Boolean>(false)
    val isActiveTrackEnabled: LiveData<Boolean> = _isActiveTrackEnabled

    private val _activeTrackStatus = MutableLiveData<String>("IDLE")
    val activeTrackStatus: LiveData<String> = _activeTrackStatus

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun onTextureViewReady(textureView: TextureView) {
        VideoStreamManager.setCameraToVideoMode()
        VideoStreamManager.startStream(textureView)
    }

    fun onTextureViewDestroyed(textureView: TextureView) {
        VideoStreamManager.stopStream(textureView)
    }

    fun toggleTakeoffLand() {
        if (isFlying.value == true) {
            FlightController.land({}, {})
        } else {
            FlightController.takeOff({}, {})
        }
    }

    fun takeOff() = toggleTakeoffLand()
    fun land() = toggleTakeoffLand()
    fun returnToHome() = FlightController.returnToHome({}, {})

    fun toggleActiveTrack() {
        val current = _isActiveTrackEnabled.value ?: false
        if (current) {
            disableActiveTrack()
        } else {
            enableActiveTrack()
        }
    }

    fun enableActiveTrack() {
        _isActiveTrackEnabled.value = true
        _activeTrackStatus.value = "READY - Draw target box"
        VirtualStickController.enable({}, {})
    }

    fun disableActiveTrack() {
        _isActiveTrackEnabled.value = false
        _activeTrackStatus.value = "IDLE"
        VisionTrackingManager.stopTracking()
        VirtualStickController.disable({}, {})
    }

    fun onTargetSelected(frame: Bitmap, rect: RectF) {
        VisionTrackingManager.selectTarget(frame, rect)
        VirtualStickController.startTracking()
    }

    fun onTrackingResultUpdated(result: TrackingResult) {
        VirtualStickController.updateTrackingResult(result)
    }

    fun clearToast() { _toastMessage.value = null }
}
