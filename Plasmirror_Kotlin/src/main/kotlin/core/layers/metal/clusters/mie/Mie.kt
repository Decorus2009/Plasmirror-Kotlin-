package core.layers.metal.clusters.mie

import core.layers.metal.clusters.MetalClustersInAlGaAs

interface MieLayerOfMetalClustersInAlGaAs : MetalClustersInAlGaAs {
  val scatteringCoefficient: Double
  override val extinctionCoefficient: Double
}