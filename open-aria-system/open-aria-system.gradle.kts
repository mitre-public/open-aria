dependencies {
    api(project(":open-aria-core"))
    api(project(":open-aria-threading"))
    api(project(":open-aria-pairing"))
    api(project(":open-aria-kafka"))

    implementation("com.beust:jcommander:1.78")
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    testLogging {
        events("SKIPPED", "FAILED") // Options are: "PASSED", "SKIPPED", "FAILED"
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "open-aria-system"
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
                description.set("OpenARIA's library for running at scale")
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