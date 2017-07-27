import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.TextField

class Controller {

    @FXML
    fun initialize() {

        concentrationTextField.textProperty().addListener { _, _, newValue ->
            try {
                x = newValue.toDouble()
            } catch(ignored: NumberFormatException) {
            }
        }

        startTextField.textProperty().addListener { _, _, newValue ->
            try {
                start = newValue.toDouble()
            } catch(ignored: NumberFormatException) {
            }
        }
        endTextField.textProperty().addListener { _, _, newValue ->
            try {
                end = newValue.toDouble()
            } catch(ignored: NumberFormatException) {
            }
        }
        stepTextField.textProperty().addListener { _, _, newValue ->
            try {
                step = newValue.toDouble()
            } catch(ignored: NumberFormatException) {
            }
        }

        computeButton.setOnAction {
            validateTextFields()
        }
    }

    private fun validateTextFields() {
        try {
            concentrationTextField.text.toDouble()
            startTextField.text.toDouble()
            endTextField.text.toDouble()
            stepTextField.text.toDouble()
        } catch (ignored: RuntimeException) {
            alert()
        }

        if (x < 0.0 || x > 1.0 || start < 0.0 || end < 0.0 || step < 0.0 || start > end) {
            alert()
        }
    }


    private fun alert() {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = "Error"
        alert.headerText = "Error"
        alert.contentText = "Incorrect value format"

        alert.showAndWait()
    }

    private var x = 0.0

    private var start = 0.5
    private var end = 6.0
    private var step = 0.01

    @FXML private lateinit var concentrationTextField: TextField
    @FXML private lateinit var startTextField: TextField
    @FXML private lateinit var endTextField: TextField
    @FXML private lateinit var stepTextField: TextField
    @FXML private lateinit var computeButton: Button
}
