/*
 * This "open-aria-deploy" module ONLY exists to build an uber-jar containing all the modules
 */

plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

//List all modules that should be included in the uber-jar
dependencies {
    api(project(":open-aria-core"))
    api(project(":open-aria-threading"))
    api(project(":open-aria-pairing"))
    api(project(":open-aria-kafka"))
    api(project(":open-aria-system"))
    api(project(":open-aria-airborne"))
}

/**
 * creates a fat jar.  run with 'gradle shadowJar'
 */
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    dependsOn(rootProject.getTasksByName("mavenJava", true))

    // ex: open-aria-x.y.z-uber.jar  or  open-aria-x.y.z-SNAPSHOT-uber.jar
    archiveClassifier.set("uber")
    archiveBaseName.set(rootProject.name)
}