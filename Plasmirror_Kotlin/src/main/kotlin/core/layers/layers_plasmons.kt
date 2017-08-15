package core.layers

import core.State.wavelengthCurrent
import core.AlGaAsPermittivity.eps_AlGaAs
import core.Complex_
import core.EpsType
import core.toEnergy
import org.apache.commons.math3.complex.Complex.NaN
import java.lang.Math.sqrt

/**
 * Maxwell-Garnett effective medium layer
 */
/**
 * @param x            AlAs concentration
 * @param wPlasma      plasma frequency for NP, eV
 * @param gammaPlasma  damping constant, (h/2Pi) / tau, tau ~ 3E-15 sec
 * @param f            volume fraction of NP in SC
 */
class EffectiveMedium(d: Double,
                      val x: Double,
                      private val wPlasma: Double, private val gammaPlasma: Double,
                      private val f: Double, private val eps_inf: Double = 1.0,
                      val eps_type: EpsType) : SimpleLayer(d) {

    override var n = Complex_(NaN)
        get() = with(eps_eff(wavelengthCurrent)) {
            val n = sqrt((abs() + real) / 2)
            val k = sqrt((abs() - real) / 2)
            return Complex_(n, k)
        }

    /**
     * https://en.wikipedia.org/wiki/Effective_medium_approximations
     * @return Maxwell-Garnett eps_eff
     */
    private fun eps_eff(wavelength: Double): Complex_ {
        val f = Complex_(f)
        val eps_m = eps_m(wavelength)
        val eps_s = eps_AlGaAs(wavelength, x, eps_type)
        val numerator = f * 2.0 * (eps_m - eps_s) + eps_m + (eps_s * 2.0)
        val denominator = (eps_s * 2.0) + eps_m + (f * (eps_s - eps_m))
        return eps_s * (numerator / denominator)
    }

    /**
     * @return NP Drude permittivity
     */
    private fun eps_m(wavelength: Double): Complex_ {
        val w = Complex_(toEnergy(wavelength)) // eV
        val numerator = Complex_(wPlasma * wPlasma)
        val denominator = w * (w + Complex_(0.0, gammaPlasma))
        return Complex_(eps_inf) - (numerator / denominator)
    }
}

