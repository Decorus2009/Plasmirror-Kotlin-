package core.layers

import core.Complex_
import core.Complex_.Companion.ONE
import core.Matrix_
import core.State
import core.optics.AlGaAsPermittivity.AlGaAsPermittivity
import core.optics.AlGaAsPermittivity.refractiveIndex
import core.optics.EpsType
import core.optics.MetallicClusters


interface MetallicClustersInAlGaAs : AlGaAsLayer {
    val epsMatrix
        get() = AlGaAsPermittivity(State.wavelengthCurrent, k, x, epsType)
    val epsMetal: Complex_
}


interface DrudeMetalClustersInAlGaAs : MetallicClustersInAlGaAs {
    val wPlasma: Double
    val gammaPlasma: Double
    val epsInf: Double

    override val epsMetal: Complex_
        get() = MetallicClusters.Permittivity.Drude.get(State.wavelengthCurrent, wPlasma, gammaPlasma, epsInf)
}


interface SbClustersInAlGaAs : MetallicClustersInAlGaAs {
    override val epsMetal: Complex_
        get() = MetallicClusters.Permittivity.SbTabulated.get(State.wavelengthCurrent)
}


abstract class TwoDimensionalLayerOfMetallicClustersInAlGaAs(d: Double, k: Double, x: Double,
                                                             val latticeFactor: Double,
                                                             epsType: EpsType) :
        MetallicClustersInAlGaAs, AlGaAs(d, k, x, epsType) {

    override val matrix: Matrix_
        get() = Matrix_().apply {
            with(MetallicClusters.TwoDimensionalLayer.rt(State.wavelengthCurrent, d, latticeFactor, epsMatrix, epsMetal)) {
                val r = first
                val t = second
                this@apply[0, 0] = (t * t - r * r) / t
                this@apply[0, 1] = r / t
                this@apply[1, 0] = -r / t
                this@apply[1, 1] = ONE / t
            }
        }

}


class TwoDimensionalLayerOfDrudeMetalClustersInAlGaAs(d: Double, k: Double, x: Double,
                                                      latticeFactor: Double,
                                                      override val wPlasma: Double,
                                                      override val gammaPlasma: Double,
                                                      override val epsInf: Double,
                                                      epsType: EpsType) :
        DrudeMetalClustersInAlGaAs,
        TwoDimensionalLayerOfMetallicClustersInAlGaAs(d, k, x, latticeFactor, epsType)


class TwoDimensionalLayerOfSbClustersInAlGaAs(d: Double, k: Double, x: Double,
                                              latticeFactor: Double,
                                              epsType: EpsType) :
        SbClustersInAlGaAs,
        TwoDimensionalLayerOfMetallicClustersInAlGaAs(d, k, x, latticeFactor, epsType)


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

    override val n = refractiveIndex(MetallicClusters.Permittivity.EffectiveMediumApproximation.get(epsMatrix, epsMetal, f))

    override fun parameters() = listOf(d, k, x, wPlasma, gammaPlasma, f, epsInf)
}