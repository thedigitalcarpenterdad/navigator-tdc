package com.tdc.navigator

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * SAFETY-CRITICAL Test Runner
 * 
 * Executes all safety-critical tests for the Navigator TDC application.
 * This test suite must pass with 90%+ coverage before any deployment.
 */
@ExperimentalCoroutinesApi
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Vehicle Control System Tests
    com.tdc.navigator.vehicle.NavigatorVehicleManagerTest::class,
    com.tdc.navigator.vehicle.control.NavigatorClimateControlTest::class,
    com.tdc.navigator.vehicle.control.NavigatorWindowControlTest::class,
    com.tdc.navigator.vehicle.control.NavigatorSeatControlTest::class,
    com.tdc.navigator.vehicle.control.NavigatorLightingControlTest::class,
    com.tdc.navigator.vehicle.control.NavigatorDoorControlTest::class,
    com.tdc.navigator.vehicle.control.NavigatorMirrorControlTest::class,
    com.tdc.navigator.vehicle.control.NavigatorAudioControlTest::class,
    com.tdc.navigator.vehicle.control.NavigatorDriveModeControlTest::class,
    
    // Wake Word Detection Tests
    com.tdc.navigator.wakework.TDCWakeWordDetectorTest::class,
    com.tdc.navigator.service.WakeWordDetectionServiceTest::class,
    
    // Core Service Tests
    com.tdc.navigator.service.TDCCarAppServiceTest::class,
    com.tdc.navigator.session.TDCNavigatorSessionTest::class,
    
    // Safety and Security Tests
    com.tdc.navigator.safety.EmergencyStopSystemTest::class,
    com.tdc.navigator.security.VehicleAccessSecurityTest::class,
    
    // Utility Tests
    com.tdc.navigator.util.LoggerTest::class
)
class TDCNavigatorTestSuite