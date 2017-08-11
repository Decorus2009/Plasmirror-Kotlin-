/**
 * Created by decorus on 09.08.17.
 */

data class A(val list: MutableList<Int>)

fun main(args: Array<String>) {
    val a = A(mutableListOf(1, 2, 3, 4, 5, 6))
    val b = A(a.list.map { it }.toMutableList())

    println(a.list); println(b.list)
    a.list.removeAt(0)
    a.list.removeAt(1)
    a.list.removeAt(2)
    a.list[0] = 666
    println(a.list); println(b.list)
}