package cnedclub.sad.canvas

import java.util.*
import kotlin.math.max
import kotlin.math.min

fun MutableCanvas.applyGradient(
    gradient: Gradient,
    gradientMapChannel: Set<ColorComponent>,
    outputChannel: Set<ColorComponent>,
    rescale: Boolean
) {
    if (gradientMapChannel.isEmpty() || outputChannel.isEmpty()) return
    val mapAlpha = ColorComponent.A in gradientMapChannel

    var minValue: Float
    var maxValue: Float

    if (rescale) {
        minValue = 1f
        maxValue = 0f

        this.forEachPixel { x, y ->
            val px = this[x, y]
            if (px.a == 0 && !mapAlpha) return@forEachPixel
            val mapValue = px.getGradientMapValue(gradientMapChannel)
            minValue = min(minValue, mapValue)
            maxValue = max(maxValue, mapValue)
        }
    } else {
        minValue = 0f
        maxValue = 1f
    }
    this.forEachPixel { x, y ->
        val px = this[x, y]
        if (px.a == 0 && !mapAlpha) return@forEachPixel
        val mapValue = proportion(minValue, maxValue, px.getGradientMapValue(gradientMapChannel))
        this[x, y] = px.replace(gradient[mapValue], outputChannel)
    }
}

private fun proportion(min: Float, max: Float, value: Float): Float =
    if (max == min) value else (value - min) / (max - min)

private fun Color.getGradientMapValue(channels: Set<ColorComponent>): Float {
    val sum = channels.sumOf { this[it] }
    return sum.toFloat() / (255f * channels.size)
}

private fun Color.replace(other: Color, channels: Set<ColorComponent>): Color = Color(
    (if (ColorComponent.A in channels) other else this).a,
    (if (ColorComponent.R in channels) other else this).r,
    (if (ColorComponent.G in channels) other else this).g,
    (if (ColorComponent.B in channels) other else this).b
)

interface GradientProvider {
    suspend fun run(ctx: CanvasOp.Context): Result<Gradient>
}

interface Gradient {
    operator fun get(value: Float): Color
}

class TextureGradientProvider(
    private val texture: CanvasOp,
    private val index: UInt,
    private val direction: GradientDirection
) : GradientProvider {
    override suspend fun run(ctx: CanvasOp.Context): Result<Gradient> {
        return texture.run(ctx, Dimension.AUTO, Dimension.AUTO).mapResult { texture ->
            val pixels: Array<Color> =
                if (direction.xAxis) Array(texture.width.toInt()) { texture[it.toUInt(), index] }
                else Array(texture.height.toInt()) { texture[index, it.toUInt()] }
            if (direction.reversed) pixels.reverse()

            Result.success(SimpleGradient(sortedMapOf(*pixels.mapIndexed { index, color ->
                (index.toFloat() / pixels.size.toFloat()) to color
            }.toTypedArray())))
        }
    }

    override fun toString() = "TextureGradient(texture='$texture', index=$index, direction=$direction)"
}

@Suppress("unused")
enum class GradientDirection(val xAxis: Boolean, val reversed: Boolean) {
    LeftToRight(true, false),
    RightToLeft(true, true),
    TopToBottom(false, false),
    BottomToTop(false, true);
}

class SimpleGradient(
    private val elements: SortedMap<Float, Color>
) : Gradient, GradientProvider {
    override fun get(value: Float): Color {
        val head = this.elements.headMap(value).let { if (it.isEmpty()) null else it.lastKey() }
        val tail = this.elements.tailMap(value).let { if (it.isEmpty()) null else it.firstKey() }
        if (head == null && tail == null) return Color.Black
        if (head == null) return this.elements[tail]!!
        if (tail == null) return this.elements[head]!!
        return this.elements[head]!!.lerp(this.elements[tail]!!, (value - head) / (tail - head))
    }

    override suspend fun run(ctx: CanvasOp.Context): Result<Gradient> = Result.success(this)

    override fun toString() = "SimpleGradient(elements=$elements)"
}
