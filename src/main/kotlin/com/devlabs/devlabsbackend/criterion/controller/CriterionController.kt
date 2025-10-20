package com.devlabs.devlabsbackend.criterion.controller

import com.devlabs.devlabsbackend.core.exception.ForbiddenException
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.criterion.domain.dto.CriterionResponse
import com.devlabs.devlabsbackend.criterion.domain.dto.UserIdRequest
import com.devlabs.devlabsbackend.criterion.service.CriterionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/criterion")
class CriterionController(
    private val criterionService: CriterionService
) {
    @GetMapping("/{id}")
    fun getCriterionById(@PathVariable id: UUID): ResponseEntity<Any> {
        return try {
            val criterion = criterionService.getCriterionById(id)
            ResponseEntity.ok(criterion)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to retrieve criterion: ${e.message}"))
        }
    }

    @GetMapping("/rubrics/{rubricsId}")
    fun getCriteriaByRubricsId(@PathVariable rubricsId: UUID): ResponseEntity<Any> {
        return try {
            val criteria = criterionService.getCriteriaByRubricsId(rubricsId)
            ResponseEntity.ok(criteria)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to retrieve criteria: ${e.message}"))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteCriterion(
        @PathVariable id: UUID,
        @RequestBody request: UserIdRequest
    ): ResponseEntity<Any> {
        return try {
            criterionService.deleteCriterion(id, request.userId)
            ResponseEntity.ok(mapOf("success" to true, "message" to "Criterion deleted successfully"))
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: ForbiddenException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to delete criterion: ${e.message}"))
        }
    }
}
