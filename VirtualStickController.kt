package com.dji.mini3activetrack.controller

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager
import dji.v5.manager.aircraft.virtualstick.VirtualStickRequest
import dji.v5.manager.aircraft.virtualstick.VirtualStickFlightControlParam
import com.dji.mini3activetrack.tracking.TrackingResult
import com.dji.mini3activetrack.tracking.TrackingState
import timber.log.Timber

object VirtualStickController {

    private val _isEnabled = MutableLiveData<Boolean>(false)
    val isEnabled: LiveData<Boolean> = _isEnabled

    private val pidYaw = PIDController(0.5, 0.01, 0.1)
    private val pidPitch = PIDController(0.3, 0.01, 0.05)
    private val pidThrottle = PIDController(0.4, 0.01, 0.08)

    private var isTracking = false

    fun enable(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val request = VirtualStickRequest.Builder().build()
        VirtualStickManager.getInstance().enableVirtualStick(request, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _isEnabled.postValue(true)
                onSuccess()
            }
            override fun onFailure(error: IDJIError) {
                onFailure(error.description())
            }
        })
    }

    fun disable(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        VirtualStickManager.getInstance().disableVirtualStick(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _isEnabled.postValue(false)
                onSuccess()
            }
            override fun onFailure(error: IDJIError) {
                onFailure(error.description())
            }
        })
    }

    fun startTracking() { isTracking = true }
    fun stopTracking() { 
        isTracking = false
        sendControlCommand(0f, 0f, 0f, 0f)
    }

    fun updateTrackingResult(result: TrackingResult) {
        if (!isTracking || result.state != TrackingState.TRACKING) return
        
        result.target?.let { target ->
            val yaw = pidYaw.update(target.errorX.toDouble()).toFloat()
            val pitch = pidPitch.update(target.errorY.toDouble()).toFloat()
            val throttle = pidThrottle.update((0.15f - target.normalizedArea).toDouble()).toFloat()
            
            sendControlCommand(0f, pitch, yaw, throttle)
        }
    }

    private fun sendControlCommand(roll: Float, pitch: Float, yaw: Float, throttle: Float) {
        val param = VirtualStickFlightControlParam()
        param.roll = roll.toDouble()
        param.pitch = pitch.toDouble()
        param.yaw = yaw.toDouble()
        param.verticalThrottle = throttle.toDouble()
        VirtualStickManager.getInstance().sendVirtualStickFlightControlParam(param)
    }

    fun resetSearch() {
        pidYaw.reset()
        pidPitch.reset()
        pidThrottle.reset()
    }

    fun destroy() {
        stopTracking()
    }
}
