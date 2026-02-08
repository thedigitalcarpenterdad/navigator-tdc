package com.tdc.navigator.session

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.tdc.navigator.screen.MainNavigatorScreen
import com.tdc.navigator.screen.VoiceControlScreen
import com.tdc.navigator.service.VehicleControlService
import com.tdc.navigator.util.Logger
import com.tdc.navigator.vehicle.NavigatorVehicleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * TDC Navigator Session
 * 
 * Manages the main car app session and coordinates between
 * vehicle systems, voice control, and UI screens.
 */
class TDCNavigatorSession : Session() {

    companion object {
        private const val TAG = "TDCNavigatorSession"
    }

    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var vehicleManager: NavigatorVehicleManager
    private var isVoiceActive = false

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                Logger.d(TAG, "Session created")
                initializeVehicleManager()
            }

            override fun onStart(owner: LifecycleOwner) {
                Logger.d(TAG, "Session started")
                startVehicleControlService()
            }

            override fun onStop(owner: LifecycleOwner) {
                Logger.d(TAG, "Session stopped")
                stopVehicleControlService()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                Logger.d(TAG, "Session destroyed")
                cleanup()
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen {
        Logger.d(TAG, "Creating screen for intent: ${intent.action}")
        
        return when (intent.action) {
            "com.tdc.navigator.VOICE_CONTROL" -> {
                isVoiceActive = true
                VoiceControlScreen(carContext, vehicleManager)
            }
            else -> {
                MainNavigatorScreen(carContext, vehicleManager)
            }
        }
    }

    private fun initializeVehicleManager() {
        try {
            vehicleManager = NavigatorVehicleManager(carContext)
            vehicleManager.initialize()
            Logger.d(TAG, "Vehicle manager initialized successfully")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize vehicle manager", e)
            // Fallback to limited functionality
            vehicleManager = NavigatorVehicleManager(carContext, limitedMode = true)
        }
    }

    private fun startVehicleControlService() {
        val serviceIntent = Intent(carContext, VehicleControlService::class.java)
        carContext.startForegroundService(serviceIntent)
        Logger.d(TAG, "Vehicle control service started")
    }

    private fun stopVehicleControlService() {
        val serviceIntent = Intent(carContext, VehicleControlService::class.java)
        carContext.stopService(serviceIntent)
        Logger.d(TAG, "Vehicle control service stopped")
    }

    private fun cleanup() {
        sessionScope.cancel()
        if (::vehicleManager.isInitialized) {
            vehicleManager.cleanup()
        }
        Logger.d(TAG, "Session cleanup completed")
    }

    /**
     * Handle wake word activation
     */
    fun onWakeWordDetected(wakeWord: String) {
        Logger.d(TAG, "Wake word detected: $wakeWord")
        
        if (!isVoiceActive) {
            // Switch to voice control screen
            val voiceIntent = Intent("com.tdc.navigator.VOICE_CONTROL")
            carContext.getCarService(androidx.car.app.CarContext.SCREEN_SERVICE)
                ?.let { screenManager ->
                    // Implementation depends on final Car API
                    Logger.d(TAG, "Switching to voice control mode")
                }
        }
    }

    /**
     * Handle voice commands from wake word detection
     */
    fun processVoiceCommand(command: String) {
        Logger.d(TAG, "Processing voice command: $command")
        vehicleManager.processVoiceCommand(command)
    }
}