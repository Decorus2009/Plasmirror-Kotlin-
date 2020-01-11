package core.layers.metal.clusters.mie

import core.State
import core.layers.metal.clusters.DrudeMetalClustersInAlGaAs
import core.layers.metal.clusters.SbClustersInAlGaAs
import core.layers.semiconductor.AlGaAs
import core.optics.EpsType
import core.optics.metal.clusters.mie.MieFull

abstract class MieFullLayerOfMetalClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  private val f: Double,
  private val r: Double,
  epsType: EpsType
) : MieLayerOfMetalClustersInAlGaAs, AlGaAs(d, k, x, epsType) {
  // value of AlGaAs refractive index is used as n-property here
  // Mie theory is for the computation of extinction and scattering, not for the computation of refractive index
  override val extinctionCoefficient: Double
    get() = MieFull.extinctionCoefficient(State.wavelengthCurrent, matrixPermittivity, clusterPermittivity, f, r)
  override val scatteringCoefficient: Double
    get() = MieFull.scatteringCoefficient(State.wavelengthCurrent, matrixPermittivity, clusterPermittivity, f, r)
}

class MieFullLayerOfDrudeMetalClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  override val wPlasma: Double,
  override val gammaPlasma: Double,
  override val epsInf: Double,
  f: Double,
  r: Double,
  epsType: EpsType
) : DrudeMetalClustersInAlGaAs, MieFullLayerOfMetalClustersInAlGaAs(d, k, x, f, r, epsType)

class MieFullLayerOfSbClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  f: Double,
  r: Double,
  epsType: EpsType
) : SbClustersInAlGaAs, MieFullLayerOfMetalClustersInAlGaAs(d, k, x, f, r, epsType)