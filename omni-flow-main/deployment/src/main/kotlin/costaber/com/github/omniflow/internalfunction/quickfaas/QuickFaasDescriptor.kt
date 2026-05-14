package costaber.com.github.omniflow.internalfunction.quickfaas

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class QuickFaasDescriptor(
    val cloudProvider: String? = null,
    val accessToken: String? = null,
    val project: String? = null,
    val function: FunctionDescriptor? = null,
    val functionFile: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FunctionDescriptor(
    val name: String? = null,
    val location: String? = null,
    val runtime: String? = null,
    val bucket: String? = null,
    val trigger: TriggerDescriptor? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TriggerDescriptor(
    val type: String? = null
)
