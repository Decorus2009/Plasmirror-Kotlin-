
import core.Complex_
import core.State
import ui.controllers.fitter.FitterState
import java.util.regex.Pattern
import java.util.regex.Pattern.CASE_INSENSITIVE

fun main(args: Array<String>) {
    val a = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
    val b = listOf(11.0, 12.0, 13.0, 14.0, 16.0)

    println(a.zip(b).sumByDouble { Math.abs(it.first - it.second) })
}

