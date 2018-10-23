package core

import core.Complex_.Companion.ONE
import core.Complex_.Companion.ZERO
import core.EpsType.*
import core.State.angle
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import java.lang.Math.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


enum class Medium { AIR, GAAS_ADACHI, GAAS_GAUSS, GAAS_GAUSS_ADACHI, CUSTOM }

enum class Polarization { S, P }

enum class Regime { REFLECTANCE, TRANSMITTANCE, ABSORBANCE, PERMITTIVITY, REFRACTIVE_INDEX }

enum class EpsType { ADACHI, GAUSS, GAUSS_ADACHI }


fun toEnergy(wavelength: Double) = 1239.8 / wavelength

fun cosThetaIncident() = Complex_(cos(angle * PI / 180.0))

/**
 *  Snell law
 */
fun cosThetaInLayer(n2: Complex_): Complex_ {

    val n1 = State.leftMediumLayer.n

    /**
     * !!!!!!!!!!!!!!!!!!!!!!!!!!
     * State.mirror is not initialized yet
     */
    //    val n1 = State.mirror.leftMediumLayer.n

    val cos1 = cosThetaIncident()
    val sin1_sq = ONE - (cos1 * cos1)
    val sin2_sq = sin1_sq * ((n1 / n2).pow(2.0))

    return Complex_((ONE - sin2_sq).sqrt())
}

fun Double.round(): Double {
    val precision = 7.0
    val power = pow(10.0, precision).toInt()
    return Math.floor((this + 1E-8) * power) / power
}

object SbTabulatedPermittivity {

    private val functions: Pair<PolynomialSplineFunction, PolynomialSplineFunction>
    private val path = Paths.get("data/inner/state_parameters/eps_Sb_Cardona_Adachi.txt")
    private val wavelengths = mutableListOf<Double>()
    private val epsSb = mutableListOf<Complex_>()

    init {
        println("Init in SbTabulatedPermittivity")
        read()
        functions = interpolate()
    }

    fun get(wavelength: Double) = with(functions) {
        if (wavelengths.isEmpty() or epsSb.isEmpty()) {
            throw IllegalStateException("Empty array of Sb permittivity")
        }

        val minWavelength = wavelengths[0]
        val maxWavelength = wavelengths[wavelengths.size - 1]
        val actualWavelength =
                if (wavelength < minWavelength) minWavelength
                else if (wavelength > maxWavelength) maxWavelength
                else wavelength

        Complex_(first.value(actualWavelength), second.value(actualWavelength))
    }

    private fun read() = Files.lines(path).forEach {
        with(Scanner(it)) {
            wavelengths += nextDouble()
            epsSb += Complex_(nextDouble(), nextDouble())
        }
    }

    private fun interpolate() = Interpolator.interpolateComplex(wavelengths, epsSb)
}

/**
 * Computation of the AlGaAs permittivity using full Gaussian approach from paper
 * J. Appl. Phys., 86, 1, pp.445 (1999). Modeling the optical constants of AlxGa1-xAs alloys.
 * and the Adachi'85 model
 */
object AlGaAsPermittivity {

    val GaussAdachiIntersections = mutableMapOf<Double, Double>()

    fun epsAlGaAs(wavelength: Double, k: Double, x: Double, epsType: EpsType) =
            eps_and_n_AlGaAs(wavelength, k, x, epsType).first

    fun nAlGaAs(wavelength: Double, k: Double, x: Double, epsType: EpsType) =
            eps_and_n_AlGaAs(wavelength, k, x, epsType).second

    /**
     * If w < intersection energy, returns eps and n computed by the Adachi'85 approximation
     * (with imaginary part computed by the Gauss approximation)
     * Else returns permittivity computed using the Gauss approximation
     */
    private fun eps_and_n_AlGaAs(wavelength: Double, k: Double, x: Double, epsType: EpsType): Pair<Complex_, Complex_> {
        val eps = when (epsType) {
            ADACHI -> epsAdachi(toEnergy(wavelength), x)
            GAUSS -> epsGauss(toEnergy(wavelength), x)
            GAUSS_ADACHI -> epsGaussAdachi(toEnergy(wavelength), x)
        }

        val n = when (epsType) {
            ADACHI -> {
                with(eps_to_n(eps)) {
                    Complex_(real, real * k)
                }
            }
            else -> eps_to_n(eps)
        }

        return eps to n
    }

    /**
     * Permittivity (Adachi)
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     */
    private fun epsAdachi(w: Double, x: Double): Complex_ {
        var w_ = w
        val Eg = 1.425 + 1.155 * x + 0.37 * x * x
        /* nonrecursive */
        if (w_ > Eg) {
            w_ = Eg
        }
        val delta = 0.34 - 0.04 * x // eV
        val A = 6.3 + 19.0 * x
        val B = 9.4 - 10.2 * x
        val hi = w_ / Eg
        val hi_so = w_ / (Eg + delta)
        val f: (Double) -> Double = { (2.0 - sqrt(1 + it) - sqrt(1 - it)) / (it * it) }
        return Complex_(A * (f.invoke(hi) + 0.5 * pow(Eg / (Eg + delta), 1.5) * f.invoke(hi_so)) + B)
    }
    /** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * Permittivity (Gauss)
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     */
    private fun epsGauss(w: Double, x: Double) = eps_inf(x) + eps_1(w, x) + eps_2(w, x) + eps_3(w, x) + eps_4(w, x)

    private fun nGauss(w: Double, x: Double) = eps_to_n(epsGauss(w, x))
    /** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * Permittivity (Gauss-Adachi)
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     */
    private fun epsGaussAdachi(w: Double, x: Double): Complex_ {
        if (GaussAdachiIntersections[x] == null) {
            findIntersection(x)
        }
        return if (w < GaussAdachiIntersections[x]!!) {
            Complex_(epsAdachi(w, x).real, epsGauss(w, x).imaginary)
        } else {
            epsGauss(w, x)
        }
    }

    private fun nGaussAdachi(w: Double, x: Double) = eps_to_n(epsGaussAdachi(w, x))
    /** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * Finds intersection point for both approaches
     * using appropriate small energy range (including E0) and a fixed energy precision
     *
     * For Re(eps):
     * Adachi describes experimental data for real part (only) of permittivity better than full approach using Gaussian broadening below E0.
     * Lorentz broadening is even worse.
     * At the very critical point E0 Adachi approach is not applicable.
     * The idea is to staple to curves for permittivity below E0, Adachi and Gauss, at the point of their intersection (eV).
     * It was found that the intersection point is located within the energy range (1.4 : 1.8) eV for x[0.0 : 0.5]
     * The intersection point is always lower than E0. But at the much lower energies there are another intersections.
     * We don't need to consider them. There will be only Adachi, no Gauss.
     */
    private fun findIntersection(x: Double) {
        println("Finding intersection for $x")
        val w = arrayListOf<Double>()
        val epsGauss = arrayListOf<Complex_>()
        val nGauss = arrayListOf<Complex_>()
        val epsAdachi = arrayListOf<Complex_>()
        val nAdachi = arrayListOf<Complex_>()

        /* compute in range [from; to] */
        val from = 1.4
        val to = 1.8
        val step = 0.001
        var w_ = from
        while (w_ <= to) {
            w_ = w_.round() // 12.000000000001 -> 12.0 && 13.99999999999 -> 14.0
            w.add(w_)
            epsGauss.add(epsGauss(w_, x))
            nGauss.add(nGauss(w_, x))
            epsAdachi.add(epsAdachi(w_, x))
            nAdachi.add(eps_to_n(epsAdachi(w_, x)))
            w_ += step
        }
        /**
        Class to keep difference of nGauss.real and nAdachi at w
         */
        data class Diff(val w: Double, val diff: Double)

        /**
        Energy range within which the only one closest to the E0 intersection is found
         */
        val upperBound = E0(x)
        val nGaussReal = w.indices.map { nGauss[it].real }
        val nAdachiReal = w.indices.map { nAdachi[it].real }
        GaussAdachiIntersections.putIfAbsent(x, w.indices
                .map { Diff(w[it], diff = abs(nGaussReal[it] - nAdachiReal[it])) }
                .filter { it.w > 1.4 && it.w < upperBound }.minBy { it.diff }!!.w)
    }

    fun eps_to_n(eps: Complex_): Complex_ {
        val n = sqrt((eps.abs() + eps.real) / 2.0)
        val k = sqrt((eps.abs() - eps.real) / 2.0)
        return Complex_(n, k)
    }

    fun n_to_eps(n: Complex_) = Complex_(n * n)


    /**
     * Permittivity (Gauss) details
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     */
    /**
     * AlGaAs Gauss permittivity components. Look at the paper
     */
    private fun eps_1(w: Double, x: Double): Complex_ {
        val A = A(x)
        val E0 = E0(x)
        val E0_plus_delta0 = E0_plus_delta0(x)
        var Gamma0_Gauss = Gamma0_Gauss(w, x)
        /*
        Precision is used to prevent errors when Gamma0_Gauss = 0.0.
        Otherwise complex roots are calculated incorrectly and eps_1 provides sharp sign change
         */
        val precision = 1E-6
        if (Gamma0_Gauss < precision) {
            Gamma0_Gauss = precision
        }
        val hi0 = Complex_(w, Gamma0_Gauss) / E0
        val hi0S = Complex_(w, Gamma0_Gauss) / E0_plus_delta0
        val d1 = A * pow(E0, -1.5)
        val d2 = 0.5 * pow(E0 / E0_plus_delta0, 1.5)
        val f = { y: Complex_ ->
            val c1 = (ONE + y).sqrt()
            val c2 = (ONE - y).sqrt()
            (Complex_(2.0) - c1 - c2) / (y.pow(2.0))
        }
        return (f.invoke(hi0) + (f.invoke(hi0S) * d2)) * d1
    }

    private fun eps_2(w: Double, x: Double): Complex_ {
        val B1 = B1(x)
        val B1S = B1S(x)
        val E1 = E1(x)
        val E1_plus_delta1 = E1_plus_delta1(x)
        val Gamma1_Gauss = Gamma1_Gauss(w, x)
        val hi1_sq = (Complex_(w, Gamma1_Gauss) / E1).pow(2.0)
        val hi1S_sq = (Complex_(w, Gamma1_Gauss) / E1_plus_delta1).pow(2.0)
        val c1 = (B1 / hi1_sq) * ((ONE - hi1_sq).log()) * -1.0
        val c2 = (B1S / hi1S_sq) * ((ONE - hi1S_sq).log()) * -1.0
        return c1 + c2
    }

    private fun eps_3(w: Double, x: Double): Complex_ {
        val B1X = B1X(x)
        val B2X = B2X(x)
        val Gamma1_Gauss = Gamma1_Gauss(w, x)
        val E1 = E1(x)
        val E1_plus_delta1 = E1_plus_delta1(x)
        var accumulator = ZERO
        var summand = ONE
        val precision = 1E-4
        var n = 1
        /*
          Check the paper. The summation of the excitonic terms
          is performed until the contribution of the next term is less than 10^-4 (precision)
         */
        while (summand.abs() >= precision) {

            val c1 = B1X / Complex_(E1 - w, -Gamma1_Gauss)
            val c2 = B2X / Complex_(E1_plus_delta1 - w, -Gamma1_Gauss)

            summand = (c1 + c2) / pow((2.0 * n - 1.0), 3.0)
            accumulator += summand
            n++
        }
        return accumulator
    }

    private fun eps_4(w: Double, x: Double): Complex_ {
        val f = doubleArrayOf(f2(x), f3(x), f4(x))
        val E = doubleArrayOf(E2(x), E3(x), E4(x))
        val Gamma_Gauss = doubleArrayOf(Gamma2_Gauss(w, x), Gamma3_Gauss(w, x), Gamma4_Gauss(w, x))
        var accumulator = ZERO
        for (i in 0..2) {
            val numerator = Complex_(f[i] * f[i])
            val denominator = Complex_(E[i] * E[i] - w * w, -w * Gamma_Gauss[i])
            val summand = numerator / denominator
            accumulator += summand
        }
        return accumulator
    }


    /**
     * Table I
     * E0, E0 + delta0, E1, E1 + delta1 dependent on x.
     *
     * E0 and delta0 are replaced by linear dependencies on x using correct values at x = 0.0
     * look at http://www.ioffe.ru/SVA/NSM/Semicond/AlGaAs/bandstr.html
     *
     * For cubic dependencies for E0 and E0 + delta0 look at the paper
     */
    private fun E0(x: Double) = 1.424 + 1.155 * x + 0.37 * x * x

    private fun E0_plus_delta0(x: Double) = E0(x) + 0.34 - 0.04 * x

    private fun E1(x: Double) = Ei(x, Ei0 = 2.926, Ei1_minus_Ei0 = 0.962, c0 = -0.2124, c1 = -0.7850)

    private fun E1_plus_delta1(x: Double) = Ei(x, Ei0 = 3.170, Ei1_minus_Ei0 = 0.917, c0 = -0.0734, c1 = -0.9393)

    private fun Ei(x: Double, Ei0: Double, Ei1_minus_Ei0: Double, c0: Double, c1: Double) = Ei0 + Ei1_minus_Ei0 * x + (c0 + c1 * x) * x * (1 - x)

    /**
     * Table II
     * Cubic dependencies for model parameter values.
     */
    private fun parameterCubic(x: Double, a: DoubleArray) = a[0] * (1 - x) + a[1] * x + (a[2] + a[3] * x) * x * (1 - x)

    /**
     * Lorentz forms modified by the exponential decay (Gaussian-like forms)
     */
    private fun Gamma0_Gauss(w: Double, x: Double) = with(Gamma0(x)) {
        this * exp(-alpha0(x) * pow((w - E0(x)) / this, 2.0))
    }

    private fun Gamma1_Gauss(w: Double, x: Double) = with(Gamma1(x)) {
        this * exp(-alpha1(x) * pow((w - E1(x)) / this, 2.0))
    }

    private fun Gamma2_Gauss(w: Double, x: Double) = with(Gamma2(x)) {
        this * exp(-alpha2(x) * pow((w - E2(x)) / this, 2.0))
    }

    private fun Gamma3_Gauss(w: Double, x: Double) = with(Gamma3(x)) {
        this * exp(-alpha3(x) * pow((w - E3(x)) / this, 2.0))
    }

    private fun Gamma4_Gauss(w: Double, x: Double) = with(Gamma4(x)) {
        this * exp(-alpha4(x) * pow((w - E4(x)) / this, 2.0))
    }


    private fun eps_inf(x: Double) = Complex_(parameterCubic(x, doubleArrayOf(1.347, 0.02, -0.568, 4.210)))
    private fun A(x: Double) = parameterCubic(x, doubleArrayOf(3.06, 14.210, -0.398, 4.763))
    private fun Gamma0(x: Double) = parameterCubic(x, doubleArrayOf(0.0001, 0.0107, -0.0187, 0.3057))
    private fun alpha0(x: Double) = parameterCubic(x, doubleArrayOf(3.960, 1.617, 3.974, -5.413))
    private fun B1(x: Double) = Complex_(parameterCubic(x, doubleArrayOf(6.099, 4.381, -4.718, -2.510)))
    private fun B1S(x: Double) = Complex_(parameterCubic(x, doubleArrayOf(0.001, 0.103, 4.447, 0.208)))
    private fun B1X(x: Double) = Complex_(parameterCubic(x, doubleArrayOf(1.185, 0.639, 0.436, 0.426)))
    private fun B2X(x: Double) = Complex_(parameterCubic(x, doubleArrayOf(0.473, 0.770, -1.971, 3.384)))
    private fun Gamma1(x: Double) = parameterCubic(x, doubleArrayOf(0.194, 0.125, -2.426, 8.601))
    private fun alpha1(x: Double) = parameterCubic(x, doubleArrayOf(0.018, 0.012, 0.0035, 0.310))
    private fun f2(x: Double) = parameterCubic(x, doubleArrayOf(4.318, 0.326, 4.201, 6.719))
    private fun Gamma2(x: Double) = parameterCubic(x, doubleArrayOf(0.496, 0.597, -0.282, -0.139))
    private fun alpha2(x: Double) = parameterCubic(x, doubleArrayOf(0.014, 0.281, -0.275, -0.569))
    private fun E2(x: Double) = parameterCubic(x, doubleArrayOf(4.529, 4.660, 0.302, 0.241))
    private fun f3(x: Double) = parameterCubic(x, doubleArrayOf(4.924, 5.483, -0.005, -0.337))
    private fun Gamma3(x: Double) = parameterCubic(x, doubleArrayOf(0.800, 0.434, 0.572, -0.553))
    private fun alpha3(x: Double) = parameterCubic(x, doubleArrayOf(0.032, 0.052, -0.300, 0.411))
    private fun E3(x: Double) = parameterCubic(x, doubleArrayOf(4.746, 4.710, -0.007, -0.565))
    private fun f4(x: Double) = parameterCubic(x, doubleArrayOf(3.529, 4.672, -6.226, 0.643))
    private fun Gamma4(x: Double) = parameterCubic(x, doubleArrayOf(0.302, 0.414, -0.414, 1.136))
    private fun alpha4(x: Double) = parameterCubic(x, doubleArrayOf(0.004, 0.023, -0.080, 0.435))
    private fun E4(x: Double) = parameterCubic(x, doubleArrayOf(4.860, 4.976, -0.229, 0.081))
    /** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
}
