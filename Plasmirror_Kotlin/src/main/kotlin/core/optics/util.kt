package core.optics

import core.Complex_
import core.Complex_.Companion.ONE
import core.State
import core.State.angle
import java.lang.Math.PI
import kotlin.math.*

enum class Polarization { S, P }

enum class Medium { AIR, GAAS_ADACHI, GAAS_GAUSS, CUSTOM }

enum class EpsType { ADACHI, GAUSS, GAUSS_WITH_VARIABLE_IM_PERMITTIVITY_BELOW_E0 }

enum class Regime {
  REFLECTANCE, TRANSMITTANCE, ABSORBANCE, PERMITTIVITY, REFRACTIVE_INDEX, EXTINCTION_COEFFICIENT, SCATTERING_COEFFICIENT
}

fun extinctionCoefficientOf(refractiveIndex: Complex_, wavelength: Double) =
  4.0 * PI * refractiveIndex.imaginary / (wavelength * 1E-7) // cm^-1

fun Complex_.toRefractiveIndex() = Complex_(sqrt((abs() + real) / 2.0), sqrt((abs() - real) / 2.0))

fun Double.toEnergy() = 1239.8 / this

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
