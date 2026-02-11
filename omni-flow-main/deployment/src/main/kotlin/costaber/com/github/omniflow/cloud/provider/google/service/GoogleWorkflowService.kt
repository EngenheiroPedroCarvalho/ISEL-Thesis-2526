package costaber.com.github.omniflow.cloud.provider.google.service

import com.google.cloud.workflows.v1.CreateWorkflowRequest
import com.google.cloud.workflows.v1.LocationName
import com.google.cloud.workflows.v1.Workflow
import com.google.cloud.workflows.v1.WorkflowsClient
import costaber.com.github.omniflow.resource.exception.ExternalCloudClientException
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

class GoogleWorkflowService {

    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    /**
     * REQUIRED 1 ENV VARIABLE
     * - GOOGLE_APPLICATION_CREDENTIALS -> path to a json file defining the credentials
     */
    fun deploy(
        projectId: String,
        zone: String = "us-east1",
        serviceAccount: String,
        workflowId: String,
        workflowDescription: String,
        workflowLabels: Map<String, String> = emptyMap(),
        workflowSourceContents: String,
    ) {
        logger.info { "Creating deployment request for Workflow $workflowId" }
        val workflowsClient = WorkflowsClient.create()
        try {
            val projectLocation = LocationName.of(projectId, zone).toString()
            val workflow = Workflow.newBuilder()
                .setDescription(workflowDescription)
                .setSourceContents(workflowSourceContents)
                .putAllLabels(workflowLabels)
                .setServiceAccount(serviceAccount)
                .build()
            val createWorkflowRequest = CreateWorkflowRequest.newBuilder()
                .setParent(projectLocation)
                .setWorkflowId(workflowId)
                .setWorkflow(workflow)
                .build()
            val workflowCreationResult = workflowsClient.createWorkflowAsync(createWorkflowRequest)
            logger.info { "Workflow creation result ${workflowCreationResult.get()}" }
            logger.info { "Metadata: ${workflowCreationResult.metadata}" }
            workflowsClient.shutdown()
            workflowsClient.awaitTermination(5000, TimeUnit.MILLISECONDS)
        } catch (exception: Exception) {
            throw ExternalCloudClientException(
                workflowName = workflowId,
                cloudProvider = "GCP",
                message = exception.message,
                throwable = exception
            )
        }
    }
}