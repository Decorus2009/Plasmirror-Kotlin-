package core.optics

import core.Complex_
import core.Complex_.Companion.I
import core.Complex_.Companion.ONE
import core.Interpolator
import core.State
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object MetallicClusters {

    object Permittivity {
        object Drude {
            fun get(wavelength: Double, wPlasma: Double, gammaPlasma: Double, epsInf: Double): Complex_ {
                val w = Complex_(toEnergy(wavelength)) // eV
                val numerator = Complex_(wPlasma * wPlasma)
                val denominator = w * (w + Complex_(0.0, gammaPlasma))
                return Complex_(epsInf) - (numerator / denominator)
            }
        }

        object SbTabulated {
            private val functions: Pair<PolynomialSplineFunction, PolynomialSplineFunction>
            private val path = Paths.get("data/inner/state_parameters/eps_Sb_Cardona_Adachi.txt")
            private val wavelengths = mutableListOf<Double>()
            private val epsSb = mutableListOf<Complex_>()

            init {
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
                with(Scanner(it).useLocale(Locale.ENGLISH)) {
                    wavelengths += nextDouble()
                    epsSb += Complex_(nextDouble(), nextDouble())
                }
            }

            private fun interpolate() = Interpolator.interpolateComplex(wavelengths, epsSb)
        }

        object EffectiveMediumApproximation {
            fun get(epsMatrix: Complex_, epsMetal: Complex_, f: Double): Complex_ {
                val numerator = (epsMetal - epsMatrix) * f * 2.0 + epsMetal + (epsMatrix * 2.0)
                val denominator = (epsMatrix * 2.0) + epsMetal - (epsMetal - epsMatrix) * f
                return epsMatrix * (numerator / denominator)
            }
        }
    }

    /**
     * Persson model (Phys. Rev. B, 28, PP. 4247 (1983))
     */
    object TwoDimensionalLayer {
        fun rt(wavelength: Double,
               d: Double, latticeFactor: Double,
               epsMatrix: Complex_, epsMetal: Complex_): Pair<Complex_, Complex_> {

            val R = d / 2.0
            val a = latticeFactor * R
            val U0 = 9.03 / (a * a * a)

            val n = AlGaAsPermittivity.refractiveIndex(epsMatrix)
            val (cos, sin) = cosSin(n)
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
                (epsMetal - epsMatrix) / (epsMetal + epsMatrix * 2.0) * Math.pow(R, 3.0)

        private fun AB(wavelength: Double, a: Double, cos: Complex_, sin: Complex_) = with(Math.pow(2 * Math.PI / a, 2.0) / wavelength) {
            I / cos * this to sin * this
        }
    }
}
