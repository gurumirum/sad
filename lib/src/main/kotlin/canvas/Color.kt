package gurumirum.sad.canvas

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
    override fun toString() = "#" + argb.toHexString(HexFormat.UpperCase)

    companion object {
        val Transparent = Color(0, 0, 0, 0)

        val AliceBlue = fromRgb(0xf0f8ff)
        val AntiqueWhite = fromRgb(0xfaebd7)
        val Aqua = fromRgb(0x00ffff)
        val Aquamarine = fromRgb(0x7fffd4)
        val Azure = fromRgb(0xf0ffff)
        val Beige = fromRgb(0xf5f5dc)
        val Bisque = fromRgb(0xffe4c4)
        val Black = fromRgb(0x000000)
        val BlanchedAlmond = fromRgb(0xffebcd)
        val Blue = fromRgb(0x0000ff)
        val BlueViolet = fromRgb(0x8a2be2)
        val Brown = fromRgb(0xa52a2a)
        val Burlywood = fromRgb(0xdeb887)
        val CadetBlue = fromRgb(0x5f9ea0)
        val Chartreuse = fromRgb(0x7fff00)
        val Chocolate = fromRgb(0xd2691e)
        val Coral = fromRgb(0xff7f50)
        val CornflowerBlue = fromRgb(0x6495ed)
        val CornSilk = fromRgb(0xfff8dc)
        val Crimson = fromRgb(0xdc143c)
        val Cyan = fromRgb(0x00ffff)
        val DarkBlue = fromRgb(0x00008b)
        val DarkCyan = fromRgb(0x008b8b)
        val DarkGoldenRod = fromRgb(0xb8860b)
        val DarkGray = fromRgb(0xa9a9a9)
        val DarkGreen = fromRgb(0x006400)
        val DarkGrey = fromRgb(0xa9a9a9)
        val DarkKhaki = fromRgb(0xbdb76b)
        val DarkMagenta = fromRgb(0x8b008b)
        val DarkOliveGreen = fromRgb(0x556b2f)
        val DarkOrange = fromRgb(0xff8c00)
        val DarkOrchid = fromRgb(0x9932cc)
        val DarkRed = fromRgb(0x8b0000)
        val DarkSalmon = fromRgb(0xe9967a)
        val DarkSeaGreen = fromRgb(0x8fbc8f)
        val DarkSlateBlue = fromRgb(0x483d8b)
        val DarkSlateGray = fromRgb(0x2f4f4f)
        val DarkSlateGrey = fromRgb(0x2f4f4f)
        val DarkTurquoise = fromRgb(0x00ced1)
        val DarkViolet = fromRgb(0x9400d3)
        val DeepPink = fromRgb(0xff1493)
        val DeepSkyBlue = fromRgb(0x00bfff)
        val DimGray = fromRgb(0x696969)
        val DimGrey = fromRgb(0x696969)
        val DodgerBlue = fromRgb(0x1e90ff)
        val FireBrick = fromRgb(0xb22222)
        val FloralWhite = fromRgb(0xfffaf0)
        val ForestGreen = fromRgb(0x228b22)
        val Fuchsia = fromRgb(0xff00ff)
        val Gainsboro = fromRgb(0xdcdcdc)
        val GhostWhite = fromRgb(0xf8f8ff)
        val Gold = fromRgb(0xffd700)
        val GoldenRod = fromRgb(0xdaa520)
        val Gray = fromRgb(0x808080)
        val Green = fromRgb(0x008000)
        val GreenYellow = fromRgb(0xadff2f)
        val Grey = fromRgb(0x808080)
        val Honeydew = fromRgb(0xf0fff0)
        val HotPink = fromRgb(0xff69b4)
        val IndianRed = fromRgb(0xcd5c5c)
        val Indigo = fromRgb(0x4b0082)
        val Ivory = fromRgb(0xfffff0)
        val Khaki = fromRgb(0xf0e68c)
        val Lavender = fromRgb(0xe6e6fa)
        val LavenderBlush = fromRgb(0xfff0f5)
        val LawnGreen = fromRgb(0x7cfc00)
        val LemonChiffon = fromRgb(0xfffacd)
        val LightBlue = fromRgb(0xadd8e6)
        val LightCoral = fromRgb(0xf08080)
        val LightCyan = fromRgb(0xe0ffff)
        val LightGoldenRodYellow = fromRgb(0xfafad2)
        val LightGray = fromRgb(0xd3d3d3)
        val LightGreen = fromRgb(0x90ee90)
        val LightGrey = fromRgb(0xd3d3d3)
        val LightPink = fromRgb(0xffb6c1)
        val LightSalmon = fromRgb(0xffa07a)
        val LightSeaGreen = fromRgb(0x20b2aa)
        val LightSkyBlue = fromRgb(0x87cefa)
        val LightSlateGray = fromRgb(0x778899)
        val LightSlateGrey = fromRgb(0x778899)
        val LightSteelBlue = fromRgb(0xb0c4de)
        val LightYellow = fromRgb(0xffffe0)
        val Lime = fromRgb(0x00ff00)
        val LimeGreen = fromRgb(0x32cd32)
        val Linen = fromRgb(0xfaf0e6)
        val Magenta = fromRgb(0xff00ff)
        val Maroon = fromRgb(0x800000)
        val MediumAquamarine = fromRgb(0x66cdaa)
        val MediumBlue = fromRgb(0x0000cd)
        val MediumOrchid = fromRgb(0xba55d3)
        val MediumPurple = fromRgb(0x9370db)
        val MediumSeaGreen = fromRgb(0x3cb371)
        val MediumSlateBlue = fromRgb(0x7b68ee)
        val MediumSpringGreen = fromRgb(0x00fa9a)
        val MediumTurquoise = fromRgb(0x48d1cc)
        val MediumVioletRed = fromRgb(0xc71585)
        val MidnightBlue = fromRgb(0x191970)
        val MintCream = fromRgb(0xf5fffa)
        val MistyRose = fromRgb(0xffe4e1)
        val Moccasin = fromRgb(0xffe4b5)
        val NavajoWhite = fromRgb(0xffdead)
        val Navy = fromRgb(0x000080)
        val OldLace = fromRgb(0xfdf5e6)
        val Olive = fromRgb(0x808000)
        val OliveDrab = fromRgb(0x6b8e23)
        val Orange = fromRgb(0xffa500)
        val OrangeRed = fromRgb(0xff4500)
        val Orchid = fromRgb(0xda70d6)
        val PaleGoldenRod = fromRgb(0xeee8aa)
        val PaleGreen = fromRgb(0x98fb98)
        val PaleTurquoise = fromRgb(0xafeeee)
        val PaleVioletRed = fromRgb(0xdb7093)
        val PapayaWhip = fromRgb(0xffefd5)
        val Peachpuff = fromRgb(0xffdab9)
        val Peru = fromRgb(0xcd853f)
        val Pink = fromRgb(0xffc0cb)
        val Plum = fromRgb(0xdda0dd)
        val PowderBlue = fromRgb(0xb0e0e6)
        val Purple = fromRgb(0x800080)
        val RebeccaPurple = fromRgb(0x663399)
        val Red = fromRgb(0xff0000)
        val RosyBrown = fromRgb(0xbc8f8f)
        val RoyalBlue = fromRgb(0x4169e1)
        val SaddleBrown = fromRgb(0x8b4513)
        val Salmon = fromRgb(0xfa8072)
        val SandyBrown = fromRgb(0xf4a460)
        val SeaGreen = fromRgb(0x2e8b57)
        val SeaShell = fromRgb(0xfff5ee)
        val Sienna = fromRgb(0xa0522d)
        val Silver = fromRgb(0xc0c0c0)
        val SkyBlue = fromRgb(0x87ceeb)
        val SlateBlue = fromRgb(0x6a5acd)
        val SlateGray = fromRgb(0x708090)
        val SlateGrey = fromRgb(0x708090)
        val Snow = fromRgb(0xfffafa)
        val SpringGreen = fromRgb(0x00ff7f)
        val SteelBlue = fromRgb(0x4682b4)
        val Tan = fromRgb(0xd2b48c)
        val Teal = fromRgb(0x008080)
        val Thistle = fromRgb(0xd8bfd8)
        val Tomato = fromRgb(0xff6347)
        val Turquoise = fromRgb(0x40e0d0)
        val Violet = fromRgb(0xee82ee)
        val Wheat = fromRgb(0xf5deb3)
        val White = fromRgb(0xffffff)
        val WhiteSmoke = fromRgb(0xf5f5f5)
        val Yellow = fromRgb(0xffff00)
        val YellowGreen = fromRgb(0x9acd32)

        private val regex = Regex("#?([0-9a-fA-F]{1,8})")

        fun fromString(string: String) =
            tryParse(string) ?: throw IllegalArgumentException("Cannot parse color '$this'")

        fun fromRgb(rgb: Int) =
            Color((rgb.toLong() or 0xFF000000).toInt())

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
