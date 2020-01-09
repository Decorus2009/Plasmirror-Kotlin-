package core.optics

import core.Complex_
import kotlin.math.sqrt

object Optics {
  fun extinctionCoefficient(wavelength: Double, refractiveIndex: Complex_) =
    4.0 * Math.PI * refractiveIndex.imaginary / (wavelength * 1E-7) // cm^-1

  fun toRefractiveIndex(eps: Complex_) = with(eps) {
    Complex_(sqrt((abs() + real) / 2.0), sqrt((abs() - real) / 2.0))
  }
}