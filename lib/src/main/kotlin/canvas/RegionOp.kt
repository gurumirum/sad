package gurumirum.sad.canvas

class RegionOp(
    private val source: CanvasOp,
    private val xOffset: UInt,
    private val yOffset: UInt,
    private val width: Dimension,
    private val height: Dimension,
) : CanvasOp {
    override suspend fun run(
        ctx: CanvasOp.Context,
        parentWidth: Dimension,
        parentHeight: Dimension
    ): Result<Canvas> {
        val width = this.width.or(parentWidth)
        val height = this.height.or(parentHeight)

        return source.run(ctx, width.mapValue { it + this.xOffset }, height.mapValue { it + this.yOffset })
            .mapResult { canvas ->
                checkDimension(width, height, canvas.width, canvas.height) {
                    Result.success(
                        canvas.copySubArea(
                            width.orElse(canvas.width - this.xOffset),
                            height.orElse(canvas.height - this.yOffset),
                            this.xOffset,
                            this.yOffset
                        )
                    )
                }
            }
    }

    private inline fun checkDimension(
        width: Dimension,
        height: Dimension,
        imageWidth: UInt,
        imageHeight: UInt,
        runIfValid: () -> Result<Canvas>
    ): Result<Canvas> {
        width.ifValid { w ->
            height.ifValid { h ->
                if (xOffset + w > imageWidth || yOffset + h > imageHeight) {
                    return fail(
                        "Region outside image boundary, region: [$xOffset, $yOffset, $width, $height], " +
                                "image dimension: $imageWidth, $imageHeight"
                    )
                }
            }
        }

        return runIfValid()
    }

    override fun toString() = "RegionOp(src='$source', x=$xOffset, y=$yOffset, width=$width, height=$height)"
}
