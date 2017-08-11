package core

import core.util.*
import core.util.Regime.*
import core.util.Validator.validateAndSetStateUsing
import ui.controllers.MainController
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.*

object State {

    @JvmStatic lateinit var mainController: MainController
    @JvmStatic val GaAs_n = mutableMapOf<Double, Cmplx>()

    @JvmStatic var wlStart: Double = 0.0
    @JvmStatic var wlEnd: Double = 0.0
    @JvmStatic var wlStep: Double = 0.0
    @JvmStatic var wlCurrent: Double = 0.0

    @JvmStatic var angle: Double = 0.0
    @JvmStatic lateinit var polarization: Polarization
    @JvmStatic lateinit var regime: Regime

    @JvmStatic lateinit var leftMedium: Medium
    @JvmStatic lateinit var rightMedium: Medium

    @JvmStatic lateinit var n_left: Cmplx
    @JvmStatic lateinit var n_right: Cmplx

    @JvmStatic lateinit var structure: Structure

    @JvmStatic lateinit var mirror: Mirror

    @JvmStatic var wl = mutableListOf<Double>()
    @JvmStatic val reflection = mutableListOf<Double>()
    @JvmStatic val transmission = mutableListOf<Double>()
    @JvmStatic val absorption = mutableListOf<Double>()
    @JvmStatic val permittivity = mutableListOf<Cmplx>()
    @JvmStatic val refractiveIndex = mutableListOf<Cmplx>()

    fun set() {
        println("State set")

        clearPreviousComputation()
        /* Reading n GaAs file only once */
        if (GaAs_n.isEmpty()) {
            readGaAsRefractiveIndex()
        }
        validateAndSetStateUsing(mainController)

        buildMirror()
    }

    private fun clearPreviousComputation() {

        fun <T> clearIfNotEmpty(vararg lists: MutableList<out T>) = lists.forEach { it.run { if (isNotEmpty()) clear() } }

        clearIfNotEmpty(reflection, transmission, absorption)
        clearIfNotEmpty(permittivity, refractiveIndex)

        /*
        TODO Doesn't clear when using this form of extension function (without "run")
        fun <T> MutableList<out T>.clearIfNotEmpty() = run { if (isNotEmpty()) clear() } // works
        fun <T> MutableList<T>.clearIfNotEmpty() = { if (isNotEmpty()) clear() } // doesn't work
        reflection.clearIfNotEmpty()
        transmission.clearIfNotEmpty()
        absorption.clearIfNotEmpty()
        permittivity.clearIfNotEmpty()
        refractiveIndex.clearIfNotEmpty()
         */
    }

    fun compute() {
        val size = ((wlEnd - wlStart) / wlStep).toInt() + 1
        wl = ArrayList<Double>(size)

        // TODO MERGE TWO LOOPS, CHECK PERFORMANCE
        (0..size - 1).forEach { wl.add(wlStart + it * wlStep) }


        // TODO PARALLEL
        (0..wl.size - 1).forEach {
            wlCurrent = wl[it]

            mirror.run {
                when (regime) {
                    R -> reflection += computeReflection()
                    T -> transmission += computeTransmission()
                    A -> absorption += computeAbsorption()
                    EPS -> permittivity += computePermittivity()
                    N -> refractiveIndex += computeRefractiveIndex()
                }
            }
        }

//        println("${wl[0]} ${reflection[0]}")
//        for (i in 0..wl.size - 1) println("${wl[i]} ${reflection[i]}")
    }

    fun printState() {
        println("$wlStart $wlEnd $wlStep\n $polarization $angle\n $leftMedium $rightMedium\n")
    }


    private fun buildMirror() {
        mirror = MirrorBuilder.build(structure, leftMedium, rightMedium)
    }


    // TODO use new AlxGa1-xAs calculation for x = 0. It's unknown whether I can trust the experimental data
    private fun readGaAsRefractiveIndex() {
        val data = Paths.get(".${File.separator}data${File.separator}inner${File.separator}state_parameters${File.separator}GaAs_n_240_840nm_interpolated.txt")

        try {
            Scanner(data).useLocale(Locale.US).use { scanner ->
                with(scanner) {
                    while (hasNextDouble()) {
                        val wl = nextDouble()
                        val n = nextDouble()
                        val k = nextDouble()
                        GaAs_n.put(wl, Cmplx(n, k))
                    }
                }
            }
        } catch (e: IOException) {
            throw StateException("IOException caught while reading GaAs_n interpolated data")
        }
    }
}


/*

    private fun readStateParameters() {

        readRegime()
        readMediumParameters()
        readLightParameters()
        readCalculationRange()
        readStructureDescription()
    }

    private fun readRegime() {
        /* lines.size should be == 1 */
        val path = Paths.get(".${File.separator}data${File.separator}inner${File.separator}state_parameters${File.separator}regime.txt")
        val lines = readFromFile(path)
        regime = Regime.valueOf(lines[0])
    }

    private fun readMediumParameters() {
        leftMedium = AIR
        rightMedium = GAAS
    }

    private fun readLightParameters() {
        /* lines.size should be == 2 */
        val path = Paths.get(".${File.separator}data${File.separator}inner${File.separator}state_parameters${File.separator}light_parameters.txt")
        val lines = readFromFile(path)

        polarization = Polarization.valueOf(lines[0])
        angle = lines[1].toDouble()
    }

    private fun readCalculationRange() {
        /* lines.size should be == 3 */
        val path = Paths.get(".${File.separator}data${File.separator}inner${File.separator}state_parameters${File.separator}computation_range.txt")
        val lines = readFromFile(path)

        wlStart = lines[0].toDouble()
        wlEnd = lines[1].toDouble()
        wlStep = lines[2].toDouble()
    }

    private fun readStructureDescription() {
        val path = Paths.get(".${File.separator}data${File.separator}inner${File.separator}state_parameters${File.separator}structure.txt")
        val lines = readFromFile(path)

        val structureDescription = StringBuilder()
        lines.forEach { structureDescription.append(it + "\refractiveIndex") }

        structure = StructureBuilder.build(StructureValidator.tokenize(structureDescription.toString()))
        structureDescriptionStringRepresentation = structureDescription.toString()
    }

    private fun readStructureDescription(structureDescription: StructureDescription) {

        structure = StructureBuilder.build(StructureValidator.tokenize(structureDescription.toString()))
        structureDescriptionStringRepresentation = structureDescription.toString()
    }

    private fun readGaAsRefractiveIndex() {
        val data = Paths.get(".${File.separator}data${File.separator}inner${File.separator}state_parameters${File.separator}GaAs_n_240_840nm_interpolated.txt")

        try {
            Scanner(data).useLocale(Locale.US).use { scanner ->
                with (scanner) {
                    while (hasNextDouble()) {
                        val wl = nextDouble()
                        val refractiveIndex = nextDouble()
                        val k = nextDouble()

                        GaAs_n.put(wl, Cmplx(refractiveIndex, k))
                    }
                }
            }
        } catch (e: IOException) {
            throw StateException("IOException caught while reading GaAs_n interpolated data")
        }
    }


 */