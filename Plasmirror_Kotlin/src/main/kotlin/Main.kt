import core.State
import javafx.application.Application
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import ui.controllers.RootController

//class Main : Application() {

//    @Throws(Exception::class)
//    override fun start(primaryStage: Stage) {
//        val root = FXMLLoader.load<Parent>(Main.javaClass.getResource("window.fxml"))
//        primaryStage.title = "AlGaAs Permittivity"
//        primaryStage.scene = Scene(root)
//        primaryStage.show()
//    }
//    companion object {
//        @JvmStatic fun main(args: Array<String>) {
//            launch(Main::class.java)
//        }
//    }
//}


class MainApp : Application() {

    lateinit var primaryStage: Stage
//    private lateinit var rootLayout: AnchorPane
    @FXML private lateinit var rootController: RootController
    private lateinit var rootLayout: AnchorPane

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            launch(MainApp::class.java)
        }
    }

    @Throws(Exception::class)
    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage

//        val rootLayout = FXMLLoader.load<Parent>(MainApp.javaClass.getResource("fxml/rootLayout.fxml"))
//        primaryStage.title = "Plasmirror v1.0"
//        primaryStage.scene = Scene(rootLayout)
//        primaryStage.show()

        val loader = FXMLLoader()
        loader.location = MainApp::class.java.getResource("fxml/Root.fxml")
        rootLayout = loader.load<AnchorPane>()
        rootController = loader.getController()
        rootController.mainApp = this


        /*
        Let state initialization be here, before the opening of the app window,
        but after the loading of all the controllers.
        During the controllers loading some state parameters (such as polarization) are set.
        At the first call of a state parameter the "init" method from State is called (if present).
        In this method the main controller was set (for validation of state parameters)
        whereas it was not fully initialized while the child controllers are loading. This is incorrect
         */
        State.set()


        val scene = Scene(rootLayout)
        scene.stylesheets.add("css/chart.css")
        primaryStage.scene = scene
        primaryStage.show()
    }
}