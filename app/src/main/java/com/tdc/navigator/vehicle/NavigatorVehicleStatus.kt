package com.tdc.navigator.vehicle

import com.tdc.navigator.vehicle.control.*

/**
 * Navigator Vehicle Status
 * 
 * Comprehensive status data class containing the current state
 * of all vehicle systems in the 2025 Lincoln Navigator.
 */
data class NavigatorVehicleStatus(
    val climate: ClimateStatus,
    val windows: WindowStatus,
    val seats: SeatStatus,
    val lighting: LightingStatus,
    val doors: DoorStatus,
    val mirrors: MirrorStatus,
    val audio: AudioStatus,
    val driveMode: DriveModeStatus,
    val timestamp: Long
) {
    /**
     * Generate a human-readable status summary for voice feedback
     */
    fun getStatusSummary(): String {
        val summaryParts = mutableListOf<String>()
        
        // Climate summary
        if (climate.isAcOn || climate.fanSpeed > 0) {
            summaryParts.add("Climate: ${climate.driverTemp.toInt()}°F, fan ${climate.fanSpeed}")
        }
        
        // Windows summary
        val openWindows = mutableListOf<String>()
        if (windows.driverWindowPosition > 0) openWindows.add("driver")
        if (windows.passengerWindowPosition > 0) openWindows.add("passenger")
        if (windows.rearLeftWindowPosition > 0) openWindows.add("rear left")
        if (windows.rearRightWindowPosition > 0) openWindows.add("rear right")
        if (windows.sunroofPosition > 0) openWindows.add("sunroof")
        
        if (openWindows.isNotEmpty()) {
            summaryParts.add("Open: ${openWindows.joinToString(", ")}")
        }
        
        // Seats summary
        val seatFeatures = mutableListOf<String>()
        if (seats.driverHeatLevel > 0) seatFeatures.add("driver heat ${seats.driverHeatLevel}")
        if (seats.passengerHeatLevel > 0) seatFeatures.add("passenger heat ${seats.passengerHeatLevel}")
        if (seats.driverCoolLevel > 0) seatFeatures.add("driver cool ${seats.driverCoolLevel}")
        if (seats.passengerCoolLevel > 0) seatFeatures.add("passenger cool ${seats.passengerCoolLevel}")
        if (seats.driverMassageIntensity > 0) {
            seatFeatures.add("driver massage ${seats.driverMassageMode.name.lowercase()}")
        }
        if (seats.passengerMassageIntensity > 0) {
            seatFeatures.add("passenger massage ${seats.passengerMassageMode.name.lowercase()}")
        }
        
        if (seatFeatures.isNotEmpty()) {
            summaryParts.add("Seats: ${seatFeatures.joinToString(", ")}")
        }
        
        // Lighting summary
        if (lighting.ambientBrightness > 0) {
            summaryParts.add("Ambient: ${lighting.ambientTheme.name} at ${lighting.ambientBrightness}%")
        }
        if (lighting.domeLightsOn) {
            summaryParts.add("Dome lights on")
        }
        
        // Doors summary
        val unlockedDoors = mutableListOf<String>()
        if (!doors.driverLocked) unlockedDoors.add("driver")
        if (!doors.passengerLocked) unlockedDoors.add("passenger")
        if (!doors.rearLeftLocked) unlockedDoors.add("rear left")
        if (!doors.rearRightLocked) unlockedDoors.add("rear right")
        
        if (unlockedDoors.isNotEmpty()) {
            summaryParts.add("Unlocked: ${unlockedDoors.joinToString(", ")}")
        }
        
        // Drive mode
        if (driveMode.currentMode != "normal") {
            summaryParts.add("Drive mode: ${driveMode.currentMode}")
        }
        
        return if (summaryParts.isNotEmpty()) {
            summaryParts.joinToString(". ")
        } else {
            "All systems normal"
        }
    }
    
    /**
     * Check for any active warnings or issues
     */
    fun getWarnings(): List<String> {
        val warnings = mutableListOf<String>()
        
        // Check for open windows while vehicle may be moving
        if (windows.driverWindowPosition > 50 || windows.passengerWindowPosition > 50) {
            warnings.add("Front windows are more than halfway open")
        }
        
        if (windows.sunroofPosition > 80) {
            warnings.add("Sunroof is almost fully open")
        }
        
        // Check for doors unlocked
        if (!doors.allLocked) {
            warnings.add("Some doors are unlocked")
        }
        
        // Check for high seat heating/cooling
        if (seats.driverHeatLevel >= 3 || seats.passengerHeatLevel >= 3) {
            warnings.add("High seat heating level active")
        }
        
        if (seats.driverCoolLevel >= 3 || seats.passengerCoolLevel >= 3) {
            warnings.add("High seat cooling level active")
        }
        
        // Check climate extreme settings
        val driverTempF = (climate.driverTemp * 9/5) + 32
        if (driverTempF < 65 || driverTempF > 85) {
            warnings.add("Temperature set to extreme level")
        }
        
        return warnings
    }
    
    /**
     * Get energy consumption estimate based on current settings
     */
    fun getEnergyImpact(): String {
        var impact = 0
        
        // Climate impact
        if (climate.isAcOn) impact += 3
        if (climate.fanSpeed > 5) impact += 2
        if (climate.isDefrostOn) impact += 2
        
        // Seat heating/cooling impact
        impact += seats.driverHeatLevel + seats.passengerHeatLevel
        impact += seats.driverCoolLevel + seats.passengerCoolLevel
        
        // Massage impact
        if (seats.driverMassageIntensity > 0) impact += 1
        if (seats.passengerMassageIntensity > 0) impact += 1
        
        // Lighting impact
        if (lighting.ambientBrightness > 50) impact += 1
        if (lighting.domeLightsOn) impact += 1
        
        return when {
            impact <= 3 -> "Low energy usage"
            impact <= 8 -> "Medium energy usage"
            else -> "High energy usage"
        }
    }
}