package core.optics.metal.clusters

import core.Complex_
import core.optics.toEnergy

object DrudeModel {
  fun permittivity(wavelength: Double, wPlasma: Double, gammaPlasma: Double, epsInf: Double): Complex_ {
    val w = Complex_(toEnergy(wavelength)) // eV
    val numerator = Complex_(wPlasma * wPlasma)
    val denominator = w * (w + Complex_(0.0, gammaPlasma))
    return Complex_(epsInf) - (numerator / denominator)
  }
}