package core.optics.metal.clusters

import core.Complex_

object EffectiveMediumApproximation {
  fun permittivity(epsMatrix: Complex_, epsMetal: Complex_, f: Double): Complex_ {
    val numerator = (epsMetal - epsMatrix) * f * 2.0 + epsMetal + (epsMatrix * 2.0)
    val denominator = (epsMatrix * 2.0) + epsMetal - (epsMetal - epsMatrix) * f
    return epsMatrix * (numerator / denominator)
  }
}