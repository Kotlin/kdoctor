package org.jetbrains.kotlin.doctor.entity

//input: [[@], [a, b], [1, 2], [#]]
//output: [[@, a, 1, #], [@, a, 2, #], [@, b, 1, #], [@, b, 2, #]]
fun <T> allCombinations(input: List<List<T>>): List<List<T>> {
    val list = input.filter { it.isNotEmpty() }
    return when {
        list.isEmpty() -> emptyList()
        list.size == 1 -> list.first().map { listOf(it) }
        else -> {
            val head: List<T> = list.first()
            val tail: List<List<T>> = list.drop(1)
            val tailResult = allCombinations(tail)
            head.flatMap { h ->
                tailResult.map { j -> listOf(h) + j }
            }
        }
    }
}