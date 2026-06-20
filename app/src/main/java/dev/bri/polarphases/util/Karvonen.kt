package dev.bri.polarphases.util

/** Returns the target heart rate bpm for a given Karvonen intensity (0.0–1.0). */
fun karvonenBpm(restingHr: Int, maxHr: Int, intensity: Double): Int =
    (restingHr + (maxHr - restingHr) * intensity).toInt()

data class KarvonenZone(
    val name: String,
    val colorArgb: Int,
    val bpmMin: Int,
    val bpmMax: Int,
)

fun defaultZones(restingHr: Int = 62, maxHr: Int = 179): List<KarvonenZone> {
    fun bpm(pct: Double) = karvonenBpm(restingHr, maxHr, pct)
    return listOf(
        KarvonenZone("Recovery",  0xFF64B5F6.toInt(), bpm(0.50), bpm(0.60)),
        KarvonenZone("Endurance", 0xFF66BB6A.toInt(), bpm(0.60) + 1, bpm(0.70)),
        KarvonenZone("Tempo",     0xFFFFEE58.toInt(), bpm(0.70) + 1, bpm(0.80)),
        KarvonenZone("Threshold", 0xFFFFA726.toInt(), bpm(0.80) + 1, bpm(0.90)),
        KarvonenZone("Peak",      0xFFEF5350.toInt(), bpm(0.90) + 1, maxHr),
    )
}
