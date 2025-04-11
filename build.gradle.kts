plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.shinriyo"
version = "0.0.1"

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.1.4") // Dart plugin 251.x に対応する IntelliJ IDEA バージョン
    type.set("IU") // Dart plugin を使うには Ultimateが必要

    plugins.set(listOf(
        "com.intellij.java",
        "org.jetbrains.kotlin",
        "org.intellij.intelliLang",
        file("libs/dart-251.23774.318.zip") // ✅ ローカル zip を使う
    ))
}

dependencies {
    // ❌ これらは削除する → IntelliJ Pluginが管理するので不要
    // compileOnly("com.jetbrains:intellij-community:241.8102.112")
    // compileOnly("com.jetbrains.plugins:dart:241.8102.112")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            languageVersion = "1.9"
            freeCompilerArgs += "-Xsuppress-deprecated-warnings"
        }
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("251.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    runIde {
        jvmArgs("-Xmx2g")
        autoReloadPlugins.set(false)
    }
}

kotlin {
    jvmToolchain(17)
}
