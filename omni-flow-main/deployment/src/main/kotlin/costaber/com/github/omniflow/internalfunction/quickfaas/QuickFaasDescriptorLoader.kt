package costaber.com.github.omniflow.internalfunction.quickfaas

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path

class QuickFaasDescriptorLoader {

    companion object {
        private val logger = KotlinLogging.logger {}
        private val mapper = ObjectMapper()

        private val GCP_VALID_RUNTIMES = setOf("java11", "java17", "java21", "nodejs14", "nodejs20", "nodejs22")
        private val AWS_VALID_RUNTIMES = setOf("java11", "java17", "java21", "nodejs18.x", "nodejs20.x", "python3.11", "python3.12")

        fun load(path: Path): QuickFaasDescriptor {
            require(Files.exists(path)) {
                "QuickFaaS deployment descriptor not found at '$path'"
            }

            val json = Files.readString(path)
            return mapper.readValue(json, QuickFaasDescriptor::class.java)
        }

        fun validate(descriptor: QuickFaasDescriptor, expectedCloudProvider: String) {
            val provider = descriptor.cloudProvider
                ?: throw IllegalStateException(
                    "QuickFaaS descriptor is missing 'cloudProvider' field"
                )

            if (!provider.equals(expectedCloudProvider, ignoreCase = true)) {
                throw IllegalStateException(
                    "Cloud provider mismatch: workflow targets '$expectedCloudProvider' " +
                            "but QuickFaaS descriptor specifies '$provider'."
                )
            }

            requireNotNull(descriptor.function?.name) {
                "QuickFaaS descriptor is missing 'function.name'"
            }

            val runtime = descriptor.function?.runtime
            val funcName = descriptor.function?.name ?: ""

            when {
                provider.equals("gcp", ignoreCase = true) -> {
                    if (runtime != null) {
                        require(runtime in GCP_VALID_RUNTIMES) {
                            "Invalid runtime '$runtime' for GCP. Supported: ${GCP_VALID_RUNTIMES.joinToString(", ")}"
                        }
                    }
                    if (descriptor.function?.bucket.isNullOrBlank()) {
                        logger.warn { "Descriptor missing 'function.bucket'. QuickFaaS needs a GCS bucket for source upload." }
                    }
                    val token = descriptor.accessToken
                    if (token != null && (token.contains("REPLACE") || token.contains("placeholder", ignoreCase = true))) {
                        logger.info { "Descriptor has placeholder accessToken. OmniFlow will inject a fresh token from ADC." }
                    }
                    if (funcName.isNotEmpty() && !Regex("^[a-z][a-z0-9-]{0,62}$").matches(funcName)) {
                        logger.warn { "Function name '$funcName' may not conform to GCP naming rules (lowercase, hyphens, max 63 chars)." }
                    }
                }
                provider.equals("aws", ignoreCase = true) -> {
                    if (runtime != null) {
                        require(runtime in AWS_VALID_RUNTIMES) {
                            "Invalid runtime '$runtime' for AWS Lambda. Supported: ${AWS_VALID_RUNTIMES.joinToString(", ")}"
                        }
                    }
                    if (descriptor.function?.bucket.isNullOrBlank()) {
                        logger.warn { "Descriptor missing 'function.bucket'. QuickFaaS needs an S3 bucket for code upload." }
                    }
                    if (descriptor.iamRoleArn.isNullOrBlank()) {
                        logger.warn { "Descriptor missing 'iamRoleArn'. AWS Lambda deployment requires an IAM execution role." }
                    }
                    if (funcName.isNotEmpty() && !Regex("^[a-zA-Z0-9_-]{1,64}$").matches(funcName)) {
                        logger.warn { "Function name '$funcName' may not conform to Lambda naming rules (alphanumeric, hyphens, underscores, max 64 chars)." }
                    }
                }
            }

            logger.info {
                "Descriptor validated: provider=$provider, " +
                        "function=${descriptor.function?.name}, " +
                        "runtime=${descriptor.function?.runtime}, " +
                        "location=${descriptor.function?.location}"
            }
        }
    }
}
