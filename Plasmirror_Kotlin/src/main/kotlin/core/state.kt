package core

import core.optics.Regime.*
import core.layers.Layer
import core.optics.Polarization
import core.optics.Regime
import core.validators.StateValidator
import core.validators.ValidationResult
import core.validators.ValidationResult.FAILURE
import core.validators.ValidationResult.SUCCESS
import rootController


object State {
//    lateinit var mainController: MainController

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

    fun init(): ValidationResult {
        saveToStorages()
        return when (StateValidator.initState()) {
            SUCCESS -> {
                clear()
                buildMirror()
                SUCCESS
            }
            FAILURE -> FAILURE
        }
    }

//    fun init() = when (StateValidator.initState()) {
//        SUCCESS -> {
//            rootController.mainController.saveToStorages()
//            clear()
//            buildMirror()
//            SUCCESS
//        }
//        FAILURE -> FAILURE
//    }

    fun compute() = (0 until wavelength.size).forEach {
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

    private fun clear() {
//        fun <T> clearIfNotEmpty(vararg lists: MutableList<out T>) = lists.forEach { it.run { if (isNotEmpty()) clear() } }

        fun <T> clear(vararg lists: MutableList<out T>) = lists.forEach { it.clear() }


        clear(reflectance, transmittance, absorbance)
        clear(permittivity, refractiveIndex)
        /*
        TODO Doesn't clear when using this form of extension function (without "run")
        fun <T> MutableList<out T>.clearIfNotEmpty() = run { if (isNotEmpty()) clear() } // works
        fun <T> MutableList<T>.clearIfNotEmpty() = { if (isNotEmpty()) clear() } // doesn't work
         */
    }

    private fun buildMirror() {
        mirror = Mirror(structure, leftMediumLayer, rightMediumLayer)
    }

    private fun saveToStorages() = rootController.mainController.saveToStorages()
}
