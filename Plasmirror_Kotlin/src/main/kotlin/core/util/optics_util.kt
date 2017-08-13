package core.util

import core.State
import core.State.GaAs_n
import core.State.angle
import core.layers.*
import org.apache.commons.math3.complex.Complex.ONE
import org.apache.commons.math3.complex.Complex.ZERO
import java.lang.Math.*

/**
 * @param wavelength wavelength
 */
fun toEnergy(wavelength: Double) = 1239.8 / wavelength

/**
 * Adachi approximation for Al(x)Ga(1-x)As dielectric function
 * @param wavelength wavelength
 * @param x AlAs concentration
 * *
 * @return n_AlGaAs
 */
//fun n_AlGaAs(wavelength: Double, x: Double): Double {
//    var w = toEnergy(wavelength)
//    val delta = 0.34 - 0.04 * x // eV
//    val Eg = 1.425 + 1.155 * x + 0.37 * x * x
//    val A = 6.3 + 19.0 * x
//    val B = 9.4 - 10.2 * x
//
//    if (w > Eg) {
//        w = Eg
//    }
//
//    val hi = w / Eg
//    val hi_so = w / (Eg + delta)
//
//    val f: (Double) -> Double = { (2.0 - sqrt(1 + it) - sqrt(1 - it)) / (it * it) }
//    return sqrt(A * (f.invoke(hi) + 0.5 * pow(Eg / (Eg + delta), 1.5) * f.invoke(hi_so)) + B)
//}

/**
 * @param wavelength wavelength
 *
 * @return n_GaAs
 */
fun n_GaAs(wavelength: Double): Cmplx? = when {
    wavelength < 240.0 -> GaAs_n[240.0]
    wavelength > 1800.0 -> GaAs_n[1800.0]
    else -> GaAs_n[wavelength.round()]
}

/**
 *  Snell law
 */
fun cosThetaInLayer(n2: Cmplx): Cmplx {

    val n1 = State.mirror.leftMediumLayer.n

    val cos1 = cosThetaIncident()
    val sin1_sq = Cmplx(ONE) - (cos1 * cos1)
    val sin2_sq = sin1_sq * ((n1 / n2).pow(2.0))

    return Cmplx((Cmplx(ONE) - sin2_sq).sqrt())
}

fun cosThetaIncident() = Cmplx(cos(angle * PI / 180.0))

fun Double.round(): Double {
    val precision = 7.0
    val power = pow(10.0, precision).toInt()
    return Math.floor((this + 1E-8) * power) / power
}


/**
 * Computation of the AlGaAs permittivity using full Gaussian approach from paper
 * J. Appl. Phys., 86, 1, pp.445 (1999). Modeling the optical constants of AlxGa1-xAs alloys.
 * and the Adachi'85 model
 */
object AlGaAsPermittivity {

    val AdachiGaussIntersections = mutableMapOf<Double, Double>()

    fun preprocessIntersections() = with(State.mirror) {
        (structure.blocks.flatMap { it.layers } + leftMediumLayer + rightMediumLayer).forEach {
            when (it) {
                is GaAs, is GaAsExciton -> findIntersection(x = 0.0)
                is AlGaAs -> findIntersection(x = it.x)
                is AlGaAsExciton -> findIntersection(x = it.x)
                is EffectiveMedium -> findIntersection(x = it.x)
            }
        }
    }

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
        val epsGauss = arrayListOf<Cmplx>()
        val nGauss = arrayListOf<Cmplx>()
        val epsAdachi = arrayListOf<Cmplx>()
        val nAdachi = arrayListOf<Cmplx>()

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
            nAdachi.add(nAdachi(w_, x))
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
        AdachiGaussIntersections.putIfAbsent(x, w.indices
                .map { Diff(w[it], diff = abs(nGaussReal[it] - nAdachiReal[it])) }
                .filter { it.w > 1.4 && it.w < upperBound }.minBy { it.diff }!!.w)
    }

    /**
     * If w < intersection energy, returns permittivity computed by the Adachi'85 approximation
     * (with imaginary part computed by the Gauss approximation)
     * Else returns permittivity computed using the Gauss approximation
     */
    fun eps_AlGaAs(wavelength: Double, x: Double): Cmplx {
        if (AdachiGaussIntersections[x] == null) {
            println("Finding intersection for $x")
            findIntersection(x)
        }
        val w = toEnergy(wavelength)
        return if (w < AdachiGaussIntersections[x]!!) {
            Cmplx(epsAdachi(w, x).real, epsGauss(w, x).imaginary)
        } else {
            epsGauss(w, x)
        }
    }

    fun n_AlGaAs(wavelength: Double, x: Double): Cmplx {
        val eps = eps_AlGaAs(wavelength, x)
        val n = sqrt((eps.abs() + eps.real) / 2.0)
        val k = sqrt((eps.abs() - eps.real) / 2.0)
        return Cmplx(n, k)
    }

    /**
     * Permittivity (Adachi)
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     */
    private fun epsAdachi(w: Double, x: Double): Cmplx {
        val nAdachi = nAdachi(w, x)
        return Cmplx(nAdachi * nAdachi)
    }

    private fun nAdachi(w: Double, x: Double): Cmplx {
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
        return Cmplx(sqrt(A * (f.invoke(hi) + 0.5 * pow(Eg / (Eg + delta), 1.5) * f.invoke(hi_so)) + B))
    }
    /** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


    /**
     * Permittivity (Gauss)
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     */
    /**
     * @param w Energy (eV)
     * @param x Al concentration
     * @return AlGaAs permittivity for w and x
     */
    private fun epsGauss(w: Double, x: Double) = eps_inf(x) + eps_1(w, x) + eps_2(w, x) + eps_3(w, x) + eps_4(w, x)

    private fun nGauss(w: Double, x: Double): Cmplx {
        val epsGauss = epsGauss(w, x)
        val nGauss = sqrt((epsGauss.abs() + epsGauss.real) / 2.0)
        val kGauss = sqrt((epsGauss.abs() - epsGauss.real) / 2.0)
        return Cmplx(nGauss, kGauss)
    }

    /**
     * AlGaAs permittivity components. Look at the paper
     */
    private fun eps_1(w: Double, x: Double): Cmplx {
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
        val hi0 = Cmplx(w, Gamma0_Gauss) / E0
        val hi0S = Cmplx(w, Gamma0_Gauss) / E0_plus_delta0
        val d1 = A * pow(E0, -1.5)
        val d2 = 0.5 * pow(E0 / E0_plus_delta0, 1.5)
        val f = { y: Cmplx ->
            val one = Cmplx(ONE)
            val c1 = (one + y).sqrt()
            val c2 = (one - y).sqrt()
            (Cmplx(2.0) - c1 - c2) / (y.pow(2.0))
        }
        return (f.invoke(hi0) + (f.invoke(hi0S) * d2)) * d1
    }

    private fun eps_2(w: Double, x: Double): Cmplx {
        val B1 = B1(x)
        val B1S = B1S(x)
        val E1 = E1(x)
        val E1_plus_delta1 = E1_plus_delta1(x)
        val Gamma1_Gauss = Gamma1_Gauss(w, x)
        val hi1_sq = (Cmplx(w, Gamma1_Gauss) / E1).pow(2.0)
        val hi1S_sq = (Cmplx(w, Gamma1_Gauss) / E1_plus_delta1).pow(2.0)
        val one = Cmplx(ONE)
        val c1 = (B1 / hi1_sq) * ((one - hi1_sq).log()) * -1.0
        val c2 = (B1S / hi1S_sq) * ((one - hi1S_sq).log()) * -1.0
        return c1 + c2
    }

    private fun eps_3(w: Double, x: Double): Cmplx {
        val B1X = B1X(x)
        val B2X = B2X(x)
        val Gamma1_Gauss = Gamma1_Gauss(w, x)
        val E1 = E1(x)
        val E1_plus_delta1 = E1_plus_delta1(x)
        var accumulator = Cmplx(ZERO)
        var summand = Cmplx(ONE)
        val precision = 1E-4
        var n = 1
        /*
          Check the paper. The summation of the excitonic terms
          is performed until the contribution of the next term is less than 10^-4 (precision)
         */
        while (summand.abs() >= precision) {

            val c1 = B1X / Cmplx(E1 - w, -Gamma1_Gauss)
            val c2 = B2X / Cmplx(E1_plus_delta1 - w, -Gamma1_Gauss)

            summand = (c1 + c2) / pow((2.0 * n - 1.0), 3.0)
            accumulator += summand
            n++
        }
        return accumulator
    }

    private fun eps_4(w: Double, x: Double): Cmplx {
        val f = doubleArrayOf(f2(x), f3(x), f4(x))
        val E = doubleArrayOf(E2(x), E3(x), E4(x))
        val Gamma_Gauss = doubleArrayOf(Gamma2_Gauss(w, x), Gamma3_Gauss(w, x), Gamma4_Gauss(w, x))
        var accumulator = Cmplx(ZERO)
        for (i in 0..2) {
            val numerator = Cmplx(f[i] * f[i])
            val denominator = Cmplx(E[i] * E[i] - w * w, -w * Gamma_Gauss[i])
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

    private fun Ei(x: Double, Ei0: Double, Ei1_minus_Ei0: Double, c0: Double, c1: Double)
            = Ei0 + Ei1_minus_Ei0 * x + (c0 + c1 * x) * x * (1 - x)

    /**
     * Table II
     * Cubic dependencies for model parameter values.
     */
    private fun parameterCubic(x: Double, a: DoubleArray) = a[0] * (1 - x) + a[1] * x + (a[2] + a[3] * x) * x * (1 - x)

    /**
     * Lorentz forms modified by the exponential decay (Gaussian-like forms)
     */
    private fun Gamma0_Gauss(w: Double, x: Double): Double {
        val Gamma0 = Gamma0(x)
        return Gamma0 * exp(-alpha0(x) * pow((w - E0(x)) / Gamma0, 2.0))
    }

    private fun Gamma1_Gauss(w: Double, x: Double): Double {
        val Gamma1 = Gamma1(x)
        return Gamma1 * exp(-alpha1(x) * pow((w - E1(x)) / Gamma1, 2.0))
    }

    private fun Gamma2_Gauss(w: Double, x: Double): Double {
        val Gamma2 = Gamma2(x)
        return Gamma2 * exp(-alpha2(x) * pow((w - E2(x)) / Gamma2, 2.0))
    }

    private fun Gamma3_Gauss(w: Double, x: Double): Double {
        val Gamma3 = Gamma3(x)
        return Gamma3 * exp(-alpha3(x) * pow((w - E3(x)) / Gamma3, 2.0))
    }

    private fun Gamma4_Gauss(w: Double, x: Double): Double {
        val Gamma4 = Gamma4(x)
        return Gamma4 * exp(-alpha4(x) * pow((w - E4(x)) / Gamma4, 2.0))
    }


    private fun eps_inf(x: Double) = Cmplx(parameterCubic(x, doubleArrayOf(1.347, 0.02, -0.568, 4.210)))
    private fun A(x: Double) = parameterCubic(x, doubleArrayOf(3.06, 14.210, -0.398, 4.763))
    private fun Gamma0(x: Double) = parameterCubic(x, doubleArrayOf(0.0001, 0.0107, -0.0187, 0.3057))
    private fun alpha0(x: Double) = parameterCubic(x, doubleArrayOf(3.960, 1.617, 3.974, -5.413))
    private fun B1(x: Double) = Cmplx(parameterCubic(x, doubleArrayOf(6.099, 4.381, -4.718, -2.510)))
    private fun B1S(x: Double) = Cmplx(parameterCubic(x, doubleArrayOf(0.001, 0.103, 4.447, 0.208)))
    private fun B1X(x: Double) = Cmplx(parameterCubic(x, doubleArrayOf(1.185, 0.639, 0.436, 0.426)))
    private fun B2X(x: Double) = Cmplx(parameterCubic(x, doubleArrayOf(0.473, 0.770, -1.971, 3.384)))
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

enum class Medium {
    AIR, GAAS, OTHER
}

enum class Polarization {
    S, P
}

enum class Regime {
    R, T, A, EPS, N
}

enum class PermittivityComputationType {
    ADACHI, GAUSS, ADACHI_GAUSS
}


