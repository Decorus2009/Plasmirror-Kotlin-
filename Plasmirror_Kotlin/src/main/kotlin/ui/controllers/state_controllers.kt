package ui.controllers

import core.State
import core.State.leftMedium
import core.State.n_left
import core.State.n_right
import core.State.polarization
import core.State.regime
import core.State.rightMedium
import core.util.Medium
import core.util.Medium.*
import core.util.Polarization
import core.util.Polarization.P
import core.util.Polarization.S
import core.util.Regime
import core.util.Regime.*
import javafx.fxml.FXML
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import java.io.File.separator
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList


class GlobalParametersController {

    lateinit var mainController: MainController

    @FXML lateinit var regimeController: RegimeController
    @FXML lateinit var temperatureController: TemperatureController
    @FXML lateinit var mediumParametersController: MediumParametersController
    @FXML lateinit var lightParametersController: LightParametersController
    @FXML lateinit var computationRangeController: ComputationRangeController

    @FXML
    fun initialize() {
        println("Global parameters controller set")
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

    @FXML private lateinit var regimeChoiceBox: ChoiceBox<String>

    var regimeBefore: Regime? = null
    private val path = Paths.get(".${separator}data${separator}inner${separator}state_parameters${separator}regime.txt")

    @FXML
    fun initialize() {
        println("Regime controller set")
        /* set initial value */
        /* lines.size should be == 1 */
        val lines = Files.lines(path).toList().filter { it.isNotBlank() }
        regime = Regime.valueOf(lines[0])
        with(regimeChoiceBox) {
            value = when (regime) {
                R -> items[0]
                T -> items[1]
                A -> items[2]
                EPS -> items[3]
                N -> items[4]
            }
            selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
                val newRegime = Regime.valueOf(newValue.toUpperCase())
                regime = newRegime
                regimeBefore = Regime.valueOf(oldValue.toUpperCase())

                with(globalParametersController) {
                    if (newRegime == R || newRegime == T || newRegime == A) {
                        mediumParametersController.enableAll()
                        lightParametersController.enableAll()
                    } else if (newRegime == EPS || newRegime == N) {
                        mediumParametersController.disableAll()
                        lightParametersController.disableAll()
                    }
                }
            }
        }
    }

    fun writeRegime() = "${State.regime}".writeTo(path.toFile())
}

class TemperatureController {

    lateinit var globalParametersController: GlobalParametersController

    @FXML private lateinit var T_TextField: TextField

//    private val path = Paths.get(".${File.separator}data${File.separator}inner${File.separator}state_parameters${File.separator}temperature.txt")

    @FXML
    fun initialize() {
        println("Temperature controller set")
        /* set initial values */
        T_TextField.isDisable = true

        // TODO read from file
    }
}

class MediumParametersController {

    @FXML private lateinit var leftMediumLabel: Label
    @FXML private lateinit var rightMediumLabel: Label
    @FXML private lateinit var n_leftMediumLabel: Label
    @FXML private lateinit var n_rightMediumLabel: Label
    @FXML lateinit var n_leftRealTextField: TextField
    @FXML lateinit var n_leftImaginaryTextField: TextField
    @FXML lateinit var n_rightRealTextField: TextField
    @FXML lateinit var n_rightImaginaryTextField: TextField
    @FXML lateinit var leftMediumChoiceBox: ChoiceBox<String>
    @FXML lateinit var rightMediumChoiceBox: ChoiceBox<String>

    private val path = Paths.get(".${separator}data${separator}inner${separator}state_parameters${separator}medium_parameters.txt")

    @FXML
    fun initialize() {
        println("Medium parameters controller set")
        /* set initial values */
        /* lines.size should be == 6 */
        val lines = Files.lines(path).toList().filter { it.isNotBlank() }

        leftMedium = Medium.valueOf(lines[0])
        with(leftMediumChoiceBox) {
            value = when (leftMedium) {
                AIR -> items[0]
                GAAS -> items[1]
                OTHER -> {
                    n_leftRealTextField.run {
                        enable(this)
                        text = lines[1]
                    }
                    n_leftImaginaryTextField.run {
                        enable(this)
                        text = lines[2]
                    }
                    items[2]
                }
            }
            selectionModel.selectedItemProperty().addListener { _, _, _ ->
                leftMedium = Medium.valueOf(value.toUpperCase())
                if (leftMedium == OTHER) {
                    enable(n_leftRealTextField, n_leftImaginaryTextField)
                } else {
                    disable(n_leftRealTextField, n_leftImaginaryTextField)
                }
            }
        }
        rightMedium = Medium.valueOf(lines[3])
        with(rightMediumChoiceBox) {
            value = when (rightMedium) {
                AIR -> items[0]
                GAAS -> items[1]
                OTHER -> {
                    n_rightRealTextField.run {
                        enable(this)
                        text = lines[4]
                    }
                    n_rightImaginaryTextField.run {
                        enable(this)
                        text = lines[5]
                    }
                    items[2]
                }
            }
            selectionModel.selectedItemProperty().addListener { _, _, _ ->
                rightMedium = Medium.valueOf(value.toUpperCase())
                if (rightMedium == OTHER) {
                    enable(n_rightRealTextField, n_rightImaginaryTextField)
                } else {
                    disable(n_rightRealTextField, n_rightImaginaryTextField)
                }
            }
        }
    }

    fun disableAll() {
        disable(leftMediumLabel, rightMediumLabel, n_leftMediumLabel, n_rightMediumLabel)
        disable(leftMediumChoiceBox, rightMediumChoiceBox)
        disable(n_leftRealTextField, n_leftImaginaryTextField, n_rightRealTextField, n_rightImaginaryTextField)
    }

    fun enableAll() {
        enable(leftMediumLabel, rightMediumLabel, n_leftMediumLabel, n_rightMediumLabel)
        enable(leftMediumChoiceBox, rightMediumChoiceBox)
        if (leftMedium == OTHER) {
            enable(n_leftRealTextField, n_leftImaginaryTextField)
        }
        if (rightMedium == OTHER) {
            enable(n_rightRealTextField, n_rightImaginaryTextField)
        }
    }

    fun writeMediumParametersToFile() = StringBuilder().apply {
        append(leftMedium.toString()).append("\n")
        when (leftMedium) {
            OTHER -> with(n_left) { append(real).append("\n").append(imaginary).append("\n") }
            else -> append("-\n-\n")
        }

        append(rightMedium.toString()).append("\n")
        when (rightMedium) {
            OTHER -> with(n_right) { append(real).append("\n").append(imaginary).append("\n") }
            else -> append("-\n-\n")
        }
    }.toString().writeTo(path.toFile())
}

class LightParametersController {

    @FXML private lateinit var angleLabel: Label
    @FXML private lateinit var polarizationLabel: Label
    @FXML lateinit var polarizationChoiceBox: ChoiceBox<String>
    @FXML lateinit var angleTextField: TextField

    private val path = Paths.get(".${separator}data${separator}inner${separator}state_parameters${separator}light_parameters.txt")

    @FXML
    fun initialize() {
        println("Light parameters controller set")
        /* set initial values */
        /* lines.size should be == 2 */
        val lines = Files.lines(path).toList().filter { it.isNotBlank() }
        polarization = Polarization.valueOf(lines[0])
        angleTextField.text = lines[1]

        with(polarizationChoiceBox) {
            value = when (polarization) {
                P -> items[0]
                S -> items[1]
            }
            selectionModel.selectedItemProperty().addListener { _, _, _ -> polarization = Polarization.valueOf(value) }
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

    fun writeLightParameters() = "${State.polarization}\n${State.angle}".writeTo(path.toFile())
}

class ComputationRangeController {

    lateinit var globalParametersController: GlobalParametersController

    @FXML lateinit var wavelengthFromTextField: TextField
    @FXML lateinit var wavelengthToTextField: TextField
    @FXML lateinit var wavelengthStepTextField: TextField

    private val path = Paths.get(".${separator}data${separator}inner${separator}state_parameters${separator}computation_range.txt")

    @FXML
    fun initialize() {
        println("Computation range controller set")
        /* set initial values */
        /* lines.size should be == 3 */
        val lines = Files.lines(path).toList().filter { it.isNotBlank() }

        wavelengthFromTextField.text = lines[0]
        wavelengthToTextField.text = lines[1]
        wavelengthStepTextField.text = lines[2]
    }

    fun writeComputationRange() = "${State.wavelengthFrom}\n${State.wavelengthTo}\n${State.wavelengthStep}".writeTo(path.toFile())
}

class StructureDescriptionController {

    @FXML lateinit var structureDescriptionTextArea: TextArea

    private val path = Paths.get(".${separator}data${separator}inner${separator}state_parameters${separator}structure.txt")

    @FXML
    fun initialize() {
        println("Structure description controller set")
        /* set initial value */
        structureDescriptionTextArea.text = Files.lines(path).toList()
//                .filter { it.isNotBlank() }
                .reduce { text, line -> text + "\n" + line }
    }

    fun writeStructureDescription() = structureDescriptionTextArea.text.writeTo(path.toFile())
}
