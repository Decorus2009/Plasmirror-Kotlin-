package core.layers

import core.State.wavelengthCurrent
import core.AlGaAsPermittivity.n_AlGaAs
import core.Complex_
import core.EpsType
import core.EpsType.ADACHI
import core.Matrix_
import core.cosThetaInLayer
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.complex.Complex.NaN
import java.lang.Math.PI

/**
 * Abstract layer
 * @property d thickness
 * @property n refractive index
 * @property matrix transfer matrix
 */
abstract class Layer(var d: Double,
                     open var n: Complex_ = Complex_(NaN),
                     open val matrix: Matrix_ = Matrix_.emptyMatrix()) {
    abstract fun parameters(): List<Any>
}


/**
 * Abstract layer without excitons
 */
abstract class SimpleLayer(d: Double) : Layer(d) {
    /**
     * @return transfer matrix for layer without excitons
     * * Polarization is unused
     */
    override val matrix: Matrix_
        get() = Matrix_().apply {
            val cos = cosThetaInLayer(n)
            var phi = Complex_(2.0 * PI * d / wavelengthCurrent) * n * cos
            if (phi.imaginary < 0) {
                phi *= -1.0
            }
            this[0, 0] = Complex_((phi * Complex.I).exp())
            this[1, 1] = Complex_((phi * Complex.I * -1.0).exp())
            setAntiDiagonal(Complex_(Complex.ZERO))
        }
}


/**
 * GaAs
 * refractiveIndex is taken from the interpolation table for the given wavelength
 */
class GaAs(d: Double, val eps_Type: EpsType) : SimpleLayer(d) {
    override var n = Complex_(NaN)
        get() = n_AlGaAs(wavelengthCurrent, x = 0.0, eps_type = eps_Type)

    override fun parameters() = listOf(d)
}


/**
 * AlGaAs
 *
 * @param k n = (Re(n); Im(n) = k * Re(n)) Re(n) is from Adachi
 * @param x AlAs concentration
 */
class AlGaAs(d: Double, var k: Double, var x: Double, val eps_type: EpsType) : SimpleLayer(d) {
    override var n = Complex_(NaN)
        get() {
            if (eps_type == ADACHI) {
                val n_AlGaAs = n_AlGaAs(wavelengthCurrent, x, eps_type)
                return Complex_(n_AlGaAs.real, n_AlGaAs.real * k)
            }
            return n_AlGaAs(wavelengthCurrent, x, eps_type)
        }

    override fun parameters() = listOf(d, k, x)
}


/**
 * Layer with constant complex refractiveIndex
 *
 * @param const_n refractive index
 */
class ConstRefractiveIndexLayer(d: Double, private val const_n: Complex_) : SimpleLayer(d) {
    override var n = Complex_(NaN)
        get() = const_n

    override fun parameters() = listOf(d, const_n)
}