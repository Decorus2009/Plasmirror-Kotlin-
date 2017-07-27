package ui.controllers

//import State.reflection
import com.sun.javafx.charts.Legend
import core.State
import core.State.regime
import core.State.wlEnd
import core.State.wlStart
import core.util.Regime.*
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.chart.XYChart.Data
import javafx.scene.chart.XYChart.Series
import javafx.scene.control.*
import javafx.scene.input.MouseButton
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
import ui.controllers.LineChartController.ComputationType.*
import ui.controllers.LineChartState.ExtendedSeries
import ui.controllers.LineChartState.SERIES_TYPE.COMPUTED
import ui.controllers.LineChartState.SERIES_TYPE.IMPORTED
import java.io.File
import java.lang.Integer.*
import java.nio.file.Files.lines
import java.nio.file.Path
import java.util.*
import kotlin.streams.toList


class LineChartController {

    lateinit var mainController: MainController

    @FXML lateinit var lineChart: LineChart<Number, Number>
    @FXML lateinit var xAxis: NumberAxis
    @FXML lateinit var yAxis: NumberAxis
    @FXML private lateinit var XYPositionLabel: Label

    private enum class ComputationType { REAL, COMPLEX, UNKNOWN }

    private var previousComputationDataType = UNKNOWN

    @FXML
    fun initialize() {
        xAxis.tickLabelFormatter = object : StringConverter<Number>() {
            override fun toString(`object`: Number): String {
                return String.format(Locale.ROOT, "%.1f", `object`.toDouble())
            }

            override fun fromString(string: String): Number {
                return 0
            }
        }

        yAxis.tickLabelFormatter = object : StringConverter<Number>() {
            override fun toString(`object`: Number): String {
                return String.format(Locale.ROOT, "%.2f", `object`.toDouble())
            }

            override fun fromString(string: String): Number {
                return 0
            }
        }
        //                it.text = String.format(Locale.ROOT, "%.5f", upperBound)


        println("Line chart controller set")
        with(lineChart) {
            createSymbols = false
            animated = false
            isLegendVisible = true
            /* force a css layout pass to ensure that subsequent lookup calls work. */
            applyCss()
        }
        xAxis.label = "Wavelength, nm"

//        updateAxisesNames()
//        setLegendListener()
        setCursorTracing()
        setPanning()
        setZooming()
        setDoubleMouseClickRescale()

//        yAxis.tickLabelFormatter.fromString()
    }

    fun update() {
        updateComputedSeries()
        updateYAxisLabel()
        /* regime == null is used during the first automatic call of rescale() method after initialization */
        with(mainController.globalParametersController.regimeController) {
            if (regimeBefore == null || regimeBefore != regime) {
                regimeBefore = regime
                rescale()
            }
        }
    }

    private fun updateComputedSeries() {

        LineChartState.updateComputed()

        lineChart.run {
            if (previousComputationDataType != UNKNOWN) {
                /* remove real series */
                data.removeAt(0)
                if (previousComputationDataType == COMPLEX) {
                    /* remove remained imaginary series */
                    data.removeAt(0)
                }
            }

            with(LineChartState.computed) {
                data.add(0, extendedSeriesReal.series)
                lookupAll(".series" + 0)
                        .forEach { it.style = "-fx-stroke: ${extendedSeriesReal.color}; -fx-stroke-width: 2px;" }
                previousComputationDataType = REAL

                if (extendedSeriesImaginary.series.data.isNotEmpty()) {
                    data.add(1, extendedSeriesImaginary.series)
                    lookupAll(".series" + 1)
                            .forEach { it.style = "-fx-stroke: ${extendedSeriesImaginary.color}; -fx-stroke-width: 2px;" }
                    previousComputationDataType = COMPLEX
                }
            }
        }

        setLegendListener()
    }

    private fun updateYAxisLabel() {
        yAxis.label = when (regime) {
            R -> "Reflection"
            T -> "Transmission"
            A -> "Absorption"
            EPS -> "Permittivity"
            N -> "Refractive index"
        }
    }

    /* TODO vertical scaling works separately for re_y and im_y of the imported complex data */
    fun importFrom(path: Path) {

        LineChartState.importFrom(path)

        lineChart.run {
            with(LineChartState.imported[LineChartState.imported.lastIndex]) {
                data.add(extendedSeriesReal.series)
                lookupAll(".series" + data.lastIndex)
                        .forEach { it.style = "-fx-stroke: ${extendedSeriesReal.color}; -fx-stroke-width: 2px;" }

                /* if imported file contained 3 columns */
                if (extendedSeriesImaginary.series.data.isNotEmpty()) {
                    data.add(extendedSeriesImaginary.series)
                    lookupAll(".series" + data.lastIndex)
                            .forEach { it.style = "-fx-stroke: ${extendedSeriesImaginary.color}; -fx-stroke-width: 2px;" }
                }
            }
        }
    }

    fun importMultiple(files: List<File>) = files.forEach { importFrom(it.toPath()) }


    private fun setLegendListener() {
        lineChart.labels().forEach { label ->
            label.setOnMouseClicked {
                val selected = LineChartState.allExtendedSeries().find { it.selected }
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

    private fun selectBy(label: Label) {
        val selectLabelCss = """
            -fx-background-insets: 0 0 -1 0, 0, 1, 2;
            -fx-padding: 7px;
            -fx-background-radius: 1px, 0px, 0px, 0px;
            -fx-background-color: #cccccc;
        """
//        label.style = selectLabelCss

        LineChartState.allExtendedSeries().find { it.series.name == label.text }?.let {
            it.series.node.style = "-fx-stroke: ${it.color}; -fx-stroke-width: 3px;"
            it.selected = true

            label.style = """
                -fx-stroke: ${it.color};
                -fx-background-insets: 0 0 -1 0, 0, 1, 2;
                -fx-padding: 7px;
                -fx-background-radius: 1px, 0px, 0px, 0px;
                -fx-background-color: #cccccc;
            """
            lineChart.lookupAll(".chart-legend-item-symbol").toTypedArray()[0].style = "-fx-background-color: ${it.color}";

//            Platform.runLater {
//                lineChart.lookupAll(".chart-legend-item-symbol").toTypedArray()[0].style = "-fx-background-color: ${it.color}";
//            }

            /* enable series manager */
            mainController.seriesManagerController.enableUsing(it)
        }
    }

    private fun deselect() {
        val deselectLabelCss = ""
        val deselectSeriesCss = "-fx-stroke-width: 2px;"

        LineChartState.allExtendedSeries().find { it.selected }?.let { extendedSeries ->
            extendedSeries.selected = false
            /* deselect all (due to the lookupAll() call) line chart series */
            for (i in 0 until lineChart.data.size) {
                lineChart.lookupAll(".series" + i)
                        .forEach { it.style = "-fx-stroke: ${extendedSeries.color}; -fx-stroke-width: 2px;" }
            }
            /* deselect label with corresponding name */
            lineChart.labels().find { it.text == extendedSeries.series.name }?.let {
//                it.style = deselectLabelCss
                it.style = "-fx-stroke: ${extendedSeries.color};"
                println(extendedSeries.color)
            }

            /* disable series manager */
            mainController.seriesManagerController.disable()
        }
    }

    /**
     * http://stackoverflow.com/questions/16473078/javafx-2-x-translate-mouse-click-coordinate-into-xychart-axis-value
     */
    private fun setCursorTracing() {

        val chartBackground = lineChart.lookup(".chart-plot-background")

        with(chartBackground) {
            parent.childrenUnmodifiable.stream()
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
            if (mouseEvent.button === MouseButton.SECONDARY || mouseEvent.button === MouseButton.PRIMARY && mouseEvent.isShortcutDown) {
                //let it through
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
            if (mouseEvent.button !== MouseButton.PRIMARY || mouseEvent.isShortcutDown) {
                mouseEvent.consume()
            }
        })
    }

    private fun setDoubleMouseClickRescale() {
        val chartBackground = lineChart.lookup(".chart-plot-background")
        chartBackground.setOnMouseClicked { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY && mouseEvent.clickCount == 2) {
                rescale()
            }
        }
    }

    // TODO fix this
    fun rescale() = with(mainController.globalParametersController.regimeController) {
        xAxis.run {
            lowerBound = wlStart
            upperBound = wlEnd
            tickUnit = 50.0
        }
        yAxis.run {
            if (regime == R || regime == A || regime == T) {
                lowerBound = 0.0
                upperBound = 1.0
                tickUnit = 0.1
            } else if (regime == EPS) {
                lowerBound = -5.0
                upperBound = 20.0
                tickUnit = 1.0
                isAutoRanging = false
            } else if (regime == N) {
                lowerBound = -3.0
                upperBound = 5.0
                tickUnit = 0.5
                isAutoRanging = false
            }
        }
    }

    private fun LineChart<Number, Number>.labels() = childrenUnmodifiable
            .filter { it is Legend }.map { it as Legend }.flatMap { it.childrenUnmodifiable }
            .filter { it is Label }.map { it as Label }
}


object LineChartState {

    val colors = mapOf(
            /* main */
            0 to "#F3622D", 1 to "#FBA71B", 2 to "#57B757", 3 to "#41A9C9", 4 to "#4258C9",
            5 to "#9A42C8", 6 to "#C84164", 7 to "#888888", 8 to "#000000", 9 to "#FFFFFF",
            /* additional */
            10 to "#FAEBD7", 11 to "#00FFFF", 12 to "#7FFFD4", 13 to "#F0FFFF", 14 to "#F5F5DC",
            15 to "#FFE4C4", 16 to "#000000", 17 to "#FFEBCD", 18 to "#0000FF", 19 to "#8A2BE2",
            20 to "#A52A2A", 21 to "#DEB887", 22 to "#5F9EA0", 23 to "#7FFF00", 24 to "#D2691E",
            25 to "#FF7F50", 26 to "#6495ED", 27 to "#FFF8DC", 28 to "#DC143C", 29 to "#00FFFF",
            30 to "#00008B", 31 to "#008B8B", 32 to "#B8860B", 33 to "#A9A9A9", 34 to "#A9A9A9",
            35 to "#006400", 36 to "#BDB76B", 37 to "#8B008B", 38 to "#556B2F", 39 to "#FF8C00",
            40 to "#9932CC", 41 to "#8B0000", 42 to "#E9967A", 43 to "#8FBC8F", 44 to "#483D8B",
            45 to "#2F4F4F", 46 to "#2F4F4F", 47 to "#00CED1", 48 to "#9400D3", 49 to "#FF1493",
            50 to "#00BFFF", 51 to "#696969", 52 to "#455B63", 53 to "#1E90FF", 54 to "#B22222",
            55 to "#FFFAF0", 56 to "#228B22", 57 to "#FF00FF", 58 to "#DCDCDC", 59 to "#F8F8FF",
            60 to "#FFD700", 61 to "#DAA520", 62 to "#808080", 63 to "#808080", 64 to "#008000",
            65 to "#ADFF2F", 66 to "#F0FFF0", 67 to "#FF69B4", 68 to "#CD5C5C", 69 to "#4B0082",
            70 to "#FFFFF0", 71 to "#F0E68C", 72 to "#E6E6FA", 73 to "#FFF0F5", 74 to "#7CFC00",
            75 to "#FFFACD", 76 to "#ADD8E6", 77 to "#F08080", 78 to "#E0FFFF", 79 to "#FAFAD2",
            80 to "#D3D3D3", 81 to "#D3D3D3", 82 to "#90EE90", 83 to "#FFB6C1", 84 to "#FFA07A",
            85 to "#20B2AA", 86 to "#87CEFA", 87 to "#778899", 88 to "#778899", 89 to "#B0C4DE",
            90 to "#FFFFE0", 91 to "#00FF00", 92 to "#32CD32", 93 to "#FAF0E6", 94 to "#FF00FF",
            95 to "#800000", 96 to "#66CDAA", 97 to "#0000CD", 98 to "#BA55D3", 99 to "#9370DB"
    )
    private var currentColorIndex = 2

    private fun nextColor(offset: Int = 0): String {
        if (currentColorIndex + offset > colors.size) {
            currentColorIndex = offset + 2
        }
        return colors[offset + currentColorIndex++]!!
    }


    val computed = LineChartSeries(ExtendedSeries(color = colors[0]!!), ExtendedSeries(color = colors[1]!!))
    val imported = mutableListOf<LineChartSeries>()

    fun allExtendedSeries() = (imported + computed).flatMap { listOf(it.extendedSeriesReal, it.extendedSeriesImaginary) }

    fun updateComputed() = with(computed) {

        extendedSeriesReal.series.data.run { if (isNotEmpty()) clear() }
        extendedSeriesImaginary.series.data.run { if (isNotEmpty()) clear() }

        extendedSeriesReal.series.data.run {
            with(State) {
                when (regime) {
                    R -> addAll(wl.indices.map { Data<Number, Number>(wl[it], reflection[it]) })
                    T -> addAll(wl.indices.map { Data<Number, Number>(wl[it], transmission[it]) })
                    A -> addAll(wl.indices.map { Data<Number, Number>(wl[it], absorption[it]) })
                    EPS -> {
                        addAll(wl.indices.map { Data<Number, Number>(wl[it], permittivity[it].real) })
                        extendedSeriesImaginary.series.data
                                .addAll(wl.indices.map { Data<Number, Number>(wl[it], permittivity[it].imaginary) })
                    }
                    N -> {
                        addAll(wl.indices.map { Data<Number, Number>(wl[it], refractiveIndex[it].real) })
                        extendedSeriesImaginary.series.data
                                .addAll(wl.indices.map { Data<Number, Number>(wl[it], refractiveIndex[it].imaginary) })
                    }
                }
            }
        }
        /* set names */
        extendedSeriesReal.series.name = "Computed Real"
        extendedSeriesImaginary.series.name = "Computed Imaginary"
    }

    fun importFrom(path: Path) {

        val x = mutableListOf<Double>()
        val re_y = mutableListOf<Double>()
        val im_y = mutableListOf<Double>()

        lines(path).toList().filter { it[0].isDigit() }.map { it.replace(Regex(","), ".") }.forEach {
            with(Scanner(it)) {
                if (hasNextDouble()) {
                    x += nextDouble()
                } else {
                    throw IllegalStateException("Input file must contain 2 or 3 columns")
                }
                if (hasNextDouble()) {
                    re_y += nextDouble()
                } else {
                    throw IllegalStateException("Input file must contain 2 or 3 columns")
                }
                /* check if file contains 3 columns for x, Re(re_y), Im(re_y) */
                if (hasNextDouble()) {
                    im_y += nextDouble()
                }
            }
        }

        val seriesReal = Series<Number, Number>()
        val seriesImaginary = Series<Number, Number>()

        seriesReal.data.addAll(x.indices.map { Data<Number, Number>(x[it], re_y[it]) })
        if (im_y.isNotEmpty()) {
            seriesImaginary.data.addAll(x.indices.map { Data<Number, Number>(x[it], im_y[it]) })
        }

        /* set names */
        with(path.fileName.toString()) {
            seriesReal.name = this + " Real"
            seriesImaginary.name = this + " Imaginary"
        }
        imported += LineChartSeries(ExtendedSeries(seriesReal, type = IMPORTED), ExtendedSeries(seriesImaginary, type = IMPORTED))
    }

    class LineChartSeries(val extendedSeriesReal: ExtendedSeries = ExtendedSeries(),
                          val extendedSeriesImaginary: ExtendedSeries = ExtendedSeries(color = nextColor(offset = 50)))

    class ExtendedSeries(val series: Series<Number, Number> = Series<Number, Number>(),
                         var visible: Boolean = true,
                         var selected: Boolean = false,
                         var color: String = nextColor(),
                         var type: SERIES_TYPE = COMPUTED,
                         var xAxisFactor: Double = 1.0, var yAxisFactor: Double = 1.0)

    enum class SERIES_TYPE { COMPUTED, IMPORTED }
}


class SeriesManagerController {

    lateinit var mainController: MainController

    @FXML private lateinit var colorLabel: Label
    @FXML private lateinit var colorPicker: ColorPicker
    @FXML private lateinit var xAxisFactorLabel: Label
    @FXML private lateinit var xAxisFactorTextField: TextField
    @FXML private lateinit var yAxisFactorLabel: Label
    @FXML private lateinit var yAxisFactorTextField: TextField
    @FXML private lateinit var visibleCheckBox: CheckBox
    @FXML private lateinit var removeButton: Button

    private lateinit var selectedSeries: ExtendedSeries

    @FXML
    fun initialize() {

        /*
          Цвет при colorPicker.getValue() выдается не в HTML формате, который нужен для css
          Конвертацию см. по ссылке http://stackoverflow.com/a/17925600
         */
        colorPicker.setOnAction {
            val hexColor = with(colorPicker.value) {
                "#" + toHexString((red * 255).toInt()) + toHexString((green * 255).toInt()) + toHexString((blue * 255).toInt())
            }

            selectedSeries.color = hexColor
            mainController.lineChartController.lineChart.lookupAll(".series" + 0)
                    .forEach { it.style = "-fx-stroke: ${selectedSeries.color};" }


//            mainController.lineChartController.updateColorsAndWidths()
        }


        xAxisFactorTextField
                .textProperty()
                .addListener { observable, oldValue, newValue -> tryScaleData(newValue, false) }

        yAxisFactorTextField
                .textProperty()
                .addListener { observable, oldValue, newValue -> tryScaleData(newValue, true) }

        visibleCheckBox.isSelected = true

        visibleCheckBox.setOnAction { event ->

            //            if (visibleCheckBox.isSelected) {
//
//                selectedSeries!!.setVisible(true)
//                selectedSeries!!.getSeries().getNode().visibleProperty().setValue(true)
//            } else {
//
//                selectedSeries!!.setVisible(false)
//                selectedSeries!!.getSeries().getNode().visibleProperty().setValue(false)
//            }
        }

        disable()
    }




    @FXML
    internal fun removeButtonClicked(event: ActionEvent) {

//        mainController!!
//                .getChartController()
//                .getLineChart().getData().remove(selectedSeries!!.getSeries())
//
//        if (ChartData.imported.contains(selectedSeries)) {
//            ChartData.imported.remove(selectedSeries)
//
//        } else if (ChartData.calculated.contains(selectedSeries)) {
//            ChartData.calculated.remove(selectedSeries)
//        }
//
//        mainController!!.getChartController().updateColorsAndWidths()
//        disable()
    }

    private fun tryScaleData(newValue: String, yAxis: Boolean) {

//        try {
//            val factor = java.lang.Double.parseDouble(newValue)
//
//            val selected = selectedSeries!!.getSeries()
//            val backup = ChartData.importedBackup.get(selected)
//
//            if (yAxis) {
//
//                for (i in 0..selected.getData().size - 1) {
//                    selected.getData().get(i).setYValue(backup.getData().get(i).getYValue().toDouble() * factor)
//                }
//                selectedSeries!!.setyAxisFactor(factor)
//            } else {
//
//                for (i in 0..selected.getData().size - 1) {
//                    selected.getData().get(i).setXValue(backup.getData().get(i).getXValue().toDouble() * factor)
//                }
//                selectedSeries!!.setxAxisFactor(factor)
//            }
//
//        } catch (ignored: RuntimeException) {
//            // нет смысла тут что-то делать, в процессе ввода могут получаться значения, которые не парсятся.
//            // Главное, чтобы конечное число было корректным
//        }

    }

    fun enableUsing(selectedSeries: ExtendedSeries) {
        this.selectedSeries = selectedSeries

        enable(colorLabel, xAxisFactorLabel, yAxisFactorLabel)
        enable(xAxisFactorTextField, yAxisFactorTextField)
        enable(colorPicker)
        enable(visibleCheckBox)
        enable(removeButton)

        with(selectedSeries) {
            if (type == COMPUTED) {
                disable(xAxisFactorLabel, yAxisFactorLabel)
                disable(xAxisFactorTextField, yAxisFactorTextField)
                disable(removeButton)
            }
            xAxisFactorTextField.text = xAxisFactor.toString()
            yAxisFactorTextField.text = yAxisFactor.toString()
            colorPicker.value = Color.valueOf(color)
            visibleCheckBox.isSelected = visible
        }
    }

    fun disable() {
        disable(colorLabel, xAxisFactorLabel, yAxisFactorLabel)
        disable(xAxisFactorTextField, yAxisFactorTextField)
        disable(colorPicker)
        disable(visibleCheckBox)
        disable(removeButton)
    }
}




