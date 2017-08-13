package core.layers

import core.State.wavelengthCurrent
import core.util.AlGaAsPermittivity.n_AlGaAs
import core.util.Cmplx
import core.util.Layer
import core.util.Mtrx
import core.util.cosThetaInLayer
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
            var phi = Cmplx(2.0 * PI * d / wavelengthCurrent) * n * cos

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
        get() = n_AlGaAs(wavelengthCurrent, x = 0.0)
}


/**
 * AlGaAs
 *
 * @param k refractiveIndex = (Re(refractiveIndex); Im(refractiveIndex) = k * Re(refractiveIndex)) Re(refractiveIndex) is from Adachi
 * @param x AlAs concentration
 */
class AlGaAs(d: Double,
             private val k: Double, val x: Double) : SimpleLayer(d) {
    override var n = Cmplx(NaN)
        get() = n_AlGaAs(wavelengthCurrent, x)
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