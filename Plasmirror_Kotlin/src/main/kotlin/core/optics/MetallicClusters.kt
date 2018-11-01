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

    object Mie {

        private lateinit var D: MutableList<Complex_>
        private lateinit var psi: MutableList<Double>
        private lateinit var xi: List<Complex_>
        private lateinit var a: MutableList<Complex_>
        private lateinit var b: MutableList<Complex_>

        private var x = 0.0
        private var nStop = 0
        private var xStop = 0.0
        private lateinit var m: Complex_
        private lateinit var mx: Complex_

        fun extinctionCoefficient(wavelength: Double, epsMatrix: Complex_, epsMetal: Complex_, f: Double, r: Double) =
                alphaExtAlphaSca(wavelength, epsMatrix, epsMetal, f, r).first

        fun scatteringCoefficient(wavelength: Double, epsMatrix: Complex_, epsMetal: Complex_, f: Double, r: Double) =
                alphaExtAlphaSca(wavelength, epsMatrix, epsMetal, f, r).second

        private fun alphaExtAlphaSca(wavelength: Double, epsMatrix: Complex_, epsMetal: Complex_, f: Double, r: Double): Pair<Double, Double> {

            val numberOfAngles = 20
            val nMatrix = Optics.toRefractiveIndex(epsMatrix)
            val nMetal = Optics.toRefractiveIndex(epsMetal)
            m = nMetal / nMatrix

            x = nMatrix.real * 2.0 * Math.PI * r / wavelength

            val (Qext, Qsca) = BHMie(x, numberOfAngles)
            val Cext = Qext * Math.PI * Math.pow(r * 1E-7, 2.0)
            val Csca = Qsca * Math.PI * Math.pow(r * 1E-7, 2.0)

            val c = 3.0 / 4.0 * f / (Math.PI * Math.pow(r * 1E-7, 3.0))
            return c * Cext to c * Csca
        }


        fun BHMie(x: Double, NANG: Int): Pair<Double, Double> {

            var numberOfAngles = NANG
            val maxNumberOfAngles = 1000

            if (numberOfAngles > maxNumberOfAngles) {
                throw IllegalArgumentException("***Error: NANG_ > MXNANG_ in BHMie")
            }

            if (numberOfAngles < 2) {
                numberOfAngles = 2
            }

//*** Series expansion terminated after NSTOP terms
//    Logarithmic derivatives calculated from NMX on down
            xStop = x + 4.0 * Math.pow(x, 1.0 / 3.0) + 2.0
            val nStop = xStop;

            var angleStep = 0.0
            if (numberOfAngles > 1) {
                angleStep = 0.5 * Math.PI / (numberOfAngles - 1)
            }
            val mu = DoubleArray(numberOfAngles + 1)

            for (i in 1 until numberOfAngles + 1) {
                val theta = (i - 1) * angleStep
                mu[i] = Math.cos(theta)
            }

            val pi0 = DoubleArray(numberOfAngles + 1) { 0.0 }
            val pi1 = DoubleArray(numberOfAngles + 1) { 1.0 }

            val NN = 2 * numberOfAngles - 1
            val S1 = DoubleArray(NN + 1).map { ZERO }.toMutableList()
            val S2 = DoubleArray(NN + 1).map { ZERO }.toMutableList()

            /*
            Logarithmic derivative D(J) calculated by downward recurrence
            beginning with initial value (0.0, 0.0) at i = nMax
             */
            computeD()

            var QSca = 0.0
            var GSca = 0.0
            var P = -1.0
            /*
            Riccati-Bessel functions with real argument x
            calculated by upward recurrence
             */
            var psiPrevPrev = Math.cos(x)
            var psiPrev = Math.sin(x)
            var chiPrevPrev = -Math.sin(x)
            var chiPrev = Math.cos(x)
            var xiPrev = Complex_(psiPrev, -chiPrev)

            for (ind in 1 until nStop.toInt() + 1) {
                val psi = (2.0 * ind - 1.0) * psiPrev / x - psiPrevPrev
                val chi = (2.0 * ind - 1.0) * chiPrev / x - chiPrevPrev
                val xi = Complex_(psi, -chi)
                /*
                Store previous values of a and b for use in computation of g=<cos(theta)>
                */
                var aPrev = ZERO
                var bPrev = ZERO
                var a = ZERO
                var b = ZERO

                if (ind > 1) {
                    aPrev = a
                    bPrev = b
                }

                val aCommon = D[ind] / m + Complex_(ind / x)
                a = (aCommon * psi - psiPrev) / (aCommon * xi - xiPrev)

                val bCommon = D[ind] * m + Complex_(ind / x)
                b = (bCommon * psi - psiPrev) / (bCommon * xi - xiPrev)

                //*** Augment sums for Qsca and g=<cos(theta)>
                QSca += (2.0 * ind + 1.0) * (Math.pow(a.abs(), 2.0) + Math.pow(b.abs(), 2.0))
                GSca += (2.0 * ind + 1.0) / (ind * (ind + 1.0)) * (a.real * b.real + a.imaginary * b.imaginary)
                if (ind > 1) {
                    GSca += (ind - 1.0) * (ind + 1.0) / ind *
                            (aPrev.real * a.real + aPrev.imaginary * a.imaginary +
                                    bPrev.real * b.real + bPrev.imaginary * b.imaginary)
                }

                /*
                Now calculate scattering intensity pattern
                First do angles from 0 to 90
                 */
                val pi = DoubleArray(numberOfAngles + 1)
                val tau = DoubleArray(numberOfAngles + 1)

                val SCommon = (2.0 * ind + 1.0) / (ind * (ind + 1.0))
                for (i in 1 until numberOfAngles + 1) {
                    pi[i] = pi1[i]
                    /*
                    Борен-Хаффман, стр. 152
                     */
                    tau[i] = ind * mu[i] * pi[i] - (ind + 1.0) * pi0[i]

                    S1[i] = S1[i] + a * Complex_(SCommon * pi[i]) + b * Complex_(SCommon * tau[i])
                    S2[i] = S2[i] + a * Complex_(SCommon * tau[i]) + b * Complex_(SCommon * pi[i])
                }

                /*
                Now do angles greater than 90 using PI and TAU from
                angles less than 90.
                P=1 for N=1,3,...; P=-1 for N=2,4,...
                 */
                P = -P
                for (i in 1 until numberOfAngles) {
                    val ii = 2 * numberOfAngles - i

                    S1[ii] += a * Complex_(SCommon * P * pi[i]) - b * Complex_(SCommon * P * tau[i])
                    S2[ii] += a * Complex_(SCommon * P * tau[i]) - b * Complex_(SCommon * P * pi[i])
                }

                psiPrevPrev = psiPrev
                psiPrev = psi
                chiPrevPrev = chiPrev
                chiPrev = chi
                xiPrev = Complex_(psiPrev, -chiPrev)

                /*
                Compute pi_n for next value of n
                For each angle J, compute pi_n+1
                from PI = pi_n , PI0 = pi_n-1
                 */
                for (i in 1 until numberOfAngles + 1) {
                    pi1[i] = ((2.0 * ind + 1.0) * mu[i] * pi[i] - (ind + 1) * pi0[i]) / ind
                    pi0[i] = pi[i]
                }
            }


//*** Have summed sufficient terms.
//    Now compute QSCA,QEXT,QBACK,and GSCA
            GSca = 2.0 * GSca / QSca
            QSca = 2.0 / (x * x) * QSca
            val QEXT = 4.0 / (x * x) * S1[1].real
            val QBACK = Math.pow(S1[2 * numberOfAngles - 1].abs() / x, 2.0) / Math.PI
            val QABS = QEXT - QSca

            return QEXT to QSca
        }

        private fun computeD() {
            val NMXX = 150000
            val mx = m * x
            val nMax = Math.round(Math.max(xStop, mx.abs()) + 15).toInt()

            if (nMax > NMXX) {
                throw IllegalArgumentException("Error: nMax > NMXX=' + NMXX + ' for |m|x=' + YMOD")
            }

            D = IntArray(nMax + 1).map { Complex_(0.0) }.toMutableList()
            D[nMax] = ZERO
            for (i in nMax - 1 downTo 1) {
                val c = Complex_(i + 1.0) / mx
                D[i] = c - ONE / (D[i + 1] + c)
            }
        }

        private fun computePsiXi() {
            psi = DoubleArray(nStop).toMutableList()
            psi[0] = Math.cos(x)
            psi[1] = Math.sin(x)

            val chi = DoubleArray(nStop).toMutableList()
            chi[0] = -Math.sin(x)
            chi[1] = Math.cos(x)

            for (i in 2 until nStop) {
                psi[i] = (2.0 * i - 1.0) / x * psi[i - 1] - psi[i - 2]
                chi[i] = (2.0 * i - 1.0) / x * chi[i - 1] - chi[i - 2]
            }

            xi = psi.indices.map { Complex_(psi[it], -chi[it]) }
        }

        private fun computeAB() {
            a = DoubleArray(nStop).map { ZERO }.toMutableList()
            b = DoubleArray(nStop).map { ZERO }.toMutableList()

            for (i in 1 until nStop) {

                val aCommon = D[i] / m + Complex_(i / x)
                a[i] = (aCommon * psi[i] - psi[i - 1]) / (aCommon * xi[i] - xi[i - 1])

                val bCommon = D[i] * m + Complex_(i / x)
                b[i] = (bCommon * psi[i] - psi[i - 1]) / (bCommon * xi[i] - xi[i - 1])
            }
        }
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
