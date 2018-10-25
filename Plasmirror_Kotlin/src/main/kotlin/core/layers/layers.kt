package core.layers

import core.optics.AlGaAsPermittivity.AlGaAsRefractiveIndex
import core.Complex_
import core.optics.EpsType
import core.Matrix_
import core.optics.cosThetaInLayer
import org.apache.commons.math3.complex.Complex
import java.lang.Math.PI
import core.State

/**
 * Abstract layer without excitons
 *
 * @property d thickness
 * @property n refractive index
 * @property matrix transfer matrix
 */
interface Layer {
    val d: Double
    val n: Complex_
    /**
     * @return transfer matrix for layer without excitons
     * * Polarization is unused
     */
    val matrix: Matrix_
        get() = Matrix_().apply {
            val cos = cosThetaInLayer(n)
            var phi = Complex_(2.0 * PI * d / State.wavelengthCurrent) * n * cos
            if (phi.imaginary < 0) {
                phi *= -1.0
            }
            this[0, 0] = Complex_((phi * Complex.I).exp())
            this[1, 1] = Complex_((phi * Complex.I * -1.0).exp())
            setAntiDiagonal(Complex_(Complex.ZERO))
        }

    fun parameters(): List<Any>
}


interface GaAsLayer : Layer {
    val epsType: EpsType

    override val n: Complex_
        get() = AlGaAsRefractiveIndex(State.wavelengthCurrent, 0.0, 0.0, epsType)

    override fun parameters() = listOf(d)
}


interface AlGaAsLayer : GaAsLayer {
    val k: Double
    val x: Double

    override val n: Complex_
        get() = AlGaAsRefractiveIndex(State.wavelengthCurrent, k, x, epsType)

    override fun parameters() = listOf(d, k, x)
}


open class GaAs(override val d: Double, override val epsType: EpsType) : GaAsLayer


/**
 * @param k for Adachi computation n = (Re(n); Im(n) = k * Re(n))
 */
open class AlGaAs(override val d: Double,
                  override val k: Double,
                  override val x: Double,
                  override val epsType: EpsType) : AlGaAsLayer


open class ConstRefractiveIndexLayer(override val d: Double, override val n: Complex_) : Layer {
    override fun parameters() = listOf(d, n)
}