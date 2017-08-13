package core.util

import core.State
import core.State.angle
import core.State.n_left
import core.State.n_right
import core.State.regime
import core.State.structure
import core.State.wavelengthFrom
import core.State.wavelengthStep
import core.State.wavelengthTo
import core.util.OpticalParametersValidator.validateAndSetOpticalParametersUsing
import core.util.Regime.EPS
import core.util.Regime.N
import core.util.StructureValidator.validateAndBuildStructure
import core.util.ValidateResult.FAILURE
import core.util.ValidateResult.SUCCESS
import javafx.scene.control.Alert
import ui.controllers.*

enum class ValidateResult { SUCCESS, FAILURE }

object StateValidator {
    fun validateAndSetStateUsing(mainController: MainController): ValidateResult {
        with(mainController) {
            if (validateAndSetOpticalParametersUsing(globalParametersController) == SUCCESS
                    && validateAndBuildStructure(structureDescriptionController) == SUCCESS) {
                return SUCCESS
            }
        }
        return FAILURE
    }
}


private object OpticalParametersValidator {
    fun validateAndSetOpticalParametersUsing(globalParametersController: GlobalParametersController): ValidateResult {
        with(globalParametersController) {
            if (validateRefractiveIndicesUsing(mediumParametersController) == SUCCESS
                    && validateAngleUsing(lightParametersController) == SUCCESS
                    && validateCalculationRangeUsing(computationRangeController) == SUCCESS) {
                return SUCCESS
            }
        }
        return FAILURE
    }

    /**
     * Negative refractive index values are allowed
     */
    private fun validateRefractiveIndicesUsing(mediumParametersController: MediumParametersController): ValidateResult {
        with(mediumParametersController) {
            try {
                with(leftMediumChoiceBox) {
                    /* left medium == OTHER */
                    if (value == items[2]) {
                        n_left = Cmplx(n_leftRealTextField.text.toDouble(), n_leftImaginaryTextField.text.toDouble())
                    }
                }
                with(rightMediumChoiceBox) {
                    /* right medium == OTHER */
                    if (value == items[2]) {
                        n_right = Cmplx(n_rightRealTextField.text.toDouble(), n_rightImaginaryTextField.text.toDouble())
                    }
                }
            } catch (e: NumberFormatException) {
                alert(headerText = "Refractive index value error", contentText = "Provide correct refractive index")
                return FAILURE
            }
        }
        return SUCCESS
    }

    private fun validateAngleUsing(lightParametersController: LightParametersController): ValidateResult {
        try {
            angle = lightParametersController.angleTextField.text.toDouble()
            if (angle.isNotAllowed()) {
                alert(headerText = "Angle value error", contentText = "Provide correct angle")
                return FAILURE
            }
        } catch (e: NumberFormatException) {
            alert(headerText = "Angle value error", contentText = "Provide correct angle")
            return FAILURE
        }
        return SUCCESS
    }

    private fun validateCalculationRangeUsing(computationRangeController: ComputationRangeController): ValidateResult {
        try {
            with(computationRangeController) {
                wavelengthFrom = wavelengthFromTextField.text.toDouble()
                wavelengthTo = wavelengthToTextField.text.toDouble()
                wavelengthStep = wavelengthStepTextField.text.toDouble()
                if (wavelengthFrom < 0.0 || wavelengthTo <= 0.0
                        || wavelengthStep <= 0.0 || wavelengthFrom > wavelengthTo || wavelengthStep >= wavelengthTo) {
                    alert(headerText = "Wavelength range error", contentText = "Provide correct wavelength range")
                    return FAILURE
                }
            }
        } catch (e: NumberFormatException) {
            alert(headerText = "Wavelength range error", contentText = "Provide correct wavelength range")
            return FAILURE
        }
        return SUCCESS
    }
}

/**
 * StructureValidator analyzes structure file as a String
 * and builds StructureDescription using LayerDescription and BlockDescription
 */
private object StructureValidator {
    private var parameterNumbers = mapOf(
            1 to 2,
            2 to 4,
            3 to 3,
            4 to 5,
            5 to 7,
            6 to 6,
            7 to 6,
            8 to 6,
            9 to 5
    )

    /**
     * Validates and builds structure description representation in the text field of structureDescriptionController
     *
     * @param structureDescriptionController controller to get its text field
     * containing structure description text representation
     */
    fun validateAndBuildStructure(structureDescriptionController: StructureDescriptionController): ValidateResult {
        try {
            val lines = toLines(structureDescriptionController.structureDescriptionTextArea.text)
            val tokenizedLines = linesToTokenizedLines(lines)
            validateTokenizedLines(tokenizedLines)
            val structureDescription = buildStructureDescription(tokenizedLines)
            structure = buildStructure(structureDescription)
        } catch(e: StructureDescriptionException) {
            alert(headerText = "Structure description error", contentText = e.message ?: "")
            return FAILURE
        }
        return SUCCESS
    }

    /**
     * Maps structure string representation to the list of lines.
     * Each line is a symbolic description of a layer
     *
     * @param structure structure string representation
     * @return structure representation as lines
     */
    @Throws(StructureDescriptionException::class)
    private fun toLines(structure: String): List<String> = structure.lines().filter { it.isNotBlank() }.apply {
        //        if (isEmpty()) {
//            throw StructureDescriptionException("Empty structure description")
//        }
    }

    /**
     * Tokenizes each line of the structure
     *
     * @param lines structure representation as lines
     * @return list of tokenized lines (each tokenized line is a list of tokens)
     */
    private fun linesToTokenizedLines(lines: List<String>) = mutableListOf<List<String>>().apply {
        lines.map { it.trim() }.filter { it.startsWith("//").not() }
                .map { it.replace(Regex("\\s*"), "") }
                .map { it.replace(Regex("[Xx]"), "x") }
                .let {
                    if (it.isEmpty()) {
                        throw StructureDescriptionException("Empty structure description")
                    }
                    if ((regime == EPS || regime == N) && it.filter { it[0].isDigit() }.size != 1) {
                        throw StructureDescriptionException("Structure must contain only one layer for this regime")
                    }
                    /* last expression is a return value of 'let' and must be a List<String> */
                    it
                }
                .forEach { add(it.split(",")) }
    }

    /**
     * Validates each tokenized line representing symbolic description of a layer
     *
     * @param tokenizedLines structure representation as lines
     */
    @Throws(StructureDescriptionException::class)
    private fun validateTokenizedLines(tokenizedLines: List<List<String>>) {
        /**
         * Structure description should start with period repeat number
         */
        if (tokenizedLines[0][0].startsWith("x").not()) {
            throw StructureDescriptionException("Structure description should start with period repeat description")
        }
        /**
         * 2 or more consecutive period descriptions
         */
        (1..tokenizedLines.size - 1)
                .filter { tokenizedLines[it - 1][0].contains("x") && tokenizedLines[it][0].contains("x") }
                .forEach {
                    throw StructureDescriptionException("Two period repeat descriptions are found together")
                }
        /**
         * Check period description format
         */
        with(tokenizedLines.flatMap { it }.filter { it.contains(Regex("^[x][0-9]+$")) }) {
            /**
             * There must be a period repeat description using format "x123"
             */
            if (isEmpty()) {
                throw StructureDescriptionException("Period repeat description does not match the specified format")
            }
            /**
             * Each period repeat number should be parsed to Int
             */
            map { it.substring(1) }.forEach { repeat ->
                try {
                    repeat.toInt()
                } catch (e: NumberFormatException) {
                    throw StructureDescriptionException("Period repeat number error")
                }
            }
        }
        /**
         * Check that structure contains at least one layer
         */
        with(tokenizedLines.flatMap { it }.filter { it.contains(Regex("^[x][0-9]+$")).not() }) {
            if (isEmpty()) {
                throw StructureDescriptionException("Structure must contain a single layer at least")
            }
        }
        /**
         * Check the layer type parameter to correspond to the appropriate number of parameters for a layer
         */
        tokenizedLines.filter { it[0].startsWith("x").not() }.forEach {
            try {
                val type = it[0].toInt()
                if (type !in parameterNumbers.keys || parameterNumbers[type] != it.size) {
                    throw StructureDescriptionException("Invalid layer type or incorrect number of parameters for a layer")
                }
            } catch (e: NumberFormatException) {
                throw StructureDescriptionException("Invalid layer type or incorrect number of layer parameters")
            }
        }
        /**
         * Check complex parameters format
         */
        with(tokenizedLines.flatMap { it }) {
            /**
             * Complex parameter description should contain both "(" and ")"
             */
            forEach {
                if ((it.contains("(") && it.contains(")").not()) || (it.contains("(").not() && it.contains(")"))) {
                    throw StructureDescriptionException("Invalid complex parameter value format")
                }
            }
            /**
             * Complex numbers must use the format of "(a; b)"
             * and should be parsed to real: Double and imaginary: Double
             */
            filter { it.contains("(") && it.contains(")") }
                    .map { it.replace(Regex("\\("), "") }
                    .map { it.replace(Regex("\\)"), "") }
                    .forEach {
                        it.split(";").forEach {
                            try {
                                this[0].toDouble() // real
                                this[1].toDouble() // imaginary
                            } catch (e: NumberFormatException) {
                                throw StructureDescriptionException("Complex number format error")
                            }
                        }
                    }
        }
        /**
         * Check double parameters format
         * Each parameter value should be parsed to Double
         */
        tokenizedLines.flatMap { it }
                /* exclude complex and period */
                .filter {
                    (it.contains(Regex("^[x][0-9]+$"))).not() && (it.contains("(") && it.contains(")")).not()
                }
                .forEach { value ->
                    try {
                        value.toDouble()
                    } catch (e: NumberFormatException) {
                        throw StructureDescriptionException("Invalid parameter value format")
                    }
                }
    }

    /**
     * Builds the StructureDescription object using the tokenized data for layers
     *
     * @param tokenizedLines structure representation as lines
     * @return structure description object
     */
    private fun buildStructureDescription(tokenizedLines: List<List<String>>) = StructureDescription().apply {
        val blockDescriptions = blockDescriptions
        var layerIndex = 0
        with(tokenizedLines) {
            while (layerIndex < size) {
                blockDescriptions += BlockDescription(repeat = this[layerIndex][0].substring(1))
                layerIndex++
                /* lazy evaluation. Need to check layerIndex < size FIRST. Otherwise IndexOutOfBoundsException!!! */
                while (layerIndex < size && this[layerIndex].isRepeat().not()) {
                    /**
                     * Add LayerDescriptions to the last BlockDescription
                     */
                    blockDescriptions[blockDescriptions.size - 1].layerDescriptions +=
                            LayerDescription(type = this[layerIndex][0],
                                    description = this[layerIndex].subList(1, this[layerIndex].size))
                    layerIndex++
                }
            }
        }
    }

    /**
     * Builds structure object using the given structure description object
     *
     * @param structureDescription description object
     * @return structure object
     */
    private fun buildStructure(structureDescription: StructureDescription) =
            StructureBuilder.build(structureDescription)

    /**
     * Checks if the line: String contains only one token and represents a repeat number for a block
     */
    private fun List<String>.isRepeat() = size == 1 && this[0].startsWith("x")
}


object MultipleExportDialogParametersValidator {
    fun validateRegime(): ValidateResult {
        if ((regime == EPS || regime == N) && State.mainController.multipleExportDialogController.anglesSelected()) {
            alert(headerText = "Computation regime for multiple export error",
                    contentText = "For permittivity or refractive index computation temperature range must be selected")
            return FAILURE
        }
        return SUCCESS
    }

    fun validateAngles(): ValidateResult {
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

    fun validateTemperatures(): ValidateResult {
        try {
            with(State.mainController.multipleExportDialogController) {
                temperatureFrom = temperatureFromTextField.text.toDouble()
                temperatureTo = temperatureToTextField.text.toDouble()
                temperatureStep = temperatureStepTextField.text.toDouble()
                if (temperatureFrom <= 0.0 || temperatureTo <= 0.0 || temperatureStep <= 0.0 || temperatureFrom > temperatureTo || temperatureStep > temperatureTo) {
                    alert(headerText = "Temperature range error", contentText = "Provide correct temperature range")
                    return FAILURE
                }
            }
        } catch (e: NumberFormatException) {
            alert(headerText = "Temperature range error", contentText = "Provide correct temperature range")
            return FAILURE
        }
        return SUCCESS
    }

    fun validateChosenDirectory(): ValidateResult {
        if (State.mainController.multipleExportDialogController.chosenDirectory == null) {
            alert(headerText = "Directory error", contentText = "Choose a directory")
            return FAILURE
        }
        return SUCCESS
    }
}


class StructureDescriptionException(message: String) : RuntimeException(message)


/**
 * Check angle value
 */
private fun Double.isNotAllowed() = this !in 0.0..89.99999999

/**
 * Show alert
 */
private fun alert(title: String = "Error", headerText: String, contentText: String) = with(Alert(Alert.AlertType.ERROR)) {
    this.title = title
    this.headerText = headerText
    this.contentText = contentText
    showAndWait()
}
