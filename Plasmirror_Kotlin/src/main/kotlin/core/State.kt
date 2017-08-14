package core

import core.util.*
import core.util.Medium.*
import core.util.Regime.*
import core.util.StateValidator.validateAndSetStateUsing
import core.util.ValidateResult.FAILURE
import core.util.ValidateResult.SUCCESS
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
    lateinit var n_left: Cmplx
    lateinit var n_right: Cmplx

    lateinit var structure: Structure
    lateinit var mirror: Mirror

    var wavelength = mutableListOf<Double>()
    val reflection = mutableListOf<Double>()
    val transmission = mutableListOf<Double>()
    val absorption = mutableListOf<Double>()
    val permittivity = mutableListOf<Cmplx>()
    val refractiveIndex = mutableListOf<Cmplx>()

    fun set(): ValidateResult {
        if (validateAndSetStateUsing(mainController) == SUCCESS) {
            clearPreviousComputation()
            buildMirror()
            return SUCCESS
        }
        return FAILURE
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

    fun compute() {
        val size = ((wavelengthTo - wavelengthFrom) / wavelengthStep).toInt() + 1
        wavelength = ArrayList<Double>(size)
        // TODO MERGE TWO LOOPS, CHECK PERFORMANCE
        (0..size - 1).forEach { wavelength.add(wavelengthFrom + it * wavelengthStep) }
        // TODO PARALLEL
        (0..wavelength.size - 1).forEach {
            wavelengthCurrent = wavelength[it]

            mirror.run {
                when (regime) {
                    R -> reflection += computeReflection()
                    T -> transmission += computeTransmission()
                    A -> absorption += computeAbsorption()
                    EPS -> permittivity += computePermittivity()
                    N -> refractiveIndex += computeRefractiveIndex()
                }
            }
        }
//        println("${wavelength[0]} ${reflection[0]}")
//        for (i in 0..wavelength.size - 1) println("${wavelength[i]} ${reflection[i]}")
    }

    private fun buildMirror() {
        mirror = MirrorBuilder.build(structure, leftMedium, rightMedium)
    }
}