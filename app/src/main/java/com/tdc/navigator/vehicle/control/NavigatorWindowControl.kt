package com.tdc.navigator.vehicle.control

import android.car.VehicleAreaType
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyManager
import android.car.hardware.cabin.CarCabinManager
import com.tdc.navigator.util.Logger
import kotlinx.coroutines.delay
import java.util.regex.Pattern

/**
 * Navigator Window Control
 * 
 * Handles all window and sunroof operations for the 2025 Lincoln Navigator
 * including individual window control and panoramic sunroof management.
 */
class NavigatorWindowControl(
    private val cabinManager: CarCabinManager?,
    private val propertyManager: CarPropertyManager?
) {
    
    companion object {
        private const val TAG = "NavigatorWindowControl"
        
        // Window positions (0-100%)
        private const val WINDOW_FULLY_CLOSED = 0
        private const val WINDOW_FULLY_OPEN = 100
        private const val WINDOW_CRACKED = 10 // Slightly open
        private const val WINDOW_HALFWAY = 50
        
        // Navigator window zones
        private const val DRIVER_WINDOW = VehicleAreaType.VEHICLE_AREA_TYPE_WINDOW_FRONT_LEFT
        private const val PASSENGER_WINDOW = VehicleAreaType.VEHICLE_AREA_TYPE_WINDOW_FRONT_RIGHT
        private const val REAR_LEFT_WINDOW = VehicleAreaType.VEHICLE_AREA_TYPE_WINDOW_REAR_LEFT
        private const val REAR_RIGHT_WINDOW = VehicleAreaType.VEHICLE_AREA_TYPE_WINDOW_REAR_RIGHT
        private const val SUNROOF = VehicleAreaType.VEHICLE_AREA_TYPE_WINDOW_ROOF_TOP_1
        
        // Safety limits
        private const val MAX_OPERATION_TIME_MS = 10000L // 10 seconds max operation time
    }
    
    private val isSimulated = cabinManager == null
    
    // Current window states (for simulation)
    private var driverWindowPosition = WINDOW_FULLY_CLOSED
    private var passengerWindowPosition = WINDOW_FULLY_CLOSED
    private var rearLeftWindowPosition = WINDOW_FULLY_CLOSED
    private var rearRightWindowPosition = WINDOW_FULLY_CLOSED
    private var sunroofPosition = WINDOW_FULLY_CLOSED
    private var sunroofTiltPosition = WINDOW_FULLY_CLOSED
    
    // Safety tracking
    private val activeOperations = mutableSetOf<Int>()
    
    suspend fun processCommand(command: String) {
        Logger.d(TAG, "Processing window command: '$command'")
        
        try {
            when {
                // Individual window commands
                command.contains("driver window") -> {
                    handleWindowCommand(command, DRIVER_WINDOW)
                }
                
                command.contains("passenger window") -> {
                    handleWindowCommand(command, PASSENGER_WINDOW)
                }
                
                command.contains("rear left window") -> {
                    handleWindowCommand(command, REAR_LEFT_WINDOW)
                }
                
                command.contains("rear right window") -> {
                    handleWindowCommand(command, REAR_RIGHT_WINDOW)
                }
                
                // Multiple window commands
                command.contains("front windows") -> {
                    handleMultipleWindows(command, listOf(DRIVER_WINDOW, PASSENGER_WINDOW))
                }
                
                command.contains("rear windows") -> {
                    handleMultipleWindows(command, listOf(REAR_LEFT_WINDOW, REAR_RIGHT_WINDOW))
                }
                
                command.contains("all windows") -> {
                    handleMultipleWindows(command, listOf(DRIVER_WINDOW, PASSENGER_WINDOW, REAR_LEFT_WINDOW, REAR_RIGHT_WINDOW))
                }
                
                // Generic window commands (default to driver)
                (command.contains("window") && !command.contains("sunroof")) -> {
                    handleWindowCommand(command, DRIVER_WINDOW)
                }
                
                // Sunroof commands
                command.contains("sunroof") || command.contains("moonroof") || command.contains("panoramic roof") -> {
                    handleSunroofCommand(command)
                }
                
                else -> {
                    Logger.d(TAG, "Unrecognized window command: '$command'")
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error processing window command", e)
        }
    }
    
    private suspend fun handleWindowCommand(command: String, windowZone: Int) {
        when {
            command.contains("open") -> {
                if (command.contains("halfway") || command.contains("half")) {
                    setWindowPosition(windowZone, WINDOW_HALFWAY)
                } else if (command.contains("crack") || command.contains("slightly")) {
                    setWindowPosition(windowZone, WINDOW_CRACKED)
                } else {
                    setWindowPosition(windowZone, WINDOW_FULLY_OPEN)
                }
            }
            
            command.contains("close") -> {
                setWindowPosition(windowZone, WINDOW_FULLY_CLOSED)
            }
            
            command.contains("up") -> {
                setWindowPosition(windowZone, WINDOW_FULLY_CLOSED)
            }
            
            command.contains("down") -> {
                setWindowPosition(windowZone, WINDOW_FULLY_OPEN)
            }
            
            command.contains("stop") -> {
                stopWindowOperation(windowZone)
            }
            
            // Percentage commands
            command.matches(Regex(".*\\d+%.*")) -> {
                handlePercentageCommand(command, windowZone)
            }
        }
    }
    
    private suspend fun handleMultipleWindows(command: String, windows: List<Int>) {
        Logger.d(TAG, "Handling multiple window command for ${windows.size} windows")
        
        // Execute commands simultaneously for all specified windows
        windows.forEach { window ->
            handleWindowCommand(command, window)
        }
    }
    
    private suspend fun handleSunroofCommand(command: String) {
        when {
            command.contains("open") -> {
                if (command.contains("tilt") || command.contains("vent")) {
                    setSunroofTilt(50) // Partially tilt
                } else {
                    setSunroofPosition(WINDOW_FULLY_OPEN)
                }
            }
            
            command.contains("close") -> {
                setSunroofPosition(WINDOW_FULLY_CLOSED)
                setSunroofTilt(WINDOW_FULLY_CLOSED)
            }
            
            command.contains("tilt") -> {
                if (command.contains("open") || command.contains("up")) {
                    setSunroofTilt(100)
                } else if (command.contains("close") || command.contains("down")) {
                    setSunroofTilt(0)
                } else {
                    setSunroofTilt(50) // Default tilt
                }
            }
            
            command.contains("vent") -> {
                setSunroofPosition(WINDOW_CRACKED)
            }
            
            command.contains("stop") -> {
                stopWindowOperation(SUNROOF)
            }
            
            // Percentage commands for sunroof
            command.matches(Regex(".*\\d+%.*")) -> {
                handlePercentageCommand(command, SUNROOF)
            }
        }
    }
    
    private suspend fun handlePercentageCommand(command: String, windowZone: Int) {
        val percentagePattern = Pattern.compile("(\\d+)%")
        val matcher = percentagePattern.matcher(command)
        
        if (matcher.find()) {
            val percentage = matcher.group(1)?.toIntOrNull()
            if (percentage != null && percentage in 0..100) {
                if (windowZone == SUNROOF && command.contains("tilt")) {
                    setSunroofTilt(percentage)
                } else {
                    setWindowPosition(windowZone, percentage)
                }
            }
        }
    }
    
    private suspend fun setWindowPosition(windowZone: Int, position: Int) {
        val clampedPosition = position.coerceIn(WINDOW_FULLY_CLOSED, WINDOW_FULLY_OPEN)
        val windowName = getWindowName(windowZone)
        
        Logger.d(TAG, "Setting $windowName to $clampedPosition% position")
        
        // Safety check - don't operate if already in motion
        if (activeOperations.contains(windowZone)) {
            Logger.w(TAG, "Window $windowName already in operation, skipping")
            return
        }
        
        activeOperations.add(windowZone)
        
        try {
            if (isSimulated) {
                // Simulate window movement time
                val currentPosition = getCurrentWindowPosition(windowZone)
                val movementTime = calculateMovementTime(currentPosition, clampedPosition)
                
                // Update simulated state
                updateSimulatedWindowPosition(windowZone, clampedPosition)
                
                delay(movementTime)
                Logger.d(TAG, "$windowName moved to $clampedPosition% (simulated)")
                
            } else {
                // Real vehicle control
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.WINDOW_POS,
                    windowZone,
                    clampedPosition
                )
                
                // Monitor movement completion
                monitorWindowMovement(windowZone, clampedPosition)
            }
            
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to set window position for $windowName", e)
        } finally {
            activeOperations.remove(windowZone)
        }
    }
    
    private suspend fun setSunroofPosition(position: Int) {
        setWindowPosition(SUNROOF, position)
    }
    
    private suspend fun setSunroofTilt(tiltPosition: Int) {
        val clampedTilt = tiltPosition.coerceIn(WINDOW_FULLY_CLOSED, WINDOW_FULLY_OPEN)
        Logger.d(TAG, "Setting sunroof tilt to $clampedTilt%")
        
        if (isSimulated) {
            sunroofTiltPosition = clampedTilt
            delay(2000) // Simulate tilt movement time
        } else {
            // Use a separate property for tilt if available
            try {
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.WINDOW_POS, // May need a specific tilt property
                    VehicleAreaType.VEHICLE_AREA_TYPE_WINDOW_ROOF_TOP_2, // Tilt mechanism
                    clampedTilt
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set sunroof tilt", e)
            }
        }
    }
    
    private suspend fun stopWindowOperation(windowZone: Int) {
        val windowName = getWindowName(windowZone)
        Logger.d(TAG, "Stopping operation for $windowName")
        
        if (isSimulated) {
            // In simulation, just remove from active operations
            activeOperations.remove(windowZone)
        } else {
            try {
                // Send stop command to window motor
                // This may require a specific "stop" property or setting current position
                val currentPos = getCurrentWindowPosition(windowZone)
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.WINDOW_POS,
                    windowZone,
                    currentPos
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to stop window operation for $windowName", e)
            }
        }
    }
    
    private suspend fun monitorWindowMovement(windowZone: Int, targetPosition: Int) {
        val startTime = System.currentTimeMillis()
        var currentPosition = getCurrentWindowPosition(windowZone)
        
        while (System.currentTimeMillis() - startTime < MAX_OPERATION_TIME_MS) {
            delay(200) // Check every 200ms
            
            val newPosition = getCurrentWindowPosition(windowZone)
            if (kotlin.math.abs(newPosition - targetPosition) <= 2) { // Within 2% tolerance
                Logger.d(TAG, "Window movement completed")
                return
            }
            
            currentPosition = newPosition
        }
        
        Logger.w(TAG, "Window movement timeout - may not have reached target position")
    }
    
    private fun getCurrentWindowPosition(windowZone: Int): Int {
        return if (isSimulated) {
            when (windowZone) {
                DRIVER_WINDOW -> driverWindowPosition
                PASSENGER_WINDOW -> passengerWindowPosition
                REAR_LEFT_WINDOW -> rearLeftWindowPosition
                REAR_RIGHT_WINDOW -> rearRightWindowPosition
                SUNROOF -> sunroofPosition
                else -> WINDOW_FULLY_CLOSED
            }
        } else {
            try {
                propertyManager?.getIntProperty(
                    VehiclePropertyIds.WINDOW_POS,
                    windowZone
                ) ?: WINDOW_FULLY_CLOSED
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to get window position", e)
                WINDOW_FULLY_CLOSED
            }
        }
    }
    
    private fun updateSimulatedWindowPosition(windowZone: Int, position: Int) {
        when (windowZone) {
            DRIVER_WINDOW -> driverWindowPosition = position
            PASSENGER_WINDOW -> passengerWindowPosition = position
            REAR_LEFT_WINDOW -> rearLeftWindowPosition = position
            REAR_RIGHT_WINDOW -> rearRightWindowPosition = position
            SUNROOF -> sunroofPosition = position
        }
    }
    
    private fun calculateMovementTime(currentPos: Int, targetPos: Int): Long {
        val distance = kotlin.math.abs(targetPos - currentPos)
        // Assume 1% position change = 100ms movement time
        return (distance * 100L).coerceAtMost(MAX_OPERATION_TIME_MS)
    }
    
    private fun getWindowName(windowZone: Int): String {
        return when (windowZone) {
            DRIVER_WINDOW -> "driver window"
            PASSENGER_WINDOW -> "passenger window"
            REAR_LEFT_WINDOW -> "rear left window"
            REAR_RIGHT_WINDOW -> "rear right window"
            SUNROOF -> "sunroof"
            else -> "unknown window"
        }
    }
    
    fun emergencyStop() {
        Logger.w(TAG, "Emergency stop - closing all windows immediately")
        
        // Stop all active operations
        activeOperations.clear()
        
        // Close all windows for safety
        if (isSimulated) {
            driverWindowPosition = WINDOW_FULLY_CLOSED
            passengerWindowPosition = WINDOW_FULLY_CLOSED
            rearLeftWindowPosition = WINDOW_FULLY_CLOSED
            rearRightWindowPosition = WINDOW_FULLY_CLOSED
            sunroofPosition = WINDOW_FULLY_CLOSED
            sunroofTiltPosition = WINDOW_FULLY_CLOSED
        } else {
            try {
                val windows = listOf(DRIVER_WINDOW, PASSENGER_WINDOW, REAR_LEFT_WINDOW, REAR_RIGHT_WINDOW, SUNROOF)
                windows.forEach { window ->
                    propertyManager?.setIntProperty(
                        VehiclePropertyIds.WINDOW_POS,
                        window,
                        WINDOW_FULLY_CLOSED
                    )
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error during emergency window closure", e)
            }
        }
    }
    
    fun getCurrentStatus(): WindowStatus {
        return WindowStatus(
            driverWindowPosition = driverWindowPosition,
            passengerWindowPosition = passengerWindowPosition,
            rearLeftWindowPosition = rearLeftWindowPosition,
            rearRightWindowPosition = rearRightWindowPosition,
            sunroofPosition = sunroofPosition,
            sunroofTiltPosition = sunroofTiltPosition,
            activeOperations = activeOperations.toList()
        )
    }
}

data class WindowStatus(
    val driverWindowPosition: Int,
    val passengerWindowPosition: Int,
    val rearLeftWindowPosition: Int,
    val rearRightWindowPosition: Int,
    val sunroofPosition: Int,
    val sunroofTiltPosition: Int,
    val activeOperations: List<Int>
)