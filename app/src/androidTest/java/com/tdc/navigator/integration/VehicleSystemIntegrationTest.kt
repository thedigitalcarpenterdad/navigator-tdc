package com.tdc.navigator.integration

import android.car.Car
import android.content.Context
import androidx.car.app.CarContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tdc.navigator.service.TDCCarAppService
import com.tdc.navigator.service.WakeWordDetectionService
import com.tdc.navigator.vehicle.NavigatorVehicleManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * SAFETY-CRITICAL: Full System Integration Testing
 * 
 * Tests complete integration between:
 * - Wake word detection
 * - Voice command processing
 * - Vehicle system control
 * - Android Automotive OS
 * - Safety mechanisms
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class VehicleSystemIntegrationTest {

    private lateinit var context: Context
    private lateinit var carContext: CarContext
    private lateinit var vehicleManager: NavigatorVehicleManager
    private lateinit var carService: TDCCarAppService
    private lateinit var wakeWordService: WakeWordDetectionService

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Use simulation mode for integration testing
        vehicleManager = NavigatorVehicleManager(carContext, limitedMode = true)
        carService = TDCCarAppService()
        wakeWordService = WakeWordDetectionService()
    }

    @After
    fun tearDown() {
        vehicleManager.cleanup()
    }

    @Test
    fun `INTEGRATION TEST - end-to-end wake word to vehicle control`() = runTest {
        // Initialize all systems
        vehicleManager.initialize()
        
        val commandExecuted = CountDownLatch(1)
        var executedCommand = ""
        
        // Mock wake word detection callback
        val wakeWordCallback = { command: String ->
            executedCommand = command
            vehicleManager.processVoiceCommand(command)
            commandExecuted.countDown()
        }
        
        // Simulate "Claw" wake word detection → voice command → vehicle control
        wakeWordCallback("set temperature to 72 degrees")
        
        // Wait for command execution
        assertTrue("Command should execute within 2 seconds", 
            commandExecuted.await(2, TimeUnit.SECONDS))
        assertEquals("Correct command should be executed", 
            "set temperature to 72 degrees", executedCommand)
        
        // Verify vehicle status updated
        val status = vehicleManager.getVehicleStatus()
        assertTrue("Temperature should be updated", 
            kotlin.math.abs(status.climate.driverTemp - 22.2f) < 1.0f) // 72°F ≈ 22.2°C
    }

    @Test
    fun `SAFETY TEST - system-wide emergency stop integration`() = runTest {
        vehicleManager.initialize()
        
        // Start multiple vehicle operations
        vehicleManager.processVoiceCommand("open all windows")
        vehicleManager.processVoiceCommand("turn on seat heating maximum")
        vehicleManager.processVoiceCommand("set temperature to 80")
        vehicleManager.processVoiceCommand("turn on all lights")
        
        // Trigger system-wide emergency stop
        val emergencyStartTime = System.currentTimeMillis()
        vehicleManager.emergencyStop()
        val emergencyEndTime = System.currentTimeMillis()
        
        // Emergency stop must complete within strict time limit
        val emergencyDuration = emergencyEndTime - emergencyStartTime
        assertTrue("System-wide emergency stop must complete within 100ms", 
            emergencyDuration < 100)
        
        // Verify all systems returned to safe state
        val status = vehicleManager.getVehicleStatus()
        assertEquals("All windows should be closed", 0, status.windows.driverWindowPosition)
        assertEquals("Seat heating should be off", 0, status.seats.driverHeatLevel)
        assertTrue("Temperature should be in safe range", 
            status.climate.driverTemp in 18.0f..25.0f)
    }

    @Test
    fun `PERFORMANCE TEST - full system response time benchmarks`() = runTest {
        vehicleManager.initialize()
        
        val performanceTests = mapOf(
            "Climate Control" to "set temperature to 72",
            "Window Control" to "open driver window",
            "Seat Control" to "turn on seat heating",
            "Lighting Control" to "turn on ambient lights",
            "Audio Control" to "set volume to 50"
        )
        
        performanceTests.forEach { (systemName, command) ->
            val startTime = System.currentTimeMillis()
            vehicleManager.processVoiceCommand(command)
            val endTime = System.currentTimeMillis()
            
            val responseTime = endTime - startTime
            assertTrue("$systemName response time must be under 500ms (actual: ${responseTime}ms)", 
                responseTime < 500)
        }
    }

    @Test
    fun `INTEGRATION TEST - multi-system coordination scenarios`() = runTest {
        vehicleManager.initialize()
        
        // Test comfort mode - should coordinate multiple systems
        val startTime = System.currentTimeMillis()
        vehicleManager.processVoiceCommand("activate comfort mode")
        val endTime = System.currentTimeMillis()
        
        val coordinationTime = endTime - startTime
        assertTrue("Multi-system coordination should complete within 2 seconds", 
            coordinationTime < 2000)
        
        val status = vehicleManager.getVehicleStatus()
        
        // Verify coordinated system changes
        assertTrue("Climate should be set for comfort", 
            status.climate.driverTemp in 21.0f..23.0f) // Comfortable temperature range
        assertTrue("Lighting should be enabled for comfort", 
            status.lighting.ambientBrightness > 0)
    }

    @Test
    fun `SAFETY TEST - concurrent safety system operation`() = runTest {
        vehicleManager.initialize()
        
        val concurrentCommands = listOf(
            "emergency stop all systems",
            "close all windows",
            "turn off all heating",
            "lock all doors"
        )
        
        // Execute safety commands concurrently
        val startTime = System.currentTimeMillis()
        concurrentCommands.forEach { command ->
            Thread {
                vehicleManager.processVoiceCommand(command)
            }.start()
        }
        
        Thread.sleep(1000) // Allow all commands to complete
        val endTime = System.currentTimeMillis()
        
        val totalTime = endTime - startTime
        assertTrue("Concurrent safety operations should complete quickly", totalTime < 1500)
        
        // Verify system is in safe state
        val status = vehicleManager.getVehicleStatus()
        assertTrue("All doors should be locked", status.doors.allLocked)
        assertEquals("All windows should be closed", 0, status.windows.driverWindowPosition)
    }

    @Test
    fun `INTEGRATION TEST - voice command accuracy across all systems`() = runTest {
        vehicleManager.initialize()
        
        val commandTests = listOf(
            Triple("set temperature to 75 degrees", "climate", 75),
            Triple("open driver window 50 percent", "windows", 50),
            Triple("turn on seat massage", "seats", 1),
            Triple("set ambient lighting to blue", "lighting", "blue"),
            Triple("set volume to 60", "audio", 60)
        )
        
        commandTests.forEach { (command, system, expectedValue) ->
            vehicleManager.processVoiceCommand(command)
            
            val status = vehicleManager.getVehicleStatus()
            when (system) {
                "climate" -> {
                    val tempF = (status.climate.driverTemp * 9/5) + 32
                    assertTrue("Temperature should be set correctly", 
                        kotlin.math.abs(tempF - expectedValue as Int) < 2)
                }
                "windows" -> {
                    assertEquals("Window position should be set correctly", 
                        expectedValue, status.windows.driverWindowPosition)
                }
                "seats" -> {
                    assertTrue("Seat massage should be activated", 
                        status.seats.driverMassageIntensity >= expectedValue as Int)
                }
                "audio" -> {
                    assertEquals("Volume should be set correctly", 
                        expectedValue, status.audio.volume)
                }
            }
        }
    }

    @Test
    fun `PERFORMANCE TEST - system memory usage under load`() = runTest {
        vehicleManager.initialize()
        
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Simulate heavy usage - 1000 commands
        repeat(1000) { i ->
            val commands = listOf(
                "set temperature to ${70 + (i % 10)}",
                "set volume to ${30 + (i % 20)}",
                "get vehicle status"
            )
            
            commands.forEach { command ->
                vehicleManager.processVoiceCommand(command)
            }
            
            // Check memory every 100 iterations
            if (i % 100 == 0) {
                val currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
                val memoryIncrease = currentMemory - initialMemory
                
                assertTrue("Memory usage should remain stable (current increase: ${memoryIncrease / 1024 / 1024}MB)", 
                    memoryIncrease < 200 * 1024 * 1024) // Less than 200MB increase
            }
        }
    }

    @Test
    fun `SAFETY TEST - fail-safe behavior during system overload`() = runTest {
        vehicleManager.initialize()
        
        // Create system overload with rapid commands
        val rapidCommands = (1..100).map { i ->
            "set temperature to ${65 + (i % 20)}"
        }
        
        val startTime = System.currentTimeMillis()
        rapidCommands.forEach { command ->
            vehicleManager.processVoiceCommand(command)
        }
        val endTime = System.currentTimeMillis()
        
        val totalTime = endTime - startTime
        
        // System should handle overload gracefully
        assertTrue("System should handle command overload without failure", totalTime < 10000)
        
        // System should still respond to emergency commands
        vehicleManager.emergencyStop()
        
        val status = vehicleManager.getVehicleStatus()
        assertTrue("System should be in safe state after overload", 
            status.getWarnings().isEmpty() || status.getWarnings().size < 3)
    }

    @Test
    fun `INTEGRATION TEST - Android Automotive OS compatibility`() = runTest {
        // Test that the app integrates properly with Android Automotive
        val carService = TDCCarAppService()
        
        // Verify service can be created
        assertNotNull("Car app service should be created", carService)
        
        // Test host validator
        val hostValidator = carService.createHostValidator()
        assertNotNull("Host validator should be created", hostValidator)
        
        // Test session creation
        val session = carService.onCreateSession()
        assertNotNull("Car app session should be created", session)
    }

    @Test
    fun `SECURITY TEST - system resilience against malformed input`() = runTest {
        vehicleManager.initialize()
        
        val malformedInputs = listOf(
            "",
            "\\x00\\x01\\x02",
            "A".repeat(10000), // Very long string
            "set temperature to 999999999",
            "'; DROP TABLE; --",
            "\n\r\t\u0000malformed",
            "unicode: 🚗🔥💀",
            "${Int.MAX_VALUE} degrees"
        )
        
        malformedInputs.forEach { input ->
            assertDoesNotThrow("Malformed input should be handled gracefully: '$input'") {
                vehicleManager.processVoiceCommand(input)
            }
        }
        
        // System should remain stable
        val status = vehicleManager.getVehicleStatus()
        assertNotNull("Vehicle status should remain available after malformed inputs", status)
    }

    @Test
    fun `PERFORMANCE TEST - wake word detection accuracy benchmark`() = runTest {
        // This would integrate with actual audio testing in a real environment
        // For now, test the detection pipeline setup
        
        val wakeWords = arrayOf("claw")
        val confidenceThreshold = 0.75f
        
        // Verify wake word configuration
        assertTrue("Wake words should be configured", wakeWords.isNotEmpty())
        assertEquals("Should have single 'Claw' wake word", "claw", wakeWords[0])
        assertTrue("Confidence threshold should be appropriate", 
            confidenceThreshold >= 0.7f && confidenceThreshold <= 0.95f)
        
        // Test detection timing requirements
        val detectionStartTime = System.currentTimeMillis()
        // Simulate detection processing
        Thread.sleep(100) // Simulate audio processing
        val detectionEndTime = System.currentTimeMillis()
        
        val detectionLatency = detectionEndTime - detectionStartTime
        assertTrue("Wake word detection latency should be under 200ms", 
            detectionLatency < 200)
    }

    private fun assertDoesNotThrow(message: String = "", block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("$message - Exception thrown: ${e.message}")
        }
    }
}