/*
 * Copyright © 2024, QuickFaaS
 *
 * Unit tests for [model.specifics.AwsSpecifics].
 *
 * Scope: LOCAL logic only. setSpecifics() resolves the region (forwarded to
 * AwsRequests.setRegion(), a pure in-memory field setter — NO SDK/network) and
 * the iamRoleArn from the deployment descriptor.
 *
 * We only exercise descriptor-provided branches plus the blank-arn WARNING
 * branch. We deliberately avoid asserting on the env-var fallback branches
 * (AWS_REGION / AWS_LAMBDA_ROLE_ARN) because environment variables cannot be
 * set reliably in-process; supplying explicit descriptor values short-circuits
 * the env lookups (Kotlin evaluates the descriptor value first via ifBlank { } /
 * ?: ), so those branches are never reached here.
 */

package model.specifics

import model.DeploymentData
import model.FunctionDeploymentData
import model.TriggerDeploymentData
import kotlin.test.Test
import kotlin.test.assertEquals

class AwsSpecificsTest {

    private fun deploymentData(
        location: String,
        iamRoleArn: String?
    ): DeploymentData = DeploymentData(
        cloudProvider = "aws",
        accessToken = "",
        subscriptionId = null,
        iamRoleArn = iamRoleArn,
        project = "123456789012",
        function = FunctionDeploymentData(
            name = "my-fn",
            location = location,
            bucket = "my-bucket",
            runtime = "java11",
            trigger = TriggerDeploymentData(type = "http")
        ),
        functionFile = "function-definition.java",
        dependenciesFile = null,
        configurationsFile = null
    )

    @Test
    fun setSpecifics_resolvesIamRoleArnFromDescriptor() {
        val specifics = AwsSpecifics()
        val data = deploymentData(
            location = "eu-west-1",
            iamRoleArn = "arn:aws:iam::123456789012:role/quickfaas-exec"
        )

        specifics.setSpecifics(data)

        assertEquals("arn:aws:iam::123456789012:role/quickfaas-exec", specifics.iamRoleArn)
    }

    @Test
    fun setSpecifics_withExplicitLocation_doesNotAlterDescriptorArn() {
        val specifics = AwsSpecifics()
        // A non-blank location resolves directly to the descriptor region without
        // touching the AWS_REGION env fallback; the iamRoleArn comes from the descriptor.
        val data = deploymentData(
            location = "eu-central-1",
            iamRoleArn = "arn:aws:iam::000000000000:role/explicit"
        )

        specifics.setSpecifics(data)

        assertEquals("arn:aws:iam::000000000000:role/explicit", specifics.iamRoleArn)
    }

    @Test
    fun setSpecifics_withBlankArn_takesWarningBranch_andLeavesArnBlank() {
        val specifics = AwsSpecifics()
        // iamRoleArn = "" is non-null and non-null wins the elvis chain, so the value
        // stays "" (it never falls through to the env var). The blank check then logs a
        // WARNING via logMessage(.., 2) — code 2 only prints, it does NOT exitProcess.
        val data = deploymentData(location = "eu-west-2", iamRoleArn = "")

        specifics.setSpecifics(data)

        assertEquals("", specifics.iamRoleArn)
    }

    @Test
    fun setSpecifics_isIdempotentForDescriptorArn() {
        val specifics = AwsSpecifics()
        val first = deploymentData(location = "eu-west-1", iamRoleArn = "arn:aws:iam::111:role/a")
        val second = deploymentData(location = "eu-west-2", iamRoleArn = "arn:aws:iam::222:role/b")

        specifics.setSpecifics(first)
        assertEquals("arn:aws:iam::111:role/a", specifics.iamRoleArn)

        // A second call fully overwrites the previously resolved value.
        specifics.setSpecifics(second)
        assertEquals("arn:aws:iam::222:role/b", specifics.iamRoleArn)
    }
}
