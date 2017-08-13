package ui.controllers

import core.State
import core.util.ValidateResult
import core.util.ValidateResult.*
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import java.lang.System.*
import java.util.*


class ControlsController {

    lateinit var mainController: MainController
    @FXML private lateinit var computationTimeLabel: Label
    @FXML private lateinit var computeButton: Button

    @FXML
    fun initialize() {
        computeButton.run {
            Platform.runLater {
                scene.accelerators
                        .put(KeyCodeCombination(KeyCode.SPACE, KeyCombination.SHORTCUT_DOWN), Runnable(this::fire))
            }
            setOnAction {
                with(State) {
                    if (set() == SUCCESS) {
                        val startTime = nanoTime()
                        compute()
                        val stopTime = nanoTime()
                        computationTimeLabel.text = "Computation time: ${String.format(Locale.US, "%.2f", (stopTime - startTime).toDouble() / 1E6)} ms"
                        /**
                         Write to file last successful computation parameters
                         */
                        mainController.writeParametersChangingsToFiles()
                    }
                }
                mainController.lineChartController.updateLineChart()
            }
        }
    }
}
