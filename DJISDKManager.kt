package com.dji.mini3activetrack.sdk

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.ProductKey
import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.sdk.keyvalue.value.product.ProductType
import dji.v5.common.error.IDJIError
import dji.v5.manager.SDKManager
import dji.v5.manager.KeyManager
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.manager.interfaces.DJISDKInitEvent
import dji.v5.common.callback.CommonCallbacks
import timber.log.Timber

object DJISDKManager {

    private val _connectionState = MutableLiveData<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: LiveData<ConnectionState> = _connectionState

    private val _productType = MutableLiveData<ProductType>(ProductType.UNKNOWN_AIRCRAFT)
    val productType: LiveData<ProductType> = _productType

    private val _batteryLevel = MutableLiveData<Int>(0)
    val batteryLevel: LiveData<Int> = _batteryLevel

    private val _signalStrength = MutableLiveData<Int>(0)
    val signalStrength: LiveData<Int> = _signalStrength

    private val _isRegistered = MutableLiveData<Boolean>(false)
    val isRegistered: LiveData<Boolean> = _isRegistered

    fun initSDK(context: Context) {
        Timber.d("Initializing DJI SDK...")
        _connectionState.postValue(ConnectionState.CONNECTING)

        SDKManager.getInstance().init(context, object : SDKManagerCallback {
            override fun onRegisterSuccess() {
                _isRegistered.postValue(true)
                _connectionState.postValue(ConnectionState.SDK_REGISTERED)
                startProductKeyListeners()
            }

            override fun onRegisterFailure(error: IDJIError?) {
                _connectionState.postValue(ConnectionState.ERROR)
            }

            override fun onProductConnect(productId: Int) {
                _connectionState.postValue(ConnectionState.CONNECTED)
                updateProductType()
            }

            override fun onProductDisconnect(productId: Int) {
                _connectionState.postValue(ConnectionState.DISCONNECTED)
            }

            override fun onComponentChange(key: dji.sdk.keyvalue.key.DJIKeyInfo<*>?, head: Int, body: Int, comp: Int, connected: Boolean) {}
            override fun onInitProcess(event: DJISDKInitEvent?, total: Int) {}
            override fun onDatabaseDownloadProgress(current: Long, total: Long) {}
        })
    }

    private fun startProductKeyListeners() {
        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyBatteryPowerPercent), this) { value ->
            value?.let { _batteryLevel.postValue(it) }
        }

        KeyManager.getInstance().listen(KeyTools.createKey(RemoteControllerKey.KeySignalQuality), this) { value ->
            value?.let { _signalStrength.postValue(it) }
        }
    }

    private fun updateProductType() {
        KeyManager.getInstance().listen(KeyTools.createKey(ProductKey.KeyProductType), this) { value ->
            value?.let { _productType.postValue(it) }
        }
    }

    fun isAircraftConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED

    enum class ConnectionState { DISCONNECTED, CONNECTING, SDK_REGISTERED, CONNECTED, ERROR }
}
