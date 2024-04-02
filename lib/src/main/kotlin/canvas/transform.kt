package cnedclub.sad.canvas

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

sealed class Transform {
    abstract val m00: Float
    abstract val m01: Float
    abstract val m10: Float
    abstract val m11: Float

    val isIdentity: Boolean
        get() = m00 == 1f && m01 == 0f && m10 == 0f && m11 == 0f

    @OptIn(ExperimentalContracts::class)
    inline fun <T> transform(x: Float, y: Float, action: (x: Float, y: Float) -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }
        return action(m00 * x + m01 * y, m10 * x + m11 * y)
    }

    fun inverse(): MutableTransform? {
        val detInv = 1f / (m00 * m11 - m01 * m10)
        return if (!detInv.isFinite()) null
        else MutableTransform(m11 * detInv, -m01 * detInv, -m10 * detInv, m00 * detInv)
    }

    companion object {
        fun identity() = MutableTransform(1f, 0f, 0f, 1f)
    }
}

data class MutableTransform(
    override var m00: Float,
    override var m01: Float,
    override var m10: Float,
    override var m11: Float
) : Transform() {
    constructor(other: Transform) : this(other.m00, other.m01, other.m10, other.m11)

    fun scale(factor: Float) = scale(factor, factor)
    fun scale(x: Float, y: Float) = append(x, 0f, 0f, y)

    fun flipX() = append(-1f, 0f, 0f, 1f)
    fun flipY() = append(1f, 0f, 0f, -1f)

    fun rotate(degrees: Float) {
        val rad = degrees / 180f * PI.toFloat()
        val sinAngle = sin(rad)
        val cosAngle = cos(rad)
        append(cosAngle, -sinAngle, sinAngle, cosAngle)
    }

    fun shear(x: Float, y: Float) = append(1f, x, y, 1f)

    fun append(transform: Transform) =
        append(transform.m00, transform.m01, transform.m10, transform.m11)

    fun append(m00: Float, m01: Float, m10: Float, m11: Float) {
        val r00 = m00 * this.m00 + m01 * this.m10
        val r01 = m00 * this.m01 + m01 * this.m11
        val r10 = m10 * this.m00 + m11 * this.m10
        val r11 = m10 * this.m01 + m11 * this.m11
        this.m00 = r00
        this.m01 = r01
        this.m10 = r10
        this.m11 = r11
    }

    override fun toString() = "[$m00 $m01 $m10 $m11]"
}
