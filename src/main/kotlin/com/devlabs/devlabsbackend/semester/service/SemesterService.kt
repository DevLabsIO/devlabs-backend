package com.devlabs.devlabsbackend.semester.service

import com.devlabs.devlabsbackend.batch.repository.BatchRepository
import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.core.pagination.PaginationInfo
import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.course.domain.dto.CourseResponse
import com.devlabs.devlabsbackend.course.domain.dto.CreateCourseRequest
import com.devlabs.devlabsbackend.course.repository.CourseRepository
import com.devlabs.devlabsbackend.semester.domain.Semester
import com.devlabs.devlabsbackend.semester.domain.dto.CreateSemesterRequest
import com.devlabs.devlabsbackend.semester.domain.dto.SemesterResponse
import com.devlabs.devlabsbackend.semester.domain.dto.UpdateSemesterDTO
import com.devlabs.devlabsbackend.semester.repository.SemesterRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class SemesterService(
    val semesterRepository: SemesterRepository,
    private val courseRepository: CourseRepository,
    private val batchRepository: BatchRepository,
    private val userRepository: UserRepository,
) {

    @Cacheable(
        value = ["semesters-list"],
        key = "'semesters-' + #page + '-' + #size + '-' + #sortBy + '-' + #sortOrder",
        condition = "#page == 0 && #size == 10"
    )
    fun getAllSemestersPaginated(page: Int, size: Int, sortBy: String = "name", sortOrder: String = "asc"): PaginatedResponse<SemesterResponse> {
        val actualSortOrder = sortOrder.uppercase()
        val offset = page * size
        
        val ids = semesterRepository.findSemesterIdsOnly(sortBy, actualSortOrder, offset, size)
        val totalCount = semesterRepository.countAllSemesters()

        if (ids.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page + 1,
                    per_page = size,
                    total_pages = 0,
                    total_count = 0
                )
            )
        }

        val semestersData = semesterRepository.findSemesterListData(ids)
        val semesterResponses = semestersData.map { mapToSemesterResponse(it) }

        return PaginatedResponse(
            data = semesterResponses,
            pagination = PaginationInfo(
                current_page = page + 1,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt()
            )
        )
    }

    fun searchSemesterPaginated(query: String, page: Int, size: Int, sortBy: String = "name", sortOrder: String = "asc"): PaginatedResponse<SemesterResponse> {
        val actualSortOrder = sortOrder.uppercase()
        val offset = page * size
        
        val ids = semesterRepository.searchSemesterIdsOnly(query, sortBy, actualSortOrder, offset, size)
        val totalCount = semesterRepository.countSearchSemesters(query)

        if (ids.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page + 1,
                    per_page = size,
                    total_pages = 0,
                    total_count = 0
                )
            )
        }

        val semestersData = semesterRepository.findSemesterListData(ids)
        val semesterResponses = semestersData.map { mapToSemesterResponse(it) }

        return PaginatedResponse(
            data = semesterResponses,
            pagination = PaginationInfo(
                current_page = page + 1,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt()
            )
        )
    }

    @Caching(
        evict = [
            CacheEvict(value = ["semesters-list"], allEntries = true),
            CacheEvict(value = ["semester-detail"], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER], allEntries = true)
        ]
    )
    fun createSemester(request: CreateSemesterRequest): SemesterResponse {
        val semester = Semester(
            name = request.name,
            year = request.year,
            isActive = request.isActive
        )
        val savedSemester = semesterRepository.save(semester)
        return savedSemester.toSemesterResponse()
    }

    @Cacheable(value = [CacheConfig.SEMESTER_DETAIL_CACHE], key = "'semester-active-' + #instructorId")
    fun getAllActiveSemesters(instructorId: String? = null): List<SemesterResponse> {
        if (instructorId != null) {
            val ids = semesterRepository.findActiveSemesterIdsByInstructor(instructorId)
            if (ids.isEmpty()) {
                return emptyList()
            }
            val semestersData = semesterRepository.findSemesterListData(ids)
            val semestersByIdMap = semestersData.associateBy { UUID.fromString(it["id"].toString()) }
            return ids.mapNotNull { id ->
                semestersByIdMap[id]?.let { mapToSemesterResponse(it) }
            }
        } else {
            val semestersData = semesterRepository.findActiveSemestersNative()
            return semestersData.map { mapToSemesterResponse(it) }
        }
    }

    @Cacheable(value = [CacheConfig.SEMESTER_DETAIL_CACHE], key = "'faculty-semesters-' + #facultyId")
    fun getFacultyAssignedSemesters(facultyId: String): List<SemesterResponse> {
        val user = userRepository.findById(facultyId).orElseThrow {
            NotFoundException("User with id $facultyId not found")
        }

        return when (user.role) {
            Role.FACULTY -> {
                getAllActiveSemesters()
            }
            Role.ADMIN, Role.MANAGER -> {
                getAllActiveSemesters()
            }
            else -> {
                emptyList()
            }
        }
    }

    @Cacheable(value = [CacheConfig.SEMESTER_DETAIL_CACHE], key = "'semester-' + #semesterId")
    fun getSemesterById(semesterId: UUID): SemesterResponse {
        val semester = semesterRepository.findById(semesterId)
            .orElseThrow { NotFoundException("Semester not found with id: $semesterId") }
        return semester.toSemesterResponse()
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.SEMESTERS_LIST_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.SEMESTER_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER], allEntries = true),
            CacheEvict(value = [CacheConfig.COURSES_ACTIVE_CACHE, CacheConfig.COURSES_USER_CACHE], allEntries = true)
        ]
    )
    fun updateSemester(semesterId: UUID, request: UpdateSemesterDTO): SemesterResponse {
        val semester = semesterRepository.findById(semesterId)
            .orElseThrow { NotFoundException("Semester not found with id: $semesterId") }

        request.name?.let { semester.name = it }
        request.year?.let { semester.year = it }
        request.isActive?.let { semester.isActive = it }

        val updatedSemester = semesterRepository.save(semester)
        return updatedSemester.toSemesterResponse()
    }

    @Caching(
        evict = [
            CacheEvict(value = ["semesters-list"], allEntries = true),
            CacheEvict(value = ["semester-detail"], allEntries = true)
        ]
    )
    fun deleteSemester(semesterId: UUID) {
        val semester = semesterRepository.findById(semesterId)
            .orElseThrow { NotFoundException("Semester not found with id: $semesterId") }

        if (semester.isActive) {
            throw IllegalStateException("Cannot delete an active semester")
        }

        if (courseRepository.existsBySemester(semester)) {
            throw IllegalStateException("Cannot delete semester with associated courses")
        }

        if (batchRepository.existsBySemester(semester)) {
            throw IllegalStateException("Cannot delete semester with associated batches")
        }
        semesterRepository.delete(semester)
    }

    private fun createSort(sortBy: String, sortOrder: String): Sort {
        val direction = if (sortOrder.lowercase() == "desc") Sort.Direction.DESC else Sort.Direction.ASC
        return Sort.by(direction, sortBy)
    }

    @Caching(
        evict = [
            CacheEvict(value = ["semesters-list"], allEntries = true),
            CacheEvict(value = ["semester-detail"], allEntries = true),
            CacheEvict(value = ["courses-list"], allEntries = true),
            CacheEvict(value = [CacheConfig.SEMESTER_DETAIL_CACHE, CacheConfig.SEMESTERS_LIST_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.COURSES_ACTIVE_CACHE, CacheConfig.COURSES_USER_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
    fun createCourseForSemester(semesterId: UUID, courseRequest: CreateCourseRequest): CourseResponse {
        val semester = semesterRepository.findById(semesterId).orElseThrow {
            NotFoundException("Semester with id $semesterId not found")
        }
        val course = Course(
            name = courseRequest.name,
            code = courseRequest.code,
            description = courseRequest.description,
            type = courseRequest.type,
            semester = semester
        )
        val savedCourse = courseRepository.save(course)
        return com.devlabs.devlabsbackend.course.domain.dto.CourseResponse(
            id = savedCourse.id!!,
            name = savedCourse.name,
            code = savedCourse.code,
            description = savedCourse.description
        )
    }

    @Caching(
        evict = [
            CacheEvict(value = ["semesters-list"], allEntries = true),
            CacheEvict(value = ["semester-detail"], allEntries = true),
            CacheEvict(value = ["courses-list"], allEntries = true),
            CacheEvict(value = [CacheConfig.SEMESTER_DETAIL_CACHE, CacheConfig.SEMESTERS_LIST_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.COURSES_ACTIVE_CACHE, CacheConfig.COURSES_USER_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
    fun deleteCourseFromSemester(semesterId: UUID, courseId: UUID): CourseResponse {
        val semester = semesterRepository.findById(semesterId).orElseThrow {
            NotFoundException("Semester with id $semesterId not found")
        }

        val course = courseRepository.findById(courseId).orElseThrow {
            NotFoundException("Course with id $courseId not found")
        }

        if (course.semester?.id != semester.id) {
            throw IllegalArgumentException("Course with id $courseId does not belong to semester with id $semesterId")
        }

        val courseResponse = CourseResponse(
            id = course.id!!,
            name = course.name,
            code = course.code,
            description = course.description
        )

        courseRepository.delete(course)
        return courseResponse
    }

    @Cacheable(value = [CacheConfig.SEMESTER_DETAIL_CACHE], key = "'semester-courses-' + #semesterId")
    @Transactional(readOnly = true)
    fun getCoursesBySemesterId(semesterId: UUID): List<CourseResponse> {
        if (!semesterRepository.existsById(semesterId)) {
            throw NotFoundException("Semester with id $semesterId not found")
        }

        val coursesData = semesterRepository.findCoursesBySemesterId(semesterId)
        return coursesData.map { data ->
            CourseResponse(
                id = UUID.fromString(data["id"].toString()),
                name = data["name"].toString(),
                code = data["code"]?.toString() ?: "",
                description = data["description"]?.toString() ?: ""
            )
        }
    }

    private fun mapToSemesterResponse(data: Map<String, Any>): SemesterResponse {
        return SemesterResponse(
            id = UUID.fromString(data["id"].toString()),
            name = data["name"].toString(),
            year = (data["year"] as Number).toInt(),
            isActive = data["is_active"] as Boolean
        )
    }
}

fun Semester.toSemesterResponse(): SemesterResponse {
    return SemesterResponse(
        id = this.id!!,
        name = this.name,
        year = this.year,
        isActive = this.isActive
    )
}
