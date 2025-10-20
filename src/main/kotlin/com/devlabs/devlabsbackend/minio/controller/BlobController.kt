package com.devlabs.devlabsbackend.minio.controller

import com.devlabs.devlabsbackend.minio.service.MinioService
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/blob")
class BlobController(
    private val minioService: MinioService
) {
    @PostMapping("/upload")
    fun uploadFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("customName", required = false) customName: String?,
        @RequestParam("teamId", required = false) teamId: String?,
        @RequestParam("projectId", required = false) projectId: String?,
        @RequestParam("projectName", required = false) projectName: String?,
        @RequestParam("reviewId", required = false) reviewId: String?,
        @RequestParam("reviewName", required = false) reviewName: String?
    ): ResponseEntity<Map<String, Any>> {
        return try {
            if (file.isEmpty) {
                return ResponseEntity.badRequest().body(mapOf("error" to "File is empty"))
            }

            val directoryPath = buildDirectoryPath(teamId, projectId, projectName, reviewId, reviewName)

            val objectName = minioService.uploadFile(file, customName, directoryPath)
            val objectUrl = minioService.getObjectUrl(objectName)
            ResponseEntity.ok(mapOf(
                "objectName" to objectName, 
                "url" to objectUrl,
                "directoryPath" to (directoryPath ?: "root")
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message.toString()))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to upload file: ${e.message}"))
        }
    }

    @PostMapping("/upload-structured")
    fun uploadFileStructured(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("customName", required = false) customName: String?,
        @RequestParam("teamId", required = false) teamId: String?,
        @RequestParam("projectId", required = false) projectId: String?,
        @RequestParam("projectName", required = false) projectName: String?,
        @RequestParam("reviewId", required = false) reviewId: String?,
        @RequestParam("reviewName", required = false) reviewName: String?
    ): ResponseEntity<Map<String, Any>> {
        return try {
            if (file.isEmpty) {
                return ResponseEntity.badRequest().body(mapOf("error" to "File is empty"))
            }

            val context = UploadContext(teamId, projectId, projectName, reviewId, reviewName)
            val directoryPath = context.toDirectoryPath()

            val objectName = minioService.uploadFile(file, customName, directoryPath)
            val objectUrl = minioService.getObjectUrl(objectName)
            
            ResponseEntity.ok(mapOf(
                "objectName" to objectName, 
                "url" to objectUrl,
                "directoryPath" to (directoryPath ?: "root"),
                "context" to mapOf(
                    "teamId" to (teamId ?: ""),
                    "projectId" to (projectId ?: ""),
                    "projectName" to (projectName ?: ""),
                    "reviewId" to (reviewId ?: ""),
                    "reviewName" to (reviewName ?: "")
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message.toString()))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to upload file: ${e.message}"))
        }
    }

    @DeleteMapping("/delete")
    fun deleteFile(@RequestParam("objectName") objectName: String): ResponseEntity<Map<String, String>> {
        return try {
            minioService.deleteFile(objectName)
            ResponseEntity.ok(mapOf("message" to "File deleted successfully"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to delete file: ${e.message}"))
        }
    }

    @GetMapping("/list")
    fun listFiles(
        @RequestParam("teamId", required = false) teamId: String?,
        @RequestParam("projectId", required = false) projectId: String?,
        @RequestParam("projectName", required = false) projectName: String?,
        @RequestParam("reviewId", required = false) reviewId: String?,
        @RequestParam("reviewName", required = false) reviewName: String?
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val directoryPath = buildDirectoryPath(teamId, projectId, projectName, reviewId, reviewName)
            val files = minioService.listFiles(directoryPath ?: "")
            
            ResponseEntity.ok(mapOf(
                "files" to files,
                "directoryPath" to (directoryPath ?: "root"),
                "count" to files.size
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to list files: ${e.message}"))
        }
    }

    @GetMapping("/download-zip")
    fun downloadDirectoryAsZip(
        @RequestParam("teamId", required = false) teamId: String?,
        @RequestParam("projectId", required = false) projectId: String?,
        @RequestParam("projectName", required = false) projectName: String?,
        @RequestParam("reviewId", required = false) reviewId: String?,
        @RequestParam("reviewName", required = false) reviewName: String?
    ): ResponseEntity<Any> {
        return try {
            val directoryPath = buildDirectoryPath(teamId, projectId, projectName, reviewId, reviewName)
            
            if (directoryPath.isNullOrBlank()) {
                return ResponseEntity.badRequest()
                    .body(mapOf("error" to "At least teamId must be provided"))
            }

            val zipStream = minioService.downloadDirectoryAsZip(directoryPath)
            val zipFileName = "${directoryPath.replace("/", "-")}.zip"

            ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"$zipFileName\"")
                .header("Content-Type", "application/zip")
                .body(InputStreamResource(zipStream))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to download files: ${e.message}"))
        }
    }

    @DeleteMapping("/delete-directory")
    fun deleteDirectory(
        @RequestParam("teamId", required = false) teamId: String?,
        @RequestParam("projectId", required = false) projectId: String?,
        @RequestParam("projectName", required = false) projectName: String?,
        @RequestParam("reviewId", required = false) reviewId: String?,
        @RequestParam("reviewName", required = false) reviewName: String?
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val directoryPath = buildDirectoryPath(teamId, projectId, projectName, reviewId, reviewName)
            
            if (directoryPath.isNullOrBlank()) {
                return ResponseEntity.badRequest()
                    .body(mapOf("error" to "At least teamId must be provided"))
            }

            val deletedCount = minioService.deleteDirectory(directoryPath)
            ResponseEntity.ok(mapOf(
                "message" to "Directory deleted successfully",
                "deletedFiles" to deletedCount,
                "directoryPath" to directoryPath
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to delete directory: ${e.message}"))
        }
    }

    @GetMapping("/file-info")
    fun getFileInfo(@RequestParam("objectName") objectName: String): ResponseEntity<Map<String, Any>> {
        return try {
            val fileUrl = minioService.getObjectUrl(objectName)
            
            ResponseEntity.ok(mapOf(
                "objectName" to objectName,
                "url" to fileUrl,
                "fileName" to objectName.substringAfterLast('/'),
                "directory" to objectName.substringBeforeLast('/', "")
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get file info: ${e.message}"))
        }
    }

    private fun buildDirectoryPath(
        teamId: String?,
        projectId: String?,
        projectName: String?,
        reviewId: String?,
        reviewName: String?
    ): String? {
        if (teamId.isNullOrBlank()) return null

        return buildString {
            append(teamId.trim())

            if (!projectId.isNullOrBlank()) {
                append("/")
                append(projectId.trim())
                
                if (!projectName.isNullOrBlank()) {
                    append("-")
                    append(projectName.trim().replace(" ", "_"))
                }

                if (!reviewId.isNullOrBlank()) {
                    append("/")
                    append(reviewId.trim())
                    
                    if (!reviewName.isNullOrBlank()) {
                        append("-")
                        append(reviewName.trim().replace(" ", "_"))
                    }
                }
            }
        }
    }
}

data class UploadContext(
    val teamId: String? = null,
    val projectId: String? = null,
    val projectName: String? = null,
    val reviewId: String? = null,
    val reviewName: String? = null
) {
    fun toDirectoryPath(): String? {
        if (teamId.isNullOrBlank()) return null

        return buildString {
            append(teamId.trim())

            if (!projectId.isNullOrBlank()) {
                append("/")
                append(projectId.trim())
                
                if (!projectName.isNullOrBlank()) {
                    append("-")
                    append(projectName.trim().replace(" ", "_"))
                }

                if (!reviewId.isNullOrBlank()) {
                    append("/")
                    append(reviewId.trim())
                    
                    if (!reviewName.isNullOrBlank()) {
                        append("-")
                        append(reviewName.trim().replace(" ", "_"))
                    }
                }
            }
        }
    }
}
