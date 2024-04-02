package cnedclub.sad.app

import cnedclub.sad.CanvasOpDispatcher
import cnedclub.sad.Hash
import cnedclub.sad.ImageLoader
import cnedclub.sad.VERSION
import cnedclub.sad.canvas.Canvas
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.googlecode.pngtastic.core.PngImage
import com.googlecode.pngtastic.core.PngOptimizer
import kotlinx.coroutines.*
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import kotlin.io.path.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.system.exitProcess
import kotlin.time.measureTime

fun main(args: Array<String>) = Main().main(args)

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

        val saveResult: SaveResult
        val time = measureTime {
            val ops = CanvasOpDispatcher.create(
                config.defaultWidth, config.defaultHeight, config.canvasOperations,
                ImageLoader(inputPath) { echo(it) }) { path, err ->
                echo("Canvas operation $path failed: $err")
            }.await().mapNotNull { (path, canvas) ->
                if (canvas == null) null else path to async {
                    val hash = canvas.pixelHash()
                    val prevHash = cache.await()[path]
                    if (hash == prevHash) hash to false
                    else if (saveImage(outputPath, path, canvas)) hash to true
                    else null
                }
            }.toMap()

            saveResult = save(cache.await(), ops, cachePath, outputPath)
        }

        echo("${saveResult.opsFinished} operations finished (${saveResult.filesSaved} files written) in $time")
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

    private suspend fun save(
        cache: Map<String, Hash>,
        ops: Map<String, Deferred<Pair<Hash, Boolean>?>>,
        cachePath: Path,
        outputPath: Path
    ): SaveResult {
        var opsFinished = 0
        var filesSaved = 0

        val filesToDelete = HashSet(cache.keys)
        if (noOutputCache) {
            for ((path, op) in ops.entries) {
                op.await()?.let { (_, saved) ->
                    opsFinished++
                    if (saved) filesSaved++
                    filesToDelete.remove(path)
                }
            }
        } else {
            cachePath.createParentDirectories()
            cachePath.bufferedWriter().use { w ->
                var nl = false
                for ((path, op) in ops) {
                    val (hash, saved) = op.await() ?: continue
                    if (nl) w.write("\n")
                    else nl = true
                    w.write(hash.toString())
                    w.write(" ")
                    w.write(path)
                    opsFinished++
                    if (saved) filesSaved++
                    filesToDelete.remove(path)
                }
            }
            echo("Wrote $opsFinished entries to .cache file")
        }
        for (f in filesToDelete) {
            try {
                outputPath.resolve("$f.png").deleteIfExists()
            } catch (ex: IOException) {
                echo("Cannot delete outdated output entry $f due to an exception: $ex", err = true)
            }
        }
        return SaveResult(opsFinished, filesSaved)
    }

    private data class SaveResult(val opsFinished: Int, val filesSaved: Int)

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun saveImage(outputPath: Path, path: String, canvas: Canvas): Boolean = try {
        val image = canvas.toBufferedImage()
        val png = PipedInputStream().use { inputStream ->
            coroutineScope {
                launch(imageWriterDispatcher) {
                    ImageIO.write(image, "png", PipedOutputStream(inputStream))
                }
                withContext(Dispatchers.Default.limitedParallelism(1)) {
                    optimizer.optimize(PngImage(inputStream))
                }
            }
        }

        withContext(Dispatchers.IO) {
            val outPath = outputPath.resolve("$path.png")
            outPath.createParentDirectories()
            outPath.outputStream().buffered().use { png.writeDataOutputStream(it) }
        }

        true
    } catch (ex: IOException) {
        echo("Cannot write output to $path: $ex")
        false
    }

    companion object {
        private val cachePattern = Regex("([0-9a-f]{64}) (.+)")
        private val imageWriterDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
        private val optimizer by lazy {
            PngOptimizer().apply {
                setCompressor("zopfli", null)
            }
        }
    }
}
