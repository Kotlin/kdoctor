package org.jetbrains.kotlin.doctor.entity

import co.touchlab.kermit.Logger

data class Application(
    val name: String,
    val version: Version,
    val location: String? = null
)

data class Plugin(
    val id: String,
    val name: String,
    val version: Version,
    val isEnabled: Boolean
)

class AppManager(private val app: Application) {

    fun getPlugin(name: String): Plugin? {
        Logger.d("getPlugin($name)")
        val plistPath = "${app.location}/Contents/Info.plist"
        if (!System.fileExists(plistPath)) return null
        val plist = System.parsePlist(plistPath) ?: return null

        var disabledPlugins = ""
        val pluginsDir = mutableListOf<String>()
        val dataDirectoryName = ((plist["JVMOptions"] as? Map<*, *>)
            ?.get("Properties") as? Map<*, *>)
            ?.get("idea.paths.selector")?.toString()?.trim('"')
        if (dataDirectoryName != null) {
            pluginsDir.add("${System.homeDir}/Library/Application Support/Google/$dataDirectoryName/plugins")
            disabledPlugins = System.readFile(
                "${System.homeDir}/Library/Application Support/Google/$dataDirectoryName/disabled_plugins.txt"
            ).orEmpty()
        }
        pluginsDir.add("${app.location}.plugins")
        pluginsDir.add("${app.location}/Contents/plugins")

        val plugin = pluginsDir.firstNotNullOfOrNull { findPlugin(it, name, disabledPlugins) }
        Logger.d("plugin = $plugin")
        return plugin
    }

    private fun findPlugin(pluginsDir: String, pluginName: String, disabledPlugins: String): Plugin? {
        Logger.d("findPlugin($pluginName)")
        val jars = System.execute("find", "$pluginsDir/$pluginName/lib", "-name", "\"*.jar\"").output
            ?.split("\n") ?: return null
        val pluginXml = jars.firstNotNullOfOrNull { System.readArchivedFile(it, "META-INF/plugin.xml") } ?: return null
        val version = Regex("<version>(.+?)</version>").find(pluginXml)?.groups?.get(1)?.value ?: return null
        val id = Regex("<id>(.+?)</id>").find(pluginXml)?.groups?.get(1)?.value ?: return null
        val isEnabled = !disabledPlugins.contains(id)
        return Plugin(pluginName, id, Version(version), isEnabled)
    }

    fun getEmbeddedJavaVersion(): Version? =
        listOf(
            "jre/Contents/Home",
            "jbr/Contents/Home",
            "jre/jdK/Contents/Home",
            "jre"
        ).firstNotNullOfOrNull { path ->
            System.execute("${app.location}/Contents/$path/bin/java", "--version").output
                ?.lineSequence()
                ?.firstOrNull()
                ?.let { Version(it) }
        }

    companion object {
        const val KOTLIN_PLUGIN = "Kotlin"
        const val KMM_PLUGIN = "kmm"
    }
}