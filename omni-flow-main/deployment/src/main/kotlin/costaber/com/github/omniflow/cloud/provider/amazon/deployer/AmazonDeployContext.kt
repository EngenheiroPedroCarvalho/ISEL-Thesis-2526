package costaber.com.github.omniflow.cloud.provider.amazon.deployer

import costaber.com.github.omniflow.deployer.DeployContext

data class AmazonDeployContext(
    val roleArn: String,
    val region: String = "us-east-1",
    val tags: Map<String, String> = emptyMap(),
    val stateMachineName: String,
) : DeployContext