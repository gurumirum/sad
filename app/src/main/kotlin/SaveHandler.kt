package cnedclub.sad.app

import cnedclub.sad.Hash
import cnedclub.sad.canvas.Canvas
import com.googlecode.pngtastic.core.PngImage
import com.googlecode.pngtastic.core.PngOptimizer
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

class SaveHandler(
    private val tracker: OpTracker,
    pngOptimizerInstances: Int
) : Closeable {
    private val optimizerThreads = Executors.newFixedThreadPool(pngOptimizerInstances).asCoroutineDispatcher()

    suspend fun saveImage(path: String, canvas: Canvas, outputPath: Path): Boolean {
        tracker.updateStatus(path, OpTracker.Stage.COMPRESSING)
        optimize(canvas).fold({ png ->
            try {
                tracker.updateStatus(path, OpTracker.Stage.SAVING)
                withContext(Dispatchers.IO) {
                    outputPath.resolve("$path.png")
                        .createParentDirectories()
                        .outputStream().buffered().use {
                            png.writeDataOutputStream(it)
                        }
                }
                return true
            } catch (ex: IOException) {
                tracker.updateStatus(path, OpTracker.Stage.SAVING_FAILED)
                tracker.addReport(path, "Cannot write output: $ex", true)
                return false
            }
        }) {
            tracker.updateStatus(path, OpTracker.Stage.COMPRESSING_FAILED)
            tracker.addReport(path, "Cannot compress PNG file: $it", true)
            return false
        }
    }

    private suspend fun optimize(canvas: Canvas): Result<PngImage> = withContext(optimizerThreads) {
        try {
            Result.success(pngOptimizerThreadLocal.get().optimize(PngImage(ByteArrayOutputStream().also {
                ImageIO.write(canvas.toBufferedImage(), "png", it)
            }.toByteArray())))
        } catch (ex: IOException) {
            Result.failure(ex)
        }
    }

    suspend fun updateCache(
        cache: Map<String, Hash>,
        ops: Map<String, Deferred<Result<Hash>>>,
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
                    w.write(it.toString())
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
        this.optimizerThreads.close()
    }
}

private val pngOptimizerThreadLocal = ThreadLocal.withInitial {
    PngOptimizer().apply {
        setCompressor("zopfli", null)
    }
}
