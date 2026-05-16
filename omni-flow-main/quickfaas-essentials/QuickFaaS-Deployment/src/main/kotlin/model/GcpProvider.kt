/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model

import model.projects.GcpProject
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

}
