package com.devlabs.devlabsbackend.rubrics.controller

import com.devlabs.devlabsbackend.core.exception.ForbiddenException
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.rubrics.domain.dto.CreateRubricsRequest
import com.devlabs.devlabsbackend.rubrics.domain.dto.UpdateRubricsRequest
import com.devlabs.devlabsbackend.rubrics.service.RubricsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/rubrics")
class RubricsController(
    private val rubricsService: RubricsService
) {

    @GetMapping
    fun getAllRubrics(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDirection: String
    ): ResponseEntity<Any> {
        return try {
            val response = rubricsService.getAllRubrics(page, size, sortBy, sortDirection)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to retrieve rubrics: ${e.message}"))
        }
    }

    @GetMapping("/search")
    fun searchRubrics(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDirection: String
    ): ResponseEntity<Any> {
        return try {
            val response = rubricsService.searchRubrics(query, page, size, sortBy, sortDirection)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to search rubrics: ${e.message}"))
        }
    }

    @GetMapping("/user/{userId}")
    fun getRubricsByUserCombined(@PathVariable userId: String): ResponseEntity<Any> {
        return try {
            val rubrics = rubricsService.getRubricsByUserCombined(userId)
            ResponseEntity.ok(rubrics)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to retrieve user's rubrics: ${e.message}"))
        }
    }

    @GetMapping("/my/{userId}")
    fun getRubricsByUser(
        @PathVariable userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDirection: String
    ): ResponseEntity<Any> {
        return try {
            val response = rubricsService.getRubricsByUser(userId, page, size, sortBy, sortDirection)
            ResponseEntity.ok(response)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to retrieve user's rubrics: ${e.message}"))
        }
    }

    @GetMapping("/shared")
    fun getSharedRubrics(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDirection: String
    ): ResponseEntity<Any> {
        return try {
            val response = rubricsService.getSharedRubrics(page, size, sortBy, sortDirection)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to retrieve shared rubrics: ${e.message}"))
        }
    }

    @GetMapping("/{id}")
    fun getRubricsById(@PathVariable id: UUID): ResponseEntity<Any> {
        return try {
            val rubrics = rubricsService.getRubricsById(id)
            ResponseEntity.ok(rubrics)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to retrieve rubrics: ${e.message}"))
        }
    }

    @PostMapping
    fun createRubrics(@RequestBody request: CreateRubricsRequest): ResponseEntity<Any> {
        return try {
            val rubrics = rubricsService.createRubrics(request)
            ResponseEntity.status(HttpStatus.CREATED).body(rubrics)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: ForbiddenException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to create rubrics: ${e.message}"))
        }
    }

    @PutMapping("/{id}")
    fun updateRubrics(
        @PathVariable id: UUID,
        @RequestBody request: UpdateRubricsRequest
    ): ResponseEntity<Any> {
        return try {
            val rubrics = rubricsService.updateRubrics(id, request)
            ResponseEntity.ok(rubrics)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: ForbiddenException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to update rubrics: ${e.message}"))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteRubrics(
        @PathVariable id: UUID,
        @RequestParam userId: String
    ): ResponseEntity<Any> {
        return try {
            rubricsService.deleteRubrics(id, userId)
            ResponseEntity.ok(mapOf("success" to true, "message" to "Rubrics deleted successfully"))
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: ForbiddenException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to delete rubrics: ${e.message}"))
        }
    }

    @GetMapping("/all")
    fun getAllRubricsNonPaginated(): ResponseEntity<Any> {
        return try {
            val rubrics = rubricsService.getAllRubricsNonPaginated()
            ResponseEntity.ok(rubrics)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to retrieve rubrics: ${e.message}"))
        }
    }
}
