/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model

import model.projects.GcpProject
import model.projects.GcpProjectData
import model.projects.ProjectData
import model.requests.GcpRequests

class GcpProvider : CloudProvider {

    companion object : CloudCompanion {
        override val name = "Google Cloud Platform"
        override val shortName = "gcp"
        override val cloudRequests = GcpRequests
        override fun newCloudProvider() = GcpProvider()
    }

    override val companion = Companion
    override var projects: List<ProjectData> = listOf()
    override val project = GcpProject()
    override val cloudSpecifics = null

    override suspend fun requestProjects(): List<ProjectData> {
        project.projectData.name = ""
        projects = GcpRequests.getProjects().projects
        return projects
    }

    /**
     * Also accepts the project ID, which is what OmniFlow and the GCP APIs use; a project's
     * display name need not match it.
     */
    override fun setProjectData(projectName: String) {
        val byId = projects.find { proj -> (proj as GcpProjectData).projectId == projectName }
        super.setProjectData(byId?.name ?: projectName)
    }

}
