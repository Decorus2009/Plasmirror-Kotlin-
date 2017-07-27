package core.util

import core.Mirror
import core.layers.*
import core.util.Medium.*
import org.apache.commons.math3.complex.Complex.ONE
import kotlin.Double.Companion.POSITIVE_INFINITY


object StructureBuilder {

    @Throws(StructureDescriptionException::class)
    fun build(structureDescription: StructureDescription): Structure {

        val blocks = mutableListOf<Block>()

        structureDescription.blockDescriptions.forEach {

            val repeat = it.repeat.toInt()
            val layers = mutableListOf<Layer>()

            it.layerDescriptions.forEach { layerDescription ->

                val type = layerDescription.type.toInt()
                with(layerDescription.description) {

                    val layer = when (type) {
                        1 -> GaAs(d = parseAt(i = 0))

                        2 -> AlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2))

                        3 -> ConstRefractiveIndexLayer(d = parseAt(i = 0), const_n = parseComplexAt(i = 1))

                        4 -> GaAsExciton(d = parseAt(i = 0),
                                w0 = parseAt(i = 1), gamma0 = parseAt(i = 2), gamma = parseAt(i = 3))

                        5 -> AlGaAsExciton(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2),
                                w0 = parseAt(i = 3), gamma0 = parseAt(i = 4), gamma = parseAt(i = 5))

                        6 -> ConstRefractiveIndexLayerExciton(d = parseAt(i = 0), const_n = parseComplexAt(i = 1),
                                w0 = parseAt(i = 2), gamma0 = parseAt(i = 3), gamma = parseAt(i = 4))

                        7 -> EffectiveMedium(d = parseAt(i = 0), x = parseAt(i = 1),
                                wPlasma = parseAt(i = 2), gammaPlasma = parseAt(i = 3), f = parseAt(i = 4))
/*
                                8 ->

                                9 ->
*/
                        else -> throw StructureDescriptionException("Unknown layer SERIESType")
                    }
                    layers += layer
                }
            }
            blocks += Block(repeat, layers)
        }
        return Structure(blocks)
    }

    private fun List<String>.parseAt(i: Int): Double = this[i].toDouble()

    private fun List<String>.parseComplexAt(i: Int): Cmplx = this[i].toComplex()

    private fun String.toComplex(): Cmplx {
        val (real, imaginary) = this.replace(Regex("[()]"), "").split(";").map { it.toDouble() }
        return Cmplx(real, imaginary)
    }
}


/**
 * Builds mirror basing on left, right media and structure
 */
object MirrorBuilder {

    fun build(structure: Structure,
              leftMedium: Medium, rightMedium: Medium,
              n_left: Cmplx = Cmplx(ONE), n_right: Cmplx = Cmplx(3.6)): Mirror {

        val leftMediumLayer = when (leftMedium) {
            AIR -> ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, const_n = Cmplx(ONE))
            GAAS -> GaAs(d = POSITIVE_INFINITY)
            OTHER -> ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, const_n = n_left)
        }

        val rightMediumLayer = when (rightMedium) {
            AIR -> ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, const_n = Cmplx(ONE))
            GAAS -> GaAs(d = POSITIVE_INFINITY)
            OTHER -> ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, const_n = n_right)
        }

        return Mirror(structure, leftMediumLayer, rightMediumLayer)
    }
}
