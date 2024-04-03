package cnedclub.sad.canvas

class ColorTintOp(
    private val target: CanvasOp,
    private val color: Color
) : CanvasOp {
    override suspend fun run(
        ctx: CanvasOp.Context,
        parentWidth: Dimension,
        parentHeight: Dimension
    ): Result<Canvas> = this.target.run(ctx, parentWidth, parentHeight).map {
        MutableCanvas.fill(it.width, it.height) { x, y ->
            multiplyColor(it[x, y], this.color)
        }
    }

    private fun multiplyColor(a: Color, b: Color) = Color(
        a.a * b.a / 255,
        a.r * b.r / 255,
        a.g * b.g / 255,
        a.b * b.b / 255,
    )

    override fun toString() = "ColorTintOp(target=$target, color=$color)"
}
