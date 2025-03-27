plugins {
    id("jacoco")
}

dependencies {

    // Approved for Public Release; Distribution Unlimited, Case Number 17-1970.
    // Note: No Software PRS records appear in MITRE archives
    // Consulted Sheng Liu on SWIM_OS PRS process to verify PRS validity
    api(files("libs/swim-parse-7.0.1.jar"))

    // JAXB dependencies for SWIM-parse, needed at runtime
    api("org.glassfish.jaxb:jaxb-runtime:4.0.0")

    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.15.2")
}


tasks.named<Test>("test") {
    useJUnitPlatform()

    testLogging {
        events("SKIPPED", "FAILED") // Options are: "PASSED", "SKIPPED", "FAILED"
    }
}


tasks {
    withType<JavaCompile> {
        description = "A place to set javac options, e.g -Xlint."

//        options.compilerArgs.add("-Xdoclint:all,-missing")
//        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "open-aria-core"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set(project.name)
                description.set("OpenARIA's core assets")
                url.set("https://github.com/mitre-public/open-aria")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("jparker")
                        name.set("Jon Parker")
                        email.set("jiparker@mitre.org")
                    }
                    developer {
                        id.set("dkun")
                        name.set("David Kun")
                        email.set("dkun@mitre.org")
                    }
                }

                //REQUIRED! To publish to maven central (from experience the leading "scm:git" is required too)
                scm {
                    connection.set("scm:git:https://github.com/mitre-public/open-aria.git")
                    developerConnection.set("scm:git:ssh://git@github.com:mitre-public/open-aria.git")
                    url.set("github.com/mitre-public/open-aria")
                }
            }
        }
    }
}