package com.devlabs.devlabsbackend.course.repository

import com.devlabs.devlabsbackend.core.constants.RoleConstants
import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.semester.domain.Semester
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CourseRepository : JpaRepository<Course, UUID> {
    
    @Query(value = """
        SELECT c.id
        FROM course c
        JOIN semester s ON c.semester_id = s.id
        WHERE s.is_active = true
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN c.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN c.name END DESC,
            CASE WHEN :sortBy = 'code' AND :sortOrder = 'ASC' THEN c.code END ASC,
            CASE WHEN :sortBy = 'code' AND :sortOrder = 'DESC' THEN c.code END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findActiveCourseIdsOnly(
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT COUNT(*)
        FROM course c
        JOIN semester s ON c.semester_id = s.id
        WHERE s.is_active = true
    """, nativeQuery = true)
    fun countActiveCourses(): Long

    @Query(value = """
        SELECT c.id
        FROM course c
        JOIN semester s ON c.semester_id = s.id
        JOIN course_instructor ci ON ci.course_id = c.id
        WHERE s.is_active = true AND ci.instructor_id = CAST(:instructorId AS VARCHAR)
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN c.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN c.name END DESC,
            CASE WHEN :sortBy = 'code' AND :sortOrder = 'ASC' THEN c.code END ASC,
            CASE WHEN :sortBy = 'code' AND :sortOrder = 'DESC' THEN c.code END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findActiveCourseIdsByInstructor(
        @Param("instructorId") instructorId: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT COUNT(*)
        FROM course c
        JOIN semester s ON c.semester_id = s.id
        JOIN course_instructor ci ON ci.course_id = c.id
        WHERE s.is_active = true AND ci.instructor_id = CAST(:instructorId AS VARCHAR)
    """, nativeQuery = true)
    fun countActiveCoursesByInstructor(@Param("instructorId") instructorId: String): Long

    @Query(value = """
        SELECT c.id, c.name, c.code, c.description, c.type,
               s.id as semester_id, s.name as semester_name
        FROM course c
        JOIN semester s ON c.semester_id = s.id
        WHERE c.id IN (:ids)
    """, nativeQuery = true)
    fun findCourseListData(@Param("ids") ids: List<UUID>): List<Map<String, Any>>


    @Query(value = """
        SELECT c.id
        FROM course c
        JOIN semester s ON c.semester_id = s.id
        WHERE s.is_active = true
        AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN c.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN c.name END DESC,
            CASE WHEN :sortBy = 'code' AND :sortOrder = 'ASC' THEN c.code END ASC,
            CASE WHEN :sortBy = 'code' AND :sortOrder = 'DESC' THEN c.code END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun searchActiveCourseIdsOnly(
        @Param("query") query: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT COUNT(*)
        FROM course c
        JOIN semester s ON c.semester_id = s.id
        WHERE s.is_active = true
        AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')))
    """, nativeQuery = true)
    fun countSearchActiveCourses(@Param("query") query: String): Long

    @Query(value = """
        SELECT c.id
        FROM course c
        JOIN semester s ON c.semester_id = s.id
        JOIN course_instructor ci ON ci.course_id = c.id
        WHERE s.is_active = true 
        AND ci.instructor_id = CAST(:instructorId AS VARCHAR)
        AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN c.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN c.name END DESC,
            CASE WHEN :sortBy = 'code' AND :sortOrder = 'ASC' THEN c.code END ASC,
            CASE WHEN :sortBy = 'code' AND :sortOrder = 'DESC' THEN c.code END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun searchActiveCourseIdsByInstructor(
        @Param("instructorId") instructorId: String,
        @Param("query") query: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT COUNT(*)
        FROM course c
        JOIN semester s ON c.semester_id = s.id
        JOIN course_instructor ci ON ci.course_id = c.id
        WHERE s.is_active = true 
        AND ci.instructor_id = CAST(:instructorId AS VARCHAR)
        AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')))
    """, nativeQuery = true)
    fun countSearchActiveCoursesByInstructor(
        @Param("instructorId") instructorId: String,
        @Param("query") query: String
    ): Long

    @Query(value = """
        SELECT u.id, u.name, u.email,
               CASE u.role
                   WHEN ${RoleConstants.STUDENT} THEN 'STUDENT'
                   WHEN ${RoleConstants.ADMIN} THEN 'ADMIN'
                   WHEN ${RoleConstants.FACULTY} THEN 'FACULTY'
                   WHEN ${RoleConstants.MANAGER} THEN 'MANAGER'
               END as role,
               u.phone_number, u.is_active
        FROM course_student cs
        JOIN "user" u ON u.id = cs.student_id
        WHERE cs.course_id = :courseId
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN u.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN u.name END DESC,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'ASC' THEN u.email END ASC,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'DESC' THEN u.email END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findStudentsByCourseId(
        @Param("courseId") courseId: UUID,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<Map<String, Any>>
    
    @Query(value = """
        SELECT COUNT(*)
        FROM course_student cs
        WHERE cs.course_id = :courseId
    """, nativeQuery = true)
    fun countStudentsByCourseId(@Param("courseId") courseId: UUID): Long

    @Query(value = """
        SELECT u.id, u.name, u.email,
               CASE u.role
                   WHEN ${RoleConstants.STUDENT} THEN 'STUDENT'
                   WHEN ${RoleConstants.ADMIN} THEN 'ADMIN'
                   WHEN ${RoleConstants.FACULTY} THEN 'FACULTY'
                   WHEN ${RoleConstants.MANAGER} THEN 'MANAGER'
               END as role,
               u.phone_number, u.is_active
        FROM course_student cs
        JOIN "user" u ON u.id = cs.student_id
        WHERE cs.course_id = :courseId
        AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN u.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN u.name END DESC,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'ASC' THEN u.email END ASC,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'DESC' THEN u.email END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun searchStudentsByCourseId(
        @Param("courseId") courseId: UUID,
        @Param("query") query: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<Map<String, Any>>
    
    @Query(value = """
        SELECT COUNT(*)
        FROM course_student cs
        JOIN "user" u ON u.id = cs.student_id
        WHERE cs.course_id = :courseId
        AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
    """, nativeQuery = true)
    fun countSearchStudentsByCourseId(
        @Param("courseId") courseId: UUID,
        @Param("query") query: String
    ): Long

    @Query(value = """
        SELECT b.id, b.name, b.section, b.join_year
        FROM course_batch cb
        JOIN batch b ON b.id = cb.batch_id
        WHERE cb.course_id = :courseId
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN b.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN b.name END DESC,
            CASE WHEN :sortBy = 'section' AND :sortOrder = 'ASC' THEN b.section END ASC,
            CASE WHEN :sortBy = 'section' AND :sortOrder = 'DESC' THEN b.section END DESC,
            CASE WHEN :sortBy = 'joinYear' AND :sortOrder = 'ASC' THEN b.join_year END ASC,
            CASE WHEN :sortBy = 'joinYear' AND :sortOrder = 'DESC' THEN b.join_year END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findBatchesByCourseId(
        @Param("courseId") courseId: UUID,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<Map<String, Any>>
    
    @Query(value = """
        SELECT COUNT(*)
        FROM course_batch cb
        JOIN batch b ON b.id = cb.batch_id
        WHERE cb.course_id = :courseId
    """, nativeQuery = true)
    fun countBatchesByCourseId(@Param("courseId") courseId: UUID): Long

    @Query(value = """
        SELECT b.id, b.name, b.section, b.join_year
        FROM course_batch cb
        JOIN batch b ON b.id = cb.batch_id
        WHERE cb.course_id = :courseId
        AND (LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(b.section) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN b.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN b.name END DESC,
            CASE WHEN :sortBy = 'section' AND :sortOrder = 'ASC' THEN b.section END ASC,
            CASE WHEN :sortBy = 'section' AND :sortOrder = 'DESC' THEN b.section END DESC,
            CASE WHEN :sortBy = 'joinYear' AND :sortOrder = 'ASC' THEN b.join_year END ASC,
            CASE WHEN :sortBy = 'joinYear' AND :sortOrder = 'DESC' THEN b.join_year END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun searchBatchesByCourseId(
        @Param("courseId") courseId: UUID,
        @Param("query") query: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<Map<String, Any>>
    
    @Query(value = """
        SELECT COUNT(*)
        FROM course_batch cb
        JOIN batch b ON b.id = cb.batch_id
        WHERE cb.course_id = :courseId
        AND (LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(b.section) LIKE LOWER(CONCAT('%', :query, '%')))
    """, nativeQuery = true)
    fun countSearchBatchesByCourseId(@Param("courseId") courseId: UUID, @Param("query") query: String): Long

    @Query(value = """
        SELECT c.id, c.name, c.code, c.description
        FROM course c
        JOIN semester s ON c.semester_id = s.id
        WHERE s.is_active = true
        ORDER BY c.name
    """, nativeQuery = true)
    fun findAllActiveCourses(): List<Map<String, Any>>

    @Query(value = """
        SELECT c.id, c.name, c.code, c.description
        FROM course c
        JOIN semester s ON c.semester_id = s.id
        JOIN course_instructor ci ON ci.course_id = c.id
        WHERE s.is_active = true 
        AND ci.instructor_id = CAST(:instructorId AS VARCHAR)
        ORDER BY c.name
    """, nativeQuery = true)
    fun findActiveCoursesByInstructor(@Param("instructorId") instructorId: String): List<Map<String, Any>>

    @Query(value = """
        SELECT c.id, c.name, c.code, c.description
        FROM course c
        JOIN semester s ON c.semester_id = s.id
        JOIN course_student cs ON cs.course_id = c.id
        WHERE s.is_active = true AND cs.student_id = :studentId
        ORDER BY c.name
    """, nativeQuery = true)
    fun findActiveCoursesByStudent(@Param("studentId") studentId: String): List<Map<String, Any>>

    @Query(value = """
        SELECT DISTINCT c.id, c.name, c.code, c.description
        FROM course c
        JOIN semester s ON c.semester_id = s.id
        JOIN course_batch cb ON cb.course_id = c.id
        JOIN batch b ON b.id = cb.batch_id
        JOIN batch_student bs ON bs.batch_id = b.id
        WHERE s.is_active = true AND bs.student_id = :studentId
        ORDER BY c.name
    """, nativeQuery = true)
    fun findActiveCoursesByStudentThroughBatch(@Param("studentId") studentId: String): List<Map<String, Any>>

    @Query(value = """
        SELECT u.id, u.name, u.email
        FROM course_instructor ci
        JOIN "user" u ON u.id = ci.instructor_id
        WHERE ci.course_id = :courseId
    """, nativeQuery = true)
    fun findInstructorsByCourseId(@Param("courseId") courseId: UUID): List<Map<String, Any>>

    @Query(value = """
        SELECT COUNT(*) > 0
        FROM course_student cs
        WHERE cs.course_id = :courseId AND cs.student_id = :studentId
    """, nativeQuery = true)
    fun isStudentEnrolledInCourse(@Param("courseId") courseId: UUID, @Param("studentId") studentId: String): Boolean

    @Query(value = """
        SELECT COUNT(*) > 0
        FROM course c
        JOIN course_batch cb ON cb.course_id = c.id
        JOIN batch_student bs ON bs.batch_id = cb.batch_id
        WHERE c.id = :courseId AND bs.student_id = :studentId
    """, nativeQuery = true)
    fun isStudentEnrolledThroughBatch(@Param("courseId") courseId: UUID, @Param("studentId") studentId: String): Boolean

    @Query("SELECT c FROM Course c WHERE c.semester.isActive = true AND :student MEMBER OF c.students")
    fun findCoursesByActiveSemestersAndStudent(@Param("student") student: User): List<Course>

    @Query("SELECT DISTINCT c FROM Course c JOIN c.batches b JOIN b.students s WHERE c.semester.isActive = true AND s = :student")
    fun findCoursesByActiveSemestersAndStudentThroughBatch(@Param("student") student: User): List<Course>

    fun existsBySemester(@Param("semester") semester: Semester): Boolean

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.batches WHERE c.id = :courseId")
    fun findByIdWithBatches(@Param("courseId") courseId: UUID): Course?
    
    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.students WHERE c.id = :courseId")
    fun findByIdWithStudents(@Param("courseId") courseId: UUID): Course?
    
    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.instructors WHERE c.id = :courseId")
    fun findByIdWithInstructors(@Param("courseId") courseId: UUID): Course?

    @Query("SELECT COUNT(c) FROM Course c WHERE c.semester.isActive = true")
    fun countByActiveSemesters(): Long
    
    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM course_instructor 
            WHERE course_id = :courseId AND instructor_id = :userId
        )
    """, nativeQuery = true)
    fun isUserInstructor(@Param("courseId") courseId: UUID, @Param("userId") userId: String): Boolean
    
    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM course_student 
            WHERE course_id = :courseId AND student_id = :userId
        )
    """, nativeQuery = true)
    fun isUserStudent(@Param("courseId") courseId: UUID, @Param("userId") userId: String): Boolean


    @Query(value = """
        SELECT DISTINCT c.* 
        FROM course c
        JOIN batch_semester bs ON bs.semester_id = c.semester_id
        WHERE bs.batch_id IN :batchIds
    """, nativeQuery = true)
    fun findCoursesByBatchSemesters(@Param("batchIds") batchIds: List<UUID>): List<Course>
    
    @Query("""
        SELECT DISTINCT c FROM Course c
        LEFT JOIN FETCH c.semester
        LEFT JOIN FETCH c.instructors
        WHERE c.semester.id IN :semesterIds
    """)
    fun findCoursesBySemesterIds(@Param("semesterIds") semesterIds: List<UUID>): List<Course>
}
