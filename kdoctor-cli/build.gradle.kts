@file:Suppress("OPT_IN_USAGE")

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        mainRun {
            mainClass.set("org.jetbrains.kotlin.doctor.MainKt")
        }
    }

    listOf(macosX64(), macosArm64()).forEach {
        it.binaries {
            executable {
                entryPoint = "org.jetbrains.kotlin.doctor.main"
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kdoctor-checks"))
                implementation("com.github.ajalt.clikt:clikt:4.2.2")
                implementation("co.touchlab:kermit:2.0.3")
            }
        }
    }
}

tasks {
    val assembleReleaseExecutableMacos by registering(Exec::class) {
        dependsOn(":kdoctor-checks:jvmTest", ":kdoctor-cli:linkReleaseExecutableMacosX64", ":kdoctor-cli:linkReleaseExecutableMacosArm64")
        commandLine("lipo", "-create", "-output", "kdoctor", "bin/macosX64/releaseExecutable/kdoctor-cli.kexe", "bin/macosArm64/releaseExecutable/kdoctor-cli.kexe")
        workingDir = buildDir
        group = "Build"
        description = "Builds universal macOS binary"
    }
    assemble {
        dependsOn(assembleReleaseExecutableMacos)
    }
}
