package gurumirum.sad.canvas

class CodeFilterOp(
    private val target: CanvasOp,
    private val filter: (MutableCanvas) -> Unit
) : CanvasOp {
    override suspend fun run(
        ctx: CanvasOp.Context,
        parentWidth: Dimension,
        parentHeight: Dimension
    ): Result<Canvas> = this.target.run(ctx, parentWidth, parentHeight).map {
        val canvas = MutableCanvas(it)
        this.filter(canvas)
        canvas
    }
}
