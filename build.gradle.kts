plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

group = "com.baymc"

val baseVersion = "1.0-SNAPSHOT"
version = providers.gradleProperty("artifactVersionOverride").orElse(baseVersion).get()

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.60-stable")

    implementation("io.lettuce:lettuce-core:7.5.1.RELEASE")
    implementation("com.google.code.gson:gson:2.14.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks {
    register("printVersion") {
        doLast {
            println(baseVersion)
        }
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    shadowJar {
        archiveBaseName.set("BayMcPatrol")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")

        relocate("io.lettuce", "com.baymc.patrol.libs.lettuce")
        relocate("io.netty", "com.baymc.patrol.libs.netty")
        relocate("reactor", "com.baymc.patrol.libs.reactor")
        relocate("org.reactivestreams", "com.baymc.patrol.libs.reactivestreams")
        relocate("com.google.gson", "com.baymc.patrol.libs.gson")
    }

    build {
        dependsOn(shadowJar)
    }
}
