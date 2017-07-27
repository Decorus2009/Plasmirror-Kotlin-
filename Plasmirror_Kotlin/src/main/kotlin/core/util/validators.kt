package core.util

import core.State.angle
import core.State.n_left
import core.State.n_right
import core.State.structure
import core.State.wlEnd
import core.State.wlStart
import core.State.wlStep
import core.util.OpticalParametersValidator.validateAndSetOpticalParameters
import core.util.StructureValidator.validateAndBuildStructure
import ui.controllers.*

object Validator {

    fun validateAndSetStateThrough(mainController: MainController) = mainController.run {

        println("Validating")

        validateAndSetOpticalParameters(globalParametersController)
        validateAndBuildStructure(structureDescriptionController)
    }
}

private object OpticalParametersValidator {

    @Throws(StateException::class)
    fun validateAndSetOpticalParameters(globalParametersController: GlobalParametersController) =
            globalParametersController.run {
                validateRefractiveIndices(mediumParametersController)
                validateAngle(lightParametersController)
                validateCalculationRange(computationRangeController)

            }
}

@Throws(StateException::class)
private fun validateRefractiveIndices(mediumParametersController: MediumParametersController) {

    mediumParametersController.run {
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
}

@Throws(StateException::class)
private fun validateAngle(lightParametersController: LightParametersController) {
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
private fun validateCalculationRange(computationRangeController: ComputationRangeController) {

    computationRangeController.run {
        try {
            wlStart = wLStartTextField.text.toDouble()
            wlEnd = wLEndTextField.text.toDouble()
            wlStep = wLStepTextField.text.toDouble()

            if (wlStart < 0.0 || wlEnd <= 0.0 || wlStep <= 0.0 || wlStart > wlEnd) {
                throw StateException("Wrong calculation range format")
            }
        } catch (e: NumberFormatException) {
            throw StateException("Wrong calculation range format")
        }
    }
}

/**
 * StructureValidator analyzes structure file as a String and builds StructureDescription using LayerDescription and BlockDescription
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
