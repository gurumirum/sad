package gurumirum.sad.app

import com.github.ajalt.clikt.core.CliktCommand
import gurumirum.sad.Hash
import gurumirum.sad.script.OperationType
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createParentDirectories

object CacheIO {
    private val metadataPattern = Regex("""%metadata\s(\S+)\s*(.+)""")
    private val cachePattern = Regex("""(\S+)\s+([0-9a-f]{64})\s+(.+)""")
    private val cachePatternLegacy = Regex("([0-9a-f]{64}) (.+)")

    suspend fun readCache(command: CliktCommand, cachePath: Path): Map<String, OpHash> = withContext(Dispatchers.IO) {
        fun reportMalformedMetadata(lineIndex: Int, reason: String? = null) {
            command.echo("Malformed cache metadata at line ${lineIndex + 1}${reason?.let { ": $it" } ?: ""}")
        }

        fun reportMalformedCache(lineIndex: Int, reason: String? = null) {
            command.echo("Malformed cache at line ${lineIndex + 1}${reason?.let { ": $it" } ?: ""}")
        }

        try {
            cachePath.bufferedReader().useLines {
                var v2 = false
                val map = hashMapOf<String, OpHash>()
                for ((i, line) in it.withIndex()) {
                    if (line.isBlank()) continue

                    when (val metadata = parseMetadata(line)) {
                        is MetadataParseResult.Version -> {
                            when (metadata.version) {
                                1u -> v2 = false
                                2u -> v2 = true
                                else -> reportMalformedMetadata(
                                    i,
                                    reason = "Unsupported version ${metadata.version}"
                                )
                            }
                        }
                        is MetadataParseResult.ParseError -> {
                            reportMalformedMetadata(i, reason = "Unsupported version ${metadata.reason}")
                        }
                        null -> {
                            val hash = if (v2) parseV2CacheLine(line) else parseV1CacheLine(line)
                            when (hash) {
                                is CacheParseResult.Fail -> reportMalformedCache(i, reason = hash.reason)
                                is CacheParseResult.Success -> {
                                    if (map.putIfAbsent(hash.key, hash.hash) != null) {
                                        command.echo("Duplicated cache entry at ${i + 1}, entry \"${hash.key}\" - skipping")
                                    }
                                }
                            }
                        }
                    }
                }
                command.echo("Read ${map.size} cache entries")
                map
            }
        } catch (ignored: NoSuchFileException) {
            command.echo("Cannot locate .cache file")
            emptyMap()
        }
    }

    private fun parseMetadata(line: String): MetadataParseResult? {
        val m = metadataPattern.matchEntire(line) ?: return null

        return when (m.groupValues[1]) {
            "version" -> m.groupValues[2].trimEnd().toUIntOrNull()
                ?.let { MetadataParseResult.Version(it) }
                ?: MetadataParseResult.ParseError("Invalid version ${m.groupValues[2]}")
            else -> MetadataParseResult.ParseError("Unsupported metadata entry ${m.groupValues[1]}")
        }
    }

    private fun parseV1CacheLine(line: String): CacheParseResult {
        val m = cachePatternLegacy.matchEntire(line) ?: return CacheParseResult.Fail()
        return CacheParseResult.Success(
            "${m.groupValues[2]}.png",
            OpHash(OperationType.IMAGE, Hash.parse(m.groupValues[1]))
        )
    }

    private fun parseV2CacheLine(line: String): CacheParseResult {
        val m = cachePattern.matchEntire(line) ?: return CacheParseResult.Fail()
        val type = when (m.groupValues[1]) {
            "image" -> OperationType.IMAGE
            "text" -> OperationType.TEXT
            else -> return CacheParseResult.Fail("Unsupported entry type ${m.groupValues[1]}")
        }
        return CacheParseResult.Success(
            URLDecoder.decode(m.groupValues[3], Charsets.UTF_8),
            OpHash(type, Hash.parse(m.groupValues[2]))
        )
    }

    suspend inline fun writeCache(
        ops: Map<String, Deferred<Result<Lazy<OpHash>>>>,
        cachePath: Path,
        cacheEntryWritten: (String) -> Unit
    ) {
        cachePath.createParentDirectories().bufferedWriter().use { w ->
            w.write("%metadata version 2")
            var first = true
            for ((path, op) in ops) {
                op.await().onSuccess {
                    if (first) {
                        first = false
                        w.write("\n")
                    }
                    w.write("\n")

                    val opHash = it.value
                    w.write(opHash.type.cacheEntryText)
                    w.write(" ")
                    w.write(opHash.hash.toString())
                    w.write(" ")
                    w.write(URLEncoder.encode(path, Charsets.UTF_8)
                        .replace("%2F", "/"))
                    cacheEntryWritten(path)
                }
            }
        }
    }

    private sealed interface MetadataParseResult {
        data class Version(val version: UInt) : MetadataParseResult
        data class ParseError(val reason: String? = null) : MetadataParseResult
    }

    private sealed interface CacheParseResult {
        data class Success(val key: String, val hash: OpHash) : CacheParseResult
        data class Fail(val reason: String? = null) : CacheParseResult
    }
}
