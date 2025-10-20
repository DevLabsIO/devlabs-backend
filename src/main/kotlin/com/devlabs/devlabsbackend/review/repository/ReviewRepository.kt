package com.devlabs.devlabsbackend.review.repository

import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.project.domain.Project
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.review.domain.dto.ReviewListProjection
import com.devlabs.devlabsbackend.user.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.*

interface ReviewRepository : JpaRepository<Review, UUID> {
    
    @Query("""
        SELECT r.id as id,
               r.name as name,
               r.startDate as startDate,
               r.endDate as endDate,
               r.createdBy.id as createdById,
               r.createdBy.name as createdByName,
               r.createdBy.role as createdByRole
        FROM Review r
    """)
    fun findAllProjected(pageable: Pageable): Page<ReviewListProjection>
    
    @Query("""
        SELECT DISTINCT r.id as id,
               r.name as name,
               r.startDate as startDate,
               r.endDate as endDate,
               r.createdBy.id as createdById,
               r.createdBy.name as createdByName,
               r.createdBy.role as createdByRole
        FROM Review r
        JOIN r.courses c
        JOIN c.instructors i
        WHERE i = :instructor
    """)
    fun findByCoursesInstructorsProjected(
        @Param("instructor") instructor: User,
        pageable: Pageable
    ): Page<ReviewListProjection>
    
    @Query("""
        SELECT DISTINCT r.id as id,
               r.name as name,
               r.startDate as startDate,
               r.endDate as endDate,
               r.createdBy.id as createdById,
               r.createdBy.name as createdByName,
               r.createdBy.role as createdByRole
        FROM Review r
        JOIN r.projects p
        JOIN p.team t
        JOIN t.members m
        WHERE m = :student
    """)
    fun findByProjectsTeamMembersProjected(
        @Param("student") student: User,
        pageable: Pageable
    ): Page<ReviewListProjection>

    @Query("SELECT r.id, r.startDate FROM Review r")
    fun findAllIdsOnly(pageable: Pageable): Page<Array<Any>>
    
    @Query("SELECT DISTINCT r.id, r.startDate FROM Review r JOIN r.courses c JOIN c.instructors i WHERE i = :instructor")
    fun findIdsByCoursesInstructorsContainingJpql(@Param("instructor") instructor: User, pageable: Pageable): Page<Array<Any>>
    
    @Query("SELECT DISTINCT r.id, r.startDate FROM Review r JOIN r.projects p JOIN p.team t JOIN t.members m WHERE m = :student")
    fun findIdsByProjectsTeamMembersContainingJpql(@Param("student") student: User, pageable: Pageable): Page<Array<Any>>
 
    @Query(value = "SELECT r.id FROM review r ORDER BY r.start_date DESC", 
           countQuery = "SELECT COUNT(r.id) FROM review r",
           nativeQuery = true)
    fun findAllIds(pageable: Pageable): Page<UUID>
    
    @Query(value = """
        SELECT r.id
        FROM (
            SELECT DISTINCT r.id, r.start_date
            FROM review r
            JOIN course_review cr ON r.id = cr.review_id
            JOIN course c ON c.id = cr.course_id
            JOIN course_instructor ci ON c.id = ci.course_id
            WHERE ci.instructor_id = :#{#instructor.id}
        ) r
        ORDER BY r.start_date DESC
    """, 
    countQuery = """
        SELECT COUNT(DISTINCT r.id)
        FROM review r
        JOIN course_review cr ON r.id = cr.review_id
        JOIN course c ON c.id = cr.course_id
        JOIN course_instructor ci ON c.id = ci.course_id
        WHERE ci.instructor_id = :#{#instructor.id}
    """,
    nativeQuery = true)
    fun findIdsByCoursesInstructorsContaining(@Param("instructor") instructor: User, pageable: Pageable): Page<UUID>
    
    @Query(value = """
        SELECT r.id
        FROM (
            SELECT DISTINCT r.id, r.start_date
            FROM review r
            JOIN project_review pr ON r.id = pr.review_id
            JOIN project p ON p.id = pr.project_id
            JOIN team t ON p.team_id = t.id
            JOIN team_member tm ON t.id = tm.team_id
            WHERE tm.member_id = :#{#student.id}
        ) r
        ORDER BY r.start_date DESC
    """, 
    countQuery = """
        SELECT COUNT(DISTINCT r.id)
        FROM review r
        JOIN project_review pr ON r.id = pr.review_id
        JOIN project p ON p.id = pr.project_id
        JOIN team t ON p.team_id = t.id
        JOIN team_member tm ON t.id = tm.team_id
        WHERE tm.member_id = :#{#student.id}
    """,
    nativeQuery = true)
    fun findIdsByProjectsTeamMembersContaining(@Param("student") student: User, pageable: Pageable): Page<UUID>
    
    @Query("""
        SELECT DISTINCT r.id FROM Review r
        LEFT JOIN r.courses c
        LEFT JOIN r.projects p
        LEFT JOIN r.batches b
        WHERE r.id IN :ids
    """)
    fun warmupCollections(@Param("ids") ids: List<UUID>): List<UUID>
    
    @Query(value = """
        SELECT 
            r.id,
            r.name,
            r.start_date,
            r.end_date,
            r.created_by_id,
            u.name as created_by_name,
            u.email as created_by_email,
            CASE u.role
                WHEN 0 THEN 'STUDENT'
                WHEN 1 THEN 'ADMIN'
                WHEN 2 THEN 'FACULTY'
                WHEN 3 THEN 'MANAGER'
            END as created_by_role,
            r.rubrics_id,
            rb.name as rubrics_name
        FROM review r
        LEFT JOIN "user" u ON u.id = r.created_by_id
        LEFT JOIN rubrics rb ON rb.id = r.rubrics_id
        WHERE r.id IN :ids
        ORDER BY r.start_date
    """, nativeQuery = true)
    fun findReviewListData(@Param("ids") ids: List<UUID>): List<Map<String, Any>>
    

    @Query(value = """
        SELECT 
            r.id,
            r.name,
            r.start_date,
            r.end_date,
            r.created_by_id,
            u.name as created_by_name,
            u.email as created_by_email,
            CASE u.role
                WHEN 0 THEN 'STUDENT'
                WHEN 1 THEN 'ADMIN'
                WHEN 2 THEN 'FACULTY'
                WHEN 3 THEN 'MANAGER'
            END as created_by_role,
            r.rubrics_id,
            rb.name as rubrics_name
        FROM review r
        LEFT JOIN "user" u ON u.id = r.created_by_id
        LEFT JOIN rubrics rb ON rb.id = r.rubrics_id
        WHERE r.id = :reviewId
    """, nativeQuery = true)
    fun findReviewDataById(@Param("reviewId") reviewId: UUID): Map<String, Any>?
    
    @Query(value = """
        SELECT DISTINCT r.id, r.name, r.start_date, r.end_date
        FROM review r
        JOIN course_review cr ON cr.review_id = r.id
        WHERE cr.course_id IN :courseIds
    """, nativeQuery = true)
    fun findReviewDataByCourses(@Param("courseIds") courseIds: List<UUID>): List<Map<String, Any>>
    
    @Query(value = """
        SELECT 
            c_r.review_id,
            c.id as id,
            c.name,
            c.code,
            s.id as semester_id,
            s.name as semester_name,
            s.year as semester_year,
            s.is_active as semester_is_active
        FROM course_review c_r
        JOIN course c ON c.id = c_r.course_id
        LEFT JOIN semester s ON s.id = c.semester_id
        WHERE c_r.review_id IN :reviewIds
    """, nativeQuery = true)
    fun findCoursesByReviewIds(@Param("reviewIds") reviewIds: List<UUID>): List<Map<String, Any>>
    

    @Query(value = """
        SELECT 
            c.id as id,
            c.name,
            c.code,
            s.id as semester_id,
            s.name as semester_name,
            s.year as semester_year,
            s.is_active as semester_is_active
        FROM course_review c_r
        JOIN course c ON c.id = c_r.course_id
        LEFT JOIN semester s ON s.id = c.semester_id
        WHERE c_r.review_id = :reviewId
    """, nativeQuery = true)
    fun findCoursesByReviewId(@Param("reviewId") reviewId: UUID): List<Map<String, Any>>
    
    @Query(value = """
        SELECT 
            rubrics_id,
            id,
            name,
            description,
            max_score,
            is_common
        FROM criterion
        WHERE rubrics_id IN :rubricsIds
    """, nativeQuery = true)
    fun findCriteriaByRubricsIds(@Param("rubricsIds") rubricsIds: List<UUID>): List<Map<String, Any>>
    

    @Query(value = """
        SELECT 
            r.id as review_id,
            r.name as review_name,
            rb.id as rubrics_id,
            rb.name as rubrics_name,
            c.id as criterion_id,
            c.name as criterion_name,
            c.description as criterion_description,
            c.max_score,
            c.is_common
        FROM review r
        LEFT JOIN rubrics rb ON rb.id = r.rubrics_id
        LEFT JOIN criterion c ON c.rubrics_id = rb.id
        WHERE r.id = :reviewId
        ORDER BY c.id
    """, nativeQuery = true)
    fun findReviewCriteriaData(@Param("reviewId") reviewId: UUID): List<Map<String, Any>>
    
    @Query(value = """
        SELECT 
            pr.review_id,
            p.id,
            p.title,
            t.id as team_id,
            t.name as team_name,
            m.id as member_id,
            m.name as member_name
        FROM review_project pr
        JOIN project p ON p.id = pr.project_id
        JOIN team t ON t.id = p.team_id
        JOIN team_members tm ON tm.team_id = t.id
        JOIN "user" m ON m.id = tm.user_id
        WHERE pr.review_id IN :reviewIds
    """, nativeQuery = true)
    fun findProjectsByReviewIds(@Param("reviewIds") reviewIds: List<UUID>): List<Map<String, Any>>
    
    @Query(value = """
        SELECT 
            p.id,
            p.title,
            t.id as team_id,
            t.name as team_name,
            m.id as member_id,
            m.name as member_name
        FROM review_project pr
        JOIN project p ON p.id = pr.project_id
        JOIN team t ON t.id = p.team_id
        JOIN team_members tm ON tm.team_id = t.id
        JOIN "user" m ON m.id = tm.user_id
        WHERE pr.review_id = :reviewId
    """, nativeQuery = true)
    fun findProjectsByReviewId(@Param("reviewId") reviewId: UUID): List<Map<String, Any>>
    
    @Query(value = """
        SELECT review_id, COUNT(*) as count
        FROM review_course_publication
        WHERE review_id IN :reviewIds
        GROUP BY review_id
    """, nativeQuery = true)
    fun findPublishedCountsByReviewIds(@Param("reviewIds") reviewIds: List<UUID>): List<Map<String, Any>>
    
    @Query("""
        SELECT new com.devlabs.devlabsbackend.review.domain.dto.ReviewListDTO(
            r.id,
            r.name,
            r.startDate,
            r.endDate,
            r.createdBy.id,
            r.createdBy.name,
            r.createdBy.email,
            r.createdBy.role,
            r.rubrics.id,
            r.rubrics.name,
            CAST(COUNT(DISTINCT c.id) AS long),
            CAST(COUNT(DISTINCT p.id) AS long),
            CAST(COUNT(DISTINCT rcp.id) AS long)
        )
        FROM Review r
        LEFT JOIN r.courses c
        LEFT JOIN r.projects p
        LEFT JOIN ReviewCoursePublication rcp ON rcp.review.id = r.id
        WHERE r.id IN :ids
        GROUP BY r.id, r.name, r.startDate, r.endDate, 
                 r.createdBy.id, r.createdBy.name, r.createdBy.email, r.createdBy.role,
                 r.rubrics.id, r.rubrics.name
    """)
    fun findReviewListDTOsByIds(@Param("ids") ids: List<UUID>): List<com.devlabs.devlabsbackend.review.domain.dto.ReviewListDTO>
    
    @Query("""
        SELECT new com.devlabs.devlabsbackend.review.domain.dto.ReviewListDTO(
            r.id,
            r.name,
            r.startDate,
            r.endDate,
            r.createdBy.id,
            r.createdBy.name,
            r.createdBy.email,
            r.createdBy.role,
            r.rubrics.id,
            r.rubrics.name,
            CAST(COUNT(DISTINCT c.id) AS long),
            CAST(COUNT(DISTINCT p.id) AS long),
            CAST(COUNT(DISTINCT rcp.id) AS long)
        )
        FROM Review r
        LEFT JOIN r.courses c
        LEFT JOIN r.projects p
        LEFT JOIN ReviewCoursePublication rcp ON rcp.review.id = r.id
        WHERE r.id IN :ids
        GROUP BY r.id, r.name, r.startDate, r.endDate, 
                 r.createdBy.id, r.createdBy.name, r.createdBy.email, r.createdBy.role,
                 r.rubrics.id, r.rubrics.name
        ORDER BY r.startDate ASC
    """)
    fun findReviewListDTOsByIdsOrderByStartDateAsc(@Param("ids") ids: List<UUID>): List<com.devlabs.devlabsbackend.review.domain.dto.ReviewListDTO>
    
    @Query("""
        SELECT new com.devlabs.devlabsbackend.review.domain.dto.ReviewListDTO(
            r.id,
            r.name,
            r.startDate,
            r.endDate,
            r.createdBy.id,
            r.createdBy.name,
            r.createdBy.email,
            r.createdBy.role,
            r.rubrics.id,
            r.rubrics.name,
            CAST(COUNT(DISTINCT c.id) AS long),
            CAST(COUNT(DISTINCT p.id) AS long),
            CAST(COUNT(DISTINCT rcp.id) AS long)
        )
        FROM Review r
        LEFT JOIN r.courses c
        LEFT JOIN r.projects p
        LEFT JOIN ReviewCoursePublication rcp ON rcp.review.id = r.id
        WHERE r.id IN :ids
        GROUP BY r.id, r.name, r.startDate, r.endDate, 
                 r.createdBy.id, r.createdBy.name, r.createdBy.email, r.createdBy.role,
                 r.rubrics.id, r.rubrics.name
        ORDER BY r.startDate DESC
    """)
    fun findReviewListDTOsByIdsOrderByStartDateDesc(@Param("ids") ids: List<UUID>): List<com.devlabs.devlabsbackend.review.domain.dto.ReviewListDTO>
    
    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM review_project WHERE review_id = :reviewId AND project_id = :projectId
            UNION
            SELECT 1 FROM course_review cr
            JOIN project_courses pc ON cr.course_id = pc.course_id
            WHERE cr.review_id = :reviewId AND pc.project_id = :projectId
            UNION
            SELECT 1 FROM review_batch rb
            JOIN batch_student bs ON rb.batch_id = bs.batch_id
            JOIN team_members tm ON tm.user_id = bs.student_id
            JOIN project p ON p.team_id = tm.team_id
            WHERE rb.review_id = :reviewId AND p.id = :projectId
            UNION
            SELECT 1 FROM course_review cr
            JOIN course c ON c.id = cr.course_id
            JOIN batch_semester bsem ON bsem.semester_id = c.semester_id
            JOIN batch_student bs ON bs.batch_id = bsem.batch_id
            JOIN team_members tm ON tm.user_id = bs.student_id
            JOIN project p ON p.team_id = tm.team_id
            WHERE cr.review_id = :reviewId AND p.id = :projectId
            UNION
            SELECT 1 FROM course_review cr
            JOIN project_courses pc ON pc.course_id = cr.course_id
            JOIN project p ON p.id = pc.project_id
            JOIN course c ON c.id = pc.course_id
            JOIN batch_semester bsem ON bsem.semester_id = c.semester_id
            JOIN batch_student bs ON bs.batch_id = bsem.batch_id
            JOIN team_members tm ON tm.user_id = bs.student_id
            JOIN project p2 ON p2.team_id = tm.team_id
            WHERE cr.review_id = :reviewId AND p2.id = :projectId
        )
    """, nativeQuery = true)
    fun isProjectAssociatedWithReview(
        @Param("reviewId") reviewId: UUID,
        @Param("projectId") projectId: UUID
    ): Boolean
    
    @Query(value = """
        SELECT CASE 
            WHEN :role IN ('ADMIN', 'MANAGER') THEN
                EXISTS(SELECT 1 FROM review WHERE id = :reviewId)
            WHEN :role = 'FACULTY' THEN
                EXISTS(SELECT 1 FROM course_review cr
                 JOIN course_instructor ci ON ci.course_id = cr.course_id
                 WHERE cr.review_id = :reviewId
                   AND ci.instructor_id = :userId)
            WHEN :role = 'STUDENT' THEN
                EXISTS(SELECT 1 FROM review_course_publication rcp
                 JOIN course_student cs ON cs.course_id = rcp.course_id
                 WHERE rcp.review_id = :reviewId
                   AND cs.student_id = :userId)
            ELSE FALSE
        END
    """, nativeQuery = true)
    fun hasUserAccessToReview(
        @Param("reviewId") reviewId: UUID,
        @Param("userId") userId: String,
        @Param("role") role: String
    ): Boolean
    
    @Query(value = """
        SELECT DISTINCT r.id
        FROM review r
        WHERE 1=1
          AND (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:courseId IS NULL OR EXISTS (
              SELECT 1 FROM course_review cr WHERE cr.review_id = r.id AND cr.course_id = :courseId
          ))
          AND (
              CASE :status
                  WHEN 'live' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'ongoing' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'current' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'completed' THEN r.end_date < :currentDate
                  WHEN 'ended' THEN r.end_date < :currentDate
                  WHEN 'past' THEN r.end_date < :currentDate
                  WHEN 'upcoming' THEN r.start_date > :currentDate
                  WHEN 'future' THEN r.start_date > :currentDate
                  ELSE TRUE
              END
          )
        ORDER BY 
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'asc' THEN r.name END ASC,
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'desc' THEN r.name END DESC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'asc' THEN r.end_date END ASC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'desc' THEN r.end_date END DESC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'asc' THEN r.start_date END ASC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'desc' THEN r.start_date END DESC
        OFFSET :offset ROWS FETCH FIRST :limit ROWS ONLY
    """, nativeQuery = true)
    fun searchReviewIdsForAdmin(
        @Param("name") name: String?,
        @Param("courseId") courseId: UUID?,
        @Param("status") status: String?,
        @Param("currentDate") currentDate: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT COUNT(DISTINCT r.id)
        FROM review r
        WHERE 1=1
          AND (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:courseId IS NULL OR EXISTS (
              SELECT 1 FROM course_review cr WHERE cr.review_id = r.id AND cr.course_id = :courseId
          ))
          AND (
              CASE :status
                  WHEN 'live' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'ongoing' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'current' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'completed' THEN r.end_date < :currentDate
                  WHEN 'ended' THEN r.end_date < :currentDate
                  WHEN 'past' THEN r.end_date < :currentDate
                  WHEN 'upcoming' THEN r.start_date > :currentDate
                  WHEN 'future' THEN r.start_date > :currentDate
                  ELSE TRUE
              END
          )
    """, nativeQuery = true)
    fun countSearchReviewsForAdmin(
        @Param("name") name: String?,
        @Param("courseId") courseId: UUID?,
        @Param("status") status: String?,
        @Param("currentDate") currentDate: String
    ): Int
    
    @Query(value = """
        SELECT DISTINCT r.id
        FROM review r
        JOIN course_review cr ON cr.review_id = r.id
        JOIN course_instructor ci ON ci.course_id = cr.course_id
        WHERE ci.instructor_id = :userId
          AND (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:courseId IS NULL OR cr.course_id = :courseId)
          AND (
              CASE :status
                  WHEN 'live' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'ongoing' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'current' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'completed' THEN r.end_date < :currentDate
                  WHEN 'ended' THEN r.end_date < :currentDate
                  WHEN 'past' THEN r.end_date < :currentDate
                  WHEN 'upcoming' THEN r.start_date > :currentDate
                  WHEN 'future' THEN r.start_date > :currentDate
                  ELSE TRUE
              END
          )
        ORDER BY 
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'asc' THEN r.name END ASC,
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'desc' THEN r.name END DESC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'asc' THEN r.end_date END ASC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'desc' THEN r.end_date END DESC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'asc' THEN r.start_date END ASC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'desc' THEN r.start_date END DESC
        OFFSET :offset ROWS FETCH FIRST :limit ROWS ONLY
    """, nativeQuery = true)
    fun searchReviewIdsForFaculty(
        @Param("userId") userId: String,
        @Param("name") name: String?,
        @Param("courseId") courseId: UUID?,
        @Param("status") status: String?,
        @Param("currentDate") currentDate: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT COUNT(DISTINCT r.id)
        FROM review r
        JOIN course_review cr ON cr.review_id = r.id
        JOIN course_instructor ci ON ci.course_id = cr.course_id
        WHERE ci.instructor_id = :userId
          AND (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:courseId IS NULL OR cr.course_id = :courseId)
          AND (
              CASE :status
                  WHEN 'live' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'ongoing' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'current' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'completed' THEN r.end_date < :currentDate
                  WHEN 'ended' THEN r.end_date < :currentDate
                  WHEN 'past' THEN r.end_date < :currentDate
                  WHEN 'upcoming' THEN r.start_date > :currentDate
                  WHEN 'future' THEN r.start_date > :currentDate
                  ELSE TRUE
              END
          )
    """, nativeQuery = true)
    fun countSearchReviewsForFaculty(
        @Param("userId") userId: String,
        @Param("name") name: String?,
        @Param("courseId") courseId: UUID?,
        @Param("status") status: String?,
        @Param("currentDate") currentDate: String
    ): Int
    
    @Query(value = """
        SELECT DISTINCT r.id
        FROM review r
        JOIN review_project rp ON rp.review_id = r.id
        JOIN project p ON p.id = rp.project_id
        JOIN team t ON t.id = p.team_id
        JOIN team_members tm ON tm.team_id = t.id
        WHERE tm.user_id = :userId
          AND (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:courseId IS NULL OR EXISTS (
              SELECT 1 FROM course_review cr WHERE cr.review_id = r.id AND cr.course_id = :courseId
          ))
          AND (
              CASE :status
                  WHEN 'live' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'ongoing' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'current' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'completed' THEN r.end_date < :currentDate
                  WHEN 'ended' THEN r.end_date < :currentDate
                  WHEN 'past' THEN r.end_date < :currentDate
                  WHEN 'upcoming' THEN r.start_date > :currentDate
                  WHEN 'future' THEN r.start_date > :currentDate
                  ELSE TRUE
              END
          )
        ORDER BY 
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'asc' THEN r.name END ASC,
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'desc' THEN r.name END DESC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'asc' THEN r.end_date END ASC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'desc' THEN r.end_date END DESC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'asc' THEN r.start_date END ASC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'desc' THEN r.start_date END DESC
        OFFSET :offset ROWS FETCH FIRST :limit ROWS ONLY
    """, nativeQuery = true)
    fun searchReviewIdsForStudent(
        @Param("userId") userId: String,
        @Param("name") name: String?,
        @Param("courseId") courseId: UUID?,
        @Param("status") status: String?,
        @Param("currentDate") currentDate: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT COUNT(DISTINCT r.id)
        FROM review r
        JOIN review_project rp ON rp.review_id = r.id
        JOIN project p ON p.id = rp.project_id
        JOIN team t ON t.id = p.team_id
        JOIN team_members tm ON tm.team_id = t.id
        WHERE tm.user_id = :userId
          AND (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:courseId IS NULL OR EXISTS (
              SELECT 1 FROM course_review cr WHERE cr.review_id = r.id AND cr.course_id = :courseId
          ))
          AND (
              CASE :status
                  WHEN 'live' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'ongoing' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'current' THEN r.start_date <= :currentDate AND r.end_date >= :currentDate
                  WHEN 'completed' THEN r.end_date < :currentDate
                  WHEN 'ended' THEN r.end_date < :currentDate
                  WHEN 'past' THEN r.end_date < :currentDate
                  WHEN 'upcoming' THEN r.start_date > :currentDate
                  WHEN 'future' THEN r.start_date > :currentDate
                  ELSE TRUE
              END
          )
    """, nativeQuery = true)
    fun countSearchReviewsForStudent(
        @Param("userId") userId: String,
        @Param("name") name: String?,
        @Param("courseId") courseId: UUID?,
        @Param("status") status: String?,
        @Param("currentDate") currentDate: String
    ): Int
    
    @Query(value = """
        SELECT r.id, 'DIRECT' as assignment_type
        FROM review r
        JOIN review_project rp ON rp.review_id = r.id
        WHERE rp.project_id = :projectId
        
        UNION
        
        SELECT r.id, 'COURSE' as assignment_type
        FROM review r
        JOIN course_review cr ON cr.review_id = r.id
        JOIN project_courses pc ON pc.course_id = cr.course_id
        WHERE pc.project_id = :projectId
        
        UNION
        
        SELECT r.id, 'BATCH' as assignment_type
        FROM review r
        JOIN review_batch rb ON rb.review_id = r.id
        JOIN batch_student bs ON rb.batch_id = bs.batch_id
        JOIN team_members tm ON tm.user_id = bs.student_id
        JOIN project p ON p.team_id = tm.team_id
        WHERE p.id = :projectId
        
        UNION
        
        SELECT r.id, 'SEMESTER' as assignment_type
        FROM review r
        JOIN course_review cr ON cr.review_id = r.id
        JOIN course c ON c.id = cr.course_id
        JOIN batch_semester bsem ON bsem.semester_id = c.semester_id
        JOIN batch_student bs ON bs.batch_id = bsem.batch_id
        JOIN team_members tm ON tm.user_id = bs.student_id
        JOIN project p ON p.team_id = tm.team_id
        WHERE p.id = :projectId
    """, nativeQuery = true)
    fun findReviewAssignmentsForProject(@Param("projectId") projectId: UUID): List<Map<String, Any>>
    
    @Query("""
        SELECT DISTINCT r FROM Review r 
        LEFT JOIN FETCH r.rubrics rb
        LEFT JOIN FETCH rb.criteria
        WHERE r.id = :reviewId
    """)
    fun findByIdWithRubrics(@Param("reviewId") reviewId: UUID): Review?
    
    @Query(value = """
        SELECT 
            COUNT(*) as total_count,
            SUM(CASE WHEN :currentDate BETWEEN start_date AND end_date THEN 1 ELSE 0 END) as active_count,
            SUM(CASE WHEN :currentDate > end_date THEN 1 ELSE 0 END) as completed_count
        FROM review
    """, nativeQuery = true)
    fun getReviewStats(@Param("currentDate") currentDate: LocalDate): Map<String, Any>
    
    @Query(value = """
        SELECT r.id, r.name, r.start_date, r.end_date
        FROM review r
        WHERE r.start_date > :currentDate
        ORDER BY r.start_date ASC
        LIMIT 5
    """, nativeQuery = true)
    fun findUpcomingReviews(@Param("currentDate") currentDate: LocalDate): List<Map<String, Any>>
}
