package cnedclub.sad.script

import cnedclub.sad.canvas.CanvasOp

data class ImageGenEntry(
    val operation: CanvasOp,
    val optimizationType: OptimizationType
)

enum class OptimizationType {
    Default,
    NoOptimization,
    Zopfli
}
