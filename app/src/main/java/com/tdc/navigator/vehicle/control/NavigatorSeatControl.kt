package com.tdc.navigator.vehicle.control

import android.car.VehicleAreaType
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyManager
import android.car.hardware.cabin.CarCabinManager
import com.tdc.navigator.util.Logger
import kotlinx.coroutines.delay
import java.util.regex.Pattern

/**
 * Navigator Seat Control
 * 
 * Handles all seat operations for the 2025 Lincoln Navigator including:
 * - Heating and cooling
 * - Massage functions
 * - Position adjustments
 * - Memory settings
 */
class NavigatorSeatControl(
    private val cabinManager: CarCabinManager?,
    private val propertyManager: CarPropertyManager?
) {
    
    companion object {
        private const val TAG = "NavigatorSeatControl"
        
        // Seat heating/cooling levels (0-3 for Navigator)
        private const val OFF = 0
        private const val LOW = 1
        private const val MEDIUM = 2
        private const val HIGH = 3
        
        // Massage intensity levels (0-5 for Navigator premium seats)
        private const val MASSAGE_OFF = 0
        private const val MASSAGE_LOW = 1
        private const val MASSAGE_MEDIUM = 3
        private const val MASSAGE_HIGH = 5
        
        // Navigator seat zones
        private const val DRIVER_SEAT = VehicleAreaType.VEHICLE_AREA_TYPE_SEAT_ROW_1_LEFT
        private const val PASSENGER_SEAT = VehicleAreaType.VEHICLE_AREA_TYPE_SEAT_ROW_1_RIGHT
        private const val REAR_LEFT_SEAT = VehicleAreaType.VEHICLE_AREA_TYPE_SEAT_ROW_2_LEFT
        private const val REAR_RIGHT_SEAT = VehicleAreaType.VEHICLE_AREA_TYPE_SEAT_ROW_2_RIGHT
        
        // Position limits (0-100%)
        private const val MIN_POSITION = 0
        private const val MAX_POSITION = 100
        
        // Memory positions (1-3 for Navigator)
        private const val MEMORY_POSITION_1 = 1
        private const val MEMORY_POSITION_2 = 2
        private const val MEMORY_POSITION_3 = 3
    }
    
    private val isSimulated = cabinManager == null
    
    // Current seat states (for simulation)
    private var driverHeatLevel = OFF
    private var passengerHeatLevel = OFF
    private var driverCoolLevel = OFF
    private var passengerCoolLevel = OFF
    
    private var driverMassageIntensity = MASSAGE_OFF
    private var passengerMassageIntensity = MASSAGE_OFF
    private var driverMassageMode = MassageMode.OFF
    private var passengerMassageMode = MassageMode.OFF
    
    // Position tracking (simplified - real implementation would have multiple motors)
    private var driverSeatPosition = SeatPosition()
    private var passengerSeatPosition = SeatPosition()
    
    enum class MassageMode {
        OFF, WAVE, PULSE, CONSTANT, FOCUS
    }
    
    data class SeatPosition(
        val height: Int = 50,
        val fore_aft: Int = 50,
        val tilt: Int = 50,
        val lumbar_support: Int = 50,
        val lumbar_height: Int = 50,
        val headrest_height: Int = 50,
        val headrest_angle: Int = 50
    )
    
    suspend fun processCommand(command: String) {
        Logger.d(TAG, "Processing seat command: '$command'")
        
        try {
            val seat = getSeatFromCommand(command)
            
            when {
                // Heating commands
                command.contains("heat") -> {
                    handleHeatingCommand(command, seat)
                }
                
                // Cooling commands
                command.contains("cool") || command.contains("ventilat") -> {
                    handleCoolingCommand(command, seat)
                }
                
                // Massage commands
                command.contains("massage") -> {
                    handleMassageCommand(command, seat)
                }
                
                // Position commands
                command.contains("position") || command.contains("adjust") -> {
                    handlePositionCommand(command, seat)
                }
                
                // Memory commands
                command.contains("memory") -> {
                    handleMemoryCommand(command, seat)
                }
                
                // Lumbar support commands
                command.contains("lumbar") -> {
                    handleLumbarCommand(command, seat)
                }
                
                else -> {
                    Logger.d(TAG, "Unrecognized seat command: '$command'")
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error processing seat command", e)
        }
    }
    
    private fun getSeatFromCommand(command: String): Int {
        return when {
            command.contains("driver") -> DRIVER_SEAT
            command.contains("passenger") -> PASSENGER_SEAT
            command.contains("rear left") -> REAR_LEFT_SEAT
            command.contains("rear right") -> REAR_RIGHT_SEAT
            else -> DRIVER_SEAT // Default to driver seat
        }
    }
    
    private suspend fun handleHeatingCommand(command: String, seat: Int) {
        val level = when {
            command.contains("off") || command.contains("stop") -> OFF
            command.contains("low") || command.contains("1") -> LOW
            command.contains("medium") || command.contains("2") -> MEDIUM
            command.contains("high") || command.contains("max") || command.contains("3") -> HIGH
            command.contains("on") -> LOW // Default to low when just turning on
            else -> {
                // Try to extract numeric level
                val numberPattern = Pattern.compile("level (\\d)")
                val matcher = numberPattern.matcher(command)
                if (matcher.find()) {
                    matcher.group(1)?.toIntOrNull()?.coerceIn(0, 3) ?: OFF
                } else {
                    OFF
                }
            }
        }
        
        setSeatHeating(seat, level)
    }
    
    private suspend fun handleCoolingCommand(command: String, seat: Int) {
        val level = when {
            command.contains("off") || command.contains("stop") -> OFF
            command.contains("low") || command.contains("1") -> LOW
            command.contains("medium") || command.contains("2") -> MEDIUM
            command.contains("high") || command.contains("max") || command.contains("3") -> HIGH
            command.contains("on") -> LOW // Default to low when just turning on
            else -> {
                // Try to extract numeric level
                val numberPattern = Pattern.compile("level (\\d)")
                val matcher = numberPattern.matcher(command)
                if (matcher.find()) {
                    matcher.group(1)?.toIntOrNull()?.coerceIn(0, 3) ?: OFF
                } else {
                    OFF
                }
            }
        }
        
        setSeatCooling(seat, level)
    }
    
    private suspend fun handleMassageCommand(command: String, seat: Int) {
        when {
            command.contains("off") || command.contains("stop") -> {
                setSeatMassage(seat, MASSAGE_OFF, MassageMode.OFF)
            }
            
            command.contains("wave") -> {
                val intensity = getMassageIntensityFromCommand(command)
                setSeatMassage(seat, intensity, MassageMode.WAVE)
            }
            
            command.contains("pulse") -> {
                val intensity = getMassageIntensityFromCommand(command)
                setSeatMassage(seat, intensity, MassageMode.PULSE)
            }
            
            command.contains("constant") || command.contains("continuous") -> {
                val intensity = getMassageIntensityFromCommand(command)
                setSeatMassage(seat, intensity, MassageMode.CONSTANT)
            }
            
            command.contains("focus") -> {
                val intensity = getMassageIntensityFromCommand(command)
                setSeatMassage(seat, intensity, MassageMode.FOCUS)
            }
            
            command.contains("on") -> {
                // Default to wave mode, medium intensity
                setSeatMassage(seat, MASSAGE_MEDIUM, MassageMode.WAVE)
            }
            
            else -> {
                // Just intensity change, keep current mode
                val intensity = getMassageIntensityFromCommand(command)
                val currentMode = if (seat == DRIVER_SEAT) driverMassageMode else passengerMassageMode
                setSeatMassage(seat, intensity, currentMode)
            }
        }
    }
    
    private fun getMassageIntensityFromCommand(command: String): Int {
        return when {
            command.contains("low") || command.contains("1") -> MASSAGE_LOW
            command.contains("medium") || command.contains("3") -> MASSAGE_MEDIUM
            command.contains("high") || command.contains("max") || command.contains("5") -> MASSAGE_HIGH
            else -> {
                // Try to extract numeric intensity (1-5)
                val numberPattern = Pattern.compile("intensity (\\d)")
                val matcher = numberPattern.matcher(command)
                if (matcher.find()) {
                    matcher.group(1)?.toIntOrNull()?.coerceIn(1, 5) ?: MASSAGE_MEDIUM
                } else {
                    MASSAGE_MEDIUM
                }
            }
        }
    }
    
    private suspend fun handlePositionCommand(command: String, seat: Int) {
        when {
            command.contains("up") && command.contains("height") -> {
                adjustSeatHeight(seat, 10)
            }
            
            command.contains("down") && command.contains("height") -> {
                adjustSeatHeight(seat, -10)
            }
            
            command.contains("forward") -> {
                adjustSeatForeAft(seat, 10)
            }
            
            command.contains("back") || command.contains("backward") -> {
                adjustSeatForeAft(seat, -10)
            }
            
            command.contains("tilt") -> {
                if (command.contains("up")) {
                    adjustSeatTilt(seat, 10)
                } else if (command.contains("down")) {
                    adjustSeatTilt(seat, -10)
                }
            }
            
            command.contains("reset") || command.contains("default") -> {
                resetSeatPosition(seat)
            }
        }
    }
    
    private suspend fun handleMemoryCommand(command: String, seat: Int) {
        val memoryPosition = when {
            command.contains("1") -> MEMORY_POSITION_1
            command.contains("2") -> MEMORY_POSITION_2
            command.contains("3") -> MEMORY_POSITION_3
            else -> MEMORY_POSITION_1 // Default
        }
        
        if (command.contains("save") || command.contains("store")) {
            saveSeatMemory(seat, memoryPosition)
        } else if (command.contains("recall") || command.contains("load")) {
            recallSeatMemory(seat, memoryPosition)
        } else {
            // Default to recall
            recallSeatMemory(seat, memoryPosition)
        }
    }
    
    private suspend fun handleLumbarCommand(command: String, seat: Int) {
        when {
            command.contains("up") -> {
                adjustLumbarHeight(seat, 10)
            }
            
            command.contains("down") -> {
                adjustLumbarHeight(seat, -10)
            }
            
            command.contains("more") || command.contains("increase") -> {
                adjustLumbarSupport(seat, 10)
            }
            
            command.contains("less") || command.contains("decrease") -> {
                adjustLumbarSupport(seat, -10)
            }
        }
    }
    
    private suspend fun setSeatHeating(seat: Int, level: Int) {
        val clampedLevel = level.coerceIn(OFF, HIGH)
        val seatName = getSeatName(seat)
        
        Logger.d(TAG, "Setting $seatName heating to level $clampedLevel")
        
        if (isSimulated) {
            when (seat) {
                DRIVER_SEAT -> driverHeatLevel = clampedLevel
                PASSENGER_SEAT -> passengerHeatLevel = clampedLevel
                // Rear seats may not have heating in all trim levels
            }
            delay(500) // Simulate heating activation time
        } else {
            try {
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.HVAC_SEAT_TEMPERATURE,
                    seat,
                    clampedLevel
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set seat heating", e)
            }
        }
    }
    
    private suspend fun setSeatCooling(seat: Int, level: Int) {
        val clampedLevel = level.coerceIn(OFF, HIGH)
        val seatName = getSeatName(seat)
        
        Logger.d(TAG, "Setting $seatName cooling to level $clampedLevel")
        
        if (isSimulated) {
            when (seat) {
                DRIVER_SEAT -> driverCoolLevel = clampedLevel
                PASSENGER_SEAT -> passengerCoolLevel = clampedLevel
                // Rear seats may not have cooling
            }
            delay(500) // Simulate cooling activation time
        } else {
            try {
                // May use same property with negative values, or separate property
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.HVAC_SEAT_TEMPERATURE,
                    seat,
                    -clampedLevel // Negative for cooling
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set seat cooling", e)
            }
        }
    }
    
    private suspend fun setSeatMassage(seat: Int, intensity: Int, mode: MassageMode) {
        val clampedIntensity = intensity.coerceIn(MASSAGE_OFF, MASSAGE_HIGH)
        val seatName = getSeatName(seat)
        
        Logger.d(TAG, "Setting $seatName massage to $mode mode, intensity $clampedIntensity")
        
        if (isSimulated) {
            when (seat) {
                DRIVER_SEAT -> {
                    driverMassageIntensity = clampedIntensity
                    driverMassageMode = mode
                }
                PASSENGER_SEAT -> {
                    passengerMassageIntensity = clampedIntensity
                    passengerMassageMode = mode
                }
                // Only front seats typically have massage
            }
            delay(1000) // Simulate massage mode setup time
        } else {
            try {
                // Set massage intensity
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.SEAT_MASSAGE_LEVEL,
                    seat,
                    clampedIntensity
                )
                
                // Set massage mode (may need custom property)
                // propertyManager?.setIntProperty(CUSTOM_SEAT_MASSAGE_MODE, seat, mode.ordinal)
                
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set seat massage", e)
            }
        }
    }
    
    private suspend fun adjustSeatHeight(seat: Int, adjustment: Int) {
        val currentPos = if (seat == DRIVER_SEAT) driverSeatPosition else passengerSeatPosition
        val newHeight = (currentPos.height + adjustment).coerceIn(MIN_POSITION, MAX_POSITION)
        
        Logger.d(TAG, "Adjusting ${getSeatName(seat)} height by $adjustment% to $newHeight%")
        
        if (isSimulated) {
            val newPosition = if (seat == DRIVER_SEAT) {
                driverSeatPosition.copy(height = newHeight).also { driverSeatPosition = it }
            } else {
                passengerSeatPosition.copy(height = newHeight).also { passengerSeatPosition = it }
            }
            delay(800) // Simulate adjustment time
        } else {
            try {
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.SEAT_HEIGHT_POS,
                    seat,
                    newHeight
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to adjust seat height", e)
            }
        }
    }
    
    private suspend fun adjustSeatForeAft(seat: Int, adjustment: Int) {
        val currentPos = if (seat == DRIVER_SEAT) driverSeatPosition else passengerSeatPosition
        val newPosition = (currentPos.fore_aft + adjustment).coerceIn(MIN_POSITION, MAX_POSITION)
        
        Logger.d(TAG, "Adjusting ${getSeatName(seat)} fore/aft by $adjustment% to $newPosition%")
        
        if (isSimulated) {
            if (seat == DRIVER_SEAT) {
                driverSeatPosition = driverSeatPosition.copy(fore_aft = newPosition)
            } else {
                passengerSeatPosition = passengerSeatPosition.copy(fore_aft = newPosition)
            }
            delay(1000) // Simulate adjustment time
        } else {
            try {
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.SEAT_FORE_AFT_POS,
                    seat,
                    newPosition
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to adjust seat fore/aft position", e)
            }
        }
    }
    
    private suspend fun adjustSeatTilt(seat: Int, adjustment: Int) {
        val currentPos = if (seat == DRIVER_SEAT) driverSeatPosition else passengerSeatPosition
        val newTilt = (currentPos.tilt + adjustment).coerceIn(MIN_POSITION, MAX_POSITION)
        
        Logger.d(TAG, "Adjusting ${getSeatName(seat)} tilt by $adjustment% to $newTilt%")
        
        if (isSimulated) {
            if (seat == DRIVER_SEAT) {
                driverSeatPosition = driverSeatPosition.copy(tilt = newTilt)
            } else {
                passengerSeatPosition = passengerSeatPosition.copy(tilt = newTilt)
            }
            delay(800)
        } else {
            try {
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.SEAT_TILT_POS,
                    seat,
                    newTilt
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to adjust seat tilt", e)
            }
        }
    }
    
    private suspend fun adjustLumbarSupport(seat: Int, adjustment: Int) {
        val currentPos = if (seat == DRIVER_SEAT) driverSeatPosition else passengerSeatPosition
        val newLumbar = (currentPos.lumbar_support + adjustment).coerceIn(MIN_POSITION, MAX_POSITION)
        
        Logger.d(TAG, "Adjusting ${getSeatName(seat)} lumbar support by $adjustment% to $newLumbar%")
        
        if (isSimulated) {
            if (seat == DRIVER_SEAT) {
                driverSeatPosition = driverSeatPosition.copy(lumbar_support = newLumbar)
            } else {
                passengerSeatPosition = passengerSeatPosition.copy(lumbar_support = newLumbar)
            }
            delay(600)
        } else {
            try {
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.SEAT_LUMBAR_FORE_AFT_POS,
                    seat,
                    newLumbar
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to adjust lumbar support", e)
            }
        }
    }
    
    private suspend fun adjustLumbarHeight(seat: Int, adjustment: Int) {
        val currentPos = if (seat == DRIVER_SEAT) driverSeatPosition else passengerSeatPosition
        val newHeight = (currentPos.lumbar_height + adjustment).coerceIn(MIN_POSITION, MAX_POSITION)
        
        Logger.d(TAG, "Adjusting ${getSeatName(seat)} lumbar height by $adjustment% to $newHeight%")
        
        if (isSimulated) {
            if (seat == DRIVER_SEAT) {
                driverSeatPosition = driverSeatPosition.copy(lumbar_height = newHeight)
            } else {
                passengerSeatPosition = passengerSeatPosition.copy(lumbar_height = newHeight)
            }
            delay(600)
        } else {
            try {
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.SEAT_LUMBAR_SIDE_SUPPORT_POS,
                    seat,
                    newHeight
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to adjust lumbar height", e)
            }
        }
    }
    
    private suspend fun resetSeatPosition(seat: Int) {
        Logger.d(TAG, "Resetting ${getSeatName(seat)} to default position")
        
        val defaultPosition = SeatPosition()
        
        if (isSimulated) {
            if (seat == DRIVER_SEAT) {
                driverSeatPosition = defaultPosition
            } else {
                passengerSeatPosition = defaultPosition
            }
            delay(2000) // Simulate full position reset time
        } else {
            // Set all position properties to default values
            try {
                propertyManager?.apply {
                    setIntProperty(VehiclePropertyIds.SEAT_HEIGHT_POS, seat, defaultPosition.height)
                    setIntProperty(VehiclePropertyIds.SEAT_FORE_AFT_POS, seat, defaultPosition.fore_aft)
                    setIntProperty(VehiclePropertyIds.SEAT_TILT_POS, seat, defaultPosition.tilt)
                    setIntProperty(VehiclePropertyIds.SEAT_LUMBAR_FORE_AFT_POS, seat, defaultPosition.lumbar_support)
                    setIntProperty(VehiclePropertyIds.SEAT_LUMBAR_SIDE_SUPPORT_POS, seat, defaultPosition.lumbar_height)
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to reset seat position", e)
            }
        }
    }
    
    private suspend fun saveSeatMemory(seat: Int, memoryPosition: Int) {
        Logger.d(TAG, "Saving ${getSeatName(seat)} memory position $memoryPosition")
        
        if (isSimulated) {
            // In simulation, just log the save operation
            delay(1000)
            Logger.d(TAG, "Memory position $memoryPosition saved (simulated)")
        } else {
            try {
                // Save current seat position to memory
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.SEAT_MEMORY_SET,
                    seat,
                    memoryPosition
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to save seat memory", e)
            }
        }
    }
    
    private suspend fun recallSeatMemory(seat: Int, memoryPosition: Int) {
        Logger.d(TAG, "Recalling ${getSeatName(seat)} memory position $memoryPosition")
        
        if (isSimulated) {
            // Simulate recalling saved position
            delay(2000) // Simulate movement to saved position
            Logger.d(TAG, "Memory position $memoryPosition recalled (simulated)")
        } else {
            try {
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.SEAT_MEMORY_SELECT,
                    seat,
                    memoryPosition
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to recall seat memory", e)
            }
        }
    }
    
    private fun getSeatName(seat: Int): String {
        return when (seat) {
            DRIVER_SEAT -> "driver seat"
            PASSENGER_SEAT -> "passenger seat"
            REAR_LEFT_SEAT -> "rear left seat"
            REAR_RIGHT_SEAT -> "rear right seat"
            else -> "unknown seat"
        }
    }
    
    fun emergencyStop() {
        Logger.w(TAG, "Emergency stop - turning off all seat functions")
        
        // Turn off heating, cooling, and massage for safety
        if (isSimulated) {
            driverHeatLevel = OFF
            passengerHeatLevel = OFF
            driverCoolLevel = OFF
            passengerCoolLevel = OFF
            driverMassageIntensity = MASSAGE_OFF
            passengerMassageIntensity = MASSAGE_OFF
            driverMassageMode = MassageMode.OFF
            passengerMassageMode = MassageMode.OFF
        } else {
            try {
                val seats = listOf(DRIVER_SEAT, PASSENGER_SEAT)
                seats.forEach { seat ->
                    propertyManager?.setIntProperty(VehiclePropertyIds.HVAC_SEAT_TEMPERATURE, seat, OFF)
                    propertyManager?.setIntProperty(VehiclePropertyIds.SEAT_MASSAGE_LEVEL, seat, MASSAGE_OFF)
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error during emergency seat shutdown", e)
            }
        }
    }
    
    fun getCurrentStatus(): SeatStatus {
        return SeatStatus(
            driverHeatLevel = driverHeatLevel,
            passengerHeatLevel = passengerHeatLevel,
            driverCoolLevel = driverCoolLevel,
            passengerCoolLevel = passengerCoolLevel,
            driverMassageIntensity = driverMassageIntensity,
            passengerMassageIntensity = passengerMassageIntensity,
            driverMassageMode = driverMassageMode,
            passengerMassageMode = passengerMassageMode,
            driverPosition = driverSeatPosition,
            passengerPosition = passengerSeatPosition
        )
    }
}

data class SeatStatus(
    val driverHeatLevel: Int,
    val passengerHeatLevel: Int,
    val driverCoolLevel: Int,
    val passengerCoolLevel: Int,
    val driverMassageIntensity: Int,
    val passengerMassageIntensity: Int,
    val driverMassageMode: NavigatorSeatControl.MassageMode,
    val passengerMassageMode: NavigatorSeatControl.MassageMode,
    val driverPosition: NavigatorSeatControl.SeatPosition,
    val passengerPosition: NavigatorSeatControl.SeatPosition
)