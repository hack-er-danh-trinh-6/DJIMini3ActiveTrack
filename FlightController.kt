package com.dji.mini3activetrack.sdk

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.flight.FlightControlManager
import timber.log.Timber

object FlightController {

    private val _isFlying = MutableLiveData<Boolean>(false)
    val isFlying: LiveData<Boolean> = _isFlying

    private val _altitude = MutableLiveData<Double>(0.0)
    val altitude: LiveData<Double> = _altitude

    private val _flightMode = MutableLiveData<String>("Unknown")
    val flightMode: LiveData<String> = _flightMode

    fun startMonitoring() {
        // Monitor IsFlying
        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyIsFlying), this) { _, newValue ->
            newValue?.let { _isFlying.postValue(it) }
        }

        // Monitor Altitude
        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyAircraftAltitude), this) { _, newValue ->
            newValue?.let { _altitude.postValue(it) }
        }

        // Monitor Flight Mode
        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyFlightModeString), this) { _, newValue ->
            newValue?.let { _flightMode.postValue(it) }
        }
    }

    fun takeOff(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        FlightControlManager.getInstance().startTakeoff(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() { onSuccess() }
            override fun onFailure(error: IDJIError) { onFailure(error.description()) }
        })
    }

    fun land(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        FlightControlManager.getInstance().startLanding(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() { onSuccess() }
            override fun onFailure(error: IDJIError) { onFailure(error.description()) }
        })
    }

    fun returnToHome(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        FlightControlManager.getInstance().startGoHome(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() { onSuccess() }
            override fun onFailure(error: IDJIError) { onFailure(error.description()) }
        })
    }
}
