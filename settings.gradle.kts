rootProject.name = "open-aria"

include("open-aria-core")
include("open-aria-threading")
include("open-aria-pairing")
include("open-aria-kafka")
include("open-aria-system")
include("open-aria-airborne")
include("open-aria-deploy")


// Check that every subproject has a custom build file, based on the project name.
//   Got this idiom from org.junit.jupiter's gradle setup. Avoid having numerous "build.gradle.kts" files.
rootProject.children.forEach { project ->
    project.buildFileName = "${project.name}.gradle.kts"
    require(project.buildFile.isFile) {
        "${project.buildFile} must exist"
    }
}