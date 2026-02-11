package costaber.com.github.omniflow.resource.exception

class ExternalCloudClientException(
    workflowName: String,
    cloudProvider: String,
    message: String?,
    throwable: Throwable,
) : RuntimeException(
    "Error while deploying $workflowName for cloud provider $cloudProvider: $message",
    throwable
)