package org.jetbrains.kotlin.doctor.entity

open class Application(
    val name: String,
    val version: Version,
    val location: String? = null
)
expect fun appFromPath(path: String): Application?

data class Plugin(
    val id: String,
    val name: String,
    val version: Version,
    val isEnabled: Boolean
)
expect fun Application.getPlugin(name: String): Plugin?
expect fun Application.getEmbeddedJavaVersion(): Version?

fun Application.getKotlinPlugin() = getPlugin("Kotlin")
fun Application.getKmmPlugin() = getPlugin("kmm")