package cnedclub.sad.app

import cnedclub.sad.CanvasOpDispatcher
import cnedclub.sad.Hash
import cnedclub.sad.ImageLoader
import cnedclub.sad.VERSION
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.*
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.bufferedReader
import kotlin.script.experimental.host.toScriptSource
import kotlin.system.exitProcess
import kotlin.time.measureTime

fun main(args: Array<String>) = Main().context {
    terminal = Terminal(interactive = true)
}.main(args)

class Main : CliktCommand(
    help = "idk man"
) {
    private val input: Path? by option().path(mustExist = true, canBeFile = false, mustBeReadable = true)
    private val output: Path? by option().path(canBeFile = false)
    private val configPath: Path? by option().path(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val cachePath: Path? by option().path(mustExist = false, canBeDir = false)
    private val ignoreCache: Boolean by option("--ignore-cache").flag("--use-cache", default = false)
    private val noOutputCache: Boolean by option("--no-output-cache").flag("--output-cache", default = false)

    override fun run(): Unit = runBlocking {
        val inputPath = input ?: Path("")
        val outputPath = output ?: inputPath.resolve("out")
        val configPath = configPath ?: inputPath.resolve("config.sad.kts")
        val cachePath = cachePath ?: outputPath.resolve(".cache")

        echo("SAD Version $VERSION")
        echo("INPUT: ${inputPath.toAbsolutePath()}")
        echo("OUTPUT: ${outputPath.toAbsolutePath()}")
        echo("CONFIG: ${configPath.toAbsolutePath()}")
        if (!ignoreCache)
            echo("CACHE: ${cachePath.toAbsolutePath()}")
        if (ignoreCache || noOutputCache)
            echo(buildString {
                append("Flags: ")
                if (ignoreCache) append("--ignore-cache")
                if (noOutputCache) {
                    if (ignoreCache) append(" ")
                    append("--no-output-cache")
                }
            })
        echo("")

        val cache = async { readCache(cachePath) }
        val config = readConfig(configPath) ?: return@runBlocking

        echo("Processing ${config.canvasOperations.size} operations")

        val tracker = OpTracker(config.canvasOperations.keys)
        val updater = tracker.startUpdate(this, currentContext.terminal)
        val saveHandler = SaveHandler(tracker, 2)

        var opsFinished = 0
        var filesWritten = 0

        val time = measureTime {
            val ops: Map<String, Deferred<Result<Hash>>> = CanvasOpDispatcher.create(
                config.defaultWidth, config.defaultHeight, config.canvasOperations,
                ImageLoader(inputPath) {
                    tracker.addGenericReport("Failed to load image file: $it", true)
                }
            ).operations.mapValues { (path, canvasAsync) ->
                async {
                    canvasAsync.await().fold({ canvas ->
                        val hash = canvas.pixelHash()
                        val prevHash = cache.await()[path]
                        if (hash == prevHash) {
                            tracker.updateStatus(path, OpTracker.Stage.SKIPPED)
                            opsFinished++
                            Result.success(hash)
                        } else if (saveHandler.saveImage(path, canvas, outputPath)) {
                            tracker.updateStatus(path, OpTracker.Stage.FINISHED)
                            opsFinished++
                            filesWritten++
                            Result.success(hash)
                        } else {
                            Result.failure(RuntimeException("Failed to save"))
                        }
                    }, {
                        tracker.updateStatus(path, OpTracker.Stage.PROCESSING_FAILED)
                        tracker.addReport(path, "Image processing failed: $it", true)
                        Result.failure(it)
                    })
                }
            }.toMap()

            saveHandler.updateCache(cache.await(), ops, cachePath, outputPath, noOutputCache)
        }

        updater.stop()
        tracker.printReports(currentContext.terminal)

        echo("\n\n${opsFinished} operation(s) finished (${filesWritten} file(s) written) in $time")
        exitProcess(0)
    }

    private fun readConfig(configPath: Path): Config? {
        try {
            val file = configPath.toFile()
            if (file.isFile) return evaluateConfig(file.toScriptSource()) { s, err -> echo(s, err = err) }
            echo("Cannot locate config file at '${configPath.toAbsolutePath()}'.")
            return null
        } catch (ex: Exception) {
            echo("Cannot load config file due to an exception: $ex")
            return null
        }
    }

    private suspend fun readCache(cachePath: Path): Map<String, Hash> = withContext(Dispatchers.IO) {
        if (ignoreCache) emptyMap() else try {
            cachePath.bufferedReader().useLines {
                val map = hashMapOf<String, Hash>()
                for ((i, line) in it.withIndex()) {
                    val m = cachePattern.matchEntire(line)
                    if (m == null) echo("Malformed cache at line ${i + 1} skipped")
                    else map[m.groupValues[2]] = Hash(m.groupValues[1])
                }
                echo("Read ${map.size} cache entries")
                map
            }
        } catch (ignored: NoSuchFileException) {
            echo("Cannot locate .cache file.")
            emptyMap()
        }
    }

    companion object {
        private val cachePattern = Regex("([0-9a-f]{64}) (.+)")
    }
}
