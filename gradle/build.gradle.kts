import org.gradle.api.tasks.testing.logging.TestLogEvent

/*
 *  Copyright 2023 CNM Ingenuity, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
plugins {
    id("java")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

val solutionArtifact = "${project.property("name")}-solution"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/${project.property("organization")}/${solutionArtifact}")
        credentials {
            username = project.property("packageConsumerUser") as String?
            password = project.property("packageConsumerToken") as String?
        }
    }
}

dependencies {

    testImplementation(libs.junit.aggregator)
    testRuntimeOnly(libs.junit.engine)

    implementation("${project.group}:${solutionArtifact}:${stem(project.version as String)}.+:tests")

}

tasks.create<Copy>("copyTests") {
    val pattern = Regex("^.*${solutionArtifact}-.*-tests.jar\$")
    val jarFile = configurations.testRuntimeClasspath.get().find {
       name.matches(pattern)
    }
    from(zipTree(jarFile!!))
    into("${projectDir}/autograding/")
}

tasks.create<Copy>("copyConfiguration") {
    dependsOn(tasks["copyTests"])
    from("${projectDir}/autograding/autograding.json")
    into("${projectDir}/.github/classroom/")
}

tasks.create<Copy>("copyReport") {
    from("${java.testReportDir}/test") {
        include("**/*")
    }
    into("${java.testReportDir}/combined/${project.property("reportName")}/")
}

tasks.compileTestJava {
    enabled = false
}

tasks.processTestResources {
    enabled = false
}

tasks.test {
    useJUnitPlatform()
    dependsOn(tasks.classes)
    dependsOn(tasks["copyTests"])
    classpath -= testClassesDirs
    classpath -= files("${layout.buildDirectory}/resources/test")
    testClassesDirs = files("${projectDir}/autograding")
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
    }
    finalizedBy(tasks["copyReport"])
}

fun stem(version: String): String {
    return version.substring(0, version.lastIndexOf('.'))
}
