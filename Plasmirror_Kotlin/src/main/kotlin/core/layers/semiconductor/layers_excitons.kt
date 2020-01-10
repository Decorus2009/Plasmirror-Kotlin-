package core.layers.semiconductor

import core.*
import core.optics.*
import core.optics.Polarization.P
import org.apache.commons.math3.complex.Complex.I
import java.lang.Math.PI

// TODO absorption coefficient value override taking into account excitonic response
/**
 * Abstract layer with excitons
 *
 * @param w0     exciton resonance frequency
 * @param gamma0 exciton radiative decay
 * @param gamma  exciton non-radiative decay
 */
interface LayerExcitonic : Layer {
  val w0: Double
  val gamma0: Double
  val gamma: Double

  override val matrix
    get() = Matrix_().apply {
      val cos = cosThetaInLayer(n)
      /* TODO проверить поляризацию (в VisMirror (GaAs) было S) */
      val gamma0e = when (State.polarization) {
        P -> gamma0 * cos.real
        else -> gamma0 * (cos.pow(-1.0)).real
      }
      val phi = Complex_(2.0 * PI * d / State.wavelengthCurrent) * n * cos
      val S = Complex_(gamma0e) / Complex_(State.wavelengthCurrent.toEnergy() - w0, gamma)

      this[0, 0] = Complex_((phi * I).exp()) * Complex_(1.0 + S.imaginary, -S.real)
      this[0, 1] = Complex_(S.imaginary, -S.real)
      this[1, 0] = Complex_(-S.imaginary, S.real)
      this[1, 1] = Complex_((phi * I * -1.0).exp()) * Complex_(1.0 - S.imaginary, S.real)
    }
}

class GaAsExcitonic(
  d: Double,
  override val w0: Double,
  override val gamma0: Double,
  override val gamma: Double,
  epsType: EpsType
) : LayerExcitonic, GaAs(d, epsType)

class AlGaAsExcitonic(
  d: Double,
  k: Double,
  x: Double,
  override val w0: Double,
  override val gamma0: Double,
  override val gamma: Double,
  epsType: EpsType
) : LayerExcitonic, AlGaAs(d, k, x, epsType)

class ConstRefractiveIndexLayerExcitonic(
  d: Double,
  n: Complex_,
  override val w0: Double,
  override val gamma0: Double,
  override val gamma: Double
) : LayerExcitonic, ConstRefractiveIndexLayer(d, n)
