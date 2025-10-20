package com.devlabs.devlabsbackend.semester.repository

import com.devlabs.devlabsbackend.semester.domain.Semester
import com.devlabs.devlabsbackend.semester.domain.dto.SemesterListProjection
import com.devlabs.devlabsbackend.user.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SemesterRepository : JpaRepository<Semester, UUID> {
    
    @Query(value = """
        SELECT s.id
        FROM semester s
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN s.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN s.name END DESC,
            CASE WHEN :sortBy = 'year' AND :sortOrder = 'ASC' THEN s.year END ASC,
            CASE WHEN :sortBy = 'year' AND :sortOrder = 'DESC' THEN s.year END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findSemesterIdsOnly(
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT COUNT(*)
        FROM semester s
    """, nativeQuery = true)
    fun countAllSemesters(): Long

    @Query(value = """
        SELECT s.id, s.name, s.year, s.is_active
        FROM semester s
        WHERE s.id IN (:ids)
    """, nativeQuery = true)
    fun findSemesterListData(@Param("ids") ids: List<UUID>): List<Map<String, Any>>

    @Query(value = """
        SELECT s.id
        FROM semester s
        WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR CAST(s.year AS VARCHAR) LIKE CONCAT('%', :query, '%')
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN s.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN s.name END DESC,
            CASE WHEN :sortBy = 'year' AND :sortOrder = 'ASC' THEN s.year END ASC,
            CASE WHEN :sortBy = 'year' AND :sortOrder = 'DESC' THEN s.year END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun searchSemesterIdsOnly(
        @Param("query") query: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT COUNT(*)
        FROM semester s
        WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR CAST(s.year AS VARCHAR) LIKE CONCAT('%', :query, '%')
    """, nativeQuery = true)
    fun countSearchSemesters(@Param("query") query: String): Long

    @Query(value = """
        SELECT c.id, c.name, c.code, c.description
        FROM course c
        WHERE c.semester_id = :semesterId
        ORDER BY c.name
    """, nativeQuery = true)
    fun findCoursesBySemesterId(@Param("semesterId") semesterId: UUID): List<Map<String, Any>>

    @Query(value = """
        SELECT s.id, s.name, s.year, s.is_active
        FROM semester s
        WHERE s.is_active = true
    """, nativeQuery = true)
    fun findActiveSemestersNative(): List<Map<String, Any>>
    
    @Query(value = """
        SELECT DISTINCT s.id
        FROM semester s
        JOIN course c ON c.semester_id = s.id
        JOIN course_instructor ci ON ci.course_id = c.id
        WHERE s.is_active = true 
        AND ci.instructor_id = CAST(:instructorId AS VARCHAR)
        ORDER BY s.id
    """, nativeQuery = true)
    fun findActiveSemesterIdsByInstructor(@Param("instructorId") instructorId: String): List<UUID>
    
    @Query(value = """
        SELECT DISTINCT s.id, s.name, s.year, s.is_active
        FROM semester s
        JOIN course c ON c.semester_id = s.id
        JOIN course_instructor ci ON ci.course_id = c.id
        WHERE s.is_active = true 
        AND ci.instructor_id = CAST(:instructorId AS VARCHAR)
        ORDER BY s.name
    """, nativeQuery = true)
    fun findActiveSemestersByInstructor(@Param("instructorId") instructorId: String): List<Map<String, Any>>
    
    override fun findAll(pageable: Pageable): Page<Semester>
    
    @Query("SELECT s FROM Semester s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR CAST(s.year AS string) LIKE CONCAT('%', :query, '%')")
    fun findByNameOrYearContainingIgnoreCase(@Param("query") query: String, pageable: Pageable): Page<Semester>
    
    fun findByIsActiveTrue(): List<Semester>
    
    @Query("SELECT s FROM Semester s LEFT JOIN FETCH s.courses WHERE s.id = :semesterId")
    fun findByIdWithCourses(@Param("semesterId") semesterId: UUID): Semester?

    @Query("SELECT s FROM Semester s LEFT JOIN FETCH s.courses WHERE s.id IN :ids")
    fun findAllByIdWithCourses(@Param("ids") ids: List<UUID>): List<Semester>
    
    fun countByIsActive(isActive: Boolean): Long
    
    fun findByManagersContaining(manager: User): List<Semester>
    
    fun existsByNameAndYear(name: String, year: Int): Boolean
    
    fun existsByNameAndYearAndIdNot(name: String, year: Int, id: UUID): Boolean
}
