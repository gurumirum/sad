package cnedclub.sad

import cnedclub.sad.canvas.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CanvasOpDispatcher {
    private val ops = mutableMapOf<String, Deferred<Canvas?>>()

    private val dependencyLock = Mutex()
    private val dependencies = mutableMapOf<String, MutableSet<String>>()

    suspend fun dispatch(
        defaultWidth: UInt,
        defaultHeight: UInt,
        canvasOperations: Map<String, CanvasOp>,
        imageLoader: ImageLoader,
        operationFailHandle: (path: String, err: String) -> Unit
    ) = coroutineScope {
        dependencyLock.lock()
        for ((path, op) in canvasOperations) {
            val ctx = CanvasOp.Context(imageLoader, DependencyHandle(this@CanvasOpDispatcher, path))
            ops[path] = async {
                try {
                    op.run(ctx, Dimension.of(defaultWidth), Dimension.of(defaultHeight)).fold({ it }) {
                        operationFailHandle(path, it.toCanvasOperationError())
                        null
                    }
                } catch (ex: Exception) {
                    operationFailHandle(path, "Unexpected exception: $ex")
                    null
                }
            }
        }
        dependencyLock.unlock()
    }

    suspend fun awaitAll(): Map<String, Canvas?> = ops.map { (k, v) -> k to v.await() }.toMap()

    private suspend fun addDependency(entry: String, dependency: String): Result<Canvas> {
        val op = dependencyLock.withLock {
            val op = ops[entry] ?: return fail("Invalid dependency: No entry with name '$entry'")
            if (!ops.containsKey(dependency)) return fail("Invalid dependency: No entry with name '$dependency'")
            if (checkForDependency(entry, dependency)) {
                return fail("Cyclic dependency detected between entry '$entry' and '$dependency'")
            }
            val e = dependencies.getOrPut(entry) { mutableSetOf() }
            e.add(dependency)
            op
        }
        return op.await()?.let { Result.success(it) } ?: fail("One or more dependency failed")
    }

    private fun checkForDependency(entry: String, dependency: String): Boolean {
        val dep = dependencies[dependency] ?: return false
        return dep.contains(entry) || dep.any { checkForDependency(entry, it) }
    }

    class DependencyHandle(
        private val dispatcher: CanvasOpDispatcher,
        private val entry: String
    ) {
        suspend fun dependOn(entry: String) = this.dispatcher.addDependency(this.entry, entry)
    }

    companion object {
        suspend fun create(
            defaultWidth: UInt,
            defaultHeight: UInt,
            canvasOperations: Map<String, CanvasOp>,
            imageLoader: ImageLoader,
            operationFailReport: (path: String, err: String) -> Unit
        ): CanvasOpDispatcher = coroutineScope {
            val dispatcher = CanvasOpDispatcher()
            dispatcher.dispatch(defaultWidth, defaultHeight, canvasOperations, imageLoader, operationFailReport)
            dispatcher
        }
    }
}
