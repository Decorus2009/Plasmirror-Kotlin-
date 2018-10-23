package core.validators

import core.Complex_
import core.Complex_.Companion.ONE
import core.ComputationParameters
import core.EpsType.*
import core.Medium.*
import core.Polarization
import core.State
import core.layers.ConstRefractiveIndexLayer
import core.layers.GaAs
import core.validators.ValidationResult.FAILURE
import core.validators.ValidationResult.SUCCESS
import kotlin.Double.Companion.POSITIVE_INFINITY


object StateValidator {
    fun initState() = when {
        OpticalParametersValidator.initOpticalParametersUsing() == SUCCESS &&
                StructureValidator.initStructure() == SUCCESS -> SUCCESS
        else -> FAILURE
    }
}


private object OpticalParametersValidator {

    fun initOpticalParametersUsing() = when {
        initOuterMediaLayers() == SUCCESS &&
                initPolarizationAndAngle() == SUCCESS &&
                initCalculationRange() == SUCCESS -> SUCCESS
        else -> FAILURE
    }

    /**
     * Negative refractive index values are allowed
     */
    private fun initOuterMediaLayers(): ValidationResult {
        val media = mapOf(
                "Air" to AIR,
                "GaAs: Adachi" to GAAS_ADACHI, "GaAs: Gauss" to GAAS_GAUSS, "GaAs: Gauss-Adachi" to GAAS_GAUSS_ADACHI,
                "Custom" to CUSTOM
        )

        State.leftMediumLayer = when (media[ComputationParameters.leftMedium]!!) {
            AIR -> ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = ONE)
            GAAS_ADACHI -> GaAs(d = POSITIVE_INFINITY, epsType = ADACHI)
            GAAS_GAUSS -> GaAs(d = POSITIVE_INFINITY, epsType = GAUSS)
            GAAS_GAUSS_ADACHI -> GaAs(d = POSITIVE_INFINITY, epsType = GAUSS_ADACHI)
            CUSTOM -> {
                try {
                    with(ComputationParameters) {
                        val nReal = leftMediumRefractiveIndexReal.toDouble()
                        val nImaginary = leftMediumRefractiveIndexImaginary.toDouble()
                        ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = Complex_(nReal, nImaginary))
                    }
                } catch (e: NumberFormatException) {
                    alert(headerText = "Custom refractive index value error",
                            contentText = "Provide correct refractive index for left medium")
                    // default value
                    ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = ONE)
                    return FAILURE
                }
            }
        }

        State.rightMediumLayer = when (media[ComputationParameters.rightMedium]!!) {
            AIR -> ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = ONE)
            GAAS_ADACHI -> GaAs(d = POSITIVE_INFINITY, epsType = ADACHI)
            GAAS_GAUSS -> GaAs(d = POSITIVE_INFINITY, epsType = GAUSS)
            GAAS_GAUSS_ADACHI -> GaAs(d = POSITIVE_INFINITY, epsType = GAUSS_ADACHI)
            CUSTOM -> {
                try {
                    with(ComputationParameters) {
                        val nReal = rightMediumRefractiveIndexReal.toDouble()
                        val nImaginary = rightMediumRefractiveIndexImaginary.toDouble()
                        ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = Complex_(nReal, nImaginary))
                    }
                } catch (e: NumberFormatException) {
                    alert(headerText = "Custom refractive index value error",
                            contentText = "Provide correct refractive index for right medium")
                    // default value
                    ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = ONE)
                    return FAILURE
                }
            }
        }
        return SUCCESS
    }

    private fun initPolarizationAndAngle(): ValidationResult {
        State.polarization = Polarization.valueOf(ComputationParameters.polarization)

        try {
            State.angle = ComputationParameters.angle.toDouble()
        } catch (e: NumberFormatException) {
            alert(headerText = "Angle value error", contentText = "Provide correct angle")
            return FAILURE
        }

        if (State.angle.isNotAllowed()) {
            alert(headerText = "Angle is out of range [0, 90) deg", contentText = "Provide correct angle")
            return FAILURE
        }
        return SUCCESS
    }

    private fun initCalculationRange(): ValidationResult {
        try {
            with(ComputationParameters) {
                State.wavelengthFrom = computationRangeStart.toDouble()
                State.wavelengthTo = computationRangeEnd.toDouble()
                State.wavelengthStep = computationRangeStep.toDouble()
            }
        } catch (e: NumberFormatException) {
            alert(headerText = "Wavelength range error", contentText = "Provide correct wavelength range")
            return FAILURE
        }

        if (State.wavelengthFrom < 0.0 || State.wavelengthTo <= 0.0 || State.wavelengthStep <= 0.0
                || State.wavelengthFrom > State.wavelengthTo || State.wavelengthStep >= State.wavelengthTo) {
            alert(headerText = "Wavelength range error", contentText = "Provide correct wavelength range")
            return FAILURE
        }
        return SUCCESS
    }
}
