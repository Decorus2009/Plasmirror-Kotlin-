package ui.controllers

import core.State
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
                State.run {
                    set()
                    val startTime = nanoTime()
                    compute()
                    val stopTime = nanoTime()
                    computationTimeLabel.text = "Computation time: ${String.format(Locale.US, "%.2f", (stopTime - startTime).toDouble() / 1E6)}, ms"
                }
                mainController.lineChartController.update()
            }
        }
    }

    /**
     * При нажатии происходит обновление параметров структуры и глобальных параметров.
     * В текстовых полях (например, угол) надо после их модификации нажимать Enter.
     * Это происходит не всегда, поэтому апдейт вызывается еще раз непосредственно с щелчком мыши.
     * После апдейтов (когда проставляются необходимые глобальные параметры и описание структуры)
     * вызывается метод recreateMirror(), в котором пересоздается объект Mirror
     * (в котором как раз глобальные параметры используются).
     *
     *
     * При каждом клике мыши (а не при закрытии (что более логично)) все изменения,
     * вводимые в окне программы, записываются в файл
     * на быстродействие это не влияет
     */
    //    @FXML
//    fun computeButtonClicked() {
//
//        try {
//            mainController.structureDescriptionController.updateStructureDescription()
//        } catch (e: StructureDescriptionException) {
//            showStructureDescriptionsAlert(e.getMessage())
//        }
//
//        mainController.lineChartController.updateIfAnotherRegime()
//
//        initAndCalculate()
//    }
//
//
//    private fun initAndCalculate() {
//
//        val calculationTime: Double
//        var startTime: Long
//        var stopTime: Long
//
//        State.buildMirror()
//
//        startTime = System.nanoTime()
//        State.calculate()
//        stopTime = System.nanoTime()
//        calculationTime = (stopTime - startTime).toDouble()
//
//        startTime = System.nanoTime()
//        mainController.getLineChartController().updateCalculationSeries()
//        stopTime = System.nanoTime()
//
//        mainController
//                .getControlsController()
//                .setCalcTimeLabelText(
//                        "Calculation time: "
//                                + String.format(Locale.US, "%.2f", calculationTime / 1E6)
//                                + " (ms). Line chart initCalculated time: "
//                                + String.format(Locale.US, "%.2f", (stopTime - startTime).toDouble() / 1E6)
//                                + " (ms)"
//                )
//    }
//
//    private fun initCalcButtonAccelerator() =
//            computeButton.scene.accelerators
//                    .put(KeyCodeCombination(KeyCode.SPACE, KeyCombination.SHORTCUT_DOWN), Runnable { computeButton.fire() })
//
//
//    private fun showStructureDescriptionsAlert(message: String) {
//
//        val alert = Alert(Alert.AlertType.ERROR)
//        alert.title = "Error"
//        alert.headerText = "Structure description error"
//        alert.contentText = message
//
//        alert.showAndWait()
//    }
//
//    fun setMainController(mainController: MainLayoutController) {
//        this.mainController = mainController
//    }
//
//    fun setCalcTimeLabelText(text: String) {
//        computationTimeLabel.text = text
//    }
}
