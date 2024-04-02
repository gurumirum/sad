package cnedclub.sad.canvas

@JvmInline
value class Dimension private constructor(
    @PublishedApi internal val value: Long
) {
    val hasValue: Boolean
        get() = value >= 0

    fun or(fallback: Dimension): Dimension = if (this.hasValue) this else fallback

    fun orElse(fallback: UInt): UInt =
        if (this.hasValue) value.toUInt() else fallback

    inline fun orElse(fallback: () -> UInt): UInt =
        if (this.hasValue) value.toUInt() else fallback()

    inline fun orThrow(exception: () -> Throwable = { IllegalStateException("Cannot resolve dimension") }): UInt =
        if (this.hasValue) value.toUInt() else throw exception()

    inline fun mapValue(transform: (UInt) -> UInt): Dimension =
        if (this.hasValue) of(transform(value.toUInt())) else this

    inline fun ifValid(action: (UInt) -> Unit) {
        if (this.hasValue) action(value.toUInt())
    }

    companion object {
        val AUTO = Dimension(-1)

        fun of(value: UInt) = Dimension(value.toLong())
        fun of(value: UInt?) = if(value!=null) Dimension(value.toLong()) else AUTO
    }
}
