/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions.runtimes.utils

import model.Utils
import model.resources.functions.runtimes.Runtime
import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.DefaultInvoker
import org.apache.maven.shared.invoker.InvocationRequest
import java.io.File

object JavaUtils : RuntimeUtils {
    override val runtime = Runtime.JAVA
    private const val mavenHome = "apache-maven-3.8.6"
    private const val pomFile = "pom.xml"

    /**
     * Returns POM file contents from [templatesDir].
     */
    fun readPom(templatesDir: String) =
        Utils.readResFile(filePath = "${runtime.templatesDirRoot}/$templatesDir/$pomFile")

    /**
     * Creates a new POM file with [pomContent] in [tmpDir].
     */
    fun createPom(pomContent: String, tmpDir: String) =
        Utils.createFile(filePath = "${runtime.tmpDirsRoot}/$tmpDir/$pomFile", pomContent)

    /**
     * Sets and returns the specified POM [properties] in [pomContent].
     */
    fun setPomProperties(pomContent: String, properties: Array<Pair<String, String>>): String {
        var pomWithProps = pomContent
        if (properties.isNotEmpty()) properties.forEach {
            pomWithProps = pomWithProps.replace("<${it.first}>", "<${it.first}>${it.second}")
        }
        return pomWithProps
    }

    /**
     * Sets and returns the specified POM [dependencies] in [pomContent].
     */
    fun setPomDependencies(pomContent: String, dependencies: String): String =
        if (dependencies.isNotBlank()) pomContent.replace("<dependencies>", dependencies.replace("</dependencies>", ""))
        else pomContent

    /**
     * Creates a Java source file named [fileName] with [content] in the temporary directory [tmpDir].
     */
    fun createJavaSourceFile(fileName: String, content: String, tmpDir: String) =
        Utils.createFileWithDirs(directories = "${runtime.tmpDirsRoot}/$tmpDir/src/main/java", fileName, content)

    /**
     * Build the Java project placed in [tmpDir].
     */
    fun mavenBuild(tmpDir: String) {
        val request: InvocationRequest = DefaultInvocationRequest()
        request.mavenHome = File("${runtime.deploymentDir}/$mavenHome")
        request.baseDirectory = File("${runtime.tmpDirsRoot}/$tmpDir")
        request.goals = listOf("package")
        val result = DefaultInvoker().execute(request)
        if (result.exitCode != 0) {
            throw Exception(result.executionException)
        }
    }

    /**
     * Zips [sourcesDir] placed in [tmpDir]/target that resulted from the Maven build.
     */
    fun zipMavenBuild(tmpDir: String, sourcesDir: String) {
        Utils.zipDirectory(
            sourcesDir = "${runtime.tmpDirsRoot}/$tmpDir/target/$sourcesDir",
            targetZip = "${runtime.tmpDirsRoot}/$tmpDir/${Utils.ZIP_FILE}"
        )
    }

}
