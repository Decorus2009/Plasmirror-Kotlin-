package core.util

import core.util.Regime.*
import org.apache.commons.math3.complex.Complex
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import core.State
import core.State.angle
import core.State.polarization
import core.State.regime

fun writeToFile(path: Path, text: String) {
    try {
        Files.newBufferedWriter(path, StandardCharsets.UTF_8).use { writer -> writer.write(text) }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun readFromFile(path: Path): List<String> {

    var lines: List<String> = ArrayList()
    try {
        lines = Files.readAllLines(path, StandardCharsets.UTF_8)
    } catch (e: IOException) {
        e.printStackTrace()
    }

    return lines
}

/*
Запись результата расчета в файл.
Перед таблицей предварительно помещается описание структуры, structureInfo.
 */
fun saveCalcResult(file: File?, structureInfo: String?, regime: Regime) {

    // если файл выбран не был или экспорт отменен
    if (file == null) {
        return
    }

    try {
        FileWriter(file).use { writer ->

            if (structureInfo != null) {
                writer.write(structureInfo)
            }
            if (regime == R || regime == T || regime == A) {
                var calculatedData: List<Double>? = null
                if (regime == R) {
                    calculatedData = State.reflection
                } else if (regime == T) {
                    calculatedData = State.transmission
                } else if (regime == A) {
                    calculatedData = State.absorption
                }

                for (i in 0..State.wavelength.size - 1) {
                    writer.write(
                            String.format(Locale.US, "%.8f", State.wavelength.get(i))
                                    + "\t"
                                    + String.format(Locale.US, "%.8f", calculatedData!![i])
                                    + "\n"
                    )
                }
            } else if (regime == EPS || regime == N) {
                var calculatedData: List<Complex>? = null

                if (regime == EPS) {
                    calculatedData = State.permittivity
                } else if (regime == N) {
                    calculatedData = State.refractiveIndex
                }

                for (i in 0..State.wavelength.size - 1) {
                    writer.write(
                            String.format(Locale.US, "%.8f", State.wavelength[i])
                                    + "\t"
                                    + String.format(Locale.US, "%.8f", calculatedData!![i].real)
                                    + "\t"
                                    + String.format(Locale.US, "%.8f", calculatedData[i].imaginary)
                                    + "\n"
                    )
                }
            }
        }
    } catch (e: IOException) {
        println("Error while opening file for writing calculation data")
        e.printStackTrace()
    }

}

fun buildExportFileName(): String {

    val name = StringBuilder()

    name
            .append("calculation_")
            .append(regime)
            .append("_")
            .append(State.wavelengthFrom)
            .append("_")
            .append(State.wavelengthTo)

    if (regime == R || regime == T || regime == A) {

        name
                .append("_")
                .append(polarization)
                .append("-POL")
                .append("_^")
                .append(String.format("%04.1f", angle))
                .append("_deg")
    }
    return name.toString()
}