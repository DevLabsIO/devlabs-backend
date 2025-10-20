package com.devlabs.devlabsbackend.department.repository

import com.devlabs.devlabsbackend.department.domain.Department
import com.devlabs.devlabsbackend.department.domain.dto.DepartmentBatchResponse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface DepartmentRepository : JpaRepository<Department, UUID> {
    
    @Query(value = """
        SELECT d.id
        FROM department d
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN d.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN d.name END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findAllIds(
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT d.id
        FROM department d
        WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN d.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN d.name END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findIdsByNameContaining(
        @Param("query") query: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT COUNT(*)
        FROM department d
        WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%'))
    """, nativeQuery = true)
    fun countByNameContaining(@Param("query") query: String): Long
    
    @Query("SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.batches WHERE d.id IN :ids")
    fun findAllByIdWithBatches(@Param("ids") ids: List<UUID>): List<Department>
    
    @Query("SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.batches WHERE d.id = :departmentId")
    fun findByIdWithBatches(@Param("departmentId") departmentId: UUID): Department?
    
    @Query("""
        SELECT new com.devlabs.devlabsbackend.department.domain.dto.DepartmentBatchResponse(
            b.id, b.name, b.graduationYear, b.section
        )
        FROM Department d 
        JOIN d.batches b 
        WHERE d.id = :deptId
    """)
    fun findBatchesByDepartmentId(@Param("deptId") deptId: UUID): List<DepartmentBatchResponse>
}
