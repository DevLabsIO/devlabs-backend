package com.devlabs.devlabsbackend.kanban.controller

import com.devlabs.devlabsbackend.kanban.domain.dto.CreateTaskRequest
import com.devlabs.devlabsbackend.kanban.domain.dto.KanbanBoardResponse
import com.devlabs.devlabsbackend.kanban.domain.dto.KanbanTaskResponse
import com.devlabs.devlabsbackend.kanban.domain.dto.MoveTaskRequest
import com.devlabs.devlabsbackend.kanban.domain.dto.UpdateTaskRequest
import com.devlabs.devlabsbackend.kanban.service.KanbanService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/kanban")
class KanbanController(
    private val kanbanService: KanbanService
) {
    @GetMapping("/project/{projectId}")
    fun getKanbanBoard(@PathVariable projectId: UUID): ResponseEntity<KanbanBoardResponse> {
        val board = kanbanService.getBoardForProject(projectId)
        return ResponseEntity.ok(board)
    }

    @PostMapping("/project/{projectId}")
    fun createKanbanBoard(@PathVariable projectId: UUID): ResponseEntity<KanbanBoardResponse> {
        val board = kanbanService.createBoardForProject(projectId)
        return ResponseEntity.status(HttpStatus.CREATED).body(board)
    }

    @PostMapping("/tasks")
    fun createTask(@RequestBody request: CreateTaskRequest): ResponseEntity<KanbanTaskResponse> {
        val task = kanbanService.createTask(request, request.userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(task)
    }

    @PutMapping("/tasks/{taskId}")
    fun updateTask(
        @PathVariable taskId: UUID,
        @RequestBody request: UpdateTaskRequest
    ): ResponseEntity<KanbanTaskResponse> {
        val task = kanbanService.updateTask(taskId, request, request.userId)
        return ResponseEntity.ok(task)
    }

    @PutMapping("/tasks/{taskId}/move")
    fun moveTask(
        @PathVariable taskId: UUID,
        @RequestBody request: MoveTaskRequest
    ): ResponseEntity<KanbanTaskResponse> {
        val task = kanbanService.moveTask(taskId, request, request.userId)
        return ResponseEntity.ok(task)
    }

    @DeleteMapping("/tasks/{taskId}")
    fun deleteTask(@PathVariable taskId: UUID): ResponseEntity<Map<String, String>> {
        kanbanService.deleteTask(taskId)
        return ResponseEntity.ok(mapOf("message" to "Task deleted successfully"))
    }

    @GetMapping("/tasks/{taskId}")
    fun getTask(@PathVariable taskId: UUID): ResponseEntity<KanbanTaskResponse> {
        val task = kanbanService.getTaskById(taskId)
        return ResponseEntity.ok(task)
    }
}
