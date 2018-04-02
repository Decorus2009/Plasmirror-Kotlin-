package ui.controllers


import MainApp
import core.State
import core.State.polarization
import core.MultipleExportDialogParametersValidator
import core.Polarization
import core.ValidateResult.SUCCESS
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.E
import javafx.scene.input.KeyCode.F
import javafx.scene.input.KeyCode.I
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination.SHIFT_DOWN
import javafx.scene.input.KeyCombination.SHORTCUT_DOWN
import javafx.scene.input.KeyEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File
import java.io.File.separator
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList


class MenuController {

    lateinit var rootController: RootController

    @FXML private lateinit var importMenuItem: MenuItem
    @FXML private lateinit var importMultipleMenuItem: MenuItem
    @FXML private lateinit var exportMenuItem: MenuItem
    @FXML private lateinit var exportMultipleMenuItem: MenuItem
    @FXML private lateinit var helpMenuItem: MenuItem
    @FXML private lateinit var fitterMenuItem: MenuItem

    @FXML
    fun initialize() {
        importMenuItem.accelerator = KeyCodeCombination(I, SHORTCUT_DOWN)
        importMultipleMenuItem.accelerator = KeyCodeCombination(I, SHORTCUT_DOWN, SHIFT_DOWN)
        exportMenuItem.accelerator = KeyCodeCombination(E, SHORTCUT_DOWN)
        exportMultipleMenuItem.accelerator = KeyCodeCombination(E, SHORTCUT_DOWN, SHIFT_DOWN)
        fitterMenuItem.accelerator = KeyCodeCombination(F, SHORTCUT_DOWN)

        importMenuItem.setOnAction {
            val file = initFileChooser(".${separator}data${separator}for_import")
                    .showOpenDialog(rootController.mainApp.primaryStage)
            if (file != null) {
                rootController.mainController.lineChartController.importFrom(file)
            }
        }

        importMultipleMenuItem.setOnAction {
            val files = initFileChooser(".${separator}data${separator}for_import")
                    .showOpenMultipleDialog(rootController.mainApp.primaryStage)
            if (files != null) {
                rootController.mainController.lineChartController.importMultiple(files)
            }
        }

        exportMenuItem.setOnAction {
            val file = initFileChooser(".${separator}data${separator}computed_single").let {
                it.initialFileName = buildExportFileName()
                it.showSaveDialog(rootController.mainApp.primaryStage)
            }
            if (file != null) {
                writeComputedDataTo(file)
            }
        }

        exportMultipleMenuItem.setOnAction {
            val page = with(FXMLLoader()) {
                location = MainApp::class.java.getResource("fxml/MultipleExportDialog.fxml")
                load<AnchorPane>()
            }
            with(Stage()) {
                title = "Export Multiple"
                isResizable = false
                scene = Scene(page)
                /* works after pressing directory button or switching between angle and temperature regimes. Why? */
                addEventHandler(KeyEvent.KEY_RELEASED) { event: KeyEvent ->
                    if (KeyCode.ESCAPE == event.code) {
                        close()
                    }
                }
                show()
            }
        }

        fitterMenuItem.setOnAction {
            val page = with(FXMLLoader()) {
                location = MainApp::class.java.getResource("fxml/fitter/Fitter.fxml")
                load<ScrollPane>()
            }
            with(Stage()) {
                title = "MainFitterController"
//                isAlwaysOnTop = true
                scene = Scene(page)
                addEventHandler(KeyEvent.KEY_RELEASED) { event: KeyEvent ->
                    if (KeyCode.ESCAPE == event.code) {
                        close()
                    }
                }
                showAndWait()
            }
        }

        helpMenuItem.setOnAction {
            val page = with(FXMLLoader()) {
                location = MainApp::class.java.getResource("fxml/HelpInfo.fxml")
                load<AnchorPane>()
            }
            with(Stage()) {
                title = "Help"
                scene = Scene(page)
                /* works after pressing directory button or switching between angle and temperature regimes. Why? */
                addEventHandler(KeyEvent.KEY_RELEASED) { event: KeyEvent ->
                    if (KeyCode.ESCAPE == event.code) {
                        close()
                    }
                }
                showAndWait()
            }
        }
    }

    private fun initFileChooser(dir: String) = FileChooser().apply {
        extensionFilters.add(FileChooser.ExtensionFilter("Data Files", "*.txt", "*.dat"))
        initialDirectory = File(dir)
    }
}

class MultipleExportDialogController {

    @FXML private lateinit var polarizationChoiceBox: ChoiceBox<String>
    @FXML private lateinit var anglesLabel: Label
    @FXML private lateinit var temperaturesLabel: Label

    @FXML private lateinit var toggleGroup: ToggleGroup

    @FXML private lateinit var angleRadioButton: RadioButton
    @FXML private lateinit var angleFromLabel: Label
    @FXML private lateinit var angleToLabel: Label
    @FXML private lateinit var angleStepLabel: Label
    @FXML lateinit var angleFromTextField: TextField
    @FXML lateinit var angleToTextField: TextField
    @FXML lateinit var angleStepTextField: TextField

    @FXML private lateinit var temperatureRadioButton: RadioButton
    @FXML private lateinit var temperatureFromLabel: Label
    @FXML private lateinit var temperatureToLabel: Label
    @FXML private lateinit var temperatureStepLabel: Label
    @FXML lateinit var temperatureFromTextField: TextField
    @FXML lateinit var temperatureToTextField: TextField
    @FXML lateinit var temperatureStepTextField: TextField

    @FXML private lateinit var directoryButton: Button
    @FXML private lateinit var exportButton: Button
    @FXML private lateinit var statusLabel: Label
    @FXML private lateinit var chosenDirectoryLabel: Label

    var angleFrom: Double = 0.0
    var angleTo: Double = 0.0
    var angleStep: Double = 0.0
    var temperatureFrom: Double = 0.0
    var temperatureTo: Double = 0.0
    var temperatureStep: Double = 0.0

    var chosenDirectory: File? = null

    fun anglesSelected(): Boolean = toggleGroup.selectedToggle as? RadioButton === angleRadioButton

    @FXML
    fun initialize() {
        /* the only way to call another controllers of GUI is using already initialized State.mainController */
        State.mainController.multipleExportDialogController = this

        with(polarizationChoiceBox) {
            /* initial value */
            value = polarization.toString()
            selectionModel.selectedItemProperty().addListener { _, _, _ ->
                polarization = Polarization.valueOf(value)
                State.mainController.globalParametersController
                        .lightParametersController.polarizationChoiceBox.value = value
            }
        }

        toggleGroup.selectedToggleProperty().addListener { _, _, _ ->
            if (anglesSelected()) {
                enableAngles()
            } else {
                enableTemperatures()
            }
        }

        directoryButton.setOnMouseClicked {
            with(DirectoryChooser()) {
                initialDirectory = File(".${separator}data${separator}computed_multiple")
                /**
                Need to pass Window or Stage. There's no access to any Stage object from this controller
                Solution: any Node from fxml that has fx:id
                http://stackoverflow.com/questions/25491732/how-do-i-open-the-javafx-filechooser-from-a-controller-class
                 */
                chosenDirectory = showDialog(directoryButton.scene.window)
            }
            chosenDirectory?.let { chosenDirectoryLabel.text = it.canonicalPath }
        }

        exportButton.setOnMouseClicked {
            with(MultipleExportDialogParametersValidator) {
                if (validateRegime() == SUCCESS && validateChosenDirectory() == SUCCESS) {
                    if (anglesSelected()) {
                        if (validateAngles() == SUCCESS) {
                            /**
                            Computation process runs through the setting fields in GUI (angle, polarization, etc.).
                            After that the validation takes place parsing these GUI fields and setting actual inner
                            parameters in program (State.angle, State.polarization, etc.).
                            To be able to compute and export data at multiple angles,
                            the corresponding GUI text field is set each time and validated and the computation process is performed.
                            In the end each field must get its initial value.
                            */
                            val initialAngle: String = State.mainController.globalParametersController
                                    .lightParametersController.angleTextField.text
                            val initialPolarization = State.mainController.globalParametersController
                                    .lightParametersController.polarizationChoiceBox.value

                            var currentAngle = angleFrom
                            while (currentAngle < 90.0 && currentAngle <= angleTo) {
                                with(State) {
                                    /* angleTextField.text will be validated before computation */
                                    mainController.globalParametersController.lightParametersController
                                            .angleTextField.text = currentAngle.toString()
                                    set()
                                    compute()
                                }
                                writeComputedDataTo(File("${chosenDirectory!!.canonicalPath}$separator${buildExportFileName()}.txt"))
                                currentAngle += angleStep
                            }
                            with(State.mainController.globalParametersController.lightParametersController) {
                                angleTextField.text = initialAngle
                                polarizationChoiceBox.value = initialPolarization
                            }
                        }
                    } else {
                        if (validateTemperatures() == SUCCESS) {
                            /* TODO multiple export for temperature range */
                        }
                    }
                    statusLabel.text = "Exported"
                }
            }
        }
    }

    private fun enableAngles() {
        enable(anglesLabel, angleFromLabel, angleToLabel, angleStepLabel)
        enable(angleFromTextField, angleToTextField, angleStepTextField)
        disable(temperaturesLabel, temperatureFromLabel, temperatureToLabel, temperatureStepLabel)
        disable(temperatureFromTextField, temperatureToTextField, temperatureStepTextField)
    }

    private fun enableTemperatures() {
        enable(temperaturesLabel, temperatureFromLabel, temperatureToLabel, temperatureStepLabel)
        enable(temperatureFromTextField, temperatureToTextField, temperatureStepTextField)
        disable(anglesLabel, angleFromLabel, angleToLabel, angleStepLabel)
        disable(angleFromTextField, angleToTextField, angleStepTextField)
    }
}

class HelpInfoController {

    @FXML private lateinit var helpTextArea: TextArea

    private val path = Paths.get("./data/inner/help.txt")

    @FXML
    fun initialize() {
        helpTextArea.text = Files.lines(path).toList().reduce { text, line -> text + "\n" + line }
    }
}

