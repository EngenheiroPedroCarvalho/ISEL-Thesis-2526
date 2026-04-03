package costaber.com.github.omniflow.cloud.provider.amazon.deployer

import costaber.com.github.omniflow.cloud.provider.amazon.renderer.AmazonRenderingContext
import costaber.com.github.omniflow.cloud.provider.amazon.service.AmazonStateMachineService
import costaber.com.github.omniflow.cloud.provider.google.provider.GoogleDefaultStrategyDeciderProvider.createNodeRendererStrategyDecider
import costaber.com.github.omniflow.deployer.CloudDeployer
import costaber.com.github.omniflow.internalfunction.InternalFunctionDeployer
import costaber.com.github.omniflow.internalfunction.NoopInternalFunctionDeployer
import costaber.com.github.omniflow.internalfunction.WorkflowInternalFunctionResolver
import costaber.com.github.omniflow.model.Workflow
import costaber.com.github.omniflow.registry.FunctionRegistryStore
import costaber.com.github.omniflow.resource.util.joinToStringNewLines
import costaber.com.github.omniflow.traversor.DepthFirstNodeVisitorTraversor
import costaber.com.github.omniflow.visitor.NodeContextVisitor
import mu.KotlinLogging
import java.nio.file.Path

class AmazonCloudDeployer internal constructor(
    private val nodeTraversor: DepthFirstNodeVisitorTraversor,
    private val contextVisitor: NodeContextVisitor,
    private val amazonStateMachineService: AmazonStateMachineService,
    private val registryPath: Path,
    private val internalFunctionDeployer: InternalFunctionDeployer
) : CloudDeployer<AmazonDeployContext> {

    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    override fun deploy(workflow: Workflow, deployContext: AmazonDeployContext) {
        val registryPath = Path.of(System.getProperty("user.dir")).resolve("function-registry.json")
        val registryStore = FunctionRegistryStore(registryPath)



        
        val content = nodeTraversor.traverse(contextVisitor, workflow, AmazonRenderingContext())
            .filterNot(String::isEmpty)
            .joinToStringNewLines()
        amazonStateMachineService.createStateMachine(
            roleArn = deployContext.roleArn,
            region = deployContext.region,
            tags = deployContext.tags,
            stateMachineName = deployContext.stateMachineName,
            stateMachineDefinition = content,
        )
    }

    class Builder {
        private var registryPath: Path = Path.of(System.getProperty("user.dir")).resolve("function-registry.json")
        private var internalFunctionDeployer: InternalFunctionDeployer = NoopInternalFunctionDeployer

        fun registryPath(value: Path) = apply { this.registryPath = value }
        fun internalFunctionDeployer(value: InternalFunctionDeployer) = apply { this.internalFunctionDeployer = value }

        fun build() = AmazonCloudDeployer(
            nodeTraversor = DepthFirstNodeVisitorTraversor(),
            contextVisitor = NodeContextVisitor(createNodeRendererStrategyDecider()),
            amazonStateMachineService = AmazonStateMachineService(),
            registryPath = registryPath,
            internalFunctionDeployer = internalFunctionDeployer
        )
    }
}