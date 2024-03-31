package cnedclub.sad.canvas

class DependencyOp(private val path: String) : CanvasOp {
    override suspend fun run(ctx: CanvasOp.Context, parentWidth: Dimension, parentHeight: Dimension): Result<Canvas> =
        ctx.dependencyHandle.dependOn(this.path)

    override fun toString() = "DependencyOp(path='$path')"
}