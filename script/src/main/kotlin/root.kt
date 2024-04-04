package cnedclub.sad.script

import cnedclub.sad.canvas.*
import java.util.*

@Suppress("unused", "MemberVisibilityCanBePrivate")
class ConfigScriptRoot(
    private val canvasOperations: MutableMap<String, ImageGenEntry>,
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

    var defaultOptimizationType = OptimizationType.DefaultOptimization

    fun generate(path: String, canvasOp: CanvasOp, optimization: OptimizationType = defaultOptimizationType) =
        if (canvasOperations.putIfAbsent(path, ImageGenEntry(canvasOp, optimization)) == null) DependencyOp(path)
        else throw IllegalStateException("Entry for location '$path' already exists")

    fun image(path: String) = RawImageOp(path)

    fun fill(color: Color, width: Int? = null, height: Int? = null) =
        ColorFillOp(color, width.dim(), height.dim())

    fun output(path: String) = DependencyOp(path)

    fun region(
        source: CanvasOp,
        xOffset: Int,
        yOffset: Int,
        width: Int? = null,
        height: Int? = null
    ) = RegionOp(source, xOffset.toUIntChecked(), yOffset.toUIntChecked(), width.dim(), height.dim())

    fun tint(target: CanvasOp, color: Color) = ColorTintOp(target, color)

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
    ) = SimpleGradient(sortedMapOf<Float, Color>().also {
        builder(SimpleGradientBuilder(it))
    })

    inline fun transform(
        target: CanvasOp,
        outOfBoundsFill: TransformOp.OutOfBoundsFill = transparentFill,
        width: Int? = null,
        height: Int? = null,
        transform: MutableTransform.() -> Unit
    ) = TransformOp(target, width.dim(), height.dim(), Transform.identity().also(transform), outOfBoundsFill)

    fun rgb(rgb: Int) = Color((rgb.toLong() or 0xFF000000).toInt())
    fun rgb(r: Int, g: Int, b: Int) = Color(r, g, b)
    fun argb(argb: Long) = Color(argb.toInt())
    fun argb(a: Int, r: Int, g: Int, b: Int) = Color(a, r, g, b)

    fun <T : Any?> echo(t: T): T = t.also { reportHandle("$it", false) }
}

@Suppress("unused", "MemberVisibilityCanBePrivate")
class LayerBuilder(private val entries: MutableList<LayerOp.Entry>) {
    fun add(op: CanvasOp, xOffset: Int = 0, yOffset: Int = 0) {
        entries += LayerOp.Entry(op, xOffset, yOffset)
    }
}

@Suppress("unused", "MemberVisibilityCanBePrivate")
class SimpleGradientBuilder(private val elements: SortedMap<Float, Color>) {
    fun add(point: Float, color: Color) {
        if (point in 0.0..1.0) {
            if (elements.containsKey(point)) throw IllegalStateException("Duplicated gradient point at value $point")
            elements[point] = color
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

@Suppress("NOTHING_TO_INLINE")
private inline fun Int.toUIntChecked(): UInt =
    if (this < 0) throw RuntimeException("Negative value is not allowed") else toUInt()

@PublishedApi
internal fun Int.dim() = Dimension.of(this.toUIntChecked())

@PublishedApi
internal fun Int?.dim() = this?.dim() ?: Dimension.AUTO

@PublishedApi
internal val transparentFill = TransformOp.OutOfBoundsFill.Fill(Color.Transparent)
