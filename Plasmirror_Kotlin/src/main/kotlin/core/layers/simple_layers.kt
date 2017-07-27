package core.layers

import core.State.wlCurrent
import core.util.*
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.complex.Complex.NaN
import java.lang.Math.PI

/**
 * Abstract layer without excitons
 */
abstract class SimpleLayer(d: Double) : Layer(d) {
    /**
     * @return transfer matrix for layer without excitons
     * * Polarization is unused
     */
    override val matrix: Mtrx
        get() {
            val cos = cosThetaInLayer(n)
            var phi = Cmplx(2.0 * PI * d / wlCurrent) * n * cos

            if (phi.imaginary < 0) {
                phi *= -1.0
            }

            val matrix = Mtrx()

            matrix[0, 0] = Cmplx((phi * Complex.I).exp())
            matrix[1, 1] = Cmplx((phi * Complex.I * -1.0).exp())
            matrix.setAntiDiagonal(Cmplx(Complex.ZERO))

            return matrix
        }
}


/**
 * GaAs
 * refractiveIndex is taken from the interpolation table for the given wavelength
 */
class GaAs(d: Double) : SimpleLayer(d) {
    override var n = Cmplx(NaN)
        get() {
            return n_GaAs(wlCurrent)!!
        }
}


/**
 * AlGaAs
 *
 * @param k refractiveIndex = (Re(refractiveIndex); Im(refractiveIndex) = k * Re(refractiveIndex)) Re(refractiveIndex) is from Adachi
 * @param x AlAs concentration
 */
class AlGaAs(d: Double,
             private val k: Double, private val x: Double) : SimpleLayer(d) {
    override var n = Cmplx(NaN)
        get() {
            val nReal = n_AlGaAs(wlCurrent, x)
            return Cmplx(nReal, k * nReal)
        }
}


/**
 * Layer with constant complex refractiveIndex
 *
 * @param const_n refractive index
 */
class ConstRefractiveIndexLayer(d: Double, private val const_n: Cmplx) : SimpleLayer(d) {
    override var n = Cmplx(NaN)
        get() = const_n
}