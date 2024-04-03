package cnedclub.sad.canvas

class ColorFillOp(
    private val fill: Color,
    private val width: Dimension,
    private val height: Dimension,
) : CanvasOp {
    override suspend fun run(
        ctx: CanvasOp.Context,
        parentWidth: Dimension,
        parentHeight: Dimension
    ): Result<Canvas> {
        val width = this.width.or(parentWidth).orElse { return fail("Cannot resolve width") }
        val height = this.height.or(parentHeight).orElse { return fail("Cannot resolve height") }
        return Result.success(SingleColorCanvas(width, height, fill))
    }

    override fun toString() = "ColorFillOp(fill=$fill, width=$width, height=$height)"
}
