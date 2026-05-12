package com.dji.mini3activetrack.controller

import kotlin.math.abs

class PIDController(
    var kp: Double,
    var ki: Double,
    var kd: Double,
    val outputMin: Double = -1.0,
    val outputMax: Double = 1.0,
    val integralLimit: Double = 1.0,
    val deadband: Double = 0.02
) {
    private var integral = 0.0
    private var previousError = 0.0
    private var lastUpdateTime = 0L
    private var isFirstUpdate = true

    fun update(error: Double): Double {
        val now = System.currentTimeMillis()
        val dt = if (isFirstUpdate) {
            isFirstUpdate = false
            lastUpdateTime = now
            0.05
        } else {
            val elapsed = (now - lastUpdateTime) / 1000.0
            lastUpdateTime = now
            elapsed.coerceIn(0.001, 0.5)
        }

        val effectiveError = if (abs(error) < deadband) 0.0 else error
        
        val proportional = kp * effectiveError
        
        integral += effectiveError * dt
        integral = integral.coerceIn(-integralLimit, integralLimit)
        val integralTerm = ki * integral

        val derivative = if (dt > 0) kd * (effectiveError - previousError) / dt else 0.0
        previousError = effectiveError

        return (proportional + integralTerm + derivative).coerceIn(outputMin, outputMax)
    }

    fun reset() {
        integral = 0.0
        previousError = 0.0
        isFirstUpdate = true
        lastUpdateTime = 0L
    }
}
