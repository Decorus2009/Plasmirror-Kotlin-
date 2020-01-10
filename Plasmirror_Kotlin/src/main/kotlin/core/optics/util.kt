package core.optics

import core.Complex_
import core.Complex_.Companion.ONE
import core.State
import core.State.angle
import java.lang.Math.PI
import kotlin.math.*

object Optics {
  fun extinctionCoefficient(wavelength: Double, refractiveIndex: Complex_) =
    4.0 * PI * refractiveIndex.imaginary / (wavelength * 1E-7) // cm^-1

  fun toRefractiveIndex(eps: Complex_) = with(eps) {
    Complex_(sqrt((abs() + real) / 2.0), sqrt((abs() - real) / 2.0))
  }
}

enum class Polarization { S, P }

enum class Medium { AIR, GAAS_ADACHI, GAAS_GAUSS, CUSTOM }

enum class EpsType { ADACHI, GAUSS, GAUSS_WITH_VARIABLE_IM_PERMITTIVITY_BELOW_E0 }

enum class Regime {
  REFLECTANCE, TRANSMITTANCE, ABSORBANCE, PERMITTIVITY, REFRACTIVE_INDEX, EXTINCTION_COEFFICIENT, SCATTERING_COEFFICIENT
}


fun toEnergy(wavelength: Double) = 1239.8 / wavelength

fun cosThetaIncident() = Complex_(cos(angle * PI / 180.0))

/**
 *  Snell law
 */
fun cosThetaInLayer(n2: Complex_): Complex_ {
  val n1 = State.leftMediumLayer.n

  val cos1 = cosThetaIncident()
  val sin1Sq = ONE - (cos1 * cos1)
  val sin2Sq = sin1Sq * ((n1 / n2).pow(2.0))

  return Complex_((ONE - sin2Sq).sqrt())
}

fun Double.round(): Double {
  val precision = 7.0
  val power = 10.0.pow(precision).toInt()
  return floor((this + 1E-8) * power) / power
}

