package core

import core.State.polarization
import core.layers.ConstRefractiveIndexLayer
import core.util.*
import core.util.Mtrx.Companion.unaryMatrix
import core.util.Polarization.P
import core.util.Polarization.S
import org.apache.commons.math3.complex.Complex
import kotlin.Double.Companion.POSITIVE_INFINITY

/**
 * Mirror: left medium layer + structure + right medium layer
 */
class Mirror(val structure: Structure, val leftMediumLayer: Layer, val rightMediumLayer: Layer) {

    fun computeReflection(): Double {
        val r = r().abs()
        return r * r
    }

    fun computeTransmission(): Double {
        val t = t().abs()

        // просто обращаться к State.n_left нельзя, NPE
        val n1 = leftMediumLayer.n
        val n2 = rightMediumLayer.n

        val cos1 = cosThetaIncident()
        val cos2 = cosThetaInLayer(rightMediumLayer.n)

        return if (polarization === P) {
            ((n2 * cos1) / (n1 * cos2)).abs() * t * t
        } else {
            ((n2 * cos2) / (n1 * cos1)).abs() * t * t
        }
    }

    fun computeAbsorption(): Double = 1.0 - computeReflection() - computeTransmission()

    fun computePermittivity(): Cmplx {
        val layer = structure.blocks[0].layers[0]
        val n = layer.n
        /**
         * eps = refractiveIndex^2 = (refractiveIndex + ik)^2 = refractiveIndex^2 - k^2 + 2ink
         * Re(eps) = refractiveIndex^2 - k^2, Im(eps) = 2nk
         */
        return n * n
    }

    fun computeRefractiveIndex(): Cmplx = structure.blocks[0].layers[0].n

    private fun r(): Cmplx {
        val mirrorMatrix = matrix
        return mirrorMatrix[1, 0] / mirrorMatrix[1, 1] * (-1.0)
    }

    private fun t(): Cmplx {
        val mirrorMatrix = matrix
        return mirrorMatrix.det() / mirrorMatrix[1, 1]
    }


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
    private val matrix: Mtrx
        get() {
            var prev = leftMediumLayer

            var first: Layer = ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, const_n = Cmplx(Complex.NaN))  // blank layer (for formal initialization)
            var beforeFirst: Layer = ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, const_n = Cmplx(Complex.NaN))  // blank layer (for formal initialization)

            var periodMatrix: Mtrx
            var tempMatrix: Mtrx
            var mirrorMatrix: Mtrx = unaryMatrix()

            var isFirst: Boolean
            for (i in 0..structure.blocks.size - 1) {

                with(structure.blocks[i]) {
                    periodMatrix = unaryMatrix()

                    isFirst = true
                    var cur: Layer = ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, const_n = Cmplx(Complex.NaN))  // blank layer (for formal initialization)
                    for (j in 0..layers.size - 1) {

                        cur = layers[j]
                        if (isFirst) {

                            first = cur
                            beforeFirst = prev
                            isFirst = false

                            tempMatrix = unaryMatrix()

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
     * @return interface transfer matrix
     */
    private fun interfaceMatrix(leftLayer: Layer, rightLayer: Layer): Mtrx {

        val n1 = leftLayer.n
        val n2 = rightLayer.n
        /**
         * cos theta in left and right layers are computed using the Snell law.
         * Left and right layers are considered to be next to the left medium (AIR, OTHER, etc.)
         */
        val cos1 = cosThetaInLayer(leftLayer.n)
        val cos2 = cosThetaInLayer(rightLayer.n)

        val polar = polarization

        val n1e: Cmplx = if (polar === S) n1 * cos1 else n1 / cos1
        val n2e: Cmplx = if (polar === S) n2 * cos2 else n2 / cos2

        val interfaceMatrix = Mtrx()

        interfaceMatrix.setDiagonal((n2e + n1e) / (n2e * 2.0))
        interfaceMatrix.setAntiDiagonal((n2e - n1e) / (n2e * 2.0))

        return interfaceMatrix
    }

    private val isASingleLayer: Boolean
        get() = structure.blocks.size == 1 && structure.blocks[0].layers.size == 1
}