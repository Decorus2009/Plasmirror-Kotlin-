package core

import core.optics.Polarization.P
import core.optics.Polarization.S
import core.layers.ConstRefractiveIndexLayer
import core.layers.Layer
import core.optics.cosThetaInLayer
import core.optics.cosThetaIncident
import org.apache.commons.math3.complex.Complex.NaN
import kotlin.Double.Companion.POSITIVE_INFINITY

/**
 * Mirror: left medium layer + structure + right medium layer
 */
class Mirror(val structure: Structure, private val leftMediumLayer: Layer, private val rightMediumLayer: Layer) {

    fun computeReflectance() = with(r().abs()) { this * this }

    fun computeTransmittance(): Double {
        val t = t().abs()

        // просто обращаться к State.n_left нельзя, NPE
        val n1 = leftMediumLayer.n
        val n2 = rightMediumLayer.n

        val cos1 = cosThetaIncident()
        val cos2 = cosThetaInLayer(rightMediumLayer.n)

        return when (State.polarization) {
            P -> ((n2 * cos1) / (n1 * cos2)).abs() * t * t
            else -> ((n2 * cos2) / (n1 * cos1)).abs() * t * t
        }
    }

    fun computeAbsorbance() = 1.0 - computeReflectance() - computeTransmittance()

    fun computeRefractiveIndex() = structure.blocks[0].layers[0].n

    fun computePermittivity() = with(computeRefractiveIndex()) { this * this }

    private fun r() = with(matrix) { this[1, 0] / this[1, 1] * (-1.0) }

    private fun t() = with(matrix) { det() / this[1, 1] }

    /**
     * Странный алгоритм перемножения матриц. Оно происходит не последовательно!
     * Не стал разделять этот метод на вычисление отдельных матриц для блоков, матрицы структуры и т.д.
     * Все делается здесь, как в оригинале, иначе почему-то не работает
     * (возможно, этот как-то связано с некоммутативностью перемножения матриц).
     *
     *
     * Примерный алгоритм:
     * 1. Рассматриваются все блоки последовательно
     * 2. Берется первый блок и верхний слой в нем.
     * Матрица для этого слоя умножается на матрицу интерфейса слева относительно данного слоя. Именно в таком порядке.
     * Матрица интерфейса - единичная, если слой - первый.
     * 3. Рассматривается следующий слой. Аналогично его матрица умножается на матрицу левого интерфейса.
     * 4. Результат из п.3 умножается на результат из п.2 и т.д.
     * Т.е., умножение происходит не совсем линейно.
     * Далее учитывается интерфейс с левой средой и интерфейс с правой средой.
     * Для подробностей см. код, он более-менее human-readable.
     * *
     * @return transfer matrix for mirror
     */
    private val matrix: Matrix_
        get() {
            var prev = leftMediumLayer
            /* blank layer (for formal initialization) */
            var first: Layer = ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = Complex_(NaN))
            /* blank layer (for formal initialization) */
            var beforeFirst: Layer = ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = Complex_(NaN))

            var periodMatrix: Matrix_
            var tempMatrix: Matrix_
            var mirrorMatrix: Matrix_ = Matrix_.unaryMatrix()

            var isFirst: Boolean
            for (i in 0..structure.blocks.size - 1) {

                with(structure.blocks[i]) {
                    periodMatrix = Matrix_.unaryMatrix()

                    isFirst = true
                    var cur: Layer = ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = Complex_(NaN))  // blank layer (for formal initialization)
                    for (j in 0..layers.size - 1) {

                        cur = layers[j]
                        if (isFirst) {

                            first = cur
                            beforeFirst = prev
                            isFirst = false

                            tempMatrix = Matrix_.unaryMatrix()

                        } else {
                            tempMatrix = interfaceMatrix(prev, cur)
                        }

                        tempMatrix = cur.matrix * tempMatrix
                        periodMatrix = tempMatrix * periodMatrix
                        prev = cur
                    }

                    if (repeat > 1) {
                        tempMatrix = interfaceMatrix(cur, first) * periodMatrix
                        tempMatrix = tempMatrix.pow(repeat - 1)
                        periodMatrix *= tempMatrix
                    }

                    periodMatrix *= interfaceMatrix(beforeFirst, first)
                    mirrorMatrix = periodMatrix * mirrorMatrix
                }
            }
            mirrorMatrix = interfaceMatrix(prev, rightMediumLayer) * mirrorMatrix
            return mirrorMatrix
        }

    /**
     * @param leftLayer  layer on the left side of the interface
     * @param rightLayer layer on the right side of the interface
     * @return interface matrix
     */
    private fun interfaceMatrix(leftLayer: Layer, rightLayer: Layer) = Matrix_().apply {
        val n1 = leftLayer.n
        val n2 = rightLayer.n
        /**
         * cos theta in left and right layers are computed using the Snell law.
         * Left and right layers are considered to be next to the left medium (AIR, CUSTOM, etc.)
         */
        val cos1 = cosThetaInLayer(leftLayer.n)
        val cos2 = cosThetaInLayer(rightLayer.n)

        val n1e = when (State.polarization) {
            S -> n1 * cos1
            else -> n1 / cos1
        }
        val n2e = when (State.polarization) {
            S -> n2 * cos2
            else -> n2 / cos2
        }
        setDiagonal((n2e + n1e) / (n2e * 2.0))
        setAntiDiagonal((n2e - n1e) / (n2e * 2.0))
    }
}