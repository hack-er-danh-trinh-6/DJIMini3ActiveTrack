package com.dji.mini3activetrack.sdk

import android.graphics.Bitmap
import android.view.TextureView
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.camera.CameraMode
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.interfaces.ICameraStreamManager
import timber.log.Timber
import java.nio.ByteBuffer

object VideoStreamManager {

    private var frameCallback: ((Bitmap) -> Unit)? = null
    private var streamWidth = 1280
    private var streamHeight = 720

    private val cameraStreamListener = ICameraStreamManager.CameraFrameListener { frameData, offset, length, width, height, format ->
        streamWidth = width
        streamHeight = height
        frameCallback?.let { callback ->
            try {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(frameData, offset, length))
                callback(bitmap)
            } catch (e: Exception) {
                Timber.e(e, "Error converting frame to bitmap")
            }
        }
    }

    fun setCameraToVideoMode() {
        KeyManager.getInstance().setValue(KeyTools.createKey(CameraKey.KeyCameraMode), CameraMode.VIDEO_NORMAL, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() { Timber.i("Camera mode set to VIDEO") }
            override fun onFailure(error: IDJIError) { Timber.e("Failed to set camera mode") }
        })
    }

    fun startStream(textureView: TextureView) {
        val manager = MediaDataCenter.getInstance().cameraStreamManager
        manager.addStreamView(textureView)
        manager.addFrameListener(ComponentIndexType.LEFT_OR_MAIN, ICameraStreamManager.FrameFormat.RGBA_8888, cameraStreamListener)
    }

    fun stopStream(textureView: TextureView) {
        val manager = MediaDataCenter.getInstance().cameraStreamManager
        manager.removeStreamView(textureView)
        manager.removeFrameListener(cameraStreamListener)
    }

    fun setFrameCallback(callback: (Bitmap) -> Unit) { this.frameCallback = callback }
    fun removeFrameCallback() { this.frameCallback = null }
    fun getStreamWidth(): Int = streamWidth
    fun getStreamHeight(): Int = streamHeight
}
