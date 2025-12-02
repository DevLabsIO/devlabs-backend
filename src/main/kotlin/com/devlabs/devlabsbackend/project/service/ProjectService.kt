package com.devlabs.devlabsbackend.project.service

import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.core.pagination.PaginationInfo
import com.devlabs.devlabsbackend.course.repository.CourseRepository
import com.devlabs.devlabsbackend.project.domain.Project
import com.devlabs.devlabsbackend.project.domain.ProjectStatus
import com.devlabs.devlabsbackend.project.domain.dto.CreateProjectRequest
import com.devlabs.devlabsbackend.project.domain.dto.CourseInfo
import com.devlabs.devlabsbackend.project.domain.dto.ProjectResponse
import com.devlabs.devlabsbackend.project.domain.dto.UpdateProjectRequest
import com.devlabs.devlabsbackend.project.domain.dto.toProjectResponse
import com.devlabs.devlabsbackend.project.repository.ProjectRepository
import com.devlabs.devlabsbackend.team.repository.TeamRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.dto.UserResponse
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.util.*

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val teamRepository: TeamRepository,
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository
) {
    
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.PROJECTS_LIST], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_ARCHIVE_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.TEAM_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
    fun createProject(projectData: CreateProjectRequest): ProjectResponse {
        try {
            val team = teamRepository.findByIdWithMembers(projectData.teamId) 
                ?: throw NotFoundException("Team with id ${projectData.teamId} not found")

            val courses = if (projectData.courseIds.isNotEmpty()) {
                courseRepository.findAllById(projectData.courseIds).also { foundCourses ->
                    if (foundCourses.size != projectData.courseIds.size) {
                        throw NotFoundException("Some courses were not found")
                    }
                }.toMutableSet()
            } else {
                mutableSetOf()
            }
            val project = Project(
                title = projectData.title,
                description = projectData.description,
                objectives = projectData.objectives,
                githubUrl = projectData.githubUrl,
                status = if (courses.isEmpty()) ProjectStatus.ONGOING else ProjectStatus.PROPOSED,
                team = team,
                courses = courses
            )
            val savedProject = projectRepository.save(project)
            return savedProject.toProjectResponse()
        } catch (e: Exception) {
            throw e
        }
    }
    
    @Cacheable(
        value = [CacheConfig.PROJECTS_LIST],
        key = "'projects_team_' + #teamId + '_' + #page + '_' + #size"
    )
    fun getProjectsByTeam(teamId: UUID, page: Int = 0, size: Int = 10, sortBy: String = "createdAt", sortOrder: String = "desc"): PaginatedResponse<ProjectResponse> {

        if (!teamRepository.existsById(teamId)) {
            throw NotFoundException("Team with id $teamId not found")
        }

        val actualSortOrder = sortOrder.uppercase()
        val offset = page * size

        val projectsData = projectRepository.findProjectsByTeamNative(teamId, sortBy, actualSortOrder, offset, size)
        val totalCount = projectRepository.countProjectsByTeam(teamId)
        
        if (projectsData.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page,
                    per_page = size,
                    total_pages = 0,
                    total_count = 0
                )
            )
        }
        
        val responses = mapProjectsDataToResponse(projectsData)

        return PaginatedResponse(
            data = responses,
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt()
            )
        )
    }
    
    @Cacheable(value = [CacheConfig.PROJECT_DETAIL], key = "'project_' + #projectId")
    fun getProjectById(projectId: UUID): ProjectResponse {
        val project = getProjectEntityById(projectId)
        return project.toProjectResponse()
    }
    
    private fun getProjectEntityById(projectId: UUID): Project {
        return projectRepository.findByIdWithRelations(projectId) 
            ?: throw NotFoundException("Project with id $projectId not found")
    }
    
    @Cacheable(
        value = [CacheConfig.PROJECTS_LIST],
        key = "'projects_course_' + #courseId + '_' + #page + '_' + #size + '_' + #sortBy + '_' + #sortOrder"
    )
    fun getProjectsByCourse(
        courseId: UUID,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "createdAt",
        sortOrder: String = "desc"
    ): PaginatedResponse<ProjectResponse> {
        if (!courseRepository.existsById(courseId)) {
            throw NotFoundException("Course with id $courseId not found")
        }

        val actualSortOrder = sortOrder.uppercase()
        val offset = page * size
        
        val projectsData = projectRepository.findProjectsByCourseNative(courseId, sortBy, actualSortOrder, offset, size)
        val totalCount = projectRepository.countProjectsByCourse(courseId)
        
        if (projectsData.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page,
                    per_page = size,
                    total_pages = 0,
                    total_count = 0
                )
            )
        }
        
        val responses = mapProjectsDataToResponse(projectsData)

        return PaginatedResponse(
            data = responses,
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt()
            )
        )
    }
    
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.PROJECT_DETAIL], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_LIST], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_ARCHIVE_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.TEAM_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
    fun updateProject(projectId: UUID, updateData: UpdateProjectRequest, requesterId: String): ProjectResponse {
        val project = getProjectEntityById(projectId)

        if (project.status !in listOf(ProjectStatus.PROPOSED, ProjectStatus.REJECTED)) {
            throw IllegalArgumentException("Project cannot be edited in current status: ${project.status}")
        }
        updateData.title?.let { project.title = it }
        updateData.description?.let { project.description = it }
        updateData.objectives?.let { project.objectives = it }
        updateData.githubUrl?.let { project.githubUrl = it }

        project.updatedAt = Timestamp.from(Instant.now())

        val savedProject = projectRepository.save(project)
        return savedProject.toProjectResponse()
    }
    
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.PROJECT_DETAIL], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_LIST], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_ARCHIVE_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECT_REVIEWS_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.TEAM_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
    fun deleteProject(projectId: UUID, userId: String): Boolean {
        val project = projectRepository.findById(projectId).orElseThrow {
            NotFoundException("Project with id $projectId not found")
        }
        
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }
        
        val isTeamMember = projectRepository.isUserTeamMember(projectId, userId)
        val isCourseInstructor = projectRepository.isUserInstructorForProject(projectId, userId)
        
        if (!isTeamMember && !isCourseInstructor) {
            throw IllegalArgumentException("You don't have permission to delete this project")
        }

        projectRepository.delete(project)
        return true
    }
    
    @Cacheable(
        value = [CacheConfig.PROJECTS_LIST],
        key = "'projects_user_course_' + #userId + '_' + #courseId + '_' + #page + '_' + #size"
    )
    fun getProjectsForUserByCourse(
        userId: String,
        courseId: UUID,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "title",
        sortOrder: String = "asc"
    ): PaginatedResponse<ProjectResponse> {
        if (!userRepository.existsById(userId)) {
            throw NotFoundException("User with id $userId not found")
        }
        if (!courseRepository.existsById(courseId)) {
            throw NotFoundException("Course with id $courseId not found")
        }

        val offset = page * size
        val projectsData = projectRepository.findProjectsByUserAndCourseNative(userId, courseId, sortBy, sortOrder, offset, size)
        val totalCount = projectRepository.countByUserAndCourse(userId, courseId)
        
        if (projectsData.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page,
                    per_page = size,
                    total_pages = 0,
                    total_count = 0
                )
            )
        }
        
        val responses = mapProjectsDataToResponse(projectsData)

        return PaginatedResponse(
            data = responses,
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = (totalCount + size - 1) / size,
                total_count = totalCount
            )
        )
    }
    
    @Cacheable(
        value = [CacheConfig.PROJECTS_LIST],
        key = "'projects_user_' + #userId + '_' + #page + '_' + #size"
    )
    fun getProjectsForUser(userId: String, page: Int = 0, size: Int = 10, sortBy: String = "createdAt", sortOrder: String = "desc"): PaginatedResponse<ProjectResponse> {
        if (!userRepository.existsById(userId)) {
            throw NotFoundException("User with id $userId not found")
        }

        val offset = page * size
        val projectsData = projectRepository.findProjectsByUserNative(userId, sortBy, sortOrder, offset, size)
        val totalCount = projectRepository.countByUser(userId)
        
        if (projectsData.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page,
                    per_page = size,
                    total_pages = 0,
                    total_count = 0
                )
            )
        }
        
        val responses = mapProjectsDataToResponse(projectsData)

        return PaginatedResponse(
            data = responses,
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = (totalCount + size - 1) / size,
                total_count = totalCount
            )
        )
    }
    
    @Cacheable(
        value = [CacheConfig.PROJECTS_LIST],
        key = "'projects_search_' + #userId + '_' + #courseId + '_' + #query + '_' + #page + '_' + #size"
    )
    fun searchProjectsByCourseForUser(
        userId: String,
        courseId: UUID,
        query: String,
        page: Int = 0,
        size: Int = 10
    ): PaginatedResponse<ProjectResponse> {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val course = courseRepository.findById(courseId).orElseThrow {
            NotFoundException("Course with id $courseId not found")
        }

        val hasAccess = when (user.role) {
            Role.ADMIN, Role.MANAGER -> true
            Role.FACULTY -> courseRepository.isUserInstructor(courseId, userId)
            Role.STUDENT -> courseRepository.isUserStudent(courseId, userId)
            else -> false
        }

        if (!hasAccess) {
            throw IllegalArgumentException("User does not have access to this course")
        }

        val pageable: Pageable = PageRequest.of(page, size)

        val projectsPage = when (user.role) {
            Role.ADMIN, Role.MANAGER -> {
                projectRepository.findByCourseAndTitleContainingIgnoreCase(course, query, pageable)
            }

            Role.FACULTY -> {
                projectRepository.findByCourseAndTitleContainingIgnoreCase(course, query, pageable)
            }

            Role.STUDENT -> {
                projectRepository.findByCourseAndTitleContainingIgnoreCaseAndTeamMembersContaining(
                    course,
                    query,
                    user,
                    pageable
                )
            }

            else -> {
                Page.empty(pageable)
            }
        }

        return PaginatedResponse(
            data = projectsPage.content.map { it.toProjectResponse() },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = projectsPage.totalPages,
                total_count = projectsPage.totalElements.toInt()
            )
        )
    }
    
    @Cacheable(
        value = [CacheConfig.PROJECTS_LIST],
        key = "'projects_active_semester_' + #semesterId"
    )
    @Transactional
    fun getActiveProjectsBySemester(semesterId: UUID): List<ProjectResponse> {
        return projectRepository.findActiveProjectsBySemester(semesterId)
            .map { it.toProjectResponse() }
    }
    
    @Cacheable(
        value = [CacheConfig.PROJECTS_LIST],
        key = "'projects_active_batch_' + #batchId"
    )
    @Transactional
    fun getActiveProjectsByBatch(batchId: UUID): List<ProjectResponse> {
        return projectRepository.findActiveProjectsByBatch(batchId)
            .map { it.toProjectResponse() }
    }
    
    @Cacheable(
        value = [CacheConfig.PROJECTS_LIST],
        key = "'projects_active_all'"
    )
    fun getAllActiveProjects(): List<ProjectResponse> {
        val projectsData = projectRepository.findAllActiveProjectsNative()
        return mapProjectsDataToResponse(projectsData)
    }
    
    @Cacheable(
        value = [CacheConfig.PROJECTS_LIST],
        key = "'projects_active_faculty_' + #facultyId"
    )
    fun getActiveProjectsByFaculty(facultyId: String): List<ProjectResponse> {
        val projectIds = projectRepository.findActiveProjectIdsByFaculty(facultyId)
        if (projectIds.isEmpty()) {
            return emptyList()
        }
        val projectsData = projectRepository.findProjectListDataByIds(projectIds)
        val projectsByIdMap = projectsData.associateBy { UUID.fromString(it["id"].toString()) }
        val orderedProjectsData = projectIds.mapNotNull { id -> projectsByIdMap[id] }
        return mapProjectsDataToResponse(orderedProjectsData)
    }
    
    private fun mapProjectsDataToResponse(projectsData: List<Map<String, Any>>): List<ProjectResponse> {
        if (projectsData.isEmpty()) {
            return emptyList()
        }
        
        val projectIds = projectsData.map { UUID.fromString(it["id"].toString()) }
        val teamIds = projectsData.mapNotNull { 
            it["team_id"]?.let { id -> UUID.fromString(id.toString()) }
        }.distinct()
        
        val teamMembersMap = if (teamIds.isNotEmpty()) {
            projectRepository.findTeamMembersByTeamIds(teamIds)
                .groupBy { UUID.fromString(it["team_id"].toString()) }
        } else emptyMap()
        
        val coursesMap = projectRepository.findCoursesByProjectIds(projectIds)
            .groupBy { UUID.fromString(it["project_id"].toString()) }
        
        return projectsData.map { data ->
            val projectId = UUID.fromString(data["id"].toString())
            val teamId = data["team_id"]?.let { UUID.fromString(it.toString()) }
            
            ProjectResponse(
                id = projectId,
                title = data["title"].toString(),
                description = data["description"].toString(),
                objectives = data["objectives"]?.toString(),
                githubUrl = data["github_url"]?.toString(),
                status = ProjectStatus.values()[(data["status"] as Number).toInt()],
                teamId = teamId,
                teamName = data["team_name"]?.toString(),
                teamMembers = teamId?.let { teamMembersMap[it]?.map { member ->
                    UserResponse(
                        id = member["id"].toString(),
                        name = member["name"].toString(),
                        email = member["email"].toString(),
                        profileId = member["profile_id"]?.toString(),
                        image = member["image"]?.toString(),
                        role = member["role"].toString(),
                        phoneNumber = member["phone_number"]?.toString(),
                        isActive = member["is_active"] as Boolean,
                        createdAt = member["created_at"] as Timestamp
                    )
                } } ?: emptyList(),
                courses = coursesMap[projectId]?.map { course ->
                    CourseInfo(
                        id = UUID.fromString(course["id"].toString()),
                        name = course["name"].toString(),
                        code = course["code"].toString()
                    )
                } ?: emptyList(),
                reviewCount = (data["review_count"] as? Number)?.toInt() ?: 0,
                createdAt = data["created_at"] as Timestamp,
                updatedAt = data["updated_at"] as Timestamp
            )
        }
    }
}
