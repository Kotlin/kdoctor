plugins {
    kotlin("multiplatform").version("1.9.10")
}


kotlin {
    targetHierarchy.default()
    jvm() //jvm doesn't require Android SDK

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
        }
    }
}
