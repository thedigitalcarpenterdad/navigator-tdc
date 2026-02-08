package com.tdc.navigator.util

import android.util.Log
import com.tdc.navigator.BuildConfig

/**
 * TDC Navigator Logger
 * 
 * Centralized logging utility for the Navigator TDC application.
 * Provides structured logging with proper tag management and
 * conditional logging based on build configuration.
 */
object Logger {
    
    private const val APP_TAG = "TDC_Navigator"
    private const val MAX_TAG_LENGTH = 23 // Android log tag limit
    
    // Log level configuration
    private val isDebugMode = BuildConfig.DEBUG
    private val enableLogging = BuildConfig.ENABLE_LOGGING
    
    /**
     * Verbose logging - most detailed level
     */
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        if (enableLogging && isDebugMode) {
            val formattedTag = formatTag(tag)
            if (throwable != null) {
                Log.v(formattedTag, message, throwable)
            } else {
                Log.v(formattedTag, message)
            }
        }
    }
    
    /**
     * Debug logging - development information
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (enableLogging) {
            val formattedTag = formatTag(tag)
            if (throwable != null) {
                Log.d(formattedTag, message, throwable)
            } else {
                Log.d(formattedTag, message)
            }
        }
    }
    
    /**
     * Info logging - general information
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        if (enableLogging) {
            val formattedTag = formatTag(tag)
            if (throwable != null) {
                Log.i(formattedTag, message, throwable)
            } else {
                Log.i(formattedTag, message)
            }
        }
    }
    
    /**
     * Warning logging - potential issues
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (enableLogging) {
            val formattedTag = formatTag(tag)
            if (throwable != null) {
                Log.w(formattedTag, message, throwable)
            } else {
                Log.w(formattedTag, message)
            }
        }
    }
    
    /**
     * Error logging - actual problems
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        // Always log errors, regardless of configuration
        val formattedTag = formatTag(tag)
        if (throwable != null) {
            Log.e(formattedTag, message, throwable)
        } else {
            Log.e(formattedTag, message)
        }
    }
    
    /**
     * What a Terrible Failure logging - should never happen
     */
    fun wtf(tag: String, message: String, throwable: Throwable? = null) {
        val formattedTag = formatTag(tag)
        if (throwable != null) {
            Log.wtf(formattedTag, message, throwable)
        } else {
            Log.wtf(formattedTag, message)
        }
    }
    
    /**
     * Vehicle command logging - special category for voice commands
     */
    fun command(tag: String, command: String, result: String = "processing") {
        if (enableLogging) {
            val formattedTag = formatTag(tag)
            Log.i(formattedTag, "COMMAND: '$command' -> $result")
        }
    }
    
    /**
     * Vehicle status logging - special category for vehicle state changes
     */
    fun status(tag: String, system: String, status: String) {
        if (enableLogging) {
            val formattedTag = formatTag(tag)
            Log.i(formattedTag, "STATUS: $system = $status")
        }
    }
    
    /**
     * Wake word detection logging
     */
    fun wakeWord(tag: String, wakeWord: String, confidence: Float) {
        if (enableLogging) {
            val formattedTag = formatTag(tag)
            Log.i(formattedTag, "WAKE_WORD: '$wakeWord' (confidence: $confidence)")
        }
    }
    
    /**
     * Performance logging for timing critical operations
     */
    fun perf(tag: String, operation: String, durationMs: Long) {
        if (enableLogging && isDebugMode) {
            val formattedTag = formatTag(tag)
            Log.d(formattedTag, "PERF: $operation took ${durationMs}ms")
        }
    }
    
    /**
     * Security logging for sensitive operations
     */
    fun security(tag: String, event: String, details: String = "") {
        val formattedTag = formatTag(tag)
        val message = if (details.isNotEmpty()) {
            "SECURITY: $event - $details"
        } else {
            "SECURITY: $event"
        }
        Log.w(formattedTag, message)
    }
    
    /**
     * Format tag to include app prefix and respect length limits
     */
    private fun formatTag(tag: String): String {
        val fullTag = "${APP_TAG}_$tag"
        return if (fullTag.length > MAX_TAG_LENGTH) {
            // Truncate the tag portion, keep app prefix
            val availableLength = MAX_TAG_LENGTH - APP_TAG.length - 1
            "${APP_TAG}_${tag.take(availableLength)}"
        } else {
            fullTag
        }
    }
    
    /**
     * Utility function to measure and log execution time
     */
    inline fun <T> measureTime(tag: String, operation: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        return try {
            block().also {
                val duration = System.currentTimeMillis() - startTime
                perf(tag, operation, duration)
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            e(tag, "$operation failed after ${duration}ms", e)
            throw e
        }
    }
    
    /**
     * Log vehicle system initialization
     */
    fun systemInit(tag: String, system: String, success: Boolean, details: String = "") {
        val status = if (success) "SUCCESS" else "FAILED"
        val message = if (details.isNotEmpty()) {
            "INIT: $system = $status - $details"
        } else {
            "INIT: $system = $status"
        }
        
        if (success) {
            i(tag, message)
        } else {
            e(tag, message)
        }
    }
    
    /**
     * Log automotive safety events
     */
    fun safety(tag: String, event: String, severity: SafetySeverity, details: String = "") {
        val message = if (details.isNotEmpty()) {
            "SAFETY [${severity.name}]: $event - $details"
        } else {
            "SAFETY [${severity.name}]: $event"
        }
        
        when (severity) {
            SafetySeverity.INFO -> i(tag, message)
            SafetySeverity.WARNING -> w(tag, message)
            SafetySeverity.CRITICAL -> e(tag, message)
            SafetySeverity.EMERGENCY -> wtf(tag, message)
        }
    }
    
    enum class SafetySeverity {
        INFO,       // Normal safety system operation
        WARNING,    // Potential safety concern
        CRITICAL,   // Safety system failure
        EMERGENCY   // Immediate safety threat
    }
}