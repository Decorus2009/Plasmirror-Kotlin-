package core.validators

import core.validators.ValidationResult.FAILURE
import core.validators.ValidationResult.SUCCESS
import java.io.File

object MultipleExportDialogParametersValidator {
  fun validateAngles(angleFromStr: String, angleToStr: String, angleStepStr: String): ValidationResult {
    try {
      val angleFrom = angleFromStr.toDouble()
      val angleTo = angleToStr.toDouble()
      val angleStep = angleStepStr.toDouble()

      if (
        angleFrom.isNotAllowed() or angleTo.isNotAllowed() or angleStep.isNotAllowed() or
        (angleFrom > angleTo) or (angleStep > angleTo) or (angleStep == 0.0)
      ) {
        alert(headerText = "Angle range error", contentText = "Provide correct angle range")
        return FAILURE
      }
    } catch (e: NumberFormatException) {
      alert(headerText = "Angle value error", contentText = "Provide correct angle")
      return FAILURE
    }
    return SUCCESS
  }

  fun validateChosenDirectory(chosenDirectory: File?) = when (chosenDirectory) {
    null -> {
      alert(headerText = "Directory error", contentText = "Choose a directory")
      FAILURE
    }
    else -> SUCCESS
  }
}