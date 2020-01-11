package core.optics.metal.clusters.mie

import core.Complex_
import core.Complex_.Companion.I
import core.Complex_.Companion.ONE
import core.Complex_.Companion.ZERO
import core.optics.toRefractiveIndex
import core.toCm
import java.lang.Math.PI
import kotlin.math.pow

interface MieSimple : Mie {
  override fun extinctionCoefficient(
    wavelength: Double, epsSemiconductor: Complex_, epsMetal: Complex_, f: Double, r: Double
  ) = alphaExtAlphaSca(wavelength, epsSemiconductor, epsMetal, f, r).first

  override fun scatteringCoefficient(
    wavelength: Double, epsSemiconductor: Complex_, epsMetal: Complex_, f: Double, r: Double
  ) = alphaExtAlphaSca(wavelength, epsSemiconductor, epsMetal, f, r).second

  fun a(x: Double, mSq: Complex_) = listOf(a1(x, mSq), a2(x, mSq))

  fun b(x: Double, mSq: Complex_) = listOf(b1(x, mSq), b2())

  private fun alphaExtAlphaSca(
    wavelength: Double, epsSemiconductor: Complex_, epsMetal: Complex_, f: Double, r: Double
  ): Pair<Double, Double> {
    val k = 2.0 * PI * epsSemiconductor.toRefractiveIndex().real / wavelength.toCm()
    val x = k * r.toCm()
    val mSq = epsMetal / epsSemiconductor
    val common1 = 2.0 * PI / k.pow(2)
    val common2 = 3.0 / 4.0 * f / (PI * (r.toCm()).pow(3.0))
    val abCoefficients = a(x, mSq).zip(b(x, mSq))

    val Cext = common1 * abCoefficients
      .mapIndexed { index, (a, b) -> (2 * (index + 1) + 1) * (a + b).real } // index + 1 - because index starts with 0
      .sum()
    val Csca = common1 * abCoefficients
      .mapIndexed { index, (a, b) -> (2 * (index + 1) + 1) * (a.abs().pow(2) + b.abs().pow(2)) }
      .sum()

    return common2 * Cext to common2 * Csca
  }

  private fun a1(x: Double, mSq: Complex_): Complex_ {
    val common1 = (mSq - 1.0) / (mSq + 2.0)
    val summand1 = -I * 2.0 / 3.0 * x.pow(3) * common1
    val summand2 = -I * 2.0 / 5.0 * x.pow(5) * (mSq - 2.0) / (mSq + 2.0) * common1
    val summand3 = ONE * 4.0 / 9.0 * x.pow(6) * common1.pow(2.0)
    return summand1 + summand2 + summand3
  }

  private fun a2(x: Double, mSq: Complex_) = -I / 15.0 * x.pow(5) * (mSq - 1.0) / (mSq * 2.0 + 3.0)

  private fun b1(x: Double, mSq: Complex_) = -I / 45.0 * x.pow(5) * (mSq - 1.0)

  private fun b2() = ZERO
}

object MieFirstOrder : MieSimple {
  override fun a(x: Double, mSq: Complex_) = super.a(x, mSq).subList(0, 1)
  override fun b(x: Double, mSq: Complex_) = super.b(x, mSq).subList(0, 1)
}

object MieFirstAndSecondOrder : MieSimple
