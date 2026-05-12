package com.dji.mini3activetrack.ui

import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.dji.mini3activetrack.databinding.ActivityMainBinding
import com.dji.mini3activetrack.tracking.BoundingBoxView
import com.dji.mini3activetrack.tracking.VisionTrackingManager
import com.dji.mini3activetrack.sdk.VideoStreamManager
import com.dji.mini3activetrack.tracking.TrackingState
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnTakeoff.setOnClickListener {
            viewModel.toggleTakeoffLand()
        }

        binding.btnActiveTrack.setOnClickListener {
            viewModel.toggleActiveTrack()
        }

        binding.boundingBoxView.onTargetSelected = { rect ->
            val bitmap = binding.textureView.bitmap
            if (bitmap != null) {
                viewModel.onTargetSelected(bitmap, rect)
            } else {
                Toast.makeText(this, "Video stream not ready", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                viewModel.onTextureViewReady(binding.textureView)
            }
            override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                viewModel.onTextureViewDestroyed(binding.textureView)
                return true
            }
            override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
        }
    }

    private fun observeViewModel() {
        viewModel.isFlying.observe(this) { isFlying ->
            binding.btnTakeoff.text = if (isFlying) "Land" else "Takeoff"
        }

        viewModel.activeTrackStatus.observe(this) { status ->
            binding.tvTrackingStatus.text = status
        }
        
        viewModel.trackingResult.observe(this) { result ->
            viewModel.onTrackingResultUpdated(result)
            result.target?.let { target ->
                binding.boundingBoxView.updateTrackingBox(target.boundingBox, result.state == TrackingState.LOST)
            }
        }

        VideoStreamManager.setFrameCallback { bitmap ->
            VisionTrackingManager.processFrame(bitmap)
        }
    }
}
