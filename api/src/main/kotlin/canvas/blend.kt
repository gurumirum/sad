package cnedclub.sad.canvas

import kotlin.math.max
import kotlin.math.min

fun blend(
    src: ARGB,
    dst: ARGB,
    eq: BlendEquation,
    srcColor: BlendFunc,
    dstColor: BlendFunc,
    srcAlpha: BlendFunc,
    dstAlpha: BlendFunc,
): ARGB = ARGB(
    eq.equate(src, dst, srcAlpha, dstAlpha, ColorComponent.A),
    eq.equate(src, dst, srcColor, dstColor, ColorComponent.R),
    eq.equate(src, dst, srcColor, dstColor, ColorComponent.G),
    eq.equate(src, dst, srcColor, dstColor, ColorComponent.B)
)

enum class BlendEquation {
    Add,
    Subtract,
    ReverseSubtract,
    Min,
    Max;

    fun equate(src: ARGB, dst: ARGB, srcFunc: BlendFunc, dstFunc: BlendFunc, c: ColorComponent): Int = when (this) {
        Add -> (src[c] * srcFunc.factor(src, dst, c) + dst[c] * dstFunc.factor(src, dst, c)).toInt()
        Subtract -> (src[c] * srcFunc.factor(src, dst, c) - dst[c] * dstFunc.factor(src, dst, c)).toInt()
        ReverseSubtract -> (dst[c] * dstFunc.factor(src, dst, c) - src[c] * srcFunc.factor(src, dst, c)).toInt()
        Min -> min(src[c], dst[c])
        Max -> max(src[c], dst[c])
    }
}

enum class BlendFunc {
    Zero,
    One,
    SrcColor,
    OneMinusSrcColor,
    DstColor,
    OneMinusDstColor,
    SrcAlpha,
    OneMinusSrcAlpha,
    DstAlpha,
    OneMinusDstAlpha;

    fun factor(src: ARGB, dst: ARGB, c: ColorComponent): Float = when (this) {
        Zero -> 0f
        One -> 1f
        SrcColor -> src[c] / 255f
        OneMinusSrcColor -> 1 - src[c] / 255f
        DstColor -> dst[c] / 255f
        OneMinusDstColor -> 1 - dst[c] / 255f
        SrcAlpha -> src.a / 255f
        OneMinusSrcAlpha -> 1 - src.a / 255f
        DstAlpha -> dst.a / 255f
        OneMinusDstAlpha -> 1 - dst.a / 255f
    }
}
