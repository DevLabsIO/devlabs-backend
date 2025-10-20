package com.devlabs.devlabsbackend.team.repository

import com.devlabs.devlabsbackend.team.domain.Team
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TeamRepository : JpaRepository<Team, UUID> {
    
    @Query(value = """
        SELECT t.id, t.name, t.created_at
        FROM team t
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN t.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN t.name END DESC,
            CASE WHEN :sortBy = 'created_at' AND :sortOrder = 'ASC' THEN t.created_at END ASC,
            CASE WHEN :sortBy = 'created_at' AND :sortOrder = 'DESC' THEN t.created_at END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findAllIds(
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<Array<Any>>
    
    @Query(value = """
        SELECT COUNT(DISTINCT t.id)
        FROM team t
    """, nativeQuery = true)
    fun countAllTeams(): Long
    
    @Query(value = """
        SELECT t.id, t.name, t.created_at
        FROM team t
        JOIN team_members tm ON t.id = tm.team_id
        WHERE tm.user_id = CAST(:memberId AS VARCHAR)
        GROUP BY t.id, t.name, t.created_at
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN t.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN t.name END DESC,
            CASE WHEN :sortBy = 'created_at' AND :sortOrder = 'ASC' THEN t.created_at END ASC,
            CASE WHEN :sortBy = 'created_at' AND :sortOrder = 'DESC' THEN t.created_at END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findIdsByMember(
        @Param("memberId") memberId: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<Array<Any>>
    
    @Query(value = """
        SELECT COUNT(DISTINCT t.id)
        FROM team t
        JOIN team_members tm ON t.id = tm.team_id
        WHERE tm.user_id = CAST(:memberId AS VARCHAR)
    """, nativeQuery = true)
    fun countTeamsByMember(@Param("memberId") memberId: String): Long
    
    @Query(value = """
        SELECT t.id, t.name, t.created_at
        FROM team t
        WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN t.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN t.name END DESC,
            CASE WHEN :sortBy = 'created_at' AND :sortOrder = 'ASC' THEN t.created_at END ASC,
            CASE WHEN :sortBy = 'created_at' AND :sortOrder = 'DESC' THEN t.created_at END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findIdsByNameContaining(
        @Param("name") name: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<Array<Any>>
    
    @Query(value = """
        SELECT COUNT(DISTINCT t.id)
        FROM team t
        WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))
    """, nativeQuery = true)
    fun countTeamsByNameContaining(@Param("name") name: String): Long
    
    @Query(value = """
        SELECT t.id, t.name, t.created_at
        FROM team t
        JOIN team_members tm ON t.id = tm.team_id
        WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))
          AND tm.user_id = CAST(:memberId AS VARCHAR)
        GROUP BY t.id, t.name, t.created_at
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN t.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN t.name END DESC,
            CASE WHEN :sortBy = 'created_at' AND :sortOrder = 'ASC' THEN t.created_at END ASC,
            CASE WHEN :sortBy = 'created_at' AND :sortOrder = 'DESC' THEN t.created_at END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findIdsByNameAndMember(
        @Param("name") name: String,
        @Param("memberId") memberId: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<Array<Any>>
    
    @Query(value = """
        SELECT COUNT(DISTINCT t.id)
        FROM team t
        JOIN team_members tm ON t.id = tm.team_id
        WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))
          AND tm.user_id = CAST(:memberId AS VARCHAR)
    """, nativeQuery = true)
    fun countTeamsByNameAndMember(
        @Param("name") name: String,
        @Param("memberId") memberId: String
    ): Long
    
    @Query(value = """
        SELECT t.id, t.name, t.created_at
        FROM team t
        JOIN project p ON p.team_id = t.id
        JOIN project_courses pc ON p.id = pc.project_id
        JOIN course c ON pc.course_id = c.id
        JOIN course_instructors ci ON c.id = ci.course_id
        WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))
          AND ci.user_id = CAST(:instructorId AS VARCHAR)
        GROUP BY t.id, t.name, t.created_at
        ORDER BY 
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN t.name END ASC,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN t.name END DESC,
            CASE WHEN :sortBy = 'created_at' AND :sortOrder = 'ASC' THEN t.created_at END ASC,
            CASE WHEN :sortBy = 'created_at' AND :sortOrder = 'DESC' THEN t.created_at END DESC
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findIdsByNameAndCourseInstructor(
        @Param("name") name: String,
        @Param("instructorId") instructorId: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<Array<Any>>
    
    @Query(value = """
        SELECT COUNT(DISTINCT t.id)
        FROM team t
        JOIN project p ON p.team_id = t.id
        JOIN project_courses pc ON p.id = pc.project_id
        JOIN course c ON pc.course_id = c.id
        JOIN course_instructors ci ON c.id = ci.course_id
        WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))
          AND ci.user_id = CAST(:instructorId AS VARCHAR)
    """, nativeQuery = true)
    fun countTeamsByNameAndCourseInstructor(
        @Param("name") name: String,
        @Param("instructorId") instructorId: String
    ): Long
    
    @Query("""
        SELECT DISTINCT t FROM Team t 
        LEFT JOIN FETCH t.members 
        LEFT JOIN FETCH t.projects 
        WHERE t.id IN :ids
    """)
    fun findAllByIdWithRelations(@Param("ids") ids: List<UUID>): List<Team>
    
    @Query("""
        SELECT DISTINCT t FROM Team t 
        LEFT JOIN FETCH t.members 
        LEFT JOIN FETCH t.projects 
        WHERE t.id = :id
    """)
    fun findByIdWithRelations(@Param("id") id: UUID): Team?
    
    @Query("""
        SELECT DISTINCT t FROM Team t 
        LEFT JOIN FETCH t.members 
        WHERE t.id = :id
    """)
    fun findByIdWithMembers(@Param("id") id: UUID): Team?
}
