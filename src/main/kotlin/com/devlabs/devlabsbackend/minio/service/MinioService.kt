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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Service
class MinioService(
    private val minioClient: MinioClient,
    @Qualifier("minioBucketNameBean") private val bucketName: String
) {
    private val allowedFileTypes = listOf(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf", "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain", "application/zip", "application/x-rar-compressed"
    )

    fun uploadFile(file: MultipartFile): String {
        if (!isValidFileType(file.contentType)) {
            throw IllegalArgumentException("File type not allowed: ${file.contentType}")
        }

        val originalName = (file.originalFilename ?: "file").replace(" ", "_")
        val timestamp = System.nanoTime()
        val objectName = "${timestamp}_$originalName"

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

    fun getDisplayName(objectName: String): String {
        val underscoreIndex = objectName.indexOf('_')
        if (underscoreIndex == -1) return objectName
        val prefix = objectName.substring(0, underscoreIndex)
        return if (prefix.all { it.isDigit() }) {
            objectName.substring(underscoreIndex + 1)
        } else {
            objectName
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

    fun listFiles(): List<Map<String, Any>> {
        val objects = minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(bucketName)
                .recursive(true)
                .build()
        )

        return objects.map { item ->
            val result = item.get()
            mapOf(
                "objectName" to result.objectName(),
                "fileName" to getDisplayName(result.objectName()),
                "size" to result.size(),
                "lastModified" to result.lastModified().toString()
            )
        }.toList()
    }

    fun downloadAllAsZip(): InputStream {
        val objects = minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(bucketName)
                .recursive(true)
                .build()
        )

        val tempZipFile = java.io.File.createTempFile("minio-download", ".zip")

        try {
            ZipOutputStream(FileOutputStream(tempZipFile)).use { zipOutputStream ->
                objects.forEach { item ->
                    val result = item.get()
                    minioClient.getObject(
                        GetObjectArgs.builder()
                            .bucket(bucketName)
                            .`object`(result.objectName())
                            .build()
                    ).use { objectStream ->
                        val zipEntry = ZipEntry(getDisplayName(result.objectName()))
                        zipOutputStream.putNextEntry(zipEntry)
                        objectStream.copyTo(zipOutputStream)
                        zipOutputStream.closeEntry()
                    }
                }
            }
        } catch (e: Exception) {
            tempZipFile.delete()
            throw e
        }

        return TempFileInputStream(tempZipFile)
    }

    private class TempFileInputStream(private val file: java.io.File) : FileInputStream(file) {
        override fun close() {
            try {
                super.close()
            } finally {
                file.delete()
            }
        }
    }

    fun deleteAllFiles(): Int {
        val objects = minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(bucketName)
                .recursive(true)
                .build()
        )

        val objectNames = objects.map { it.get().objectName() }.toList()

        if (objectNames.isNotEmpty()) {
            val deleteObjects = objectNames.map { DeleteObject(it) }
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
