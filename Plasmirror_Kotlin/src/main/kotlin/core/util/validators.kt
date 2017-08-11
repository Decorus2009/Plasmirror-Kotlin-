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

object Validator {
    fun validateAndSetStateUsing(mainController: MainController) = mainController.run {
        println("Validating")
        validateAndSetOpticalParametersUsing(globalParametersController)
        validateAndBuildStructure(structureDescriptionController)
    }
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
                fun Double.isNotAllowed() = this !in 0.0..89.99999999
                if (angleFrom.isNotAllowed() || angleTo.isNotAllowed() || angleStep.isNotAllowed()
                        || angleFrom > angleTo || angleStep > angleTo || angleStep == 0.0) {
                    alert(headerText = "Angle Range Error", contentText = "Provide Correct Angle Range")
                    return FAILURE
                }
            }
        } catch (e: NumberFormatException) {
            alert(headerText = "Angle Range Error", contentText = "Provide Correct Angle Range")
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
                    alert(headerText = "Temperature Range Error", contentText = "Provide Correct Temperature Range")
                    return FAILURE
                }
            }
        } catch (e: NumberFormatException) {
            alert(headerText = "Temperature Range Error", contentText = "Provide Correct Temperature Range")
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

private object OpticalParametersValidator {

    @Throws(StateException::class)
    fun validateAndSetOpticalParametersUsing(globalParametersController: GlobalParametersController) =
            with(globalParametersController) {
                validateRefractiveIndicesUsing(mediumParametersController)
                validateAngleUsing(lightParametersController)
                validateCalculationRangeUsing(computationRangeController)
            }

    @Throws(StateException::class)
    private fun validateRefractiveIndicesUsing(mediumParametersController: MediumParametersController) =
            with(mediumParametersController) {
                try {
                    leftMediumChoiceBox.run {
                        /* left medium == OTHER */
                        if (value == items[2]) {
                            n_left = Cmplx(n_leftRealTextField.text.toDouble(),
                                    n_leftImaginaryTextField.text.toDouble())
                        }
                    }
                    rightMediumChoiceBox.run {
                        /* right medium == OTHER */
                        if (value == items[2]) {
                            n_right = Cmplx(n_rightRealTextField.text.toDouble(),
                                    n_rightImaginaryTextField.text.toDouble())
                        }
                    }
                } catch (e: NumberFormatException) {
                    throw StateException("Wrong medium refractive index format")
                }
            }

    @Throws(StateException::class)
    private fun validateAngleUsing(lightParametersController: LightParametersController) {
        try {
            angle = lightParametersController.angleTextField.text.toDouble()
            if (angle < 0.0 || angle >= 90.0) {
                throw StateException("Wrong angle value")
            }
        } catch (e: NumberFormatException) {
            throw StateException("Wrong angle value format")
        }
    }

    @Throws(StateException::class)
    private fun validateCalculationRangeUsing(computationRangeController: ComputationRangeController) =
            try {
                with(computationRangeController) {
                    wavelengthFrom = wavelengthFromTextField.text.toDouble()
                    wavelengthTo = wavelengthToTextField.text.toDouble()
                    wavelengthStep = wavelengthStepTextField.text.toDouble()
                    if (wavelengthFrom < 0.0 || wavelengthTo <= 0.0 || wavelengthStep <= 0.0 || wavelengthFrom > wavelengthTo) {
                        throw StateException("Wrong calculation range format")
                    }
                }
            } catch (e: NumberFormatException) {
                throw StateException("Wrong calculation range format")
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
    @Throws(StructureDescriptionException::class)
    fun validateAndBuildStructure(structureDescriptionController: StructureDescriptionController) {

        val lines = toLines(structureDescriptionController.structureDescriptionTextArea.text)
        val tokenizedLines = linesToTokenizedLines(lines)
        validateTokenizedLines(tokenizedLines)
        val structureDescription = buildStructureDescription(tokenizedLines)
        structure = buildStructure(structureDescription)
    }

    /**
     * Maps structure string representation to the list of lines.
     * Each line is a symbolic description of a layer
     *
     * @param structure structure string representation
     * @return structure representation as lines
     */
    @Throws(StructureDescriptionException::class)
    private fun toLines(structure: String): List<String> = structure.lines().filter { it.isNotBlank() }.run {
        if (isNotEmpty()) return this
        throw StructureDescriptionException("Empty structure description")
    }

    /**
     * Tokenizes each line of the structure
     *
     * @param lines structure representation as lines
     * @return list of tokenized lines (each tokenized line is a list of tokens)
     */
    private fun linesToTokenizedLines(lines: List<String>): List<List<String>> {

        val tokenizedLines = mutableListOf<List<String>>()
        lines
                .map { it.trim() }
                .filter { !it.startsWith("//") }
                .apply {
                    forEach {
                        /*
                                                it.replace(Regex("\\s*x\\s*"), "x")
                                                it.replace(Regex("\\s*X\\s*"), "x")
                                                it.replace(Regex("\\s*,\\s*"), " ")
                                                it.replace(Regex("\\(\\s*"), "(")
                                                it.replace(Regex("\\s*;\\s*"), ";")
                                                it.replace(Regex("\\s*\\)"), ")")
                        */
                        it.replace(Regex("\\s*"), "")
                        it.replace(Regex("[Xx]"), "x")

                        tokenizedLines.add(it.split(","))
                    }
                }
        return tokenizedLines
    }

    /**
     * Validates each tokenized line representing symbolic description of a layer
     *
     * @param lines structure representation as lines
     */
    @Throws(StructureDescriptionException::class)
    private fun validateTokenizedLines(tokenizedLines: List<List<String>>) {

        /**
         * Structure description should start with period repeat number
         */
        if (!tokenizedLines[0][0].startsWith("x")) {
            throw StructureDescriptionException("Structure description should start with period repeat description")
        }

        /**
         * 2 or more consecutive period descriptions
         */
        (1..tokenizedLines.size - 1)
                .filter { tokenizedLines[it - 1][0].contains("x") && tokenizedLines[it][0].contains("x") }
                .forEach { throw StructureDescriptionException("Two period repeat descriptions are found together") }

        /**
         * Check period description format
         */
        tokenizedLines.flatMap { it }.filter { it.contains(Regex("^[x][0-9]+$")) }.run {

            /**
             * There must be a period repeat description using format "x123"
             */
            if (isEmpty()) {
                throw StructureDescriptionException("Period repeat description does not match the specified format")
            }

            /**
             * Each period repeat number should be parsed to Int
             */
            map { it.substring(1) }
                    .forEach { repeat ->
                        try {
                            repeat.toInt()
                        } catch (e: NumberFormatException) {
                            throw StructureDescriptionException("Period repeat number error")
                        }
                    }
        }


        /**
         * Check the SERIESType parameter to correspond to the appropriate number of parameters for layer
         */
        tokenizedLines
                .filter { !it[0].startsWith("x") } // exclude period lines
                .forEach {
                    try {
                        val type = it[0].toInt()
                        if (type <= 0 || parameterNumbers[type] != it.size) {
                            throw StructureDescriptionException("Wrong layer SERIESType format")
                        }
                    } catch (e: NumberFormatException) {
                        throw StructureDescriptionException("Wrong layer SERIESType format")
                    }
                }

        /**
         * Check complex parameters format
         */
        tokenizedLines.flatMap { it }.run {

            /**
             * Complex parameter description should contain both "(" and ")"
             */
            forEach {
                if ((it.contains("(") && !it.contains(")")) || (!it.contains("(") && it.contains(")"))) {
                    throw StructureDescriptionException("Invalid complex parameter value format")
                }
            }

            /**
             * Complex numbers must use the format of "(a; b)"
             * and should be parsed to real: Double and imag: Double
             */
            filter { it.contains("(") && it.contains(")") }.forEach {
                it.replace(Regex("\\("), "")
                it.replace(Regex("\\)"), "")

                it.split(";").forEach {
                    try {
                        this[0].toDouble() // real
                        this[1].toDouble() // imag
                    } catch (e: NumberFormatException) {
                        throw StructureDescriptionException("Period repeat number error")
                    }
                }
            }
        }

        /**
         * Check double parameters format
         * Each parameter value should be parsed to Double
         */
        tokenizedLines
                .flatMap { it }
                .filter { !(it.contains(Regex("^[x][0-9]+$"))) && !(it.contains("(") && it.contains(")")) } // exclude complex and period
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
     * @param lines structure representation as lines
     * @return structure description object
     */
    private fun buildStructureDescription(tokenizedLines: List<List<String>>): StructureDescription {

        val structureDescription = StructureDescription()
        val blockDescriptions = structureDescription.blockDescriptions
        var layerInd = 0

        tokenizedLines.run {
            while (layerInd < size) {
                blockDescriptions += BlockDescription(repeat = this[layerInd][0].substring(1))
                layerInd++

                // lazy eval. Need to check layerInd < size FIRST. Otherwise IndexOutOfBoundsException!!!
                while (layerInd < size && this[layerInd].isRepeat().not()) {
                    /**
                     * add LayerDescriptions to the last BlockDescription
                     */
                    blockDescriptions[blockDescriptions.size - 1].layerDescriptions +=
                            LayerDescription(type = this[layerInd][0], description = this[layerInd].subList(1, this[layerInd].size))

                    layerInd++
                }
            }
        }
        return structureDescription
    }

    /**
     * Builds structure object using the given structure description object
     *
     * @param structure description object
     * @return structure object
     */
    private fun buildStructure(structureDescription: StructureDescription) =
            StructureBuilder.build(structureDescription)

    /**
     * Checks if the line: String contains only one token and represents a repeat number for a block
     */
    private fun List<String>.isRepeat() = size == 1 && this[0].startsWith("x")
}


class StructureDescriptionException(message: String) : RuntimeException(message)


class StateException(message: String) : RuntimeException(message)

private fun alert(title: String = "Error", headerText: String, contentText: String) = with(Alert(Alert.AlertType.ERROR)) {
    this.title = title
    this.headerText = headerText
    this.contentText = contentText
    showAndWait()
}
