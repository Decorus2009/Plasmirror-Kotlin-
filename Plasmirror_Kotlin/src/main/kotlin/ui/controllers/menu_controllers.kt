package ui.controllers


import MainApp
import core.State
import core.validators.MultipleExportDialogParametersValidator
import core.validators.ValidationResult.FAILURE
import core.validators.ValidationResult.SUCCESS
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.input.*
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyCombination.SHIFT_DOWN
import javafx.scene.input.KeyCombination.SHORTCUT_DOWN
import javafx.scene.layout.AnchorPane
import javafx.stage.*
import org.apache.commons.io.FileUtils
import rootController
import java.io.File
import java.io.File.separator
import java.nio.file.Paths


class MenuController {

  lateinit var rootController: RootController

  @FXML
  private lateinit var importMenuItem: MenuItem
  @FXML
  private lateinit var importMultipleMenuItem: MenuItem
  @FXML
  private lateinit var exportMenuItem: MenuItem
  @FXML
  private lateinit var exportMultipleMenuItem: MenuItem
  @FXML
  private lateinit var helpMenuItem: MenuItem
  @FXML
  private lateinit var fitterMenuItem: MenuItem

  @FXML
  fun initialize() {
    importMenuItem.accelerator = KeyCodeCombination(I, SHORTCUT_DOWN)
    importMultipleMenuItem.accelerator = KeyCodeCombination(I, SHORTCUT_DOWN, SHIFT_DOWN)
    exportMenuItem.accelerator = KeyCodeCombination(E, SHORTCUT_DOWN)
    exportMultipleMenuItem.accelerator = KeyCodeCombination(E, SHORTCUT_DOWN, SHIFT_DOWN)
    fitterMenuItem.accelerator = KeyCodeCombination(F, SHORTCUT_DOWN)

    importMenuItem.setOnAction {
      val file = initFileChooser(".").showOpenDialog(rootController.mainApp.primaryStage)
      if (file != null) {
        rootController.mainController.lineChartController.importFrom(file)
      }
    }

    importMultipleMenuItem.setOnAction {
      val files = initFileChooser(".").showOpenMultipleDialog(rootController.mainApp.primaryStage)
      if (files != null) {
        rootController.mainController.lineChartController.importMultiple(files)
      }
    }

    exportMenuItem.setOnAction {
      val file = initFileChooser(".").let {
        it.initialFileName = buildExportFileName()
        it.showSaveDialog(rootController.mainApp.primaryStage)
      }
      if (file != null) {
        writeComputedDataTo(file)
      }
    }

    exportMultipleMenuItem.setOnAction {
      val page = with(FXMLLoader()) {
        location = MainApp::class.java.getResource("fxml/MultipleExportDialog.fxml")
        load<AnchorPane>()
      }
      with(Stage()) {
        title = "Export Multiple"
        isResizable = false
        scene = Scene(page)
        /* works after pressing directory button or switching between angle and temperature regimes. Why? */
        addEventHandler(KeyEvent.KEY_RELEASED) { event: KeyEvent ->
          if (KeyCode.ESCAPE == event.code) {
            close()
          }
        }
        show()
      }
    }

    helpMenuItem.setOnAction {
      val page = with(FXMLLoader()) {
        location = MainApp::class.java.getResource("fxml/HelpInfo.fxml")
        load<AnchorPane>()
      }
      with(Stage()) {
        title = "Help"
        scene = Scene(page)
        /* works after pressing directory button or switching between angle and temperature regimes. Why? */
        addEventHandler(KeyEvent.KEY_RELEASED) { event: KeyEvent ->
          if (KeyCode.ESCAPE == event.code) {
            close()
          }
        }
        showAndWait()
      }
    }
  }

  private fun initFileChooser(dir: String) = FileChooser().apply {
    extensionFilters.add(FileChooser.ExtensionFilter("Data Files", "*.txt", "*.dat"))
    initialDirectory = File(dir)
  }
}

class MultipleExportDialogController {

//    lateinit var mainController: MainController
//    lateinit var rootController: RootController

  @FXML
  private lateinit var polarizationChoiceBox: ChoiceBox<String>

  @FXML
  lateinit var angleFromTextField: TextField
  @FXML
  lateinit var angleToTextField: TextField
  @FXML
  lateinit var angleStepTextField: TextField

  @FXML
  private lateinit var directoryButton: Button
  @FXML
  private lateinit var exportButton: Button
  //    @FXML
//    private lateinit var statusLabel: Label
  @FXML
  private lateinit var chosenDirectoryLabel: Label

//    var angleFrom: Double = 0.0
//    var angleTo: Double = 0.0
//    var angleStep: Double = 0.0

  var chosenDirectory: File? = null

  @FXML
  fun initialize() {
    println("multiple export dialog controller init")

    with(rootController.mainController.globalParametersController.lightParametersController) {
      this@MultipleExportDialogController.polarizationChoiceBox.selectionModel.selectedItemProperty()
        .addListener { _, _, newValue ->
          polarizationChoiceBox.value = newValue
        }

      angleFromTextField.textProperty().addListener { _, _, newValue ->
        angleTextField.text = newValue
      }
    }

    directoryButton.setOnMouseClicked {
      with(DirectoryChooser()) {
        initialDirectory = File(".")
        /**
        Need to pass Window or Stage. There's no access to any Stage object from this controller
        Solution: any Node from fxml that has fx:id
        http://stackoverflow.com/questions/25491732/how-do-i-open-the-javafx-filechooser-from-a-controller-class
         */
        chosenDirectory = showDialog(directoryButton.scene.window)
      }
      chosenDirectory?.let { chosenDirectoryLabel.text = it.canonicalPath }
    }

    exportButton.setOnMouseClicked {
      with(MultipleExportDialogParametersValidator) {
        if (validateChosenDirectory(chosenDirectory) == FAILURE) {
          return@setOnMouseClicked
        }

        if (validateAngles(angleFromTextField.text, angleToTextField.text, angleStepTextField.text) == FAILURE) {
          return@setOnMouseClicked
        }
      }

      with(rootController.mainController.globalParametersController.lightParametersController) {
        polarizationChoiceBox.value = this@MultipleExportDialogController.polarizationChoiceBox.value

        var currentAngle = angleFromTextField.text.toDouble()
        val angleTo = angleToTextField.text.toDouble()
        val angleStep = angleStepTextField.text.toDouble()

        while (currentAngle < 90.0 && currentAngle <= angleTo) {
          angleTextField.text = currentAngle.toString()
          with(State) {
            if (init() == SUCCESS) {
              compute()
              println("${angleTextField.text} ${State.angle}")
            }
          }
          writeComputedDataTo(File("${chosenDirectory!!.canonicalPath}$separator${buildExportFileName()}.txt"))
          currentAngle += angleStep
        }
      }
      info(contentText = "Export complete")
    }
  }

  private fun info(title: String = "Information", contentText: String) = with(Alert(AlertType.INFORMATION)) {
    this.title = title
    this.headerText = null
    this.contentText = contentText
    showAndWait()
  }
}

class HelpInfoController {

  @FXML
  private lateinit var helpTextArea: TextArea

  @FXML
  fun initialize() {
    helpTextArea.text = FileUtils.readFileToString(Paths.get("./data/inner/help.txt").toFile())
  }
}

