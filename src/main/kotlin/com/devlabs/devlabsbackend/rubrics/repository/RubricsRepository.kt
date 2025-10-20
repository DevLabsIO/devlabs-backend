package com.devlabs.devlabsbackend.rubrics.repository

import com.devlabs.devlabsbackend.rubrics.domain.Rubrics
import com.devlabs.devlabsbackend.rubrics.domain.dto.RubricsListProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface RubricsRepository : JpaRepository<Rubrics, UUID> {
    
    @Query("""
        SELECT r.id as id,
               r.name as name,
               r.createdBy.id as createdById,
               r.createdBy.name as createdByName,
               r.createdBy.role as createdByRole,
               r.createdAt as createdAt,
               r.isShared as isShared
        FROM Rubrics r
    """)
    fun findAllProjected(pageable: Pageable): Page<RubricsListProjection>
    
    @Query("""
        SELECT r.id as id,
               r.name as name,
               r.createdBy.id as createdById,
               r.createdBy.name as createdByName,
               r.createdBy.role as createdByRole,
               r.createdAt as createdAt,
               r.isShared as isShared
        FROM Rubrics r
        WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
    """)
    fun searchProjected(
        @Param("searchTerm") searchTerm: String,
        pageable: Pageable
    ): Page<RubricsListProjection>
    
    @Query("""
        SELECT r.id as id,
               r.name as name,
               r.createdBy.id as createdById,
               r.createdBy.name as createdByName,
               r.createdBy.role as createdByRole,
               r.createdAt as createdAt,
               r.isShared as isShared
        FROM Rubrics r
        WHERE r.createdBy.id = :userId
    """)
    fun findByCreatedByProjected(
        @Param("userId") userId: String,
        pageable: Pageable
    ): Page<RubricsListProjection>
    
    @Query("""
        SELECT r.id as id,
               r.name as name,
               r.createdBy.id as createdById,
               r.createdBy.name as createdByName,
               r.createdBy.role as createdByRole,
               r.createdAt as createdAt,
               r.isShared as isShared
        FROM Rubrics r
        WHERE r.isShared = true
    """)
    fun findBySharedProjected(pageable: Pageable): Page<RubricsListProjection>
    
    @Query("""
        SELECT DISTINCT r FROM Rubrics r 
        LEFT JOIN FETCH r.createdBy 
        LEFT JOIN FETCH r.criteria 
        WHERE r.id = :id
    """)
    fun findByIdWithDetails(@Param("id") id: UUID): Rubrics?
    
    @Query("""
        SELECT DISTINCT r FROM Rubrics r 
        LEFT JOIN FETCH r.createdBy 
        LEFT JOIN FETCH r.criteria 
        WHERE r.createdBy.id = :userId
    """)
    fun findByCreatedByWithDetails(@Param("userId") userId: String): List<Rubrics>
    
    @Query("""
        SELECT DISTINCT r FROM Rubrics r 
        LEFT JOIN FETCH r.createdBy 
        LEFT JOIN FETCH r.criteria 
        WHERE r.isShared = true
    """)
    fun findByIsSharedTrueWithDetails(): List<Rubrics>
    
    @Query("""
        SELECT DISTINCT r FROM Rubrics r 
        LEFT JOIN FETCH r.createdBy 
        LEFT JOIN FETCH r.criteria 
        ORDER BY r.name
    """)
    fun findAllWithCriteria(): List<Rubrics>
}
