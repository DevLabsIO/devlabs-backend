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
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, Any>> {
        return try {
            if (file.isEmpty) {
                return ResponseEntity.badRequest().body(mapOf("error" to "File is empty"))
            }

            val objectName = minioService.uploadFile(file)
            val displayName = minioService.getDisplayName(objectName)
            val objectUrl = minioService.getObjectUrl(objectName)
            
            ResponseEntity.ok(
                mapOf(
                    "objectName" to objectName,
                    "fileName" to displayName,
                    "url" to objectUrl
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to e.message.toString()))
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
    fun listFiles(): ResponseEntity<Map<String, Any>> {
        return try {
            val files = minioService.listFiles()
            ResponseEntity.ok(
                mapOf(
                    "files" to files,
                    "count" to files.size
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to list files: ${e.message}"))
        }
    }

    @GetMapping("/download-zip")
    fun downloadAllAsZip(): ResponseEntity<Any> {
        return try {
            val zipStream = minioService.downloadAllAsZip()
            ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"files.zip\"")
                .header("Content-Type", "application/zip")
                .body(InputStreamResource(zipStream))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to download files: ${e.message}"))
        }
    }

    @DeleteMapping("/delete-all")
    fun deleteAllFiles(): ResponseEntity<Map<String, Any>> {
        return try {
            val deletedCount = minioService.deleteAllFiles()
            ResponseEntity.ok(
                mapOf(
                    "message" to "All files deleted successfully",
                    "deletedFiles" to deletedCount
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to delete files: ${e.message}"))
        }
    }

    @GetMapping("/file-info")
    fun getFileInfo(@RequestParam("objectName") objectName: String): ResponseEntity<Map<String, Any>> {
        return try {
            val fileUrl = minioService.getObjectUrl(objectName)
            ResponseEntity.ok(
                mapOf(
                    "objectName" to objectName,
                    "fileName" to minioService.getDisplayName(objectName),
                    "url" to fileUrl
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get file info: ${e.message}"))
        }
    }
}
