package ui.controllers

import MainApp
import core.State
import javafx.fxml.FXML

class RootController {

    @FXML lateinit var menuController: MenuController
    @FXML lateinit var mainController: MainController

    lateinit var mainApp: MainApp

    @FXML
    fun initialize() {
        println("Root controller set")
        /*
        mainController is "lateinit" due to it's initialized through the reflection (@FXML)
        BEFORE the root controller initialization.
         */
        menuController.rootController = this
        mainController.rootController = this
    }
}


class MainController {

    lateinit var rootController: RootController
    private lateinit var mainApp: MainApp
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
        println("Main controller set")

        globalParametersController.mainController = this
        controlsController.mainController = this
        lineChartController.mainController = this
        xAxisRangeController.mainController = this
        yAxisRangeController.mainController = this
        seriesManagerController.mainController = this

        State.run {
            mainController = this@MainController
            set()
            compute()
        }
        lineChartController.updateLineChart()
    }

    fun writeParametersChangingsToFiles() {
        globalParametersController.writeGlobalParameters()
        structureDescriptionController.writeStructureDescription()
    }
}
