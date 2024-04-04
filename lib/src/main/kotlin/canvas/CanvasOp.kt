package cnedclub.sad.canvas

import cnedclub.sad.ImageLoader

interface CanvasOp {
    suspend fun run(
        ctx: Context,
        parentWidth: Dimension,
        parentHeight: Dimension
    ): Result<Canvas>

    data class Context(
        val imageLoader: ImageLoader,
        val dependencyHandle: DependencyHandle
    )

    interface DependencyHandle {
        suspend fun dependOn(entry: String): Result<Canvas>
    }
}
