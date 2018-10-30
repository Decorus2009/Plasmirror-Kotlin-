package core.optics

import core.Complex_
import core.Complex_.Companion.I
import core.Complex_.Companion.ONE
import core.Complex_.Companion.ZERO
import core.Interpolator
import core.State
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import java.lang.Math.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object MetallicClusters {

    object OpticalConstants {

        object DrudeModel {

            fun permittivity(wavelength: Double, wPlasma: Double, gammaPlasma: Double, epsInf: Double): Complex_ {
                val w = Complex_(toEnergy(wavelength)) // eV
                val numerator = Complex_(wPlasma * wPlasma)
                val denominator = w * (w + Complex_(0.0, gammaPlasma))
                return Complex_(epsInf) - (numerator / denominator)
            }
        }

        /**
         * tabulated data by Cardona and Adachi TODO add references
         */
        object SbTabulated {

            private val functions: Pair<PolynomialSplineFunction, PolynomialSplineFunction>
            private val path = Paths.get("data/inner/state_parameters/eps_Sb_Cardona_Adachi.txt")
            private val wavelengths = mutableListOf<Double>()
            private val epsSb = mutableListOf<Complex_>()

            init {
                read()
                functions = interpolate()
            }

            fun permittivity(wavelength: Double) = with(functions) {
                if (wavelengths.isEmpty() or epsSb.isEmpty()) {
                    throw IllegalStateException("Empty array of Sb get")
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
                with(Scanner(it).useLocale(Locale.ENGLISH)) {
                    wavelengths += nextDouble()
                    epsSb += Complex_(nextDouble(), nextDouble())
                }
            }

            private fun interpolate() = Interpolator.interpolateComplex(wavelengths, epsSb)
        }
    }

    object EffectiveMediumApproximation {

        fun permittivity(epsMatrix: Complex_, epsMetal: Complex_, f: Double): Complex_ {
            val numerator = (epsMetal - epsMatrix) * f * 2.0 + epsMetal + (epsMatrix * 2.0)
            val denominator = (epsMatrix * 2.0) + epsMetal - (epsMetal - epsMatrix) * f
            return epsMatrix * (numerator / denominator)
        }
    }

    object MieTheory {

        private lateinit var D: MutableList<Complex_>
        private lateinit var psi: MutableList<Double>
        private lateinit var xi: List<Complex_>
        private lateinit var a: MutableList<Complex_>
        private lateinit var b: MutableList<Complex_>

        private var x = 0.0
        private var wavevector = 0.0
        private var radius = 0.0
        private var nStop = 0
        private lateinit var m: Complex_
        private lateinit var mx: Complex_

        fun extinctionCoefficient(wavelength: Double, epsMatrix: Complex_, epsMetal: Complex_, f: Double, r: Double) =
                alphaExtAlphaSca(wavelength, epsMatrix, epsMetal, f, r).first

        fun scatteringCoefficient(wavelength: Double, epsMatrix: Complex_, epsMetal: Complex_, f: Double, r: Double) =
                alphaExtAlphaSca(wavelength, epsMatrix, epsMetal, f, r).second

        private fun alphaExtAlphaSca(wavelength: Double, epsMatrix: Complex_, epsMetal: Complex_, f: Double, r: Double): Pair<Double, Double> {

            init(wavelength, epsMatrix, epsMetal, r)

            val c1 = 2.0 * PI / (wavevector * wavevector)

            val Cext = c1 * a.subList(1, a.lastIndex).indices
                    .sumByDouble { (2.0 * it + 1.0) * (a[it] + b[it]).real }
            val Csca = c1 * a.subList(1, a.lastIndex).indices
                    .sumByDouble { (2.0 * it + 1.0) * (pow(a[it].abs(), 2.0) + pow(b[it].abs(), 2.0)) }

//            println("$wavelength ${magnitude * 4.0 * PI * a2 * x * (c2).imaginary} ${magnitude * Cext2ndTerm}")
//            println("$wavelength ${ampl * Cext1stTerm} ${ampl * Cext2ndTerm} ${c1.real} ${c1.imaginary}")

            val c2 = 3.0 / 4.0 * f / (PI * pow(radius, 3.0))
            return c2 * Cext to c2 * Csca
        }

        private fun init(wavelength: Double, epsMatrix: Complex_, epsMetal: Complex_, r: Double) {
            radius = r * 1E-7 // cm^-1 as for wavelength
            val n = Optics.toRefractiveIndex(epsMatrix)
            wavevector = n.real * 2.0 * PI / (wavelength * 1E-7)
            x = wavevector * radius
            m = Optics.toRefractiveIndex(epsMetal) / Optics.toRefractiveIndex(epsMatrix)
            mx = m * x

            nStop = Math.round(x + 4.0 * Math.pow(x, 1.0 / 3.0) + 2.0).toInt()

            computeD()
            println("************")
            D.map { it.real }.forEach { println(it) }
            D.map { it.imaginary }.forEach { println(it) }

            computePsiChiXi()
            computeab()
        }

        private fun computeD() {
            val nMax = Math.round(Math.max(nStop.toDouble(), mx.abs())).toInt() + 15

            println("$nStop $nMax")

            D = IntArray(nMax).map { ZERO }.toMutableList()
            for (i in nMax - 2 downTo 0) {
                val c = Complex_(i.toDouble()) / mx
                D[i] = c - ONE / (D[i + 1] + c)
            }
        }

        private fun computePsiChiXi() {
            psi = DoubleArray(nStop).toMutableList()
            psi[0] = cos(x)
            psi[1] = sin(x)

            val chi = DoubleArray(nStop).toMutableList()
            chi[0] = -sin(x)
            chi[1] = cos(x)

            for (i in 2 until nStop) {
                psi[i] = (2.0 * i - 1.0) / x * psi[i - 1] - psi[i - 2]
                chi[i] = (2.0 * i - 1.0) / x * chi[i - 1] - chi[i - 2]
            }

            xi = psi.indices.map { Complex_(psi[it], -chi[it]) }
        }

        private fun computeab() {
            a = DoubleArray(nStop).map { ZERO }.toMutableList()
            b = DoubleArray(nStop).map { ZERO }.toMutableList()
            for (i in 1 until nStop) {
                val aCommon = D[i] / m + i / x
                a[i] = (aCommon * psi[i] - psi[i - 1]) / (aCommon * xi[i] - xi[i - 1])

                val bCommon = D[i] * m + i / x
                b[i] = (bCommon * psi[i] - psi[i - 1]) / (bCommon * xi[i] - xi[i - 1])
            }
        }










//        fun extinctionCoefficient(wavelength: Double, epsMatrix: Complex_, epsMetal: Complex_, f: Double, r: Double) =
//                alphaExtAlphaSca(wavelength, epsMatrix, epsMetal, f, r).first
//
//        fun scatteringCoefficient(wavelength: Double, epsMatrix: Complex_, epsMetal: Complex_, f: Double, r: Double) =
//                alphaExtAlphaSca(wavelength, epsMatrix, epsMetal, f, r).second
//
//        private fun alphaExtAlphaSca(wavelength: Double, epsMatrix: Complex_, epsMetal: Complex_, f: Double, r: Double): Pair<Double, Double> {
//
//            // r independent
////            val n = Optics.toRefractiveIndex(epsMatrix)
////            val m = epsMetal / epsMatrix
////            val waveVector = n.real * 2.0 * PI / (wavelength * 1E-7)
////
////            println("$wavelength ${3.0 * f * waveVector * ((m - 1.0) / (m + 2.0) * (n.real * n.real) / (n * n)).imaginary}")
////
////            return 3.0 * f * waveVector * ((m - 1.0) / (m + 2.0)).imaginary to 1000.0
//////            return 3.0 * f * waveVector * ((m - 1.0) / (m + 2.0) * (n.real * n.real) / (n * n)).imaginary to 1000.0
//
//
//            val a = r * 1E-7 // cm^-1 as for wavelength
//            val a2 = pow(a, 2.0)
//            val a3 = pow(a, 3.0)
//
//            val n = Optics.toRefractiveIndex(epsMatrix)
//            val x = n.real * 2.0 * PI / (wavelength * 1E-7) * a
//
//            val x2 = pow(x, 2.0)
//            val x4 = pow(x, 4.0)
//            val m = epsMetal / epsMatrix // m_squared in Nolte paper
//
//            val c1 = (m - 1.0) / (m + 2.0)
//            val c2 = c1 * (n.real * n.real) / (n * n)
//            val c3 = 32.0 / 3.0 * PI * a2 * x4
//
//            val Cext1stTerm =
//                    4.0 * PI * a2 * x * (c2 * (ONE + c1 * x2 / 15.0 * (m * m + m * 27.0 + 38.0) / (m * 2.0 + 3.0))).imaginary
//            val Cext2ndTerm = c3 * (c2 * c1).real
//            val Cext = Cext1stTerm + Cext2ndTerm
//
//            val ampl = 3.0 / 4.0 * f / (PI * a3)
//
//            println("$wavelength ${ampl * 4.0 * PI * a2 * x * (c2).imaginary} ${ampl * Cext2ndTerm}")
////            println("$wavelength ${ampl * Cext1stTerm} ${ampl * Cext2ndTerm} ${c1.real} ${c1.imaginary}")
//
//            val Csca = c3 * pow(c1.abs(), 2.0)
//
//            return with(3.0 / 4.0 * f / (PI * a3)) { this * Cext to this * Csca }
//
//
//
////            val n = Optics.toRefractiveIndex(epsMatrix)
////            val rReduced = r * 1E-7 // cm^-1 as for wavelength
////            val rReduced3 = pow(rReduced, 3.0)
////            val wavelengthReduced = wavelength * 1E-7
////            val waveVector = n * 2.0 * PI / wavelengthReduced
////            val waveVectorReal = n.real * 2.0 * PI / wavelengthReduced
////
////            val x = waveVector * rReduced
////
////            val x3 = x * x * x
////            val x5 = x * x * x * x * x
////            val x6 = x * x * x * x * x * x
////            val m = epsMetal / epsMatrix // m_squared in Nolte paper
////
////            val c1 = m - 1.0
////            val c2 = c1 / (m + 2.0)
////
////            val a = listOf(
////                    -I * 2.0 / 3.0 * x3 * c2 - I * 2.0 / 5.0 * x5 * c2 * (m - 2.0) / (m + 2.0) + c2 * c2 * 4.0 / 6.0 * x6,
////                    -I / 15.0 * x5 * c1 / (m * 2.0 + 3.0)
////            )
////
////            val b = listOf(
////                    -I / 45.0 * x5 * c1,
////                    ZERO
////            )
////
//////            val c3 = Complex_(2.0 * PI) / (waveVector * waveVector)
////            val c3 = 2.0 * PI / (waveVectorReal * waveVectorReal)
////            val Cext = c3 * a.indices.sumByDouble { (2 * it + 1) * (a[it] + b[it]).real }
////            val Csca = c3 * a.indices.sumByDouble { (2 * it + 1) * (pow(a[it].abs(), 2.0) + pow(b[it].abs(), 2.0)) }
////
////            return with(3.0 / 4.0 * f / (PI * rReduced3)) { this * Cext to this * Csca }
//        }
    }

    /**
     * Phys. Rev. B, 28, PP. 4247 (1983) - Persson model
     */
    object TwoDimensionalLayer {

        fun rt(wavelength: Double,
               d: Double, latticeFactor: Double,
               epsMatrix: Complex_, epsMetal: Complex_): Pair<Complex_, Complex_> {

            val R = d / 2.0
            val a = latticeFactor * R
            val U0 = 9.03 / (a * a * a)

            val (cos, sin) = cosSin(Optics.toRefractiveIndex(epsMatrix))
            val theta = Complex_(cos.acos())

            val (alphaParallel, alphaOrthogonal) = alphaParallelOrthogonal(alpha(epsMatrix, epsMetal, R), U0)
            val (A, B) = AB(wavelength, a, cos, sin)

            val common1 = cos * cos * alphaParallel
            val common2 = sin * sin * alphaOrthogonal
            val common3 = ONE + B * (alphaOrthogonal - alphaParallel)
            val common4 = A * B * alphaParallel * alphaOrthogonal * ((theta * I * 2.0).exp())

            val rNumerator = when (State.polarization) {
                Polarization.S -> -A * common1
                Polarization.P -> -A * (common1 - common2) - common4
            }
            val tNumerator = when (State.polarization) {
                Polarization.S -> ONE - B * alphaParallel
                Polarization.P -> common3
            }
            val commonDenominator = when (State.polarization) {
                Polarization.S -> ONE - B * alphaParallel - A * common1
                Polarization.P -> common3 - A * (common1 + common2) - common4
            }

            return rNumerator / commonDenominator to tNumerator / commonDenominator
        }

        private fun cosSin(n: Complex_) = with(cosThetaInLayer(n)) { this to Complex_((ONE - this * this).sqrt()) }

        private fun alphaParallelOrthogonal(alpha: Complex_, U0: Double) = with(alpha) {
            this / (ONE - this * 0.5 * U0) to this / (ONE + this * U0)
        }

        private fun alpha(epsMatrix: Complex_, epsMetal: Complex_, R: Double) =
                (epsMetal - epsMatrix) / (epsMetal + epsMatrix * 2.0) * pow(R, 3.0)

        private fun AB(wavelength: Double, a: Double, cos: Complex_, sin: Complex_) = with(pow(2 * PI / a, 2.0) / wavelength) {
            I / cos * this to sin * this
        }
    }
}
