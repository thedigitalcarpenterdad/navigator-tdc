package com.tdc.navigator.service

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.tdc.navigator.session.TDCNavigatorSession
import com.tdc.navigator.util.Logger

/**
 * TDC Navigator Car App Service
 * 
 * Main entry point for Android Automotive OS integration.
 * Handles session management and vehicle system integration.
 */
class TDCCarAppService : CarAppService() {

    companion object {
        private const val TAG = "TDCCarAppService"
    }

    override fun createHostValidator(): HostValidator {
        // Allow all automotive hosts for now
        // In production, restrict to specific Lincoln/Ford SYNC 4A signatures
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        Logger.d(TAG, "Creating new TDC Navigator session")
        return TDCNavigatorSession().also { session ->
            // Add lifecycle observer for proper cleanup
            session.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    Logger.d(TAG, "TDC Navigator session destroyed")
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d(TAG, "TDC Navigator service started")
        
        // Start wake word detection if not already running
        startWakeWordDetection()
        
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startWakeWordDetection() {
        val wakeWordIntent = Intent(this, WakeWordDetectionService::class.java)
        startForegroundService(wakeWordIntent)
        Logger.d(TAG, "Wake word detection service started")
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d(TAG, "TDC Navigator service destroyed")
        
        // Stop wake word detection
        val wakeWordIntent = Intent(this, WakeWordDetectionService::class.java)
        stopService(wakeWordIntent)
    }
}