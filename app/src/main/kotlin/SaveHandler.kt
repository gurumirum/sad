package gurumirum.sad.app

import gurumirum.sad.Hash
import gurumirum.sad.canvas.Canvas
import gurumirum.sad.script.OptimizationType
import com.googlecode.pngtastic.core.PngImage
import com.googlecode.pngtastic.core.PngOptimizer
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

// size of zopfli png optimizers are motherfucking beasts, so I can't just spam them
private const val MAX_PARALLEL = 4

class SaveHandler(private val tracker: OpTracker) : Closeable {
    private val optimizerContext = ThreadPoolExecutor(
        0, MAX_PARALLEL,
        60L, TimeUnit.SECONDS,
        LinkedBlockingQueue()
    ).asCoroutineDispatcher()

    suspend fun saveImage(path: String, canvas: Canvas, outputPath: Path, optimizationType: OptimizationType): Boolean =
        when (optimizationType) {
            OptimizationType.DefaultOptimization -> optimizeAndSave(canvas, false, path, outputPath)
            OptimizationType.ZopfliOptimization -> optimizeAndSave(canvas, true, path, outputPath)
            OptimizationType.NoOptimization -> write(path, outputPath) {
                ImageIO.write(canvas.toBufferedImage(), "png", it)
            }
        }

    private suspend fun optimizeAndSave(canvas: Canvas, zopfli: Boolean, path: String, outputPath: Path): Boolean {
        tracker.updateStatus(path, OpTracker.Stage.COMPRESSING)
        return withContext(optimizerContext) {
            try {
                val optimizer = (if (zopfli) zopfliOptimizerThreadLocal else defaultOptimizerThreadLocal).get()
                Result.success(optimizer.optimize(PngImage(ByteArrayOutputStream().also {
                    ImageIO.write(canvas.toBufferedImage(), "png", it)
                }.toByteArray())))
            } catch (ex: IOException) {
                Result.failure(ex)
            }
        }.fold({ png ->
            write(path, outputPath) { png.writeDataOutputStream(it) }
        }) {
            tracker.updateStatus(path, OpTracker.Stage.COMPRESSING_FAILED)
            tracker.addReport(path, "Cannot compress PNG file: $it", true)
            false
        }
    }

    private suspend inline fun write(
        path: String,
        outputPath: Path,
        crossinline writer: (BufferedOutputStream) -> Unit
    ): Boolean {
        tracker.updateStatus(path, OpTracker.Stage.SAVING)
        return try {
            withContext(Dispatchers.IO) {
                outputPath.resolve("$path.png")
                    .createParentDirectories()
                    .outputStream().buffered()
                    .use(writer)
            }
            true
        } catch (ex: IOException) {
            tracker.updateStatus(path, OpTracker.Stage.SAVING_FAILED)
            tracker.addReport(path, "Cannot write output: $ex", true)
            false
        }
    }

    suspend fun updateCache(
        cache: Map<String, Hash>,
        ops: Map<String, Deferred<Result<Lazy<Hash>>>>,
        cachePath: Path,
        outputPath: Path,
        noOutputCache: Boolean
    ) {
        val filesToDelete = HashSet(cache.keys)
        if (noOutputCache) {
            for ((path, op) in ops) {
                op.await().onSuccess { filesToDelete.remove(path) }
            }
        } else cachePath.createParentDirectories().bufferedWriter().use { w ->
            var nl = false
            for ((path, op) in ops) {
                op.await().onSuccess {
                    if (nl) w.write("\n")
                    else nl = true
                    w.write(it.value.toString())
                    w.write(" ")
                    w.write(path)
                    filesToDelete.remove(path)
                }
            }
        }

        for (f in filesToDelete) {
            try {
                outputPath.resolve("$f.png").deleteIfExists()
            } catch (ex: IOException) {
                tracker.addGenericReport("Cannot delete outdated output entry $f due to an exception: $ex", true)
            }
        }
    }

    override fun close() {
        this.optimizerContext.close()
    }
}

private val defaultOptimizerThreadLocal = ThreadLocal.withInitial { PngOptimizer() }
private val zopfliOptimizerThreadLocal = ThreadLocal.withInitial {
    PngOptimizer().apply {
        setCompressor("zopfli", null)
    }
}
