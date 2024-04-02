package cnedclub.sad

import cnedclub.sad.canvas.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
        errorHandler: (path: String, err: String) -> Unit
    ) = coroutineScope {
        dependencyLock.withLock {
            for ((path, op) in canvasOperations) {
                val ctx = CanvasOp.Context(imageLoader, DependencyHandle(this@CanvasOpDispatcher, path))
                ops[path] = async {
                    try {
                        op.run(ctx, Dimension.of(defaultWidth), Dimension.of(defaultHeight)).fold({ it }) {
                            errorHandler(path, it.toCanvasOperationError())
                            null
                        }
                    } catch (ex: Exception) {
                        errorHandler(path, "Unexpected exception: $ex")
                        null
                    }
                }
            }
        }
    }

    suspend fun await(): Map<String, Canvas?> = coroutineScope {
        awaitAll(*ops.map { (k, v) -> async { k to v.await() } }.toTypedArray()).toMap()
    }

    private suspend fun addDependency(entry: String, dependency: String): Result<Canvas> {
        val op = dependencyLock.withLock {
            if (!ops.containsKey(entry)) return fail("Invalid dependency: No entry with name '$entry'")
            val op = ops[dependency] ?: return fail("Invalid dependency: No entry with name '$dependency'")
            if (entry == dependency) return fail("Entry '$entry' depending on itself")
            if (checkCycles(entry, dependency)) {
                return fail("Cyclic dependency detected between entry '$entry' and '$dependency'")
            }
            val e = dependencies.getOrPut(entry) { mutableSetOf() }
            e.add(dependency)
            op
        }
        return op.await()?.let { Result.success(it) } ?: fail("One or more dependency failed")
    }

    private fun checkCycles(entry: String, dependency: String): Boolean {
        val dep = dependencies[dependency] ?: return false
        return dep.contains(entry) || dep.any { checkCycles(entry, it) }
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
            errorHandler: (path: String, err: String) -> Unit
        ): CanvasOpDispatcher = coroutineScope {
            val dispatcher = CanvasOpDispatcher()
            dispatcher.dispatch(defaultWidth, defaultHeight, canvasOperations, imageLoader, errorHandler)
            dispatcher
        }
    }
}
