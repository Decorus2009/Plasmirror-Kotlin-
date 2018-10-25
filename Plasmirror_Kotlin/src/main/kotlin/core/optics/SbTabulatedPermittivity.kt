package core.optics

import core.Complex_
import core.Interpolator
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

//object SbTabulatedPermittivity {
//
//    private val functions: Pair<PolynomialSplineFunction, PolynomialSplineFunction>
//    private val path = Paths.get("data/inner/state_parameters/eps_Sb_Cardona_Adachi.txt")
//    private val wavelengths = mutableListOf<Double>()
//    private val epsSb = mutableListOf<Complex_>()
//
//    init {
//        println("Init in SbTabulatedPermittivity")
//        read()
//        functions = interpolate()
//    }
//
//    fun get(wavelength: Double) = with(functions) {
//        if (wavelengths.isEmpty() or epsSb.isEmpty()) {
//            throw IllegalStateException("Empty array of Sb permittivity")
//        }
//
//        val minWavelength = wavelengths[0]
//        val maxWavelength = wavelengths[wavelengths.size - 1]
//        val actualWavelength =
//                if (wavelength < minWavelength) minWavelength
//                else if (wavelength > maxWavelength) maxWavelength
//                else wavelength
//
//        Complex_(first.value(actualWavelength), second.value(actualWavelength))
//    }
//
//    private fun read() = Files.lines(path).forEach {
//        with(Scanner(it).useLocale(Locale.ENGLISH)) {
//            wavelengths += nextDouble()
//            epsSb += Complex_(nextDouble(), nextDouble())
//        }
//    }
//
//    private fun interpolate() = Interpolator.interpolateComplex(wavelengths, epsSb)
//}
