package core.layers

import core.*
import core.Complex_.Companion.ONE
import core.optics.*

/** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
interface MetallicClustersInAlGaAs : AlGaAsLayer {
  val epsMatrix
    get() = AlGaAsMatrix.Permittivity.get(State.wavelengthCurrent, k, x, epsType)
  val epsMetal: Complex_
}

interface DrudeMetalClustersInAlGaAs : MetallicClustersInAlGaAs {
  val wPlasma: Double
  val gammaPlasma: Double
  val epsInf: Double

  override val epsMetal
    get() = MetallicClusters.OpticalConstants.DrudeModel.permittivity(State.wavelengthCurrent, wPlasma, gammaPlasma, epsInf)
}

interface SbClustersInAlGaAs : MetallicClustersInAlGaAs {
  override val epsMetal
    get() = MetallicClusters.OpticalConstants.SbTabulated.permittivity(State.wavelengthCurrent)
}


/** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
abstract class TwoDimensionalLayerOfMetallicClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  val latticeFactor: Double,
  epsType: EpsType
) :
  MetallicClustersInAlGaAs,
  AlGaAs(d, k, x, epsType) {

  override val matrix
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

class TwoDimensionalLayerOfDrudeMetalClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  latticeFactor: Double,
  override val wPlasma: Double,
  override val gammaPlasma: Double,
  override val epsInf: Double,
  epsType: EpsType
) :
  DrudeMetalClustersInAlGaAs,
  TwoDimensionalLayerOfMetallicClustersInAlGaAs(d, k, x, latticeFactor, epsType)

class TwoDimensionalLayerOfSbClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  latticeFactor: Double,
  epsType: EpsType
) :
  SbClustersInAlGaAs,
  TwoDimensionalLayerOfMetallicClustersInAlGaAs(d, k, x, latticeFactor, epsType)


/** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
/**
 * https://en.wikipedia.oxrg/wiki/Effective_medium_approximations
 * @param f  volume fraction of metallic clusters in AlGaAs matrix
 * @return Maxwell-Garnett epsEff
 */
abstract class EffectiveMediumLayerOfMetallicClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  private val f: Double,
  epsType: EpsType
) :
  MetallicClustersInAlGaAs, AlGaAs(d, k, x, epsType) {

  override val n
    get() = Optics.toRefractiveIndex(MetallicClusters.EffectiveMediumApproximation.permittivity(epsMatrix, epsMetal, f))
}

class EffectiveMediumLayerOfDrudeMetalClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  override val wPlasma: Double,
  override val gammaPlasma: Double,
  override val epsInf: Double,
  f: Double,
  epsType: EpsType
) :
  DrudeMetalClustersInAlGaAs,
  EffectiveMediumLayerOfMetallicClustersInAlGaAs(d, k, x, f, epsType)

class EffectiveMediumLayerOfSbClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  f: Double,
  epsType: EpsType
) :
  SbClustersInAlGaAs,
  EffectiveMediumLayerOfMetallicClustersInAlGaAs(d, k, x, f, epsType)


/** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
abstract class MieTheoryLayerOfMetallicClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  private val f: Double,
  private val r: Double,
  epsType: EpsType
) :
  MetallicClustersInAlGaAs, AlGaAs(d, k, x, epsType) {

  // value of AlGaAs refractive index is used as n-property here
  // (Mie theory is for the computation of extinction and scattering, not for the computation of refractive index

  override val alphaExt: Double
    get() = MetallicClusters.Mie.extinctionCoefficient(State.wavelengthCurrent, epsMatrix, epsMetal, f, r)
  val alphaSca: Double
    get() = MetallicClusters.Mie.scatteringCoefficient(State.wavelengthCurrent, epsMatrix, epsMetal, f, r)

}

class MieTheoryLayerOfDrudeMetalClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  override val wPlasma: Double,
  override val gammaPlasma: Double,
  override val epsInf: Double,
  f: Double,
  r: Double,
  epsType: EpsType
) :
  DrudeMetalClustersInAlGaAs,
  MieTheoryLayerOfMetallicClustersInAlGaAs(d, k, x, f, r, epsType)

class MieTheoryLayerOfSbClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  f: Double,
  r: Double,
  epsType: EpsType
) :
  SbClustersInAlGaAs,
  MieTheoryLayerOfMetallicClustersInAlGaAs(d, k, x, f, r, epsType)