package com.devlabs.devlabsbackend.project.repository

import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.project.domain.Project
import com.devlabs.devlabsbackend.project.domain.ProjectStatus
import com.devlabs.devlabsbackend.team.domain.Team
import com.devlabs.devlabsbackend.user.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ProjectRepository : JpaRepository<Project, UUID> {
    
    @Query("SELECT DISTINCT p FROM Project p JOIN p.courses c WHERE c = :course AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    fun findByCourseAndTitleContainingIgnoreCase(@Param("course") course: Course, @Param("query") query: String, pageable: Pageable): Page<Project>

    @Query("SELECT DISTINCT p FROM Project p JOIN p.courses c WHERE p.status IN (com.devlabs.devlabsbackend.project.domain.ProjectStatus.ONGOING, com.devlabs.devlabsbackend.project.domain.ProjectStatus.PROPOSED) AND c.semester.id = :semesterId")
    fun findActiveProjectsBySemester(@Param("semesterId") semesterId: UUID): List<Project>
    
    @Query("SELECT DISTINCT p FROM Project p JOIN p.courses c JOIN c.batches b WHERE p.status IN (com.devlabs.devlabsbackend.project.domain.ProjectStatus.ONGOING, com.devlabs.devlabsbackend.project.domain.ProjectStatus.PROPOSED) AND b.id = :batchId")
    fun findActiveProjectsByBatch(@Param("batchId") batchId: UUID): List<Project>
    
    @Query("SELECT DISTINCT p FROM Project p JOIN p.courses c JOIN p.team t JOIN t.members m WHERE c = :course AND m = :student AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    fun findByCourseAndTitleContainingIgnoreCaseAndTeamMembersContaining(
        @Param("course") course: Course,
        @Param("query") query: String, 
        @Param("student") student: User, 
        pageable: Pageable
    ): Page<Project>
    
    @Query(value = """
        SELECT 
            p.id, p.title, p.description, p.objectives, p.github_url, p.status,
            p.created_at, p.updated_at,
            t.id as team_id, t.name as team_name,
            (SELECT COUNT(*) FROM review_project rp WHERE rp.project_id = p.id) as review_count
        FROM project p
        LEFT JOIN team t ON t.id = p.team_id
        WHERE p.status IN (0, 1)
        ORDER BY p.updated_at DESC
    """, nativeQuery = true)
    fun findAllActiveProjectsNative(): List<Map<String, Any>>
    
    @Query(value = """
        SELECT DISTINCT p.id
        FROM project p
        JOIN project_courses pc ON pc.project_id = p.id
        JOIN course_instructor ci ON ci.course_id = pc.course_id
        WHERE p.status IN (0, 1)
        AND ci.instructor_id = CAST(:facultyId AS VARCHAR)
        ORDER BY p.id
    """, nativeQuery = true)
    fun findActiveProjectIdsByFaculty(@Param("facultyId") facultyId: String): List<UUID>
    
    @Query(value = """
        SELECT DISTINCT
            p.id, p.title, p.description, p.objectives, p.github_url, p.status,
            p.created_at, p.updated_at,
            t.id as team_id, t.name as team_name,
            (SELECT COUNT(*) FROM review_project rp WHERE rp.project_id = p.id) as review_count
        FROM project p
        LEFT JOIN team t ON t.id = p.team_id
        JOIN project_courses pc ON pc.project_id = p.id
        JOIN course_instructor ci ON ci.course_id = pc.course_id
        WHERE p.status IN (0, 1)
        AND ci.instructor_id = CAST(:facultyId AS VARCHAR)
        ORDER BY p.updated_at DESC
    """, nativeQuery = true)
    fun findActiveProjectsByFacultyNative(@Param("facultyId") facultyId: String): List<Map<String, Any>>
    
    @Query(value = """
        SELECT 
            p.id, p.title, p.description, p.objectives, p.github_url, p.status,
            p.created_at, p.updated_at,
            t.id as team_id, t.name as team_name
        FROM project p
        LEFT JOIN team t ON t.id = p.team_id
        WHERE p.id IN (:ids)
    """, nativeQuery = true)
    fun findProjectListDataByIds(@Param("ids") ids: List<UUID>): List<Map<String, Any>>
    
    @Query("""
        SELECT DISTINCT p FROM Project p 
        LEFT JOIN FETCH p.team t
        LEFT JOIN FETCH t.members
        LEFT JOIN FETCH p.courses c
        LEFT JOIN FETCH c.students
        LEFT JOIN FETCH c.instructors
        LEFT JOIN FETCH p.reviews
        WHERE p.id = :projectId
    """)
    fun findByIdWithRelations(@Param("projectId") projectId: UUID): Project?

    @Query("""
        SELECT DISTINCT p FROM Project p 
        LEFT JOIN FETCH p.team t
        LEFT JOIN FETCH t.members
        WHERE p.id = :projectId
    """)
    fun findByIdWithTeamAndMembers(@Param("projectId") projectId: UUID): Project?

    @Query(value = """
        SELECT 
            p.id, p.title, p.description, p.objectives, p.github_url, p.status,
            p.created_at, p.updated_at,
            t.id as team_id, t.name as team_name,
            (SELECT COUNT(*) FROM review_project rp WHERE rp.project_id = p.id) as review_count
        FROM project p
        LEFT JOIN team t ON t.id = p.team_id
        WHERE p.id IN (
            SELECT DISTINCT pc.project_id 
            FROM project_courses pc 
            WHERE pc.course_id = :courseId
        )
        ORDER BY 
            CASE WHEN :sortBy = 'title' AND :sortOrder = 'ASC' THEN p.title END ASC,
            CASE WHEN :sortBy = 'title' AND :sortOrder = 'DESC' THEN p.title END DESC,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN p.created_at END ASC NULLS LAST,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN p.created_at END DESC NULLS LAST,
            CASE WHEN :sortBy = 'updatedAt' AND :sortOrder = 'ASC' THEN p.updated_at END ASC NULLS LAST,
            CASE WHEN :sortBy = 'updatedAt' AND :sortOrder = 'DESC' THEN p.updated_at END DESC NULLS LAST
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findProjectsByCourseNative(
        @Param("courseId") courseId: UUID,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<Map<String, Any>>
    
    @Query(value = """
        SELECT COUNT(DISTINCT p.id)
        FROM project p
        JOIN project_courses pc ON pc.project_id = p.id
        WHERE pc.course_id = :courseId
    """, nativeQuery = true)
    fun countProjectsByCourse(@Param("courseId") courseId: UUID): Long
    
    @Query(value = """
        SELECT tm.team_id, u.id, u.name, u.email, u.profile_id, u.image,
               CASE u.role
                   WHEN 0 THEN 'STUDENT'
                   WHEN 1 THEN 'ADMIN'
                   WHEN 2 THEN 'FACULTY'
                   WHEN 3 THEN 'MANAGER'
               END as role,
               u.phone_number, u.is_active, u.created_at
        FROM team_members tm
        JOIN "user" u ON u.id = tm.user_id
        WHERE tm.team_id IN :teamIds
    """, nativeQuery = true)
    fun findTeamMembersByTeamIds(@Param("teamIds") teamIds: List<UUID>): List<Map<String, Any>>
    
    @Query(value = """
        SELECT pc.project_id, c.id, c.name, c.code
        FROM project_courses pc
        JOIN course c ON c.id = pc.course_id
        WHERE pc.project_id IN :projectIds
    """, nativeQuery = true)
    fun findCoursesByProjectIds(@Param("projectIds") projectIds: List<UUID>): List<Map<String, Any>>
    
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM project_courses pc
            JOIN course_instructor ci ON ci.course_id = pc.course_id
            WHERE pc.project_id = :projectId
              AND ci.instructor_id = :userId
        )
    """, nativeQuery = true)
    fun isUserInstructorForProject(
        @Param("projectId") projectId: UUID,
        @Param("userId") userId: String
    ): Boolean
    
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM project p
            JOIN team t ON t.id = p.team_id
            JOIN team_members tm ON tm.team_id = t.id
            WHERE p.id = :projectId
              AND tm.user_id = :userId
        )
    """, nativeQuery = true)
    fun isUserTeamMember(
        @Param("projectId") projectId: UUID,
        @Param("userId") userId: String
    ): Boolean

    @Query(value = """
        SELECT 
            p.id, p.title, p.description, p.objectives, p.github_url, p.status,
            p.created_at, p.updated_at,
            t.id as team_id, t.name as team_name,
            (SELECT COUNT(*) FROM review_project rp WHERE rp.project_id = p.id) as review_count
        FROM project p
        LEFT JOIN team t ON t.id = p.team_id
        WHERE p.team_id = :teamId
        ORDER BY 
            CASE WHEN :sortBy = 'title' AND :sortOrder = 'ASC' THEN p.title END ASC,
            CASE WHEN :sortBy = 'title' AND :sortOrder = 'DESC' THEN p.title END DESC,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN p.created_at END ASC NULLS LAST,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN p.created_at END DESC NULLS LAST,
            CASE WHEN :sortBy = 'updatedAt' AND :sortOrder = 'ASC' THEN p.updated_at END ASC NULLS LAST,
            CASE WHEN :sortBy = 'updatedAt' AND :sortOrder = 'DESC' THEN p.updated_at END DESC NULLS LAST
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findProjectsByTeamNative(
        @Param("teamId") teamId: UUID,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<Map<String, Any>>
    
    @Query(value = """
        SELECT COUNT(p.id)
        FROM project p
        WHERE p.team_id = :teamId
    """, nativeQuery = true)
    fun countProjectsByTeam(@Param("teamId") teamId: UUID): Long

    
    @Query(value = """
        SELECT 
            p.id, p.title, p.description, p.objectives, p.github_url, p.status,
            p.created_at, p.updated_at,
            t.id as team_id, t.name as team_name,
            (SELECT COUNT(*) FROM review_project rp WHERE rp.project_id = p.id) as review_count
        FROM project p
        LEFT JOIN team t ON t.id = p.team_id
        JOIN team_members tm ON tm.team_id = t.id
        JOIN project_courses pc ON pc.project_id = p.id
        WHERE tm.user_id = :userId
          AND pc.course_id = :courseId
        ORDER BY 
          CASE WHEN :sortBy = 'title' AND :sortOrder = 'ASC' THEN p.title END ASC,
          CASE WHEN :sortBy = 'title' AND :sortOrder = 'DESC' THEN p.title END DESC,
          CASE WHEN :sortBy = 'status' AND :sortOrder = 'ASC' THEN p.status END ASC,
          CASE WHEN :sortBy = 'status' AND :sortOrder = 'DESC' THEN p.status END DESC,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN p.created_at END ASC NULLS LAST,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN p.created_at END DESC NULLS LAST,
          CASE WHEN :sortBy = 'updatedAt' AND :sortOrder = 'ASC' THEN p.updated_at END ASC NULLS LAST,
          CASE WHEN :sortBy = 'updatedAt' AND :sortOrder = 'DESC' THEN p.updated_at END DESC NULLS LAST
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findProjectsByUserAndCourseNative(
        @Param("userId") userId: String,
        @Param("courseId") courseId: UUID,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<Map<String, Any>>
    
    @Query(value = """
        SELECT COUNT(DISTINCT p.id)
        FROM project p
        JOIN team t ON t.id = p.team_id
        JOIN team_members tm ON tm.team_id = t.id
        JOIN project_courses pc ON pc.project_id = p.id
        WHERE tm.user_id = :userId
          AND pc.course_id = :courseId
    """, nativeQuery = true)
    fun countByUserAndCourse(
        @Param("userId") userId: String,
        @Param("courseId") courseId: UUID
    ): Int

    
    @Query(value = """
        SELECT 
            p.id, p.title, p.description, p.objectives, p.github_url, p.status,
            p.created_at, p.updated_at,
            t.id as team_id, t.name as team_name,
            (SELECT COUNT(*) FROM review_project rp WHERE rp.project_id = p.id) as review_count
        FROM project p
        LEFT JOIN team t ON t.id = p.team_id
        JOIN team_members tm ON tm.team_id = t.id
        WHERE tm.user_id = :userId
        ORDER BY 
          CASE WHEN :sortBy = 'title' AND :sortOrder = 'ASC' THEN p.title END ASC,
          CASE WHEN :sortBy = 'title' AND :sortOrder = 'DESC' THEN p.title END DESC,
          CASE WHEN :sortBy = 'status' AND :sortOrder = 'ASC' THEN p.status END ASC,
          CASE WHEN :sortBy = 'status' AND :sortOrder = 'DESC' THEN p.status END DESC,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN p.created_at END ASC NULLS LAST,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN p.created_at END DESC NULLS LAST,
          CASE WHEN :sortBy = 'updatedAt' AND :sortOrder = 'ASC' THEN p.updated_at END ASC NULLS LAST,
          CASE WHEN :sortBy = 'updatedAt' AND :sortOrder = 'DESC' THEN p.updated_at END DESC NULLS LAST
        OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    fun findProjectsByUserNative(
        @Param("userId") userId: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<Map<String, Any>>
    
    @Query(value = """
        SELECT COUNT(DISTINCT p.id)
        FROM project p
        JOIN team t ON t.id = p.team_id
        JOIN team_members tm ON tm.team_id = t.id
        WHERE tm.user_id = :userId
    """, nativeQuery = true)
    fun countByUser(@Param("userId") userId: String): Int
    

    @Query("SELECT DISTINCT p.id, p.updatedAt, p.title FROM Project p WHERE p.status = :status ORDER BY p.updatedAt DESC, p.title")
    fun findIdsByStatusOrderByUpdatedAtDesc(@Param("status") status: ProjectStatus, pageable: Pageable): Page<Array<Any>>

    @Query("""
        SELECT DISTINCT p.id, p.updatedAt, p.title FROM Project p 
        WHERE p.status = com.devlabs.devlabsbackend.project.domain.ProjectStatus.COMPLETED
        AND EXISTS (SELECT 1 FROM Course course JOIN course.instructors instructor WHERE instructor = :faculty AND course MEMBER OF p.courses)
        ORDER BY p.updatedAt DESC
    """)
    fun findCompletedProjectIdsByFaculty(@Param("faculty") user: User, pageable: Pageable): Page<Array<Any>>

    @Query("""
        SELECT DISTINCT p.id, p.updatedAt, p.title FROM Project p 
        WHERE p.status = com.devlabs.devlabsbackend.project.domain.ProjectStatus.COMPLETED
        AND EXISTS (SELECT 1 FROM Team team JOIN team.members member WHERE member = :student AND team = p.team)
        ORDER BY p.updatedAt DESC
    """)
    fun findCompletedProjectIdsByStudent(@Param("student") student: User, pageable: Pageable): Page<Array<Any>>

    @Query("""
        SELECT DISTINCT p.id, p.updatedAt, p.title FROM Project p 
        WHERE p.status = com.devlabs.devlabsbackend.project.domain.ProjectStatus.COMPLETED
        AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY p.updatedAt DESC
    """)
    fun searchCompletedProjectIds(@Param("query") query: String, pageable: Pageable): Page<Array<Any>>

    @Query("""
        SELECT DISTINCT p.id, p.updatedAt, p.title FROM Project p 
        WHERE p.status = com.devlabs.devlabsbackend.project.domain.ProjectStatus.COMPLETED
        AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
        AND EXISTS (SELECT 1 FROM Course course JOIN course.instructors instructor WHERE instructor = :faculty AND course MEMBER OF p.courses)
        ORDER BY p.updatedAt DESC
    """)
    fun searchCompletedProjectIdsByFaculty(@Param("faculty") faculty: User, @Param("query") query: String, pageable: Pageable): Page<Array<Any>>
    

    @Query("""
        SELECT DISTINCT p.id, p.updatedAt, p.title FROM Project p 
        WHERE p.status = com.devlabs.devlabsbackend.project.domain.ProjectStatus.COMPLETED
        AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
        AND EXISTS (SELECT 1 FROM Team team JOIN team.members member WHERE member = :student AND team = p.team)
        ORDER BY p.updatedAt DESC
    """)
    fun searchCompletedProjectIdsByStudent(@Param("student") student: User, @Param("query") query: String, pageable: Pageable): Page<Array<Any>>
    
    @Query("SELECT p FROM Project p JOIN p.team t JOIN t.members m WHERE m = :user AND p.status IN :statuses")
    fun findByTeamMembersContainingAndStatusIn(@Param("user") user: User, @Param("statuses") statuses: List<ProjectStatus>): List<Project>

    @Query(value = """
        SELECT COUNT(DISTINCT p.id)
        FROM project p
        JOIN project_courses pc ON pc.project_id = p.id
        JOIN course c ON c.id = pc.course_id
        JOIN semester s ON s.id = c.semester_id
        WHERE s.is_active = true
    """, nativeQuery = true)
    fun countByActiveSemesters(): Long
    
    @Query(value = """
        SELECT COUNT(DISTINCT p.id)
        FROM project p
        JOIN project_courses pc ON pc.project_id = p.id
        JOIN course c ON c.id = pc.course_id
        JOIN semester s ON s.id = c.semester_id
        WHERE s.is_active = true
        AND p.status IN (0, 1)
    """, nativeQuery = true)
    fun countActiveProjectsByActiveSemesters(): Long
}

