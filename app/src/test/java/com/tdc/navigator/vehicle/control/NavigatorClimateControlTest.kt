package com.tdc.navigator.vehicle.control

import android.car.VehicleAreaType
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyManager
import android.car.hardware.hvac.CarHvacManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * SAFETY-CRITICAL: Climate Control Testing
 * 
 * Tests all climate control safety mechanisms, temperature limits,
 * and emergency stop functionality.
 */
@ExperimentalCoroutinesApi
class NavigatorClimateControlTest {

    private lateinit var hvacManager: CarHvacManager
    private lateinit var propertyManager: CarPropertyManager
    private lateinit var climateControl: NavigatorClimateControl

    @Before
    fun setUp() {
        hvacManager = mockk(relaxed = true)
        propertyManager = mockk(relaxed = true)
        climateControl = NavigatorClimateControl(hvacManager, propertyManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `SAFETY TEST - temperature limits enforced`() = runTest {
        // Test extreme temperature rejection
        climateControl.processCommand("set temperature to 120 degrees fahrenheit")
        
        // Should clamp to safe maximum (90°F = 32.2°C)
        verify {
            hvacManager.setFloatProperty(
                VehiclePropertyIds.HVAC_TEMPERATURE_SET,
                any(),
                match { it <= 32.2f }
            )
        }
    }

    @Test
    fun `SAFETY TEST - minimum temperature limits enforced`() = runTest {
        climateControl.processCommand("set temperature to 40 degrees fahrenheit")
        
        // Should clamp to safe minimum (60°F = 15.6°C)
        verify {
            hvacManager.setFloatProperty(
                VehiclePropertyIds.HVAC_TEMPERATURE_SET,
                any(),
                match { it >= 15.6f }
            )
        }
    }

    @Test
    fun `SAFETY TEST - emergency stop disables all climate functions`() = runTest {
        // Start with active climate functions
        climateControl.processCommand("set temperature to 72")
        climateControl.processCommand("turn on fan")
        
        // Trigger emergency stop
        climateControl.emergencyStop()
        
        // All systems should be safely disabled
        verify {
            hvacManager.setBooleanProperty(VehiclePropertyIds.HVAC_AC_ON, any(), false)
            hvacManager.setIntProperty(VehiclePropertyIds.HVAC_FAN_SPEED, any(), 0)
        }
    }

    @Test
    fun `UNIT TEST - zone-specific temperature control`() = runTest {
        climateControl.processCommand("set driver temperature to 70 degrees")
        
        verify {
            hvacManager.setFloatProperty(
                VehiclePropertyIds.HVAC_TEMPERATURE_SET,
                VehicleAreaType.VEHICLE_AREA_TYPE_SEAT_ROW_1_LEFT,
                match { kotlin.math.abs(it - 21.1f) < 0.5f } // 70°F = ~21.1°C
            )
        }
    }

    @Test
    fun `UNIT TEST - fan speed control with safety limits`() = runTest {
        // Test maximum fan speed enforcement
        climateControl.processCommand("set fan speed to 15")
        
        verify {
            hvacManager.setIntProperty(
                VehiclePropertyIds.HVAC_FAN_SPEED,
                any(),
                7 // Should clamp to maximum safe speed
            )
        }
    }

    @Test
    fun `UNIT TEST - defrost safety priority`() = runTest {
        climateControl.processCommand("turn on defrost")
        
        verify {
            // Front windshield defrost
            hvacManager.setBooleanProperty(
                VehiclePropertyIds.HVAC_DEFROSTER,
                VehicleAreaType.VEHICLE_AREA_TYPE_WINDOW_FRONT_WINDSHIELD,
                true
            )
            // Rear windshield defrost
            hvacManager.setBooleanProperty(
                VehiclePropertyIds.HVAC_DEFROSTER,
                VehicleAreaType.VEHICLE_AREA_TYPE_WINDOW_REAR_WINDSHIELD,
                true
            )
        }
    }

    @Test
    fun `PERFORMANCE TEST - response time under 500ms`() = runTest {
        val startTime = System.currentTimeMillis()
        
        climateControl.processCommand("set temperature to 72")
        
        val endTime = System.currentTimeMillis()
        val responseTime = endTime - startTime
        
        assertTrue("Climate control response time must be under 500ms", responseTime < 500)
    }

    @Test
    fun `SAFETY TEST - invalid commands rejected safely`() = runTest {
        // Test malformed commands don't cause unsafe behavior
        climateControl.processCommand("set temperature to invalid")
        climateControl.processCommand("turn on everything maximum")
        
        // No unsafe operations should be attempted
        verify(exactly = 0) {
            hvacManager.setFloatProperty(any(), any(), match { it < 15.6f || it > 32.2f })
        }
    }

    @Test
    fun `INTEGRATION TEST - multi-zone coordination`() = runTest {
        climateControl.processCommand("set all zones to 72 degrees")
        
        // All zones should be set safely
        verify {
            hvacManager.setFloatProperty(
                VehiclePropertyIds.HVAC_TEMPERATURE_SET,
                VehicleAreaType.VEHICLE_AREA_TYPE_SEAT_ROW_1_LEFT,
                any()
            )
            hvacManager.setFloatProperty(
                VehiclePropertyIds.HVAC_TEMPERATURE_SET,
                VehicleAreaType.VEHICLE_AREA_TYPE_SEAT_ROW_1_RIGHT,
                any()
            )
        }
    }

    @Test
    fun `SAFETY TEST - AC protection during extreme conditions`() = runTest {
        // Simulate extreme external temperature
        every { 
            propertyManager.getFloatProperty(any(), any()) 
        } returns 45.0f // 45°C external temperature
        
        climateControl.processCommand("turn on AC maximum")
        
        // System should still operate within safe parameters
        verify {
            hvacManager.setBooleanProperty(VehiclePropertyIds.HVAC_AC_ON, any(), true)
            hvacManager.setIntProperty(
                VehiclePropertyIds.HVAC_FAN_SPEED,
                any(),
                match { it <= 7 } // Max safe fan speed
            )
        }
    }

    @Test
    fun `UNIT TEST - status reporting accuracy`() = runTest {
        // Set known state
        climateControl.processCommand("set temperature to 72")
        climateControl.processCommand("set fan speed to 3")
        
        val status = climateControl.getCurrentStatus()
        
        // Verify accurate status reporting
        assertTrue("Driver temperature should be reported accurately", 
            kotlin.math.abs(status.driverTemp - 22.2f) < 0.5f)
        assertEquals("Fan speed should be reported accurately", 3, status.fanSpeed)
    }
}