/*
 * Copyright © 2024, QuickFaaS
 *
 * Unit tests for [model.AwsProvider].
 *
 * Scope: LOCAL logic only. No AWS SDK / HTTP / network calls are made.
 *  - setProjectData(accountId) propagates the Account ID and iamRoleArn locally.
 *  - requestProjects() returns the in-memory list and performs no cloud call.
 *
 * NOTE on the blank-name branch: setProjectData("") takes the
 * logPropertyMissing("project", "") path, which ultimately calls
 * controller.General.logMessage(.., 1) -> kotlin.system.exitProcess(1). That path
 * terminates the JVM and is therefore not unit-testable in-process (trapping
 * System.exit requires a SecurityManager, which is unavailable on modern JDKs).
 * It is left as a documented limitation rather than tested here.
 */

package model

import model.projects.AwsProjectData
import model.resources.functions.AwsLambdaFunction
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AwsProviderTest {

    @Test
    fun setProjectData_setsAccountIdName_andPropagatesIamRoleArn() {
        val provider = AwsProvider()
        // Seed an iamRoleArn on the specifics; setProjectData must copy it onto the Lambda fn.
        provider.cloudSpecifics.iamRoleArn = "arn:aws:iam::123456789012:role/quickfaas"

        provider.setProjectData("123456789012")

        val projectData = provider.project.projectData
        assertTrue(projectData is AwsProjectData, "projectData should be an AwsProjectData")
        assertEquals("123456789012", projectData.name)

        // Selecting a project resets the chosen bucket.
        assertEquals("", provider.project.function.bucket.bucketData.name)

        val lambdaFn = provider.project.function as AwsLambdaFunction
        assertEquals("arn:aws:iam::123456789012:role/quickfaas", lambdaFn.iamRoleArn)
    }

    @Test
    fun setProjectData_withDefaultBlankArn_propagatesBlankArn() {
        val provider = AwsProvider()
        // cloudSpecifics.iamRoleArn defaults to "".
        provider.setProjectData("999988887777")

        val lambdaFn = provider.project.function as AwsLambdaFunction
        assertEquals("", lambdaFn.iamRoleArn)
        assertEquals("999988887777", provider.project.projectData.name)
    }

    @Test
    fun requestProjects_returnsInMemoryList_withoutCloudCall() = runBlocking {
        val provider = AwsProvider()
        // Default projects list is empty; no network is involved.
        val result = provider.requestProjects()
        assertEquals(provider.projects, result)
        assertTrue(result.isEmpty(), "AWS requestProjects() should return the (empty) in-memory list")
        // requestProjects() also clears the project name.
        assertEquals("", provider.project.projectData.name)
    }

    @Test
    fun companionMetadata_isStableAndLocal() {
        assertEquals("Amazon Web Services", AwsProvider.name)
        assertEquals("aws", AwsProvider.shortName)
        // newCloudProvider() yields a fresh AwsProvider instance.
        val created = AwsProvider.newCloudProvider()
        assertTrue(created is AwsProvider)
    }
}
