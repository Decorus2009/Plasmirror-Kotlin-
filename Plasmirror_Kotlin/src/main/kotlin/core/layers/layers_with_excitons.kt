package core.layers

import core.State.polarization
import core.State.wavelengthCurrent
import core.util.*
import core.util.AlGaAsPermittivity.n_AlGaAs
import core.util.EpsType.ADACHI
import core.util.EpsType.GAUSS
import core.util.Polarization.P
import org.apache.commons.math3.complex.Complex.I
import org.apache.commons.math3.complex.Complex.NaN
import java.lang.Math.PI

/**
 * Abstract layer with excitons
 *
 * @param w0     exciton resonance frequency
 * @param gamma0 exciton radiative decay
 * @param gamma  exciton non-radiative decay
 */
abstract class LayerExciton(d: Double,
                            protected var w0: Double,
                            protected var gamma0: Double,
                            protected var gamma: Double) : Layer(d) {
    override val matrix: Mtrx
        get() {
            val cos = cosThetaInLayer(n)
            val phi = Cmplx(2.0 * PI * d / wavelengthCurrent) * n * cos

            // TODO проверить поляризацию (в VisMirror (GaAs) было S)
            val gamma0e = if (polarization === P) gamma0 * cos.real else gamma0 * (cos.pow(-1.0)).real
            val S = Cmplx(gamma0e) / Cmplx(toEnergy(wavelengthCurrent) - w0, gamma)

            val matrix = Mtrx()

            matrix[0, 0] = Cmplx((phi * I).exp()) * Cmplx(1.0 + S.imaginary, -S.real)
            matrix[0, 1] = Cmplx(S.imaginary, -S.real)
            matrix[1, 0] = Cmplx(-S.imaginary, S.real)
            matrix[1, 1] = Cmplx((phi * I * -1.0).exp()) * Cmplx(1.0 - S.imaginary, S.real)

            return matrix
        }
}

/**
 * GaAs with excitons
 * refractiveIndex is taken from the interpolation table for the given wavelength
 */
class GaAsExciton(d: Double, w0: Double, gamma0: Double, gamma: Double,
                  val eps_type: EpsType) : LayerExciton(d, w0, gamma0, gamma) {
    override var n = Cmplx(NaN)
        get() = n_AlGaAs(wavelengthCurrent, x = 0.0, eps_type = eps_type)
}


/**
 * AlGaAs with excitons
 *
 * @param k n = (Re(n); Im(n) = k * Re(n)) Re(n) is from Adachi
 * @param x AlAs concentration
 */
class AlGaAsExciton(d: Double,
                    private val k: Double, val x: Double,
                    w0: Double, gamma0: Double, gamma: Double,
                    val eps_type: EpsType) : LayerExciton(d, w0, gamma0, gamma) {
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
 * Layer with constant complex refractiveIndex with excitons
 *
 * @param const_n refractive index
 */
class ConstRefractiveIndexLayerExciton(d: Double,
                                       private val const_n: Cmplx,
                                       w0: Double, gamma0: Double, gamma: Double) : LayerExciton(d, w0, gamma0, gamma) {
    override var n = Cmplx(NaN)
        get() = const_n
}


