package costaber.com.github.omniflow.cloud.provider.google.deployer

import costaber.com.github.omniflow.deployer.DeployContext

data class GoogleDeployContext(
    val projectId: String,
    val zone: String,
    val serviceAccount: String,
    val workflowId: String,
    val workflowDescription: String,
    val workflowLabels: Map<String, String>
) : DeployContext
