package com.devlabs.devlabsbackend.minio.service

import io.minio.*
import io.minio.http.Method
import io.minio.messages.DeleteObject
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Service
class MinioService(
    private val minioClient: MinioClient,
    @Qualifier("minioBucketName") private val bucketName: String
) {
    private val allowedFileTypes = listOf(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf", "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain", "application/zip", "application/x-rar-compressed"
    )

    fun uploadFile(file: MultipartFile, customName: String? = null, directoryPath: String? = null): String {
        if (!isValidFileType(file.contentType)) {
            throw IllegalArgumentException("File type not allowed: ${file.contentType}")
        }

        val fileName = if (customName.isNullOrBlank()) {
            UUID.randomUUID().toString() + "_" + file.originalFilename?.replace(" ", "_")
        } else {
            generateUniqueObjectName(customName, directoryPath)
        }

        val objectName = if (directoryPath.isNullOrBlank()) {
            fileName
        } else {
            "${directoryPath.trim('/')}/$fileName"
        }

        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .stream(file.inputStream, file.size, -1)
                .contentType(file.contentType)
                .build()
        )

        return objectName
    }

    private fun generateUniqueObjectName(customName: String, directoryPath: String? = null): String {
        val sanitizedName = customName.replace(" ", "_")
        
        val fullPath = if (directoryPath.isNullOrBlank()) {
            sanitizedName
        } else {
            "${directoryPath.trim('/')}/$sanitizedName"
        }

        val exists = objectExists(fullPath)

        return if (exists) {
            "$sanitizedName-${UUID.randomUUID()}"
        } else {
            sanitizedName
        }
    }

    private fun objectExists(objectName: String): Boolean {
        return try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .build()
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deleteFile(objectName: String) {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .build()
        )
    }

    private fun isValidFileType(contentType: String?): Boolean {
        if (contentType == null) return false
        return allowedFileTypes.contains(contentType)
    }

    fun getFile(objectName: String): InputStream {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .build()
        )
    }

    fun getObjectUrl(objectName: String, expirySeconds: Int = 7 * 24 * 3600): String {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .method(Method.GET)
                .expiry(expirySeconds)
                .build()
        )
    }

    fun listFiles(directoryPath: String): List<Map<String, Any>> {
        val prefix = if (directoryPath.isBlank()) "" else "${directoryPath.trim('/')}/"
        
        val objects = minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .recursive(true)
                .build()
        )

        return objects.map { item ->
            val result = item.get()
            mapOf(
                "objectName" to result.objectName(),
                "size" to result.size(),
                "lastModified" to result.lastModified().toString(),
                "etag" to result.etag(),
                "fileName" to result.objectName().substringAfterLast('/'),
                "directory" to result.objectName().substringBeforeLast('/', "")
            )
        }.toList()
    }

    fun downloadDirectoryAsZip(directoryPath: String): InputStream {
        val prefix = if (directoryPath.isBlank()) "" else "${directoryPath.trim('/')}/"
        
        val objects = minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .recursive(true)
                .build()
        )

        val tempZipFile = java.io.File.createTempFile("minio-download", ".zip")
        val zipOutputStream = ZipOutputStream(FileOutputStream(tempZipFile))

        try {
            objects.forEach { item ->
                val result = item.get()
                val objectStream = minioClient.getObject(
                    GetObjectArgs.builder()
                        .bucket(bucketName)
                        .`object`(result.objectName())
                        .build()
                )

                val fileName = result.objectName().substringAfterLast('/')
                val zipEntry = ZipEntry(fileName)
                zipOutputStream.putNextEntry(zipEntry)
                
                objectStream.copyTo(zipOutputStream)
                objectStream.close()
                zipOutputStream.closeEntry()
            }
        } finally {
            zipOutputStream.close()
        }

        return FileInputStream(tempZipFile)
    }

    fun deleteDirectory(directoryPath: String): Int {
        val prefix = if (directoryPath.isBlank()) "" else "${directoryPath.trim('/')}/"
        
        val objects = minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .recursive(true)
                .build()
        )

        val objectNames = objects.map { it.get().objectName() }.toList()
        
        if (objectNames.isNotEmpty()) {
            val deleteObjects = objectNames.map { 
                DeleteObject(it) 
            }
            
            minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                    .bucket(bucketName)
                    .objects(deleteObjects)
                    .build()
            )
        }

        return objectNames.size
    }
}
