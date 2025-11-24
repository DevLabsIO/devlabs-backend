package com.devlabs.devlabsbackend.kanban.domain.dto

import com.devlabs.devlabsbackend.kanban.domain.KanbanBoard
import com.devlabs.devlabsbackend.kanban.domain.KanbanColumn
import com.devlabs.devlabsbackend.kanban.domain.KanbanTask
import com.devlabs.devlabsbackend.user.domain.dto.UserResponse
import com.devlabs.devlabsbackend.user.service.toUserResponse
import java.sql.Timestamp
import java.util.*

data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    val columnId: UUID,
    val assignedToId: String? = null,
    val userId: String
)

data class UpdateTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val assignedToId: String? = null,
    val userId: String
)

data class MoveTaskRequest(
    val columnId: UUID,
    val position: Int,
    val userId: String
)

data class KanbanTaskResponse(
    val id: UUID?,
    val title: String,
    val description: String?,
    val position: Int,
    val createdBy: UserResponse,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val projectId: UUID? = null
)

data class KanbanColumnResponse(
    val id: UUID?,
    val name: String,
    val position: Int,
    val tasks: List<KanbanTaskResponse>,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)

data class KanbanBoardResponse(
    val id: UUID?,
    val projectId: UUID?,
    val columns: List<KanbanColumnResponse>,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)

fun KanbanTask.toTaskResponse(): KanbanTaskResponse {
    return KanbanTaskResponse(
        id = this.id,
        title = this.title,
        description = this.description,
        position = this.position,
        createdBy = this.createdBy.toUserResponse(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun KanbanTask.toTaskResponseWithProject(projectId: UUID): KanbanTaskResponse {
    return KanbanTaskResponse(
        id = this.id,
        title = this.title,
        description = this.description,
        position = this.position,
        createdBy = this.createdBy.toUserResponse(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        projectId = projectId
    )
}

fun KanbanColumn.toColumnResponse(): KanbanColumnResponse {
    return KanbanColumnResponse(
        id = this.id,
        name = this.name,
        position = this.position,
        tasks = this.tasks.sortedBy { it.position }.map { it.toTaskResponse() },
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun KanbanBoard.toBoardResponse(): KanbanBoardResponse {
    return KanbanBoardResponse(
        id = this.id,
        projectId = this.project.id,
        columns = this.columns.sortedBy { it.position }.map { it.toColumnResponse() },
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
