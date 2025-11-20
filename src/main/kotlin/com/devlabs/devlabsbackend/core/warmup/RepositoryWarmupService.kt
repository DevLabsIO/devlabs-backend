package com.devlabs.devlabsbackend.core.warmup

import com.devlabs.devlabsbackend.batch.repository.BatchRepository
import com.devlabs.devlabsbackend.course.repository.CourseRepository
import com.devlabs.devlabsbackend.department.repository.DepartmentRepository
import com.devlabs.devlabsbackend.project.repository.ProjectRepository
import com.devlabs.devlabsbackend.review.repository.ReviewRepository
import com.devlabs.devlabsbackend.semester.repository.SemesterRepository
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RepositoryWarmupService(
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
    private val projectRepository: ProjectRepository,
    private val reviewRepository: ReviewRepository,
    private val departmentRepository: DepartmentRepository,
    private val semesterRepository: SemesterRepository,
    private val batchRepository: BatchRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    fun warmupRepositories() {
        logger.info("🔥 Warming up repositories...")
        val startTime = System.currentTimeMillis()
        
        try {
            userRepository.count()
            courseRepository.count()
            projectRepository.count()
            reviewRepository.count()
            departmentRepository.count()
            semesterRepository.count()
            batchRepository.count()
            
            userRepository.findTop5ByOrderByCreatedAtDesc()
            courseRepository.countByActiveSemesters()
            
            val duration = System.currentTimeMillis() - startTime
            logger.info("✅ Repositories warmed up in ${duration}ms")
        } catch (e: Exception) {
            logger.error("❌ Failed to warmup repositories", e)
        }
    }
}
