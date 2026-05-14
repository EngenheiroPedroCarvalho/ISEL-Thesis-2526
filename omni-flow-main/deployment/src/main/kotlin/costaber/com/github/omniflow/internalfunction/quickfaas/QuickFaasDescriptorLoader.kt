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
                            "but QuickFaaS descriptor specifies '$provider'. " +
                            "A Google workflow can only use functions deployed on Google."
                )
            }

            requireNotNull(descriptor.function?.name) {
                "QuickFaaS descriptor is missing 'function.name'"
            }

            val runtime = descriptor.function?.runtime
            if (provider.equals("gcp", ignoreCase = true) && runtime != null) {
                require(runtime in GCP_VALID_RUNTIMES) {
                    "Invalid runtime '$runtime' for GCP. " +
                            "QuickFaaS supports: ${GCP_VALID_RUNTIMES.joinToString(", ")}"
                }
            }

            if (descriptor.function?.bucket.isNullOrBlank()) {
                logger.warn { "Descriptor missing 'function.bucket'. QuickFaaS needs a GCS bucket for source upload." }
            }

            val token = descriptor.accessToken
            if (token != null && (token.contains("REPLACE") || token.contains("placeholder", ignoreCase = true))) {
                logger.info { "Descriptor has placeholder accessToken. OmniFlow will inject a fresh token from ADC." }
            }

            val funcName = descriptor.function?.name ?: ""
            if (funcName.isNotEmpty() && !Regex("^[a-z][a-z0-9-]{0,62}$").matches(funcName)) {
                logger.warn { "Function name '$funcName' may not be valid for GCP naming rules (lowercase, hyphens, max 63 chars)." }
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
