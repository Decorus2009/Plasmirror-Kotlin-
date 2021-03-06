package core

import core.layers.semiconductor.Layer
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
  val extinctionCoefficient = mutableListOf<Double>()
  val scatteringCoefficient = mutableListOf<Double>()

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

  fun compute() = (0 until wavelength.size).forEach {
    wavelengthCurrent = wavelength[it]

    with(mirror) {
      when (regime) {
        Regime.REFLECTANCE -> reflectance += reflectance()
        Regime.TRANSMITTANCE -> transmittance += transmittance()
        Regime.ABSORBANCE -> absorbance += absorbance()
        Regime.PERMITTIVITY -> permittivity += permittivity()
        Regime.REFRACTIVE_INDEX -> refractiveIndex += refractiveIndex()
        Regime.EXTINCTION_COEFFICIENT -> {
          extinctionCoefficient += extinctionCoefficient()
//          wavelength.forEach { print("$it\t") }
//          println()
        }
        Regime.SCATTERING_COEFFICIENT -> scatteringCoefficient += scatteringCoefficient()
      }
    }
  }

  private fun clear() {
    fun <T> clear(vararg lists: MutableList<out T>) = lists.forEach { it.clear() }

    clear(reflectance, transmittance, absorbance, extinctionCoefficient, scatteringCoefficient)
    clear(permittivity, refractiveIndex)
  }

  private fun buildMirror() {
    mirror = Mirror(structure, leftMediumLayer, rightMediumLayer)
  }

  private fun saveToStorages() = rootController.mainController.saveToStorages()
}