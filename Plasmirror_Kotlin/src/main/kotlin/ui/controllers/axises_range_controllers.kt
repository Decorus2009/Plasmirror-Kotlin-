package ui.controllers

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.util.converter.NumberStringConverter
import java.util.*

/**
 * Platform.runLater due to the mainController is not initialized during this.initialize()
 *
 * Using of Locale.ROOT is explained here http://stackoverflow.com/a/5236096/7149251
 *
 * Here I use StringConverter<Number> parametrized with Number
 * due to different properties (incompatible) StringProperty and DoubleProperty
 * http://stackoverflow.com/questions/21450328/how-to-bind-two-different-javafx-properties-string-and-double-with-stringconve
 *
 * toDouble parsing is here in listeners, not in validators due to the real-time response
 */
class XAxisRangeController {

    lateinit var mainController: MainController

    @FXML private lateinit var fromTextField: TextField
    @FXML private lateinit var toTextField: TextField
    @FXML private lateinit var tickTextField: TextField

    @FXML
    fun initialize() = Platform.runLater {
        with(mainController.lineChartController.xAxis) {
            val converter = NumberStringConverter(Locale.ROOT)
            fromTextField.let {
                it.text = lowerBound.toString()
                it.textProperty().bindBidirectional(lowerBoundProperty(), converter)
                it.textProperty().addListener { _, _, newValue ->
                    try {
                        lowerBound = newValue.toDouble()
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }
            toTextField.let {
                it.text = upperBound.toString()
                it.textProperty().bindBidirectional(upperBoundProperty(), converter)
                it.textProperty().addListener { _, _, newValue ->
                    try {
                        upperBound = newValue.toDouble()
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }
            tickTextField.let {
                it.text = tickUnit.toString()
                it.textProperty().bindBidirectional(tickUnitProperty(), converter)
                it.textProperty().addListener { _, _, newValue ->
                    try {
                        tickUnit = newValue.toDouble()
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }
        }
    }
}

class YAxisRangeController {

    lateinit var mainController: MainController

    @FXML private lateinit var fromTextField: TextField
    @FXML private lateinit var toTextField: TextField
    @FXML private lateinit var tickTextField: TextField

    @FXML
    fun initialize() = Platform.runLater {
        with(mainController.lineChartController.yAxis) {
            val converter = NumberStringConverter(Locale.ROOT)
            fromTextField.let {
                it.text = lowerBound.toString()
                it.textProperty().bindBidirectional(lowerBoundProperty(), converter)
                it.textProperty().addListener { _, _, newValue ->
                    try {
                        lowerBound = newValue.toDouble()
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }
            toTextField.let {
                it.text = upperBound.toString()
                it.textProperty().bindBidirectional(upperBoundProperty(), converter)
                it.textProperty().addListener { _, _, newValue ->
                    try {
                        upperBound = newValue.toDouble()
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }
            tickTextField.let {
                it.text = tickUnit.toString()
                it.textProperty().bindBidirectional(tickUnitProperty(), converter)
                it.textProperty().addListener { _, _, newValue ->
                    try {
                        tickUnit = newValue.toDouble()
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }
        }
    }
}
