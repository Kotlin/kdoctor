package org.jetbrains.kotlin.doctor.entity

actual fun Application.getPlugin(name: String): Plugin? {
    val plistPath = "$location/Contents/Info.plist"
    if (!System.fileExists(plistPath)) return null
    val plist = System.parsePlist(plistPath) ?: return null

    val dataDirs = mutableListOf<String>()
    val dataDirectoryName = ((plist["JVMOptions"] as? Map<*, *>)
        ?.get("Properties") as? Map<*, *>)
        ?.get("idea.paths.selector")?.toString()?.trim('"')
    if (dataDirectoryName != null) {
        dataDirs.add("${System.homeDir}/Library/Application Support/Google/$dataDirectoryName")
    }
    dataDirs.add("$location")

    return dataDirs.firstNotNullOfOrNull { findPlugin(it, name) }
}

private fun findPlugin(dataDir: String, pluginName: String): Plugin? {
    val jars = pluginJars(dataDir, pluginName) ?: return null
    val pluginXml = jars.firstNotNullOfOrNull { System.readArchivedFile(it, "META-INF/plugin.xml") } ?: return null
    val version = Regex("<version>(.+?)</version>").find(pluginXml)?.groups?.get(1)?.value ?: return null
    val id = Regex("<id>(.+?)</id>").find(pluginXml)?.groups?.get(1)?.value ?: return null
    val disabledPlugins = System.readFile("$dataDir/disabled_plugins.txt")
    val isEnabled = disabledPlugins == null || !disabledPlugins.contains(id)
    return Plugin(pluginName, id, Version(version), isEnabled)
}

private fun pluginJars(dataDir: String, pluginName: String): List<String>? =
    listOf(
        "$dataDir/Contents/plugins/$pluginName/lib",
        "$dataDir/plugins/$pluginName/lib",
        "$dataDir.plugins/$pluginName/lib"
    ).firstNotNullOfOrNull { path ->
        System.execute(
            command = "find",
            path,
            "-name",
            "*.jar"
        ).output?.split("\n")
    }

actual fun appFromPath(path: String): Application? {
    val plist = System.parsePlist("$path/Contents/Info.plist") ?: return null
    val version = plist["CFBundleShortVersionString"]?.toString()?.trim('"') ?: return null
    val name = plist["CFBundleName"]?.toString()?.trim('"') ?: path.substringAfterLast("/").substringBeforeLast(".")
    return Application(name, Version(version), path)
}

actual fun Application.getEmbeddedJavaVersion(): Version? =
    listOf(
        "jre/Contents/Home",
        "jre/jdK/Contents/Home",
        "jre",
        "jbr/Contents/Home"
    ).firstNotNullOfOrNull { path ->
        System.execute("$location/Contents/$path/bin/java", "--version").output
            ?.lineSequence()
            ?.firstOrNull()
            ?.let { Version(it) }
    }

