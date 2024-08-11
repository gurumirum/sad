package cnedclub.sad.canvas

import kotlin.math.roundToInt

@JvmInline
@Suppress("unused")
value class Color(val argb: Int) {
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

    fun lerp(other: Color, d: Float) = Color(
        lerp(this.a, other.a, d).roundToInt(),
        lerp(this.r, other.r, d).roundToInt(),
        lerp(this.g, other.g, d).roundToInt(),
        lerp(this.b, other.b, d).roundToInt()
    )

    fun copy(
        a: Int = this.a,
        r: Int = this.r,
        g: Int = this.g,
        b: Int = this.b
    ) = Color(a, r, g, b)

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "#" + (if (a == 255) rgb else argb).toHexString(HexFormat.UpperCase)

    companion object {
        val Transparent = Color(0, 0, 0, 0)
        val Black = Color(0, 0, 0)
        val White = Color(255, 255, 255)

        private val regex = Regex("#?([0-9a-fA-F]{1,8})")

        fun fromString(string: String) =
            tryParse(string) ?: throw IllegalArgumentException("Cannot parse color '$this'")

        fun tryParse(string: String): Color? {
            val m = regex.matchEntire(string) ?: return null
            val colorText = m.groupValues[1]
            return when (colorText.length) {
                1 -> c1(colorText, 0).let { Color(it, it, it) }
                2 -> c2(colorText, 0).let { Color(it, it, it) }
                3 -> Color(c1(colorText, 0), c1(colorText, 1), c1(colorText, 2))
                4 -> Color(c1(colorText, 0), c1(colorText, 1), c1(colorText, 2), c1(colorText, 3))
                6 -> Color(c2(colorText, 0), c2(colorText, 2), c2(colorText, 4))
                8 -> Color(c2(colorText, 0), c2(colorText, 2), c2(colorText, 4), c2(colorText, 6))
                else -> null
            }
        }

        private fun c1(string: String, i: Int) = string[i].digitToInt(16).let { (it shl 4) or it }
        private fun c2(string: String, i: Int) = (string[i].digitToInt(16) shl 4) or string[i + 1].digitToInt(16)

        private fun lerp(a: Int, b: Int, d: Float): Float = a + d * (b - a)
    }
}
