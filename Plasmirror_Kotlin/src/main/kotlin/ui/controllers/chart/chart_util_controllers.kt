package ui.controllers.chart

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.paint.Color
import javafx.util.converter.NumberStringConverter
import ui.controllers.MainController
import ui.controllers.enable
import java.util.*

class SeriesManagerController {

    lateinit var mainController: MainController

    @FXML
    private lateinit var colorLabel: Label
    @FXML
    private lateinit var colorPicker: ColorPicker
    @FXML
    private lateinit var xAxisFactorLabel: Label
    @FXML
    private lateinit var xAxisFactorTextField: TextField
    @FXML
    private lateinit var yAxisFactorLabel: Label
    @FXML
    private lateinit var yAxisFactorTextField: TextField
    @FXML
    private lateinit var visibleCheckBox: CheckBox
    @FXML
    private lateinit var removeButton: Button

    private lateinit var selectedSeries: LineChartState.ExtendedSeries

    @FXML
    fun initialize() {
        /* http://stackoverflow.com/a/17925600 */
        colorPicker.setOnAction {
            val hexColor = with(colorPicker.value) {
                "#" + Integer.toHexString((red * 255).toInt()) + Integer.toHexString((green * 255).toInt()) + Integer.toHexString((blue * 255).toInt())
            }
            selectedSeries.color = hexColor
            mainController.lineChartController.updateStyleOf(selectedSeries)
        }

        xAxisFactorTextField.textProperty().addListener { _, _, newValue ->
            try {
                val newFactor = newValue.toDouble()
                /* 0.0 as the value of the previous newFactor will be remembered an will break the scaling */
                if (newFactor != 0.0) {
                    with(selectedSeries) {
                        series.data.forEach { it.xValue = it.xValue.toDouble() / previousXAxisFactor * newFactor }
                        previousXAxisFactor = newFactor
                    }
                }
            } catch (ignored: NumberFormatException) {
            }
        }

        yAxisFactorTextField.textProperty().addListener { _, _, newValue ->
            try {
                val newFactor = newValue.toDouble()
                /* 0.0 as the value of the previous newFactor will be remembered an will break the scaling */
                if (newFactor != 0.0) {
                    with(selectedSeries) {
                        series.data.forEach { it.yValue = it.yValue.toDouble() / previousYAxisFactor * newFactor }
                        previousYAxisFactor = newFactor
                    }
                }
            } catch (ignored: NumberFormatException) {
            }
        }

        with(visibleCheckBox) {
            isSelected = true
            setOnAction {
                selectedSeries.visible = selectedSeries.visible.not()
                mainController.lineChartController.setVisibilityBy(selectedSeries)
            }
        }

        removeButton.setOnMouseClicked {
            LineChartState.removeByName(selectedSeries.series.name)
            with(mainController) {
                with(lineChartController) {
                    removeByName(selectedSeries.series.name)
                    updateStyleOfAll()
                    updateLegendListener()
                }
                seriesManagerController.disable()
            }
        }

        disable()
    }

    fun enableUsing(selectedSeries: LineChartState.ExtendedSeries) {
        this.selectedSeries = selectedSeries

        enable(colorLabel, xAxisFactorLabel, yAxisFactorLabel)
        enable(xAxisFactorTextField, yAxisFactorTextField)
        enable(colorPicker)
        enable(visibleCheckBox)
        enable(removeButton)

        with(selectedSeries) {
            if (type == LineChartState.SERIES_TYPE.COMPUTED) {
                ui.controllers.disable(xAxisFactorLabel, yAxisFactorLabel)
                ui.controllers.disable(xAxisFactorTextField, yAxisFactorTextField)
                ui.controllers.disable(removeButton)
            }
            xAxisFactorTextField.text = previousXAxisFactor.toString()
            yAxisFactorTextField.text = previousYAxisFactor.toString()
            colorPicker.value = Color.valueOf(color)
            visibleCheckBox.isSelected = visible
        }
    }

    fun disable() {
        ui.controllers.disable(colorLabel, xAxisFactorLabel, yAxisFactorLabel)
        ui.controllers.disable(xAxisFactorTextField, yAxisFactorTextField)
        ui.controllers.disable(colorPicker)
        ui.controllers.disable(visibleCheckBox)
        ui.controllers.disable(removeButton)
    }
}


class XAxisRangeController {

    lateinit var mainController: MainController

    @FXML
    private lateinit var fromTextField: TextField
    @FXML
    private lateinit var toTextField: TextField
    @FXML
    private lateinit var tickTextField: TextField

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
    @FXML
    fun initialize() = Platform.runLater {
        with(mainController.lineChartController.xAxis) {
            val converter = NumberStringConverter(Locale.ROOT)
            with(fromTextField) {
                text = lowerBound.toString()
                textProperty().bindBidirectional(lowerBoundProperty(), converter)
                textProperty().addListener { _, _, newValue ->
                    try {
                        lowerBound = newValue.toDouble()
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }
            with(toTextField) {
                text = upperBound.toString()
                textProperty().bindBidirectional(upperBoundProperty(), converter)
                textProperty().addListener { _, _, newValue ->
                    try {
                        upperBound = newValue.toDouble()
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }
            with(tickTextField) {
                text = tickUnit.toString()
                textProperty().bindBidirectional(tickUnitProperty(), converter)
                textProperty().addListener { _, _, newValue ->
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

    @FXML
    private lateinit var fromTextField: TextField
    @FXML
    private lateinit var toTextField: TextField
    @FXML
    private lateinit var tickTextField: TextField

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
    @FXML
    fun initialize() = Platform.runLater {
        with(mainController.lineChartController.yAxis) {
            val converter = NumberStringConverter(Locale.ROOT)
            with(fromTextField) {
                text = lowerBound.toString()
                textProperty().bindBidirectional(lowerBoundProperty(), converter)
                textProperty().addListener { _, _, newValue ->
                    try {
                        lowerBound = newValue.toDouble()
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }
            with(toTextField) {
                text = upperBound.toString()
                textProperty().bindBidirectional(upperBoundProperty(), converter)
                textProperty().addListener { _, _, newValue ->
                    try {
                        upperBound = newValue.toDouble()
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }
            with(tickTextField) {
                text = tickUnit.toString()
                textProperty().bindBidirectional(tickUnitProperty(), converter)
                textProperty().addListener { _, _, newValue ->
                    try {
                        tickUnit = newValue.toDouble()
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }
        }
    }
}