package costaber.com.github.omniflow.cloud.provider.google.deployer

import costaber.com.github.omniflow.cloud.provider.google.provider.GoogleDefaultStrategyDeciderProvider.createNodeRendererStrategyDecider
import costaber.com.github.omniflow.cloud.provider.google.renderer.GoogleRenderingContext
import costaber.com.github.omniflow.cloud.provider.google.renderer.GoogleTermContext
import costaber.com.github.omniflow.cloud.provider.google.service.CloudRunV2RestCatalog
import costaber.com.github.omniflow.cloud.provider.google.service.CloudRunV2ServiceInspector
import costaber.com.github.omniflow.cloud.provider.google.service.GoogleWorkflowService
import costaber.com.github.omniflow.deployer.CloudDeployer
import costaber.com.github.omniflow.internalfunction.InternalFunctionDeployer
import costaber.com.github.omniflow.internalfunction.NoopInternalFunctionDeployer
import costaber.com.github.omniflow.internalfunction.WorkflowInternalFunctionResolver
import costaber.com.github.omniflow.model.*
import costaber.com.github.omniflow.registry.FunctionRegistryBootstrapper
import costaber.com.github.omniflow.registry.FunctionRegistryStore
import costaber.com.github.omniflow.registry.WorkflowInternalCallEndpointResolver
import costaber.com.github.omniflow.resource.util.joinToStringNewLines
import costaber.com.github.omniflow.traversor.DepthFirstNodeVisitorTraversor
import costaber.com.github.omniflow.visitor.NodeContextVisitor
import mu.KotlinLogging
import java.nio.file.Path

class GoogleCloudDeployer internal constructor(
    private val nodeTraversor: DepthFirstNodeVisitorTraversor,
    private val contextVisitor: NodeContextVisitor,
    private val googleWorkflowService: GoogleWorkflowService,
    private val registryPath: Path = Path.of(System.getProperty("user.dir")).resolve("function-registry.json"),
    private val functionsCatalog: CloudRunV2RestCatalog = CloudRunV2RestCatalog(),
    private val internalFunctionDeployer: InternalFunctionDeployer = NoopInternalFunctionDeployer
) : CloudDeployer<GoogleDeployContext> {

    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    override fun deploy(workflow: Workflow, deployContext: GoogleDeployContext) {
        logger.info {"Starting to convert Workflow into a Workflow" }

        bootstrapFunctionRegisterIfMissing(deployContext.projectId)

        val resolvedWorkflow = WorkflowInternalFunctionResolver(
            projectId = deployContext.projectId,
            preferredRegion = deployContext.zone,
            registry = FunctionRegistryStore(registryPath),
            inspector = CloudRunV2ServiceInspector(),
            internalFunctionDeployer = internalFunctionDeployer
        ).resolve(workflow)

        val renderingContext = GoogleRenderingContext(termContext = GoogleTermContext())
        val content = nodeTraversor.traverse(contextVisitor, resolvedWorkflow, renderingContext)
            .filterNot(String::isEmpty)
            .joinToStringNewLines()


        googleWorkflowService.deploy(
            projectId = deployContext.projectId,
            zone = deployContext.zone,
            serviceAccount = deployContext.serviceAccount,
            workflowId = deployContext.workflowId,
            workflowDescription = deployContext.workflowDescription,
            workflowLabels = deployContext.workflowLabels,
            workflowSourceContents = content,
        )
    }

    private fun bootstrapFunctionRegisterIfMissing(projectId: String){
        val store = FunctionRegistryStore(registryPath)

        if(store.exists()){
            logger.info { "Function Registry found at '$registryPath' (skipping bootstrap). "}
            return
        }

        logger.warn{
            "Function Registry not found at 'registryPath'. Boothstrapping registry from Cloud Functions APIs for project '$projectId'... "
        }

        FunctionRegistryBootstrapper(
            store = store,
            catalog = functionsCatalog
        ).bootstrapIfMissing(projectId)

        logger.info { "Function Registry created and populated at '$registryPath'." }
    }



    class Builder {
        private var registryPath: Path = Path.of(System.getProperty("user.dir")).resolve("function-registry.json")
        private var functionsCatalog: CloudRunV2RestCatalog = CloudRunV2RestCatalog()
        private var internalFunctionDeployer: InternalFunctionDeployer = NoopInternalFunctionDeployer

        fun registryPath(path: Path) = apply { this.registryPath = path }
        fun internalFunctionDeployer(value: InternalFunctionDeployer) = apply { this.internalFunctionDeployer = value }

        fun build() = GoogleCloudDeployer(
            nodeTraversor = DepthFirstNodeVisitorTraversor(),
            contextVisitor = NodeContextVisitor(createNodeRendererStrategyDecider()),
            googleWorkflowService = GoogleWorkflowService(),
            registryPath = registryPath,
            functionsCatalog = functionsCatalog,
            internalFunctionDeployer = internalFunctionDeployer
        )
    }
}