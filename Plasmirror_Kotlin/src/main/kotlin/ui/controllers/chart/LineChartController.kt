package ui.controllers.chart

import com.sun.javafx.charts.Legend
import core.optics.Regime.*
import core.State
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Label
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.shape.StrokeType
import javafx.util.StringConverter
import org.gillius.jfxutils.JFXUtil
import org.gillius.jfxutils.chart.ChartPanManager
import org.gillius.jfxutils.chart.ChartZoomManager
import ui.controllers.MainController
import ui.controllers.chart.LineChartController.ComputationType.*
import ui.controllers.chart.LineChartState.ExtendedSeries
import ui.controllers.chart.LineChartState.allExtendedSeries
import ui.controllers.chart.LineChartState.computed
import ui.controllers.chart.LineChartState.imported
import java.io.File
import java.util.*


class LineChartController {

    lateinit var mainController: MainController

    @FXML
    lateinit var lineChart: LineChart<Number, Number>
    @FXML
    lateinit var xAxis: NumberAxis
    @FXML
    lateinit var yAxis: NumberAxis
    @FXML
    private lateinit var XYPositionLabel: Label

    private enum class ComputationType { REAL, COMPLEX, NONE }

    private var previousComputationDataType = NONE

    @FXML
    fun initialize() {
        println("Line chart controller init")

        /* init number formatters for axises' values */
        xAxis.tickLabelFormatter = object : StringConverter<Number>() {
            override fun toString(`object`: Number) = String.format(Locale.ROOT, "%.1f", `object`.toDouble())
            override fun fromString(string: String) = 0
        }
        yAxis.tickLabelFormatter = object : StringConverter<Number>() {
            override fun toString(`object`: Number) = String.format(Locale.ROOT, "%.2f", `object`.toDouble())
            override fun fromString(string: String) = 0
        }

        lineChart.let {
            it.createSymbols = false
            it.animated = false
            it.isLegendVisible = true
            /* force a css layout pass to ensure that subsequent lookup calls work */
            it.applyCss()
        }
        xAxis.label = "Wavelength, nm"

        setCursorTracing()
        setPanning()
        setZooming()
        setDoubleMouseClickRescaling()
        updateLegendListener()
    }

    fun updateLineChart() {
        fun updateComputedSeries() {
            LineChartState.updateComputed()
            lineChart.run {
                if (previousComputationDataType != NONE) {
                    /* remove real series */
                    data.removeAt(0)
                    if (previousComputationDataType == COMPLEX) {
                        /* remove remained imaginary series */
                        data.removeAt(0)
                    }
                }
                with(computed) {
                    data.add(0, extendedSeriesReal.series)
                    previousComputationDataType = REAL
                    if (extendedSeriesImaginary.series.data.isNotEmpty()) {
                        data.add(1, extendedSeriesImaginary.series)
                        previousComputationDataType = COMPLEX
                    }
                }
            }
        }

        fun updateYAxisLabel() {
            yAxis.label = when (State.regime) {
                REFLECTANCE -> "Reflectance"
                TRANSMITTANCE -> "Transmittance"
                ABSORBANCE -> "Absorbance"
                PERMITTIVITY -> "OpticalConstants"
                REFRACTIVE_INDEX -> "Refractive Index"
            }
        }

        // TODO commented
        /* regime == null is used during the first automatic call of rescale() method after initialization */
        fun updateRegimeAndRescale() = with(mainController.globalParametersController.regimeController) {
            /* if another regime */
            if (regimeBefore == null || regimeBefore != State.regime) {
                regimeBefore = State.regime
                /* deselect all series, labels and disable activated series manager */
                allExtendedSeries().forEach { deselect() }
                rescale()
            }
        }

        updateComputedSeries()
        updateYAxisLabel()

// TODO commented updateRegimeAndRescale !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1
//        updateRegimeAndRescale()
        updateLegendListener()
        updateStyleOfAll()
    }

    fun updateStyleOf(extendedSeries: ExtendedSeries) = with(extendedSeries) {
        /* if series.data.isEmpty(), series.node == null. No need to init styles (NPE) */
        if (series.data.isEmpty()) {
            return@with
        }
        series.node.style = """
            -fx-stroke: $color;
            -fx-stroke-width: $width;
        """
        lineChart.labels().find { it.text == series.name }!!.style =
                if (selected) {
                    """
                    -fx-stroke: $color;
                    -fx-background-insets: 0 0 -1 0, 0, 1, 2;
                    -fx-padding: 7px;
                    -fx-background-radius: 1px, 0px, 0px, 0px;
                    -fx-background-color: #cccccc;
                """
                } else {
                    ""
                }
        /**
         * http://news.kynosarges.org/2017/05/14/javafx-chart-coloring/
         *
         * sometimes when pressing compute button a few times in sequence, 'chart-legend-item-symbol' color is init to default.
         * a bug?
         */
        Platform.runLater {
            lineChart.lookupAll(".chart-legend-item-symbol").forEach { node ->
                node.styleClass.filter { it.startsWith("series") }.forEach {
                    val i = it.substring("series".length).toInt()
                    val color = allExtendedSeries().find { it.series.name == lineChart.data[i].name }!!.color
                    node.style = "-fx-background-color: $color;"
                }
            }
        }
    }

    fun updateStyleOfAll() = allExtendedSeries().forEach { updateStyleOf(it) }


    /* TODO vertical scaling works separately for re_y and im_y of the imported complex data */
    fun importFrom(file: File) {
        LineChartState.importFrom(file)
        lineChart.run {
            with(imported[imported.lastIndex]) {
                data.add(extendedSeriesReal.series)
                /* if imported file contained 3 columns */
                if (extendedSeriesImaginary.series.data.isNotEmpty()) {
                    data.add(extendedSeriesImaginary.series)
                }
            }
        }
        updateLegendListener()
        updateStyleOfAll()
    }

    fun importMultiple(files: List<File>) = files.forEach { importFrom(it) }

    fun removeByName(name: String) = with(lineChart) { data.remove(data.find { it.name == name }) }

    /**
     * Sets the visibility for the line chart series corresponding to the extendedSeries
     */
    fun setVisibilityBy(extendedSeries: ExtendedSeries) {
        lineChart.data.find { it.name == extendedSeries.series.name }!!.node.visibleProperty().value = extendedSeries.visible
    }

    /**
     * Legend is initialized after the line chart is added to the scene, so 'Platform.runLater'
     * Legend items are dynamically changed (added and removed when changing regimes),
     * so this method is called at each 'updateLineChart' call to handle new legend items.
     * Otherwise mouse clicks after updates don't work.
     */
    fun updateLegendListener() = Platform.runLater {
        lineChart.labels().forEach { label ->
            label.setOnMouseClicked {
                val selected = allExtendedSeries().find { it.selected }
                if (selected == null) {
                    selectBy(label)
                } else {
                    deselect()
                    if (selected.series.name != label.text) {
                        selectBy(label)
                    }
                }
            }
        }
    }

    private fun selectBy(label: Label) =
            allExtendedSeries().find { it.series.name == label.text }?.let {
                it.select()
                updateStyleOf(it)
                mainController.seriesManagerController.enableUsing(it)
            }

    private fun deselect() =
            allExtendedSeries().find { it.selected }?.let {
                it.deselect()
                updateStyleOf(it)
                mainController.seriesManagerController.disable()
            }

    /* TODO fix this */
    private fun rescale() = with(mainController.globalParametersController.regimeController) {
        with(xAxis) {
            lowerBound = State.wavelengthStart
            upperBound = State.wavelengthEnd
            tickUnit = 50.0
            tickUnit = when {
                upperBound - lowerBound >= 4000.0 -> 500.0
                upperBound - lowerBound in 3000.0..4000.0 -> 250.0
                upperBound - lowerBound in 2000.0..3000.0 -> 200.0
                upperBound - lowerBound in 1000.0..2000.0 -> 100.0
                upperBound - lowerBound in 500.0..1000.0 -> 50.0
                upperBound - lowerBound in 200.0..500.0 -> 25.0
                upperBound - lowerBound in 200.0..500.0 -> 20.0
                upperBound - lowerBound < 200.0 -> 10.0
                else -> 5.0
            }
        }
        with(yAxis) {
            if (State.regime == REFLECTANCE || State.regime == ABSORBANCE || State.regime == TRANSMITTANCE) {
                lowerBound = 0.0
                upperBound = 1.0
                tickUnit = 0.1
            } else if (State.regime == PERMITTIVITY) {
                lowerBound = -10.0
                upperBound = 30.0
                tickUnit = 5.0
                isAutoRanging = false
            } else if (State.regime == REFRACTIVE_INDEX) {
                lowerBound = -1.0
                upperBound = 4.5
                tickUnit = 0.5
                isAutoRanging = false
            }
        }
    }

    /**
     * http://stackoverflow.com/questions/16473078/javafx-2-x-translate-mouse-click-coordinate-into-xychart-axis-value
     */
    private fun setCursorTracing() {

        val chartBackground = lineChart.lookup(".chart-plot-background")

        with(chartBackground) {
            parent.childrenUnmodifiable
                    .filter { it !== chartBackground && it !== xAxis && it !== yAxis }
                    .forEach { it.isMouseTransparent = true }

            setOnMouseEntered { XYPositionLabel.isVisible = true }
            setOnMouseMoved {
                XYPositionLabel.text = String.format(Locale.US, "x = %.2f, y = %.3f",
                        xAxis.getValueForDisplay(it.x).toDouble(),
                        yAxis.getValueForDisplay(it.y).toDouble())
            }
            setOnMouseExited { XYPositionLabel.isVisible = false }
        }
        with(xAxis) {
            setOnMouseEntered { XYPositionLabel.isVisible = true }
            setOnMouseMoved { mouseEvent ->
                XYPositionLabel.text = String.format(Locale.US, "x = %.2f", getValueForDisplay(mouseEvent.x).toDouble())
            }
            setOnMouseExited { XYPositionLabel.isVisible = false }
        }
        with(yAxis) {
            setOnMouseEntered { XYPositionLabel.isVisible = true }
            setOnMouseMoved { mouseEvent ->
                XYPositionLabel.text = String.format(Locale.US, "y = %.3f", getValueForDisplay(mouseEvent.y).toDouble())
            }
            setOnMouseExited { XYPositionLabel.isVisible = false }
        }
    }

    /**
     * from gillius zoomable and panning chart sample
     */
    private fun setPanning() {
        //Panning works via either secondary (right) mouse or primary with ctrl held down
        val panner = ChartPanManager(lineChart)
        panner.setMouseFilter { mouseEvent ->
            if (mouseEvent.button === SECONDARY || mouseEvent.button === PRIMARY && mouseEvent.isShortcutDown) {
                // let it through
            } else {
                mouseEvent.consume()
            }
        }
        panner.start()
    }

    /**
     * from gillius zoomable and panning chart sample
     */
    private fun setZooming() {
        /**
         * Redefined method from JFXChartUtil for customization
         */
        fun setupZooming(chart: XYChart<*, *>, mouseFilter: EventHandler<in MouseEvent>): Region = StackPane().apply {
            if (chart.parent != null) {
                JFXUtil.replaceComponent(chart, this)
            }

            val selectRect = Rectangle(0.0, 0.0, 0.0, 0.0)
            with(selectRect) {
                fill = Color.DARKGRAY
                isMouseTransparent = true
                opacity = 0.15
                stroke = Color.rgb(0, 0x29, 0x66)
                strokeType = StrokeType.INSIDE
                strokeWidth = 1.0
            }
            StackPane.setAlignment(selectRect, Pos.TOP_LEFT)

            children.addAll(chart, selectRect)

            with(ChartZoomManager(this@apply, selectRect, chart)) {
                this.mouseFilter = mouseFilter
                start()
            }
        }

        setupZooming(lineChart, EventHandler<MouseEvent> { mouseEvent ->
            if (mouseEvent.button !== PRIMARY || mouseEvent.isShortcutDown) {
                mouseEvent.consume()
            }
        })
    }

    private fun setDoubleMouseClickRescaling() {
        val chartBackground = lineChart.lookup(".chart-plot-background")
        chartBackground.setOnMouseClicked { mouseEvent ->
            if (mouseEvent.button == PRIMARY && mouseEvent.clickCount == 2) {
                rescale()
            }
        }
    }

    private fun LineChart<Number, Number>.labels() = childrenUnmodifiable
            .filter { it is Legend }.map { it as Legend }.flatMap { it.childrenUnmodifiable }
            .filter { it is Label }.map { it as Label }
}



