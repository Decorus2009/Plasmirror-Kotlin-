package core.optics

import core.Complex_

object Optics {
    fun extinctionCoefficient(wavelength: Double, refractiveIndex: Complex_) =
            4.0 * Math.PI * refractiveIndex.imaginary / (wavelength * 1E-7) // cm^-1

//    fun toRefractiveIndex(eps: Complex_): Complex_ {
//        val n = Math.sqrt((eps.abs() + eps.real) / 2.0)
//        val k = Math.sqrt((eps.abs() - eps.real) / 2.0)
//        return Complex_(n, k)
//    }

    fun toRefractiveIndex(eps: Complex_) = with(eps) {
        Complex_(Math.sqrt((abs() + real) / 2.0), Math.sqrt((abs() - real) / 2.0))
    }
}