package core.optics

import core.Complex_
import core.Complex_.Companion.I
import core.Complex_.Companion.ONE
import core.Complex_.Companion.ZERO
import core.Interpolator
import core.State
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import org.apache.commons.math3.complex.Complex
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
        private var xStop = 0.0
        private lateinit var m: Complex_
        private lateinit var mx: Complex_

        fun extinctionCoefficient(wavelength: Double, epsMatrix: Complex_, epsMetal: Complex_, f: Double, r: Double) =
                alphaExtAlphaSca(wavelength, epsMatrix, epsMetal, f, r).first

        fun scatteringCoefficient(wavelength: Double, epsMatrix: Complex_, epsMetal: Complex_, f: Double, r: Double) =
                alphaExtAlphaSca(wavelength, epsMatrix, epsMetal, f, r).second

        private fun alphaExtAlphaSca(wavelength: Double, epsMatrix: Complex_, epsMetal: Complex_, f: Double, r: Double): Pair<Double, Double> {

            val NANG = 20
            val REFREL = Optics.toRefractiveIndex(epsMetal) / Optics.toRefractiveIndex(epsMatrix)
            val X = 2.0 * Math.PI * r / wavelength

            val (QEXT, QSCA) = bhmie(X, REFREL, NANG)

            val c2 = 3.0 / 4.0 * f / (Math.PI * pow(r * 1E-7, 3.0)) * Math.PI * pow(r * 1E-7, 2.0)
            return c2 * QEXT to c2 * QSCA
        }


        fun bhmie(x: Double, m: Complex, NANG: Int): Pair<Double, Double> {

            var numberOfAngles = NANG
            val maxNumberOfAngles = 1000
            val NMXX = 150000

            if (numberOfAngles > maxNumberOfAngles) {
                throw IllegalArgumentException("***Error: NANG_ > MXNANG_ in bhmie")
            }

            if (numberOfAngles < 2) {
                numberOfAngles = 2
            }
            //*** Obtain pi:
            val mx = m.multiply(x)

//*** Series expansion terminated after NSTOP terms
//    Logarithmic derivatives calculated from NMX on down
            val xStop = x + 4.0 * Math.pow(x, 1.0 / 3.0) + 2.0
            val nMax = Math.round(Math.max(xStop, mx.abs()) + 15).toInt()

// BTD experiment 91/1/15: add one more term to series and compare results
//      NMX=AMAX1(XSTOP,YMOD)+16
// test: compute 7001 wavelengths between .0001 and 1000 micron
// for a=1.0micron SiC grain.  When NMX increased by 1, only a single
// computed number changed (out of 4*7001) and it only changed by 1/8387
// conclusion: we are indeed retaining enough terms in series!
            val nStop = xStop;

            if (nMax > NMXX) {
                throw IllegalArgumentException("Error: NMX > NMXX=' + NMXX + ' for |m|x=' + YMOD")
            }

//*** Require NANG_.GE.1 in order to calculate scattering intensities
            var angleDisctrete = 0.0
            if (numberOfAngles > 1) {
                angleDisctrete = 0.5 * Math.PI / (numberOfAngles - 1)
            }

            var theta: Double
            var j = 1
            val amu = DoubleArray(numberOfAngles + 1)
            while (j <= numberOfAngles) {
                theta = (j - 1) * angleDisctrete
                amu[j] = Math.cos(theta)
                j++
            }

            val Pi0 = DoubleArray(numberOfAngles + 1)
            val Pi1 = DoubleArray(numberOfAngles + 1)
            j = 1
            while (j <= numberOfAngles) {
                Pi0[j] = 0.0
                Pi1[j] = 1.0
                j++
            }

            var NN = 2 * numberOfAngles - 1
            val S1 = DoubleArray(NN + 1).map { Complex_(it) }.toMutableList()
            val S2 = DoubleArray(NN + 1).map { Complex_(it) }.toMutableList()

            j = 1
            while (j <= NN) {
                S1[j] = ZERO
                S2[j] = ZERO
                j++
            }

//
//*** Logarithmic derivative D(J) calculated by downward recurrence
//    beginning with initial value (0.,0.) at J=NMX
//
            val D = IntArray(nMax + 1).map { Complex_(0.0) }.toMutableList()

            D[nMax] = ZERO
            NN = nMax - 1
            var N = 1
            while (N <= NN) {
                val EN = nMax - N + 1
                val ENY = Complex_(EN.toDouble()) / mx
                D[nMax - N] = ENY - ONE / (D[nMax - N + 1] + ENY)
                N++
            }
//
//*** Riccati-Bessel functions with real argument x
//    calculated by upward recurrence
//
            var PSI0 = Math.cos(x)
            var PSIminus1 = Math.sin(x)
            var CHI0 = -Math.sin(x)
            var CHI1 = Math.cos(x)
            var XIminus1 = Complex_(PSIminus1, -CHI1)
            var QSCA = 0.0
            var GSCA = 0.0
            var P = -1.0


            N = 1
            while (N <= nStop) {
                var EN = N
                var FN = (2 * EN + 1) / (EN * (EN + 1))
                // for given N, PSI  = psi_n        CHI  = chi_n
                //              PSI1 = psi_{n-1}    CHI1 = chi_{n-1}
                //              PSI0 = psi_{n-2}    CHI0 = chi_{n-2}
                // Calculate psi_n and chi_n
                var PSI = (2 * EN - 1) * PSIminus1 / x - PSI0
                var CHI = (2 * EN - 1) * CHI1 / x - CHI0
                var XI = Complex_(PSI, -CHI)
                //
                //*** Store previous values of AN and BN for use
                //    in computation of g=<cos(theta)>
                var AN1 = ZERO
                var BN1 = ZERO
                var AN = ZERO
                var BN = ZERO

                if (N > 1) {
                    AN1 = AN
                    BN1 = BN
                }
                //
                //*** Compute AN and BN:
                val aCommon = D[N] / m + Complex_(EN / x)
                AN = (aCommon * PSI - PSIminus1) / (aCommon * XI - XIminus1)

                val bCommon = D[N] * m + Complex_(EN / x)
                BN = (bCommon * PSI - PSIminus1) / (bCommon * XI - XIminus1)

//                AN = D[N].divide(m).add(EN / x).multiply(PSI).subtract(PSIminus1)
//                AN = AN.divide(D[N].divide(m).add(EN / x).multiply(XI).subtract(XIminus1))
//                BN = m.multiply(D[N]).add(EN / x).multiply(PSI).subtract(PSIminus1)
//                BN = BN.divide(m.multiply(D[N]).add(EN / x).multiply(XI).subtract(XIminus1))
                //
                //*** Augment sums for Qsca and g=<cos(theta)>
                QSCA += (2 * EN + 1) * (Math.pow(AN.abs(), 2.0) + Math.pow(BN.abs(), 2.0))
                GSCA += (2 * EN + 1) / (EN * (EN + 1)) * (AN.real * BN.real + AN.imaginary * BN.imaginary)
                if (N > 1) {
                    GSCA += (EN - 1) * (EN + 1) / EN * (AN1.real * AN.real + AN1.imaginary * AN.imaginary +
                            BN1.real * BN.real + BN1.imaginary * BN.imaginary)
                }
                //
                //*** Now calculate scattering intensity pattern
                //    First do angles from 0 to 90
                val PI = DoubleArray(numberOfAngles + 1)
                val TAU = DoubleArray(numberOfAngles + 1)
                j = 1
                while (j <= numberOfAngles) {
                    var JJ = 2 * numberOfAngles - j
                    PI[j] = Pi1[j]
                    TAU[j] = EN * amu[j] * PI[j] - (EN + 1) * Pi0[j]



                    S1[j] = S1[j] + AN * Complex_(FN * PI[j]) + BN * Complex_(FN * TAU[j])
                    S2[j] = S2[j] + AN * Complex_(FN * TAU[j]) + BN * Complex_(FN * PI[j])

//                    S1[j] = S1[j].add(AN.multiply(FN * PI[j])).add(BN.multiply(FN * TAU[j]))
//                    S2[j] = S2[j].add(AN.multiply(FN * TAU[j])).add(BN.multiply(FN * PI[j]))
                    j++
                }
                //
                //*** Now do angles greater than 90 using PI and TAU from
                //    angles less than 90.
                //    P=1 for N=1,3,...; P=-1 for N=2,4,...
                P = -P
                j = 1
                while (j <= numberOfAngles - 1) {
                    var JJ = 2 * numberOfAngles - j

                    S1[JJ] += AN * Complex_(FN * P * PI[j]) - BN * Complex_(FN * P * TAU[j])
                    S2[JJ] += AN * Complex_(FN * P * TAU[j]) - BN * Complex_(FN * P * PI[j])



//                    S1[JJ] = S1[JJ].add(AN.multiply(FN * P * PI[j]).subtract(BN.multiply(FN * P * TAU[j])))
//                    S2[JJ] = S2[JJ].add(AN.multiply(FN * P * TAU[j]).subtract(BN.multiply(FN * P * PI[j])))
                    j++
                }
                PSI0 = PSIminus1
                PSIminus1 = PSI
                CHI0 = CHI1
                CHI1 = CHI
                XIminus1 = Complex_(PSIminus1, -CHI1)
                //
                //*** Compute pi_n for next value of n
                //    For each angle J, compute pi_n+1
                //    from PI = pi_n , PI0 = pi_n-1
                j = 1
                while (j <= numberOfAngles) {
                    Pi1[j] = ((2 * EN + 1) * amu[j] * PI[j] - (EN + 1) * Pi0[j]) / EN
                    Pi0[j] = PI[j]
                    j++
                }
                N++
            }

            //
//*** Have summed sufficient terms.
//    Now compute QSCA,QEXT,QBACK,and GSCA
            GSCA = 2.0 * GSCA / QSCA
            QSCA = 2.0 / (x * x) * QSCA
            val QEXT = 4.0 / (x * x) * S1[1].real
            val QBACK = Math.pow(S1[2 * numberOfAngles - 1].abs() / x, 2.0) / Math.PI
            val QABS = QEXT - QSCA

            return QEXT to QSCA
        }


//        private fun alphaExtAlphaSca(wavelength: Double, epsMatrix: Complex_, epsMetal: Complex_, f: Double, r: Double): Pair<Double, Double> {
//
//            init(wavelength, epsMatrix, epsMetal, r)
//
//            val c1 = 2.0 * PI / (wavevector * wavevector)
//
//            var Cext = 0.0
//            var Csca = 0.0
//            for (i in 1 until a.size) {
//                Cext += (2.0 * i + 1.0) * (a[i] + b[i]).real
//                Csca += (2.0 * i + 1.0) * (pow(a[i].abs(), 2.0) + pow(b[i].abs(), 2.0))
//            }
//            Cext *= c1
//            Csca *= c1
//
//
//
////            val Cext = c1 * a.subList(1, a.lastIndex).indices
////                    .sumByDouble { (2.0 * it + 1.0) * (a[it] + b[it]).real }
////            val Csca = c1 * a.subList(1, a.lastIndex).indices
////                    .sumByDouble { (2.0 * it + 1.0) * (pow(a[it].abs(), 2.0) + pow(b[it].abs(), 2.0)) }
//
////            println(
////                    "$wavelength " +
////                            "${a[1].real} " +
////                            "${a[1].imaginary} " +
////                            "${a[2].real} " +
////                            "${a[2].imaginary} " +
////                            "${a[3].real} " +
////                            "${a[3].imaginary} " +
////
////                            "${b[1].real} " +
////                            "${b[1].imaginary} " +
////                            "${b[2].real} " +
////                            "${b[2].imaginary} " +
////                            "${b[3].real} " +
////                            "${b[3].imaginary}"
////            )
////            println("$wavelength $Cext")
//
//            val c2 = 3.0 / 4.0 * f / (PI * pow(radius, 3.0))
//
//            var aAcc = 0
//            var bAcc = 0
//            for (i in 1 until a.size) {
//                aAcc += 2 * i + 1
//                bAcc += 2 * i + 1
//            }
//
//            println("$wavelength ${xStop}")
////            println(
////                    "$wavelength " +
////                            "${c1 * c2 * (
////                                    3.0 * (a[1] + b[1]).real +
////                                            5.0 * (a[2] + b[2]).real +
////                                            7.0 * (a[3] + b[3]).real +
////                                            9.0 * (a[4] + b[4]).real +
////                                            11.0 * (a[5] + b[5]).real)} "
////            )
//
//            return c2 * Cext to c2 * Csca
////            return Cext to Csca
//        }
//
//        private fun init(wavelength: Double, epsMatrix: Complex_, epsMetal: Complex_, r: Double) {
//            radius = r * 1E-7 // cm^-1 as for wavelength
//            val n = Optics.toRefractiveIndex(epsMatrix)
//            wavevector = n.real * 2.0 * PI / (wavelength * 1E-7)
//            x = wavevector * radius
//            m = Optics.toRefractiveIndex(epsMetal) / Optics.toRefractiveIndex(epsMatrix)
//            mx = m * x
//
//            xStop = x + 4.0 * Math.pow(x, 1.0 / 3.0) + 2.0
//            nStop = xStop.toInt()
////            nStop = Math.round(x + 4.0 * Math.pow(x, 1.0 / 3.0) + 2.0).toInt()
//
//            computeD()
//            computePsiChiXi()
//            computeab()
//        }
//
//        private fun computeD() {
//            val nMax = Math.round(Math.max(xStop, mx.abs()) + 15).toInt()
////            val nMax = Math.round(Math.max(nStop.toDouble(), mx.abs())).toInt() + 15
//
////            println("$xStop $nMax")
//
//            D = IntArray(nMax).map { ZERO }.toMutableList()
//            for (i in nMax - 2 downTo 0) {
//                val c = Complex_(i.toDouble()) / mx
//                D[i] = c - ONE / (D[i + 1] + c)
//            }
//        }
//
//        private fun computePsiChiXi() {
//            psi = DoubleArray(nStop).toMutableList()
//            psi[0] = cos(x)
//            psi[1] = sin(x)
//
//            val chi = DoubleArray(nStop).toMutableList()
//            chi[0] = -sin(x)
//            chi[1] = cos(x)
//
//            for (i in 2 until nStop) {
//                psi[i] = (2.0 * i - 1.0) / x * psi[i - 1] - psi[i - 2]
//                chi[i] = (2.0 * i - 1.0) / x * chi[i - 1] - chi[i - 2]
//            }
//
//            xi = psi.indices.map { Complex_(psi[it], -chi[it]) }
//        }
//
//        private fun computeab() {
//            a = DoubleArray(nStop).map { ZERO }.toMutableList()
//            b = DoubleArray(nStop).map { ZERO }.toMutableList()
//            for (i in 1 until nStop) {
//                val aCommon = D[i] / m + i / x
//                a[i] = (aCommon * psi[i] - psi[i - 1]) / (aCommon * xi[i] - xi[i - 1])
//
//                val bCommon = D[i] * m + i / x
//                b[i] = (bCommon * psi[i] - psi[i - 1]) / (bCommon * xi[i] - xi[i - 1])
//            }
//        }


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
////            val a = r * 1E-7 // cm^-1 as for wavelength
////            val a2 = pow(a, 2.0)
////            val a3 = pow(a, 3.0)
////
////            val n = Optics.toRefractiveIndex(epsMatrix)
////            val x = n.real * 2.0 * PI / (wavelength * 1E-7) * a
////
////            val x2 = pow(x, 2.0)
////            val x4 = pow(x, 4.0)
////            val m = epsMetal / epsMatrix // m_squared in Nolte paper
////
////            val c1 = (m - 1.0) / (m + 2.0)
////            val c2 = c1 * (n.real * n.real) / (n * n)
////            val c3 = 32.0 / 3.0 * PI * a2 * x4
////
////            val Cext1stTerm =
////                    4.0 * PI * a2 * x * (c2 * (ONE + c1 * x2 / 15.0 * (m * m + m * 27.0 + 38.0) / (m * 2.0 + 3.0))).imaginary
////            val Cext2ndTerm = c3 * (c2 * c1).real
////            val Cext = Cext1stTerm + Cext2ndTerm
////
////            val ampl = 3.0 / 4.0 * f / (PI * a3)
////
////            println("$wavelength ${ampl * 4.0 * PI * a2 * x * (c2).imaginary} ${ampl * Cext2ndTerm}")
//////            println("$wavelength ${ampl * Cext1stTerm} ${ampl * Cext2ndTerm} ${c1.real} ${c1.imaginary}")
////
////            val Csca = c3 * pow(c1.abs(), 2.0)
////
////            return with(3.0 / 4.0 * f / (PI * a3)) { this * Cext to this * Csca }
//
//
//
//            val n = Optics.toRefractiveIndex(epsMatrix)
//            val rReduced = r * 1E-7 // cm^-1 as for wavelength
//            val rReduced3 = pow(rReduced, 3.0)
//            val wavelengthReduced = wavelength * 1E-7
//            val waveVector = n * 2.0 * PI / wavelengthReduced
//            val waveVectorReal = n.real * 2.0 * PI / wavelengthReduced
//
//            val x = waveVector * rReduced
//
//            val x3 = x * x * x
//            val x5 = x * x * x * x * x
//            val x6 = x * x * x * x * x * x
//            val m = epsMetal / epsMatrix // m_squared in Nolte paper
//
//            val c1 = m - 1.0
//            val c2 = c1 / (m + 2.0)
//
//            val a = listOf(
//                    -I * 2.0 / 3.0 * x3 * c2 - I * 2.0 / 5.0 * x5 * c2 * (m - 2.0) / (m + 2.0) + c2 * c2 * 4.0 / 6.0 * x6,
//                    -I / 15.0 * x5 * c1 / (m * 2.0 + 3.0)
//            )
//
//            val b = listOf(
//                    -I / 45.0 * x5 * c1,
//                    ZERO
//            )
//
////            val c3 = Complex_(2.0 * PI) / (waveVector * waveVector)
//            val c3 = 2.0 * PI / (waveVectorReal * waveVectorReal)
//            val Cext = c3 * a.indices.sumByDouble { (2 * (it + 1) + 1) * (a[it] + b[it]).real } // it + 1
//            val Csca = c3 * a.indices.sumByDouble { (2 * (it + 1) + 1) * (pow(a[it].abs(), 2.0) + pow(b[it].abs(), 2.0)) }  // it + 1
//
//            return with(3.0 / 4.0 * f / (PI * rReduced3)) { this * Cext to this * Csca }
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
