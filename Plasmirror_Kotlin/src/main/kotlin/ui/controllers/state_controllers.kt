package ui.controllers

import core.ComputationParametersStorage
import core.optics.Regime
import core.StructureDescriptionStorage
import javafx.fxml.FXML
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.AnchorPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.regex.Pattern


class GlobalParametersController {

    lateinit var mainController: MainController

    @FXML
    lateinit var regimeController: RegimeController
    @FXML
    lateinit var mediumParametersController: MediumParametersController
    @FXML
    lateinit var lightParametersController: LightParametersController
    @FXML
    lateinit var computationRangeController: ComputationRangeController

    @FXML
    fun initialize() {
        println("Global parameters controller init")

        regimeController.globalParametersController = this
        computationRangeController.globalParametersController = this
    }

    fun save() {
        regimeController.save()
        mediumParametersController.save()
        lightParametersController.save()
        computationRangeController.save()

//        temperatureController.writeTemperature()
    }
}

class RegimeController {

    lateinit var globalParametersController: GlobalParametersController

    @FXML
    private lateinit var regimeChoiceBox: ChoiceBox<String>

    var regimeBefore: Regime? = null

//    private val regimes = mapOf(
//            REFLECTANCE to 0, TRANSMITTANCE to 1, ABSORBANCE to 2, PERMITTIVITY to 3, REFRACTIVE_INDEX to 4
//    )
//    private val regimesMap = mapOf(
//            "Reflectance" to 0, "Transmittance" to 1, "Absorbance" to 2, "Permittivity" to 3, "Refractive Index" to 4
//    )

    @FXML
    fun initialize() {
        println("Regime controller init")

        with(regimeChoiceBox) {
            value = ComputationParametersStorage.regime

            selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                with(globalParametersController) {
                    when (newValue) {
                        "Reflectance", "Transmittance", "Absorbance" -> {
                            mediumParametersController.enableAll()
                            lightParametersController.enableAll()
                        }
                        "Permittivity", "Refractive Index" -> {
                            mediumParametersController.disableAll()
                            lightParametersController.disableAll()
                        }
                    }
                }
            }
        }
    }

    fun save() {
        ComputationParametersStorage.regime = regimeChoiceBox.value
    }
}

//class TemperatureController {
//
//    lateinit var globalParametersController: GlobalParametersController
//
//    @FXML
//    private lateinit var T_TextField: TextField
//
////    private val path = Paths.get(".${File.separator}data${File.separator}inner${File.separator}state_parameters${File.separator}temperature.txt")
//
//    @FXML
//    fun initialize() {
//        println("Temperature controller init")
//        /* init initial values */
//        T_TextField.isDisable = true
//
//        /* TODO read from file */
//    }
//}

class MediumParametersController {

    @FXML
    private lateinit var leftMediumLabel: Label
    @FXML
    private lateinit var rightMediumLabel: Label
    @FXML
    private lateinit var leftMediumRefractiveIndexLabel: Label
    @FXML
    private lateinit var rightMediumRefractiveIndexLabel: Label
    @FXML
    lateinit var leftMediumRefractiveIndexRealTextField: TextField
    @FXML
    lateinit var leftMediumRefractiveIndexImaginaryTextField: TextField
    @FXML
    lateinit var rightMediumRefractiveIndexRealTextField: TextField
    @FXML
    lateinit var rightMediumRefractiveIndexImaginaryTextField: TextField
    @FXML
    lateinit var leftMediumChoiceBox: ChoiceBox<String>
    @FXML
    lateinit var rightMediumChoiceBox: ChoiceBox<String>


//    private val media = mapOf(
//            AIR to 0, GAAS_ADACHI to 1, GAAS_GAUSS to 2, GAAS_GAUSS_ADACHI to 3, CUSTOM to 4
//    )

//    private val media = mapOf(
//            "Air" to 0, "GaAs: Adachi" to 1, "GaAs: Gauss" to 2, "GaAs: Gauss-Adachi" to 3, "Custom" to 4
//    )
//    private val invMedia = media.map { it.value to it.key }

    @FXML
    fun initialize() {
        println("Medium parameters controller init")

        fun initFields(medium: String,
                       mediumRefractiveIndexReal: String,
                       mediumRefractiveIndexImaginary: String,
                       mediumChoiceBox: ChoiceBox<String>,
                       nRealTextField: TextField, nImagTextField: TextField) {

            with(mediumChoiceBox) {
                value = medium

                if (medium == "Custom") {
                    nRealTextField.run {
                        enable(this)
                        text = mediumRefractiveIndexReal
                    }
                    nImagTextField.run {
                        enable(this)
                        text = mediumRefractiveIndexImaginary
                    }
                }

                selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                    when (newValue) {
                        "Custom" -> enable(nRealTextField, nImagTextField)
                        else -> disable(nRealTextField, nImagTextField)
                    }
                }
            }
        }

        initFields(
                ComputationParametersStorage.leftMedium,
                ComputationParametersStorage.leftMediumRefractiveIndexReal,
                ComputationParametersStorage.leftMediumRefractiveIndexImaginary,
                leftMediumChoiceBox,
                leftMediumRefractiveIndexRealTextField, leftMediumRefractiveIndexImaginaryTextField
        )
        initFields(
                ComputationParametersStorage.rightMedium,
                ComputationParametersStorage.rightMediumRefractiveIndexReal,
                ComputationParametersStorage.rightMediumRefractiveIndexImaginary,
                rightMediumChoiceBox,
                rightMediumRefractiveIndexRealTextField, rightMediumRefractiveIndexImaginaryTextField
        )

//        leftMediumChoiceBox.selectionModel.selectedIndexProperty().addListener { _, _, newValue ->
//            ComputationParametersStorage.leftMedium = invMap[newValue as Int].second
//        }
//        rightMediumChoiceBox.selectionModel.selectedIndexProperty().addListener { _, _, newValue ->
//            ComputationParametersStorage.rightMedium = invMap[newValue as Int].second
//        }
    }

    fun disableAll() {
        disable(leftMediumLabel, rightMediumLabel, leftMediumRefractiveIndexLabel, rightMediumRefractiveIndexLabel)
        disable(leftMediumChoiceBox, rightMediumChoiceBox)
        disable(leftMediumRefractiveIndexRealTextField, leftMediumRefractiveIndexImaginaryTextField,
                rightMediumRefractiveIndexRealTextField, rightMediumRefractiveIndexImaginaryTextField)
    }

    fun enableAll() {
        enable(leftMediumLabel, rightMediumLabel, leftMediumRefractiveIndexLabel, rightMediumRefractiveIndexLabel)
        enable(leftMediumChoiceBox, rightMediumChoiceBox)

        if (leftMediumChoiceBox.value == "Custom") {
            enable(leftMediumRefractiveIndexRealTextField, leftMediumRefractiveIndexImaginaryTextField)
        }
        if (rightMediumChoiceBox.value == "Custom") {
            enable(rightMediumRefractiveIndexRealTextField, rightMediumRefractiveIndexImaginaryTextField)
        }
    }

    fun save() = with(ComputationParametersStorage) {

        leftMedium = leftMediumChoiceBox.value
        if (leftMedium == "Custom") {
            leftMediumRefractiveIndexReal = leftMediumRefractiveIndexRealTextField.text
            leftMediumRefractiveIndexImaginary = leftMediumRefractiveIndexImaginaryTextField.text
        } else {
            leftMediumRefractiveIndexReal = "1.0"
            leftMediumRefractiveIndexImaginary = "0.0"
        }

        rightMedium = rightMediumChoiceBox.value
        if (rightMedium == "Custom") {
            rightMediumRefractiveIndexReal = rightMediumRefractiveIndexRealTextField.text
            rightMediumRefractiveIndexImaginary = rightMediumRefractiveIndexImaginaryTextField.text
        } else {
            rightMediumRefractiveIndexReal = "1.0"
            rightMediumRefractiveIndexImaginary = "0.0"
        }
    }
}

class LightParametersController {

    @FXML
    private lateinit var angleLabel: Label
    @FXML
    private lateinit var polarizationLabel: Label
    @FXML
    lateinit var polarizationChoiceBox: ChoiceBox<String>
    @FXML
    lateinit var angleTextField: TextField

    @FXML
    fun initialize() {
        println("Light parameters controller init")

        polarizationChoiceBox.value = ComputationParametersStorage.polarization
        angleTextField.text = ComputationParametersStorage.angle
    }

    fun disableAll() {
        disable(polarizationLabel, angleLabel)
        disable(polarizationChoiceBox)
        disable(angleTextField)
    }

    fun enableAll() {
        enable(polarizationLabel, angleLabel)
        enable(polarizationChoiceBox)
        enable(angleTextField)
    }

    fun save() = with(ComputationParametersStorage) {
        polarization = polarizationChoiceBox.value
        angle = angleTextField.text
    }
}

class ComputationRangeController {

    lateinit var globalParametersController: GlobalParametersController

    @FXML
    lateinit var fromTextField: TextField
    @FXML
    lateinit var toTextField: TextField
    @FXML
    lateinit var stepTextField: TextField

    @FXML
    fun initialize() {
        println("Computation range controller init")

        with(ComputationParametersStorage) {
            fromTextField.text = wavelengthStart
            toTextField.text = wavelengthEnd
            stepTextField.text = wavelengthStep
        }

//        with(fromTextField) {
//            val previousValue = ComputationParametersStorage.wavelengthStart
//            text = previousValue.toString()
//
//            textProperty().addListener { _, _, newValue ->
//                try {
//                    ComputationParametersStorage.wavelengthStart = newValue.toDouble()
//                } catch (e: NumberFormatException) {
//                    ComputationParametersStorage.wavelengthStart = previousValue
//                }
//            }
//        }
//
//        with(toTextField) {
//            val previousValue = ComputationParametersStorage.wavelengthEnd
//            text = previousValue.toString()
//
//            textProperty().addListener { _, _, newValue ->
//                try {
//                    ComputationParametersStorage.wavelengthEnd = newValue.toDouble()
//                } catch (e: NumberFormatException) {
//                    ComputationParametersStorage.wavelengthEnd = previousValue
//                }
//            }
//        }
//
//        with(toTextField) {
//            val previousValue = ComputationParametersStorage.wavelengthStep
//            text = previousValue.toString()
//
//            textProperty().addListener { _, _, newValue ->
//                try {
//                    ComputationParametersStorage.wavelengthStep = newValue.toDouble()
//                } catch (e: NumberFormatException) {
//                    ComputationParametersStorage.wavelengthStep = previousValue
//                }
//            }
//        }
    }

    fun save() = with(ComputationParametersStorage) {
        wavelengthStart = fromTextField.text
        wavelengthEnd = toTextField.text
        wavelengthStep = stepTextField.text
    }
}

class StructureDescriptionController {

    @FXML
    private var anchorPane = AnchorPane()
    val structureDescriptionCodeArea = CodeArea()

    @FXML
    fun initialize() = with(structureDescriptionCodeArea) {
        anchorPane.children.add(this)
        AnchorPane.setTopAnchor(this, 0.0)
        AnchorPane.setBottomAnchor(this, 0.0)
        AnchorPane.setRightAnchor(this, 0.0)
        AnchorPane.setLeftAnchor(this, 0.0)

        richChanges().filter { it.inserted != it.removed }.subscribe { setStyleSpans(0, computeHighlighting(text)) }
        style = """
                -fx-font-family: system;
                -fx-font-size: 11pt;
                -fx-highlight-fill: #dbdddd;
                -fx-highlight-text-fill: #dbdddd;
        """
        replaceText(0, 0, StructureDescriptionStorage.description)
    }

    fun save() {
        StructureDescriptionStorage.description = structureDescriptionCodeArea.text
    }

    /**
     * Code in this method is used using Java style as in the example (to be able to understand what's going on here)
     */
    private fun computeHighlighting(text: String): StyleSpans<Collection<String>> {
        val COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"
        val NAME_PATTERN = "\\w+\\s*=\\s*+"
        val REPEAT_PATTERN = "\\s*[xX]\\s*[0-9]+\\s*"
        val PATTERN = Pattern.compile("(?<COMMENT>$COMMENT_PATTERN)|(?<NAME>$NAME_PATTERN)|(?<REPEAT>$REPEAT_PATTERN)")

        val matcher = PATTERN.matcher(text)
        var lastKwEnd = 0
        val spansBuilder = StyleSpansBuilder<Collection<String>>()
        while (matcher.find()) {
            val styleClass = (when {
                matcher.group("COMMENT") != null -> "comment"
                matcher.group("NAME") != null -> "name"
                matcher.group("REPEAT") != null -> "repeat"
                else -> null
            })!! /* never happens */
            spansBuilder.add(kotlin.collections.emptyList<String>(), matcher.start() - lastKwEnd)
            spansBuilder.add(setOf(styleClass), matcher.end() - matcher.start())
            lastKwEnd = matcher.end()
        }
        spansBuilder.add(kotlin.collections.emptyList<String>(), text.length - lastKwEnd)
        return spansBuilder.create()
    }
}