package core.util

import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.complex.ComplexField
import org.apache.commons.math3.linear.Array2DRowFieldMatrix
import org.apache.commons.math3.linear.FieldMatrix

class Cmplx(real: Double, imaginary: Double) : Complex(real, imaginary) {

    constructor(real: Double) : this(real, 0.0)
    constructor(complex: Complex) : this(complex.real, complex.imaginary)

    operator fun plus(that: Complex?) = Cmplx(this.add(that!!))
    operator fun plus(that: Double) = Cmplx(this.add(that))
    operator fun plus(that: Cmplx) = Cmplx(this.add(that))

    operator fun minus(that: Complex?) = Cmplx(this.subtract(that!!))
    operator fun minus(that: Double) = Cmplx(this.subtract(that))
    operator fun minus(that: Cmplx) = Cmplx(this.subtract(that))

    operator fun times(that: Complex?) = Cmplx(this.multiply(that!!))
    operator fun times(that: Double) = Cmplx(this.multiply(that))
    operator fun times(that: Cmplx) = Cmplx(this.multiply(that))

    operator fun div(that: Complex?) = Cmplx(this.divide(that!!))
    operator fun div(that: Double) = Cmplx(this.divide(that))
    operator fun div(that: Cmplx) = Cmplx(this.divide(that))
}


/**
 * 2 x 2 matrix of complex numbers
 */
class Mtrx(val matrix: FieldMatrix<Complex> = Array2DRowFieldMatrix(ComplexField.getInstance(), 2, 2)) {

    operator fun get(i: Int, j: Int): Cmplx = Cmplx(matrix.getEntry(i, j))

    operator fun set(i: Int, j: Int, value: Cmplx) = matrix.setEntry(i, j, value)

    fun setDiagonal(value: Cmplx) {
        matrix.setEntry(0, 0, value)
        matrix.setEntry(1, 1, value)
    }

    fun setAntiDiagonal(value: Cmplx) {
        matrix.setEntry(0, 1, value)
        matrix.setEntry(1, 0, value)
    }

    operator fun times(that: Mtrx) = Mtrx(matrix.multiply(that.matrix))

    fun pow(value: Int) = Mtrx(matrix.power(value))

    fun det(): Cmplx {

        val diagMultiplied = matrix
                .getEntry(0, 0)
                .multiply(matrix.getEntry(1, 1))

        val antiDiagMultiplied = matrix
                .getEntry(0, 1)
                .multiply(matrix.getEntry(1, 0))

        return Cmplx(diagMultiplied.subtract(antiDiagMultiplied))
    }

    companion object {
        fun emptyMatrix(): Mtrx {
            val empty = Mtrx()
            empty.setDiagonal(Cmplx(Complex.NaN))
            empty.setAntiDiagonal(Cmplx(Complex.NaN))
            return empty
        }

        fun unaryMatrix(): Mtrx {
            val unary = Mtrx()
            unary.setDiagonal(Cmplx(Complex.ONE))
            unary.setAntiDiagonal(Cmplx(Complex.ZERO))
            return unary
        }
    }
}
