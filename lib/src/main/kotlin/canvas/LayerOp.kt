package cnedclub.sad.canvas

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class LayerOp(
    private val entries: List<Entry>,
    private val width: Dimension,
    private val height: Dimension,
    private val equation: BlendEquation,
    private val srcColor: BlendFunc,
    private val dstColor: BlendFunc,
    private val srcAlpha: BlendFunc,
    private val dstAlpha: BlendFunc
) : CanvasOp {
    override suspend fun run(
        ctx: CanvasOp.Context,
        parentWidth: Dimension,
        parentHeight: Dimension
    ): Result<Canvas> = coroutineScope {
        val width = width.or(parentWidth).orElse { return@coroutineScope fail("Cannot resolve width") }
        val height = height.or(parentHeight).orElse { return@coroutineScope fail("Cannot resolve height") }
        val errors = arrayListOf<Throwable>()
        val entries =
            awaitAll(*entries.map { async { it to it.op.run(ctx, Dimension.of(width), Dimension.of(height)) } }
                .toTypedArray())
                .mapNotNull { (entry, res) ->
                    res.fold({
                        entry to it
                    }) {
                        errors.add(it)
                        null
                    }
                }

        if (errors.isNotEmpty()) {
            fail(
                "One of the child operations failed:\n  ${
                    errors.joinToString(
                        "\n  ",
                        transform = { it.toCanvasOperationError() })
                }"
            )
        } else if (entries.isEmpty()) {
            Result.success(SingleColorCanvas(width, height, Color.Transparent))
        } else {
            val ret = MutableCanvas(width, height)
            var first = true
            for ((e, c) in entries.reversed()) {
                ret.forEachPixel { x, y ->
                    val x0 = (x.toLong() - e.xOffset)
                    val y0 = (y.toLong() - e.yOffset)
                    if (x0 < 0 || y0 < 0 || !c.isInBoundary(x0.toUInt(), y0.toUInt())) return@forEachPixel
                    val src = c[x0.toUInt(), y0.toUInt()]
                    ret[x, y] = if (first) src else blend(
                        src, ret[x, y],
                        equation, srcColor, dstColor, srcAlpha, dstAlpha
                    )
                }
                if (first) first = false
            }
            Result.success(ret)
        }
    }

    override fun toString() =
        "LayerOp(entries=$entries, width=$width, height=$height, equation=$equation, srcColor=$srcColor, dstColor=$dstColor, srcAlpha=$srcAlpha, dstAlpha=$dstAlpha)"

    data class Entry(
        val op: CanvasOp,
        val xOffset: Int,
        val yOffset: Int
    )
}
