package cnedclub.sad.canvas

internal fun <T> fail(message: String): Result<T> = Result.failure(CanvasOpException(message))

internal inline fun <R, T> Result<T>.mapResult(transform: (value: T) -> Result<R>): Result<R> =
    this.map { return transform(it) }

internal fun Throwable.toCanvasOperationError(): String =
    if (this is CanvasOpException && message != null) message!! else toString()
