package gurumirum.sad.canvas

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

sealed class Transform {
    abstract val m00: Float
    abstract val m01: Float
    abstract val m02: Float
    abstract val m10: Float
    abstract val m11: Float
    abstract val m12: Float
    abstract val m20: Float
    abstract val m21: Float
    abstract val m22: Float

    val isIdentity: Boolean
        get() = m00 == 1f && m01 == 0f && m02 == 0f &&
                m10 == 0f && m11 == 1f && m12 == 0f &&
                m20 == 0f && m21 == 0f && m22 == 1f

    @OptIn(ExperimentalContracts::class)
    inline fun <T> transform(x: Float, y: Float, action: (x: Float, y: Float) -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }
        val tx = m00 * x + m01 * y + m02
        val ty = m10 * x + m11 * y + m12
        val tz = m20 * x + m21 * y + m22
        return action(tx / tz, ty / tz)
    }

    fun inverse(): MutableTransform? {
        val detInv = 1f / (m00 * (m11 * m22 - m12 * m21) -
                m01 * (m10 * m22 - m12 * m20) +
                m02 * (m10 * m21 - m11 * m20))
        return if (!detInv.isFinite()) null
        else MutableTransform(
            (m11 * m22 - m21 * m12) * detInv,
            (m02 * m21 - m01 * m22) * detInv,
            (m01 * m12 - m02 * m11) * detInv,
            (m12 * m20 - m10 * m22) * detInv,
            (m00 * m22 - m02 * m20) * detInv,
            (m10 * m02 - m00 * m12) * detInv,
            (m10 * m21 - m20 * m11) * detInv,
            (m20 * m01 - m00 * m21) * detInv,
            (m00 * m11 - m10 * m01) * detInv
        )
    }

    companion object {
        fun identity() = MutableTransform(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
    }
}

@Suppress("unused", "MemberVisibilityCanBePrivate")
data class MutableTransform(
    override var m00: Float,
    override var m01: Float,
    override var m02: Float,
    override var m10: Float,
    override var m11: Float,
    override var m12: Float,
    override var m20: Float,
    override var m21: Float,
    override var m22: Float
) : Transform() {
    constructor(other: Transform) : this(
        other.m00, other.m01, other.m02,
        other.m10, other.m11, other.m12,
        other.m20, other.m21, other.m22
    )

    fun translate(x: Float, y: Float) = append(
        1f, 0f, x,
        0f, 1f, y,
        0f, 0f, 1f
    )

    fun scale(factor: Float) = scale(factor, factor)
    fun scale(x: Float, y: Float) = append(
        x, 0f, 0f,
        0f, y, 0f,
        0f, 0f, 1f
    )

    fun flipX() = append(
        -1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    )

    fun flipY() = append(
        1f, 0f, 0f,
        0f, -1f, 0f,
        0f, 0f, 1f
    )

    fun rotate(degrees: Float) {
        val rad = degrees / 180f * PI.toFloat()
        val sinAngle = sin(rad)
        val cosAngle = cos(rad)
        append(
            cosAngle, -sinAngle, 0f,
            sinAngle, cosAngle, 0f,
            0f, 0f, 1f
        )
    }

    fun shear(x: Float, y: Float) = append(
        1f, x, 0f,
        y, 1f, 0f,
        0f, 0f, 1f
    )

    fun append(transform: Transform) = append(
        transform.m00, transform.m01, transform.m02,
        transform.m10, transform.m11, transform.m12,
        transform.m20, transform.m21, transform.m22
    )

    fun append(
        m00: Float, m01: Float, m02: Float,
        m10: Float, m11: Float, m12: Float,
        m20: Float, m21: Float, m22: Float
    ) {
        val r00 = m00 * this.m00 + m01 * this.m10 + m02 * this.m20
        val r01 = m00 * this.m01 + m01 * this.m11 + m02 * this.m21
        val r02 = m00 * this.m02 + m01 * this.m12 + m02 * this.m22
        val r10 = m10 * this.m00 + m11 * this.m10 + m12 * this.m20
        val r11 = m10 * this.m01 + m11 * this.m11 + m12 * this.m21
        val r12 = m10 * this.m02 + m11 * this.m12 + m12 * this.m22
        val r20 = m20 * this.m00 + m21 * this.m10 + m22 * this.m20
        val r21 = m20 * this.m01 + m21 * this.m11 + m22 * this.m21
        val r22 = m20 * this.m02 + m21 * this.m12 + m22 * this.m22
        this.m00 = r00
        this.m01 = r01
        this.m02 = r02
        this.m10 = r10
        this.m11 = r11
        this.m12 = r12
        this.m20 = r20
        this.m21 = r21
        this.m22 = r22
    }

    override fun toString() = "[$m00 $m01 $m02, $m10 $m11 $m12, $m20 $m21 $m22]"
}
