package cnedclub.sad.canvas

import cnedclub.sad.Hash
import java.awt.image.BufferedImage

interface Canvas {
    val width: UInt
    val height: UInt

    operator fun get(x: UInt, y: UInt): ARGB

    fun isInBoundary(x: UInt, y: UInt) = x in 0u..<width && y in 0u..<height

    fun pixelHash(): Hash
    fun toBufferedImage(): BufferedImage
}

class MutableCanvas(
    override val width: UInt,
    override val height: UInt,
    private val data: IntArray
) : Canvas {
    init {
        if (data.size.toUInt() != width * height) throw IllegalArgumentException("mismatching data size")
    }

    constructor(width: UInt, height: UInt) : this(width, height, IntArray((width * height).toInt()))
    constructor(canvas: Canvas) : this(canvas.width, canvas.height) {
        if (canvas is MutableCanvas) canvas.data.copyInto(this.data)
        else canvas.forEachPixel { x, y -> this[x, y] = canvas[x, y] }
    }

    override operator fun get(x: UInt, y: UInt): ARGB = ARGB(data[(y * width + x).toInt()])
    operator fun set(x: UInt, y: UInt, argb: ARGB) {
        data[(y * width + x).toInt()] = argb.argb
    }

    override fun pixelHash() = Hash(this.data)

    override fun toBufferedImage() =
        BufferedImage(this.width.toInt(), this.height.toInt(), BufferedImage.TYPE_INT_ARGB).also {
            it.setRGB(0, 0, width.toInt(), height.toInt(), data, 0, width.toInt())
        }

    companion object {
        inline fun fill(width: UInt, height: UInt, fill: (x: UInt, y: UInt) -> ARGB) =
            MutableCanvas(width, height, IntArray((width * height).toInt()) {
                fill((it % width.toInt()).toUInt(), (it / width.toInt()).toUInt()).argb
            })
    }
}

fun Canvas.copySubArea(width: UInt, height: UInt, xOffset: UInt = 0u, yOffset: UInt = 0u) =
    MutableCanvas.fill(width, height) { x, y -> this[x + xOffset, y + yOffset] }

inline fun Canvas.forEachPixel(action: (x: UInt, y: UInt) -> Unit) {
    for (x in 0u until width) {
        for (y in 0u until height) {
            action(x, y)
        }
    }
}

fun BufferedImage.toCanvas(): MutableCanvas =
    MutableCanvas(width.toUInt(), height.toUInt(), getRGB(0, 0, width, height, null, 0, width))
