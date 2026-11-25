package com.trainwise.app.model
import java.time.LocalDate

data class CheckIn(val fatigue: Int = 2, val legs: Int = 2, val pain: Int = 0, val motivation: Int = 4, val note: String = "")
data class TrainingBlock(
    val longRunMinutes: Int = 75, val longRunTargetMinutes: Int = 110,
    val sundayMode: String = "long", val bikeGear: Boolean = false,
    val z2PaceMinSec: Int = 380, val z2PaceMaxSec: Int = 400,
    val weeklyVolumePct: Int = 50, val minRunMinutes: Int = 30
)
data class Goal(val name: String = "Базовый беговой объём", val date: String = "", val priority: String = "A", val note: String = "Лёгкий бег без интервалов. Силовая без отказа.")
data class Race(val id: String, val name: String, val date: String, val distance: String, val type: String, val priority: String, val target: String = "")
data class Recovery(val hrv: Int, val restingHr: Int, val sleepScore: Int, val bodyBatteryHigh: Int, val stress: Int)
data class Workout(
    val id: String, val date: String, val type: String, val workoutKind: String, val title: String,
    val distanceKm: Double? = null, val minutes: Double, val avgPace: String? = null,
    val avgPaceSec: Int? = null, val firstKmPaceSec: Int? = null, val avgHr: Int? = null,
    val maxHr: Int? = null, val hrDriftPct: Double? = null, val paceCv: Double? = null,
    val cadenceSpm: Int? = null, val rpe: Int? = null, val trainingEffect: Double? = null,
    val trainingLoad: Double? = null, val highHrPct: Double? = null, val highHrThreshold: Int? = null,
    val weeklyRunKm: Double? = null, val totalReps: Int? = null,
    val recoveryBefore: Recovery? = null, val recoveryNextDay: Recovery? = null
)
data class TrendPoint(val label: String, val load: Int, val recovery: Int)
data class AppState(
    val schemaVersion: Int = 15,
    val baselineHrv: Int? = null, val baselineRestingHr: Int? = null, val baselineSleepHours: Double? = null,
    val todayHrv: Int? = null, val todayRestingHr: Int? = null, val todaySleepHours: Double? = null,
    val todayBodyBattery: Int? = null, val todayStress: Int? = null,
    val checkIn: CheckIn = CheckIn(), val trainingBlock: TrainingBlock = TrainingBlock(),
    val goal: Goal = Goal(), val races: List<Race> = emptyList(),
    val workouts: List<Workout> = emptyList(), val trend: List<TrendPoint> = emptyList()
)
data class PlannedSession(
    val date: LocalDate, val type: String, val title: String, val minutes: Int,
    val intensity: String, val purpose: String, val durationLabel: String, val distanceKm: Double? = null
)
