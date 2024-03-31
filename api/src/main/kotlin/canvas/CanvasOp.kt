package cnedclub.sad.canvas

import cnedclub.sad.CanvasOpDispatcher
import cnedclub.sad.ImageLoader

sealed interface CanvasOp {
    suspend fun run(
        ctx: Context,
        parentWidth: Dimension,
        parentHeight: Dimension
    ): Result<Canvas>

    data class Context(
        val imageLoader: ImageLoader,
        val dependencyHandle: CanvasOpDispatcher.DependencyHandle
    )
}
