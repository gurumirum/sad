package cnedclub.sad

import cnedclub.sad.canvas.Canvas
import cnedclub.sad.canvas.CanvasOp
import cnedclub.sad.canvas.Dimension
import cnedclub.sad.canvas.fail
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CanvasOpDispatcher {
    private val ops = mutableMapOf<String, Deferred<Result<Canvas>>>()

    val operations: Map<String, Deferred<Result<Canvas>>>
        get() = ops

    private val dependencyLock = Mutex()
    private val dependencies = mutableMapOf<String, MutableSet<String>>()

    suspend fun dispatch(
        defaultWidth: UInt,
        defaultHeight: UInt,
        canvasOperations: Map<String, CanvasOp>,
        imageLoader: ImageLoader
    ) = coroutineScope {
        dependencyLock.withLock {
            for ((path, op) in canvasOperations) {
                val ctx = CanvasOp.Context(imageLoader, DependencyHandle(this@CanvasOpDispatcher, path))
                ops[path] = async {
                    try {
                        op.run(ctx, Dimension.of(defaultWidth), Dimension.of(defaultHeight))
                    } catch (ex: Exception) {
                        fail("Unexpected exception: $ex")
                    }
                }
            }
        }
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
        return op.await().fold({ Result.success(it) }) { fail("One or more dependency failed") }
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
        ): CanvasOpDispatcher = coroutineScope {
            val dispatcher = CanvasOpDispatcher()
            dispatcher.dispatch(defaultWidth, defaultHeight, canvasOperations, imageLoader)
            dispatcher
        }
    }
}
