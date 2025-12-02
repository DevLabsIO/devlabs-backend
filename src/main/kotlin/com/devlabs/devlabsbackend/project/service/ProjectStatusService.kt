package com.devlabs.devlabsbackend.project.service

import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.core.pagination.PaginationInfo
import com.devlabs.devlabsbackend.project.domain.ProjectStatus
import com.devlabs.devlabsbackend.project.domain.dto.ProjectResponse
import com.devlabs.devlabsbackend.project.domain.dto.toProjectResponse
import com.devlabs.devlabsbackend.project.repository.ProjectRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.util.*

@Service
class ProjectStatusService(
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository
) {
    
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.PROJECT_DETAIL], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_LIST], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_ARCHIVE_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.TEAM_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
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
    
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.PROJECT_DETAIL], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_LIST], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_ARCHIVE_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.TEAM_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
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
    
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.PROJECT_DETAIL], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_LIST], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_ARCHIVE_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.TEAM_DETAIL_CACHE], allEntries = true)
        ]
    )
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
    
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.PROJECT_DETAIL], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_LIST], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_ARCHIVE_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.TEAM_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
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
    
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.PROJECT_DETAIL], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_LIST], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_ARCHIVE_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.TEAM_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
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
    
    @Cacheable(
        value = [CacheConfig.PROJECTS_ARCHIVE_CACHE],
        key = "'archived-' + #userId + '-' + #page + '-' + #size + '-' + #sortBy + '-' + #sortOrder"
    )
    @Transactional(readOnly = true)
    fun getArchivedProjects(userId: String, page: Int = 0, size: Int = 10, sortBy: String = "updatedAt", sortOrder: String = "desc"): PaginatedResponse<ProjectResponse> {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val sort = createSort(sortBy, sortOrder)
        val pageable: Pageable = PageRequest.of(page, size, sort)

        val projectIdsPage = when (user.role) {
            Role.ADMIN, Role.MANAGER -> {
                projectRepository.findIdsByStatusOrderByUpdatedAtDesc(ProjectStatus.COMPLETED, pageable)
            }
            Role.FACULTY -> {
                projectRepository.findCompletedProjectIdsByFaculty(user, pageable)
            }
            Role.STUDENT -> {
                projectRepository.findCompletedProjectIdsByStudent(user, pageable)
            }
        }
        
        if (projectIdsPage.content.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page,
                    per_page = size,
                    total_pages = projectIdsPage.totalPages,
                    total_count = projectIdsPage.totalElements.toInt()
                )
            )
        }
        
        val projectIds = projectIdsPage.content.map { it[0] as UUID }
        val projects = projectRepository.findAllById(projectIds)
        val projectsMap = projects.associateBy { it.id }
        val orderedProjects = projectIds.mapNotNull { projectsMap[it] }

        return PaginatedResponse(
            data = orderedProjects.map { it.toProjectResponse() },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = projectIdsPage.totalPages,
                total_count = projectIdsPage.totalElements.toInt()
            )
        )
    }
    
    @Transactional(readOnly = true)
    fun searchArchivedProjects(userId: String, query: String, page: Int = 0, size: Int = 10, sortBy: String = "updatedAt", sortOrder: String = "desc"): PaginatedResponse<ProjectResponse> {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val sort = createSort(sortBy, sortOrder)
        val pageable: Pageable = PageRequest.of(page, size, sort)

        val projectIdsPage = when (user.role) {
            Role.ADMIN, Role.MANAGER -> {
                projectRepository.searchCompletedProjectIds(query, pageable)
            }
            Role.FACULTY -> {
                projectRepository.searchCompletedProjectIdsByFaculty(user, query, pageable)
            }
            Role.STUDENT -> {
                projectRepository.searchCompletedProjectIdsByStudent(user, query, pageable)
            }
        }
        
        if (projectIdsPage.content.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page,
                    per_page = size,
                    total_pages = projectIdsPage.totalPages,
                    total_count = projectIdsPage.totalElements.toInt()
                )
            )
        }
        
        val projectIds = projectIdsPage.content.map { it[0] as UUID }
        val projects = projectRepository.findAllById(projectIds)
        val projectsMap = projects.associateBy { it.id }
        val orderedProjects = projectIds.mapNotNull { projectsMap[it] }

        return PaginatedResponse(
            data = orderedProjects.map { it.toProjectResponse() },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = projectIdsPage.totalPages,
                total_count = projectIdsPage.totalElements.toInt()
            )
        )
    }
    
    private fun createSort(sortBy: String, sortOrder: String): Sort {
        val direction = if (sortOrder.lowercase() == "desc") Sort.Direction.DESC else Sort.Direction.ASC
        return Sort.by(direction, sortBy)
    }
}
