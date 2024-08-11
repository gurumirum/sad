package gurumirum.sad

import java.math.BigInteger
import java.security.MessageDigest

@JvmInline
value class Hash(private val value: BigInteger) {
    constructor(data: IntArray) : this(ByteArray(data.size * 4) { (data[it / 4] shr it % 4 * 8).toByte() })
    constructor(data: ByteArray) : this(BigInteger(1, MessageDigest.getInstance("SHA-256").digest(data)))
    constructor(data: String) : this(BigInteger(data, 16))

    override fun toString(): String = value.toString(16).padStart(64, '0')
}
