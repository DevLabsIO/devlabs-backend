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
    @Transactional(readOnly = true)
    fun getBoardForProject(projectId: UUID): KanbanBoardResponse {
        val project = projectRepository.findById(projectId).orElseThrow {
            NotFoundException("Project with id $projectId not found")
        }
        
        val board = kanbanBoardRepository.findByProject(project)
            ?: throw NotFoundException("Kanban board not found for project $projectId")
        
        val boardWithColumns = kanbanBoardRepository.findByProjectWithColumns(project)!!
        val tasks = kanbanBoardRepository.findTasksByBoard(board)
        
        val tasksByColumnId = tasks.groupBy { it.column.id!! }
        
        val columns = boardWithColumns.columns.sortedBy { it.position }.map { column ->
            KanbanColumnResponse(
                id = column.id,
                name = column.name,
                position = column.position,
                tasks = (tasksByColumnId[column.id] ?: emptyList())
                    .sortedBy { it.position }
                    .map { it.toTaskResponse() },
                createdAt = column.createdAt,
                updatedAt = column.updatedAt
            )
        }
        
        return KanbanBoardResponse(
            id = board.id,
            projectId = board.project.id,
            columns = columns,
            createdAt = board.createdAt,
            updatedAt = board.updatedAt
        )
    }

    @CacheEvict(value = [CacheConfig.KANBAN_BOARD], key = "'project_' + #projectId")
    fun createBoardForProject(projectId: UUID): KanbanBoardResponse {
        val project = projectRepository.findByIdWithTeamAndMembers(projectId) 
            ?: throw NotFoundException("Project with id $projectId not found")
        
        val existingBoard = kanbanBoardRepository.findByProject(project)
        if (existingBoard != null) {
            throw IllegalArgumentException("Kanban board already exists for project $projectId")
        }
        
        val newBoard = KanbanBoard(project = project)
        newBoard.initializeDefaultColumns()
        kanbanBoardRepository.save(newBoard)
        
        return getBoardForProject(projectId)
    }

    @CacheEvict(value = [CacheConfig.KANBAN_BOARD], key = "'project_' + #result.projectId")
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
        val projectId = column.board.project.id!!
        
        val task = KanbanTask(
            title = request.title,
            description = request.description,
            position = maxPosition + 1,
            column = column,
            createdBy = createdBy,
        )
        
        val savedTask = kanbanTaskRepository.save(task)
        return savedTask.toTaskResponseWithProject(projectId)
    }

    @CacheEvict(value = [CacheConfig.KANBAN_BOARD], key = "'project_' + #result.projectId")
    fun updateTask(taskId: UUID, request: UpdateTaskRequest, userId: String): KanbanTaskResponse {
        val task = kanbanTaskRepository.findByIdWithRelations(taskId) 
            ?: throw NotFoundException("Task with id $taskId not found")
        
        val projectId = task.column.board.project.id!!
        
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
        return savedTask.toTaskResponseWithProject(projectId)
    }

    @CacheEvict(value = [CacheConfig.KANBAN_BOARD], key = "'project_' + #result.projectId")
    fun moveTask(taskId: UUID, request: MoveTaskRequest, userId: String): KanbanTaskResponse {
        val task = kanbanTaskRepository.findByIdWithRelations(taskId) 
            ?: throw NotFoundException("Task with id $taskId not found")
        
        val projectId = task.column.board.project.id!!
        
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
        return savedTask.toTaskResponseWithProject(projectId)
    }

    @CacheEvict(value = [CacheConfig.KANBAN_BOARD], key = "'project_' + #result")
    fun deleteTask(taskId: UUID): UUID {
        val task = kanbanTaskRepository.findByIdWithRelations(taskId) 
            ?: throw NotFoundException("Task with id $taskId not found")
        
        val projectId = task.column.board.project.id!!
        
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
        
        kanbanTaskRepository.delete(task)
        return projectId
    }

    @Transactional(readOnly = true)
    fun getTaskById(taskId: UUID): KanbanTaskResponse {
        val task = kanbanTaskRepository.findById(taskId).orElseThrow {
            NotFoundException("Task with id $taskId not found")
        }

        task.createdBy.name
        
        return task.toTaskResponse()
    }
}
