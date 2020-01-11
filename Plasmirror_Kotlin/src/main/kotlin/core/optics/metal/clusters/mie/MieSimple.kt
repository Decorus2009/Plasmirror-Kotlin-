package core.optics.metal.clusters.mie

import core.Complex_

abstract class MieSimple : Mie {
  abstract fun a(): List<Complex_>
  abstract fun b(): List<Complex_>

  override fun scatteringCoefficient(wavelength: Double, epsSemiconductor: Complex_, epsMetal: Complex_, f: Double, r: Double): Double {
    a().forEach {  }
    b().forEach {  }
//    sum a and b coefficients (Bohren 103) using a() and b()
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun extinctionCoefficient(wavelength: Double, epsSemiconductor: Complex_, epsMetal: Complex_, f: Double, r: Double): Double {
    a().forEach {  }
    b().forEach {  }
//    sum a and b coefficients (Bohren 103)
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}

open class MieFirstOrder : MieSimple() {
  protected val a1: Complex_
    get() {
      return Complex_.ONE
    }

  protected val b1: Complex_
    get() {
      return Complex_.ONE
    }

  override fun a() = listOf(a1)
  override fun b() = listOf(b1)
}

class MieFirstAndSecondOrder : MieFirstOrder() {
  private val a2: Complex_
    get() {
      TODO()
    }

  override fun a() = listOf(a1, a2)
  override fun b() = listOf(b1)
}