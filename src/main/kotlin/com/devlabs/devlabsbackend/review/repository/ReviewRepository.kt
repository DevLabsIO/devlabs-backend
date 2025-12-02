package com.devlabs.devlabsbackend.review.repository

import com.devlabs.devlabsbackend.core.constants.RoleConstants
import com.devlabs.devlabsbackend.review.domain.Review
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.*

interface ReviewRepository : JpaRepository<Review, UUID> {
    
    @Query(value = """
        SELECT r.id
        FROM review r
        ORDER BY 
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'asc' THEN r.name END ASC,
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'desc' THEN r.name END DESC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'asc' THEN r.end_date END ASC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'desc' THEN r.end_date END DESC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'asc' THEN r.start_date END ASC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'desc' THEN r.start_date END DESC,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'asc' THEN r.created_at END ASC NULLS LAST,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'desc' THEN r.created_at END DESC NULLS LAST
        OFFSET :offset ROWS FETCH FIRST :limit ROWS ONLY
    """, nativeQuery = true)
    fun findAllReviewIds(
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT COUNT(r.id)
        FROM review r
    """, nativeQuery = true)
    fun countAllReviews(): Int
    
    @Query(value = """
        SELECT r.id
        FROM (
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date, r.created_at
            FROM review r
            WHERE r.created_by_id = :userId
            OR EXISTS (
                -- Reviews with courses taught by the faculty
                SELECT 1
                FROM course_review cr
                JOIN course_instructor ci ON ci.course_id = cr.course_id
                WHERE cr.review_id = r.id
                  AND ci.instructor_id = :userId
            )
            OR EXISTS (
                -- Reviews with directly assigned projects from courses taught by the faculty
                SELECT 1
                FROM review_project rp
                JOIN project p ON p.id = rp.project_id
                JOIN project_courses pc ON pc.project_id = p.id
                JOIN course_instructor ci ON ci.course_id = pc.course_id
                WHERE rp.review_id = r.id
                  AND ci.instructor_id = :userId
            )
            OR EXISTS (
                -- Reviews assigned to batches where faculty teaches courses that have those batches
                SELECT 1
                FROM review_batch rb
                JOIN course_batch cb ON cb.batch_id = rb.batch_id
                JOIN course_instructor ci ON ci.course_id = cb.course_id
                WHERE rb.review_id = r.id
                  AND ci.instructor_id = :userId
            )
        ) r
        ORDER BY 
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'asc' THEN r.name END ASC,
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'desc' THEN r.name END DESC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'asc' THEN r.end_date END ASC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'desc' THEN r.end_date END DESC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'asc' THEN r.start_date END ASC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'desc' THEN r.start_date END DESC,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'asc' THEN r.created_at END ASC NULLS LAST,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'desc' THEN r.created_at END DESC NULLS LAST
        OFFSET :offset ROWS FETCH FIRST :limit ROWS ONLY
    """, nativeQuery = true)
    fun findReviewIdsForFaculty(
        @Param("userId") userId: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT COUNT(DISTINCT r.id)
        FROM review r
        WHERE r.created_by_id = :userId
        OR EXISTS (
            -- Reviews with courses taught by the faculty
            SELECT 1
            FROM course_review cr
            JOIN course_instructor ci ON ci.course_id = cr.course_id
            WHERE cr.review_id = r.id
              AND ci.instructor_id = :userId
        )
        OR EXISTS (
            -- Reviews with directly assigned projects from courses taught by the faculty
            SELECT 1
            FROM review_project rp
            JOIN project p ON p.id = rp.project_id
            JOIN project_courses pc ON pc.project_id = p.id
            JOIN course_instructor ci ON ci.course_id = pc.course_id
            WHERE rp.review_id = r.id
              AND ci.instructor_id = :userId
        )
        OR EXISTS (
            -- Reviews assigned to batches where faculty teaches courses that have those batches
            SELECT 1
            FROM review_batch rb
            JOIN course_batch cb ON cb.batch_id = rb.batch_id
            JOIN course_instructor ci ON ci.course_id = cb.course_id
            WHERE rb.review_id = r.id
              AND ci.instructor_id = :userId
        )
    """, nativeQuery = true)
    fun countReviewsForFaculty(@Param("userId") userId: String): Int
    
    @Query(value = """
        SELECT r.id
        FROM (
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date, r.created_at
            FROM review r
            WHERE r.id IN (
                -- Direct project assignment
                SELECT rp.review_id
                FROM review_project rp
                JOIN project p ON p.id = rp.project_id
                JOIN team t ON t.id = p.team_id
                JOIN team_members tm ON tm.team_id = t.id
                WHERE tm.user_id = :userId
                
                UNION
                
                -- Course assignment
                SELECT cr.review_id
                FROM course_review cr
                JOIN project_courses pc ON pc.course_id = cr.course_id
                JOIN project p ON p.id = pc.project_id
                JOIN team t ON t.id = p.team_id
                JOIN team_members tm ON tm.team_id = t.id
                WHERE tm.user_id = :userId
                
                UNION
                
                -- Batch assignment
                SELECT rb.review_id
                FROM review_batch rb
                JOIN batch_student bs ON rb.batch_id = bs.batch_id
                WHERE bs.student_id = :userId
                
                UNION
                
                -- Semester assignment
                SELECT cr.review_id
                FROM course_review cr
                JOIN course c ON c.id = cr.course_id
                JOIN batch_semester bsem ON bsem.semester_id = c.semester_id
                JOIN batch_student bs ON bs.batch_id = bsem.batch_id
                WHERE bs.student_id = :userId
            )
        ) r
        ORDER BY 
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'asc' THEN r.name END ASC,
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'desc' THEN r.name END DESC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'asc' THEN r.end_date END ASC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'desc' THEN r.end_date END DESC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'asc' THEN r.start_date END ASC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'desc' THEN r.start_date END DESC,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'asc' THEN r.created_at END ASC NULLS LAST,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'desc' THEN r.created_at END DESC NULLS LAST
        OFFSET :offset ROWS FETCH FIRST :limit ROWS ONLY
    """, nativeQuery = true)
    fun findReviewIdsForStudent(
        @Param("userId") userId: String,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int
    ): List<UUID>
    
    @Query(value = """
        SELECT COUNT(DISTINCT review_id)
        FROM (
            -- Direct project assignment
            SELECT rp.review_id
            FROM review_project rp
            JOIN project p ON p.id = rp.project_id
            JOIN team t ON t.id = p.team_id
            JOIN team_members tm ON tm.team_id = t.id
            WHERE tm.user_id = :userId
            
            UNION
            
            -- Course assignment
            SELECT cr.review_id
            FROM course_review cr
            JOIN project_courses pc ON pc.course_id = cr.course_id
            JOIN project p ON p.id = pc.project_id
            JOIN team t ON t.id = p.team_id
            JOIN team_members tm ON tm.team_id = t.id
            WHERE tm.user_id = :userId
            
            UNION
            
            -- Batch assignment
            SELECT rb.review_id
            FROM review_batch rb
            JOIN batch_student bs ON rb.batch_id = bs.batch_id
            WHERE bs.student_id = :userId
            
            UNION
            
            -- Semester assignment
            SELECT cr.review_id
            FROM course_review cr
            JOIN course c ON c.id = cr.course_id
            JOIN batch_semester bsem ON bsem.semester_id = c.semester_id
            JOIN batch_student bs ON bs.batch_id = bsem.batch_id
            WHERE bs.student_id = :userId
        ) all_reviews
    """, nativeQuery = true)
    fun countReviewsForStudent(@Param("userId") userId: String): Int


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
                WHEN ${RoleConstants.STUDENT} THEN 'STUDENT'
                WHEN ${RoleConstants.ADMIN} THEN 'ADMIN'
                WHEN ${RoleConstants.FACULTY} THEN 'FACULTY'
                WHEN ${RoleConstants.MANAGER} THEN 'MANAGER'
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
                WHEN ${RoleConstants.STUDENT} THEN 'STUDENT'
                WHEN ${RoleConstants.ADMIN} THEN 'ADMIN'
                WHEN ${RoleConstants.FACULTY} THEN 'FACULTY'
                WHEN ${RoleConstants.MANAGER} THEN 'MANAGER'
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
        SELECT DISTINCT
            review_id,
            id,
            name,
            code,
            semester_id,
            semester_name,
            semester_year,
            semester_is_active
        FROM (
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
            
            UNION
            
            SELECT 
                rp.review_id,
                c.id as id,
                c.name,
                c.code,
                s.id as semester_id,
                s.name as semester_name,
                s.year as semester_year,
                s.is_active as semester_is_active
            FROM review_project rp
            JOIN project p ON p.id = rp.project_id
            JOIN project_courses pc ON pc.project_id = p.id
            JOIN course c ON c.id = pc.course_id
            LEFT JOIN semester s ON s.id = c.semester_id
            WHERE rp.review_id IN :reviewIds
            
            UNION
            
            SELECT 
                rb.review_id,
                c.id as id,
                c.name,
                c.code,
                s.id as semester_id,
                s.name as semester_name,
                s.year as semester_year,
                s.is_active as semester_is_active
            FROM review_batch rb
            JOIN batch_student bs ON bs.batch_id = rb.batch_id
            JOIN team_members tm ON tm.user_id = bs.student_id
            JOIN project p ON p.team_id = tm.team_id
            JOIN project_courses pc ON pc.project_id = p.id
            JOIN course c ON c.id = pc.course_id
            LEFT JOIN semester s ON s.id = c.semester_id
            WHERE rb.review_id IN :reviewIds
        ) all_courses
    """, nativeQuery = true)
    fun findCoursesByReviewIds(@Param("reviewIds") reviewIds: List<UUID>): List<Map<String, Any>>
    

    @Query(value = """
        SELECT DISTINCT
            id,
            name,
            code,
            semester_id,
            semester_name,
            semester_year,
            semester_is_active
        FROM (
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
            
            UNION
            
            SELECT 
                c.id as id,
                c.name,
                c.code,
                s.id as semester_id,
                s.name as semester_name,
                s.year as semester_year,
                s.is_active as semester_is_active
            FROM review_project rp
            JOIN project p ON p.id = rp.project_id
            JOIN project_courses pc ON pc.project_id = p.id
            JOIN course c ON c.id = pc.course_id
            LEFT JOIN semester s ON s.id = c.semester_id
            WHERE rp.review_id = :reviewId
            
            UNION
            
            SELECT 
                c.id as id,
                c.name,
                c.code,
                s.id as semester_id,
                s.name as semester_name,
                s.year as semester_year,
                s.is_active as semester_is_active
            FROM review_batch rb
            JOIN batch_student bs ON bs.batch_id = rb.batch_id
            JOIN team_members tm ON tm.user_id = bs.student_id
            JOIN project p ON p.team_id = tm.team_id
            JOIN project_courses pc ON pc.project_id = p.id
            JOIN course c ON c.id = pc.course_id
            LEFT JOIN semester s ON s.id = c.semester_id
            WHERE rb.review_id = :reviewId
        ) all_courses
    """, nativeQuery = true)
    fun findCoursesByReviewId(@Param("reviewId") reviewId: UUID): List<Map<String, Any>>

    @Query(value = """
        SELECT DISTINCT id FROM (
            SELECT c.id
            FROM course_review c_r
            JOIN course c ON c.id = c_r.course_id
            WHERE c_r.review_id = :reviewId
            
            UNION
            
            SELECT c.id
            FROM review_project rp
            JOIN project p ON p.id = rp.project_id
            JOIN project_courses pc ON pc.project_id = p.id
            JOIN course c ON c.id = pc.course_id
            WHERE rp.review_id = :reviewId
            
            UNION
            
            SELECT c.id
            FROM review_batch rb
            JOIN batch_student bs ON bs.batch_id = rb.batch_id
            JOIN team_members tm ON tm.user_id = bs.student_id
            JOIN project p ON p.team_id = tm.team_id
            JOIN project_courses pc ON pc.project_id = p.id
            JOIN course c ON c.id = pc.course_id
            WHERE rb.review_id = :reviewId
        ) all_courses
    """, nativeQuery = true)
    fun findAllCourseIdsForReview(@Param("reviewId") reviewId: UUID): List<UUID>
    
    @Query(value = """
        SELECT DISTINCT
            rb.review_id,
            b.id,
            b.name
        FROM review_batch rb
        JOIN batch b ON b.id = rb.batch_id
        WHERE rb.review_id IN :reviewIds
    """, nativeQuery = true)
    fun findBatchesByReviewIds(@Param("reviewIds") reviewIds: List<UUID>): List<Map<String, Any>>
    
    @Query(value = """
        SELECT DISTINCT
            b.id,
            b.name
        FROM review_batch rb
        JOIN batch b ON b.id = rb.batch_id
        WHERE rb.review_id = :reviewId
    """, nativeQuery = true)
    fun findBatchesByReviewId(@Param("reviewId") reviewId: UUID): List<Map<String, Any>>
    
    // Get direct course mappings from course_review table (actual DB mappings, not derived)
    @Query(value = """
        SELECT cr.course_id
        FROM course_review cr
        WHERE cr.review_id = :reviewId
    """, nativeQuery = true)
    fun findDirectCourseIdsByReviewId(@Param("reviewId") reviewId: UUID): List<UUID>
    
    // Get direct batch mappings from review_batch table (actual DB mappings, not derived)
    @Query(value = """
        SELECT rb.batch_id
        FROM review_batch rb
        WHERE rb.review_id = :reviewId
    """, nativeQuery = true)
    fun findDirectBatchIdsByReviewId(@Param("reviewId") reviewId: UUID): List<UUID>
    
    // Get direct project mappings from review_project table (actual DB mappings, not derived)
    @Query(value = """
        SELECT rp.project_id
        FROM review_project rp
        WHERE rp.review_id = :reviewId
    """, nativeQuery = true)
    fun findDirectProjectIdsByReviewId(@Param("reviewId") reviewId: UUID): List<UUID>
    
    // Get semester IDs from directly mapped courses (actual DB mappings)
    @Query(value = """
        SELECT DISTINCT c.semester_id
        FROM course_review cr
        JOIN course c ON c.id = cr.course_id
        WHERE cr.review_id = :reviewId
        AND c.semester_id IS NOT NULL
    """, nativeQuery = true)
    fun findDirectSemesterIdsByReviewId(@Param("reviewId") reviewId: UUID): List<UUID>
    
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
            review_id,
            id,
            title,
            team_id,
            team_name,
            STRING_AGG(CAST(member_id AS VARCHAR), '|' ORDER BY member_id) as member_ids,
            STRING_AGG(member_name, '|' ORDER BY member_id) as member_names
        FROM (
            SELECT DISTINCT
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
            LEFT JOIN team_members tm ON tm.team_id = t.id
            LEFT JOIN "user" m ON m.id = tm.user_id
            WHERE pr.review_id IN :reviewIds
            
            UNION
            
            SELECT DISTINCT
                cr.review_id,
                p.id,
                p.title,
                t.id as team_id,
                t.name as team_name,
                m.id as member_id,
                m.name as member_name
            FROM course_review cr
            JOIN project_courses pc ON pc.course_id = cr.course_id
            JOIN project p ON p.id = pc.project_id
            JOIN team t ON t.id = p.team_id
            LEFT JOIN team_members tm ON tm.team_id = t.id
            LEFT JOIN "user" m ON m.id = tm.user_id
            WHERE cr.review_id IN :reviewIds
            
            UNION
            
            SELECT DISTINCT
                rb.review_id,
                p.id,
                p.title,
                t.id as team_id,
                t.name as team_name,
                m.id as member_id,
                m.name as member_name
            FROM review_batch rb
            JOIN batch_student bs ON bs.batch_id = rb.batch_id
            JOIN team_members tm ON tm.user_id = bs.student_id
            JOIN project p ON p.team_id = tm.team_id
            JOIN team t ON t.id = p.team_id
            LEFT JOIN "user" m ON m.id = tm.user_id
            WHERE rb.review_id IN :reviewIds
        ) all_projects
        GROUP BY review_id, id, title, team_id, team_name
        ORDER BY review_id, id
    """, nativeQuery = true)
    fun findProjectsByReviewIds(@Param("reviewIds") reviewIds: List<UUID>): List<Map<String, Any>>
    
    @Query(value = """
        SELECT 
            id,
            title,
            team_id,
            team_name,
            STRING_AGG(CAST(member_id AS VARCHAR), '|' ORDER BY member_id) as member_ids,
            STRING_AGG(member_name, '|' ORDER BY member_id) as member_names
        FROM (
            SELECT DISTINCT
                p.id,
                p.title,
                t.id as team_id,
                t.name as team_name,
                m.id as member_id,
                m.name as member_name
            FROM review_project pr
            JOIN project p ON p.id = pr.project_id
            JOIN team t ON t.id = p.team_id
            LEFT JOIN team_members tm ON tm.team_id = t.id
            LEFT JOIN "user" m ON m.id = tm.user_id
            WHERE pr.review_id = :reviewId
            
            UNION
            
            SELECT DISTINCT
                p.id,
                p.title,
                t.id as team_id,
                t.name as team_name,
                m.id as member_id,
                m.name as member_name
            FROM course_review cr
            JOIN project_courses pc ON pc.course_id = cr.course_id
            JOIN project p ON p.id = pc.project_id
            JOIN team t ON t.id = p.team_id
            LEFT JOIN team_members tm ON tm.team_id = t.id
            LEFT JOIN "user" m ON m.id = tm.user_id
            WHERE cr.review_id = :reviewId
            
            UNION
            
            SELECT DISTINCT
                p.id,
                p.title,
                t.id as team_id,
                t.name as team_name,
                m.id as member_id,
                m.name as member_name
            FROM review_batch rb
            JOIN batch_student bs ON bs.batch_id = rb.batch_id
            JOIN team_members tm ON tm.user_id = bs.student_id
            JOIN project p ON p.team_id = tm.team_id
            JOIN team t ON t.id = p.team_id
            LEFT JOIN "user" m ON m.id = tm.user_id
            WHERE rb.review_id = :reviewId
        ) all_projects
        GROUP BY id, title, team_id, team_name
        ORDER BY id
    """, nativeQuery = true)
    fun findProjectsByReviewId(@Param("reviewId") reviewId: UUID): List<Map<String, Any>>
    
    @Query(value = """
        SELECT review_id, COUNT(*) as count
        FROM review_course_publication
        WHERE review_id IN :reviewIds
        GROUP BY review_id
    """, nativeQuery = true)
    fun findPublishedCountsByReviewIds(@Param("reviewIds") reviewIds: List<UUID>): List<Map<String, Any>>
    

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
            WHEN :role IN ('ADMIN', 'MANAGER', 'FACULTY') THEN
                EXISTS(SELECT 1 FROM review WHERE id = :reviewId)
            WHEN :role = 'STUDENT' THEN
                EXISTS(
                    SELECT 1 FROM review_project rp
                    JOIN project p ON p.id = rp.project_id
                    JOIN team t ON t.id = p.team_id
                    JOIN team_members tm ON tm.team_id = t.id
                    WHERE rp.review_id = :reviewId
                      AND tm.user_id = :userId
                )
            ELSE FALSE
        END
    """, nativeQuery = true)
    fun hasUserAccessToReview(
        @Param("reviewId") reviewId: UUID,
        @Param("userId") userId: String,
        @Param("role") role: String
    ): Boolean
    
    @Query(value = """
        SELECT r.id
        FROM (
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date, r.created_at
            FROM review r
            WHERE 1=1
              AND (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:courseId IS NULL OR EXISTS (
                  SELECT 1 FROM course_review cr WHERE cr.review_id = r.id AND cr.course_id = :courseId
              ))
              AND (
                  CASE :status
                      WHEN 'live' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                      WHEN 'ongoing' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                      WHEN 'current' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                      WHEN 'completed' THEN r.end_date < CAST(:currentDate AS DATE)
                      WHEN 'ended' THEN r.end_date < CAST(:currentDate AS DATE)
                      WHEN 'past' THEN r.end_date < CAST(:currentDate AS DATE)
                      WHEN 'upcoming' THEN r.start_date > CAST(:currentDate AS DATE)
                      WHEN 'future' THEN r.start_date > CAST(:currentDate AS DATE)
                      ELSE TRUE
                  END
              )
        ) r
        ORDER BY 
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'asc' THEN r.name END ASC,
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'desc' THEN r.name END DESC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'asc' THEN r.end_date END ASC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'desc' THEN r.end_date END DESC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'asc' THEN r.start_date END ASC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'desc' THEN r.start_date END DESC,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'asc' THEN r.created_at END ASC NULLS LAST,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'desc' THEN r.created_at END DESC NULLS LAST
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
                  WHEN 'live' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                  WHEN 'ongoing' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                  WHEN 'current' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                  WHEN 'completed' THEN r.end_date < CAST(:currentDate AS DATE)
                  WHEN 'ended' THEN r.end_date < CAST(:currentDate AS DATE)
                  WHEN 'past' THEN r.end_date < CAST(:currentDate AS DATE)
                  WHEN 'upcoming' THEN r.start_date > CAST(:currentDate AS DATE)
                  WHEN 'future' THEN r.start_date > CAST(:currentDate AS DATE)
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
        SELECT r.id
        FROM (
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date, r.created_at
            FROM review r
            WHERE (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:courseId IS NULL OR EXISTS (
                  SELECT 1 FROM course_review cr WHERE cr.review_id = r.id AND cr.course_id = :courseId
              ))
              AND (
                  CASE :status
                      WHEN 'live' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                      WHEN 'ongoing' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                      WHEN 'current' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                      WHEN 'completed' THEN r.end_date < CAST(:currentDate AS DATE)
                      WHEN 'ended' THEN r.end_date < CAST(:currentDate AS DATE)
                      WHEN 'past' THEN r.end_date < CAST(:currentDate AS DATE)
                      WHEN 'upcoming' THEN r.start_date > CAST(:currentDate AS DATE)
                      WHEN 'future' THEN r.start_date > CAST(:currentDate AS DATE)
                      ELSE TRUE
                  END
              )
              AND (
                  -- Reviews created by the faculty
                  r.created_by_id = :userId
                  OR EXISTS (
                      -- Reviews with courses taught by the faculty
                      SELECT 1
                      FROM course_review cr
                      JOIN course_instructor ci ON ci.course_id = cr.course_id
                      WHERE cr.review_id = r.id
                        AND ci.instructor_id = :userId
                  )
                  OR EXISTS (
                      -- Reviews with directly assigned projects from courses taught by the faculty
                      SELECT 1
                      FROM review_project rp
                      JOIN project p ON p.id = rp.project_id
                      JOIN project_courses pc ON pc.project_id = p.id
                      JOIN course_instructor ci ON ci.course_id = pc.course_id
                      WHERE rp.review_id = r.id
                        AND ci.instructor_id = :userId
                  )
                  OR EXISTS (
                      -- Reviews assigned to batches where faculty teaches courses that have those batches
                      SELECT 1
                      FROM review_batch rb
                      JOIN course_batch cb ON cb.batch_id = rb.batch_id
                      JOIN course_instructor ci ON ci.course_id = cb.course_id
                      WHERE rb.review_id = r.id
                        AND ci.instructor_id = :userId
                  )
              )
        ) r
        ORDER BY 
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'asc' THEN r.name END ASC,
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'desc' THEN r.name END DESC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'asc' THEN r.end_date END ASC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'desc' THEN r.end_date END DESC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'asc' THEN r.start_date END ASC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'desc' THEN r.start_date END DESC,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'asc' THEN r.created_at END ASC NULLS LAST,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'desc' THEN r.created_at END DESC NULLS LAST
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
        WHERE (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:courseId IS NULL OR EXISTS (
              SELECT 1 FROM course_review cr WHERE cr.review_id = r.id AND cr.course_id = :courseId
          ))
          AND (
              CASE :status
                  WHEN 'live' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                  WHEN 'ongoing' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                  WHEN 'current' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                  WHEN 'completed' THEN r.end_date < CAST(:currentDate AS DATE)
                  WHEN 'ended' THEN r.end_date < CAST(:currentDate AS DATE)
                  WHEN 'past' THEN r.end_date < CAST(:currentDate AS DATE)
                  WHEN 'upcoming' THEN r.start_date > CAST(:currentDate AS DATE)
                  WHEN 'future' THEN r.start_date > CAST(:currentDate AS DATE)
                  ELSE TRUE
              END
          )
          AND (
              -- Reviews created by the faculty
              r.created_by_id = :userId
              OR EXISTS (
                  -- Reviews with courses taught by the faculty
                  SELECT 1
                  FROM course_review cr
                  JOIN course_instructor ci ON ci.course_id = cr.course_id
                  WHERE cr.review_id = r.id
                    AND ci.instructor_id = :userId
              )
              OR EXISTS (
                  -- Reviews with directly assigned projects from courses taught by the faculty
                  SELECT 1
                  FROM review_project rp
                  JOIN project p ON p.id = rp.project_id
                  JOIN project_courses pc ON pc.project_id = p.id
                  JOIN course_instructor ci ON ci.course_id = pc.course_id
                  WHERE rp.review_id = r.id
                    AND ci.instructor_id = :userId
              )
              OR EXISTS (
                  -- Reviews assigned to batches where faculty teaches courses that have those batches
                  SELECT 1
                  FROM review_batch rb
                  JOIN course_batch cb ON cb.batch_id = rb.batch_id
                  JOIN course_instructor ci ON ci.course_id = cb.course_id
                  WHERE rb.review_id = r.id
                    AND ci.instructor_id = :userId
              )
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
        SELECT r.id
        FROM (
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date, r.created_at
            FROM review r
            WHERE r.id IN (
                -- Direct project assignment
                SELECT rp.review_id
                FROM review_project rp
                JOIN project p ON p.id = rp.project_id
                JOIN team t ON t.id = p.team_id
                JOIN team_members tm ON tm.team_id = t.id
                WHERE tm.user_id = :userId
                
                UNION
                
                -- Course assignment
                SELECT cr.review_id
                FROM course_review cr
                JOIN project_courses pc ON pc.course_id = cr.course_id
                JOIN project p ON p.id = pc.project_id
                JOIN team t ON t.id = p.team_id
                JOIN team_members tm ON tm.team_id = t.id
                WHERE tm.user_id = :userId
                
                UNION
                
                -- Batch assignment
                SELECT rb.review_id
                FROM review_batch rb
                JOIN batch_student bs ON rb.batch_id = bs.batch_id
                WHERE bs.student_id = :userId
                
                UNION
                
                -- Semester assignment
                SELECT cr.review_id
                FROM course_review cr
                JOIN course c ON c.id = cr.course_id
                JOIN batch_semester bsem ON bsem.semester_id = c.semester_id
                JOIN batch_student bs ON bs.batch_id = bsem.batch_id
                WHERE bs.student_id = :userId
            )
              AND (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:courseId IS NULL OR EXISTS (
                  SELECT 1 FROM course_review cr WHERE cr.review_id = r.id AND cr.course_id = :courseId
              ))
              AND (
                  CASE :status
                      WHEN 'live' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                      WHEN 'ongoing' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                      WHEN 'current' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                      WHEN 'completed' THEN r.end_date < CAST(:currentDate AS DATE)
                      WHEN 'ended' THEN r.end_date < CAST(:currentDate AS DATE)
                      WHEN 'past' THEN r.end_date < CAST(:currentDate AS DATE)
                      WHEN 'upcoming' THEN r.start_date > CAST(:currentDate AS DATE)
                      WHEN 'future' THEN r.start_date > CAST(:currentDate AS DATE)
                      ELSE TRUE
                  END
              )
        ) r
        ORDER BY 
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'asc' THEN r.name END ASC,
          CASE WHEN :sortBy = 'name' AND :sortOrder = 'desc' THEN r.name END DESC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'asc' THEN r.end_date END ASC,
          CASE WHEN :sortBy = 'endDate' AND :sortOrder = 'desc' THEN r.end_date END DESC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'asc' THEN r.start_date END ASC,
          CASE WHEN :sortBy = 'startDate' AND :sortOrder = 'desc' THEN r.start_date END DESC,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'asc' THEN r.created_at END ASC NULLS LAST,
          CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'desc' THEN r.created_at END DESC NULLS LAST
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
        SELECT COUNT(DISTINCT review_id)
        FROM (
            SELECT r.id as review_id
            FROM review r
            WHERE r.id IN (
                -- Direct project assignment
                SELECT rp.review_id
                FROM review_project rp
                JOIN project p ON p.id = rp.project_id
                JOIN team t ON t.id = p.team_id
                JOIN team_members tm ON tm.team_id = t.id
                WHERE tm.user_id = :userId
                
                UNION
                
                -- Course assignment
                SELECT cr.review_id
                FROM course_review cr
                JOIN project_courses pc ON pc.course_id = cr.course_id
                JOIN project p ON p.id = pc.project_id
                JOIN team t ON t.id = p.team_id
                JOIN team_members tm ON tm.team_id = t.id
                WHERE tm.user_id = :userId
                
                UNION
                
                -- Batch assignment
                SELECT rb.review_id
                FROM review_batch rb
                JOIN batch_student bs ON rb.batch_id = bs.batch_id
                WHERE bs.student_id = :userId
                
                UNION
                
                -- Semester assignment
                SELECT cr.review_id
                FROM course_review cr
                JOIN course c ON c.id = cr.course_id
                JOIN batch_semester bsem ON bsem.semester_id = c.semester_id
                JOIN batch_student bs ON bs.batch_id = bsem.batch_id
                WHERE bs.student_id = :userId
            )
              AND (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:courseId IS NULL OR EXISTS (
                  SELECT 1 FROM course_review cr WHERE cr.review_id = r.id AND cr.course_id = :courseId
              ))
              AND (
                  CASE :status
                      WHEN 'live' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                      WHEN 'ongoing' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                      WHEN 'current' THEN r.start_date <= CAST(:currentDate AS DATE) AND r.end_date >= CAST(:currentDate AS DATE)
                      WHEN 'completed' THEN r.end_date < CAST(:currentDate AS DATE)
                      WHEN 'ended' THEN r.end_date < CAST(:currentDate AS DATE)
                      WHEN 'past' THEN r.end_date < CAST(:currentDate AS DATE)
                      WHEN 'upcoming' THEN r.start_date > CAST(:currentDate AS DATE)
                      WHEN 'future' THEN r.start_date > CAST(:currentDate AS DATE)
                      ELSE TRUE
                  END
              )
        ) all_reviews
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
        WITH active_semester_reviews AS (
            -- Reviews assigned to courses in active semesters
            SELECT DISTINCT r.id, r.start_date, r.end_date
            FROM review r
            JOIN course_review cr ON cr.review_id = r.id
            JOIN course c ON c.id = cr.course_id
            JOIN semester s ON s.id = c.semester_id
            WHERE s.is_active = true
            
            UNION
            
            -- Reviews assigned to batches in active semesters
            SELECT DISTINCT r.id, r.start_date, r.end_date
            FROM review r
            JOIN review_batch rb ON rb.review_id = r.id
            JOIN batch_semester bsem ON bsem.batch_id = rb.batch_id
            JOIN semester s ON s.id = bsem.semester_id
            WHERE s.is_active = true
            
            UNION
            
            -- Reviews with directly assigned projects in active semesters
            SELECT DISTINCT r.id, r.start_date, r.end_date
            FROM review r
            JOIN review_project rp ON rp.review_id = r.id
            JOIN project p ON p.id = rp.project_id
            JOIN project_courses pc ON pc.project_id = p.id
            JOIN course c ON c.id = pc.course_id
            JOIN semester s ON s.id = c.semester_id
            WHERE s.is_active = true
        )
        SELECT 
            COUNT(DISTINCT id) as total_count,
            SUM(CASE WHEN :currentDate BETWEEN start_date AND end_date THEN 1 ELSE 0 END) as active_count,
            SUM(CASE WHEN :currentDate > end_date THEN 1 ELSE 0 END) as completed_count
        FROM active_semester_reviews
    """, nativeQuery = true)
    fun getReviewStatsForActiveSemesters(@Param("currentDate") currentDate: LocalDate): Map<String, Any>
    
    @Query(value = """
        WITH active_semester_reviews AS (
            -- Reviews assigned to courses in active semesters
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date
            FROM review r
            JOIN course_review cr ON cr.review_id = r.id
            JOIN course c ON c.id = cr.course_id
            JOIN semester s ON s.id = c.semester_id
            WHERE s.is_active = true 
            AND r.start_date > :currentDate
            
            UNION
            
            -- Reviews assigned to batches in active semesters
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date
            FROM review r
            JOIN review_batch rb ON rb.review_id = r.id
            JOIN batch_semester bsem ON bsem.batch_id = rb.batch_id
            JOIN semester s ON s.id = bsem.semester_id
            WHERE s.is_active = true 
            AND r.start_date > :currentDate
            
            UNION
            
            -- Reviews with directly assigned projects in active semesters
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date
            FROM review r
            JOIN review_project rp ON rp.review_id = r.id
            JOIN project p ON p.id = rp.project_id
            JOIN project_courses pc ON pc.project_id = p.id
            JOIN course c ON c.id = pc.course_id
            JOIN semester s ON s.id = c.semester_id
            WHERE s.is_active = true 
            AND r.start_date > :currentDate
        )
        SELECT id, name, start_date, end_date
        FROM active_semester_reviews
        ORDER BY start_date ASC
        LIMIT 5
    """, nativeQuery = true)
    fun findUpcomingReviewsForActiveSemesters(@Param("currentDate") currentDate: LocalDate): List<Map<String, Any>>

    // ==================== FACULTY DASHBOARD QUERIES ====================
    
    @Query(value = """
        WITH faculty_reviews AS (
            -- Reviews created by the faculty
            SELECT DISTINCT r.id, r.start_date, r.end_date
            FROM review r
            WHERE r.created_by_id = :userId
            
            UNION
            
            -- Reviews assigned to courses taught by the faculty
            SELECT DISTINCT r.id, r.start_date, r.end_date
            FROM review r
            JOIN course_review cr ON cr.review_id = r.id
            JOIN course_instructor ci ON ci.course_id = cr.course_id
            WHERE ci.instructor_id = :userId
            
            UNION
            
            -- Reviews with directly assigned projects from courses taught by the faculty
            SELECT DISTINCT r.id, r.start_date, r.end_date
            FROM review r
            JOIN review_project rp ON rp.review_id = r.id
            JOIN project p ON p.id = rp.project_id
            JOIN project_courses pc ON pc.project_id = p.id
            JOIN course_instructor ci ON ci.course_id = pc.course_id
            WHERE ci.instructor_id = :userId
            
            UNION
            
            -- Reviews assigned to batches where faculty teaches courses that have those batches
            SELECT DISTINCT r.id, r.start_date, r.end_date
            FROM review r
            JOIN review_batch rb ON rb.review_id = r.id
            JOIN course_batch cb ON cb.batch_id = rb.batch_id
            JOIN course_instructor ci ON ci.course_id = cb.course_id
            WHERE ci.instructor_id = :userId
        )
        SELECT 
            COUNT(DISTINCT id) as total_count,
            SUM(CASE WHEN :currentDate BETWEEN start_date AND end_date THEN 1 ELSE 0 END) as active_count,
            SUM(CASE WHEN :currentDate > end_date THEN 1 ELSE 0 END) as completed_count
        FROM faculty_reviews
    """, nativeQuery = true)
    fun getReviewStatsForFaculty(
        @Param("userId") userId: String,
        @Param("currentDate") currentDate: LocalDate
    ): Map<String, Any>
    
    @Query(value = """
        WITH faculty_reviews AS (
            -- Reviews created by the faculty
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date
            FROM review r
            WHERE r.created_by_id = :userId
            AND r.start_date > :currentDate
            
            UNION
            
            -- Reviews assigned to courses taught by the faculty
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date
            FROM review r
            JOIN course_review cr ON cr.review_id = r.id
            JOIN course_instructor ci ON ci.course_id = cr.course_id
            WHERE ci.instructor_id = :userId
            AND r.start_date > :currentDate
            
            UNION
            
            -- Reviews with directly assigned projects from courses taught by the faculty
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date
            FROM review r
            JOIN review_project rp ON rp.review_id = r.id
            JOIN project p ON p.id = rp.project_id
            JOIN project_courses pc ON pc.project_id = p.id
            JOIN course_instructor ci ON ci.course_id = pc.course_id
            WHERE ci.instructor_id = :userId
            AND r.start_date > :currentDate
            
            UNION
            
            -- Reviews assigned to batches where faculty teaches courses that have those batches
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date
            FROM review r
            JOIN review_batch rb ON rb.review_id = r.id
            JOIN course_batch cb ON cb.batch_id = rb.batch_id
            JOIN course_instructor ci ON ci.course_id = cb.course_id
            WHERE ci.instructor_id = :userId
            AND r.start_date > :currentDate
        )
        SELECT id, name, start_date, end_date
        FROM faculty_reviews
        ORDER BY start_date ASC
        LIMIT 5
    """, nativeQuery = true)
    fun findUpcomingReviewsForFaculty(
        @Param("userId") userId: String,
        @Param("currentDate") currentDate: LocalDate
    ): List<Map<String, Any>>

    // ==================== STUDENT DASHBOARD QUERIES ====================
    
    @Query(value = """
        WITH student_reviews AS (
            -- Reviews with directly assigned projects where student is a team member
            SELECT DISTINCT r.id, r.start_date, r.end_date
            FROM review r
            JOIN review_project rp ON rp.review_id = r.id
            JOIN project p ON p.id = rp.project_id
            JOIN team_members tm ON tm.team_id = p.team_id
            WHERE tm.user_id = :userId
            
            UNION
            
            -- Reviews assigned to courses the student is enrolled in
            SELECT DISTINCT r.id, r.start_date, r.end_date
            FROM review r
            JOIN course_review cr ON cr.review_id = r.id
            JOIN course_student cs ON cs.course_id = cr.course_id
            WHERE cs.student_id = :userId
            
            UNION
            
            -- Reviews assigned to batches the student belongs to
            SELECT DISTINCT r.id, r.start_date, r.end_date
            FROM review r
            JOIN review_batch rb ON rb.review_id = r.id
            JOIN batch_student bs ON bs.batch_id = rb.batch_id
            WHERE bs.student_id = :userId
            
            UNION
            
            -- Reviews assigned to courses where student is in a batch that has that course
            SELECT DISTINCT r.id, r.start_date, r.end_date
            FROM review r
            JOIN course_review cr ON cr.review_id = r.id
            JOIN course_batch cb ON cb.course_id = cr.course_id
            JOIN batch_student bs ON bs.batch_id = cb.batch_id
            WHERE bs.student_id = :userId
        )
        SELECT 
            COUNT(DISTINCT id) as total_count,
            SUM(CASE WHEN :currentDate BETWEEN start_date AND end_date THEN 1 ELSE 0 END) as active_count,
            SUM(CASE WHEN :currentDate > end_date THEN 1 ELSE 0 END) as completed_count
        FROM student_reviews
    """, nativeQuery = true)
    fun getReviewStatsForStudent(
        @Param("userId") userId: String,
        @Param("currentDate") currentDate: LocalDate
    ): Map<String, Any>
    
    @Query(value = """
        WITH student_reviews AS (
            -- Reviews with directly assigned projects where student is a team member
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date
            FROM review r
            JOIN review_project rp ON rp.review_id = r.id
            JOIN project p ON p.id = rp.project_id
            JOIN team_members tm ON tm.team_id = p.team_id
            WHERE tm.user_id = :userId
            AND r.start_date > :currentDate
            
            UNION
            
            -- Reviews assigned to courses the student is enrolled in
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date
            FROM review r
            JOIN course_review cr ON cr.review_id = r.id
            JOIN course_student cs ON cs.course_id = cr.course_id
            WHERE cs.student_id = :userId
            AND r.start_date > :currentDate
            
            UNION
            
            -- Reviews assigned to batches the student belongs to
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date
            FROM review r
            JOIN review_batch rb ON rb.review_id = r.id
            JOIN batch_student bs ON bs.batch_id = rb.batch_id
            WHERE bs.student_id = :userId
            AND r.start_date > :currentDate
            
            UNION
            
            -- Reviews assigned to courses where student is in a batch that has that course
            SELECT DISTINCT r.id, r.name, r.start_date, r.end_date
            FROM review r
            JOIN course_review cr ON cr.review_id = r.id
            JOIN course_batch cb ON cb.course_id = cr.course_id
            JOIN batch_student bs ON bs.batch_id = cb.batch_id
            WHERE bs.student_id = :userId
            AND r.start_date > :currentDate
        )
        SELECT id, name, start_date, end_date
        FROM student_reviews
        ORDER BY start_date ASC
        LIMIT 5
    """, nativeQuery = true)
    fun findUpcomingReviewsForStudent(
        @Param("userId") userId: String,
        @Param("currentDate") currentDate: LocalDate
    ): List<Map<String, Any>>
    
    @Query(value = """
        WITH review_projects AS (
            SELECT DISTINCT 
                p.id as project_id,
                p.title as project_title,
                t.id as team_id,
                t.name as team_name
            FROM review_project rp
            JOIN project p ON p.id = rp.project_id
            JOIN team t ON t.id = p.team_id
            WHERE rp.review_id = :reviewId
            
            UNION
            
            SELECT DISTINCT 
                p.id as project_id,
                p.title as project_title,
                t.id as team_id,
                t.name as team_name
            FROM course_review cr
            JOIN project_courses pc ON pc.course_id = cr.course_id
            JOIN project p ON p.id = pc.project_id
            JOIN team t ON t.id = p.team_id
            WHERE cr.review_id = :reviewId
            
            UNION
            
            SELECT DISTINCT 
                p.id as project_id,
                p.title as project_title,
                t.id as team_id,
                t.name as team_name
            FROM review_batch rb
            JOIN batch_student bs ON bs.batch_id = rb.batch_id
            JOIN team_members tm ON tm.user_id = bs.student_id
            JOIN project p ON p.team_id = tm.team_id
            JOIN team t ON t.id = p.team_id
            WHERE rb.review_id = :reviewId
        )
        SELECT DISTINCT
            rp.project_id,
            rp.project_title,
            rp.team_id,
            rp.team_name,
            STRING_AGG(DISTINCT CAST(m.id AS VARCHAR), '|') as member_ids,
            STRING_AGG(DISTINCT m.profile_id, '|') as member_profile_ids,
            STRING_AGG(DISTINCT m.name, '|') as member_names,
            STRING_AGG(DISTINCT m.email, '|') as member_emails,
            STRING_AGG(DISTINCT CAST(bs.batch_id AS VARCHAR), '|') as batch_ids,
            STRING_AGG(DISTINCT CAST(pc.course_id AS VARCHAR), '|') as course_ids
        FROM review_projects rp
        LEFT JOIN team_members tm ON tm.team_id = rp.team_id
        LEFT JOIN "user" m ON m.id = tm.user_id
        LEFT JOIN batch_student bs ON bs.student_id = m.id
        LEFT JOIN project_courses pc ON pc.project_id = rp.project_id
        WHERE (:teamId IS NULL OR rp.team_id = CAST(:teamId AS UUID))
          AND (:batchId IS NULL OR bs.batch_id = CAST(:batchId AS UUID))
          AND (:courseId IS NULL OR pc.course_id = CAST(:courseId AS UUID))
          AND (
              :userRole IN ('ADMIN', 'MANAGER')
              OR :userId IS NULL
              OR EXISTS (
                  SELECT 1 FROM project_courses pc2
                  JOIN course_instructor ci ON ci.course_id = pc2.course_id
                  JOIN course c ON c.id = ci.course_id
                  JOIN semester s ON s.id = c.semester_id
                  WHERE pc2.project_id = rp.project_id
                    AND ci.instructor_id = :userId
                    AND s.is_active = true
              )
          )
        GROUP BY rp.project_id, rp.project_title, rp.team_id, rp.team_name
        ORDER BY rp.team_name, rp.project_title
    """, nativeQuery = true)
    fun findFilteredProjectsByReviewId(
        @Param("reviewId") reviewId: UUID,
        @Param("userId") userId: String?,
        @Param("userRole") userRole: String,
        @Param("teamId") teamId: String?,
        @Param("batchId") batchId: String?,
        @Param("courseId") courseId: String?
    ): List<Map<String, Any>>

    @Query(value = """
        SELECT DISTINCT c.id
        FROM (
            SELECT c.id
            FROM course_review c_r
            JOIN course c ON c.id = c_r.course_id
            JOIN course_instructor ci ON ci.course_id = c.id
            WHERE c_r.review_id = :reviewId AND ci.instructor_id = :facultyId
            
            UNION
            
            SELECT c.id
            FROM review_project rp
            JOIN project p ON p.id = rp.project_id
            JOIN project_courses pc ON pc.project_id = p.id
            JOIN course c ON c.id = pc.course_id
            JOIN course_instructor ci ON ci.course_id = c.id
            WHERE rp.review_id = :reviewId AND ci.instructor_id = :facultyId
            
            UNION
            
            SELECT c.id
            FROM review_batch rb
            JOIN batch_student bs ON bs.batch_id = rb.batch_id
            JOIN team_members tm ON tm.user_id = bs.student_id
            JOIN project p ON p.team_id = tm.team_id
            JOIN project_courses pc ON pc.project_id = p.id
            JOIN course c ON c.id = pc.course_id
            JOIN course_instructor ci ON ci.course_id = c.id
            WHERE rb.review_id = :reviewId AND ci.instructor_id = :facultyId
        ) c
    """, nativeQuery = true)
    fun findFacultyCourseIdsForReview(
        @Param("reviewId") reviewId: UUID,
        @Param("facultyId") facultyId: String
    ): List<UUID>
}
