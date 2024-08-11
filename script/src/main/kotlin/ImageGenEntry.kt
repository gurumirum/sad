package gurumirum.sad.script

import gurumirum.sad.canvas.CanvasOp

data class ImageGenEntry(
    val operation: CanvasOp,
    val optimizationType: OptimizationType
)
