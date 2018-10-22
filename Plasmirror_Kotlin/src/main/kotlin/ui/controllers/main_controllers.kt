package ui.controllers

import MainApp
import core.State
import core.ValidateResult
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import java.util.*

class RootController {

    @FXML lateinit var menuController: MenuController
    @FXML lateinit var mainController: MainController

    lateinit var mainApp: MainApp

    @FXML
    fun initialize() {
        println("Root controller init")
        /**
        mainController is "lateinit" due to it's initialized through the reflection (@FXML)
        BEFORE the root controller initialization.
         */
        menuController.rootController = this
        mainController.rootController = this
    }
}


class MainController {

    lateinit var rootController: RootController
    @FXML lateinit var structureDescriptionController: StructureDescriptionController
    @FXML lateinit var globalParametersController: GlobalParametersController
    @FXML lateinit var lineChartController: LineChartController
    @FXML lateinit var controlsController: ControlsController
    @FXML private lateinit var xAxisRangeController: XAxisRangeController
    @FXML private lateinit var yAxisRangeController: YAxisRangeController
    @FXML lateinit var seriesManagerController: SeriesManagerController
    @FXML lateinit var multipleExportDialogController: MultipleExportDialogController

    @FXML
    fun initialize() {
        println("Main controller init")
        globalParametersController.mainController = this
        controlsController.mainController = this
        lineChartController.mainController = this
        xAxisRangeController.mainController = this
        yAxisRangeController.mainController = this
        seriesManagerController.mainController = this

        with(State) {
            mainController = this@MainController
            init()
            compute()
        }
        lineChartController.updateLineChart()
    }

    fun writeParametersChangingsToFiles() {
        globalParametersController.writeGlobalParameters()
        structureDescriptionController.writeStructureDescription()
    }
}


class ControlsController {

    lateinit var mainController: MainController
    @FXML private lateinit var computationTimeLabel: Label
    @FXML private lateinit var computeButton: Button

    @FXML
    fun initialize() {
        with(computeButton) {
            Platform.runLater {
                scene.accelerators
                        .put(KeyCodeCombination(KeyCode.SPACE, KeyCombination.SHORTCUT_DOWN), Runnable(this::fire))
            }
            setOnAction {
                with(State) {
                    if (init() == ValidateResult.SUCCESS) {
                        val startTime = System.nanoTime()
                        compute()
                        val stopTime = System.nanoTime()
                        computationTimeLabel.text =
                                "Computation time: ${kotlin.String.format(Locale.US, "%.2f", (stopTime - startTime).toDouble() / 1E6)} ms"
                        /**
                        Write to file last successful computation parameters
                         */
                        mainController.writeParametersChangingsToFiles()
                        mainController.lineChartController.updateLineChart()
                    }
                }
            }
        }
    }
}

