package core.optics.metal.clusters.mie

import core.Complex_

interface Mie {
  fun scatteringCoefficient(wavelength: Double, epsSemiconductor: Complex_, epsMetal: Complex_, f: Double, r: Double): Double
  fun extinctionCoefficient(wavelength: Double, epsSemiconductor: Complex_, epsMetal: Complex_, f: Double, r: Double): Double
}