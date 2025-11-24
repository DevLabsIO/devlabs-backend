package com.devlabs.devlabsbackend.batch.repository

import com.devlabs.devlabsbackend.batch.domain.Batch
import com.devlabs.devlabsbackend.semester.domain.Semester
import com.devlabs.devlabsbackend.user.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BatchRepository : JpaRepository<Batch, UUID> {
    
    @Query(value = """
        SELECT b.id
        FROM batch b
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN b.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN b.name END DESC,
            CASE WHEN :sortBy = 'joinYear' AND :sortOrder = 'ASC' THEN b.join_year END ASC,
            CASE WHEN :sortBy = 'joinYear' AND :sortOrder = 'DESC' THEN b.join_year END DESC,
            CASE WHEN :sortBy = 'section' AND :sortOrder = 'ASC' THEN b.section END ASC,
            CASE WHEN :sortBy = 'section' AND :sortOrder = 'DESC' THEN b.section END DESC,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN b.created_at END ASC NULLS LAST,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN b.created_at END DESC NULLS LAST
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findBatchIdsOnly(
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT COUNT(*)
        FROM batch b
    """, nativeQuery = true)
    fun countAllBatches(): Long

    @Query(value = """
        SELECT b.id, b.name, b.join_year, b.section, b.is_active, b.department_id,
               d.name as department_name
        FROM batch b
        LEFT JOIN department d ON d.id = b.department_id
        WHERE b.id IN (:ids)
    """, nativeQuery = true)
    fun findBatchListData(@Param("ids") ids: List<UUID>): List<Map<String, Any>>

    @Query(value = """
        SELECT b.id
        FROM batch b
        WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR CAST(b.join_year AS VARCHAR) LIKE CONCAT('%', :query, '%')
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN b.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN b.name END DESC,
            CASE WHEN :sortBy = 'joinYear' AND :sortOrder = 'ASC' THEN b.join_year END ASC,
            CASE WHEN :sortBy = 'joinYear' AND :sortOrder = 'DESC' THEN b.join_year END DESC,
            CASE WHEN :sortBy = 'section' AND :sortOrder = 'ASC' THEN b.section END ASC,
            CASE WHEN :sortBy = 'section' AND :sortOrder = 'DESC' THEN b.section END DESC,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN b.created_at END ASC NULLS LAST,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN b.created_at END DESC NULLS LAST
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun searchBatchIdsOnly(
        @Param("query") query: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT COUNT(*)
        FROM batch b
        WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR CAST(b.join_year AS VARCHAR) LIKE CONCAT('%', :query, '%')
    """, nativeQuery = true)
    fun countSearchBatches(@Param("query") query: String): Long

    @Query(value = """
        SELECT b.id
        FROM batch b
        WHERE b.is_active = :isActive
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN b.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN b.name END DESC,
            CASE WHEN :sortBy = 'joinYear' AND :sortOrder = 'ASC' THEN b.join_year END ASC,
            CASE WHEN :sortBy = 'joinYear' AND :sortOrder = 'DESC' THEN b.join_year END DESC,
            CASE WHEN :sortBy = 'section' AND :sortOrder = 'ASC' THEN b.section END ASC,
            CASE WHEN :sortBy = 'section' AND :sortOrder = 'DESC' THEN b.section END DESC,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN b.created_at END ASC NULLS LAST,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN b.created_at END DESC NULLS LAST
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findBatchIdsByIsActive(
        @Param("isActive") isActive: Boolean,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>

    @Query(value = """
        SELECT COUNT(*)
        FROM batch b
        WHERE b.is_active = :isActive
    """, nativeQuery = true)
    fun countBatchesByIsActive(@Param("isActive") isActive: Boolean): Long

    @Query(value = """
        SELECT b.id
        FROM batch b
        WHERE b.is_active = :isActive
        AND (LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR CAST(b.join_year AS VARCHAR) LIKE CONCAT('%', :query, '%'))
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN b.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN b.name END DESC,
            CASE WHEN :sortBy = 'joinYear' AND :sortOrder = 'ASC' THEN b.join_year END ASC,
            CASE WHEN :sortBy = 'joinYear' AND :sortOrder = 'DESC' THEN b.join_year END DESC,
            CASE WHEN :sortBy = 'section' AND :sortOrder = 'ASC' THEN b.section END ASC,
            CASE WHEN :sortBy = 'section' AND :sortOrder = 'DESC' THEN b.section END DESC,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN b.created_at END ASC NULLS LAST,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN b.created_at END DESC NULLS LAST
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun searchBatchIdsByIsActive(
        @Param("query") query: String,
        @Param("isActive") isActive: Boolean,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>

    @Query(value = """
        SELECT COUNT(*)
        FROM batch b
        WHERE b.is_active = :isActive
        AND (LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR CAST(b.join_year AS VARCHAR) LIKE CONCAT('%', :query, '%'))
    """, nativeQuery = true)
    fun countSearchBatchesByIsActive(@Param("query") query: String, @Param("isActive") isActive: Boolean): Long

    @Query(value = """
        SELECT u.id, u.name, u.email,
               CASE u.role
                   WHEN 0 THEN 'STUDENT'
                   WHEN 1 THEN 'ADMIN'
                   WHEN 2 THEN 'FACULTY'
                   WHEN 3 THEN 'MANAGER'
               END as role,
               u.phone_number, u.is_active, u.created_at
        FROM batch_student bs
        JOIN "user" u ON u.id = bs.student_id
        WHERE bs.batch_id = :batchId
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN u.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN u.name END DESC,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'ASC' THEN u.email END ASC,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'DESC' THEN u.email END DESC,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN u.created_at END ASC,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN u.created_at END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true, countQuery = """
        SELECT COUNT(*)
        FROM batch_student bs
        WHERE bs.batch_id = :batchId
    """)
    fun findStudentsByBatchIdNative(
        @Param("batchId") batchId: UUID,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<Map<String, Any>>

    @Query(value = """
        SELECT COUNT(*)
        FROM batch_student bs
        WHERE bs.batch_id = :batchId
    """, nativeQuery = true)
    fun countStudentsByBatchId(@Param("batchId") batchId: UUID): Long

    @Query(value = """
        SELECT u.id, u.name, u.email,
               CASE u.role
                   WHEN 0 THEN 'STUDENT'
                   WHEN 1 THEN 'ADMIN'
                   WHEN 2 THEN 'FACULTY'
                   WHEN 3 THEN 'MANAGER'
               END as role,
               u.phone_number, u.is_active, u.created_at
        FROM batch_student bs
        JOIN "user" u ON u.id = bs.student_id
        WHERE bs.batch_id = :batchId
        AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN u.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN u.name END DESC,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'ASC' THEN u.email END ASC,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'DESC' THEN u.email END DESC,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN u.created_at END ASC,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN u.created_at END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun searchStudentsByBatchIdNative(
        @Param("batchId") batchId: UUID,
        @Param("query") query: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<Map<String, Any>>

    @Query(value = """
        SELECT COUNT(*)
        FROM batch_student bs
        JOIN "user" u ON u.id = bs.student_id
        WHERE bs.batch_id = :batchId
        AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
    """, nativeQuery = true)
    fun countSearchStudentsByBatchId(@Param("batchId") batchId: UUID, @Param("query") query: String): Long
    
    override fun findAll(pageable: Pageable): Page<Batch>
    
    @Query("SELECT b FROM Batch b WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%')) OR CAST(b.joinYear AS string) LIKE CONCAT('%', :query, '%')")
    fun searchByNameOrYearContainingIgnoreCase(@Param("query") query: String, pageable: Pageable): Page<Batch>
    
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Batch b WHERE :semester MEMBER OF b.semester")
    fun existsBySemester(@Param("semester") semester: Semester): Boolean
    
    fun findByStudentsContaining(student: User): List<Batch>

    @Query(value = """
        SELECT b.id, b.name, b.join_year, b.section, b.is_active, b.department_id,
               d.name as department_name
        FROM batch b
        LEFT JOIN department d ON d.id = b.department_id
        WHERE b.is_active = true
    """, nativeQuery = true)
    fun findActiveBatchesNative(): List<Map<String, Any>>

    @Query(value = """
        SELECT DISTINCT b.id, b.name, b.join_year, b.section, b.is_active, b.department_id,
               d.name as department_name
        FROM batch b
        LEFT JOIN department d ON d.id = b.department_id
        JOIN batch_student bs ON bs.batch_id = b.id
        WHERE b.is_active = true AND bs.student_id = :studentId
    """, nativeQuery = true)
    fun findActiveBatchesByStudentIdNative(@Param("studentId") studentId: String): List<Map<String, Any>>
    
    @Query(value = """
        SELECT DISTINCT b.id
        FROM batch b
        JOIN course_batch cb ON cb.batch_id = b.id
        JOIN course c ON c.id = cb.course_id
        JOIN semester s ON s.id = c.semester_id
        JOIN course_instructor ci ON ci.course_id = c.id
        WHERE b.is_active = true 
        AND s.is_active = true
        AND ci.instructor_id = CAST(:instructorId AS VARCHAR)
        ORDER BY b.id
    """, nativeQuery = true)
    fun findActiveBatchIdsByInstructor(@Param("instructorId") instructorId: String): List<UUID>
    
    @Query(value = """
        SELECT DISTINCT b.id, b.name, b.join_year, b.section, b.is_active, b.department_id,
               d.name as department_name
        FROM batch b
        LEFT JOIN department d ON d.id = b.department_id
        JOIN course_batch cb ON cb.batch_id = b.id
        JOIN course c ON c.id = cb.course_id
        JOIN semester s ON s.id = c.semester_id
        JOIN course_instructor ci ON ci.course_id = c.id
        WHERE b.is_active = true 
        AND s.is_active = true
        AND ci.instructor_id = CAST(:instructorId AS VARCHAR)
    """, nativeQuery = true)
    fun findActiveBatchesByInstructorIdNative(@Param("instructorId") instructorId: String): List<Map<String, Any>>
    
    fun findByIsActiveTrue(): List<Batch>
    
    @Query("SELECT u FROM Batch b JOIN b.students u WHERE b.id = :batchId")
    fun findStudentsByBatchId(@Param("batchId") batchId: UUID, pageable: Pageable): Page<User>
    
    @Query("SELECT u FROM Batch b JOIN b.students u WHERE b.id = :batchId AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    fun searchStudentsByBatchId(@Param("batchId") batchId: UUID, @Param("query") query: String, pageable: Pageable): Page<User>
    
    @Query("SELECT b FROM Batch b JOIN b.students s WHERE b.isActive = true AND s.id = :studentId")
    fun findByIsActiveTrueAndStudentsId(@Param("studentId") studentId: String): List<Batch>
    
    fun countByIsActive(isActive: Boolean): Long
}
