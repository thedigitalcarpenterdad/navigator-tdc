package com.tdc.navigator.wakework

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/**
 * SAFETY-CRITICAL: Wake Word Detection Testing
 * 
 * Tests wake word detection accuracy, performance, and safety.
 * False positives could cause unintended vehicle control.
 * False negatives could prevent emergency voice commands.
 */
@ExperimentalCoroutinesApi
class TDCWakeWordDetectorTest {

    private lateinit var context: Context
    private lateinit var interpreter: Interpreter
    private lateinit var wakeWordDetector: TDCWakeWordDetector
    private val detectedWakeWords = mutableListOf<Pair<String, Float>>()

    private val mockOnWakeWordDetected = { wakeWord: String, confidence: Float ->
        detectedWakeWords.add(Pair(wakeWord, confidence))
    }

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        interpreter = mockk(relaxed = true)
        
        // Mock assets for model loading
        val assetManager = mockk<android.content.res.AssetManager>(relaxed = true)
        val assetFileDescriptor = mockk<android.content.res.AssetFileDescriptor>(relaxed = true)
        
        every { context.assets } returns assetManager
        every { assetManager.openFd(any()) } returns assetFileDescriptor
        every { assetFileDescriptor.fileDescriptor } returns mockk(relaxed = true)
        every { assetFileDescriptor.startOffset } returns 0L
        every { assetFileDescriptor.declaredLength } returns 1024L
        
        wakeWordDetector = TDCWakeWordDetector(
            context = context,
            wakeWords = arrayOf("claw"),
            confidenceThreshold = 0.75f,
            onWakeWordDetected = mockOnWakeWordDetected
        )
        
        detectedWakeWords.clear()
    }

    @After
    fun tearDown() {
        unmockkAll()
        detectedWakeWords.clear()
    }

    @Test
    fun `SAFETY TEST - high confidence threshold prevents false positives`() = runTest {
        // Simulate low confidence detection
        val lowConfidenceAudio = generateTestAudioBuffer("background noise", 1024)
        
        // Mock TensorFlow Lite to return low confidence
        mockInferenceResult(floatArrayOf(0.3f, 0.7f)) // Below threshold (Claw: 0.3, Background: 0.7)
        
        wakeWordDetector.processAudioBuffer(lowConfidenceAudio, lowConfidenceAudio.size)
        
        // Should NOT trigger wake word
        assertTrue("Low confidence should not trigger wake word", detectedWakeWords.isEmpty())
    }

    @Test
    fun `SAFETY TEST - genuine wake words detected with high confidence`() = runTest {
        val clawAudio = generateTestAudioBuffer("claw", 1024)
        
        // Mock high confidence for "claw"
        mockInferenceResult(floatArrayOf(0.9f, 0.1f)) // Claw: 0.9, Background: 0.1
        
        wakeWordDetector.processAudioBuffer(clawAudio, clawAudio.size)
        
        // Should detect "claw" with high confidence
        assertEquals("Should detect one wake word", 1, detectedWakeWords.size)
        assertEquals("Should detect 'claw'", "claw", detectedWakeWords[0].first)
        assertTrue("Confidence should be high", detectedWakeWords[0].second > 0.75f)
    }

    @Test
    fun `PERFORMANCE TEST - detection latency under 200ms`() = runTest {
        val testAudio = generateTestAudioBuffer("claw", 1024)
        mockInferenceResult(floatArrayOf(0.9f, 0.1f)) // Claw: 0.9, Background: 0.1
        
        val startTime = System.currentTimeMillis()
        wakeWordDetector.processAudioBuffer(testAudio, testAudio.size)
        val endTime = System.currentTimeMillis()
        
        val latency = endTime - startTime
        assertTrue("Wake word detection must be under 200ms", latency < 200)
    }

    @Test
    fun `SAFETY TEST - claw wake word consistency`() = runTest {
        // Test multiple "claw" detections for consistency
        repeat(5) { i ->
            detectedWakeWords.clear()
            mockInferenceResult(floatArrayOf(0.85f + i * 0.01f, 0.15f - i * 0.01f)) // Varying confidence
            wakeWordDetector.processAudioBuffer(generateTestAudioBuffer("claw", 1024), 1024)
            
            assertEquals("Should consistently detect 'claw'", "claw", detectedWakeWords[0].first)
            assertTrue("Confidence should remain high", detectedWakeWords[0].second > 0.75f)
        }
    }

    @Test
    fun `SAFETY TEST - noise rejection capabilities`() = runTest {
        val noiseScenarios = listOf(
            "engine noise",
            "road noise", 
            "wind noise",
            "music playing",
            "conversation"
        )
        
        noiseScenarios.forEach { noise ->
            detectedWakeWords.clear()
            // Mock background/noise classification
            mockInferenceResult(floatArrayOf(0.2f, 0.8f)) // Claw: 0.2, Background: 0.8
            
            wakeWordDetector.processAudioBuffer(generateTestAudioBuffer(noise, 1024), 1024)
            
            assertTrue("Noise '$noise' should not trigger wake word", detectedWakeWords.isEmpty())
        }
    }

    @Test
    fun `PERFORMANCE TEST - continuous operation memory stability`() = runTest {
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Simulate 10 minutes of continuous operation (600 audio buffers)
        repeat(600) { i ->
            val testAudio = generateTestAudioBuffer("background", 1024)
            mockInferenceResult(floatArrayOf(0.1f, 0.1f, 0.8f)) // Background noise
            wakeWordDetector.processAudioBuffer(testAudio, testAudio.size)
            
            // Check memory every 100 iterations
            if (i % 100 == 0) {
                val currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
                val memoryIncrease = currentMemory - initialMemory
                
                assertTrue("Memory usage should remain stable", memoryIncrease < 50 * 1024 * 1024) // <50MB increase
            }
        }
    }

    @Test
    fun `SECURITY TEST - buffer overflow protection`() = runTest {
        // Test with oversized audio buffer
        val oversizedBuffer = ShortArray(100000) // Much larger than expected
        mockInferenceResult(floatArrayOf(0.1f, 0.1f, 0.8f))
        
        // Should handle gracefully without crashing
        assertDoesNotThrow {
            wakeWordDetector.processAudioBuffer(oversizedBuffer, oversizedBuffer.size)
        }
    }

    @Test
    fun `UNIT TEST - confidence scoring accuracy`() = runTest {
        val confidenceValues = listOf(0.95f, 0.85f, 0.75f, 0.65f)
        
        confidenceValues.forEach { confidence ->
            detectedWakeWords.clear()
            mockInferenceResult(floatArrayOf(confidence, 1.0f - confidence)) // Claw vs Background
            
            wakeWordDetector.processAudioBuffer(generateTestAudioBuffer("claw", 1024), 1024)
            
            if (confidence >= 0.75f) {
                assertEquals("High confidence should trigger detection", 1, detectedWakeWords.size)
                assertTrue("Reported confidence should match", 
                    kotlin.math.abs(detectedWakeWords[0].second - confidence) < 0.01f)
            } else {
                assertTrue("Low confidence should not trigger", detectedWakeWords.isEmpty())
            }
        }
    }

    @Test
    fun `SAFETY TEST - rapid successive detections handled properly`() = runTest {
        // Simulate rapid wake word repetition
        repeat(5) {
            mockInferenceResult(floatArrayOf(0.9f, 0.1f)) // Rapid "Claw" detections
            wakeWordDetector.processAudioBuffer(generateTestAudioBuffer("claw", 1024), 1024)
        }
        
        // Should prevent excessive triggering
        assertTrue("Should handle rapid detections gracefully", detectedWakeWords.size <= 2)
    }

    @Test
    fun `INTEGRATION TEST - audio preprocessing pipeline`() = runTest {
        // Test different sample rates and formats
        val testCases = listOf(
            Pair(16000, 1024),  // Standard case
            Pair(16000, 512),   // Smaller buffer
            Pair(16000, 2048),  // Larger buffer
        )
        
        testCases.forEach { (sampleRate, bufferSize) ->
            val testAudio = generateTestAudioBuffer("claw", bufferSize)
            mockInferenceResult(floatArrayOf(0.9f, 0.1f)) // Claw detection
            
            assertDoesNotThrow("Should handle various audio formats") {
                wakeWordDetector.processAudioBuffer(testAudio, bufferSize)
            }
        }
    }

    @Test
    fun `SAFETY TEST - model failure graceful degradation`() = runTest {
        // Simulate TensorFlow model failure
        every { interpreter.run(any(), any()) } throws RuntimeException("Model inference failed")
        
        val testAudio = generateTestAudioBuffer("claw", 1024)
        
        // Should not crash, should log error and continue
        assertDoesNotThrow("Model failure should be handled gracefully") {
            wakeWordDetector.processAudioBuffer(testAudio, testAudio.size)
        }
        
        // No false detections should occur during failure
        assertTrue("No wake words should be detected during model failure", detectedWakeWords.isEmpty())
    }

    private fun mockInferenceResult(outputValues: FloatArray) {
        every { interpreter.run(any(), any()) } answers {
            val outputBuffer = secondArg<ByteBuffer>()
            outputBuffer.rewind()
            outputValues.forEach { value ->
                outputBuffer.putFloat(value)
            }
            outputBuffer.rewind()
        }
    }

    private fun generateTestAudioBuffer(content: String, size: Int): ShortArray {
        // Generate synthetic audio data for testing
        val buffer = ShortArray(size)
        val frequency = when (content) {
            "claw" -> 440.0 // A4 note - distinctive frequency for "Claw"
            else -> 100.0 // Low frequency for noise/background
        }
        
        for (i in buffer.indices) {
            buffer[i] = (Short.MAX_VALUE * 0.1 * kotlin.math.sin(2 * kotlin.math.PI * frequency * i / 16000)).toInt().toShort()
        }
        
        return buffer
    }

    private fun assertDoesNotThrow(message: String = "", block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("$message - Exception thrown: ${e.message}")
        }
    }
}