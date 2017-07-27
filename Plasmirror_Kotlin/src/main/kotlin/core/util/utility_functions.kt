package core.util

import core.State
import core.State.GaAs_n
import core.State.angle
import org.apache.commons.math3.complex.Complex.ONE
import java.lang.Math.*

/**
 * @param wl wavelength
 */
fun toEnergy(wl: Double) = 1239.8 / wl

/**
 * Adachi approximation for Al(x)Ga(1-x)As dielectric function
 * @param wl wavelength
 * @param x AlAs concentration
 * *
 * @return n_AlGaAs
 */
fun n_AlGaAs(wl: Double, x: Double): Double {

    var w = toEnergy(wl)

    val delta = 0.34 - 0.04 * x // eV
    val Eg = 1.425 + 1.155 * x + 0.37 * x * x
    val A = 6.3 + 19.0 * x
    val B = 9.4 - 10.2 * x

    if (w > Eg) {
        w = Eg
    }

    val hi = w / Eg
    val hi_so = w / (Eg + delta)

    val f: (Double) -> Double = { (2.0 - sqrt(1 + it) - sqrt(1 - it)) / (it * it) }
    return sqrt(A * (f.invoke(hi) + 0.5 * pow(Eg / (Eg + delta), 1.5) * f.invoke(hi_so)) + B)
}

/**
 * @param wl wavelength
 *
 * @return n_GaAs
 */
fun n_GaAs(wl: Double): Cmplx? = when {
    wl < 240.0 -> GaAs_n[240.0]
    wl > 1800.0 -> GaAs_n[1800.0]
    else -> GaAs_n[wl.round()]
}

/**
 *  Snell law
 */
fun cosThetaInLayer(n2: Cmplx): Cmplx {

    val n1 = State.mirror.leftMediumLayer.n

    val cos1 = cosThetaIncident()
    val sin1_sq = Cmplx(ONE) - (cos1 * cos1)
    val sin2_sq = sin1_sq * ((n1 / n2).pow(2.0))

    return Cmplx((Cmplx(ONE) - sin2_sq).sqrt())
}

fun cosThetaIncident() = Cmplx(cos(angle * PI / 180.0))


fun Double.round(): Double {
    val precision = 7.0
    val power = pow(10.0, precision).toInt()
    return Math.floor((this + 1E-8) * power) / power
}
