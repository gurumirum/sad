package gurumirum.sad.canvas

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class GradientMapOp(
    private val target: CanvasOp,
    private val gradient: GradientProvider,
    private val gradientMapChannel: Set<ColorComponent>,
    private val outputChannel: Set<ColorComponent>,
    private val rescale: Boolean
) : CanvasOp {
    override suspend fun run(
        ctx: CanvasOp.Context,
        parentWidth: Dimension,
        parentHeight: Dimension
    ): Result<Canvas> = coroutineScope {
        val targetAsync = async { target.run(ctx, parentWidth, parentHeight) }
        val gradientAsync = async { gradient.run(ctx) }

        targetAsync.await().mapResult { canvas ->
            gradientAsync.await().mapResult { gradient ->
                Result.success(MutableCanvas(canvas).apply {
                    applyGradient(gradient, gradientMapChannel, outputChannel, rescale)
                })
            }
        }
    }

    override fun toString() = "GradientMapOp(target=$target, gradient=$gradient)"
}