import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.6.20"
    kotlin("plugin.serialization") version "1.6.10"
    jacoco
}

group = "com.pexers.quickfaas"
version = "1.0"

repositories {
    mavenCentral()
}

application {
    mainClass.set("controller.MainKt") // No need to specify the package
}

tasks {
    val fatJar = register<Jar>("fatJar") {
        dependsOn.addAll(
            listOf(
                "compileJava",
                "compileKotlin",
                "processResources"
            )
        ) // Needed for Gradle optimization to work
        archiveClassifier.set("fat") // Naming the jar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes["Main-Class"] = application.mainClass }
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
    }
}

dependencies {
    val ktorVersion = "2.0.3"
    val awsSdkVersion = "2.20.26"
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")                      // CIO engine for HTTP client
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.apache.maven.shared:maven-invoker:3.2.0")               // Maven invoker
    implementation("software.amazon.awssdk:lambda:$awsSdkVersion")             // AWS Lambda SDK
    implementation("software.amazon.awssdk:s3:$awsSdkVersion")                 // AWS S3 SDK
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
    // Some tests exercise the REAL QuickFaaS build pipeline (AwsLambdaFunction.buildAndZip ->
    // JavaUtils.mavenBuild), which resolves the bundled Maven distribution and scratch build dir
    // via paths relative to the process cwd ("function-deployment/java/..."), rooted at
    // omni-flow-main/. Point the test JVM's working directory there so those paths resolve.
    workingDir = file("$projectDir/../../")
}

// Gradle 7.3.3's default JaCoCo version predates JDK 21 bytecode support (class file major
// version 65) and crashes when its agent is inherited by a nested forked JVM (e.g. the real
// `mvn package` run by AwsLambdaFunctionBuildIntegrationTest via Maven Invoker). Pin a version
// that supports JDK 21, matching the Maven side (deployment/pom.xml uses 0.8.12).
jacoco {
    toolVersion = "0.8.12"
}

// Coverage of the AWS provider's LOCAL logic (provider/specifics/function/build-scripts).
// Cloud-calling classes (AwsRequests, deployZip) are out of scope and excluded.
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required.set(true)
        csv.required.set(true)
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                include(
                    "model/AwsProvider*",
                    "model/projects/AwsProject*",
                    "model/specifics/AwsSpecifics*",
                    "model/resources/functions/AwsLambdaFunction*",
                    "model/resources/buckets/AwsS3Bucket*",
                    "model/resources/functions/runtimes/scripts/AwsBuildScripts*"
                )
                exclude("model/requests/AwsRequests*")
            }
        })
    )
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}