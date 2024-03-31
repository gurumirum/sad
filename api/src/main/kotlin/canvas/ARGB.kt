package cnedclub.sad.canvas

import kotlin.math.roundToInt

@JvmInline
value class ARGB(val argb: Int) {
    val a: Int
        get() = argb shr 24 and 0xff
    val r: Int
        get() = argb shr 16 and 0xff
    val g: Int
        get() = argb shr 8 and 0xff
    val b: Int
        get() = argb and 0xff

    val rgb: Int
        get() = argb and 0xffffff

    constructor(r: Int, g: Int, b: Int) : this(255, r, g, b)
    constructor(a: Int, r: Int, g: Int, b: Int) : this(
        (a.coerceIn(0, 255) shl 24) or
                (r.coerceIn(0, 255) shl 16 and 0xff0000) or
                (g.coerceIn(0, 255) shl 8 and 0xff00) or
                (b.coerceIn(0, 255) and 0xff)
    )

    operator fun get(c: ColorComponent) = when (c) {
        ColorComponent.A -> a
        ColorComponent.R -> r
        ColorComponent.G -> g
        ColorComponent.B -> b
    }

    fun lerp(other: ARGB, d: Float) = ARGB(
        lerp(this.a, other.a, d).roundToInt(),
        lerp(this.r, other.r, d).roundToInt(),
        lerp(this.g, other.g, d).roundToInt(),
        lerp(this.b, other.b, d).roundToInt()
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "#" + (if (a == 255) rgb else argb).toHexString(HexFormat.UpperCase)

    companion object {
        val BLACK = ARGB(0, 0, 0)

        private val regex = Regex("#?([0-9a-fA-F]{1,8})")

        fun tryParse(string: String): ARGB? {
            val m = regex.matchEntire(string) ?: return null
            val colorText = m.groupValues[1]
            return when (colorText.length) {
                1 -> c1(colorText, 0).let { ARGB(it, it, it) }
                2 -> c2(colorText, 0).let { ARGB(it, it, it) }
                3 -> ARGB(c1(colorText, 0), c1(colorText, 1), c1(colorText, 2))
                4 -> ARGB(c1(colorText, 0), c1(colorText, 1), c1(colorText, 2), c1(colorText, 3))
                6 -> ARGB(c2(colorText, 0), c2(colorText, 2), c2(colorText, 4))
                8 -> ARGB(c2(colorText, 0), c2(colorText, 2), c2(colorText, 4), c2(colorText, 6))
                else -> null
            }
        }

        private fun c1(string: String, i: Int) = string[i].digitToInt(16).let { (it shl 4) or it }
        private fun c2(string: String, i: Int) = (string[i].digitToInt(16) shl 4) or string[i + 1].digitToInt(16)

        private fun lerp(a: Int, b: Int, d: Float): Float = a + d * (b - a)
    }
}
