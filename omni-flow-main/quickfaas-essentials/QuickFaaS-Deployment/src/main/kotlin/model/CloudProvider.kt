/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model

import controller.logPropertyMissing
import model.projects.CloudProject
import model.projects.ProjectData
import model.specifics.CloudSpecifics

interface CloudProvider {
    val companion: CloudCompanion
    var projects: List<ProjectData>
    val project: CloudProject
    val cloudSpecifics: CloudSpecifics?

    /**
     * Requests available projects (MsAzure -> Resource Groups).
     */
    suspend fun requestProjects(): List<ProjectData>

    /**
     * Sets the project data for the specified [projectName].
     */
    fun setProjectData(projectName: String) {
        val projectData = projects.find { proj -> proj.name == projectName }
        if (projectData == null) {
            logPropertyMissing("project", projectName)
            return
        }
        project.projectData = projectData
        project.function.bucket.bucketData.name = ""  // Selected bucket depends on the selected project
    }
}
