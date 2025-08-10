package com.devlabs.devlabsbackend.project.service

import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.core.pagination.PaginationInfo
import com.devlabs.devlabsbackend.course.repository.CourseRepository
import com.devlabs.devlabsbackend.project.domain.DTO.CreateProjectRequest
import com.devlabs.devlabsbackend.project.domain.DTO.ProjectResponse
import com.devlabs.devlabsbackend.project.domain.DTO.UpdateProjectRequest
import com.devlabs.devlabsbackend.project.domain.DTO.toProjectResponse
import com.devlabs.devlabsbackend.project.domain.Project
import com.devlabs.devlabsbackend.project.domain.ProjectStatus
import com.devlabs.devlabsbackend.project.repository.ProjectRepository
import com.devlabs.devlabsbackend.team.repository.TeamRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.repository.UserRepository
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
@Transactional
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val teamRepository: TeamRepository,
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository
) {

//     @CacheEvict(
//        value = [CacheConfig.PROJECT_CACHE],
//        allEntries = true
//    )
    fun createProject(projectData: CreateProjectRequest): Project {
        try {
            val team = teamRepository.findByIdWithMembers(projectData.teamId) ?: throw NotFoundException("Team with id ${projectData.teamId} not found")

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
            return projectRepository.save(project)
        } catch (e: Exception) {
            throw e
        }
    }

//     @Cacheable(
//        value = [CacheConfig.PROJECT_CACHE],
//        key = "'projects_team_' + #teamId + '_' + #page + '_' + #size"
//    )
    fun getProjectsByTeam(teamId: UUID, page: Int = 0, size: Int = 10, sortBy: String = "title", sortOrder: String = "asc"): PaginatedResponse<ProjectResponse> {
        val team = teamRepository.findByIdWithMembers(teamId) ?: throw NotFoundException("Team with id $teamId not found")

        val sort = createSort(sortBy, sortOrder)
        val pageable: Pageable = PageRequest.of(page, size, sort)
        val projectsPage = projectRepository.findByTeamWithRelations(team, pageable)

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

//     @Cacheable(value = [CacheConfig.PROJECT_CACHE], key = "'project_' + #projectId")
    fun getProjectById(projectId: UUID): Project {
        val project = projectRepository.findByIdWithRelations(projectId) ?: throw NotFoundException("Project with id $projectId not found")
        return project
    }

//     @Cacheable(
//        value = [CacheConfig.PROJECT_CACHE],
//        key = "'projects_course_' + #courseId + '_' + #page + '_' + #size + '_' + #sortBy + '_' + #sortOrder"
//    )
    fun getProjectsByCourse(
        courseId: UUID,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "title",
        sortOrder: String = "asc"
    ): PaginatedResponse<ProjectResponse> {
        val course = courseRepository.findByIdWithStudentsAndInstructors(courseId) ?: throw NotFoundException("Course with id $courseId not found")

        val sort = createSort(sortBy, sortOrder)
        val pageable: Pageable = PageRequest.of(page, size, sort)
        val projectsPage = projectRepository.findByCourseWithRelations(course, pageable)

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

//     @CacheEvict(value = [CacheConfig.PROJECT_CACHE], allEntries = true)
    fun updateProject(projectId: UUID, updateData: UpdateProjectRequest, requesterId: String): Project {
        val project = getProjectById(projectId)

        if (project.status !in listOf(ProjectStatus.PROPOSED, ProjectStatus.REJECTED)) {
            throw IllegalArgumentException("Project cannot be edited in current status: ${project.status}")
        }
        updateData.title?.let { project.title = it }
        updateData.description?.let { project.description = it }
        updateData.objectives?.let { project.objectives = it }
        updateData.githubUrl?.let { project.githubUrl = it }

        project.updatedAt = Timestamp.from(Instant.now())

        return projectRepository.save(project)
    }


//     @CacheEvict(value = [CacheConfig.PROJECT_CACHE], allEntries = true)
    fun deleteProject(projectId: UUID, userId: String): Boolean {
        val project = projectRepository.findByIdWithRelations(projectId) ?: throw NotFoundException("Project with id $projectId not found")
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }
        val isTeamMember = project.team.members.any { it.id == userId }
        val isCourseInstructor = project.courses.any { course ->
            course.instructors.any { it.id == userId }
        }
        if (!isTeamMember && !isCourseInstructor) {
            throw IllegalArgumentException("You don't have permission to delete this project")
        }

        projectRepository.delete(project)
        return true
    }

//     @Cacheable(
//        value = [CacheConfig.PROJECT_CACHE],
//        key = "'projects_active_all'"
//    )
    @Transactional
    fun getAllActiveProjects(): List<ProjectResponse> {
        return projectRepository.findByStatusIn(listOf(ProjectStatus.ONGOING, ProjectStatus.PROPOSED))
            .map { it.toProjectResponse() }
    }

//     @Cacheable(
//        value = [CacheConfig.PROJECT_CACHE],
//        key = "'projects_user_course_' + #userId + '_' + #courseId + '_' + #page + '_' + #size"
//    )
    fun getProjectsForUserByCourse(
        userId: String,
        courseId: UUID,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "title",
        sortOrder: String = "asc"
    ): PaginatedResponse<ProjectResponse> {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val course = courseRepository.findById(courseId).orElseThrow {
            NotFoundException("Course with id $courseId not found")
        }

        val projects = projectRepository.findProjectsByUserAndCourseWithRelations(user, course)

        val direction = if (sortOrder.lowercase() == "desc") Sort.Direction.DESC else Sort.Direction.ASC
        val sortedProjects = when (sortBy.lowercase()) {
            "title" -> if (direction == Sort.Direction.ASC) projects.sortedBy { it.title } else projects.sortedByDescending { it.title }
            "status" -> if (direction == Sort.Direction.ASC) projects.sortedBy { it.status } else projects.sortedByDescending { it.status }
            "createdat" -> if (direction == Sort.Direction.ASC) projects.sortedBy { it.createdAt } else projects.sortedByDescending { it.createdAt }
            "updatedat" -> if (direction == Sort.Direction.ASC) projects.sortedBy { it.updatedAt } else projects.sortedByDescending { it.updatedAt }
            else -> projects.sortedBy { it.title }
        }

        val total = sortedProjects.size
        val totalPages = (total + size - 1) / size
        val startIndex = page * size
        val endIndex = minOf(startIndex + size, total)

        val pagedProjects = if (startIndex < total) {
            sortedProjects.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        return PaginatedResponse(
            data = pagedProjects.map { it.toProjectResponse() },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = totalPages,
                total_count = total
            )
        )
    }

//     @Cacheable(
//        value = [CacheConfig.PROJECT_CACHE],
//        key = "'projects_user_' + #userId + '_' + #page + '_' + #size"
//    )
    fun getProjectsForUser(userId: String, page: Int = 0, size: Int = 10, sortBy: String = "title", sortOrder: String = "asc"): PaginatedResponse<ProjectResponse> {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val projects = projectRepository.findProjectsByUserWithRelations(user)

        val direction = if (sortOrder.lowercase() == "desc") Sort.Direction.DESC else Sort.Direction.ASC
        val sortedProjects = when (sortBy.lowercase()) {
            "title" -> if (direction == Sort.Direction.ASC) projects.sortedBy { it.title } else projects.sortedByDescending { it.title }
            "status" -> if (direction == Sort.Direction.ASC) projects.sortedBy { it.status } else projects.sortedByDescending { it.status }
            "createdat" -> if (direction == Sort.Direction.ASC) projects.sortedBy { it.createdAt } else projects.sortedByDescending { it.createdAt }
            "updatedat" -> if (direction == Sort.Direction.ASC) projects.sortedBy { it.updatedAt } else projects.sortedByDescending { it.updatedAt }
            else -> projects.sortedBy { it.title }
        }

        val total = sortedProjects.size
        val totalPages = (total + size - 1) / size
        val startIndex = page * size
        val endIndex = minOf(startIndex + size, total)

        val pagedProjects = if (startIndex < total) {
            sortedProjects.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        return PaginatedResponse(
            data = pagedProjects.map { it.toProjectResponse() },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = totalPages,
                total_count = total
            )
        )
    }

//     @Cacheable(
//        value = [CacheConfig.PROJECT_CACHE],
//        key = "'projects_search_' + #userId + '_' + #courseId + '_' + #query + '_' + #page + '_' + #size"
//    )
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

        val course = courseRepository.findByIdWithStudentsAndInstructors(courseId) ?: throw NotFoundException("Course with id $courseId not found")

        val hasAccess = when (user.role) {
            Role.ADMIN, Role.MANAGER -> true
            Role.FACULTY -> course.instructors.contains(user)
            Role.STUDENT -> course.students.contains(user)
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

//     @Cacheable(
//        value = [CacheConfig.PROJECT_CACHE],
//        key = "'projects_active_semester_' + #semesterId"
//    )
    @Transactional
    fun getActiveProjectsBySemester(semesterId: UUID): List<ProjectResponse> {
        return projectRepository.findActiveProjectsBySemester(semesterId)
            .map { it.toProjectResponse() }
    }

//     @Cacheable(
//        value = [CacheConfig.PROJECT_CACHE],
//        key = "'projects_active_batch_' + #batchId"
//    )
    @Transactional
    fun getActiveProjectsByBatch(batchId: UUID): List<ProjectResponse> {
        return projectRepository.findActiveProjectsByBatch(batchId)
            .map { it.toProjectResponse() }
    }

//     @Cacheable(
//        value = [CacheConfig.PROJECT_CACHE],
//        key = "'projects_active_faculty_' + #facultyId"
//    )
    fun getActiveProjectsByFaculty(facultyId: String): List<ProjectResponse> {
        val faculty =
            userRepository.findById(facultyId).orElseThrow { NotFoundException("Faculty with id $facultyId not found") }
        val projects = projectRepository.findActiveProjectsByFaculty(facultyId)
        return projects.map { it.toProjectResponse() }
    }

//     @CacheEvict(value = [CacheConfig.PROJECT_CACHE], allEntries = true)
    @Transactional
    fun approveProject(projectId: UUID, userId: String) {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User not found")
        }
        if (user.role != Role.ADMIN && user.role != Role.MANAGER && user.role != Role.FACULTY) {
            throw IllegalArgumentException("U are not authorized to approve projects")
        }
        val project = projectRepository.findById(projectId).orElseThrow {
            NotFoundException("Project with id $projectId not found")
        }
        if (project.status == ProjectStatus.COMPLETED) {
            throw IllegalArgumentException("Project cannot be approved in current status: ${project.status}")
        }
        project.status = ProjectStatus.ONGOING
        project.updatedAt = Timestamp.from(Instant.now())
        projectRepository.save(project)
    }

//     @CacheEvict(value = [CacheConfig.PROJECT_CACHE], allEntries = true)
    @Transactional
    fun rejectProject(projectId: UUID, userId: String) {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User not found")
        }
        if (user.role != Role.ADMIN && user.role != Role.MANAGER && user.role != Role.FACULTY) {
            throw IllegalArgumentException("U are not authorized to reject projects")
        }
        val project = projectRepository.findById(projectId).orElseThrow {
            NotFoundException("Project with id $projectId not found")
        }
        if (project.status == ProjectStatus.COMPLETED) {
            throw IllegalArgumentException("Project cannot be rejected in current status: ${project.status}")
        }
        project.status = ProjectStatus.REJECTED
        project.updatedAt = Timestamp.from(Instant.now())
        projectRepository.save(project)
    }

//     @CacheEvict(value = [CacheConfig.PROJECT_CACHE], allEntries = true)
    @Transactional
    fun reProposeProject(projectId: UUID, userId: String) {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User not found")
        }
        val project = projectRepository.findById(projectId).orElseThrow {
            NotFoundException("Project with id $projectId not found")
        }
        if (project.status != ProjectStatus.REJECTED) {
            throw IllegalArgumentException("Project cannot be re-proposed in current status: ${project.status}")
        }
        project.status = ProjectStatus.PROPOSED
        project.updatedAt = Timestamp.from(Instant.now())
        projectRepository.save(project)
    }

//     @CacheEvict(value = [CacheConfig.PROJECT_CACHE], allEntries = true)
    @Transactional
    fun completeProject(projectId: UUID, userId: String) {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User not found")
        }
        val project = projectRepository.findById(projectId).orElseThrow {
            NotFoundException("Project with id $projectId not found")
        }
        if (project.status != ProjectStatus.ONGOING) {
            throw IllegalArgumentException("Project cannot be completed in current status: ${project.status}")
        }
        project.status = ProjectStatus.COMPLETED
        project.updatedAt = Timestamp.from(Instant.now())
        projectRepository.save(project)
    }

//     @CacheEvict(value = [CacheConfig.PROJECT_CACHE], allEntries = true)
    @Transactional
    fun revertProjectCompletion(projectId: UUID, userId: String) {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User not found")
        }
        val project = projectRepository.findById(projectId).orElseThrow {
            NotFoundException("Project with id $projectId not found")
        }
        if (project.status != ProjectStatus.COMPLETED) {
            throw IllegalArgumentException("Project cannot be reverted in current status: ${project.status}")
        }
        project.status = ProjectStatus.ONGOING
        project.updatedAt = Timestamp.from(Instant.now())
        projectRepository.save(project)
    }

    @Transactional(readOnly = true)
    fun getArchivedProjects(userId: String, page: Int = 0, size: Int = 10, sortBy: String = "title", sortOrder: String = "asc"): PaginatedResponse<ProjectResponse> {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val sort = createSort(sortBy, sortOrder)
        val pageable: Pageable = PageRequest.of(page, size, sort)

        val projectsPage = when (user.role) {
            Role.ADMIN, Role.MANAGER -> {
                projectRepository.findByStatusWithRelationsOrderByUpdatedAtDesc(ProjectStatus.COMPLETED, pageable)
            }
            Role.FACULTY -> {
                projectRepository.findCompletedProjectsByFacultyWithRelations(user, pageable)
            }
            Role.STUDENT -> {
                projectRepository.findCompletedProjectsByStudentWithRelations(user, pageable)
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

    @Transactional(readOnly = true)
    fun searchArchivedProjects(userId: String, query: String, page: Int = 0, size: Int = 10, sortBy: String = "title", sortOrder: String = "asc"): PaginatedResponse<ProjectResponse> {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val sort = createSort(sortBy, sortOrder)
        val pageable: Pageable = PageRequest.of(page, size, sort)

        val projectsPage = when (user.role) {
            Role.ADMIN, Role.MANAGER -> {
                projectRepository.searchCompletedProjectsWithRelations(query, pageable)
            }
            Role.FACULTY -> {
                projectRepository.searchCompletedProjectsByFacultyWithRelations(user, query, pageable)
            }
            Role.STUDENT -> {
                projectRepository.searchCompletedProjectsByStudentWithRelations(user, query, pageable)
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

    private fun createSort(sortBy: String, sortOrder: String): Sort {
        val direction = if (sortOrder.lowercase() == "desc") Sort.Direction.DESC else Sort.Direction.ASC
        return Sort.by(direction, sortBy)
    }
}