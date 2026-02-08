package com.tdc.navigator.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tdc.navigator.R
import com.tdc.navigator.wakework.TDCWakeWordDetector
import com.tdc.navigator.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Wake Word Detection Service
 * 
 * Continuously listens for "Claw" wake word activation
 * using a custom TensorFlow Lite model optimized for automotive environments.
 * 
 * Simple, distinctive activation: "Claw" → TDC conversation mode
 */
class WakeWordDetectionService : Service() {

    companion object {
        private const val TAG = "WakeWordService"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "tdc_wake_word"
        private const val SAMPLE_RATE = 16000
        private const val BUFFER_SIZE = 4096
        
        // Wake word configuration - "Claw" activation
        private val WAKE_WORDS = arrayOf("claw")
        private const val CONFIDENCE_THRESHOLD = 0.75f
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var audioRecord: AudioRecord? = null
    private var detectionJob: Job? = null
    private lateinit var wakeWordDetector: TDCWakeWordDetector
    private var isListening = false

    override fun onCreate() {
        super.onCreate()
        Logger.d(TAG, "Wake word detection service created")
        
        createNotificationChannel()
        initializeWakeWordDetector()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d(TAG, "Starting wake word detection")
        
        startForeground(NOTIFICATION_ID, createNotification())
        startWakeWordDetection()
        
        return START_STICKY // Restart if killed
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d(TAG, "Wake word detection service destroyed")
        
        stopWakeWordDetection()
        serviceScope.cancel()
        wakeWordDetector.cleanup()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "TDC Wake Word Detection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Always listening for Claw voice commands"
            setSound(null, null)
            enableVibration(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("TDC Voice Assistant")
            .setContentText("Listening for voice commands...")
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun initializeWakeWordDetector() {
        try {
            wakeWordDetector = TDCWakeWordDetector(
                context = this,
                wakeWords = WAKE_WORDS,
                confidenceThreshold = CONFIDENCE_THRESHOLD,
                onWakeWordDetected = { wakeWord, confidence ->
                    handleWakeWordDetected(wakeWord, confidence)
                }
            )
            Logger.d(TAG, "Wake word detector initialized")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize wake word detector", e)
        }
    }

    private fun startWakeWordDetection() {
        if (isListening) return
        
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(bufferSize, BUFFER_SIZE)
            ).apply {
                startRecording()
            }
            
            isListening = true
            
            detectionJob = serviceScope.launch {
                val audioBuffer = ShortArray(BUFFER_SIZE)
                
                while (isActive && isListening) {
                    val bytesRead = audioRecord?.read(audioBuffer, 0, BUFFER_SIZE) ?: 0
                    
                    if (bytesRead > 0) {
                        // Process audio through wake word detector
                        wakeWordDetector.processAudioBuffer(audioBuffer, bytesRead)
                    }
                }
            }
            
            Logger.d(TAG, "Wake word detection started")
            
        } catch (e: SecurityException) {
            Logger.e(TAG, "Microphone permission not granted", e)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to start wake word detection", e)
        }
    }

    private fun stopWakeWordDetection() {
        isListening = false
        detectionJob?.cancel()
        
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                stop()
            }
            release()
        }
        audioRecord = null
        
        Logger.d(TAG, "Wake word detection stopped")
    }

    private fun handleWakeWordDetected(wakeWord: String, confidence: Float) {
        Logger.d(TAG, "Wake word detected: '$wakeWord' (confidence: $confidence)")
        
        // Trigger voice assistant activation
        val activationIntent = Intent("com.tdc.navigator.WAKE_WORD_DETECTED").apply {
            putExtra("wake_word", wakeWord)
            putExtra("confidence", confidence)
            putExtra("timestamp", System.currentTimeMillis())
        }
        
        sendBroadcast(activationIntent)
        
        // Provide haptic feedback if available
        // Note: In automotive context, haptic feedback might be through seat vibration
        // This would need integration with Lincoln's haptic systems
        
        // Update notification to show activation
        val activeNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("TDC Voice Assistant")
            .setContentText("Voice command activated - Listening...")
            .setSmallIcon(R.drawable.ic_mic_active)
            .setOngoing(true)
            .setSilent(true)
            .build()
            
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, activeNotification)
        
        // Reset notification after delay
        serviceScope.launch {
            kotlinx.coroutines.delay(3000) // 3 seconds
            val defaultNotification = createNotification()
            notificationManager.notify(NOTIFICATION_ID, defaultNotification)
        }
    }
}