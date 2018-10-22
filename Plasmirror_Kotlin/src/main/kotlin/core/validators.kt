package core

import core.Complex_.Companion.ONE
import core.Medium.*
import core.OpticalParametersValidator.validateAndSetOpticalParametersUsing
import core.Regime.EPS
import core.Regime.N
import core.State.angle
import core.State.leftMedium
import core.State.leftMediumLayer
import core.State.n_left
import core.State.n_right
import core.State.regime
import core.State.rightMedium
import core.State.rightMediumLayer
import core.State.structure
import core.State.wavelengthFrom
import core.State.wavelengthStep
import core.State.wavelengthTo
import core.StructureValidator.validateAndBuildStructure
import core.ValidateResult.FAILURE
import core.ValidateResult.SUCCESS
import core.layers.ConstRefractiveIndexLayer
import core.layers.GaAs
import javafx.scene.control.Alert
import ui.controllers.*
import kotlin.Double.Companion.POSITIVE_INFINITY


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
                        n_left = Complex_(n_leftRealTextField.text.toDouble(), n_leftImaginaryTextField.text.toDouble())
                    }
                }
                with(rightMediumChoiceBox) {
                    /* right medium == OTHER */
                    if (value == items[2]) {
                        n_right = Complex_(n_rightRealTextField.text.toDouble(), n_rightImaginaryTextField.text.toDouble())
                    }
                }
            } catch (e: NumberFormatException) {
                alert(headerText = "Refractive index value error", contentText = "Provide correct refractive index")
                return FAILURE
            }
        }

        /*
        !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        duplicated from MirrorBuilder.build() so that left medium layer be initialized before the structure building
         */

        leftMediumLayer = when (leftMedium) {
            AIR -> ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = ONE)
            GAAS -> GaAs(d = POSITIVE_INFINITY, epsType = EpsType.GAUSS)
            OTHER -> ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = n_left)
        }
        rightMediumLayer = when (rightMedium) {
            AIR -> ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = ONE)
            GAAS -> GaAs(d = POSITIVE_INFINITY, epsType = EpsType.GAUSS)
            OTHER -> ConstRefractiveIndexLayer(d = POSITIVE_INFINITY, n = n_right)
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
                wavelengthFrom = fromTextField.text.toDouble()
                wavelengthTo = toTextField.text.toDouble()
                wavelengthStep = stepTextField.text.toDouble()
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
            "1-1" to 2, "1-2" to 2, "1-3" to 2,
            "2-1" to 4, "2-2" to 4, "2-3" to 4,
            "3" to 3,
            "4-1" to 5, "4-2" to 5, "4-3" to 5,
            "5-1" to 7, "5-2" to 7, "5-3" to 7,
            "6" to 6,
            "7-1" to 8, "7-2" to 8, "7-3" to 8,
            "8-1" to 8, "8-2" to 8, "8-3" to 8,
            "9-1" to 5, "9-2" to 5, "9-3" to 5
    )

    /**
     * Validates and builds structure description representation from the text field of structureDescriptionController
     *
     * @param structureDescriptionController controller to get its text field
     * containing the structure description text representation
     */
    fun validateAndBuildStructure(structureDescriptionController: StructureDescriptionController): ValidateResult {
        try {
            val lines = toLines(structureDescriptionController.structureDescriptionCodeArea.text)
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
     * Maps structure string representation to the list of lines
     * such that each line represents description of a layer or period.
     * Ignore: single-line comments, multi-line comments
     * Expand each layer description named tokens to single line:
     *
     *    type = 7-2, d = 10, x = 0.31,
     *    wPlasma = 7.38, gammaPlasma = 0.18,  ---->  type = 7-2, d = 10, x = 0.31, wPlasma = 7.38, gammaPlasma = 0.18, f = 0.0017
     *    f = 0.0017
     *
     * Remove empty lines
     *
     * (?s) activates Pattern.DOTALL notation.
     * "In dotall mode, the expression . matches any character, including a line terminator.
     * By default this expression does not match line terminators."
     *
     * @param structure structure string representation
     * @return structure representation as lines
     */
    @Throws(StructureDescriptionException::class)
    private fun toLines(structure: String): List<String> = structure.toLowerCase()
            /** exclude multi-line comments */
            .replace(Regex("(?s)/\\*.*\\*/"), "")
            /** exclude single-line comments */
            .replace(Regex("\\s*[/]{2,}.*"), "")
            /**
            replace all new line characters by whitespaces
            (the case when the structure description of some layer occupied several lines)
             */
            .replace(Regex("\\R*"), "")
            /** remove all whitespaces */
            .replace(Regex("\\s*"), "")
            /** put period descriptions to separate lines */
            .replace(Regex("([x][0-9]+)"), "\n\$1\n")
            /** each line will start with the keyword "type" */
            .replace(Regex("(type)"), "\n\$1")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

    /**
     * Tokenizes each line of the structure
     *
     * @param lines structure representation as lines
     * @return list of tokenized lines (each tokenized line is a list of tokens)
     */
    private fun linesToTokenizedLines(lines: List<String>) = mutableListOf<List<String>>().apply {
        /** remove names of layer parameters ("type=", "d=", "k=", "x=") */
        lines.forEach { add(it.split(",").map { it.replace(Regex(".+=+"), "") }) }
    }

    /**
     * Validates each tokenized line representing symbolic description of a layer
     *
     * @param tokenizedLines structure representation as lines
     */
    @Throws(StructureDescriptionException::class)
    private fun validateTokenizedLines(tokenizedLines: List<List<String>>) = with(tokenizedLines) {
        /**
        Structure description is empty
         */
        if (isEmpty()) {
            throw StructureDescriptionException("Empty structure description")
        }
        /**
        Structure description should start with period repeat number
         */
        if (this[0][0].startsWith("x").not()) {
            throw StructureDescriptionException("Structure description should start with period repeat description")
        }
        /**
        2 or more consecutive period descriptions
         */
        (1..size - 1).filter { this[it - 1][0].contains("x") && this[it][0].contains("x") }.forEach {
            throw StructureDescriptionException("Two period repeat descriptions are found together")
        }
        /**
        Check period description format
         */
        with(flatMap { it }.filter { it.contains(Regex("^[x][0-9]+$")) }) {
            /**
            There must be a period repeat description using format "x123"
             */
            if (isEmpty()) {
                throw StructureDescriptionException("Period repeat description does not match the specified format")
            }
            /**
            Each period repeat number should be parsed to Int
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
        Check that structure contains at least one layer
         */
        with(flatMap { it }.filterNot { it.contains("x") }) {
            if (isEmpty()) {
                throw StructureDescriptionException("Structure must contain a single layer at least")
            }
        }
        /**
        Check that structure contains only one layer for EPS and N regimes
        (check that tokenizedLines contains only one List<String> with layer parameters)
         */
        if ((regime == EPS || regime == N) && filterNot { it.all { it.contains("x") } }.size != 1) {
            throw StructureDescriptionException("Structure must contain only one layer for this regime")
        }
        /**
        Check the layer type parameter to correspond to the appropriate number of parameters for a layer
         */
        filterNot { it[0].startsWith("x") }.forEach {
            val type = it[0]
            if (type !in parameterNumbers.keys || parameterNumbers[type] != it.size) {
                println(type)
                println(parameterNumbers[type])
                println(it)
                throw StructureDescriptionException("Invalid layer type or incorrect number of parameters for a layer")
            }
        }
        /**
        Check complex and double parameters format
         */
        with(flatMap { it }) {
            /**
            Complex parameter description should contain both "(" and ")"
             */
            forEach {
                if ((it.contains("(") && it.contains(")").not()) || (it.contains("(").not() && it.contains(")"))) {
                    throw StructureDescriptionException("Invalid complex parameter value format")
                }
            }
            /**
            Complex numbers must use the format of "(a; b)"
            and should be parsed to real: Double and imaginary: Double
             */
            filter { it.contains("(") && it.contains(")") }
                    .map { it.replace(Regex("\\("), "") }
                    .map { it.replace(Regex("\\)"), "") }
                    .forEach {
                        it.split(";").let {
                            try {
                                it[0].toDouble() // real
                                it[1].toDouble() // imaginary
                            } catch (e: NumberFormatException) {
                                throw StructureDescriptionException("Complex number format error")
                            }
                        }
                    }
            /**
            Check double parameters format. Each parameter value should be parsed to Double.
            Exclude complex, period and type
             */
            filterNot { it.contains("x") || it.contains("(") || it.contains("-") }.forEach { value ->
                try {
                    value.toDouble()
                } catch (e: NumberFormatException) {
                    throw StructureDescriptionException("Invalid parameter value format")
                }
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
                    Add LayerDescriptions to the last BlockDescription
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
