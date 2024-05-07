plugins {
    `java-library`
    `maven-publish`
}


subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }


//  This idiom completely mutes the "javadoc" task from java-library.
//  Javadoc is still produced, but you won't get warnings OR build failures due to javadoc
//  I decided to turn warning off because the amount of javadoc required for builder was too much.
    tasks {
        javadoc {
            options {
                (this as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
            }
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
        withSourcesJar()
        withJavadocJar()
    }

    dependencies {
        implementation("org.mitre:commons:0.0.54")

        implementation("com.google.guava:guava:32.1.2-jre")
        implementation("com.google.code.gson:gson:2.8.9")

        implementation("org.apache.commons:commons-math3:3.6.1")
        implementation("org.apache.avro:avro:1.11.0")

        implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.4")
        implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
        implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
        implementation("org.yaml:snakeyaml:1.26")

        testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
        testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
        testImplementation("org.hamcrest:hamcrest-all:1.3")
    }
}