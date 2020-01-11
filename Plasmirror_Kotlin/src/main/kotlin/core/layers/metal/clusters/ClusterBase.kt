package core.layers.metal.clusters

import core.Complex_
import core.State
import core.layers.semiconductor.AlGaAsLayer
import core.optics.metal.clusters.DrudeModel
import core.optics.metal.clusters.SbTabulated
import core.optics.semiconductor.AlGaAsMatrix

interface MetalClustersInAlGaAs : AlGaAsLayer {
  val matrixPermittivity
    get() = AlGaAsMatrix.permittivity(State.wavelengthCurrent, k, x, epsType)
  val clusterPermittivity: Complex_
}

interface DrudeMetalClustersInAlGaAs : MetalClustersInAlGaAs {
  val wPlasma: Double
  val gammaPlasma: Double
  val epsInf: Double
  override val clusterPermittivity
    get() = DrudeModel.permittivity(State.wavelengthCurrent, wPlasma, gammaPlasma, epsInf)
}

interface SbClustersInAlGaAs : MetalClustersInAlGaAs {
  override val clusterPermittivity
    get() = SbTabulated.permittivity(State.wavelengthCurrent)
}
