plugins {
    kotlin("multiplatform") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    targetHierarchy.default()
    jvm() //for unit tests
    listOf(macosX64(), macosArm64()).forEach {
        it.binaries {
            executable {
                entryPoint = "org.jetbrains.kotlin.doctor.main"
            }
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("com.github.ajalt.clikt:clikt:4.2.1")
                implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation ("co.touchlab:kermit:2.0.1")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.register<Exec>("assembleReleaseExecutableMacos") {
    dependsOn(":kdoctor:jvmTest", ":kdoctor:linkReleaseExecutableMacosX64", ":kdoctor:linkReleaseExecutableMacosArm64")
    commandLine("lipo", "-create", "-output", "kdoctor", "bin/macosX64/releaseExecutable/kdoctor.kexe", "bin/macosArm64/releaseExecutable/kdoctor.kexe")
    workingDir = buildDir
    group = "Build"
    description = "Builds universal macOS binary"
}