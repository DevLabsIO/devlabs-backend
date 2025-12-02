package com.devlabs.devlabsbackend.project.domain.dto

import com.devlabs.devlabsbackend.project.domain.Project
import com.devlabs.devlabsbackend.project.domain.ProjectReference
import com.devlabs.devlabsbackend.project.domain.ProjectStatus
import com.devlabs.devlabsbackend.user.domain.dto.UserResponse
import com.devlabs.devlabsbackend.user.service.toUserResponse
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import java.sql.Timestamp
import java.util.*

data class ProjectReferenceResponse(
    val id: String,
    val title: String,
    val url: String?,
    val description: String?
)

data class ProjectReferenceRequest(
    val id: String? = null,
    val title: String,
    val url: String? = null,
    val description: String? = null
)

data class ProjectResponse(
    val id: UUID?,
    val title: String,
    val description: String,
    val objectives: String?,
    val githubUrl: String?,
    val status: ProjectStatus,
    val teamId: UUID?,
    val teamName: String?,
    val teamMembers: List<UserResponse>,
    val courses: List<CourseInfo>,
    val reviewCount: Int,
    val references: List<ProjectReferenceResponse>,
    val uploadedFiles: List<String>,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)

data class CourseInfo(
    val id: UUID,
    val name: String,
    val code: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateProjectRequest(
    val title: String,
    val description: String,
    val objectives: String? = null,
    val githubUrl: String? = null,
    val teamId: UUID,
    
    @JsonSetter(nulls = Nulls.SKIP)
    val courseIds: List<UUID> = emptyList(),
    
    @JsonSetter(nulls = Nulls.SKIP)
    val references: List<ProjectReferenceRequest> = emptyList(),
    
    @JsonSetter(nulls = Nulls.SKIP)
    val uploadedFiles: List<String> = emptyList()
)

data class UpdateProjectRequest(
    val userId: String,
    val title: String? = null,
    val description: String? = null,
    val objectives: String? = null,
    val githubUrl: String? = null,
    val references: List<ProjectReferenceRequest>? = null,
    val uploadedFiles: List<String>? = null
)

data class UserIdRequest(
    val userId: String
)

fun ProjectReference.toProjectReferenceResponse(): ProjectReferenceResponse {
    return ProjectReferenceResponse(
        id = this.id,
        title = this.title,
        url = this.url,
        description = this.description
    )
}

fun Project.toProjectResponse(): ProjectResponse {
    return ProjectResponse(
        id = this.id,
        title = this.title,
        description = this.description,
        objectives = this.objectives,
        githubUrl = this.githubUrl,
        status = this.status,
        teamId = this.team.id,
        teamName = this.team.name,
        teamMembers = this.team.members.map { it.toUserResponse() },
        courses = this.courses.map { course ->
            CourseInfo(
                id = course.id ?: throw IllegalArgumentException("Course ID cannot be null"),
                name = course.name,
                code = course.code
            )
        },
        reviewCount = this.reviews.size,
        references = this.references?.map { it.toProjectReferenceResponse() } ?: emptyList(),
        uploadedFiles = this.uploadedFiles?.toList() ?: emptyList(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun createProjectResponse(
    projectData: Map<String, Any>,
    teamMembers: List<UserResponse>,
    courses: List<CourseInfo>,
    references: List<ProjectReferenceResponse> = emptyList(),
    uploadedFiles: List<String> = emptyList()
): ProjectResponse {
    require(projectData["id"] != null) { "Project ID is required" }
    require(projectData["title"] != null) { "Project title is required" }
    require(projectData["description"] != null) { "Project description is required" }
    require(projectData["status"] != null) { "Project status is required" }
    require(projectData["created_at"] != null) { "Created timestamp is required" }
    require(projectData["updated_at"] != null) { "Updated timestamp is required" }
    return ProjectResponse(
        id = UUID.fromString(projectData["id"].toString()),
        title = projectData["title"].toString(),
        description = projectData["description"].toString(),
        objectives = projectData["objectives"]?.toString(),
        githubUrl = projectData["github_url"]?.toString(),
        status = ProjectStatus.values()[(projectData["status"] as Number).toInt()],
        teamId = projectData["team_id"]?.let { UUID.fromString(it.toString()) },
        teamName = projectData["team_name"]?.toString(),
        teamMembers = teamMembers,
        courses = courses,
        reviewCount = (projectData["review_count"] as? Number)?.toInt() ?: 0,
        references = references,
        uploadedFiles = uploadedFiles,
        createdAt = projectData["created_at"] as Timestamp,
        updatedAt = projectData["updated_at"] as Timestamp
    )
}
