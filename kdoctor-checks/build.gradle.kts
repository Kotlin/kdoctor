plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm()
    macosX64()
    macosArm64()

    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
        commonMain {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }
        val jvmAndMacosMain by creating {
            dependsOn(commonMain.get())
        }
        jvmMain {
            dependsOn(jvmAndMacosMain)
        }
        macosMain {
            dependsOn(jvmAndMacosMain)
            dependencies {
                implementation("co.touchlab:kermit:2.0.1")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmTest {
            dependencies {
                implementation("co.touchlab:kermit:2.0.1")
            }
        }
    }

    applyDefaultHierarchyTemplate()
}