package gurumirum.sad

import gurumirum.sad.canvas.Canvas
import gurumirum.sad.canvas.toCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.io.path.inputStream

class ImageLoader(
    private val basePath: Path,
    private val errorReporter: (Throwable) -> Unit
) {
    private val cache = ConcurrentHashMap<String, Canvas?>()

    suspend fun readFrom(path: String): Canvas? = this.cache.getOrPut(path) {
        try {
            withContext(Dispatchers.IO) {
                basePath.resolve("$path.png").inputStream().buffered().use(ImageIO::read).toCanvas()
            }
        } catch (err: IOException) {
            this.errorReporter(err)
            null
        }
    }
}
