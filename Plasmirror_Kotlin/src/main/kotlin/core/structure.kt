package core

import core.layers.metal.clusters.*
import core.layers.metal.clusters.mie.MieFullLayerOfDrudeMetalClustersInAlGaAs
import core.layers.metal.clusters.mie.MieFullLayerOfSbClustersInAlGaAs
import core.layers.semiconductor.*
import core.optics.EpsType
import core.optics.EpsType.*


class LayerDescription(val type: String, val description: List<String>)

class BlockDescription(
  val repeat: String,
  val layerDescriptions: MutableList<LayerDescription> = mutableListOf()
)

class StructureDescription(val blockDescriptions: MutableList<BlockDescription> = mutableListOf())

/**
 * Period is a sequence of layers
 * Block is the sequence of @code periods periods
 *
 * @param repeat number of periods
 * @param layers    list of layerDescriptions in a period
 */
class Block(val repeat: Int, val layers: List<Layer>)

/**
 * Structure is a sequence of blocks
 */
class Structure(var blocks: List<Block>)

object StructureBuilder {
  fun build(structureDescription: StructureDescription) = Structure(structureDescription.blockDescriptions.map {
    Block(it.repeat.toInt(), it.layerDescriptions.map { layerDescription -> layer(layerDescription) })
  })

  private fun layer(layerDescription: LayerDescription): Layer {
    val types = layerDescription.type
    with(layerDescription.description) {
      val typesList = types.split("-")
      val layerType = typesList[0]
      val matrixType = epsType(typesList[1])

      return when (typesList.size) {
        1 -> {
          when (layerType) {
            "3" -> constRefractiveIndexLayer(this)
            "6" -> constRefractiveIndexLayerExcitonic(this)
            else -> throw IllegalStateException("Unknown const refractive index layer (must never be reached)")
          }
        }
        2 -> {
          when (layerType) {
            "1" -> GaAs(this, matrixType)
            "2" -> AlGaAs(this, matrixType)
            "4" -> GaAsExcitonic(this, matrixType)
            "5" -> AlGaAsExcitonic(this, matrixType)
            else -> throw IllegalStateException("Unknown GaAs or AlGaAs layer (must never be reached)")
          }
        }
        3 -> {
          val metalClustersType = typesList[2]
          when (layerType) {
            "7" -> {
              when (metalClustersType) {
                "1" -> effectiveMediumLayerOfDrudeMetalClustersInAlGaAs(this, matrixType)
                "2" -> effectiveMediumLayerOfSbClustersInAlGaAs(this, matrixType)
                else -> throw IllegalStateException("Unknown effective medium layer (must never be reached)")
              }
            }
            "8" -> {
              when (metalClustersType) {
                "1" -> mieTheoryLayerOfDrudeMetalClustersInAlGaAs(this, matrixType)
                "2" -> mieTheoryLayerOfSbClustersInAlGaAs(this, matrixType)
                else -> throw IllegalStateException("Unknown Mie theory layer of metal clusters in AlGaAs (must never be reached)")
              }
            }
            "9" -> {
              when (metalClustersType) {
                "1" -> twoDimensionalLayerOfDrudeMetalClustersInAlGaAs(this, matrixType)
                "2" -> twoDimensionalLayerOfSbClustersInAlGaAs(this, matrixType)
                else -> throw IllegalStateException("Unknown two-dimensional layer of metal clusters in AlGaAs (must never be reached)"
                )
              }
            }
            else -> throw IllegalStateException("Unknown AlGaAs layer with metal clusters (must never be reached)")
          }
        }
        else -> throw IllegalStateException("Unknown layer (must never be reached)")
      }
    }
  }

  // type = 1-1, type = 1-2, type = 1-3
  private fun GaAs(description: List<String>, epsType: EpsType) = with(description) {
    GaAs(d = parseAt(i = 0), epsType = epsType)
  }

  // type = 2-1, type = 2-2, type = 2-3
  private fun AlGaAs(description: List<String>, epsType: EpsType) = with(description) {
    AlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), epsType = epsType)
  }

  // type = 3
  private fun constRefractiveIndexLayer(description: List<String>) = with(description) {
    ConstRefractiveIndexLayer(d = parseAt(i = 0), n = parseComplexAt(i = 1))
  }

  // type = 4-1, type = 4-2, type = 4-3
  private fun GaAsExcitonic(description: List<String>, epsType: EpsType) = with(description) {
    GaAsExcitonic(
      d = parseAt(i = 0),
      w0 = parseAt(i = 1),
      gamma0 = parseAt(i = 2),
      gamma = parseAt(i = 3),
      epsType = epsType
    )
  }

  // type = 5-1, type = 5-2, type = 5-3
  private fun AlGaAsExcitonic(description: List<String>, epsType: EpsType) = with(description) {
    AlGaAsExcitonic(
      d = parseAt(i = 0),
      k = parseAt(i = 1),
      x = parseAt(i = 2),
      w0 = parseAt(i = 3),
      gamma0 = parseAt(i = 4),
      gamma = parseAt(i = 5),
      epsType = epsType
    )
  }

  // type = 6
  private fun constRefractiveIndexLayerExcitonic(description: List<String>) = with(description) {
    ConstRefractiveIndexLayerExcitonic(
      d = parseAt(i = 0),
      n = parseComplexAt(i = 1),
      w0 = parseAt(i = 2),
      gamma0 = parseAt(i = 3),
      gamma = parseAt(i = 4)
    )
  }

  // type = 7-1-1, type = 7-2-1, type = 7-3-1
  private fun effectiveMediumLayerOfDrudeMetalClustersInAlGaAs(description: List<String>, epsType: EpsType) = with(description) {
    EffectiveMediumLayerOfDrudeMetalClustersInAlGaAs(
      d = parseAt(i = 0),
      k = parseAt(i = 1),
      x = parseAt(i = 2),
      wPlasma = parseAt(i = 3),
      gammaPlasma = parseAt(i = 4),
      epsInf = parseAt(i = 5),
      f = parseAt(i = 6),
      epsType = epsType
    )
  }

  // type = 7-1-2, type = 7-2-2, type = 7-3-2
  private fun effectiveMediumLayerOfSbClustersInAlGaAs(description: List<String>, epsType: EpsType) = with(description) {
    EffectiveMediumLayerOfSbClustersInAlGaAs(
      d = parseAt(i = 0),
      k = parseAt(i = 1),
      x = parseAt(i = 2),
      f = parseAt(i = 3),
      epsType = epsType
    )
  }

  // type = 8-1-1, type = 8-2-1, type = 8-3-1
  private fun mieTheoryLayerOfDrudeMetalClustersInAlGaAs(description: List<String>, epsType: EpsType) = with(description) {
    MieFullLayerOfDrudeMetalClustersInAlGaAs(
      d = parseAt(i = 0),
      k = parseAt(i = 1),
      x = parseAt(i = 2),
      wPlasma = parseAt(i = 3),
      gammaPlasma = parseAt(i = 4),
      epsInf = parseAt(i = 5),
      f = parseAt(i = 6),
      r = parseAt(i = 7),
      epsType = epsType
    )
  }

  // type = 8-1-2, type = 8-2-2, type = 8-3-2
  private fun mieTheoryLayerOfSbClustersInAlGaAs(description: List<String>, epsType: EpsType) = with(description) {
    MieFullLayerOfSbClustersInAlGaAs(
      d = parseAt(i = 0),
      k = parseAt(i = 1),
      x = parseAt(i = 2),
      f = parseAt(i = 3),
      r = parseAt(i = 4),
      epsType = epsType
    )
  }

  // type = 9-1-1, type = 9-2-1, type = 9-3-1
  private fun twoDimensionalLayerOfDrudeMetalClustersInAlGaAs(description: List<String>, epsType: EpsType) = with(description) {
    TwoDimensionalLayerOfDrudeMetalClustersInAlGaAs(
      d = parseAt(i = 0),
      k = parseAt(i = 1),
      x = parseAt(i = 2),
      latticeFactor = parseAt(i = 3),
      wPlasma = parseAt(i = 4),
      gammaPlasma = parseAt(i = 5),
      epsInf = parseAt(i = 6),
      epsType = epsType
    )
  }

  // type = 9-1-2, type = 9-2-2, type = 9-3-2
  private fun twoDimensionalLayerOfSbClustersInAlGaAs(description: List<String>, epsType: EpsType) = with(description) {
    TwoDimensionalLayerOfSbClustersInAlGaAs(
      d = parseAt(i = 0),
      k = parseAt(i = 1),
      x = parseAt(i = 2),
      latticeFactor = parseAt(i = 3),
      epsType = epsType
    )
  }

  private fun epsType(epsTypeCode: String) = when (epsTypeCode) {
    "1" -> ADACHI
    "2" -> GAUSS
    "3" -> GAUSS_WITH_VARIABLE_IM_PERMITTIVITY_BELOW_E0
    else -> throw IllegalArgumentException("Unknown epsType (must never be reached)")
  }

  private fun List<String>.parseAt(i: Int) = this[i].toDouble()

  private fun List<String>.parseComplexAt(i: Int) = this[i].toComplex()

  private fun String.toComplex(): Complex_ {
    val (real, imaginary) = replace(Regex("[()]"), "").split(";").map { it.toDouble() }
    return Complex_(real, imaginary)
  }
}