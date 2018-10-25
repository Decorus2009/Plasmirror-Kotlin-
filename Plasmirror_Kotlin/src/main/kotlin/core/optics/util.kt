package core.optics

import core.Complex_
import core.Complex_.Companion.ONE
import core.State
import core.State.angle
import java.lang.Math.*


enum class Medium { AIR, GAAS_ADACHI, GAAS_GAUSS, GAAS_GAUSS_ADACHI, CUSTOM }

enum class Polarization { S, P }

enum class Regime { REFLECTANCE, TRANSMITTANCE, ABSORBANCE, PERMITTIVITY, REFRACTIVE_INDEX }

enum class EpsType { ADACHI, GAUSS, GAUSS_WITH_ZERO_IM_PERMITTIVITY_BELOW_E0, GAUSS_ADACHI }


fun toEnergy(wavelength: Double) = 1239.8 / wavelength

fun cosThetaIncident() = Complex_(cos(angle * PI / 180.0))

/**
 *  Snell law
 */
fun cosThetaInLayer(n2: Complex_): Complex_ {


    /**
     * !!!!!!!!!!!!!!!!!!!!!!!!!!
     * State.mirror is not initialized yet
     */
    //    val n1 = State.mirror.leftMediumLayer.n
    val n1 = State.leftMediumLayer.n

    val cos1 = cosThetaIncident()
    val sin1_sq = ONE - (cos1 * cos1)
    val sin2_sq = sin1_sq * ((n1 / n2).pow(2.0))

    return Complex_((ONE - sin2_sq).sqrt())
}

fun Double.round(): Double {
    val precision = 7.0
    val power = pow(10.0, precision).toInt()
    return Math.floor((this + 1E-8) * power) / power
}

