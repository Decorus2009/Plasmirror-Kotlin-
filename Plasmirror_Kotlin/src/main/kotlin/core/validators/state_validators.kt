package core.validators

import core.*
import core.Complex_.Companion.ONE
import core.layers.ConstRefractiveIndexLayer
import core.layers.GaAs
import core.optics.EpsType.ADACHI
import core.optics.EpsType.GAUSS
import core.optics.Medium.*
import core.optics.Polarization
import core.optics.Regime.*
import core.validators.ValidationResult.FAILURE
import core.validators.ValidationResult.SUCCESS
import kotlin.Double.Companion.POSITIVE_INFINITY


object StateValidator {
  fun initState() = when {
    OpticalParametersValidator.initOpticalParameters() == SUCCESS &&
      StructureValidator.initStructure() == SUCCESS -> SUCCESS
    else -> FAILURE
  }
}


private object OpticalParametersValidator {

  fun initOpticalParameters() = when {
    initRegime() == SUCCESS &&
      initExternalMediaLayers() == SUCCESS &&
      initPolarizationAndAngle() == SUCCESS &&
      initComputationRange() == SUCCESS -> SUCCESS
    else -> FAILURE
  }

  private fun initRegime(): ValidationResult {
    val regimes = mapOf(
      "Reflectance" to REFLECTANCE,
      "Transmittance" to TRANSMITTANCE,
      "Absorbance" to ABSORBANCE,
      "Permittivity" to PERMITTIVITY,
      "Refractive Index" to REFRACTIVE_INDEX,
      "Extinction Coefficient" to EXTINCTION_COEFFICIENT,
      "Scattering Coefficient" to SCATTERING_COEFFICIENT
    )

    /*
    Setting values in ComputationParametersStorage and State to defaults
     */
    fun toDefaults() {
      ComputationParametersStorage.regime = "Reflectance"
      State.regime = regimes[ComputationParametersStorage.regime]!!
    }

    try {
      State.regime = regimes[ComputationParametersStorage.regime]!!
      // if something is wrong with regime in JSON file and no mapping is found
    } catch (e: KotlinNullPointerException) {
      alert(headerText = "Regime type error", contentText = "Check current regime type")
      toDefaults()
      return FAILURE
    }
    return SUCCESS
  }

  /**
   * Negative refractive index values are allowed
   */
  private fun initExternalMediaLayers(): ValidationResult {
    val media = mapOf(
      "Air" to AIR,
      "GaAs: Adachi" to GAAS_ADACHI,
      "GaAs: Gauss" to GAAS_GAUSS,
      "Custom" to CUSTOM
    )

    /*
    Setting values in ComputationParametersStorage and State to defaults
     */
    fun toDefaults() {
      with(ComputationParametersStorage) {
        leftMedium = "Air"
        leftMediumRefractiveIndexReal = "1.0"
        leftMediumRefractiveIndexImaginary = "0.0"

        rightMedium = "GaAs: Adachi"
        rightMediumRefractiveIndexReal = "1.0"
        rightMediumRefractiveIndexImaginary = "0.0"
      }
      State.leftMediumLayer = ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = ONE)
      State.rightMediumLayer = GaAs(d = POSITIVE_INFINITY, epsType = ADACHI)
    }

    fun initExternalMediumLayer(medium: String,
                                mediumRefractiveIndexReal: String,
                                mediumRefractiveIndexImaginary: String) =
      when (media[medium]!!) {
        AIR -> ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = ONE) to SUCCESS
        GAAS_ADACHI -> GaAs(d = POSITIVE_INFINITY, epsType = ADACHI) to SUCCESS
        GAAS_GAUSS -> GaAs(d = POSITIVE_INFINITY, epsType = GAUSS) to SUCCESS
        CUSTOM -> {
          try {
            val nReal = mediumRefractiveIndexReal.toDouble()
            val nImaginary = mediumRefractiveIndexImaginary.toDouble()
            ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = Complex_(nReal, nImaginary)) to SUCCESS
            // if something is wrong with medium types in JSON file and no mapping is found
          } catch (e: KotlinNullPointerException) {
            alert(headerText = "External medium type error", contentText = "Provide correct types for external media")
            ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = ONE) to FAILURE
          } catch (e: NumberFormatException) {
            alert(headerText = "Custom refractive index value error",
              contentText = "Provide correct refractive index for medium")
            ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = ONE) to FAILURE
          }
        }
      }

    with(ComputationParametersStorage) {
      val (leftMediumLayer, validResForLeftMediumLayer) =
        initExternalMediumLayer(leftMedium, leftMediumRefractiveIndexReal, leftMediumRefractiveIndexImaginary)

      val (rightMediumLayer, validResForRightMediumLayer) =
        initExternalMediumLayer(rightMedium, rightMediumRefractiveIndexReal, rightMediumRefractiveIndexImaginary)

      State.leftMediumLayer = leftMediumLayer
      State.rightMediumLayer = rightMediumLayer

      return when {
        validResForLeftMediumLayer == FAILURE || validResForRightMediumLayer == FAILURE -> {
          // may reset value for leftMedium and rightMedium in ComputationParametersStorage and State back to defaults
          toDefaults()
          FAILURE
        }
        else -> SUCCESS
      }
    }
  }

  private fun initPolarizationAndAngle(): ValidationResult {
    /*
    Setting values in ComputationParametersStorage and State to defaults
     */
    fun toDefaults() {
      ComputationParametersStorage.polarization = "P"
      ComputationParametersStorage.angle = "0.0"
      State.polarization = Polarization.valueOf(ComputationParametersStorage.polarization)
      State.angle = 0.0
    }

    try {
      State.polarization = Polarization.valueOf(ComputationParametersStorage.polarization)
      State.angle = ComputationParametersStorage.angle.toDouble()
      // if something is wrong with polarization in JSON file and no value is found in enum
    } catch (e: IllegalArgumentException) {
      alert(headerText = "Polarization type or angle value error",
        contentText = "Check current polarization type and angle value")
      toDefaults()
      return FAILURE
    } catch (e: NumberFormatException) {
      alert(headerText = "Angle value error", contentText = "Provide correct angle")
      toDefaults()
      return FAILURE
    }

    if (State.angle.isNotAllowed()) {
      alert(headerText = "Angle is out of range [0, 90) deg", contentText = "Provide correct angle")
      toDefaults()
      return FAILURE
    }
    return SUCCESS
  }

  private fun initComputationRange(): ValidationResult {
    /*
    Setting values in ComputationParametersStorage and State to defaults
     */
    fun toDefaults() {
      State.wavelengthStart = 600.0
      State.wavelengthEnd = 1000.0
      State.wavelengthStep = 1.0

      ComputationParametersStorage.wavelengthStart = State.wavelengthStart.toString()
      ComputationParametersStorage.wavelengthEnd = State.wavelengthEnd.toString()
      ComputationParametersStorage.wavelengthStep = State.wavelengthStep.toString()
    }

    try {
      State.wavelengthStart = ComputationParametersStorage.wavelengthStart.toDouble()
      State.wavelengthEnd = ComputationParametersStorage.wavelengthEnd.toDouble()
      State.wavelengthStep = ComputationParametersStorage.wavelengthStep.toDouble()
    } catch (e: NumberFormatException) {
      alert(headerText = "Wavelength range error", contentText = "Provide correct wavelength range")
      toDefaults()
      return FAILURE
    }

    if (State.wavelengthStart < 0.0 || State.wavelengthEnd <= 0.0 || State.wavelengthStep <= 0.0
      || State.wavelengthStart > State.wavelengthEnd || State.wavelengthStep >= State.wavelengthEnd) {
      alert(headerText = "Wavelength range error", contentText = "Provide correct wavelength range")
      toDefaults()
      return FAILURE
    }

    initComputationWavelengths()
    return SUCCESS
  }

  private fun initComputationWavelengths() = with(State) {
    val size = ((wavelengthEnd - wavelengthStart) / wavelengthStep).toInt() + 1
    wavelength = (0 until size).map { wavelengthStart + it * wavelengthStep }.toMutableList()
  }
}
