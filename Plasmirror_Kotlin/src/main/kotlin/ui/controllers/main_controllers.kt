package ui.controllers

import MainApp
import core.State
import javafx.fxml.FXML

class RootController {

    //    @FXML lateinit var menuController: MenuController
    @FXML lateinit var mainController: MainController

    lateinit var mainApp: MainApp

    @FXML
    fun initialize() {
        println("Root controller set")

//        menuLayoutController.setRootController(this)

        /*
        mainController is "lateinit" due to it's initialized through the reflection (@FXML)
        BEFORE the root controller initialization.
         */
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

//    @FXML private val computationTimeLabel: Label

//    fun setRootController(rootController: RootController) {
//        this.rootController = rootController
//    }


    // TODO подумать, как, не нажимая кнопку calculationButton, запустить расчет при изменени value в choiceBox для поляризации
    // это можно сделать через listener, как в SeriesManagerController
    @FXML
    fun initialize() {
        println("Main controller set")

        globalParametersController.mainController = this
        controlsController.mainController = this
        lineChartController.mainController = this
        xAxisRangeController.mainController = this
        yAxisRangeController.mainController = this
        seriesManagerController.mainController = this

        State.mainController = this
        State.run {
            set()
            compute()
        }
        lineChartController.update()

//        graphManagerController.setMainController(this)
//        xAxisRangeController.setMainController(this)
//        yAxisRangeController.setMainController(this)
//        globalParametersController.setMainController(this)
    }

//    fun writeChangingsToFiles() {
//
//        globalParametersController.writeGlobalParamsToFiles()
//        structureDescriptionController.writeStructureDescriptionToFile()
//    }
}
