package core.layers

import core.*
import core.AlGaAsPermittivity.epsAlGaAs
import core.AlGaAsPermittivity.eps_to_n
import core.Complex_.Companion.I
import core.Complex_.Companion.ONE
import core.Polarization.P
import core.Polarization.S
import core.State.polarization
import core.State.wavelengthCurrent
import java.lang.Math.PI
import java.lang.Math.pow


interface MetallicClustersInAlGaAs : AlGaAsLayer {
    val epsMatrix
        get() = epsAlGaAs(wavelengthCurrent, k, x, epsType)
    val epsMetal: Complex_
}


interface DrudeMetalClustersInAlGaAs : MetallicClustersInAlGaAs {
    val wPlasma: Double
    val gammaPlasma: Double
    val epsInf: Double

    override val epsMetal: Complex_
        get() {
            println("***********")
            println(wPlasma)
            println(gammaPlasma)
            println(epsInf)

            val w = Complex_(toEnergy(wavelengthCurrent)) // eV
            val numerator = Complex_(wPlasma * wPlasma)
            val denominator = w * (w + Complex_(0.0, gammaPlasma))
            return Complex_(epsInf) - (numerator / denominator)
        }
}


interface SbClustersInAlGaAs : MetallicClustersInAlGaAs {

    override val epsMetal: Complex_
        get() = SbTabulatedPermittivity.get(wavelengthCurrent)
}


abstract class PerssonModelForMetallicClustersInAlGaAs(d: Double, k: Double, x: Double,
                                                       val latticeFactor: Double,
                                                       epsType: EpsType) :
        MetallicClustersInAlGaAs, AlGaAs(d, k, x, epsType) {
    override val matrix: Matrix_
        get() = Matrix_().apply {
            with(r_and_t) {
                val r = first
                val t = second
                this@apply[0, 0] = (t * t - r * r) / t
                this@apply[0, 1] = r / t
                this@apply[1, 0] = -r / t
                this@apply[1, 1] = ONE / t
            }
        }

    private val R = d / 2.0
    private val a = latticeFactor * R
    private val U_0 = 9.03 / (a * a * a)
    private val cos
        get() = cosThetaInLayer(n)
    private val sin
        get() = Complex_((ONE - cos * cos).sqrt())


    private val r_and_t: Pair<Complex_, Complex_>
        get() {
            val theta = Complex_(cos.acos())

            val common1 = cos * cos * alphaParallel
            val common2 = sin * sin * alphaOrthogonal
            val common3 = ONE + B * (alphaOrthogonal - alphaParallel)
            val common4 = A * B * alphaParallel * alphaOrthogonal * ((theta * I * 2.0).exp())

            val rNumerator = when (polarization) {
                S -> -A * common1
                P -> -A * (common1 - common2) - common4
            }
            val tNumerator = when (polarization) {
                S -> ONE - B * alphaParallel
                P -> common3
            }
            val commonDenominator = when (polarization) {
                S -> ONE - B * alphaParallel - A * common1
                P -> common3 - A * (common1 + common2) - common4
            }

            return rNumerator / commonDenominator to tNumerator / commonDenominator
        }

    private val alpha: Complex_
        get() = (epsMetal - epsMatrix) / (epsMetal + epsMatrix * 2.0) * pow(R, 3.0)

    private val alphaParallel = with(alpha) { this / (ONE - this * 0.5 * U_0) }

    private val alphaOrthogonal: Complex_ = with(alpha) { this / (ONE + this * U_0) }

    private val A = I / cos * pow(2 * PI / a, 2.0) / wavelengthCurrent

    private val B = sin * pow(2 * PI / a, 2.0) / wavelengthCurrent
}


class PerssonModelForDrudeMetalClustersInAlGaAs(d: Double, k: Double, x: Double,
                                                latticeFactor: Double,
                                                override val wPlasma: Double,
                                                override val gammaPlasma: Double,
                                                override val epsInf: Double,
                                                epsType: EpsType) :
        DrudeMetalClustersInAlGaAs,
        PerssonModelForMetallicClustersInAlGaAs(d, k, x, latticeFactor, epsType) {

    init {
        println("Constructing with d = $d, k = $k, x = $x, wPlasma = $wPlasma, gammaPlasma = $gammaPlasma, epsInf = $epsInf")
    }

    override val epsMetal: Complex_
        get() {
            println("***********")
            println(wPlasma)
            println(gammaPlasma)
            println(epsInf)

            val w = Complex_(toEnergy(wavelengthCurrent)) // eV
            val numerator = Complex_(wPlasma * wPlasma)
            val denominator = w * (w + Complex_(0.0, gammaPlasma))
            return Complex_(epsInf) - (numerator / denominator)
        }
}


class PerssonModelForSbClustersInAlGaAs(d: Double, k: Double, x: Double,
                                        latticeFactor: Double,
                                        epsType: EpsType) :
        SbClustersInAlGaAs,
        PerssonModelForMetallicClustersInAlGaAs(d, k, x, latticeFactor, epsType) {
}


/**
 * https://en.wikipedia.oxrg/wiki/Effective_medium_approximations
 * @param f  volume fraction of metallic clusters in AlGaAs matrix
 * @return Maxwell-Garnett epsEff
 */
class EffectiveMediumForDrudeMetalClustersInAlGaAs(d: Double, k: Double, x: Double,
                                                   override val wPlasma: Double,
                                                   override val gammaPlasma: Double,
                                                   val f: Double,
                                                   override val epsInf: Double,
                                                   epsType: EpsType) :
        DrudeMetalClustersInAlGaAs, AlGaAs(d, k, x, epsType) {
    private val epsEff: Complex_
        get() {
            val numerator = (epsMetal - epsMatrix) * f * 2.0 + epsMetal + (epsMatrix * 2.0)
            val denominator = (epsMatrix * 2.0) + epsMetal - (epsMetal - epsMatrix) * f
            return epsMatrix * (numerator / denominator)
        }

    override val n = eps_to_n(epsEff)

    override fun parameters() = listOf(d, k, x, wPlasma, gammaPlasma, f, epsInf)
}