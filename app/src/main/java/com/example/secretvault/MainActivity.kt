package com.example.secretvault

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.secretvault.data.VaultRepository
import com.example.secretvault.security.SecurityManager
import com.example.secretvault.theme.SecretVaultTheme
import com.example.secretvault.theme.VaultBackground
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var securityManager: SecurityManager
    private lateinit var repository: VaultRepository
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    private var lastAcceleration = 0f
    private var currentAcceleration = 0f
    private var shakeAcceleration = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        securityManager = SecurityManager.getInstance(this)
        repository = VaultRepository.getInstance(this)

        // Block screenshots and recents preview if enabled
        if (securityManager.isScreenshotBlocked) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        // Initialize Sensors for Panic Flip/Shake
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH

        enableEdgeToEdge()
        setContent {
            SecretVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VaultBackground
                ) {
                    MainNavigation(
                        securityManager = securityManager,
                        repository = repository
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onStop() {
        super.onStop()
        // Clear any lingering decrypted temp media files
        lifecycleScope.launch {
            repository.clearDecryptedTempFiles()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // 1. Flip face-down detection (phone laid face-down on table: z is negative gravity ~ -9.8)
        if (securityManager.isFlipToLockEnabled && z < -7.5f) {
            if (securityManager.currentMode != com.example.secretvault.model.VaultMode.LOCKED) {
                securityManager.lockVault()
            }
        }

        // 2. Shake detection
        if (securityManager.isShakeToLockEnabled) {
            lastAcceleration = currentAcceleration
            currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta = currentAcceleration - lastAcceleration
            shakeAcceleration = shakeAcceleration * 0.9f + delta
            if (shakeAcceleration > 14f) {
                if (securityManager.currentMode != com.example.secretvault.model.VaultMode.LOCKED) {
                    securityManager.lockVault()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
