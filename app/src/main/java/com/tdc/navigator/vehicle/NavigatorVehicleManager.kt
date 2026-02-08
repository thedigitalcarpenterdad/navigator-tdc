package com.tdc.navigator.vehicle

import android.car.Car
import android.car.CarNotConnectedException
import android.car.hardware.CarPropertyManager
import android.car.hardware.cabin.CarCabinManager
import android.car.hardware.hvac.CarHvacManager
import android.car.hardware.property.CarPropertyManager
import androidx.car.app.CarContext
import com.tdc.navigator.util.Logger
import com.tdc.navigator.vehicle.control.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Navigator Vehicle Manager
 * 
 * Central coordinator for all 2025 Lincoln Navigator vehicle systems.
 * Provides high-level interface for voice commands and vehicle control.
 */
class NavigatorVehicleManager(
    private val carContext: CarContext,
    private val limitedMode: Boolean = false
) {
    
    companion object {
        private const val TAG = "NavigatorVehicleManager"
    }
    
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Android Automotive car managers
    private var car: Car? = null
    private var hvacManager: CarHvacManager? = null
    private var cabinManager: CarCabinManager? = null
    private var propertyManager: CarPropertyManager? = null
    
    // Lincoln Navigator specific control modules
    private lateinit var climateControl: NavigatorClimateControl
    private lateinit var windowControl: NavigatorWindowControl
    private lateinit var seatControl: NavigatorSeatControl
    private lateinit var lightingControl: NavigatorLightingControl
    private lateinit var doorControl: NavigatorDoorControl
    private lateinit var mirrorControl: NavigatorMirrorControl
    private lateinit var audioControl: NavigatorAudioControl
    private lateinit var driveModeControl: NavigatorDriveModeControl
    
    private var isInitialized = false
    
    fun initialize() {
        if (limitedMode) {
            initializeLimitedMode()
            return
        }
        
        try {
            // Connect to Android Automotive Car API
            car = Car.createCar(carContext)
            
            // Get vehicle system managers
            hvacManager = car?.getCarManager(Car.HVAC_SERVICE) as? CarHvacManager
            cabinManager = car?.getCarManager(Car.CABIN_SERVICE) as? CarCabinManager
            propertyManager = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            
            // Initialize control modules
            initializeControlModules()
            
            isInitialized = true
            Logger.d(TAG, "Navigator vehicle manager initialized successfully")
            
        } catch (e: CarNotConnectedException) {
            Logger.e(TAG, "Car API not available", e)
            initializeLimitedMode()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize vehicle manager", e)
            throw e
        }
    }
    
    private fun initializeLimitedMode() {
        Logger.w(TAG, "Initializing in limited mode (simulation)")
        // Initialize with simulated control modules for testing
        initializeControlModules()
        isInitialized = true
    }
    
    private fun initializeControlModules() {
        climateControl = NavigatorClimateControl(hvacManager, propertyManager)
        windowControl = NavigatorWindowControl(cabinManager, propertyManager)
        seatControl = NavigatorSeatControl(cabinManager, propertyManager)
        lightingControl = NavigatorLightingControl(cabinManager, propertyManager)
        doorControl = NavigatorDoorControl(cabinManager, propertyManager)
        mirrorControl = NavigatorMirrorControl(cabinManager, propertyManager)
        audioControl = NavigatorAudioControl(carContext)
        driveModeControl = NavigatorDriveModeControl(propertyManager)
    }
    
    /**
     * Process voice commands and route to appropriate control modules
     */
    fun processVoiceCommand(command: String) {
        if (!isInitialized) {
            Logger.w(TAG, "Vehicle manager not initialized")
            return
        }
        
        managerScope.launch {
            try {
                val normalizedCommand = command.lowercase().trim()
                Logger.d(TAG, "Processing voice command: '$normalizedCommand'")
                
                when {
                    // Climate Control Commands
                    isClimateCommand(normalizedCommand) -> {
                        climateControl.processCommand(normalizedCommand)
                    }
                    
                    // Window Control Commands
                    isWindowCommand(normalizedCommand) -> {
                        windowControl.processCommand(normalizedCommand)
                    }
                    
                    // Seat Control Commands
                    isSeatCommand(normalizedCommand) -> {
                        seatControl.processCommand(normalizedCommand)
                    }
                    
                    // Lighting Control Commands
                    isLightingCommand(normalizedCommand) -> {
                        lightingControl.processCommand(normalizedCommand)
                    }
                    
                    // Door Control Commands
                    isDoorCommand(normalizedCommand) -> {
                        doorControl.processCommand(normalizedCommand)
                    }
                    
                    // Mirror Control Commands
                    isMirrorCommand(normalizedCommand) -> {
                        mirrorControl.processCommand(normalizedCommand)
                    }
                    
                    // Audio Control Commands
                    isAudioCommand(normalizedCommand) -> {
                        audioControl.processCommand(normalizedCommand)
                    }
                    
                    // Drive Mode Commands
                    isDriveModeCommand(normalizedCommand) -> {
                        driveModeControl.processCommand(normalizedCommand)
                    }
                    
                    else -> {
                        Logger.d(TAG, "Unrecognized command: '$normalizedCommand'")
                        // Could integrate with general TDC AI for fallback
                    }
                }
                
            } catch (e: Exception) {
                Logger.e(TAG, "Error processing voice command: '$command'", e)
            }
        }
    }
    
    // Command pattern matching functions
    private fun isClimateCommand(command: String): Boolean {
        val climateKeywords = arrayOf(
            "temperature", "temp", "climate", "heat", "cool", "ac", "air conditioning",
            "fan", "defrost", "humidity", "zone"
        )
        return climateKeywords.any { command.contains(it) }
    }
    
    private fun isWindowCommand(command: String): Boolean {
        val windowKeywords = arrayOf(
            "window", "sunroof", "moonroof", "panoramic roof"
        )
        return windowKeywords.any { command.contains(it) }
    }
    
    private fun isSeatCommand(command: String): Boolean {
        val seatKeywords = arrayOf(
            "seat", "massage", "lumbar", "position", "memory"
        )
        return seatKeywords.any { command.contains(it) }
    }
    
    private fun isLightingCommand(command: String): Boolean {
        val lightingKeywords = arrayOf(
            "light", "lighting", "ambient", "dome", "headlight", "taillight"
        )
        return lightingKeywords.any { command.contains(it) }
    }
    
    private fun isDoorCommand(command: String): Boolean {
        val doorKeywords = arrayOf(
            "door", "lock", "unlock", "trunk", "liftgate"
        )
        return doorKeywords.any { command.contains(it) }
    }
    
    private fun isMirrorCommand(command: String): Boolean {
        val mirrorKeywords = arrayOf(
            "mirror", "side mirror", "rearview"
        )
        return mirrorKeywords.any { command.contains(it) }
    }
    
    private fun isAudioCommand(command: String): Boolean {
        val audioKeywords = arrayOf(
            "audio", "music", "radio", "volume", "sound", "revel"
        )
        return audioKeywords.any { command.contains(it) }
    }
    
    private fun isDriveModeCommand(command: String): Boolean {
        val driveModeKeywords = arrayOf(
            "drive mode", "sport", "comfort", "eco", "normal", "towing"
        )
        return driveModeKeywords.any { command.contains(it) }
    }
    
    /**
     * Get current vehicle status for all systems
     */
    fun getVehicleStatus(): NavigatorVehicleStatus {
        return NavigatorVehicleStatus(
            climate = climateControl.getCurrentStatus(),
            windows = windowControl.getCurrentStatus(),
            seats = seatControl.getCurrentStatus(),
            lighting = lightingControl.getCurrentStatus(),
            doors = doorControl.getCurrentStatus(),
            mirrors = mirrorControl.getCurrentStatus(),
            audio = audioControl.getCurrentStatus(),
            driveMode = driveModeControl.getCurrentStatus(),
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Emergency stop - immediately halt all vehicle operations
     */
    fun emergencyStop() {
        Logger.w(TAG, "Emergency stop triggered")
        
        try {
            // Stop all potentially dangerous operations
            windowControl.emergencyStop()
            seatControl.emergencyStop()
            driveModeControl.emergencyStop()
            
            Logger.d(TAG, "Emergency stop completed")
        } catch (e: Exception) {
            Logger.e(TAG, "Error during emergency stop", e)
        }
    }
    
    fun cleanup() {
        Logger.d(TAG, "Cleaning up vehicle manager")
        
        managerScope.cancel()
        
        try {
            car?.disconnect()
        } catch (e: Exception) {
            Logger.e(TAG, "Error disconnecting from car API", e)
        }
        
        isInitialized = false
    }
}