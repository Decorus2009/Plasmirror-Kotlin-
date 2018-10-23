package ui.controllers

import core.*
import core.Medium.*
import core.Polarization.P
import core.Polarization.S
import core.Regime.*

import javafx.fxml.FXML
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.AnchorPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.io.File.separator
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern
import kotlin.streams.toList


class GlobalParametersController {

    lateinit var mainController: MainController

    @FXML
    lateinit var regimeController: RegimeController
    @FXML
    lateinit var temperatureController: TemperatureController
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
        temperatureController.globalParametersController = this
        computationRangeController.globalParametersController = this
    }

    fun writeGlobalParameters() {
        regimeController.writeRegime()
//        temperatureController.writeTemperature()
        mediumParametersController.writeMediumParametersToFile()
        lightParametersController.writeLightParameters()
        computationRangeController.writeComputationRange()
    }
}

class RegimeController {

    lateinit var globalParametersController: GlobalParametersController

    @FXML
    private lateinit var regimeChoiceBox: ChoiceBox<String>

    var regimeBefore: Regime? = null

    @FXML
    fun initialize() {
        println("Regime controller init")

        val regimes = mapOf(
                R to 0, T to 1, A to 2, PERMITTIVITY to 3, REFRACTIVE_INDEX to 4
        )
        val inversed = regimes.map { it.value to it.key }

        with(regimeChoiceBox) {
            value = items[regimes[ComputationParameters.regime]!!]

            selectionModel.selectedIndexProperty().addListener { _, _, newValue ->
                val regime = inversed[newValue as Int].second
                ComputationParameters.regime = regime

                with(globalParametersController) {
                    when (regime) {
                        R, T, A -> {
                            mediumParametersController.enableAll()
                            lightParametersController.enableAll()
                        }
                        PERMITTIVITY, REFRACTIVE_INDEX -> {
                            mediumParametersController.disableAll()
                            lightParametersController.disableAll()
                        }
                    }
                }
            }
        }
    }
}

class TemperatureController {

    lateinit var globalParametersController: GlobalParametersController

    @FXML
    private lateinit var T_TextField: TextField

//    private val path = Paths.get(".${File.separator}data${File.separator}inner${File.separator}state_parameters${File.separator}temperature.txt")

    @FXML
    fun initialize() {
        println("Temperature controller init")
        /* init initial values */
        T_TextField.isDisable = true

        /* TODO read from file */
    }
}

class MediumParametersController {

    @FXML
    private lateinit var leftMediumLabel: Label
    @FXML
    private lateinit var rightMediumLabel: Label
    @FXML
    private lateinit var nLeftMediumLabel: Label
    @FXML
    private lateinit var nRightMediumLabel: Label
    @FXML
    lateinit var nLeftRealTextField: TextField
    @FXML
    lateinit var nLeftImagTextField: TextField
    @FXML
    lateinit var nRightRealTextField: TextField
    @FXML
    lateinit var nRightImagTextField: TextField
    @FXML
    lateinit var leftMediumChoiceBox: ChoiceBox<String>
    @FXML
    lateinit var rightMediumChoiceBox: ChoiceBox<String>

    @FXML
    fun initialize() {
        println("Medium parameters controller init")

        val mediaMap = mapOf(
                AIR to 0, GAAS_ADACHI to 1, GAAS_GAUSS to 2, GAAS_GAUSS_ADACHI to 3, OTHER to 4
        )
        val invMap = mediaMap.map { it.value to it.key }

        fun initMediumFields(medium: Medium,
                             mediumRefractiveIndex: Complex_,
                             mediumChoiceBox: ChoiceBox<String>,
                             nRealTextField: TextField, nImagTextField: TextField) {

            with(mediumChoiceBox) {
                value = items[mediaMap[medium]!!]

                if (medium == OTHER) {
                    val n = mediumRefractiveIndex
                    nRealTextField.run {
                        enable(this)
                        text = n.real.toString()
                    }
                    nImagTextField.run {
                        enable(this)
                        text = n.imaginary.toString()
                    }
                }

                selectionModel.selectedIndexProperty().addListener { _, _, newValue ->
                    when (medium) {
                        OTHER -> enable(nRealTextField, nImagTextField)
                        else -> disable(nRealTextField, nImagTextField)
                    }
                }
            }
        }


        initMediumFields(
                ComputationParameters.leftMedium,
                ComputationParameters.leftMediumRefractiveIndex,
                leftMediumChoiceBox,
                nLeftRealTextField, nLeftImagTextField
        )
        initMediumFields(
                ComputationParameters.rightMedium,
                ComputationParameters.rightMediumRefractiveIndex,
                rightMediumChoiceBox,
                nRightRealTextField, nRightImagTextField
        )

//        leftMediumChoiceBox.selectionModel.selectedIndexProperty().addListener { _, _, newValue ->
//            ComputationParameters.leftMedium = invMap[newValue as Int].second
//        }
//        rightMediumChoiceBox.selectionModel.selectedIndexProperty().addListener { _, _, newValue ->
//            ComputationParameters.rightMedium = invMap[newValue as Int].second
//        }
    }

    fun disableAll() {
        disable(leftMediumLabel, rightMediumLabel, nLeftMediumLabel, nRightMediumLabel)
        disable(leftMediumChoiceBox, rightMediumChoiceBox)
        disable(nLeftRealTextField, nLeftImagTextField, nRightRealTextField, nRightImagTextField)
    }

    fun enableAll() {
        enable(leftMediumLabel, rightMediumLabel, nLeftMediumLabel, nRightMediumLabel)
        enable(leftMediumChoiceBox, rightMediumChoiceBox)

        if (ComputationParameters.leftMedium == OTHER) {
            enable(nLeftRealTextField, nLeftImagTextField)
        }
        if (ComputationParameters.rightMedium == OTHER) {
            enable(nRightRealTextField, nRightImagTextField)
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

        val polarizationMap = mapOf(P to 0, S to 1)
        val invMap = polarizationMap.map { it.value to it.key }

        with(polarizationChoiceBox) {
            value = items[polarizationMap[ComputationParameters.polarization]!!]

            selectionModel.selectedIndexProperty().addListener { _, _, newValue ->
                ComputationParameters.polarization = invMap[newValue as Int].second
            }
        }

        with(angleTextField) {
            val previousValue = ComputationParameters.angle
            text = previousValue.toString()

            textProperty().addListener { _, _, newValue ->
                try {
                    ComputationParameters.angle = newValue.toDouble()
                } catch (e: NumberFormatException) {
                    ComputationParameters.angle = previousValue
                }
            }
        }
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

        with(fromTextField) {
            val previousValue = ComputationParameters.computationRangeStart
            text = previousValue.toString()

            textProperty().addListener { _, _, newValue ->
                try {
                    ComputationParameters.computationRangeStart = newValue.toDouble()
                } catch (e: NumberFormatException) {
                    ComputationParameters.computationRangeStart = previousValue
                }
            }
        }

        with(toTextField) {
            val previousValue = ComputationParameters.computationRangeEnd
            text = previousValue.toString()

            textProperty().addListener { _, _, newValue ->
                try {
                    ComputationParameters.computationRangeEnd = newValue.toDouble()
                } catch (e: NumberFormatException) {
                    ComputationParameters.computationRangeEnd = previousValue
                }
            }
        }

        with(toTextField) {
            val previousValue = ComputationParameters.computationRangeStep
            text = previousValue.toString()

            textProperty().addListener { _, _, newValue ->
                try {
                    ComputationParameters.computationRangeStep = newValue.toDouble()
                } catch (e: NumberFormatException) {
                    ComputationParameters.computationRangeStep = previousValue
                }
            }
        }
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