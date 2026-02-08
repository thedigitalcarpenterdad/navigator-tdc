package com.tdc.navigator.vehicle.control

import android.car.VehicleAreaType
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyManager
import android.car.hardware.hvac.CarHvacManager
import com.tdc.navigator.util.Logger
import kotlinx.coroutines.delay
import java.util.regex.Pattern

/**
 * Navigator Climate Control
 * 
 * Handles all climate control operations for the 2025 Lincoln Navigator
 * including multi-zone temperature control, fan speed, defrost, and more.
 */
class NavigatorClimateControl(
    private val hvacManager: CarHvacManager?,
    private val propertyManager: CarPropertyManager?
) {
    
    companion object {
        private const val TAG = "NavigatorClimateControl"
        
        // Temperature limits for safety
        private const val MIN_TEMP_CELSIUS = 16.0f  // ~60°F
        private const val MAX_TEMP_CELSIUS = 32.0f  // ~90°F
        private const val MIN_TEMP_FAHRENHEIT = 60.0f
        private const val MAX_TEMP_FAHRENHEIT = 90.0f
        
        // Lincoln Navigator climate zones
        private const val DRIVER_ZONE = VehicleAreaType.VEHICLE_AREA_TYPE_SEAT_ROW_1_LEFT
        private const val PASSENGER_ZONE = VehicleAreaType.VEHICLE_AREA_TYPE_SEAT_ROW_1_RIGHT
        private const val REAR_LEFT_ZONE = VehicleAreaType.VEHICLE_AREA_TYPE_SEAT_ROW_2_LEFT
        private const val REAR_RIGHT_ZONE = VehicleAreaType.VEHICLE_AREA_TYPE_SEAT_ROW_2_RIGHT
        
        // Fan speed levels (0-7 for Navigator)
        private const val MAX_FAN_SPEED = 7
        private const val MIN_FAN_SPEED = 0
    }
    
    private val isSimulated = hvacManager == null
    
    // Current climate state (for simulation mode)
    private var driverTemp = 22.0f // 72°F
    private var passengerTemp = 22.0f
    private var rearLeftTemp = 22.0f
    private var rearRightTemp = 22.0f
    private var fanSpeed = 3
    private var isAutoMode = true
    private var isAcOn = true
    private var isDefrostOn = false
    private var isRecirculating = false
    
    suspend fun processCommand(command: String) {
        Logger.d(TAG, "Processing climate command: '$command'")
        
        try {
            when {
                // Temperature commands
                command.contains("set temperature") || command.contains("set temp") -> {
                    handleTemperatureCommand(command)
                }
                
                command.contains("increase temperature") || command.contains("warmer") -> {
                    adjustTemperature(true, getZoneFromCommand(command))
                }
                
                command.contains("decrease temperature") || command.contains("cooler") -> {
                    adjustTemperature(false, getZoneFromCommand(command))
                }
                
                // Fan speed commands
                command.contains("fan speed") -> {
                    handleFanSpeedCommand(command)
                }
                
                command.contains("turn on fan") || command.contains("start fan") -> {
                    setFanSpeed(3) // Medium speed
                }
                
                command.contains("turn off fan") || command.contains("stop fan") -> {
                    setFanSpeed(0)
                }
                
                // AC/Heat commands
                command.contains("turn on ac") || command.contains("air conditioning on") -> {
                    setAcState(true)
                }
                
                command.contains("turn off ac") || command.contains("air conditioning off") -> {
                    setAcState(false)
                }
                
                // Auto mode
                command.contains("auto") -> {
                    setAutoMode(true)
                }
                
                command.contains("manual") -> {
                    setAutoMode(false)
                }
                
                // Defrost commands
                command.contains("defrost") -> {
                    if (command.contains("on") || command.contains("start")) {
                        setDefrost(true)
                    } else if (command.contains("off") || command.contains("stop")) {
                        setDefrost(false)
                    } else {
                        toggleDefrost()
                    }
                }
                
                // Air circulation
                command.contains("recirculate") -> {
                    setRecirculation(!isRecirculating)
                }
                
                else -> {
                    Logger.d(TAG, "Unrecognized climate command: '$command'")
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error processing climate command", e)
        }
    }
    
    private suspend fun handleTemperatureCommand(command: String) {
        val tempPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(degrees?|°)?\\s*(fahrenheit|fahrenheit|celsius|celsius|f|c)?")
        val matcher = tempPattern.matcher(command.lowercase())
        
        if (matcher.find()) {
            val tempValue = matcher.group(1)?.toFloatOrNull()
            val unit = matcher.group(3)?.let { it.first().lowercase() } ?: "f" // Default to Fahrenheit
            
            if (tempValue != null) {
                val celsiusTemp = if (unit == "f") {
                    (tempValue - 32) * 5 / 9
                } else {
                    tempValue
                }
                
                val zone = getZoneFromCommand(command)
                setTemperature(celsiusTemp, zone)
            }
        }
    }
    
    private suspend fun handleFanSpeedCommand(command: String) {
        val speedPattern = Pattern.compile("(\\d+)")
        val matcher = speedPattern.matcher(command)
        
        if (matcher.find()) {
            val speed = matcher.group(1)?.toIntOrNull()
            if (speed != null) {
                setFanSpeed(speed)
            }
        } else if (command.contains("max") || command.contains("high")) {
            setFanSpeed(MAX_FAN_SPEED)
        } else if (command.contains("low") || command.contains("minimum")) {
            setFanSpeed(1)
        } else if (command.contains("medium") || command.contains("mid")) {
            setFanSpeed(MAX_FAN_SPEED / 2)
        }
    }
    
    private fun getZoneFromCommand(command: String): Int {
        return when {
            command.contains("driver") -> DRIVER_ZONE
            command.contains("passenger") -> PASSENGER_ZONE
            command.contains("rear left") -> REAR_LEFT_ZONE
            command.contains("rear right") -> REAR_RIGHT_ZONE
            command.contains("rear") -> REAR_LEFT_ZONE or REAR_RIGHT_ZONE
            command.contains("front") -> DRIVER_ZONE or PASSENGER_ZONE
            command.contains("all") -> DRIVER_ZONE or PASSENGER_ZONE or REAR_LEFT_ZONE or REAR_RIGHT_ZONE
            else -> DRIVER_ZONE // Default to driver zone
        }
    }
    
    private suspend fun setTemperature(celsius: Float, zone: Int) {
        val clampedTemp = celsius.coerceIn(MIN_TEMP_CELSIUS, MAX_TEMP_CELSIUS)
        val fahrenheit = (clampedTemp * 9 / 5) + 32
        
        Logger.d(TAG, "Setting temperature to ${fahrenheit.toInt()}°F (${clampedTemp.toInt()}°C) for zone $zone")
        
        if (isSimulated) {
            // Update simulated state
            when (zone) {
                DRIVER_ZONE -> driverTemp = clampedTemp
                PASSENGER_ZONE -> passengerTemp = clampedTemp
                REAR_LEFT_ZONE -> rearLeftTemp = clampedTemp
                REAR_RIGHT_ZONE -> rearRightTemp = clampedTemp
                else -> {
                    // Set for multiple zones
                    if (zone and DRIVER_ZONE != 0) driverTemp = clampedTemp
                    if (zone and PASSENGER_ZONE != 0) passengerTemp = clampedTemp
                    if (zone and REAR_LEFT_ZONE != 0) rearLeftTemp = clampedTemp
                    if (zone and REAR_RIGHT_ZONE != 0) rearRightTemp = clampedTemp
                }
            }
            
            // Simulate adjustment time
            delay(500)
            
        } else {
            // Real vehicle control
            try {
                hvacManager?.setFloatProperty(
                    VehiclePropertyIds.HVAC_TEMPERATURE_SET,
                    zone,
                    clampedTemp
                )
                Logger.d(TAG, "Temperature set successfully")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set temperature", e)
            }
        }
    }
    
    private suspend fun adjustTemperature(increase: Boolean, zone: Int) {
        val adjustment = if (increase) 1.0f else -1.0f
        
        if (isSimulated) {
            when (zone) {
                DRIVER_ZONE -> {
                    driverTemp = (driverTemp + adjustment).coerceIn(MIN_TEMP_CELSIUS, MAX_TEMP_CELSIUS)
                    setTemperature(driverTemp, DRIVER_ZONE)
                }
                PASSENGER_ZONE -> {
                    passengerTemp = (passengerTemp + adjustment).coerceIn(MIN_TEMP_CELSIUS, MAX_TEMP_CELSIUS)
                    setTemperature(passengerTemp, PASSENGER_ZONE)
                }
                else -> {
                    // Adjust driver zone by default
                    driverTemp = (driverTemp + adjustment).coerceIn(MIN_TEMP_CELSIUS, MAX_TEMP_CELSIUS)
                    setTemperature(driverTemp, DRIVER_ZONE)
                }
            }
        } else {
            // Get current temperature and adjust
            try {
                val currentTemp = hvacManager?.getFloatProperty(
                    VehiclePropertyIds.HVAC_TEMPERATURE_SET,
                    zone
                ) ?: 22.0f
                
                val newTemp = (currentTemp + adjustment).coerceIn(MIN_TEMP_CELSIUS, MAX_TEMP_CELSIUS)
                setTemperature(newTemp, zone)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to adjust temperature", e)
            }
        }
    }
    
    private suspend fun setFanSpeed(speed: Int) {
        val clampedSpeed = speed.coerceIn(MIN_FAN_SPEED, MAX_FAN_SPEED)
        Logger.d(TAG, "Setting fan speed to $clampedSpeed")
        
        if (isSimulated) {
            fanSpeed = clampedSpeed
            delay(300)
        } else {
            try {
                hvacManager?.setIntProperty(
                    VehiclePropertyIds.HVAC_FAN_SPEED,
                    VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL,
                    clampedSpeed
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set fan speed", e)
            }
        }
    }
    
    private suspend fun setAcState(on: Boolean) {
        Logger.d(TAG, "Setting AC state to ${if (on) "ON" else "OFF"}")
        
        if (isSimulated) {
            isAcOn = on
            delay(300)
        } else {
            try {
                hvacManager?.setBooleanProperty(
                    VehiclePropertyIds.HVAC_AC_ON,
                    VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL,
                    on
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set AC state", e)
            }
        }
    }
    
    private suspend fun setAutoMode(auto: Boolean) {
        Logger.d(TAG, "Setting auto mode to ${if (auto) "ON" else "OFF"}")
        
        if (isSimulated) {
            isAutoMode = auto
            delay(300)
        } else {
            try {
                hvacManager?.setBooleanProperty(
                    VehiclePropertyIds.HVAC_AUTO_ON,
                    VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL,
                    auto
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set auto mode", e)
            }
        }
    }
    
    private suspend fun setDefrost(on: Boolean) {
        Logger.d(TAG, "Setting defrost to ${if (on) "ON" else "OFF"}")
        
        if (isSimulated) {
            isDefrostOn = on
            delay(500)
        } else {
            try {
                // Set both windshield and rear defrost
                hvacManager?.setBooleanProperty(
                    VehiclePropertyIds.HVAC_DEFROSTER,
                    VehicleAreaType.VEHICLE_AREA_TYPE_WINDOW_FRONT_WINDSHIELD,
                    on
                )
                hvacManager?.setBooleanProperty(
                    VehiclePropertyIds.HVAC_DEFROSTER,
                    VehicleAreaType.VEHICLE_AREA_TYPE_WINDOW_REAR_WINDSHIELD,
                    on
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set defrost", e)
            }
        }
    }
    
    private suspend fun toggleDefrost() {
        setDefrost(!isDefrostOn)
    }
    
    private suspend fun setRecirculation(recirculate: Boolean) {
        Logger.d(TAG, "Setting air recirculation to ${if (recirculate) "ON" else "OFF"}")
        
        if (isSimulated) {
            isRecirculating = recirculate
            delay(300)
        } else {
            try {
                hvacManager?.setBooleanProperty(
                    VehiclePropertyIds.HVAC_RECIRC_ON,
                    VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL,
                    recirculate
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set recirculation", e)
            }
        }
    }
    
    fun getCurrentStatus(): ClimateStatus {
        return ClimateStatus(
            driverTemp = driverTemp,
            passengerTemp = passengerTemp,
            rearLeftTemp = rearLeftTemp,
            rearRightTemp = rearRightTemp,
            fanSpeed = fanSpeed,
            isAutoMode = isAutoMode,
            isAcOn = isAcOn,
            isDefrostOn = isDefrostOn,
            isRecirculating = isRecirculating
        )
    }
}

data class ClimateStatus(
    val driverTemp: Float,
    val passengerTemp: Float,
    val rearLeftTemp: Float,
    val rearRightTemp: Float,
    val fanSpeed: Int,
    val isAutoMode: Boolean,
    val isAcOn: Boolean,
    val isDefrostOn: Boolean,
    val isRecirculating: Boolean
)