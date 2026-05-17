/*
 * Copyright © 2024, QuickFaaS
 */

package model.specifics

import controller.General.logMessage
import model.DeploymentData
import model.requests.AwsRequests

class AwsSpecifics : CloudSpecifics {

    var iamRoleArn: String = ""

    override fun setSpecifics(deploymentData: DeploymentData) {
        val region = deploymentData.function.location.ifBlank {
            System.getenv("AWS_REGION") ?: "us-east-1"
        }
        AwsRequests.setRegion(region)

        iamRoleArn = deploymentData.iamRoleArn
            ?: System.getenv("AWS_LAMBDA_ROLE_ARN")
            ?: ""

        if (iamRoleArn.isBlank()) {
            logMessage(
                "WARNING: 'iamRoleArn' not found in descriptor or AWS_LAMBDA_ROLE_ARN env var. " +
                        "Lambda deployment requires an execution role ARN.",
                2
            )
        }
    }
}
