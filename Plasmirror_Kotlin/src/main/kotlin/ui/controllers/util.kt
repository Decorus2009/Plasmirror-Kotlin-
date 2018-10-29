package ui.controllers

import core.*
import core.optics.Regime

import core.optics.Regime.*

import javafx.scene.control.*
import java.io.File
import java.util.*

fun buildExportFileName() = StringBuilder().apply {
    append("computation_${State.regime}_${State.wavelengthStart}_${State.wavelengthEnd}")
    if (State.regime == REFLECTANCE || State.regime == TRANSMITTANCE || State.regime == ABSORBANCE) {
        append("_${State.polarization}-POL_^${String.format(Locale.US, "%04.1f", State.angle)}_deg")
    }
}.toString()

fun writeComputedDataTo(file: File) = StringBuilder().apply {
    val computedReal: List<Double>
    var computedImaginary: List<Double> = emptyList()

    computedReal = when (State.regime) {
        Regime.REFLECTANCE -> State.reflectance
        Regime.TRANSMITTANCE -> State.transmittance
        Regime.ABSORBANCE -> State.absorbance
        Regime.PERMITTIVITY -> {
            computedImaginary = State.permittivity.map { it.imaginary }.toList()
            State.permittivity.map { it.real }.toList()
        }
        Regime.REFRACTIVE_INDEX -> {
            computedImaginary = State.refractiveIndex.map { it.imaginary }.toList()
            State.refractiveIndex.map { it.real }.toList()
        }
        Regime.EXTINCTION_COEFFICIENT -> State.extinctionCoefficient
        Regime.SCATTERING_COEFFICIENT -> State.scatteringCoefficient
    }

    val columnSeparator = "    "
    with(computedReal) {
        indices.forEach { i ->
            append(String.format(Locale.US, "%.8f", State.wavelength[i]))
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
