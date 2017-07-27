package core.util

import org.apache.commons.math3.complex.Complex

/**
 * Abstract layer
 * @property d thickness
 * @property n refractive index
 * @property matrix transfer matrix
 */
abstract class Layer(protected var d: Double,
                     open var n: Cmplx = Cmplx(Complex.NaN),
                     open val matrix: Mtrx = Mtrx.emptyMatrix())


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
