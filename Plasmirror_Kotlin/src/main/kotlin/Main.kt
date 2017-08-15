import core.State
import javafx.application.Application
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import ui.controllers.RootController

class MainApp : Application() {

    @FXML private lateinit var rootController: RootController

    lateinit var primaryStage: Stage
    private lateinit var rootLayout: AnchorPane

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            launch(MainApp::class.java)
        }
    }

    @Throws(Exception::class)
    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage
        with(FXMLLoader()) {
            location = MainApp::class.java.getResource("fxml/Root.fxml")
            rootLayout = load<AnchorPane>()
            rootController = getController()
            rootController.mainApp = this@MainApp
        }
        /**
        Let state initialization be here, before the opening of the app window,
        but after the loading of all the controllers.
        During the controllers loading some state parameters (such as polarization) are set.
        At the first call of a state parameter the "init" method from State is called (if present).
        In this method the main controller was set (for validation of state parameters)
        whereas it was not fully initialized while the child controllers are loading. This is incorrect
         */
        State.set()
        with(Scene(rootLayout)) {
            stylesheets.add("css/chart.css")
            primaryStage.scene = this
            primaryStage.show()
        }
    }
}