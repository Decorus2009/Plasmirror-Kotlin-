package core.validators

import core.*

// TODO
object MultipleExportDialogParametersValidator {
    fun validateRegime(): ValidationResult {
        if ((State.regime == Regime.PERMITTIVITY || State.regime == Regime.REFRACTIVE_INDEX) && State.mainController.multipleExportDialogController.anglesSelected()) {
            alert(headerText = "Computation regime for multiple export error",
                    contentText = "For permittivity or refractive index computation temperature range must be selected")
            return ValidationResult.FAILURE
        }
        return ValidationResult.SUCCESS
    }

    fun validateAngles(): ValidationResult {
        try {
            with(State.mainController.multipleExportDialogController) {
                angleFrom = angleFromTextField.text.toDouble()
                angleTo = angleToTextField.text.toDouble()
                angleStep = angleStepTextField.text.toDouble()
                /* angles allowed range */
                if (angleFrom.isNotAllowed() || angleTo.isNotAllowed() || angleStep.isNotAllowed()
                        || angleFrom > angleTo || angleStep > angleTo || angleStep == 0.0) {
                    alert(headerText = "Angle range error", contentText = "Provide correct angle range")
                    return ValidationResult.FAILURE
                }
            }
        } catch (e: NumberFormatException) {
            alert(headerText = "Angle range error", contentText = "Provide correct angle range")
            return ValidationResult.FAILURE
        }
        return ValidationResult.SUCCESS
    }

    fun validateTemperatures(): ValidationResult {
        try {
            with(State.mainController.multipleExportDialogController) {
                temperatureFrom = temperatureFromTextField.text.toDouble()
                temperatureTo = temperatureToTextField.text.toDouble()
                temperatureStep = temperatureStepTextField.text.toDouble()
                if (temperatureFrom <= 0.0 || temperatureTo <= 0.0 || temperatureStep <= 0.0 || temperatureFrom > temperatureTo || temperatureStep > temperatureTo) {
                    alert(headerText = "Temperature range error", contentText = "Provide correct temperature range")
                    return ValidationResult.FAILURE
                }
            }
        } catch (e: NumberFormatException) {
            alert(headerText = "Temperature range error", contentText = "Provide correct temperature range")
            return ValidationResult.FAILURE
        }
        return ValidationResult.SUCCESS
    }

    fun validateChosenDirectory(): ValidationResult {
        if (State.mainController.multipleExportDialogController.chosenDirectory == null) {
            alert(headerText = "Directory error", contentText = "Choose a directory")
            return ValidationResult.FAILURE
        }
        return ValidationResult.SUCCESS
    }
}