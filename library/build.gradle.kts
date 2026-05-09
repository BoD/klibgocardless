import com.gradleup.librarian.gradle.Librarian

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  jvm()

  sourceSets {
    commonMain {
      dependencies {
        // Ktor
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.contentNegotiation)
        implementation(libs.ktor.client.auth)
        implementation(libs.ktor.client.logging)
        implementation(libs.ktor.serialization.kotlinx.json)

        // Date time
        implementation(libs.kotlinx.datetime)

        // Serialization
        implementation(libs.kotlinx.serialization.json)

        // Logging
        implementation(libs.klibnanolog)
      }
    }

    jvmMain {
      dependencies {
        // Coroutines
        implementation(libs.kotlinx.coroutines.jdk9)

        // Ktor OkHttp
        implementation(libs.ktor.client.okhttp)
      }
    }
  }
}

Librarian.module(project)
