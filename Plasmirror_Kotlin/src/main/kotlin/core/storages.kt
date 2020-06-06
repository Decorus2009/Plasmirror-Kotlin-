package core

import org.apache.commons.io.FileUtils
import org.json.JSONObject
import ui.controllers.writeTo
import java.nio.file.Paths

/*
TODO Must be initialized before GUI elements
 */
object StructureDescriptionStorage {
  private val file = Paths.get("C:\\Users\\Decorus\\IdeaProjects\\Plasmirror\\Plasmirror_Kotlin\\data\\inner\\state_parameters\\structure.txt").toFile()
  var description = FileUtils.readFileToString(file, "utf-8")

  fun saveToFile() = FileUtils.writeStringToFile(file, description)
}

object ComputationParametersStorage {
  private val file = Paths.get("C:\\Users\\Decorus\\IdeaProjects\\Plasmirror\\Plasmirror_Kotlin\\data\\inner\\state_parameters\\parameters.json").toFile()
  private val content = FileUtils.readFileToString(file, "utf-8")
  private var parameters: JSONObject = JSONObject(content)

  fun saveToFile() = parameters.toString(4).writeTo(file)

  var regime: String
    get() = parameters.getString("regime")
    set(value) {
      parameters.put("regime", value)
    }

  var leftMedium: String
    get() = parameters.getString("left_medium")
    set(value) {
      parameters.put("left_medium", value)
    }

  var rightMedium: String
    get() = parameters.getString("right_medium")
    set(value) {
      parameters.put("right_medium", value)
    }

  var leftMediumRefractiveIndexReal: String
    get() = with(parameters.getJSONObject("left_medium_n")) {
      getString("real")
    }
    set(value) = with(parameters.getJSONObject("left_medium_n")) {
      put("real", value)
    }

  var leftMediumRefractiveIndexImaginary: String
    get() = with(parameters.getJSONObject("left_medium_n")) {
      getString("imaginary")
    }
    set(value) = with(parameters.getJSONObject("left_medium_n")) {
      put("imaginary", value)
    }

  var rightMediumRefractiveIndexReal: String
    get() = with(parameters.getJSONObject("right_medium_n")) {
      getString("real")
    }
    set(value) = with(parameters.getJSONObject("right_medium_n")) {
      put("real", value)
    }

  var rightMediumRefractiveIndexImaginary: String
    get() = with(parameters.getJSONObject("right_medium_n")) {
      getString("imaginary")
    }
    set(value) = with(parameters.getJSONObject("right_medium_n")) {
      put("imaginary", value)
    }

  var polarization: String
    get() = parameters.getString("polarization")
    set(value) {
      parameters.put("polarization", value)
    }

  var angle: String
    get() = parameters.getString("angle")
    set(value) {
      parameters.put("angle", value)
    }

  var wavelengthStart: String
    get() = with(parameters.getJSONObject("computation_range")) {
      getString("start")
    }
    set(value) = with(parameters.getJSONObject("computation_range")) {
      put("start", value)
    }

  var wavelengthEnd: String
    get() = with(parameters.getJSONObject("computation_range")) {
      getString("end")
    }
    set(value) = with(parameters.getJSONObject("computation_range")) {
      put("end", value)
    }

  var wavelengthStep: String
    get() = with(parameters.getJSONObject("computation_range")) {
      getString("step")
    }
    set(value) = with(parameters.getJSONObject("computation_range")) {
      put("step", value)
    }
}


