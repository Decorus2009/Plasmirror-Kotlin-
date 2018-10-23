package core

import core.Regime.*
import core.layers.Layer
import core.validators.StateValidator
import core.validators.ValidationResult.*
import ui.controllers.MainController
import java.util.*


object State {
    lateinit var mainController: MainController

    var wavelengthStart: Double = 600.0
    var wavelengthEnd: Double = 1000.0
    var wavelengthStep: Double = 1.0
    var wavelengthCurrent = wavelengthStart

    lateinit var regime: Regime

    lateinit var leftMediumLayer: Layer
    lateinit var rightMediumLayer: Layer

    lateinit var polarization: Polarization
    var angle: Double = 0.0

    lateinit var structure: Structure
    lateinit var mirror: Mirror

    var wavelength = mutableListOf<Double>()
    val reflectance = mutableListOf<Double>()
    val transmittance = mutableListOf<Double>()
    val absorbance = mutableListOf<Double>()
    val permittivity = mutableListOf<Complex_>()
    val refractiveIndex = mutableListOf<Complex_>()

    /**
     * Here mirror is built after the validation procedure 'initState'
     * BUT! In that procedure during the layers' constructing some parameters such as n of the left medium are accessed via mirror
     */
    //    fun init() {
//        if (StateValidator.initState() == SUCCESS) {
//            clearPreviousComputation()
//            buildMirror()
//        }
//    }

    fun init() = when (StateValidator.initState()) {
        SUCCESS -> {
            clearPreviousComputation()
            buildMirror()
            SUCCESS
        }
        FAILURE -> FAILURE
    }

    fun compute() {
        val size = ((wavelengthEnd - wavelengthStart) / wavelengthStep).toInt() + 1
        wavelength = ArrayList(size)
        /* TODO MERGE TWO LOOPS, CHECK PERFORMANCE */
        (0 until size).forEach { wavelength.add(wavelengthStart + it * wavelengthStep) }

        /* TODO PARALLEL */
        (0 until wavelength.size).forEach {
            wavelengthCurrent = wavelength[it]
            with(mirror) {
                when (regime) {
                    REFLECTANCE -> reflectance += computeReflectance()
                    TRANSMITTANCE -> transmittance += computeTransmittance()
                    ABSORBANCE -> absorbance += computeAbsorbance()
                    PERMITTIVITY -> permittivity += computePermittivity()
                    REFRACTIVE_INDEX -> refractiveIndex += computeRefractiveIndex()
                }
            }
        }

//        fun set_fit() = reflectance.clear()
//
//        /**
//         * Wavelengths are already initialized.
//         */
//        fun compute_fit() = (0..wavelength.size - 1).forEach {
//            wavelengthCurrent = wavelength[it]
//            reflectance += mirror.computeReflectance()
//        }
//
//        fun computeDifference() {
//            val imported = LineChartState.imported[0].extendedSeriesReal.series.data
//        }
    }

    private fun clearPreviousComputation() {
        fun <T> clearIfNotEmpty(vararg lists: MutableList<out T>) = lists.forEach { it.run { if (isNotEmpty()) clear() } }

        clearIfNotEmpty(reflectance, transmittance, absorbance)
        clearIfNotEmpty(permittivity, refractiveIndex)
        /*
        TODO Doesn't clear when using this form of extension function (without "run")
        fun <T> MutableList<out T>.clearIfNotEmpty() = run { if (isNotEmpty()) clear() } // works
        fun <T> MutableList<T>.clearIfNotEmpty() = { if (isNotEmpty()) clear() } // doesn't work
         */
    }

    private fun buildMirror() {
        mirror = Mirror(structure, leftMediumLayer, rightMediumLayer)
    }
}
