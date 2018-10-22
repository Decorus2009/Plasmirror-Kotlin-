package ui.controllers

import core.*
import core.State.absorption
import core.State.permittivity
import core.State.polarization
import core.State.reflection
import core.State.refractiveIndex
import core.State.regime
import core.State.transmission
import core.State.wavelengthFrom
import core.State.wavelengthTo
import core.Regime.*
import core.State.wavelength
import javafx.scene.control.*
import org.apache.commons.io.FileUtils
import org.json.JSONObject
import java.io.File
import java.nio.file.Paths
import java.util.*

fun buildExportFileName() = StringBuilder().apply {
    append("computation_${regime}_${wavelengthFrom}_$wavelengthTo")
    if (regime == R || regime == T || regime == A) {
        append("_$polarization-POL_^${String.format(Locale.US, "%04.1f", State.angle)}_deg")
    }
}.toString()

fun writeComputedDataTo(file: File) = StringBuilder().apply {
    val computedReal: List<Double>
    var computedImaginary: List<Double> = emptyList()

    computedReal = when (regime) {
        R -> reflection
        T -> transmission
        A -> absorption
        PERMITTIVITY -> {
            computedImaginary = permittivity.map { it.imaginary }.toList()
            permittivity.map { it.real }.toList()
        }
        REFRACTIVE_INDEX -> {
            computedImaginary = refractiveIndex.map { it.imaginary }.toList()
            refractiveIndex.map { it.real }.toList()
        }
    }

    val columnSeparator = "    "
    with(computedReal) {
        indices.forEach { i ->
            append(String.format(Locale.US, "%.8f", wavelength[i]))
            append(columnSeparator)
            append(String.format(Locale.US, "%.8f", this[i]))

            with(computedImaginary) {
                if (isNotEmpty()) {
                    append(columnSeparator)
                    append(String.format(Locale.US, "%.8f", this[i]))
                }
            }
            append(System.lineSeparator())
        }
    }
}.toString().writeTo(file)

fun String.writeTo(file: File) = file.writeText(this)

fun disable(vararg labels: Label) = labels.forEach { it.isDisable = true }
fun <T> disable(vararg choiceBoxes: ChoiceBox<T>) = choiceBoxes.forEach { it.isDisable = true }
fun disable(vararg textFields: TextField) = textFields.forEach { it.isDisable = true }
fun disable(vararg checkBoxes: CheckBox) = checkBoxes.forEach { it.isDisable = true }
fun disable(vararg buttons: Button) = buttons.forEach { it.isDisable = true }
fun disable(vararg colorPickers: ColorPicker) = colorPickers.forEach { it.isDisable = true }

fun enable(vararg labels: Label) = labels.forEach { it.isDisable = false }
fun <T> enable(vararg choiceBoxes: ChoiceBox<T>) = choiceBoxes.forEach { it.isDisable = false }
fun enable(vararg textFields: TextField) = textFields.forEach { it.isDisable = false }
fun enable(vararg checkBoxes: CheckBox) = checkBoxes.forEach { it.isDisable = false }
fun enable(vararg buttons: Button) = buttons.forEach { it.isDisable = false }
fun enable(vararg colorPickers: ColorPicker) = colorPickers.forEach { it.isDisable = false }


object ComputationParameters {

    private var file = Paths.get("C:\\Users\\Vitalii\\YandexDisk\\RESEARCH [FTI]\\Util\\Plasmirror_Kotlin\\Plasmirror_Kotlin\\data\\inner\\state_parameters\\computation_range.txt").toFile()
    private val content = FileUtils.readFileToString(file, "utf-8")
    private var parameters: JSONObject = JSONObject(content)

    fun writeParameters() = parameters.toString(4).writeTo(file)

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


