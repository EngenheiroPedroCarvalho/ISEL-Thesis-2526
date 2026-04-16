package costaber.com.github.omniflow.internalfunction.quickfaas

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path

class QuickFaasDescriptorLoader {

    companion object {
        private val logger = KotlinLogging.logger {}
        private val mapper = ObjectMapper()

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

            logger.info {
                "Descriptor validated: provider=$provider, " +
                        "function=${descriptor.function?.name}, " +
                        "location=${descriptor.function?.location}"
            }
        }
    }
}
