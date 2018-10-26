package core.layers

import core.Complex_
import core.Matrix_
import org.apache.commons.math3.complex.Complex
import java.lang.Math.PI
import core.State
import core.optics.*

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
    val alphaExt
        get() = Optics.extinctionCoefficient(State.wavelengthCurrent, n)

    /**
     * @return transfer matrix for layer without excitons
     * * Polarization is unused
     */
    val matrix
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
}


interface GaAsLayer : Layer {
    val epsType: EpsType

    override val n
        get() = Optics.toRefractiveIndex(AlGaAsMatrix.Permittivity.permittivity(State.wavelengthCurrent, 0.0, 0.0, epsType))
}


interface AlGaAsLayer : GaAsLayer {
    val k: Double
    val x: Double

    override val n
        get() = Optics.toRefractiveIndex(AlGaAsMatrix.Permittivity.permittivity(State.wavelengthCurrent, k, x, epsType))
}


open class GaAs(override val d: Double, override val epsType: EpsType) : GaAsLayer


/**
 * @param k for Adachi computation n = (Re(n); Im(n) = k * Re(n))
 */
open class AlGaAs(
        override val d: Double,
        override val k: Double,
        override val x: Double,
        override val epsType: EpsType
) : AlGaAsLayer


open class ConstRefractiveIndexLayer(
        override val d: Double,
        override val n: Complex_
) : Layer