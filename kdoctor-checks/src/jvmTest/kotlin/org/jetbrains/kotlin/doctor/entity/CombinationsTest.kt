package org.jetbrains.kotlin.doctor.entity

import kotlin.test.Test
import kotlin.test.assertEquals

class CombinationsTest {

    @Test
    fun `check all combinations`() {
        val input = listOf(
            listOf("@"),
            listOf("a", "b"),
            listOf("1", "2"),
            listOf("#"),
        )
        val expected = listOf(
            listOf("@", "a", "1", "#"),
            listOf("@", "a", "2", "#"),
            listOf("@", "b", "1", "#"),
            listOf("@", "b", "2", "#"),
        )
        val actual = allCombinations(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `check all combinations with nulls`() {
        val input = listOf(
            listOf("@"),
            listOf("a", "b"),
            listOf(null),
            listOf("1", "2"),
            listOf("#"),
        )
        val expected = listOf(
            listOf("@", "a", null, "1", "#"),
            listOf("@", "a", null, "2", "#"),
            listOf("@", "b", null, "1", "#"),
            listOf("@", "b", null, "2", "#"),
        )
        val actual = allCombinations(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `check filter empty`() {
        val input = listOf(
            listOf("@"),
            listOf(),
            listOf("a", "b"),
        )
        val expected = listOf(
            listOf("@", "a"),
            listOf("@", "b"),
        )
        val actual = allCombinations(input)
        assertEquals(expected, actual)
    }
}