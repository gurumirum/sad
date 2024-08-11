package gurumirum.sad.canvas

class RawImageOp(val path: String) : CanvasOp {
    override suspend fun run(
        ctx: CanvasOp.Context,
        parentWidth: Dimension,
        parentHeight: Dimension
    ): Result<Canvas> = ctx.imageLoader.readFrom(path)?.let { Result.success(it) } ?: fail("No image at path '$path'")

    override fun toString() = "RawImageOp(path='$path')"
}
