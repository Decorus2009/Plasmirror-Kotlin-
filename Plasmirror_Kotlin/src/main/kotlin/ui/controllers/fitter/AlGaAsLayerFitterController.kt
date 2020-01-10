package ui.controllers.fitter

import core.layers.semiconductor.AlGaAs
import core.layers.semiconductor.Layer
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import java.util.*


/**
 * Created by decorus on 18.08.17.
 */

class AlGaAsLayerFitterController : LayerFitterController() {

  @FXML
  lateinit var enableCheckBox: CheckBox
  @FXML
  private lateinit var gridPane: GridPane
  @FXML
  private lateinit var d_valueTextField: TextField
  @FXML
  private lateinit var d_fromTextField: TextField
  @FXML
  private lateinit var d_toTextField: TextField
  @FXML
  private lateinit var d_stepTextField: TextField
  @FXML
  private lateinit var k_valueTextField: TextField
  @FXML
  private lateinit var k_fromTextField: TextField
  @FXML
  private lateinit var k_toTextField: TextField
  @FXML
  private lateinit var k_stepTextField: TextField
  @FXML
  private lateinit var x_valueTextField: TextField
  @FXML
  private lateinit var x_fromTextField: TextField
  @FXML
  private lateinit var x_toTextField: TextField
  @FXML
  private lateinit var x_stepTextField: TextField

  override lateinit var layer: Layer
  override lateinit var mainFitterController: MainFitterController

  @FXML
  fun initialize() {
    enableCheckBox.setOnAction {
      gridPane.isDisable = enableCheckBox.isSelected.not()
      mainFitterController.layersToFitterControllers.values.filterNot { it === this }.forEach {
        it.disable()
      }
    }
    Platform.runLater {
      with(layer as AlGaAs) {
        d.let {
          d_valueTextField.text = String.format(Locale.US, "%.2f", it)
          d_fromTextField.text = String.format(Locale.US, "%.2f", it * 0.5)
          d_toTextField.text = String.format(Locale.US, "%.2f", it * 1.5)
          d_stepTextField.text = String.format(Locale.US, "%.2f", it / 100.0)
        }
        k.let {
          k_valueTextField.text = String.format(Locale.US, "%.2f", it)
          k_fromTextField.text = String.format(Locale.US, "%.2f", it * 0.5)
          k_toTextField.text = String.format(Locale.US, "%.2f", it * 1.5)
          k_stepTextField.text = String.format(Locale.US, "%.2f", it / 100.0)
        }
        x.let {
          x_valueTextField.text = String.format(Locale.US, "%.2f", it)
          x_fromTextField.text = String.format(Locale.US, "%.2f", it * 0.5)
          x_toTextField.text = String.format(Locale.US, "%.2f", it * 1.5)
          x_stepTextField.text = String.format(Locale.US, "%.2f", it / 100.0)
        }
      }
    }
  }

  override fun fit() {
    // fitting procedure is slow and ineffective
    TODO()
/*
        println("Fitting AlGaAs layer")

        val d_from = d_fromTextField.text.toDouble()
        val d_to = d_toTextField.text.toDouble()
        val d_step = d_stepTextField.text.toDouble()

        val k_from = k_fromTextField.text.toDouble()
        val k_to = k_toTextField.text.toDouble()
        val k_step = k_stepTextField.text.toDouble()

        val x_from = x_fromTextField.text.toDouble()
        val x_to = x_toTextField.text.toDouble()
        val x_step = x_stepTextField.text.toDouble()

        var d = d_from

        with (layer as AlGaAs) {
            while (d <= d_to) {
                this.d = d

                var k = k_from
                while (k <= k_to) {
                    this.k = k

                    var x = x_from
                    while (x <= x_to) {
                        this.x = x

                        println("d = $d k = $k x = $x")
                        with(FitterState) {
                            clearPrevious()
                            compute()
                            checkDifference()
                        }

                        x += x_step
                    }
                    k += k_step
                }
                d += d_step
            }
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