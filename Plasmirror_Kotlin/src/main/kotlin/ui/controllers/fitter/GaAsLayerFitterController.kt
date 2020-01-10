package ui.controllers.fitter

import core.layers.semiconductor.Layer
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import java.util.*


class GaAsLayerFitterController : LayerFitterController() {

  @FXML
  private lateinit var enableCheckBox: CheckBox
  @FXML
  private lateinit var d_valueTextField: TextField
  @FXML
  private lateinit var d_fromTextField: TextField
  @FXML
  private lateinit var d_toTextField: TextField
  @FXML
  private lateinit var d_stepTextField: TextField
  @FXML
  private lateinit var gridPane: GridPane

  override lateinit var layer: Layer
  override lateinit var mainFitterController: MainFitterController

  @FXML
  fun initialize() {
    gridPane.isDisable = true
    enableCheckBox.setOnAction {
      gridPane.isDisable = enableCheckBox.isSelected.not()
      mainFitterController.layersToFitterControllers.values.filterNot { it === this }.forEach {
        it.disable()
      }
    }
    Platform.runLater {
      layer.d.let {
        d_valueTextField.text = String.format(Locale.US, "%.2f", it)
        d_fromTextField.text = String.format(Locale.US, "%.2f", it * 0.5)
        d_toTextField.text = String.format(Locale.US, "%.2f", it * 1.5)
        d_stepTextField.text = String.format(Locale.US, "%.2f", it / 100.0)
      }
    }
  }

  override fun fit() {
    // fitting procedure is slow and ineffective
    TODO()
/*
        println("Fitting GaAs layer")

        val from = d_fromTextField.text.toDouble()
        val to = d_toTextField.text.toDouble()
        val step = d_stepTextField.text.toDouble()

        var d = from
        while (d <= to) {
            layer.d = d

            with(FitterState) {
                clearPrevious()
                compute()
                checkDifference()
            }
            d += step
        }

        println("Fitter values: ${FitterState.listOfParameters}")
*/
  }

  override fun enable() {
    enableCheckBox.isSelected = true
    gridPane.isDisable = false
  }

  override fun disable() {
    enableCheckBox.isSelected = false
    gridPane.isDisable = true
  }

  override val selected: Boolean
    get() = enableCheckBox.isSelected
}
