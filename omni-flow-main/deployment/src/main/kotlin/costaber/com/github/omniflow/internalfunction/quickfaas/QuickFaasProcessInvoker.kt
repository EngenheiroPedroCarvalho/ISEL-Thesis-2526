package costaber.com.github.omniflow.internalfunction.quickfaas

import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

class QuickFaasProcessInvoker(
    private val jarPath: Path,
    private val timeoutMinutes: Long = 5
) {
    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val DESCRIPTOR_FILENAME = "func-deployment.json"
        private const val FUNCTION_DEPLOYMENT_DIR = "function-deployment"
        private const val SUCCESS_MARKER = "Deployment finished successfully!"
        private const val RESET   = "[0m"
        private const val BOLD    = "[1m"
        private const val GREEN   = "[32m"
        private const val YELLOW  = "[33m"
        private const val BLUE    = "[34m"
    }

    fun invoke(descriptorPath: Path) {
        require(jarPath.toFile().exists()) {
            "QuickFaaS JAR not found at '$jarPath'. " +
                    "Set the QUICKFAAS_JAR_PATH environment variable or system property to the correct path."
        }

        val jarDir = jarPath.parent
            ?: throw IllegalStateException("JAR path '$jarPath' has no parent directory")

        require(jarDir.resolve(FUNCTION_DEPLOYMENT_DIR).toFile().isDirectory) {
            "QuickFaaS requires a '$FUNCTION_DEPLOYMENT_DIR' directory in the same location as the JAR. " +
                    "Expected at: '${jarDir.resolve(FUNCTION_DEPLOYMENT_DIR)}'. " +
                    "Either point QUICKFAAS_JAR_PATH to the JAR inside the QuickFaaS project directory " +
                    "(where '$FUNCTION_DEPLOYMENT_DIR' already exists), or copy the '$FUNCTION_DEPLOYMENT_DIR' " +
                    "directory from the QuickFaaS project to '${jarDir}'."
        }
        val descriptorDir = descriptorPath.toAbsolutePath().parent
            ?: throw IllegalStateException("Descriptor path '$descriptorPath' has no parent directory")

        val copiedFiles = mutableListOf<Path>()
        try {
            println("$BLUE  →$RESET Copying deployment descriptor to QuickFaaS JAR directory...")
            val targetDescriptor = jarDir.resolve(DESCRIPTOR_FILENAME)
            Files.copy(descriptorPath.toAbsolutePath(), targetDescriptor, StandardCopyOption.REPLACE_EXISTING)
            copiedFiles.add(targetDescriptor)

            val descriptor = QuickFaasDescriptorLoader.load(descriptorPath.toAbsolutePath())
            descriptor.functionFile?.let { relFunctionFile ->
                val sourceFile = descriptorDir.resolve(relFunctionFile).toAbsolutePath()
                if (Files.exists(sourceFile)) {
                    val targetFile = jarDir.resolve(Path.of(relFunctionFile).fileName)
                    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
                    copiedFiles.add(targetFile)
                    println("$BLUE  →$RESET Copying function source '${sourceFile.fileName}' to JAR directory...")
                    logger.info { "Copied function file '${sourceFile.fileName}' to JAR directory" }
                }
            }

            val command = listOf("java", "-jar", jarPath.toAbsolutePath().toString())
            println("$BLUE  →$RESET Starting QuickFaaS process: ${BOLD}java -jar ${jarPath.fileName}$RESET")
            logger.info { "Invoking QuickFaaS: ${command.joinToString(" ")} (workDir=$jarDir)" }

            val process = ProcessBuilder(command)
                .directory(jarDir.toFile())
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES)

            if (!finished) {
                process.destroyForcibly()
                throw IllegalStateException(
                    "QuickFaaS deployment timed out after $timeoutMinutes minutes. Output:\n$output"
                )
            }

            val exitCode = process.exitValue()
            val deploymentSucceeded = output.contains(SUCCESS_MARKER)

            if (exitCode != 0 && !deploymentSucceeded) {
                throw IllegalStateException(
                    "QuickFaaS deployment failed (exit code $exitCode). Output:\n$output"
                )
            }

            if (exitCode != 0) {
                println("$YELLOW  !$RESET QuickFaaS exited with code $exitCode but deployment marker found — treating as success")
                logger.warn {
                    "QuickFaaS exited with code $exitCode but deployment marker was found; " +
                            "treating as success (QuickFaaS bug: doesn't wait for async Cloud Functions operation)."
                }
            } else {
                println("$GREEN  ✓$RESET QuickFaaS process finished successfully (exit code 0)")
                logger.info { "QuickFaaS process exited successfully (code=0)" }
            }
            logger.debug { "QuickFaaS output:\n$output" }
        } finally {
            copiedFiles.forEach { file ->
                try {
                    Files.deleteIfExists(file)
                } catch (e: Exception) {
                    logger.warn { "Failed to clean up temporary file '$file': ${e.message}" }
                }
            }
        }
    }
}
