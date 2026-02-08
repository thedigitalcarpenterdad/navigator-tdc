package com.tdc.navigator.vehicle.control

import android.car.VehicleAreaType
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyManager
import android.car.hardware.cabin.CarCabinManager
import com.tdc.navigator.util.Logger
import kotlinx.coroutines.delay

/**
 * Navigator Lighting Control
 * 
 * Handles all lighting operations for the 2025 Lincoln Navigator including:
 * - Ambient lighting
 * - Dome lights
 * - Exterior lights
 * - Puddle lights
 * - Welcome lighting
 */
class NavigatorLightingControl(
    private val cabinManager: CarCabinManager?,
    private val propertyManager: CarPropertyManager?
) {
    
    companion object {
        private const val TAG = "NavigatorLightingControl"
        
        // Brightness levels (0-100%)
        private const val OFF = 0
        private const val LOW = 25
        private const val MEDIUM = 50
        private const val HIGH = 75
        private const val MAX = 100
        
        // Light zones
        private const val FRONT_LEFT = VehicleAreaType.VEHICLE_AREA_TYPE_SEAT_ROW_1_LEFT
        private const val FRONT_RIGHT = VehicleAreaType.VEHICLE_AREA_TYPE_SEAT_ROW_1_RIGHT
        private const val REAR_LEFT = VehicleAreaType.VEHICLE_AREA_TYPE_SEAT_ROW_2_LEFT
        private const val REAR_RIGHT = VehicleAreaType.VEHICLE_AREA_TYPE_SEAT_ROW_2_RIGHT
        private const val GLOBAL = VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL
        
        // Lincoln Navigator ambient lighting themes
        private val AMBIENT_THEMES = mapOf(
            "warm" to AmbientTheme(255, 180, 120, "Warm White"),
            "cool" to AmbientTheme(120, 180, 255, "Cool Blue"),
            "purple" to AmbientTheme(200, 120, 255, "Purple"),
            "red" to AmbientTheme(255, 80, 80, "Red"),
            "green" to AmbientTheme(80, 255, 120, "Green"),
            "blue" to AmbientTheme(80, 120, 255, "Blue"),
            "orange" to AmbientTheme(255, 140, 80, "Orange"),
            "lincoln" to AmbientTheme(255, 215, 0, "Lincoln Gold"),
            "off" to AmbientTheme(0, 0, 0, "Off")
        )
    }
    
    data class AmbientTheme(val red: Int, val green: Int, val blue: Int, val name: String)
    
    private val isSimulated = cabinManager == null
    
    // Current lighting state (for simulation)
    private var ambientBrightness = MEDIUM
    private var currentAmbientTheme = AMBIENT_THEMES["lincoln"]!!
    private var domeLightsOn = false
    private var readingLightsOn = false
    private var puddleLightsEnabled = true
    private var welcomeLightingEnabled = true
    private var autoHeadlightsEnabled = true
    
    suspend fun processCommand(command: String) {
        Logger.d(TAG, "Processing lighting command: '$command'")
        
        try {
            when {
                // Ambient lighting commands
                command.contains("ambient") -> {
                    handleAmbientLightCommand(command)
                }
                
                // Dome light commands
                command.contains("dome") || command.contains("interior") -> {
                    handleDomeLightCommand(command)
                }
                
                // Reading light commands
                command.contains("reading") -> {
                    handleReadingLightCommand(command)
                }
                
                // Puddle light commands
                command.contains("puddle") -> {
                    handlePuddleLightCommand(command)
                }
                
                // Welcome lighting commands
                command.contains("welcome") -> {
                    handleWelcomeLightCommand(command)
                }
                
                // Headlight commands
                command.contains("headlight") -> {
                    handleHeadlightCommand(command)
                }
                
                // Generic brightness commands
                command.contains("brightness") -> {
                    handleBrightnessCommand(command)
                }
                
                // Color/theme commands
                isColorCommand(command) -> {
                    handleColorCommand(command)
                }
                
                else -> {
                    Logger.d(TAG, "Unrecognized lighting command: '$command'")
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error processing lighting command", e)
        }
    }
    
    private suspend fun handleAmbientLightCommand(command: String) {
        when {
            command.contains("off") || command.contains("disable") -> {
                setAmbientLighting(false)
            }
            
            command.contains("on") || command.contains("enable") -> {
                setAmbientLighting(true)
            }
            
            command.contains("brighter") || command.contains("increase") -> {
                adjustAmbientBrightness(20)
            }
            
            command.contains("dimmer") || command.contains("decrease") -> {
                adjustAmbientBrightness(-20)
            }
            
            command.contains("max") || command.contains("brightest") -> {
                setAmbientBrightness(MAX)
            }
            
            command.contains("dim") || command.contains("low") -> {
                setAmbientBrightness(LOW)
            }
            
            else -> {
                // Check for theme/color in command
                handleColorCommand(command)
            }
        }
    }
    
    private suspend fun handleDomeLightCommand(command: String) {
        when {
            command.contains("on") || command.contains("turn on") -> {
                setDomeLights(true)
            }
            
            command.contains("off") || command.contains("turn off") -> {
                setDomeLights(false)
            }
            
            else -> {
                // Toggle if no specific action
                setDomeLights(!domeLightsOn)
            }
        }
    }
    
    private suspend fun handleReadingLightCommand(command: String) {
        val zone = when {
            command.contains("driver") -> FRONT_LEFT
            command.contains("passenger") -> FRONT_RIGHT
            command.contains("rear left") -> REAR_LEFT
            command.contains("rear right") -> REAR_RIGHT
            command.contains("all") -> GLOBAL
            else -> FRONT_LEFT // Default to driver
        }
        
        when {
            command.contains("on") -> {
                setReadingLight(zone, true)
            }
            
            command.contains("off") -> {
                setReadingLight(zone, false)
            }
            
            else -> {
                toggleReadingLight(zone)
            }
        }
    }
    
    private suspend fun handlePuddleLightCommand(command: String) {
        when {
            command.contains("on") || command.contains("enable") -> {
                setPuddleLights(true)
            }
            
            command.contains("off") || command.contains("disable") -> {
                setPuddleLights(false)
            }
            
            else -> {
                setPuddleLights(!puddleLightsEnabled)
            }
        }
    }
    
    private suspend fun handleWelcomeLightCommand(command: String) {
        when {
            command.contains("on") || command.contains("enable") -> {
                setWelcomeLighting(true)
            }
            
            command.contains("off") || command.contains("disable") -> {
                setWelcomeLighting(false)
            }
            
            else -> {
                setWelcomeLighting(!welcomeLightingEnabled)
            }
        }
    }
    
    private suspend fun handleHeadlightCommand(command: String) {
        when {
            command.contains("auto") -> {
                setAutoHeadlights(true)
            }
            
            command.contains("manual") -> {
                setAutoHeadlights(false)
            }
            
            command.contains("on") -> {
                setHeadlights(true)
            }
            
            command.contains("off") -> {
                setHeadlights(false)
            }
        }
    }
    
    private suspend fun handleBrightnessCommand(command: String) {
        val brightnessLevel = when {
            command.contains("max") || command.contains("100") -> MAX
            command.contains("high") || command.contains("bright") -> HIGH
            command.contains("medium") || command.contains("50") -> MEDIUM
            command.contains("low") || command.contains("dim") -> LOW
            command.contains("off") || command.contains("0") -> OFF
            else -> {
                // Try to extract percentage
                val regex = Regex("(\\d+)%?")
                val match = regex.find(command)
                match?.groups?.get(1)?.value?.toIntOrNull()?.coerceIn(0, 100) ?: MEDIUM
            }
        }
        
        setAmbientBrightness(brightnessLevel)
    }
    
    private fun isColorCommand(command: String): Boolean {
        return AMBIENT_THEMES.keys.any { command.contains(it) }
    }
    
    private suspend fun handleColorCommand(command: String) {
        val theme = AMBIENT_THEMES.entries.find { (key, _) -> 
            command.contains(key) 
        }?.value
        
        if (theme != null) {
            setAmbientTheme(theme)
        }
    }
    
    private suspend fun setAmbientLighting(enabled: Boolean) {
        Logger.d(TAG, "Setting ambient lighting ${if (enabled) "ON" else "OFF"}")
        
        if (isSimulated) {
            ambientBrightness = if (enabled) MEDIUM else OFF
            delay(500)
        } else {
            try {
                propertyManager?.setBooleanProperty(
                    VehiclePropertyIds.CABIN_LIGHTS_STATE,
                    GLOBAL,
                    enabled
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set ambient lighting", e)
            }
        }
    }
    
    private suspend fun setAmbientBrightness(brightness: Int) {
        val clampedBrightness = brightness.coerceIn(OFF, MAX)
        Logger.d(TAG, "Setting ambient brightness to $clampedBrightness%")
        
        if (isSimulated) {
            ambientBrightness = clampedBrightness
            delay(300)
        } else {
            try {
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.CABIN_LIGHTS_SWITCH,
                    GLOBAL,
                    clampedBrightness
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set ambient brightness", e)
            }
        }
    }
    
    private suspend fun adjustAmbientBrightness(adjustment: Int) {
        val newBrightness = (ambientBrightness + adjustment).coerceIn(OFF, MAX)
        setAmbientBrightness(newBrightness)
    }
    
    private suspend fun setAmbientTheme(theme: AmbientTheme) {
        Logger.d(TAG, "Setting ambient theme to ${theme.name}")
        
        if (isSimulated) {
            currentAmbientTheme = theme
            if (theme.red == 0 && theme.green == 0 && theme.blue == 0) {
                ambientBrightness = OFF
            } else if (ambientBrightness == OFF) {
                ambientBrightness = MEDIUM
            }
            delay(800)
        } else {
            try {
                // Set RGB values for ambient lighting
                // These properties may be custom Lincoln/Ford specific
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.CABIN_LIGHTS_SWITCH, // May need custom RGB properties
                    GLOBAL,
                    if (theme.red == 0 && theme.green == 0 && theme.blue == 0) OFF else ambientBrightness
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set ambient theme", e)
            }
        }
    }
    
    private suspend fun setDomeLights(on: Boolean) {
        Logger.d(TAG, "Setting dome lights ${if (on) "ON" else "OFF"}")
        
        if (isSimulated) {
            domeLightsOn = on
            delay(200)
        } else {
            try {
                propertyManager?.setBooleanProperty(
                    VehiclePropertyIds.CABIN_LIGHTS_STATE,
                    VehicleAreaType.VEHICLE_AREA_TYPE_WINDOW_ROOF_TOP_1,
                    on
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set dome lights", e)
            }
        }
    }
    
    private suspend fun setReadingLight(zone: Int, on: Boolean) {
        val zoneName = when (zone) {
            FRONT_LEFT -> "driver"
            FRONT_RIGHT -> "passenger"
            REAR_LEFT -> "rear left"
            REAR_RIGHT -> "rear right"
            else -> "all"
        }
        
        Logger.d(TAG, "Setting $zoneName reading light ${if (on) "ON" else "OFF"}")
        
        if (isSimulated) {
            readingLightsOn = on
            delay(200)
        } else {
            try {
                if (zone == GLOBAL) {
                    // Turn on/off all reading lights
                    val zones = listOf(FRONT_LEFT, FRONT_RIGHT, REAR_LEFT, REAR_RIGHT)
                    zones.forEach { z ->
                        propertyManager?.setBooleanProperty(
                            VehiclePropertyIds.READING_LIGHTS_STATE,
                            z,
                            on
                        )
                    }
                } else {
                    propertyManager?.setBooleanProperty(
                        VehiclePropertyIds.READING_LIGHTS_STATE,
                        zone,
                        on
                    )
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set reading light", e)
            }
        }
    }
    
    private suspend fun toggleReadingLight(zone: Int) {
        setReadingLight(zone, !readingLightsOn)
    }
    
    private suspend fun setPuddleLights(enabled: Boolean) {
        Logger.d(TAG, "Setting puddle lights ${if (enabled) "ENABLED" else "DISABLED"}")
        
        if (isSimulated) {
            puddleLightsEnabled = enabled
            delay(300)
        } else {
            try {
                // Puddle lights may use custom property for Lincoln
                propertyManager?.setBooleanProperty(
                    VehiclePropertyIds.EXTERIOR_LIGHTS,
                    VehicleAreaType.VEHICLE_AREA_TYPE_DOOR_HOOD,
                    enabled
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set puddle lights", e)
            }
        }
    }
    
    private suspend fun setWelcomeLighting(enabled: Boolean) {
        Logger.d(TAG, "Setting welcome lighting ${if (enabled) "ENABLED" else "DISABLED"}")
        
        if (isSimulated) {
            welcomeLightingEnabled = enabled
            delay(300)
        } else {
            try {
                // Welcome lighting sequence - may need custom implementation
                propertyManager?.setBooleanProperty(
                    VehiclePropertyIds.CABIN_LIGHTS_STATE,
                    GLOBAL,
                    enabled
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set welcome lighting", e)
            }
        }
    }
    
    private suspend fun setAutoHeadlights(auto: Boolean) {
        Logger.d(TAG, "Setting auto headlights ${if (auto) "ON" else "OFF"}")
        
        if (isSimulated) {
            autoHeadlightsEnabled = auto
            delay(200)
        } else {
            try {
                propertyManager?.setBooleanProperty(
                    VehiclePropertyIds.HEADLIGHTS_SWITCH,
                    GLOBAL,
                    auto
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set auto headlights", e)
            }
        }
    }
    
    private suspend fun setHeadlights(on: Boolean) {
        Logger.d(TAG, "Setting headlights ${if (on) "ON" else "OFF"}")
        
        if (isSimulated) {
            // In simulation, just log
            delay(200)
        } else {
            try {
                propertyManager?.setIntProperty(
                    VehiclePropertyIds.HEADLIGHTS_STATE,
                    GLOBAL,
                    if (on) 1 else 0
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set headlights", e)
            }
        }
    }
    
    fun getCurrentStatus(): LightingStatus {
        return LightingStatus(
            ambientBrightness = ambientBrightness,
            ambientTheme = currentAmbientTheme,
            domeLightsOn = domeLightsOn,
            readingLightsOn = readingLightsOn,
            puddleLightsEnabled = puddleLightsEnabled,
            welcomeLightingEnabled = welcomeLightingEnabled,
            autoHeadlightsEnabled = autoHeadlightsEnabled
        )
    }
}

data class LightingStatus(
    val ambientBrightness: Int,
    val ambientTheme: NavigatorLightingControl.AmbientTheme,
    val domeLightsOn: Boolean,
    val readingLightsOn: Boolean,
    val puddleLightsEnabled: Boolean,
    val welcomeLightingEnabled: Boolean,
    val autoHeadlightsEnabled: Boolean
)