package dev.bri.polarphases.ble

object HrParser {
    /**
     * Parses HR Measurement characteristic 0x2A37.
     * Byte 0 flags: bit 0 = 0 → 8-bit HR value, bit 0 = 1 → 16-bit HR value.
     */
    fun parse(value: ByteArray): Int? {
        if (value.isEmpty()) return null
        val flags = value[0].toInt() and 0xFF
        val is16Bit = (flags and 0x01) != 0
        return if (is16Bit) {
            if (value.size < 3) null
            else (value[1].toInt() and 0xFF) or ((value[2].toInt() and 0xFF) shl 8)
        } else {
            if (value.size < 2) null
            else value[1].toInt() and 0xFF
        }
    }
}
