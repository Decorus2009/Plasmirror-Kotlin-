package core.layers

import core.State.wavelengthCurrent
import core.util.*
import core.util.AlGaAsPermittivity.n_AlGaAs
import core.util.EpsType.ADACHI
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
class GaAs(d: Double, val eps_Type: EpsType) : SimpleLayer(d) {
    override var n = Cmplx(NaN)
        get() = n_AlGaAs(wavelengthCurrent, x = 0.0, eps_type = eps_Type)
}


/**
 * AlGaAs
 *
 * @param k n = (Re(n); Im(n) = k * Re(n)) Re(n) is from Adachi
 * @param x AlAs concentration
 */
class AlGaAs(d: Double, private val k: Double, val x: Double, val eps_type: EpsType) : SimpleLayer(d) {
    override var n = Cmplx(NaN)
        get() {
            if (eps_type == ADACHI) {
                val n_AlGaAs = n_AlGaAs(wavelengthCurrent, x, eps_type)
                return Cmplx(n_AlGaAs.real, n_AlGaAs.real * k)
            }
            return n_AlGaAs(wavelengthCurrent, x, eps_type)
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