package costaber.com.github.omniflow.deployer

import costaber.com.github.omniflow.model.Workflow

interface CloudDeployer<D : DeployContext> {
    fun deploy(workflow: Workflow, deployContext: D)
}