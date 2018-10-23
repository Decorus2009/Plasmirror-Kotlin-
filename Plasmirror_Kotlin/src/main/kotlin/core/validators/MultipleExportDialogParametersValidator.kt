package core.validators

import core.State
import core.validators.ValidationResult.FAILURE
import core.validators.ValidationResult.SUCCESS

// TODO
object MultipleExportDialogParametersValidator {
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
                    return FAILURE
                }
            }
        } catch (e: NumberFormatException) {
            alert(headerText = "Angle range error", contentText = "Provide correct angle range")
            return FAILURE
        }
        return SUCCESS
    }

    fun validateChosenDirectory(): ValidationResult {
        if (State.mainController.multipleExportDialogController.chosenDirectory == null) {
            alert(headerText = "Directory error", contentText = "Choose a directory")
            return FAILURE
        }
        return SUCCESS
    }
}