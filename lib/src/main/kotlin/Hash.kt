package gurumirum.sad

import java.math.BigInteger
import java.security.MessageDigest

@JvmInline
value class Hash(private val value: BigInteger) {
    constructor(data: IntArray, metadata: ByteArray? = null) : this(ByteArray(data.size * 4 + (metadata?.size ?: 0)) {
        if(it / 4 < data.size) (data[it / 4] shr it % 4 * 8).toByte()
        else metadata!![it - data.size * 4]
    })
    constructor(data: ByteArray) : this(BigInteger(1, MessageDigest.getInstance("SHA-256").digest(data)))
    constructor(data: String) : this(BigInteger(data, 16))

    override fun toString(): String = value.toString(16).padStart(64, '0')
}
