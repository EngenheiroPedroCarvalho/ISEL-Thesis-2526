/*
 * Copyright © 2024, QuickFaaS
 */

package model

import controller.logPropertyMissing
import model.projects.AwsProject
import model.projects.AwsProjectData
import model.projects.ProjectData
import model.requests.AwsRequests
import model.specifics.AwsSpecifics

class AwsProvider : CloudProvider {

    companion object : CloudCompanion {
        override val name = "Amazon Web Services"
        override val shortName = "aws"
        override val cloudRequests = AwsRequests
        override fun newCloudProvider(): CloudProvider = AwsProvider()
    }

    override val companion = Companion
    override var projects: List<ProjectData> = listOf()
    override val project = AwsProject()
    override val cloudSpecifics = AwsSpecifics()

    override suspend fun requestProjects(): List<ProjectData> {
        project.projectData.name = ""
        // AWS uses Account ID as the project identifier.
        // The account ID comes from the descriptor's "project" field — no API call needed.
        return projects
    }

    override fun setProjectData(projectName: String) {
        // For AWS, the "project" is the AWS Account ID stored directly in the descriptor.
        // We just assign it without a lookup in a list.
        if (projectName.isBlank()) {
            logPropertyMissing("project", projectName)
            return
        }
        (project.projectData as AwsProjectData).name = projectName
        project.function.bucket.bucketData.name = ""

        // Propagate iamRoleArn from specifics to the Lambda function
        val lambdaFn = project.function as? model.resources.functions.AwsLambdaFunction
        lambdaFn?.iamRoleArn = cloudSpecifics.iamRoleArn
    }
}
