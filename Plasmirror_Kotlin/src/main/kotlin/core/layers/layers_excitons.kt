package core.layers

import core.*
import core.State.polarization
import core.State.wavelengthCurrent
import core.AlGaAsPermittivity.n_AlGaAs
import core.EpsType.ADACHI
import core.Polarization.P
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
    override val matrix: Matrix_
        get() = Matrix_().apply {
            val cos = cosThetaInLayer(n)
            /* TODO проверить поляризацию (в VisMirror (GaAs) было S) */
            val gamma0e = when (polarization) {
                P -> gamma0 * cos.real
                else -> gamma0 * (cos.pow(-1.0)).real
            }
            val phi = Complex_(2.0 * PI * d / wavelengthCurrent) * n * cos
            val S = Complex_(gamma0e) / Complex_(toEnergy(wavelengthCurrent) - w0, gamma)

            this[0, 0] = Complex_((phi * I).exp()) * Complex_(1.0 + S.imaginary, -S.real)
            this[0, 1] = Complex_(S.imaginary, -S.real)
            this[1, 0] = Complex_(-S.imaginary, S.real)
            this[1, 1] = Complex_((phi * I * -1.0).exp()) * Complex_(1.0 - S.imaginary, S.real)
        }
}

/**
 * GaAs with excitons
 * refractiveIndex is taken from the interpolation table for the given wavelength
 */
class GaAsExciton(d: Double, w0: Double, gamma0: Double, gamma: Double,
                  val eps_type: EpsType) : LayerExciton(d, w0, gamma0, gamma) {
    override var n = Complex_(NaN)
        get() = n_AlGaAs(wavelengthCurrent, x = 0.0, eps_type = eps_type)

    override fun parameters() = listOf(d, w0, gamma0, gamma)
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
    override var n = Complex_(NaN)
        get() {
            if (eps_type == ADACHI) {
                val n_AlGaAs = n_AlGaAs(wavelengthCurrent, x, eps_type)
                return Complex_(n_AlGaAs.real, n_AlGaAs.real * k)
            }
            return n_AlGaAs(wavelengthCurrent, x, eps_type)
        }

    override fun parameters() = listOf(d, k, x, w0, gamma0, gamma)
}


/**
 * Layer with constant complex refractiveIndex with excitons
 *
 * @param const_n refractive index
 */
class ConstRefractiveIndexLayerExciton(d: Double,
                                       private val const_n: Complex_,
                                       w0: Double, gamma0: Double, gamma: Double) : LayerExciton(d, w0, gamma0, gamma) {
    override var n = Complex_(NaN)
        get() = const_n

    override fun parameters() = listOf(d, const_n, w0, gamma0, gamma)
}


