package gurumirum.sad

import java.math.BigInteger
import java.security.MessageDigest

@JvmInline
value class Hash(private val value: BigInteger) {
    private constructor(data: ByteArray) : this(
        BigInteger(1, MessageDigest.getInstance("SHA-256").digest(data))
    )

    override fun toString(): String = this.value.toString(16).padStart(64, '0')

    companion object {
        fun of(data: IntArray, metadata: ByteArray? = null) =
            Hash(ByteArray(data.size * 4 + (metadata?.size ?: 0)) {
                if (it / 4 < data.size) (data[it / 4] shr it % 4 * 8).toByte()
                else metadata!![it - data.size * 4]
            })

        fun of(string: String) = Hash(string.toByteArray())

        fun parse(data: String) = Hash(BigInteger(data, 16))
    }
}
