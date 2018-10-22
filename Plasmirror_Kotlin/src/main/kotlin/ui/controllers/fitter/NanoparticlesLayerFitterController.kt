package ui.controllers.fitter

import core.layers.Layer
import core.layers.PerssonModelForDrudeMetalClustersInAlGaAs
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import java.util.*


class NanoparticlesLayerFitterController : LayerFitterController() {

    @FXML private lateinit var enableCheckBox: CheckBox
    @FXML private lateinit var gridPane: GridPane
    @FXML private lateinit var d_valueTextField: TextField
    @FXML private lateinit var d_fromTextField: TextField
    @FXML private lateinit var d_toTextField: TextField
    @FXML private lateinit var d_stepTextField: TextField
    @FXML private lateinit var k_valueTextField: TextField
    @FXML private lateinit var k_fromTextField: TextField
    @FXML private lateinit var k_toTextField: TextField
    @FXML private lateinit var k_stepTextField: TextField
    @FXML private lateinit var x_valueTextField: TextField
    @FXML private lateinit var x_fromTextField: TextField
    @FXML private lateinit var x_toTextField: TextField
    @FXML private lateinit var x_stepTextField: TextField
    @FXML private lateinit var latticeFactor_valueTextField: TextField
    @FXML private lateinit var latticeFactor_fromTextField: TextField
    @FXML private lateinit var latticeFactor_toTextField: TextField
    @FXML private lateinit var latticeFactor_stepTextField: TextField
    @FXML private lateinit var w_plasma_valueTextField: TextField
    @FXML private lateinit var w_plasma_fromTextField: TextField
    @FXML private lateinit var w_plasma_toTextField: TextField
    @FXML private lateinit var w_plasma_stepTextField: TextField
    @FXML private lateinit var gamma_plasma_valueTextField: TextField
    @FXML private lateinit var gamma_plasma_fromTextField: TextField
    @FXML private lateinit var gamma_plasma_toTextField: TextField
    @FXML private lateinit var gamma_plasma_stepTextField: TextField
    @FXML private lateinit var eps_inf_valueTextField: TextField
    @FXML private lateinit var eps_inf_fromTextField: TextField
    @FXML private lateinit var eps_inf_toTextField: TextField
    @FXML private lateinit var eps_inf_stepTextField: TextField

    override lateinit var layer: Layer
    override lateinit var mainFitterController: MainFitterController

    @FXML
    fun initialize() {
        enableCheckBox.setOnAction {
            gridPane.isDisable = enableCheckBox.isSelected.not()
            mainFitterController.layersToFitterControllers.values.filterNot { it === this }.forEach {
                it.disable()
            }
        }
        Platform.runLater {
            with(layer as PerssonModelForDrudeMetalClustersInAlGaAs) {
                d.let {
                    d_valueTextField.text = String.format(Locale.US, "%.2f", it)
                    d_fromTextField.text = String.format(Locale.US, "%.2f", it * 0.5)
                    d_toTextField.text = String.format(Locale.US, "%.2f", it * 1.5)
                    d_stepTextField.text = String.format(Locale.US, "%.2f", it / 100.0)
                }
                k.let {
                    k_valueTextField.text = String.format(Locale.US, "%.2f", it)
                    k_fromTextField.text = String.format(Locale.US, "%.2f", it * 0.5)
                    k_toTextField.text = String.format(Locale.US, "%.2f", it * 1.5)
                    k_stepTextField.text = String.format(Locale.US, "%.2f", it / 100.0)
                }
                x.let {
                    x_valueTextField.text = String.format(Locale.US, "%.2f", it)
                    x_fromTextField.text = String.format(Locale.US, "%.2f", it * 0.5)
                    x_toTextField.text = String.format(Locale.US, "%.2f", it * 1.5)
                    x_stepTextField.text = String.format(Locale.US, "%.2f", it / 100.0)
                }
                latticeFactor.let {
                    latticeFactor_valueTextField.text = String.format(Locale.US, "%.2f", it)
                    latticeFactor_fromTextField.text = String.format(Locale.US, "%.2f", it * 0.5)
                    latticeFactor_toTextField.text = String.format(Locale.US, "%.2f", it * 1.5)
                    latticeFactor_stepTextField.text = String.format(Locale.US, "%.2f", it / 100.0)
                }
                wPlasma.let {
                    w_plasma_valueTextField.text = String.format(Locale.US, "%.2f", it)
                    w_plasma_fromTextField.text = String.format(Locale.US, "%.2f", it * 0.5)
                    w_plasma_toTextField.text = String.format(Locale.US, "%.2f", it * 1.5)
                    w_plasma_stepTextField.text = String.format(Locale.US, "%.2f", it / 100.0)
                }
                gammaPlasma.let {
                    gamma_plasma_valueTextField.text = String.format(Locale.US, "%.2f", it)
                    gamma_plasma_fromTextField.text = String.format(Locale.US, "%.2f", it * 0.5)
                    gamma_plasma_toTextField.text = String.format(Locale.US, "%.2f", it * 1.5)
                    gamma_plasma_stepTextField.text = String.format(Locale.US, "%.2f", it / 100.0)
                }
                epsInf.let {
                    eps_inf_valueTextField.text = String.format(Locale.US, "%.2f", it)
                    eps_inf_fromTextField.text = String.format(Locale.US, "%.2f", it * 0.5)
                    eps_inf_toTextField.text = String.format(Locale.US, "%.2f", it * 1.5)
                    eps_inf_stepTextField.text = String.format(Locale.US, "%.2f", it / 100.0)
                }
            }
        }

    }

    override fun fit() {
        // fitting procedure is slow and ineffective
        TODO()
/*
        println("Fitting Nanoparticles layer")

        val d_from = d_fromTextField.text.toDouble()
        val d_to = d_toTextField.text.toDouble()
        val d_step = d_stepTextField.text.toDouble()

        val k_from = k_fromTextField.text.toDouble()
        val k_to = k_toTextField.text.toDouble()
        val k_step = k_stepTextField.text.toDouble()

        val x_from = x_fromTextField.text.toDouble()
        val x_to = x_toTextField.text.toDouble()
        val x_step = x_stepTextField.text.toDouble()

        val latticeFactor_from = latticeFactor_fromTextField.text.toDouble()
        val latticeFactor_to = latticeFactor_toTextField.text.toDouble()
        val latticeFactor_step = latticeFactor_stepTextField.text.toDouble()

        val w_plasma_from = w_plasma_fromTextField.text.toDouble()
        val w_plasma_to = w_plasma_toTextField.text.toDouble()
        val w_plasma_step = w_plasma_stepTextField.text.toDouble()

        val gamma_plasma_from = gamma_plasma_fromTextField.text.toDouble()
        val gamma_plasma_to = gamma_plasma_toTextField.text.toDouble()
        val gamma_plasma_step = gamma_plasma_stepTextField.text.toDouble()

        val eps_inf_from = eps_inf_fromTextField.text.toDouble()
        val eps_inf_to = eps_inf_toTextField.text.toDouble()
        val eps_inf_step = eps_inf_stepTextField.text.toDouble()

        with(layer as PerssonModelForDrudeMetalClustersInAlGaAs) {
            var d = d_from
            while (d <= d_to) {
                this.d = d

                var k = k_from
                while (k <= k_to) {
                    this.k = k

                    var x = x_from
                    while (x <= x_to) {
                        this.x = x

                        var latticeFactor = latticeFactor_from
                        while (latticeFactor <= latticeFactor_to) {
                            this.latticeFactor = latticeFactor

                            var w_plasma = w_plasma_from
                            while (w_plasma <= w_plasma_to) {
                                this.w_plasma = w_plasma

                                var gamma_plasma = gamma_plasma_from
                                while (gamma_plasma <= gamma_plasma_to) {
                                    this.gamma_plasma = gamma_plasma

                                    var eps_inf = eps_inf_from
                                    while (eps_inf <= eps_inf_to) {
                                        this.eps_inf = eps_inf

//                                        println("d = $d k = $k x = $x latticeFactor = $latticeFactor wPlasma = $wPlasma gammaPlasma = $gammaPlasma epsInf = $epsInf")
                                        with(FitterState) {
                                            clearPrevious()
                                            compute()
                                            checkDifference()
                                        }

                                        eps_inf += eps_inf_step
                                    }
                                    gamma_plasma += gamma_plasma_step
                                }
                                w_plasma += w_plasma_step
                            }
                            latticeFactor += latticeFactor_step
                        }
                        x += x_step
                    }
                    k += k_step
                }
                d += d_step
            }
        }
        println("Fitter values: ${FitterState.listOfParameters}")
*/
    }

    override fun enable() {
        enableCheckBox.isSelected = true
        gridPane.isDisable = false
    }

    override fun disable() {
        enableCheckBox.isSelected = false
        gridPane.isDisable = true
    }

    override val selected: Boolean
        get() = enableCheckBox.isSelected
}