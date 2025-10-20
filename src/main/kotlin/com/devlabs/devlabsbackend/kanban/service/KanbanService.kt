package com.devlabs.devlabsbackend.kanban.service

import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.ForbiddenException
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.kanban.domain.KanbanBoard
import com.devlabs.devlabsbackend.kanban.domain.KanbanTask
import com.devlabs.devlabsbackend.kanban.domain.dto.*
import com.devlabs.devlabsbackend.kanban.repository.KanbanBoardRepository
import com.devlabs.devlabsbackend.kanban.repository.KanbanColumnRepository
import com.devlabs.devlabsbackend.kanban.repository.KanbanTaskRepository
import com.devlabs.devlabsbackend.project.repository.ProjectRepository
import com.devlabs.devlabsbackend.security.utils.SecurityUtils
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.util.*

@Service
@Transactional
class KanbanService(
    private val kanbanBoardRepository: KanbanBoardRepository,
    private val kanbanColumnRepository: KanbanColumnRepository,
    private val kanbanTaskRepository: KanbanTaskRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository
) {

    @Cacheable(value = [CacheConfig.KANBAN_BOARD], key = "'project_' + #projectId")
    fun getOrCreateBoardForProject(projectId: UUID): KanbanBoardResponse {
        val project = projectRepository.findByIdWithTeamAndMembers(projectId) 
            ?: throw NotFoundException("Project with id $projectId not found")
        
        val board = kanbanBoardRepository.findByProjectWithRelations(project) ?: run {
            val newBoard = KanbanBoard(project = project)
            newBoard.initializeDefaultColumns()
            kanbanBoardRepository.save(newBoard)
            kanbanBoardRepository.findByProjectWithRelations(project)!!
        }

        return board.toBoardResponse()
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.KANBAN_BOARD], key = "'project_' + #request.columnId"),
            CacheEvict(value = [CacheConfig.KANBAN_TASK], key = "#result.id")
        ]
    )
    fun createTask(request: CreateTaskRequest, userId: String): KanbanTaskResponse {
        val column = kanbanColumnRepository.findByIdWithProjectAndTeam(request.columnId)
            ?: throw NotFoundException("Column with id ${request.columnId} not found")
        
        val createdBy = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val isAuthorized = createdBy.role in setOf(Role.ADMIN, Role.MANAGER, Role.FACULTY) ||
                          column.board.project.team.members.contains(createdBy)
        
        if (!isAuthorized) {
            throw ForbiddenException("Only admin, manager, faculty, or team members can create tasks")
        }

        val maxPosition = kanbanTaskRepository.findMaxPositionInColumn(column) ?: -1
        
        val task = KanbanTask(
            title = request.title,
            description = request.description,
            position = maxPosition + 1,
            column = column,
            createdBy = createdBy,
        )
        
        val savedTask = kanbanTaskRepository.save(task)
        evictBoardCache(column.board.project.id!!)
        return savedTask.toTaskResponse()
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.KANBAN_TASK], key = "#taskId")
        ]
    )
    fun updateTask(taskId: UUID, request: UpdateTaskRequest, userId: String): KanbanTaskResponse {
        val task = kanbanTaskRepository.findByIdWithRelations(taskId) 
            ?: throw NotFoundException("Task with id $taskId not found")
        
        val requester = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val isAuthorized = requester.role in setOf(Role.ADMIN, Role.MANAGER, Role.FACULTY) ||
                          task.column.board.project.team.members.contains(requester)
        
        if (!isAuthorized) {
            throw ForbiddenException("Only admin, manager, faculty, or team members can update tasks")
        }
        
        request.title?.let { task.title = it }
        request.description?.let { task.description = it }
        
        task.updatedAt = Timestamp.from(Instant.now())
        
        val savedTask = kanbanTaskRepository.save(task)
        evictBoardCache(task.column.board.project.id!!)
        return savedTask.toTaskResponse()
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.KANBAN_TASK], key = "#taskId")
        ]
    )
    fun moveTask(taskId: UUID, request: MoveTaskRequest, userId: String): KanbanTaskResponse {
        val task = kanbanTaskRepository.findByIdWithRelations(taskId) 
            ?: throw NotFoundException("Task with id $taskId not found")
        
        val newColumn = kanbanColumnRepository.findById(request.columnId).orElseThrow {
            NotFoundException("Column with id ${request.columnId} not found")
        }
        
        val requester = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val isAuthorized = requester.role in setOf(Role.ADMIN, Role.MANAGER, Role.FACULTY) ||
                          task.column.board.project.team.members.contains(requester)
        
        if (!isAuthorized) {
            throw ForbiddenException("Only admin, manager, faculty, or team members can move tasks")
        }

        task.column = newColumn
        task.position = request.position
        task.updatedAt = Timestamp.from(Instant.now())
        
        val savedTask = kanbanTaskRepository.save(task)
        evictBoardCache(task.column.board.project.id!!)
        return savedTask.toTaskResponse()
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.KANBAN_TASK], key = "#taskId")
        ]
    )
    fun deleteTask(taskId: UUID) {
        val task = kanbanTaskRepository.findByIdWithRelations(taskId) 
            ?: throw NotFoundException("Task with id $taskId not found")
        
        val currentUserId = SecurityUtils.getCurrentUserId() 
            ?: throw ForbiddenException("User not authenticated")
        
        val requester = userRepository.findById(currentUserId).orElseThrow {
            NotFoundException("User with id $currentUserId not found")
        }

        val isAuthorized = requester.role in setOf(Role.ADMIN, Role.MANAGER, Role.FACULTY) ||
                          task.column.board.project.team.members.contains(requester)
        
        if (!isAuthorized) {
            throw ForbiddenException("Only admin, manager, faculty, or team members can delete tasks")
        }
        
        val projectId = task.column.board.project.id!!
        kanbanTaskRepository.delete(task)
        evictBoardCache(projectId)
    }

    @Cacheable(value = [CacheConfig.KANBAN_TASK], key = "#taskId")
    fun getTaskById(taskId: UUID): KanbanTaskResponse {
        val task = kanbanTaskRepository.findById(taskId).orElseThrow {
            NotFoundException("Task with id $taskId not found")
        }

        task.createdBy.name
        
        return task.toTaskResponse()
    }

    @CacheEvict(value = [CacheConfig.KANBAN_BOARD], allEntries = true)
    private fun evictBoardCache(@Suppress("UNUSED_PARAMETER") projectId: UUID) {
    }
}
