package core

import core.optics.EpsType.*
import core.layers.*
import java.lang.IllegalStateException


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

    fun build(structureDescription: StructureDescription) = Structure(structureDescription.blockDescriptions.map {
        Block(it.repeat.toInt(), it.layerDescriptions.map { ld -> layer(ld.type, ld.description) })
    })

    private fun layer(type: String, description: List<String>): Layer = with(description) {
        return when (type) {

            // TODO generalize


            "1-1" -> GaAs(
                    d = parseAt(i = 0),
                    epsType = ADACHI
            )
            "1-2" -> GaAs(
                    d = parseAt(i = 0),
                    epsType = GAUSS
            )
            "1-3" -> GaAs(
                    d = parseAt(i = 0),
                    epsType = GAUSS_WITH_VARIABLE_IM_PERMITTIVITY_BELOW_E0
            )
//                        "1-3" -> GaAs(d = parseAt(i = 0), epsType = GAUSS_ADACHI)

            "2-1" -> AlGaAs(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    epsType = ADACHI
            )
            "2-2" -> AlGaAs(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    epsType = GAUSS
            )
            "2-3" -> AlGaAs(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    epsType = GAUSS_WITH_VARIABLE_IM_PERMITTIVITY_BELOW_E0
            )
//                        "2-3" -> AlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), epsType = GAUSS_ADACHI)

            "3" -> ConstRefractiveIndexLayer(
                    d = parseAt(i = 0),
                    n = parseComplexAt(i = 1)
            )

            "4-1" -> GaAsExcitonic(
                    d = parseAt(i = 0),
                    w0 = parseAt(i = 1),
                    gamma0 = parseAt(i = 2),
                    gamma = parseAt(i = 3),
                    epsType = ADACHI
            )
            "4-2" -> GaAsExcitonic(
                    d = parseAt(i = 0),
                    w0 = parseAt(i = 1),
                    gamma0 = parseAt(i = 2),
                    gamma = parseAt(i = 3),
                    epsType = GAUSS
            )
            "4-3" -> GaAsExcitonic(
                    d = parseAt(i = 0),
                    w0 = parseAt(i = 1),
                    gamma0 = parseAt(i = 2),
                    gamma = parseAt(i = 3),
                    epsType = GAUSS_WITH_VARIABLE_IM_PERMITTIVITY_BELOW_E0
            )
//                        "4-3" -> GaAsExcitonic(d = parseAt(i = 0), w0 = parseAt(i = 1), gamma0 = parseAt(i = 2), gamma = parseAt(i = 3), epsType = GAUSS_ADACHI)

            "5-1" -> AlGaAsExcitonic(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    w0 = parseAt(i = 3),
                    gamma0 = parseAt(i = 4),
                    gamma = parseAt(i = 5),
                    epsType = ADACHI
            )
            "5-2" -> AlGaAsExcitonic(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    w0 = parseAt(i = 3),
                    gamma0 = parseAt(i = 4),
                    gamma = parseAt(i = 5),
                    epsType = GAUSS
            )
            "5-3" -> AlGaAsExcitonic(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    w0 = parseAt(i = 3),
                    gamma0 = parseAt(i = 4),
                    gamma = parseAt(i = 5),
                    epsType = GAUSS_WITH_VARIABLE_IM_PERMITTIVITY_BELOW_E0
            )
//                        "5-3" -> AlGaAsExcitonic(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), w0 = parseAt(i = 3), gamma0 = parseAt(i = 4), gamma = parseAt(i = 5), epsType = GAUSS_ADACHI)

            "6" -> ConstRefractiveIndexLayerExcitonic(
                    d = parseAt(i = 0),
                    n = parseComplexAt(i = 1),
                    w0 = parseAt(i = 2),
                    gamma0 = parseAt(i = 3),
                    gamma = parseAt(i = 4)
            )

            "7-1-1" -> EffectiveMediumLayerOfDrudeMetalClustersInAlGaAs(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    wPlasma = parseAt(i = 3),
                    gammaPlasma = parseAt(i = 4),
                    epsInf = parseAt(i = 5),
                    f = parseAt(i = 6),
                    epsType = ADACHI
            )

            "7-2-1" -> EffectiveMediumLayerOfDrudeMetalClustersInAlGaAs(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    wPlasma = parseAt(i = 3),
                    gammaPlasma = parseAt(i = 4),
                    epsInf = parseAt(i = 5),
                    f = parseAt(i = 6),
                    epsType = GAUSS
            )

            "7-3-1" -> EffectiveMediumLayerOfDrudeMetalClustersInAlGaAs(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    wPlasma = parseAt(i = 3),
                    gammaPlasma = parseAt(i = 4),
                    epsInf = parseAt(i = 5),
                    f = parseAt(i = 6),
                    epsType = GAUSS_WITH_VARIABLE_IM_PERMITTIVITY_BELOW_E0
            )

            "7-1-2" -> EffectiveMediumLayerOfSbClustersInAlGaAs(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    f = parseAt(i = 3),
                    epsType = ADACHI
            )

            "7-2-2" -> EffectiveMediumLayerOfSbClustersInAlGaAs(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    f = parseAt(i = 3),
                    epsType = GAUSS
            )

            "7-3-2" -> EffectiveMediumLayerOfSbClustersInAlGaAs(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    f = parseAt(i = 3),
                    epsType = GAUSS_WITH_VARIABLE_IM_PERMITTIVITY_BELOW_E0
            )

//                        "7-3" -> EffectiveMediumLayerOfDrudeMetalClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), wPlasma = parseAt(i = 3), gammaPlasma = parseAt(i = 4), f = parseAt(i = 5), epsInf = parseAt(i = 6), epsType = GAUSS_ADACHI)

            "8-1" -> TwoDimensionalLayerOfDrudeMetalClustersInAlGaAs(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    latticeFactor = parseAt(i = 3),
                    wPlasma = parseAt(i = 4),
                    gammaPlasma = parseAt(i = 5),
                    epsInf = parseAt(i = 6),
                    epsType = ADACHI
            )

            "8-2" -> TwoDimensionalLayerOfDrudeMetalClustersInAlGaAs(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    latticeFactor = parseAt(i = 3),
                    wPlasma = parseAt(i = 4),
                    gammaPlasma = parseAt(i = 5),
                    epsInf = parseAt(i = 6),
                    epsType = GAUSS
            )

            "8-3" -> TwoDimensionalLayerOfDrudeMetalClustersInAlGaAs(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    latticeFactor = parseAt(i = 3),
                    wPlasma = parseAt(i = 4),
                    gammaPlasma = parseAt(i = 5),
                    epsInf = parseAt(i = 6),
                    epsType = GAUSS_WITH_VARIABLE_IM_PERMITTIVITY_BELOW_E0
            )

//                        "8-3" -> TwoDimensionalLayerOfDrudeMetalClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), wPlasma = parseAt(i = 4), gammaPlasma = parseAt(i = 5), epsInf = parseAt(i = 6), epsType = GAUSS_ADACHI)

            "9-1" -> TwoDimensionalLayerOfSbClustersInAlGaAs(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    latticeFactor = parseAt(i = 3),
                    epsType = ADACHI
            )
            "9-2" -> TwoDimensionalLayerOfSbClustersInAlGaAs(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    latticeFactor = parseAt(i = 3),
                    epsType = GAUSS
            )
            "9-3" -> TwoDimensionalLayerOfSbClustersInAlGaAs(
                    d = parseAt(i = 0),
                    k = parseAt(i = 1),
                    x = parseAt(i = 2),
                    latticeFactor = parseAt(i = 3),
                    epsType = GAUSS_WITH_VARIABLE_IM_PERMITTIVITY_BELOW_E0
            )

//                        "9-3" -> TwoDimensionalLayerOfSbClustersInAlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), epsType = GAUSS_ADACHI)

            /* must never be reached because of validating procedure */
            else -> throw IllegalStateException()
        }
    }

    private fun List<String>.parseAt(i: Int) = this[i].toDouble()

    private fun List<String>.parseComplexAt(i: Int) = this[i].toComplex()

    private fun String.toComplex(): Complex_ {
        val (real, imaginary) = replace(Regex("[()]"), "").split(";").map { it.toDouble() }
        return Complex_(real, imaginary)
    }
}
