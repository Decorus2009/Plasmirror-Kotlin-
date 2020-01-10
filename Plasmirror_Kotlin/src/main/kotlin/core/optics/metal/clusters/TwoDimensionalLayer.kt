package core.optics.metal.clusters

import core.Complex_
import core.State
import core.optics.*
import kotlin.math.pow

/**
 * Phys. Rev. B, 28, PP. 4247 (1983) - Persson model
 */
object TwoDimensionalLayer {
  fun rt(
    wavelength: Double,
    d: Double,
    latticeFactor: Double,
    epsMatrix: Complex_,
    epsMetal: Complex_
  ): Pair<Complex_, Complex_> {
    val R = d / 2.0
    val a = latticeFactor * R
    val U0 = 9.03 / (a * a * a)

    val (cos, sin) = cosSin(epsMatrix.toRefractiveIndex())
    val theta = Complex_(cos.acos())

    val (alphaParallel, alphaOrthogonal) = alphaParallelOrthogonal(alpha(epsMatrix, epsMetal, R), U0)
    val (A, B) = AB(wavelength, a, cos, sin)

    val common1 = cos * cos * alphaParallel
    val common2 = sin * sin * alphaOrthogonal
    val common3 = Complex_.ONE + B * (alphaOrthogonal - alphaParallel)
    val common4 = A * B * alphaParallel * alphaOrthogonal * ((theta * Complex_.I * 2.0).exp())

    val rNumerator = when (State.polarization) {
      Polarization.S -> -A * common1
      Polarization.P -> -A * (common1 - common2) - common4
    }
    val tNumerator = when (State.polarization) {
      Polarization.S -> Complex_.ONE - B * alphaParallel
      Polarization.P -> common3
    }
    val commonDenominator = when (State.polarization) {
      Polarization.S -> Complex_.ONE - B * alphaParallel - A * common1
      Polarization.P -> common3 - A * (common1 + common2) - common4
    }

    return rNumerator / commonDenominator to tNumerator / commonDenominator
  }

  private fun cosSin(n: Complex_) = with(cosThetaInLayer(n)) { this to Complex_((Complex_.ONE - this * this).sqrt()) }

  private fun alphaParallelOrthogonal(alpha: Complex_, U0: Double) = with(alpha) {
    this / (Complex_.ONE - this * 0.5 * U0) to this / (Complex_.ONE + this * U0)
  }

  private fun alpha(epsMatrix: Complex_, epsMetal: Complex_, R: Double) =
    (epsMetal - epsMatrix) / (epsMetal + epsMatrix * 2.0) * R.pow(3.0)

  private fun AB(wavelength: Double, a: Double, cos: Complex_, sin: Complex_) =
    with((2 * Math.PI / a).pow(2.0) / wavelength) {
      Complex_.I / cos * this to sin * this
    }
}