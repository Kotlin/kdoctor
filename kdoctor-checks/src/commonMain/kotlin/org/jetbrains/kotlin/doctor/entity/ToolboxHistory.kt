package org.jetbrains.kotlin.doctor.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ToolboxItem(
    @SerialName("build") val build: String
)

@Serializable
data class ToolboxHistoryEntry(
    @SerialName("item") val item: ToolboxItem,
    @SerialName("timestamp") val timestamp: String
)

@Serializable
data class ToolboxHistory(
    @SerialName("history") val history: List<ToolboxHistoryEntry>
)