package cnedclub.sad.script

import cnedclub.sad.canvas.*
import java.util.*
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

@KotlinScript(
    fileExtension = "sad.kts",
    compilationConfiguration = ConfigScriptCompilationConfig::class
)
abstract class ConfigScript

object ConfigScriptCompilationConfig : ScriptCompilationConfiguration({
    defaultImports("cnedclub.sad.*")
    defaultImports("cnedclub.sad.canvas.*")
    defaultImports("cnedclub.sad.canvas.BlendEquation.*")
    defaultImports("cnedclub.sad.canvas.BlendFunc.*")
    defaultImports("cnedclub.sad.canvas.GradientDirection.*")

    implicitReceivers(ConfigScriptRoot::class)

    jvm {
        // Extract the whole classpath from context classloader and use it as dependencies
        dependenciesFromCurrentContext(wholeClasspath = true)
        compilerOptions.append("-jvm-target", "17")
    }
}) {
    private fun readResolve(): Any = ConfigScriptCompilationConfig
}

class ConfigScriptEvalConfig(root: ConfigScriptRoot) : ScriptEvaluationConfiguration({
    implicitReceivers.put(listOf(root))
})

@Suppress("unused", "MemberVisibilityCanBePrivate")
class ConfigScriptRoot(
    private val canvasOperations: MutableMap<String, CanvasOp>,
    private val reportHandle: (String, err: Boolean) -> Unit
) {
    var defaultWidth: Int? = null
        set(value) {
            if (value != null && value < 0) throw RuntimeException("Negative width is not allowed")
            field = value
        }
    var defaultHeight: Int? = null
        set(value) {
            if (value != null && value < 0) throw RuntimeException("Negative height is not allowed")
            field = value
        }

    fun generate(path: String, canvasOp: CanvasOp): DependencyOp {
        validatePath(path)
        if (canvasOperations.putIfAbsent(path, canvasOp) != null) {
            throw IllegalStateException("Entry for location '$path' already exists")
        }
        return DependencyOp(path)
    }

    fun image(path: String): RawImageOp {
        validatePath(path)
        return RawImageOp(path)
    }

    fun fill(color: String, width: Int? = null, height: Int? = null) =
        ColorRectOp(color.toColorOrThrow(), width.dim(), height.dim())

    fun dependency(path: String) = DependencyOp(path)

    fun region(
        source: CanvasOp,
        xOffset: Int,
        yOffset: Int,
        width: Int? = null,
        height: Int? = null
    ) = RegionOp(source, xOffset.toUIntChecked(), yOffset.toUIntChecked(), width.dim(), height.dim())

    fun layer(
        vararg entries: CanvasOp,
        equation: BlendEquation = BlendEquation.Add,
        srcBlend: BlendFunc = BlendFunc.SrcAlpha,
        dstBlend: BlendFunc = BlendFunc.OneMinusSrcAlpha,
        width: Int? = null,
        height: Int? = null
    ): LayerOp = layer(
        entries = entries,
        equation = equation,
        srcColor = srcBlend,
        dstColor = dstBlend,
        srcAlpha = srcBlend,
        dstAlpha = dstBlend,
        width = width,
        height = height,
    )

    fun layer(
        vararg entries: CanvasOp,
        equation: BlendEquation,
        srcColor: BlendFunc,
        dstColor: BlendFunc,
        srcAlpha: BlendFunc,
        dstAlpha: BlendFunc,
        width: Int? = null,
        height: Int? = null
    ) = LayerOp(
        entries.map { LayerOp.Entry(it, 0, 0) },
        width.dim(), height.dim(),
        equation, srcColor, dstColor, srcAlpha, dstAlpha
    )

    inline fun layer(
        equation: BlendEquation = BlendEquation.Add,
        srcBlend: BlendFunc = BlendFunc.SrcAlpha,
        dstBlend: BlendFunc = BlendFunc.OneMinusSrcAlpha,
        width: Int? = null,
        height: Int? = null,
        builder: LayerBuilder.() -> Unit
    ): LayerOp = layer(
        equation = equation,
        srcColor = srcBlend,
        dstColor = dstBlend,
        srcAlpha = srcBlend,
        dstAlpha = dstBlend,
        width = width,
        height = height,
        builder = builder
    )

    inline fun layer(
        equation: BlendEquation,
        srcColor: BlendFunc,
        dstColor: BlendFunc,
        srcAlpha: BlendFunc,
        dstAlpha: BlendFunc,
        width: Int? = null,
        height: Int? = null,
        builder: LayerBuilder.() -> Unit
    ) = LayerOp(
        mutableListOf<LayerOp.Entry>().also {
            builder(LayerBuilder(it))
        },
        width.dim(), height.dim(),
        equation, srcColor, dstColor, srcAlpha, dstAlpha
    )

    fun gradientMap(
        target: CanvasOp,
        gradient: GradientProvider,
        gradientMapChannel: String = "RGB",
        outputChannel: String = "RGBA",
        rescale: Boolean = true
    ) = GradientMapOp(
        target, gradient, parseChannel(gradientMapChannel, reportHandle),
        parseChannel(outputChannel, reportHandle), rescale
    )

    fun gradient(
        texture: CanvasOp,
        index: Int,
        direction: GradientDirection = GradientDirection.LeftToRight
    ) = TextureGradientProvider(texture, index.toUIntChecked(), direction)

    inline fun gradient(
        builder: SimpleGradientBuilder.() -> Unit
    ) = SimpleGradient(sortedMapOf<Float, ARGB>().also {
        builder(SimpleGradientBuilder(it))
    })
}

@Suppress("unused", "MemberVisibilityCanBePrivate")
class LayerBuilder(private val entries: MutableList<LayerOp.Entry>) {
    fun add(op: CanvasOp, xOffset: Int = 0, yOffset: Int = 0) {
        entries += LayerOp.Entry(op, xOffset, yOffset)
    }
}

@Suppress("unused", "MemberVisibilityCanBePrivate")
class SimpleGradientBuilder(private val elements: SortedMap<Float, ARGB>) {
    fun add(point: Float, color: String) {
        if (point >= 0 && point < 1) {
            if (elements.containsKey(point)) throw IllegalStateException("Duplicated gradient point at value $point")
            elements[point] = color.toColorOrThrow()
        } else throw IllegalArgumentException("Invalid point value $point, must be in range of 0 ~ 1")
    }
}

private fun parseChannel(str: String, reportHandle: (String, err: Boolean) -> Unit): Set<ColorComponent> =
    EnumSet.noneOf(ColorComponent::class.java).apply {
        str.chars().forEach {
            when (it.toChar()) {
                'A', 'a' -> addComponent(it.toChar(), ColorComponent.A, reportHandle)
                'R', 'r' -> addComponent(it.toChar(), ColorComponent.R, reportHandle)
                'G', 'g' -> addComponent(it.toChar(), ColorComponent.G, reportHandle)
                'B', 'b' -> addComponent(it.toChar(), ColorComponent.B, reportHandle)
                else -> reportHandle(
                    "Unknown color component flag '${it.toChar()}'(${it.toString(16)}); " +
                            "allowed values are: [A, R, G, B]", false
                )
            }
        }
    }

private fun EnumSet<ColorComponent>.addComponent(
    ch: Char,
    c: ColorComponent,
    reportHandle: (String, err: Boolean) -> Unit
) {
    if (!this.add(c)) reportHandle("Duplicated color component '$ch'", false)
}

private fun validatePath(location: String) {
    if (!isValidLocation(location)) throw IllegalArgumentException("'$location' is not a valid location")
}

private fun isValidLocation(location: String): Boolean {
    for (c in location) {
        if (!isValidPathChar(c)) return false
    }
    return true
}

private fun isValidPathChar(c: Char): Boolean =
    c == '_' || c == '-' || c in 'a'..'z' || c in '0'..'9' || c == '/' || c == '.'

@Suppress("NOTHING_TO_INLINE")
private inline fun Int.toUIntChecked(): UInt =
    if (this < 0) throw RuntimeException("Negative value is not allowed") else toUInt()

@PublishedApi
internal fun Int.dim() = Dimension.of(this.toUIntChecked())

@PublishedApi
internal fun Int?.dim() = this?.dim() ?: Dimension.AUTO

@PublishedApi
internal fun String.toColorOrThrow() =
    ARGB.tryParse(this) ?: throw IllegalArgumentException("Cannot parse color '$this'")
