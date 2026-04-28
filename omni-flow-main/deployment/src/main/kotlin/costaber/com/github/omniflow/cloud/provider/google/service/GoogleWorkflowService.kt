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
        private const val RESET   = "[0m"
        private const val BOLD    = "[1m"
        private const val GREEN   = "[32m"
        private const val BLUE    = "[34m"
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
        println("$BLUE  →$RESET Creating Google Workflows client...")
        logger.info { "Creating deployment request for Workflow $workflowId" }
        val workflowsClient = WorkflowsClient.create()
        try {
            val projectLocation = LocationName.of(projectId, zone).toString()
            println("$BLUE  →$RESET Building workflow definition (project=$projectId, zone=$zone)...")
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
            println("$BLUE  →$RESET Sending workflow '${BOLD}$workflowId${RESET}' to Google Cloud Workflows API (async)...")
            val workflowCreationResult = workflowsClient.createWorkflowAsync(createWorkflowRequest)
            println("$BLUE  →$RESET Waiting for workflow creation to complete...")
            val result = workflowCreationResult.get()
            println("$GREEN  ✓$RESET Workflow '${BOLD}$workflowId${RESET}' created successfully on Google Cloud")
            logger.info { "Workflow creation result $result" }
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