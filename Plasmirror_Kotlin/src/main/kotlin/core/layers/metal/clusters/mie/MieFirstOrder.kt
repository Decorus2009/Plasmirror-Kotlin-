package core.layers.metal.clusters.mie

import core.State
import core.layers.metal.clusters.DrudeMetalClustersInAlGaAs
import core.layers.metal.clusters.SbClustersInAlGaAs
import core.layers.semiconductor.AlGaAs
import core.optics.EpsType
import core.optics.metal.clusters.mie.MieFirstOrder

abstract class MieFirstOrderLayerOfMetalClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  private val f: Double,
  private val r: Double,
  epsType: EpsType
) : MieLayerOfMetalClustersInAlGaAs, AlGaAs(d, k, x, epsType) {
  override val extinctionCoefficient: Double
    get() = MieFirstOrder.extinctionCoefficient(State.wavelengthCurrent, matrixPermittivity, clusterPermittivity, f, r)
  override val scatteringCoefficient: Double
    get() = MieFirstOrder.scatteringCoefficient(State.wavelengthCurrent, matrixPermittivity, clusterPermittivity, f, r)
}

class MieFirstOrderLayerOfDrudeMetalClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  override val wPlasma: Double,
  override val gammaPlasma: Double,
  override val epsInf: Double,
  f: Double,
  r: Double,
  epsType: EpsType
) : DrudeMetalClustersInAlGaAs, MieFirstOrderLayerOfMetalClustersInAlGaAs(d, k, x, f, r, epsType)

class MieFirstOrderLayerOfSbClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  f: Double,
  r: Double,
  epsType: EpsType
) : SbClustersInAlGaAs, MieFirstOrderLayerOfMetalClustersInAlGaAs(d, k, x, f, r, epsType)