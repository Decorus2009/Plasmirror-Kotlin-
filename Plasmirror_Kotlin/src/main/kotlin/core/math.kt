package core

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.complex.Complex.*
import org.apache.commons.math3.complex.ComplexField
import org.apache.commons.math3.linear.Array2DRowFieldMatrix
import org.apache.commons.math3.linear.FieldMatrix
import kotlin.math.floor
import kotlin.math.pow

class Complex_(real: Double, imaginary: Double) : Complex(real, imaginary) {

  companion object {
    val I = Complex_(0.0, 1.0)
    val ONE = Complex_(1.0, 0.0)
    val ZERO = Complex_(0.0, 0.0)
    val NaN = Complex_(Complex.NaN)
  }

  constructor(real: Double) : this(real, 0.0)
  constructor(complex: Complex) : this(complex.real, complex.imaginary)

  operator fun plus(that: Complex?) = Complex_(this.add(that!!))
  operator fun plus(that: Double) = Complex_(this.add(that))
  operator fun plus(that: Complex_) = Complex_(this.add(that))

  operator fun minus(that: Complex?) = Complex_(this.subtract(that!!))
  operator fun minus(that: Double) = Complex_(this.subtract(that))
  operator fun minus(that: Complex_) = Complex_(this.subtract(that))

  operator fun times(that: Complex?) = Complex_(this.multiply(that!!))
  operator fun times(that: Double) = Complex_(this.multiply(that))
  operator fun times(that: Complex_) = Complex_(this.multiply(that))

  operator fun div(that: Complex?) = Complex_(this.divide(that!!))
  operator fun div(that: Double) = Complex_(this.divide(that))
  operator fun div(that: Complex_) = Complex_(this.divide(that))

  operator fun unaryMinus() = Complex_(-real, -imaginary)
}


/**
 * 2 x 2 matrix of complex numbers
 */
class Matrix_(private val matrix: FieldMatrix<Complex> = Array2DRowFieldMatrix(ComplexField.getInstance(), 2, 2)) {

  operator fun get(i: Int, j: Int): Complex_ = Complex_(matrix.getEntry(i, j))

  operator fun set(i: Int, j: Int, value: Complex_) = matrix.setEntry(i, j, value)

  fun setDiagonal(value: Complex_) {
    matrix.setEntry(0, 0, value)
    matrix.setEntry(1, 1, value)
  }

  fun setAntiDiagonal(value: Complex_) {
    matrix.setEntry(0, 1, value)
    matrix.setEntry(1, 0, value)
  }

  operator fun times(that: Matrix_) = Matrix_(matrix.multiply(that.matrix))
  operator fun times(that: Complex_) = Matrix_.apply {
    set(0, 0, get(0, 0) * that)
    set(0, 1, get(0, 1) * that)
    set(1, 0, get(1, 0) * that)
    set(1, 1, get(1, 1) * that)
  }

  operator fun div(that: Complex_) = Matrix_.apply {
    set(0, 0, get(0, 0) / that)
    set(0, 1, get(0, 1) / that)
    set(1, 0, get(1, 0) / that)
    set(1, 1, get(1, 1) / that)
  }

  fun pow(value: Int) = Matrix_(matrix.power(value))

  fun det(): Complex_ {
    val diagMultiplied = matrix.getEntry(0, 0).multiply(matrix.getEntry(1, 1))
    val antiDiagMultiplied = matrix.getEntry(0, 1).multiply(matrix.getEntry(1, 0))
    return Complex_(diagMultiplied.subtract(antiDiagMultiplied))
  }

  companion object {
    fun emptyMatrix() = Matrix_().apply {
      setDiagonal(Complex_(NaN))
      setAntiDiagonal(Complex_(NaN))
    }

    fun unaryMatrix() = Matrix_().apply {
      setDiagonal(Complex_(ONE))
      setAntiDiagonal(Complex_(ZERO))
    }
  }
}

object Interpolator {
  fun interpolateComplex(x: List<Double>, y: List<Complex_>): Pair<PolynomialSplineFunction, PolynomialSplineFunction> {
    with(LinearInterpolator()) {
      val functionReal = interpolate(x.toDoubleArray(), y.map { it.real }.toDoubleArray())
      val functionImag = interpolate(x.toDoubleArray(), y.map { it.imaginary }.toDoubleArray())
      return functionReal to functionImag
    }
  }
}

fun Double.round(): Double {
  val precision = 7.0
  val power = 10.0.pow(precision).toInt()
  return floor((this + 1E-8) * power) / power
}

fun Double.toCm() = this * 1E-7 // wavelength: nm -> cm^-1
