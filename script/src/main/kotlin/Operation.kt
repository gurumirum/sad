package gurumirum.sad.script

import gurumirum.sad.canvas.CanvasOp

sealed interface Operation {
    val type: OperationType
}

data class ImageGen(
    val canvasOp: CanvasOp,
    val optimizationType: OptimizationType
) : Operation {
    override val type: OperationType
        get() = OperationType.IMAGE
}

data class TextGen(
    val text: String
) : Operation {
    override val type: OperationType
        get() = OperationType.TEXT
}

enum class OperationType(val cacheEntryText: String) {
    IMAGE("image"),
    TEXT("text")
}
