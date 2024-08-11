package gurumirum.sad.script

enum class OptimizationType {
    DefaultOptimization,
    ZopfliOptimization,
    NoOptimization;

    fun metadata(): ByteArray = byteArrayOf(this.ordinal.toByte())
}
