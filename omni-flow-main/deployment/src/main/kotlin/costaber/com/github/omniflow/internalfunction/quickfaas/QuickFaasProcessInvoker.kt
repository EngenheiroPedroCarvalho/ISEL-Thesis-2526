package costaber.com.github.omniflow.internalfunction.quickfaas

import mu.KotlinLogging
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class QuickFaasProcessInvoker(
    private val jarPath: Path,
    private val timeoutMinutes: Long = 5
) {
    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun invoke(descriptorPath: Path) {
        val workingDir = descriptorPath.parent?.toFile()
            ?: throw IllegalStateException("Descriptor path '$descriptorPath' has no parent directory")

        require(jarPath.toFile().exists()) {
            "QuickFaaS JAR not found at '$jarPath'. " +
                    "Set the QUICKFAAS_JAR_PATH environment variable or system property to the correct path."
        }

        val command = listOf("java", "-jar", jarPath.toAbsolutePath().toString())

        logger.info { "Invoking QuickFaaS: ${command.joinToString(" ")} (workDir=$workingDir)" }

        val process = ProcessBuilder(command)
            .directory(workingDir)
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
        if (exitCode != 0) {
            throw IllegalStateException(
                "QuickFaaS deployment failed (exit code $exitCode). Output:\n$output"
            )
        }

        logger.info { "QuickFaaS process exited successfully (code=0)" }
        logger.debug { "QuickFaaS output:\n$output" }
    }
}
