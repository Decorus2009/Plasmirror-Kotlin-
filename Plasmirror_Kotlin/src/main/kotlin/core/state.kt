package core

import core.Regime.*
import core.StateValidator.initStateUsing
import core.ValidateResult.FAILURE
import core.ValidateResult.SUCCESS
import core.layers.ConstRefractiveIndexLayer
import core.layers.Layer
import org.apache.commons.math3.complex.Complex
import ui.controllers.LineChartState
import ui.controllers.MainController
import java.util.*

object State {

    lateinit var mainController: MainController

    var wavelengthFrom: Double = 0.0
    var wavelengthTo: Double = 0.0
    var wavelengthStep: Double = 0.0
    var wavelengthCurrent: Double = 0.0

    var angle: Double = 0.0
    lateinit var polarization: Polarization
    lateinit var regime: Regime

    lateinit var leftMedium: Medium
    lateinit var rightMedium: Medium
    lateinit var leftMediumLayer: Layer
    lateinit var rightMediumLayer: Layer
    lateinit var n_left: Complex_
    lateinit var n_right: Complex_

    lateinit var structure: Structure
    lateinit var mirror: Mirror

    var wavelength = mutableListOf<Double>()
    val reflection = mutableListOf<Double>()
    val transmission = mutableListOf<Double>()
    val absorption = mutableListOf<Double>()
    val permittivity = mutableListOf<Complex_>()
    val refractiveIndex = mutableListOf<Complex_>()

    /**
     * Here mirror is built after the validation procedure 'initStateUsing'
     * BUT! In that procedure during the layers' constructing some parameters such as n of the left medium are accessed via mirror
     */
    fun init(): ValidateResult {
        if (initStateUsing(mainController) == SUCCESS) {
            clearPreviousComputation()
            buildMirror()
            return SUCCESS
        }
        return FAILURE
    }

    fun compute() {
        val size = ((wavelengthTo - wavelengthFrom) / wavelengthStep).toInt() + 1
        wavelength = ArrayList<Double>(size)
        /* TODO MERGE TWO LOOPS, CHECK PERFORMANCE */
        (0 until size).forEach { wavelength.add(wavelengthFrom + it * wavelengthStep) }

        /* TODO PARALLEL */
        (0..wavelength.size - 1).forEach {
            wavelengthCurrent = wavelength[it]
            with(mirror) {
                when (regime) {
                    R -> reflection += computeReflection()
                    T -> transmission += computeTransmission()
                    A -> absorption += computeAbsorption()
                    PERMITTIVITY -> permittivity += computePermittivity()
                    REFRACTIVE_INDEX -> refractiveIndex += computeRefractiveIndex()
                }
            }
        }

        fun set_fit() = reflection.clear()

        /**
         * Wavelengths are already initialized.
         */
        fun compute_fit() = (0..wavelength.size - 1).forEach {
            wavelengthCurrent = wavelength[it]
            reflection += mirror.computeReflection()
        }

        fun computeDifference() {
            val imported = LineChartState.imported[0].extendedSeriesReal.series.data


        }
    }

    private fun clearPreviousComputation() {
        fun <T> clearIfNotEmpty(vararg lists: MutableList<out T>) = lists.forEach { it.run { if (isNotEmpty()) clear() } }

        clearIfNotEmpty(reflection, transmission, absorption)
        clearIfNotEmpty(permittivity, refractiveIndex)
        /*
        TODO Doesn't clear when using this form of extension function (without "run")
        fun <T> MutableList<out T>.clearIfNotEmpty() = run { if (isNotEmpty()) clear() } // works
        fun <T> MutableList<T>.clearIfNotEmpty() = { if (isNotEmpty()) clear() } // doesn't work
         */
    }

    private fun buildMirror() {
        mirror = MirrorBuilder.build(structure, leftMedium, rightMedium)
    }
}


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

        return if (State.polarization === Polarization.P) {
            ((n2 * cos1) / (n1 * cos2)).abs() * t * t
        } else {
            ((n2 * cos2) / (n1 * cos1)).abs() * t * t
        }
    }

    fun computeAbsorption(): Double = 1.0 - computeReflection() - computeTransmission()

    fun computePermittivity(): Complex_ {
        val layer = structure.blocks[0].layers[0]
        val n = layer.n
        /**
         * eps = n^2 = (n + ik)^2 = n^2 - k^2 + 2ink
         * Re(eps) = n^2 - k^2, Im(eps) = 2nk
         */
        return n * n
    }

    fun computeRefractiveIndex(): Complex_ = structure.blocks[0].layers[0].n

    private fun r(): Complex_ {
        val mirrorMatrix = matrix
        return mirrorMatrix[1, 0] / mirrorMatrix[1, 1] * (-1.0)
    }

    private fun t(): Complex_ {
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
    private val matrix: Matrix_
        get() {
            var prev = leftMediumLayer
            /* blank layer (for formal initialization) */
            var first: Layer = ConstRefractiveIndexLayer(d = Double.POSITIVE_INFINITY, n = Complex_(Complex.NaN))
            /* blank layer (for formal initialization) */
            var beforeFirst: Layer = ConstRefractiveIndexLayer(d = Double.POSITIVE_INFINITY, n = Complex_(Complex.NaN))

            var periodMatrix: Matrix_
            var tempMatrix: Matrix_
            var mirrorMatrix: Matrix_ = Matrix_.unaryMatrix()

            var isFirst: Boolean
            for (i in 0..structure.blocks.size - 1) {

                with(structure.blocks[i]) {
                    periodMatrix = Matrix_.unaryMatrix()

                    isFirst = true
                    var cur: Layer = ConstRefractiveIndexLayer(d = Double.POSITIVE_INFINITY, n = Complex_(Complex.NaN))  // blank layer (for formal initialization)
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
         * Left and right layers are considered to be next to the left medium (AIR, OTHER, etc.)
         */
        val cos1 = cosThetaInLayer(leftLayer.n)
        val cos2 = cosThetaInLayer(rightLayer.n)
        val n1e = if (State.polarization === Polarization.S) {
            n1 * cos1
        } else {
            n1 / cos1
        }
        val n2e = if (State.polarization === Polarization.S) {
            n2 * cos2
        } else {
            n2 / cos2
        }
        setDiagonal((n2e + n1e) / (n2e * 2.0))
        setAntiDiagonal((n2e - n1e) / (n2e * 2.0))
    }
}