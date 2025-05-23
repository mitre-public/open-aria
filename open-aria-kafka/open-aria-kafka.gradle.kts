dependencies {
    api(project(":open-aria-core"))
    api("org.apache.kafka:kafka-clients:2.3.1")
    api("org.apache.kafka:kafka_2.12:2.3.0")
    api("software.amazon.msk:aws-msk-iam-auth:2.3.1")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "open-aria-kafka"
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
                description.set("OpenARIA's library for Kafka Integration")
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