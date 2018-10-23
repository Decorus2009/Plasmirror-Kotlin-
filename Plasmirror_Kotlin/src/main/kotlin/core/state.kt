package core

import core.Regime.*
import core.layers.Layer
import core.validators.StateValidator
import core.validators.ValidationResult
import core.validators.ValidationResult.*
import ui.controllers.MainController
import java.util.*

object State {

    lateinit var mainController: MainController

    var wavelengthFrom: Double = 0.0
    var wavelengthTo: Double = 1000.0
    var wavelengthStep: Double = 0.0
    var wavelengthCurrent: Double = 0.0

    var angle: Double = 0.0
    lateinit var polarization: Polarization
    lateinit var regime: Regime

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
        val size = ((wavelengthTo - wavelengthFrom) / wavelengthStep).toInt() + 1
        wavelength = ArrayList<Double>(size)
        /* TODO MERGE TWO LOOPS, CHECK PERFORMANCE */
        (0 until size).forEach { wavelength.add(wavelengthFrom + it * wavelengthStep) }

        /* TODO PARALLEL */
        (0..wavelength.size - 1).forEach {
            wavelengthCurrent = wavelength[it]
            with(mirror) {
                when (regime) {
                    REFLECTANCE -> reflection += computeReflection()
                    TRANSMITTANCE -> transmission += computeTransmission()
                    ABSORBANCE -> absorption += computeAbsorption()
                    PERMITTIVITY -> permittivity += computePermittivity()
                    REFRACTIVE_INDEX -> refractiveIndex += computeRefractiveIndex()
                }
            }
        }

//        fun set_fit() = reflection.clear()
//
//        /**
//         * Wavelengths are already initialized.
//         */
//        fun compute_fit() = (0..wavelength.size - 1).forEach {
//            wavelengthCurrent = wavelength[it]
//            reflection += mirror.computeReflection()
//        }
//
//        fun computeDifference() {
//            val imported = LineChartState.imported[0].extendedSeriesReal.series.data
//        }
    }

    private fun clearPreviousComputation() {
        fun <T> clearIfNotEmpty(vararg lists: MutableList<out T>) = lists.forEach { it.run { if (isNotEmpty()) clear() } }

        clearIfNotEmpty(reflection, transmission, absorption)
        clearIfNotEmpty(permittivity, refractiveIndex)
        /*
        TODO Doesn't clear when using this form of extension function (without "run")
        fun <TRANSMITTANCE> MutableList<out TRANSMITTANCE>.clearIfNotEmpty() = run { if (isNotEmpty()) clear() } // works
        fun <TRANSMITTANCE> MutableList<TRANSMITTANCE>.clearIfNotEmpty() = { if (isNotEmpty()) clear() } // doesn't work
         */
    }

    private fun buildMirror() {
        mirror = Mirror(structure, leftMediumLayer, rightMediumLayer)
    }
}


