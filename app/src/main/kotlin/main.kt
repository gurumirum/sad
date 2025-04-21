package gurumirum.sad.app

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.terminal.Terminal
import gurumirum.sad.Hash
import gurumirum.sad.ImageLoader
import gurumirum.sad.VERSION
import gurumirum.sad.script.ImageGen
import gurumirum.sad.script.OperationType
import gurumirum.sad.script.TextGen
import kotlinx.coroutines.*
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeText
import kotlin.script.experimental.host.toScriptSource
import kotlin.system.exitProcess
import kotlin.time.TimeSource.Monotonic.markNow

fun main(args: Array<String>) = NoOpCliktCommand("sad").context {
    terminal = Terminal(interactive = true)
}.subcommands(Generate(), Init()).main(args)

class Generate : CliktCommand() {
    private val input: Path? by option(
        help = "Base directory for all other files and directories; all individual paths can be configured via parameters"
    ).path(mustExist = true, canBeFile = false, mustBeReadable = true)

    private val output: Path? by option(
        help = "Base directory for cache file and generated image files"
    ).path(canBeFile = false)

    private val config: Path? by option(
        help = "Location of .sad.kts file"
    ).path(mustExist = true, canBeDir = false, mustBeReadable = true)

    private val cache: Path? by option(
        help = "Location of cache file"
    ).path(mustExist = false, canBeDir = false)

    private val ignoreCache: Boolean by option(
        "--ignore-cache",
        help = "Whether to ignore cache and re-generate all outputs again"
    ).flag("--use-cache", default = false)
    private val noOutputCache: Boolean by option(
        "--no-output-cache",
        help = "Whether to skip updating cache file"
    ).flag("--output-cache", default = false)
    private val maxCompressingParallel: Int? by option(
        "--max-compressing-parallel",
        help = "Maximum number of process usable for image compression, default is 4"
    ).int().validate {
        require(it > 0) { "max-compressing-parallel must be positive" }
    }

    override fun help(context: Context): String = "Generate images with given configuration data and input files"

    override fun run(): Unit = runBlocking {
        val startTime = markNow()

        val inputPath = input ?: Path("")
        val outputPath = output ?: inputPath.resolve("out")
        val configPath = config ?: inputPath.resolve("config.sad.kts")
        val cachePath = cache ?: outputPath.resolve(".cache")

        echo("SAD Version $VERSION")
        echo("INPUT: ${inputPath.toAbsolutePath()}")
        echo("OUTPUT: ${outputPath.toAbsolutePath()}")
        echo("CONFIG: ${configPath.toAbsolutePath()}")
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

        val cache = async { CacheIO.readCache(this@Generate, cachePath) }
        val config = readConfig(configPath) ?: return@runBlocking

        echo("Processing ${config.operations.size} operations")

        val tracker = OpTracker(config.operations.keys, startTime)
        val updater = tracker.startUpdate(this, currentContext.terminal)
        val saveHandler = SaveHandler(tracker, maxCompressingParallel ?: 4)

        var opsFinished = 0
        var filesWritten = 0

        val imageGenOps = mutableMapOf<String, ImageGen>()
        val textGenOps = mutableMapOf<String, TextGen>()

        for ((k, v) in config.operations) {
            when (v) {
                is ImageGen -> imageGenOps[k] = v
                is TextGen -> textGenOps[k] = v
            }
        }

        val ops: Map<String, Deferred<Result<Lazy<OpHash>>>> = CanvasOpDispatcher.create(
            config.defaultWidth, config.defaultHeight, imageGenOps,
            ImageLoader(inputPath) {
                tracker.addGenericReport("Failed to load image file: $it", true)
            }
        ).operations.mapValues { (path, entry) ->
            async {
                entry.canvasOp.await().fold({ canvas ->
                    val hash = lazy {
                        OpHash(OperationType.IMAGE, canvas.pixelHash(entry.optimizationType.metadata()))
                    }
                    if (!isChanged(path, hash, cache)) {
                        tracker.updateStatus(path, OpTracker.Stage.SKIPPED)
                        opsFinished++
                        Result.success(hash)
                    } else if (saveHandler.saveImage(path, canvas, outputPath, entry.optimizationType)) {
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
        } + textGenOps.mapValues { (path, textGen) ->
            async {
                val hash = lazy {
                    OpHash(OperationType.TEXT, Hash.of(textGen.text))
                }
                if (!isChanged(path, hash, cache)) {
                    tracker.updateStatus(path, OpTracker.Stage.SKIPPED)
                    opsFinished++
                    Result.success(hash)
                } else if (saveHandler.saveText(path, textGen.text, outputPath)) {
                    tracker.updateStatus(path, OpTracker.Stage.FINISHED)
                    opsFinished++
                    filesWritten++
                    Result.success(hash)
                } else {
                    Result.failure(RuntimeException("Failed to save"))
                }
            }
        }

        saveHandler.updateCache(cache.await(), ops, cachePath, outputPath, noOutputCache)

        updater.stop()
        tracker.printReports(currentContext.terminal)

        echo("\n\n${opsFinished} operation(s) finished (${filesWritten} file(s) written) in ${startTime.elapsedNow()}")
        exitProcess(0)
    }

    private fun readConfig(configPath: Path): Config? {
        try {
            val file = configPath.toFile()
            if (file.isFile) return evaluateConfig(file.toScriptSource()) { s, err -> echo(s, err = err) }
            echo("Cannot locate config file at '${configPath.toAbsolutePath()}'.")
            return null
        } catch (ex: Exception) {
            echo("Cannot load config file due to an exception: $ex", err = true)
            return null
        }
    }

    private suspend fun isChanged(
        path: String,
        canvasHash: Lazy<OpHash>,
        cache: Deferred<Map<String, OpHash>>
    ): Boolean {
        if (this.ignoreCache) return true
        val hash = cache.await()[path] ?: return true
        return canvasHash.value != hash
    }
}

class Init : CliktCommand() {
    private val input: Path? by option().path(mustExist = true, canBeFile = false, mustBeReadable = true)
    private val config: Path? by option().path(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val force: Boolean by option("--force", "-f").flag(default = false)

    override fun help(context: Context): String = "Create sample .sad.kts config file"

    override fun run() {
        val inputPath = input ?: Path("")
        val configPath = config ?: inputPath.resolve("config.sad.kts")

        if (!force && configPath.isRegularFile()) {
            echo("Config file already exists, use --force to", err = true)
            exitProcess(1)
        } else {
            try {
                configPath.writeText(DEFAULT_CONFIG, options = arrayOf(StandardOpenOption.CREATE))
            } catch (ex: Exception) {
                echo("Cannot write config file due to an exception: $ex", err = true)
                exitProcess(1)
            }
            exitProcess(0)
        }
    }
}
