package ui.controllers

import core.State
import core.State.absorption
import core.State.permittivity
import core.State.reflection
import core.State.refractiveIndex
import core.State.regime
import core.State.transmission
import core.util.Regime.*
import javafx.scene.control.*
import java.io.File
import java.util.*

fun buildExportFileName() = StringBuilder().apply {
    append("calculation_${State.regime}_${State.wavelengthFrom}_${State.wavelengthTo}")
    if (State.regime == R || State.regime == T || State.regime == A) {
        append("_${State.polarization}-POL_^${String.format(Locale.US, "%04.1f", State.angle)}_deg")
    }
}.toString()

//fun writeTo(file: File) = file.writeText(StringBuilder().apply {
//    val columnSeparator = "    "
//    with(LineChartState.computed.extendedSeriesReal.series.data) {
//        indices.forEach { i ->
//            append(String.format(Locale.US, "%.8f", this[i].xValue.toDouble()))
//            append(columnSeparator)
//            append(String.format(Locale.US, "%.8f", this[i].yValue.toDouble()))
//
//            with(LineChartState.computed.extendedSeriesImaginary.series.data) {
//                if (isNotEmpty()) {
//                    append(columnSeparator)
//                    append(kotlin.String.format(Locale.US, "%.8f", this[i].yValue.toDouble()))
//                }
//            }
//            append(System.lineSeparator())
//        }
//    }
//}.toString())

fun writeTo(file: File) = file.writeText(StringBuilder().apply {

    val computedReal: List<Double>
    var computedImaginary: List<Double> = emptyList()

    computedReal = when (regime) {
        R -> reflection
        T -> transmission
        A -> absorption
        EPS -> {
            computedImaginary = permittivity.map { it.imaginary }.toList()
            permittivity.map { it.real }.toList()
        }
        N -> {
            computedImaginary = refractiveIndex.map { it.imaginary }.toList()
            refractiveIndex.map { it.real }.toList()
        }
    }
//
//    println(computedReal.size)
//    println(computedImaginary.size)
//

    val columnSeparator = "    "
    with(computedReal) {
        indices.forEach { i ->
            append(String.format(Locale.US, "%.8f", this[i]))
            append(columnSeparator)
            append(String.format(Locale.US, "%.8f", this[i]))

            with(computedImaginary) {
                if (isNotEmpty()) {
                    append(columnSeparator)
                    append(kotlin.String.format(Locale.US, "%.8f", this[i]))
                }
            }
            append(System.lineSeparator())
        }
    }
}.toString())


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


