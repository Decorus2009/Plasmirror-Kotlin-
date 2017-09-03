package core

import core.Complex_.Companion.ONE
import core.EpsType.*
import core.Medium.*
import core.State.n_left
import core.State.n_right
import core.layers.*
import kotlin.Double.Companion.POSITIVE_INFINITY


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
class Structure(val blocks: List<Block>)


object StructureBuilder {

    fun build(structureDescription: StructureDescription): Structure {
        val blocks = mutableListOf<Block>()

        structureDescription.blockDescriptions.forEach {
            val repeat = it.repeat.toInt()
            val layers = mutableListOf<Layer>()

            it.layerDescriptions.forEach { layerDescription ->
                val type = layerDescription.type

                with(layerDescription.description) {
                    val layer = when (type) {
                        "1-1" -> GaAs(d = parseAt(i = 0), eps_Type = ADACHI)
                        "1-2" -> GaAs(d = parseAt(i = 0), eps_Type = GAUSS)
                        "1-3" -> GaAs(d = parseAt(i = 0), eps_Type = GAUSS_ADACHI)

                        "2-1" -> AlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), eps_type = ADACHI)
                        "2-2" -> AlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), eps_type = GAUSS)
                        "2-3" -> AlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), eps_type = GAUSS_ADACHI)

                        "3" -> ConstRefractiveIndexLayer(d = parseAt(i = 0), const_n = parseComplexAt(i = 1))

                        "4-1" -> GaAsExciton(d = parseAt(i = 0), w0 = parseAt(i = 1), gamma0 = parseAt(i = 2), gamma = parseAt(i = 3), eps_type = ADACHI)
                        "4-2" -> GaAsExciton(d = parseAt(i = 0), w0 = parseAt(i = 1), gamma0 = parseAt(i = 2), gamma = parseAt(i = 3), eps_type = GAUSS)
                        "4-3" -> GaAsExciton(d = parseAt(i = 0), w0 = parseAt(i = 1), gamma0 = parseAt(i = 2), gamma = parseAt(i = 3), eps_type = GAUSS_ADACHI)

                        "5-1" -> AlGaAsExciton(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), w0 = parseAt(i = 3), gamma0 = parseAt(i = 4), gamma = parseAt(i = 5), eps_type = ADACHI)
                        "5-2" -> AlGaAsExciton(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), w0 = parseAt(i = 3), gamma0 = parseAt(i = 4), gamma = parseAt(i = 5), eps_type = GAUSS)
                        "5-3" -> AlGaAsExciton(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), w0 = parseAt(i = 3), gamma0 = parseAt(i = 4), gamma = parseAt(i = 5), eps_type = GAUSS_ADACHI)

                        "6" -> ConstRefractiveIndexLayerExciton(d = parseAt(i = 0), const_n = parseComplexAt(i = 1), w0 = parseAt(i = 2), gamma0 = parseAt(i = 3), gamma = parseAt(i = 4))

                        "7-1" -> EffectiveMedium(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), w_plasma = parseAt(i = 3), gamma_plasma = parseAt(i = 4), f = parseAt(i = 5), eps_inf = parseAt(i = 6), eps_type = ADACHI)
                        "7-2" -> EffectiveMedium(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), w_plasma = parseAt(i = 3), gamma_plasma = parseAt(i = 4), f = parseAt(i = 5), eps_inf = parseAt(i = 6), eps_type = GAUSS)
                        "7-3" -> EffectiveMedium(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), w_plasma = parseAt(i = 3), gamma_plasma = parseAt(i = 4), f = parseAt(i = 5), eps_inf = parseAt(i = 6), eps_type = GAUSS_ADACHI)

                        "8-1" -> NanoparticlesLayer(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), w_plasma = parseAt(i = 4), gamma_plasma = parseAt(i = 5), eps_inf = parseAt(i = 6), eps_type = ADACHI)
                        "8-2" -> NanoparticlesLayer(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), w_plasma = parseAt(i = 4), gamma_plasma = parseAt(i = 5), eps_inf = parseAt(i = 6), eps_type = GAUSS)
                        "8-3" -> NanoparticlesLayer(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), latticeFactor = parseAt(i = 3), w_plasma = parseAt(i = 4), gamma_plasma = parseAt(i = 5), eps_inf = parseAt(i = 6), eps_type = GAUSS_ADACHI)
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


/**
 * Builds mirror basing on left, right media and structure
 */
object MirrorBuilder {
    fun build(structure: Structure,
              leftMedium: Medium, rightMedium: Medium): Mirror {
        val leftMediumLayer = when (leftMedium) {
            AIR -> ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, const_n = ONE)
            GAAS -> GaAs(d = POSITIVE_INFINITY, eps_Type = GAUSS)
            OTHER -> ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, const_n = n_left)
        }
        val rightMediumLayer = when (rightMedium) {
            AIR -> ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, const_n = ONE)
            GAAS -> GaAs(d = POSITIVE_INFINITY, eps_Type = GAUSS)
            OTHER -> ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, const_n = n_right)
        }
        return Mirror(structure, leftMediumLayer, rightMediumLayer)
    }
}
