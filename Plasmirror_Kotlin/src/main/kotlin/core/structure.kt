package core

import core.EpsType.*
import core.layers.*


class LayerDescription(val type: String, val description: List<String>)

class BlockDescription(val repeat: String,
                       val layerDescriptions: MutableList<LayerDescription> = mutableListOf<LayerDescription>())

class StructureDescription(val blockDescriptions: MutableList<BlockDescription> = mutableListOf<BlockDescription>())


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

    // TODO use this in Kotlin style
/*
fun build(structureDescription: StructureDescription) = Structure(mutableListOf()).apply {
        structureDescription.blockDescriptions.forEach {
            val repeat = it.repeat.toInt()
            val layers = mutableListOf<Layer>()

            it.layerDescriptions.forEach { layerDescription ->
                val type = layerDescription.type

                with(layerDescription.description) {
                    val layer: Layer = when (type) {
                        "1-1" -> GaAs(d = parseAt(i = 0), epsType = ADACHI)
                        "1-2" -> GaAs(d = parseAt(i = 0), epsType = GAUSS)
                        "1-3" -> GaAs(d = parseAt(i = 0), epsType = GAUSS_ADACHI)

                        "2-1" -> AlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), epsType = ADACHI)
                        "2-2" -> AlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), epsType = GAUSS)
                        "2-3" -> AlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), epsType = GAUSS_ADACHI)

                        "3" -> ConstRefractiveIndexLayer(d = parseAt(i = 0), n = parseComplexAt(i = 1))

                        "4-1" -> GaAsExcitonic(d = parseAt(i = 0), w0 = parseAt(i = 1), gamma0 = parseAt(i = 2), gamma = parseAt(i = 3), epsType = ADACHI)
                        "4-2" -> GaAsExcitonic(d = parseAt(i = 0), w0 = parseAt(i = 1), gamma0 = parseAt(i = 2), gamma = parseAt(i = 3), epsType = GAUSS)
                        "4-3" -> GaAsExcitonic(d = parseAt(i = 0), w0 = parseAt(i = 1), gamma0 = parseAt(i = 2), gamma = parseAt(i = 3), epsType = GAUSS_ADACHI)

                        "5-1" -> AlGaAsExcitonic(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), w0 = parseAt(i = 3), gamma0 = parseAt(i = 4), gamma = parseAt(i = 5), epsType = ADACHI)
                        "5-2" -> AlGaAsExcitonic(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), w0 = parseAt(i = 3), gamma0 = parseAt(i = 4), gamma = parseAt(i = 5), epsType = GAUSS)
                        "5-3" -> AlGaAsExcitonic(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), w0 = parseAt(i = 3), gamma0 = parseAt(i = 4), gamma = parseAt(i = 5), epsType = GAUSS_ADACHI)

                        "6" -> ConstRefractiveIndexLayerExcitonic(d = parseAt(i = 0), n = parseComplexAt(i = 1), w0 = parseAt(i = 2), gamma0 = parseAt(i = 3), gamma = parseAt(i = 4))

                        "7-1" -> EffectiveMediumForDrudeMetalClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), wPlasma = parseAt(i = 3), gammaPlasma = parseAt(i = 4), f = parseAt(i = 5), epsInf = parseAt(i = 6), epsType = ADACHI)
                        "7-2" -> EffectiveMediumForDrudeMetalClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), wPlasma = parseAt(i = 3), gammaPlasma = parseAt(i = 4), f = parseAt(i = 5), epsInf = parseAt(i = 6), epsType = GAUSS)
                        "7-3" -> EffectiveMediumForDrudeMetalClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), wPlasma = parseAt(i = 3), gammaPlasma = parseAt(i = 4), f = parseAt(i = 5), epsInf = parseAt(i = 6), epsType = GAUSS_ADACHI)

                        "8-1" -> PerssonModelForDrudeMetalClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), wPlasma = parseAt(i = 4), gammaPlasma = parseAt(i = 5), epsInf = parseAt(i = 6), epsType = ADACHI)
                        "8-2" -> PerssonModelForDrudeMetalClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), wPlasma = parseAt(i = 4), gammaPlasma = parseAt(i = 5), epsInf = parseAt(i = 6), epsType = GAUSS)
                        "8-3" -> PerssonModelForDrudeMetalClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), wPlasma = parseAt(i = 4), gammaPlasma = parseAt(i = 5), epsInf = parseAt(i = 6), epsType = GAUSS_ADACHI)

                        "9-1" -> PerssonModelForSbClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), epsType = ADACHI)
                        "9-2" -> PerssonModelForSbClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), epsType = GAUSS)
                        "9-3" -> PerssonModelForSbClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), epsType = GAUSS_ADACHI)

                        *//* must never be reached because of validating procedure *//*
                        else -> return@with
                    }
                    layers += layer
                }
            }
            blocks += Block(repeat, layers)
        }
    }
*/

    fun build(structureDescription: StructureDescription): Structure {
        val blocks = mutableListOf<Block>()

        structureDescription.blockDescriptions.forEach {
            val repeat = it.repeat.toInt()
            val layers = mutableListOf<Layer>()

            it.layerDescriptions.forEach { layerDescription ->
                val type = layerDescription.type

                with(layerDescription.description) {
                    val layer: Layer = when (type) {
                        "1-1" -> GaAs(d = parseAt(i = 0), epsType = ADACHI)
                        "1-2" -> GaAs(d = parseAt(i = 0), epsType = GAUSS)
                        "1-3" -> GaAs(d = parseAt(i = 0), epsType = GAUSS_ADACHI)

                        "2-1" -> AlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), epsType = ADACHI)
                        "2-2" -> AlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), epsType = GAUSS)
                        "2-3" -> AlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), epsType = GAUSS_ADACHI)

                        "3" -> ConstRefractiveIndexLayer(d = parseAt(i = 0), n = parseComplexAt(i = 1))

                        "4-1" -> GaAsExcitonic(d = parseAt(i = 0), w0 = parseAt(i = 1), gamma0 = parseAt(i = 2), gamma = parseAt(i = 3), epsType = ADACHI)
                        "4-2" -> GaAsExcitonic(d = parseAt(i = 0), w0 = parseAt(i = 1), gamma0 = parseAt(i = 2), gamma = parseAt(i = 3), epsType = GAUSS)
                        "4-3" -> GaAsExcitonic(d = parseAt(i = 0), w0 = parseAt(i = 1), gamma0 = parseAt(i = 2), gamma = parseAt(i = 3), epsType = GAUSS_ADACHI)

                        "5-1" -> AlGaAsExcitonic(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), w0 = parseAt(i = 3), gamma0 = parseAt(i = 4), gamma = parseAt(i = 5), epsType = ADACHI)
                        "5-2" -> AlGaAsExcitonic(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), w0 = parseAt(i = 3), gamma0 = parseAt(i = 4), gamma = parseAt(i = 5), epsType = GAUSS)
                        "5-3" -> AlGaAsExcitonic(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), w0 = parseAt(i = 3), gamma0 = parseAt(i = 4), gamma = parseAt(i = 5), epsType = GAUSS_ADACHI)

                        "6" -> ConstRefractiveIndexLayerExcitonic(d = parseAt(i = 0), n = parseComplexAt(i = 1), w0 = parseAt(i = 2), gamma0 = parseAt(i = 3), gamma = parseAt(i = 4))

                        "7-1" -> EffectiveMediumForDrudeMetalClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), wPlasma = parseAt(i = 3), gammaPlasma = parseAt(i = 4), f = parseAt(i = 5), epsInf = parseAt(i = 6), epsType = ADACHI)
                        "7-2" -> EffectiveMediumForDrudeMetalClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), wPlasma = parseAt(i = 3), gammaPlasma = parseAt(i = 4), f = parseAt(i = 5), epsInf = parseAt(i = 6), epsType = GAUSS)
                        "7-3" -> EffectiveMediumForDrudeMetalClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), wPlasma = parseAt(i = 3), gammaPlasma = parseAt(i = 4), f = parseAt(i = 5), epsInf = parseAt(i = 6), epsType = GAUSS_ADACHI)

                        "8-1" -> PerssonModelForDrudeMetalClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), wPlasma = parseAt(i = 4), gammaPlasma = parseAt(i = 5), epsInf = parseAt(i = 6), epsType = ADACHI)
                        "8-2" -> PerssonModelForDrudeMetalClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), wPlasma = parseAt(i = 4), gammaPlasma = parseAt(i = 5), epsInf = parseAt(i = 6), epsType = GAUSS)
                        "8-3" -> PerssonModelForDrudeMetalClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), wPlasma = parseAt(i = 4), gammaPlasma = parseAt(i = 5), epsInf = parseAt(i = 6), epsType = GAUSS_ADACHI)

                        "9-1" -> PerssonModelForSbClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), epsType = ADACHI)
                        "9-2" -> PerssonModelForSbClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), epsType = GAUSS)
                        "9-3" -> PerssonModelForSbClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), epsType = GAUSS_ADACHI)

                        /* must never be reached because of validating procedure */
                        else -> return@with
                    }
                    layers += layer
                }
            }
            blocks += Block(repeat, layers)
        }
        return Structure(blocks)
    }

    private fun List<String>.parseAt(i: Int): Double = this[i].toDouble()

    private fun List<String>.parseComplexAt(i: Int): Complex_ = this[i].toComplex()

    private fun String.toComplex(): Complex_ {
        val (real, imaginary) = replace(Regex("[()]"), "").split(";").map { it.toDouble() }
        return Complex_(real, imaginary)
    }
}
