/*
package ui.controllers

//import State.reflection
import com.sun.javafx.charts.Legend
import State
import State.regime
import State.wlEnd
import State.wlStart
import Regime.*
import javafx.fxml.FXML
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart.Data
import javafx.scene.chart.XYChart.Series
import javafx.scene.control.Label
import javafx.scene.input.MouseButton
import org.gillius.jfxutils.chart.ChartPanManager
import org.gillius.jfxutils.chart.JFXChartUtil
import LineChartController.ComputationType.*
import LineChartState.SeriesType.COMPUTED
import LineChartState.SeriesType.IMPORTED
import java.nio.file.Files.lines
import java.nio.file.Path
import java.util.*
import kotlin.streams.toList


class LineChartController {

    lateinit var mainController: MainController

    @FXML lateinit var lineChart: LineChart<Number, Number>
    @FXML private lateinit var xAxis: NumberAxis
    @FXML private lateinit var yAxis: NumberAxis
    @FXML private lateinit var XYPositionLabel: Label

    private enum class ComputationType { REAL, COMPLEX, UNKNOWN }

    private var previousComputationDataType = UNKNOWN

    @FXML
    fun initialize() {
        println("Line chart controller set")
        with(lineChart) {
            createSymbols = false
            animated = false
            isLegendVisible = true
            */
/* force a css layout pass to ensure that subsequent lookup calls work. *//*

            applyCss()
        }
        xAxis.label = "Wavelength, nm"

//        updateAxisesNames()
//        setLegendListener()
        setCursorTracing()
        setPanning()
        setZooming()
        setDoubleMouseClickRescale()
    }

    fun update() {
        updateComputedSeries()
        updateYAxisLabel()
        */
/* regime == null is used during the first automatic call of rescale() method after initialization *//*

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
                */
/* remove real series *//*

                data.removeAt(0)
                if (previousComputationDataType == COMPLEX) {
                    */
/* remove remained imaginary series *//*

                    data.removeAt(0)
                }
            }

            with(LineChartState.computed) {
                data.add(0, seriesReal)
                lookupAll(".series" + 0).forEach { it.style = "-fx-stroke: $colorReal;" }
                previousComputationDataType = REAL

                if (seriesImaginary.data.isNotEmpty()) {
                    data.add(1, seriesImaginary)
                    lookupAll(".series" + 1).forEach { it.style = "-fx-stroke: $colorImaginary;" }
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

    */
/* TODO vertical scaling works separately for re_y and im_y of the imported complex data *//*

    fun importFrom(path: Path) {

        LineChartState.importFrom(path)

        lineChart.run {
            with(LineChartState.imported[LineChartState.imported.lastIndex]) {
                data.add(seriesReal)
                lookupAll(".series" + data[data.lastIndex]).forEach { it.style = "-fx-stroke: $colorReal;" }

                */
/* if imported file contained 3 columns *//*

                if (seriesImaginary.data.isNotEmpty()) {
                    data.add(seriesImaginary)
                    lookupAll(".series" + data[data.lastIndex]).forEach { it.style = "-fx-stroke: $colorImaginary;" }
                }
            }
        }
    }


    */
/* =========================== updates =========================== *//*


//    internal fun updateCalculationSeries() {
//
//        if (regime === reflection || regime === transmission || regime === absorption) {
//
//            ChartData.updateRealCalculationData()
//            updateRealCalculationSeries()
//        } else {
//
//            ChartData.updateComplexCalculationData()
//            updateComplexCalculationSeries()
//        }
//        updateSelection()
//    }
//
//    private fun updateRealCalculationSeries() {
//
//        // на графике всегда есть данные расчета (у них всегда индекс 0), их стереть
//        if (lineChart.data.size != 0) {
//            lineChart.data.removeAt(0)
//        }
//        lineChart.data.add(0, ChartData.computed.get(0).getSeries())
//    }
//
//    private fun updateComplexCalculationSeries() {
//
//        if (lineChart.data.size > 1) {
//            lineChart.data.remove(0, 2)
//        }
//        lineChart.data.add(ChartData.computed.get(0).getSeries())
//        lineChart.data.add(ChartData.computed.get(1).getSeries())
//    }
//
//    internal fun updateIfAnotherRegime() {
//
//        if (regime !== RegimeController.regimeBefore && RegimeController.regimeBefore != null) {
//
//            deselectAll()
//            ChartData.clearAll()
//            lineChart.data.clear()
//            ChartData.initCalculated()
//            rescale()
//
//            RegimeController.regimeBefore = regime
//        }
//    }
//
//    */
/**
//     * Установка css на Node работает только через lookupAll.
//     * Просто, применяя setStyle() к Node без lookupAll, ничего не происходит.
//     * И обязательно надо насильно использовать chart.applyCss(); Без него ничего работать не будет.
//     *//*

//    internal fun updateColorsAndWidths() {
//
//        // force a css layout pass to ensure that subsequent lookup calls work.
//        lineChart.applyCss()
//
//        val selected = findSelected()
//
//        if (selected == null) {
//
//            for (i in 0..lineChart.data.size - 1) {
//
//                val current = findByName(lineChart.data[i].name)
//
//                val nodes = lineChart.lookupAll(".series" + i)
//                for (refractiveIndex in nodes) {
//                    refractiveIndex.style = "-fx-stroke-width: 2px;"
//                    +"-fx-stroke: " + current.getColor() + "; "
//                    +"-fx-background-color: " + current.getColor() + ", " + current.getColor() + ";"
//                }
//            }
//        } else {
//
//            for (i in 0..lineChart.data.size - 1) {
//
//                if (lineChart.data[i].name == selected.getSeries().getName()) {
//
//                    val nodes = lineChart.lookupAll(".series" + i)
//                    for (refractiveIndex in nodes) {
//                        refractiveIndex.style = "-fx-stroke-width: 4px;"
//                        +"-fx-stroke: " + selected.getColor() + "; "
//                        +"-fx-background-color: " + selected.getColor() + ", " + selected.getColor() + ";"
//                    }
//                } else {
//                    val current = findByName(lineChart.data[i].name)
//
//                    val nodes = lineChart.lookupAll(".series" + i)
//                    for (refractiveIndex in nodes) {
//                        refractiveIndex.style = "-fx-stroke-width: 2px;"
//                        +"-fx-stroke: " + current.getColor() + "; "
//                        +"-fx-background-color: " + current.getColor() + ", " + current.getColor() + ";"
//                    }
//                }
//            }
//        }
//    }
//
//    */
/**
//     * Импорт данных вида (x; y) из внешнего файла
//     *//*

//    internal fun importSingle(file: Path?) {
//
//        if (file == null) {
//            return
//        }
//
//        ChartData.importData(file)
//        lineChart.data.add(ChartData.imported.get(ChartData.imported.size() - 1).getSeries())
//
//        updateSelection()
//    }
//
//    internal fun importMultiple(files: List<File>) {
//        for (file in files) {
//            importSingle(file.toPath())
//        }
//    }
//
//    fun updateAxisesNames() {
//
//        if (regime === reflection || regime === transmission || regime === absorption) {
//            yAxis.label = regime.name()
//        } else if (regime === N) {
//            yAxis.label = regime.name().toLowerCase()
//
//        } else if (regime === EPS) {
//
//            yAxis.label = regime.name().substring(0, 1) + regime.name().substring(1).toLowerCase()
//        }
//        xAxis.label = "Wavelength (nm)"
//    }
//
//
//    */
/* =========================== selection =========================== *//*

//
//    private fun selectBy(label: Label) {
//
//        val series = findByName(label.text)
//        if (series != null && !series.isSelected()) {
//
//            label.style = "-fx-background-insets: 0 0 -1 0, 0, 1, 2;"
//            +"-fx-padding: 7px;"
//            +"-fx-background-radius: 1px, 0px, 0px, 0px;"
//            +"-fx-background-color: #cccccc;"
//            series.setSelected(true)
//
//            updateColorsAndWidths()
//            mainController.getGraphManagerController().enableGraphInfo(series)
//        }
//    }
//
//    private fun deselectBy(label: Label) {
//
//        val series = findByName(label.text)
//        if (series != null && series.isSelected()) {
//
//            label.style = ""
//            series.setSelected(false)
//
//            updateColorsAndWidths()
//            mainController.getGraphManagerController().disableGraphInfo()
//        }
//    }
//
//    private fun deselectAll() {
//
//        lineChart.childrenUnmodifiable
//                .stream()
//                .filter { node -> node is Legend }
//                .forEach { legendNode ->
//
//                    val legend = legendNode as Legend
//
//                    legend.childrenUnmodifiable
//                            .stream()
//                            .filter { it -> it is Label }
//                            .forEach { label -> deselectBy(label as Label) }
//                }
//    }
//
//    private fun updateSelection() {
//
//        lineChart.childrenUnmodifiable
//                .stream()
//                .filter { node -> node is Legend }
//                .forEach { legendNode ->
//
//                    val legend = legendNode as Legend
//
//                    legend.childrenUnmodifiable
//                            .stream()
//                            .filter { node -> node is Label }
//                            .forEach { node ->
//
//                                val label = node as Label
//                                if (findSelected() != null) {
//
//                                    if (label.text == findSelected().getSeries().getName()) {
//
//                                        deselectBy(label)
//                                        selectBy(label)
//                                    } */
/*else {
//
//                                        selectBy(label);
//                                        deselectBy(label);
//                                    }*//*

//                                } else {
//
//                                    selectBy(label)
//                                    deselectBy(label)
//                                }
//                            }
//                }
//    }
//
//    private fun findByName(name: String): RealSeries? {
//
//        // найти по имени среди computed
//        var res = ChartData.computed
//                .stream()
//                // отфильтровать it.getSeries() == null, иначе NPE при it.getSeries().getName()
//                .filter({ it -> it.getSeries() != null })
//                .filter({ it ->
//                    it.getSeries()
//                            .getName()
//                            .equals(name)
//                })
//                .findFirst()
//                .orElse(null)
//
//        if (res != null) {
//            return res
//        }
//
//        // найти по имени среди imported
//        res = ChartData.imported
//                .stream()
//                .filter({ it -> it.getSeries().getName().equals(name) })
//                .findFirst().orElse(null)
//
//        return res
//    }
//
//    private fun findSelected(): RealSeries? {
//
//        // найти среди computed такие RealSeries, что isSelected() == true
//        var res = ChartData.computed
//                .stream()
//                // надо отфильтровать it.getSeries() == null, иначе NPE при it.getSeries().getName()
//                .filter({ it -> it.getSeries() != null })
//                .filter(???({ RealSeries.isSelected() }))
//        .findFirst().orElse(null)
//
//        if (res != null) {
//            return res
//        }
//
//        // найти среди imported такие RealSeries, что isSelected() == true
//        res = ChartData.imported
//                .stream()
//                .filter(???({ RealSeries.isSelected() }))
//        .findFirst().orElse(null)
//
//        return res
//    }
//
//    internal fun setName(selected: RealSeries, newName: String) {
//
//        lineChart.childrenUnmodifiable
//                .stream()
//                .filter { node -> node is Legend }
//                .forEach { node ->
//
//                    val legend = node as Legend
//
//                    legend.childrenUnmodifiable
//                            .stream()
//                            .filter { it -> it is Label }
//                            .forEach { labelNode ->
//
//                                val label = labelNode as Label
//
//                                // тут почему-то снимается выделение с самого Label, поэтому updateSelection()
//                                if (label.text == selected.getSeries().getName()) {
//                                    selected.getSeries().setName(newName)
//                                    updateSelection()
//                                }
//                            }
//                }
//    }


    */
/* =========================== setups =========================== *//*


    */
/**
     * http://stackoverflow.com/questions/16473078/javafx-2-x-translate-mouse-click-coordinate-into-xychart-axis-value
     *//*

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

    */
/**
     * from gillius zoomable and panning chart sample
     *//*

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

    */
/**
     * from gillius zoomable and panning chart sample
     * todo зуммировать получается только протягивая слева направо, сверху вниз
     *//*

    private fun setZooming() {
        //Zooming works only via primary mouse button without ctrl held down
        JFXChartUtil.setupZooming(lineChart, { mouseEvent ->
            if (mouseEvent.button !== MouseButton.PRIMARY || mouseEvent.isShortcutDown) {
                mouseEvent.consume()
            }
        })
    }

    */
/**
     * http://stackoverflow.com/questions/12622709/javafx-2-0-how-to-change-legend-color-of-a-linechart-dynamically
     *//*

    private fun setLegendListener() {
        lineChart.childrenUnmodifiable
                .filter { it is Legend }.map { it as Legend }
                .flatMap { it.childrenUnmodifiable }
                .filter { it is Label }.map { it as Label }
                .forEach { label ->
                    label.setOnMouseClicked {
                        selectBy(label)


                    }
                }


//                 .forEach {
//                     it.childrenUnmodifiable
//                             .filter { it is Label }
//                             .map { it as Label }
//                             .forEach { println(it.text) }
//                 }

//                .forEach { node ->
//
//                    val legend = node as Legend
//
//                    // рассматриваем все Label в легенде. Их количество меняется динамически при добавлении графиков
//                    legend.childrenUnmodifiable.addListener(javafx.collections.ListChangeListener<kotlin.Any> {
//                        legend.childrenUnmodifiable
//                                .stream()
//                                .filter { node -> node is Label }
//                                .forEach { labelNode ->
//
//                                    val label = labelNode as Label
//
//                                    label.setOnMouseClicked { event ->
//
//                                        if (findSelected() == null) {
//                                            selectBy(label)
//
//                                        } else {
//                                            if (findSelected().getSeries().getName().equals(label.text)) {
//                                                deselectBy(label)
//
//                                            } else {
//                                                deselectAll()
//                                                selectBy(label)
//                                            }
//                                        }
//                                    }
//                                }
//                    })
//                }
    }

    private fun selectBy(label: Label) {
        */
/* select label *//*

        label.style = """
            -fx-background-insets: 0 0 -1 0, 0, 1, 2;
            -fx-padding: 7px;
            -fx-background-radius: 1px, 0px, 0px, 0px;
            -fx-background-color: #cccccc;
        """

        */
/* select series *//*

        with(LineChartState) {

            val matchedLineChartSeries = (imported + computed)
                    .find { it.seriesReal.name == label.text || it.seriesImaginary.name == label.text }

            val series = with(matchedLineChartSeries!!) {
                if (seriesReal.name == label.text) {
                    selectedReal = true
                    seriesReal
                } else {
                    selectedImaginary = true
                    seriesImaginary
                }
            }
            series.node.style = "-fx-stroke-width: 4px;"
        }
//        val series = (imported + computed).flatMap { listOf(it.seriesReal, it.seriesImaginary) }.find { it.name == name }
    }




    private fun setDoubleMouseClickRescale() {
        val chartBackground = lineChart.lookup(".chart-plot-background")
        chartBackground.setOnMouseClicked { mouseEvent ->
            if (mouseEvent.button == MouseButton.PRIMARY && mouseEvent.clickCount == 2) {
                rescale()
            }
        }
    }
//
//    private fun setupSingleMouseClickOnLineChartBackground() {
//        */
/*
//               final Node chartBackground = lineChart.lookup(".chart-plot-background");
//               chartBackground.setOnMouseClicked(mouseEvent -> {
//
//                   for (RealSeries seriesWithInfo : allSeriesWithInfo) {
//                       if (seriesWithInfo.isSelected()) {
//
//                           Node selectedLegendItem = findLegendItemForSelectedSeriesWithInfo(seriesWithInfo);
//                           if (selectedLegendItem != null) {
//                               selectedLegendItem.setStyle(" ");
//                           }
//
//                           seriesWithInfo.setSelected(false);
//                           setDefaultSeriesWidth(seriesWithInfo.getSeries().getNode());
//
//                           mainController
//                                   .getGraphManagerController()
//                                   .disableGraphInfo();
//                       }
//                   }
//                   updateLegendHandler(false);
//               });
//       *//*

//    }
//


*/
/* =========================== other =========================== *//*


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
}


object LineChartState {

    val colors = mapOf(
            */
/* main *//*

            0 to "#F3622D", 1 to "#FBA71B", 2 to "#57B757", 3 to "#41A9C9", 4 to "#4258C9",
            5 to "#9A42C8", 6 to "#C84164", 7 to "#888888", 8 to "#000000", 9 to "#FFFFFF",
            */
/* additional *//*

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


    val computed = LineChartSeries(SERIESType = COMPUTED, colorReal = colors[0]!!, colorImaginary = colors[1]!!)
    val imported = mutableListOf<LineChartSeries>()


    fun updateComputed() = with(computed) {

        seriesImaginary.data.run { if (isNotEmpty()) clear() }
        seriesReal.data.run { if (isNotEmpty()) clear() }

        seriesReal.data.run {
            with(State) {
                when (regime) {
                    R -> addAll(wl.indices.map { Data<Number, Number>(wl[it], reflection[it]) })
                    T -> addAll(wl.indices.map { Data<Number, Number>(wl[it], transmission[it]) })
                    A -> addAll(wl.indices.map { Data<Number, Number>(wl[it], absorption[it]) })
                    EPS -> {
                        addAll(wl.indices.map { Data<Number, Number>(wl[it], permittivity[it].real) })
                        seriesImaginary.data.addAll(wl.indices.map { Data<Number, Number>(wl[it], permittivity[it].imaginary) })
                    }
                    N -> {
                        addAll(wl.indices.map { Data<Number, Number>(wl[it], refractiveIndex[it].real) })
                        seriesImaginary.data.addAll(wl.indices.map { Data<Number, Number>(wl[it], refractiveIndex[it].imaginary) })
                    }
                }
            }
        }
        */
/* set names *//*

        seriesReal.name = "Computed Real"
        seriesImaginary.name = "Computed Imaginary"
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
                */
/* check if file contains 3 columns for x, Re(re_y), Im(re_y) *//*

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

        */
/* set names *//*

        with(path.fileName.toString()) {
            seriesReal.name = this + "Real"
            seriesImaginary.name = this + "Imaginary"
        }
        imported += LineChartSeries(seriesReal, seriesImaginary, SERIESType = IMPORTED)
    }

    class LineChartSeries(val seriesReal: Series<Number, Number> = Series<Number, Number>(),
                          val seriesImaginary: Series<Number, Number> = Series<Number, Number>(),
                          var SERIESType: SeriesType,
                          var visibleReal: Boolean = true, var visibleImaginary: Boolean = true,
                          var selectedReal: Boolean = false, var selectedImaginary: Boolean = false,
                          var realXAxisFactor: Double = 1.0, var realYAxisFactor: Double = 1.0,
                          var imaginaryXAxisFactor: Double = 1.0, var imaginaryYAxisFactor: Double = 1.0,
                          var colorReal: String = nextColor(), var colorImaginary: String = nextColor(offset = 50))

//    class ComplexSeries(realSeries: RealSeries = RealSeries(), imaginarySeries: RealSeries)

    class RealSeries(val series: Series<Number, Number> = Series<Number, Number>(),
                     var visible: Boolean = true,
                     var selected: Boolean = false,
                     var color: String = nextColor(),
                     var xAxisFactor: Double = 1.0, var yAxisFactor: Double = 1.0)

    enum class SeriesType { COMPUTED, IMPORTED }
}



*/
