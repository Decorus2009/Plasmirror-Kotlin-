package core.validators

import javafx.scene.control.Alert

enum class ValidationResult { SUCCESS, FAILURE }

fun Double.isNotAllowed() = this !in 0.0..89.99999999

fun alert(title: String = "Error", headerText: String, contentText: String) = with(Alert(Alert.AlertType.ERROR)) {
    this.title = title
    this.headerText = headerText
    this.contentText = contentText
    showAndWait()
}
