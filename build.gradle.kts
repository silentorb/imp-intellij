plugins {
    base
    id("org.jetbrains.intellij") version "0.4.16"
    kotlin("jvm") version "1.3.71"
}

group = "silentorb.imp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("silentorb.imp:parsing")
    implementation("silentorb.imp:execution")
    implementation("silentorb.imp:libraries_standard")
    implementation("silentorb.imp:libraries_standard_implementation")
    implementation("silentorb.mythic:imaging")
    implementation("silentorb.mythic:lookinglass")
    implementation("silentorb.mythic:desktop")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2019.3.1"
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes("""
      Add change notes here.<br>
      <em>most HTML tags may be used</em>""")
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
