package ui.controllers


import MainApp
import core.State
import core.State.polarization
import core.util.Polarization
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode.E
import javafx.scene.input.KeyCode.I
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination.SHIFT_DOWN
import javafx.scene.input.KeyCombination.SHORTCUT_DOWN
import javafx.scene.layout.AnchorPane
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File
import java.io.File.separator
import java.io.IOException


class MenuController {

    lateinit var mainController: MainController
    lateinit var rootController: RootController

    @FXML private lateinit var importMenuItem: MenuItem
    @FXML private lateinit var importMultipleMenuItem: MenuItem
    @FXML private lateinit var exportMenuItem: MenuItem
    @FXML private lateinit var exportMultipleMenuItem: MenuItem

    @FXML
    fun initialize() {
        importMenuItem.accelerator = KeyCodeCombination(I, SHORTCUT_DOWN)
        importMultipleMenuItem.accelerator = KeyCodeCombination(I, SHORTCUT_DOWN, SHIFT_DOWN)
        exportMenuItem.accelerator = KeyCodeCombination(E, SHORTCUT_DOWN)
        exportMultipleMenuItem.accelerator = KeyCodeCombination(E, SHORTCUT_DOWN, SHIFT_DOWN)

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
                it.initialFileName = LineChartState.buildExportFileName()
                it.showSaveDialog(rootController.mainApp.primaryStage)
            }
            if (file != null) {
                LineChartState.writeTo(file)
            }
        }

        exportMultipleMenuItem.setOnAction {
            try {
                val loader = FXMLLoader()
                loader.location = MainApp::class.java.getResource("fxml/ExportMultipleDialog.fxml")
                val page = loader.load<AnchorPane>()

                // Создаём диалоговое окно Stage.
                val exportMultipleDialogStage = Stage()
                exportMultipleDialogStage.title = "Export Multiple"
                exportMultipleDialogStage.isResizable = false

                //            dialogStage.initModality(Modality.WINDOW_MODAL);
                //            dialogStage.initOwner(mainLayoutController.);


                val scene = Scene(page)
                exportMultipleDialogStage.scene = scene

                // Отображаем диалоговое окно и ждём, пока пользователь его не закроет
                exportMultipleDialogStage.showAndWait()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    //    @FXML
//    internal fun handleExport(event: ActionEvent) {
////
////        val fileChooser = FileChooser()
////
////        // фильтр расширений
////        fileChooser
////                .extensionFilters
////                .add(FileChooser.ExtensionFilter("Data Files", "*.txt", "*.dat"))
////
////        fileChooser.initialDirectory = File("./data/calculated")
////
////
////        fileChooser.initialFileName = buildExportFileName()
////
////        // Показываем диалог сохранения файла
////        val file = fileChooser.showSaveDialog(rootLayoutController.getMainApp().getPrimaryStage())
////
////        saveCalcResult(
////                file,
////                rootLayoutController
////                        .getMainLayoutController()
////                        .getStructureDescriptionController()
////                        .getStructureDescriptionAsText(),
////                MainState.regime
////        )
//    }
//
//    // todo Export Multiple
//    @FXML
//    internal fun handleExportMultiple(event: ActionEvent) {
////        try {
////            val loader = FXMLLoader()
////            loader.location = MainApp::class.java!!.getResource("view/ExportMultipleDialog.fxml")
////            val page = loader.load<AnchorPane>()
////
////            // Создаём диалоговое окно Stage.
////            val exportMultipleDialogStage = Stage()
////            exportMultipleDialogStage.title = "Export Multiple"
////            exportMultipleDialogStage.isResizable = false
////
////            //            dialogStage.initModality(Modality.WINDOW_MODAL);
////            //            dialogStage.initOwner(mainLayoutController.);
////
////
////            val scene = Scene(page)
////            exportMultipleDialogStage.scene = scene
////
////            // Отображаем диалоговое окно и ждём, пока пользователь его не закроет
////            exportMultipleDialogStage.showAndWait()
////        } catch (e: IOException) {
////            e.printStackTrace()
////        }
////
//    }
//
    private fun initFileChooser(dir: String) = FileChooser().apply {
        extensionFilters.add(FileChooser.ExtensionFilter("Data Files", "*.txt", "*.dat"))
        initialDirectory = File(dir)
    }

//    @FXML
//    internal fun handleHelpInfo(event: ActionEvent) {
////
////        try {
////            val loader = FXMLLoader()
////            loader.location = MainApp::class.java!!.getResource("view/HelpInfo.fxml")
////            val page = loader.load<AnchorPane>()
////
////            // Создаём диалоговое окно Stage.
////            val infoStage = Stage()
////            infoStage.title = "Info"
////            infoStage.isAlwaysOnTop = true
////            //            dialogStage.initModality(Modality.WINDOW_MODAL);
////            //            dialogStage.initOwner(mainLayoutController.);
////            val scene = Scene(page)
////            infoStage.scene = scene
////
////            // Отображаем диалоговое окно и ждём, пока пользователь его не закроет
////            infoStage.showAndWait()
////        } catch (e: IOException) {
////            e.printStackTrace()
////        }
////
//    }
}

class ExportMultipleDialogController {

    lateinit var mainController: MainController

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
    @FXML private lateinit var curDirLabel: Label

    private var multipleExportDirectory: File? = null

    @FXML
    fun initialize() {
        State.mainController.exportMultipleDialogController.mainController = State.mainController


        with(polarizationChoiceBox) {
            /* initial value */
            value = polarization.toString()
            selectionModel.selectedItemProperty().addListener { _, _, _ ->
                polarization = Polarization.valueOf(value)
                State.mainController.globalParametersController
                        .lightParametersController.polarizationChoiceBox.value = value
            }
        }

        toggleGroup.selectedToggleProperty().addListener { observable, oldValue, newValue ->
            if (toggleGroup.selectedToggle != null) {
                val selectedRadioButton = toggleGroup.selectedToggle as RadioButton
                if (selectedRadioButton === angleRadioButton) {
                    enableAngles()
                } else if (selectedRadioButton === temperatureRadioButton) {
                    enableTemperatures()
                }
            }
        }

        directoryButton.setOnMouseClicked {
            with(DirectoryChooser()) {
                initialDirectory = File(".${separator}data${separator}computed_multiple")
                /*
                Need to pass Window or Stage. There's no access to any Stage object from this controller
                Solution: any Node from fxml that has fx:id
                http://stackoverflow.com/questions/25491732/how-do-i-open-the-javafx-filechooser-from-a-controller-class
                 */
                multipleExportDirectory = showDialog(directoryButton.scene.window)
//                if (multipleExportDirectory == null) {
//                    with(Alert(Alert.AlertType.ERROR)) {
//                        title = "Error"
//                        headerText = "Directory error"
//                        contentText = "Choose a directory"
//                        showAndWait()
//                    }
//                }
            }
        }

        exportButton.setOnMouseClicked {

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

