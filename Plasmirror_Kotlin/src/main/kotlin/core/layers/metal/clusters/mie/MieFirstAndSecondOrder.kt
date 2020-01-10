package core.layers.metal.clusters.mie

import core.State
import core.layers.metal.clusters.DrudeMetalClustersInAlGaAs
import core.layers.metal.clusters.SbClustersInAlGaAs
import core.layers.semiconductor.AlGaAs
import core.optics.EpsType
import core.optics.metal.clusters.mie.MieFirstAndSecondOrder

abstract class MieFirstAndSecondOrderLayerOfMetalClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  private val f: Double,
  private val r: Double,
  epsType: EpsType
) : MieLayerOfMetalClustersInAlGaAs, AlGaAs(d, k, x, epsType) {
  override val alphaExt: Double
    get() = MieFirstAndSecondOrder.extinctionCoefficient(State.wavelengthCurrent, matrixPermittivity, clusterPermittivity, f, r)
  override val alphaSca: Double
    get() = MieFirstAndSecondOrder.scatteringCoefficient(State.wavelengthCurrent, matrixPermittivity, clusterPermittivity, f, r)
}

class MieFirstAndSecondOrderLayerOfDrudeMetalClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  override val wPlasma: Double,
  override val gammaPlasma: Double,
  override val epsInf: Double,
  f: Double,
  r: Double,
  epsType: EpsType
) : DrudeMetalClustersInAlGaAs, MieFirstAndSecondOrderLayerOfMetalClustersInAlGaAs(d, k, x, f, r, epsType)

class MieFirstAndSecondOrderLayerOfSbClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  f: Double,
  r: Double,
  epsType: EpsType
) : SbClustersInAlGaAs, MieFirstAndSecondOrderLayerOfMetalClustersInAlGaAs(d, k, x, f, r, epsType)