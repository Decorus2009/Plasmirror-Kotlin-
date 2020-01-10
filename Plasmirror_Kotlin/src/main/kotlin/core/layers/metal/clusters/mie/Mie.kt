package core.layers.metal.clusters.mie

import core.layers.metal.clusters.MetalClustersInAlGaAs

interface MieLayerOfMetalClustersInAlGaAs : MetalClustersInAlGaAs {
  val alphaSca: Double
  override val alphaExt: Double
}