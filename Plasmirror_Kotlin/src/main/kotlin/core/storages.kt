package core

import org.apache.commons.io.FileUtils
import org.json.JSONObject
import ui.controllers.writeTo
import java.nio.file.Paths

object StructureDescriptionStorage {
    private val file = Paths.get("D:\\Clouds\\YandexDisk\\RESEARCH [FTI]\\Util\\Plasmirror_Kotlin\\Plasmirror_Kotlin\\data\\inner\\state_parameters\\structure.txt").toFile()
    var description = FileUtils.readFileToString(file, "utf-8")

    fun save() = FileUtils.writeStringToFile(file, description)
}

object ComputationParameters {

    private val file = Paths.get("C:\\Users\\Vitalii\\YandexDisk\\RESEARCH [FTI]\\Util\\Plasmirror_Kotlin\\Plasmirror_Kotlin\\data\\inner\\state_parameters\\computation_range.txt").toFile()
    private val content = FileUtils.readFileToString(file, "utf-8")
    private var parameters: JSONObject = JSONObject(content)

    fun save() = parameters.toString(4).writeTo(file)

    var regime: Regime
        get() = Regime.valueOf(parameters.getString("regime"))
        set(value) {
            parameters.put("regime", value.toString())
        }

    var leftMedium: Medium
        get() = Medium.valueOf(parameters.getString("left_medium"))
        set(value) {
            parameters.put("left_medium", value.toString())
        }

    var rightMedium: Medium
        get() = Medium.valueOf(parameters.getString("right_medium"))
        set(value) {
            parameters.put("right_medium", value.toString())
        }

    var leftMediumRefractiveIndex: Complex_
        get() = with(parameters.getJSONObject("left_medium_n")) {
            Complex_(getDouble("real"), getDouble("imag"))
        }
        set(value) = with(parameters.getJSONObject("left_medium_n")) {
            put("real", value.real)
            put("imag", value.imaginary)
        }

    var rightMediumRefractiveIndex: Complex_
        get() = with(parameters.getJSONObject("right_medium_n")) {
            Complex_(getDouble("real"), getDouble("imag"))
        }
        set(value) = with(parameters.getJSONObject("right_medium_n")) {
            put("real", value.real)
            put("imag", value.imaginary)
        }

    var polarization: Polarization
        get() = Polarization.valueOf(parameters.getString("polarization"))
        set(value) {
            parameters.put("polarization", value.toString())
        }

    var angle: Double
        get() = parameters.getDouble("angle")
        set(value) {
            parameters.put("angle", value)
        }

    var computationRangeStart: Double
        get() = with(parameters.getJSONObject("computation_range")) {
            getDouble("start")
        }
        set(value) = with(parameters.getJSONObject("computation_range")) {
            put("start", value)
        }

    var computationRangeEnd: Double
        get() = with(parameters.getJSONObject("computation_range")) {
            getDouble("end")
        }
        set(value) = with(parameters.getJSONObject("computation_range")) {
            put("end", value)
        }

    var computationRangeStep: Double
        get() = with(parameters.getJSONObject("computation_range")) {
            getDouble("step")
        }
        set(value) = with(parameters.getJSONObject("computation_range")) {
            put("step", value)
        }
}


