package core.layers

import core.*
import core.AlGaAsPermittivity.n_AlGaAs
import core.AlGaAsPermittivity.n_to_eps
import core.Complex_.Companion.I
import core.Complex_.Companion.NaN
import core.Complex_.Companion.ONE
import core.EpsType.ADACHI
import core.Polarization.P
import core.Polarization.S
import core.State.polarization
import core.State.wavelengthCurrent
import java.lang.Math.*

/**
 */
/**
 * @param x            AlAs concentration
 * @param w_plasma      plasma frequency for NP, eV
 * @param gamma_plasma  damping constant, (h/2Pi) / tau, tau ~ 3E-15 sec
 * @param f            volume fraction of NP in SC
 */
class EffectiveMedium(d: Double,
                      private val k: Double,
                      private val x: Double,
                      private val w_plasma: Double, private val gamma_plasma: Double,
                      private val f: Double, private val eps_inf: Double,
                      val eps_type: EpsType) : SimpleLayer(d) {
    override var n = Complex_(NaN)
        get() = with(eps_eff(wavelengthCurrent)) {
            val n = sqrt((abs() + real) / 2)
            val k = sqrt((abs() - real) / 2)
            return Complex_(n, k)
        }

    override fun parameters() = listOf(d, k, x, w_plasma, gamma_plasma, f, eps_inf)


    /**
     * https://en.wikipedia.oxrg/wiki/Effective_medium_approximations
     * @return Maxwell-Garnett eps_eff
     */
    private fun eps_eff(wavelength: Double): Complex_ {
        val f = Complex_(f)
        val eps_metal = eps_metal(wavelength)
        val n_semiconductor: Complex_ =
                when (eps_type) {
                    ADACHI -> with(n_AlGaAs(wavelength, x, eps_type)) { Complex_(real, real * k) }
                    else -> n_AlGaAs(wavelength, x, eps_type)
                }
        val eps_semiconductor = n_to_eps(n_semiconductor)

        val numerator = f * 2.0 * (eps_metal - eps_semiconductor) + eps_metal + (eps_semiconductor * 2.0)
        val denominator = (eps_semiconductor * 2.0) + eps_metal + (f * (eps_semiconductor - eps_metal))
        return eps_semiconductor * (numerator / denominator)
    }

    /**
     * Drude permittivity
     */
    private fun eps_metal(wavelength: Double): Complex_ {
        val w = Complex_(toEnergy(wavelength)) // eV
        val numerator = Complex_(w_plasma * w_plasma)
        val denominator = w * (w + Complex_(0.0, gamma_plasma))
        return Complex_(eps_inf) - (numerator / denominator)
    }
}

class NanoparticlesLayer(d: Double,
                         var k: Double,
                         var x: Double,
                         var latticeFactor: Double,
                         var w_plasma: Double, var gamma_plasma: Double,
                         var eps_inf: Double = 1.0,
                         val eps_type: EpsType) : SimpleLayer(d) {
    override var n = NaN
        get() {
            if (eps_type == ADACHI) {
                val n_AlGaAs = n_AlGaAs(wavelengthCurrent, x, eps_type)
                return Complex_(n_AlGaAs.real, n_AlGaAs.real * k)
            }
            return n_AlGaAs(wavelengthCurrent, x, eps_type)
        }

    override val matrix: Matrix_
        get() = Matrix_().apply {
            with(r_t(wavelengthCurrent)) {
                val r = first
                val t = second
                this@apply[0, 0] = (t * t - r * r) / t
                this@apply[0, 1] = r / t
                this@apply[1, 0] = -r / t
                this@apply[1, 1] = ONE / t
            }
        }

    override fun parameters() = listOf(d, k, x, latticeFactor, w_plasma, gamma_plasma, eps_inf)

    private val R = d / 2.0
//    private val a = 2 * R / sqrt(0.3)
    private val a = latticeFactor * R
    private val U0 = 9.03 / (a * a * a)
    private val cos
        get() = cosThetaInLayer(n)
    private val sin
        get() = Complex_((ONE - cos * cos).sqrt())


    fun r_t(wavelength: Double): Pair<Complex_, Complex_> {
        val alpha_parallel = alpha_parallel(wavelength)
        val alpha_orthogonal = alpha_orthogonal(wavelength)

        /* ***************************************************************************** */
//        println("$wavelength ${alpha_parallel.real} ${alpha_parallel.imaginary} ${alpha_orthogonal.real} ${alpha_orthogonal.imaginary}")
        /* ***************************************************************************** */

        val A = A(wavelength)
        val B = B(wavelength)
        val theta = Complex_(cos.acos())

        val common1 = cos * cos * alpha_parallel
        val common2 = sin * sin * alpha_orthogonal
        val common3 = ONE + B * (alpha_orthogonal - alpha_parallel)
        val common4 = A * B * alpha_parallel * alpha_orthogonal * ((theta * I * 2.0).exp())

        val r = when (polarization) {
            S -> (-A * common1) / (ONE - B * alpha_parallel - A * common1)
            P -> (-A * (common1 - common2) - common4) / (common3 - A * (common1 + common2) - common4)
        }
        val t = when (polarization) {
            S -> (ONE - B * alpha_parallel) / (ONE - B * alpha_parallel - A * common1)
            P -> common3 / (common3 - A * (common1 + common2) - common4)
        }
        return r to t
    }

    private fun alpha(wavelength: Double): Complex_ {
        val eps_metal = eps_metal(wavelength)
        val eps_semiconductor = eps_semiconductor(wavelength)
        return (eps_metal - eps_semiconductor) / (eps_metal + eps_semiconductor * 2.0) * pow(R, 3.0)
    }

    private fun alpha_parallel(wavelength: Double): Complex_ = with(alpha(wavelength)) {
        return this / (ONE - this * 0.5 * U0)
    }

    private fun alpha_orthogonal(wavelength: Double): Complex_ = with(alpha(wavelength)) {
        return this / (ONE + this * U0)
    }

    private fun A(wavelength: Double) = Complex_(pow(2 * PI / a, 2.0) / wavelength) * I / cos

    private fun B(wavelength: Double) = Complex_(pow(2 * PI / a, 2.0) / wavelength) * sin

    /**
     * Drude permittivity
     */
    private fun eps_metal(wavelength: Double): Complex_ {
        val w = Complex_(toEnergy(wavelength)) // eV
        val numerator = Complex_(w_plasma * w_plasma)
        val denominator = w * (w + Complex_(0.0, gamma_plasma))
        return Complex_(eps_inf) - (numerator / denominator)
    }

    /**
     * Semiconductor matrix permittivity
     */
    private fun eps_semiconductor(wavelength: Double): Complex_ {
        val n_semiconductor: Complex_ =
                when (eps_type) {
                    ADACHI -> with(n_AlGaAs(wavelength, x, eps_type)) { Complex_(real, real * k) }
                    else -> n_AlGaAs(wavelength, x, eps_type)
                }
        return n_to_eps(n_semiconductor)
    }
}

