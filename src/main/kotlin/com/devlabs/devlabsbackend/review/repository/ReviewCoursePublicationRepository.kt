package com.devlabs.devlabsbackend.review.repository

import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.review.domain.ReviewCoursePublication
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface ReviewCoursePublicationRepository : JpaRepository<ReviewCoursePublication, UUID> {
    
    @Query("SELECT rcp FROM ReviewCoursePublication rcp WHERE rcp.review = :review")
    fun findByReview(@Param("review") review: Review): List<ReviewCoursePublication>
    
    @Query("SELECT rcp FROM ReviewCoursePublication rcp WHERE rcp.review = :review AND rcp.course = :course")
    fun findByReviewAndCourse(@Param("review") review: Review, @Param("course") course: Course): ReviewCoursePublication?
    
    @Query("SELECT rcp.course FROM ReviewCoursePublication rcp WHERE rcp.review = :review")
    fun findPublishedCoursesByReview(@Param("review") review: Review): List<Course>
    
    @Modifying
    @Query("DELETE FROM ReviewCoursePublication rcp WHERE rcp.review = :review")
    fun deleteByReview(@Param("review") review: Review)
    
    @Modifying
    @Query("DELETE FROM ReviewCoursePublication rcp WHERE rcp.review = :review AND rcp.course = :course")
    fun deleteByReviewAndCourse(@Param("review") review: Review, @Param("course") course: Course)
    
    @Query("""
        SELECT DISTINCT rcp FROM ReviewCoursePublication rcp 
        LEFT JOIN FETCH rcp.review r
        LEFT JOIN FETCH rcp.course c
        LEFT JOIN FETCH rcp.publishedBy pb
        WHERE rcp.course IN :courses 
        ORDER BY rcp.publishedAt DESC
    """)
    fun findRecentPublicationsByCourses(@Param("courses") courses: List<Course>): List<ReviewCoursePublication>
    
    @Query("""
        SELECT DISTINCT rcp FROM ReviewCoursePublication rcp 
        WHERE rcp.course IN :courses
    """)
    fun findReviewsByCourses(@Param("courses") courses: List<Course>): List<ReviewCoursePublication>
    
    @Query("""
        SELECT rcp.review.id as reviewId, COUNT(rcp) as publishedCount
        FROM ReviewCoursePublication rcp
        WHERE rcp.review IN :reviews
        GROUP BY rcp.review.id
    """)
    fun countPublishedCoursesByReviews(@Param("reviews") reviews: List<Review>): List<Map<String, Any>>
    
    @Query(value = """
        SELECT CASE 
            WHEN :role IN ('ADMIN', 'MANAGER') THEN
                -- For ADMIN/MANAGER: Check if published count equals total courses
                (SELECT COUNT(DISTINCT rcp.course_id)
                 FROM review_course_publication rcp
                 WHERE rcp.review_id = :reviewId) =
                (SELECT COUNT(DISTINCT cr.course_id)
                 FROM course_review cr
                 WHERE cr.review_id = :reviewId)
            WHEN :role = 'FACULTY' THEN
                -- For FACULTY: Check if any instructor course is published
                (SELECT COUNT(*)
                 FROM review_course_publication rcp
                 JOIN course_instructor ci ON ci.course_id = rcp.course_id
                 WHERE rcp.review_id = :reviewId
                   AND ci.instructor_id = :userId) > 0
            WHEN :role = 'STUDENT' THEN
                -- For STUDENT: Check if any enrolled course (direct or through batch) is published
                (SELECT COUNT(*)
                 FROM review_course_publication rcp
                 WHERE rcp.review_id = :reviewId
                   AND (
                       -- Direct enrollment
                       EXISTS (
                           SELECT 1
                           FROM course_student cs
                           WHERE cs.course_id = rcp.course_id
                             AND cs.student_id = :userId
                       )
                       OR
                       -- Batch enrollment
                       EXISTS (
                           SELECT 1
                           FROM course_batch cb
                           JOIN batch_student bs ON cb.batch_id = bs.batch_id
                           WHERE cb.course_id = rcp.course_id
                             AND bs.student_id = :userId
                       )
                   )
                ) > 0
            ELSE FALSE
        END
    """, nativeQuery = true)
    fun isPublishedForUser(
        @Param("reviewId") reviewId: UUID,
        @Param("userId") userId: String,
        @Param("role") role: String
    ): Boolean
    

    @Query(value = """
        SELECT r.id as review_id,
               CASE 
                   WHEN :role IN ('ADMIN', 'MANAGER') THEN
                       (SELECT COUNT(DISTINCT rcp.course_id)
                        FROM review_course_publication rcp
                        WHERE rcp.review_id = r.id) =
                       (SELECT COUNT(DISTINCT cr.course_id)
                        FROM course_review cr
                        WHERE cr.review_id = r.id)
                   WHEN :role = 'FACULTY' THEN
                       EXISTS(
                           SELECT 1
                           FROM review_course_publication rcp
                           JOIN course_instructor ci ON ci.course_id = rcp.course_id
                           WHERE rcp.review_id = r.id
                             AND ci.instructor_id = :userId
                       )
                   WHEN :role = 'STUDENT' THEN
                       EXISTS(
                           SELECT 1
                           FROM review_course_publication rcp
                           WHERE rcp.review_id = r.id
                             AND (
                                 EXISTS (
                                     SELECT 1
                                     FROM course_student cs
                                     WHERE cs.course_id = rcp.course_id
                                       AND cs.student_id = :userId
                                 )
                                 OR
                                 EXISTS (
                                     SELECT 1
                                     FROM course_batch cb
                                     JOIN batch_student bs ON cb.batch_id = bs.batch_id
                                     WHERE cb.course_id = rcp.course_id
                                       AND bs.student_id = :userId
                                 )
                             )
                       )
                   ELSE FALSE
               END as is_published
        FROM review r
        WHERE r.id IN :reviewIds
    """, nativeQuery = true)
    fun arePublishedForUser(
        @Param("reviewIds") reviewIds: List<UUID>,
        @Param("userId") userId: String,
        @Param("role") role: String
    ): List<Map<String, Any>>

    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM review_course_publication
            WHERE review_id = :reviewId
              AND course_id = :courseId
        )
    """, nativeQuery = true)
    fun isPublishedForCourse(
        @Param("reviewId") reviewId: UUID,
        @Param("courseId") courseId: UUID
    ): Boolean

    @Query(value = """
        SELECT COUNT(DISTINCT course_id)
        FROM review_course_publication
        WHERE review_id = :reviewId
    """, nativeQuery = true)
    fun getPublishedCourseCount(@Param("reviewId") reviewId: UUID): Int
    
    @Query(value = """
        SELECT COUNT(DISTINCT course_id)
        FROM course_review
        WHERE review_id = :reviewId
    """, nativeQuery = true)
    fun getTotalCourseCount(@Param("reviewId") reviewId: UUID): Int
    
    @Query(value = """
        SELECT 
            r.id as review_id,
            r.name as review_name,
            MAX(rcp.published_at) as published_at
        FROM review_course_publication rcp
        JOIN review r ON r.id = rcp.review_id
        GROUP BY r.id, r.name
        ORDER BY MAX(rcp.published_at) DESC
        LIMIT 5
    """, nativeQuery = true)
    fun findRecentPublicationsNative(): List<Map<String, Any>>
    
    @Query(value = """
        SELECT 
            r.id as review_id,
            r.name as review_name,
            MAX(rcp.published_at) as published_at
        FROM review_course_publication rcp
        JOIN review r ON r.id = rcp.review_id
        JOIN course c ON c.id = rcp.course_id
        JOIN semester s ON s.id = c.semester_id
        WHERE s.is_active = true
        GROUP BY r.id, r.name
        ORDER BY MAX(rcp.published_at) DESC
        LIMIT 5
    """, nativeQuery = true)
    fun findRecentPublicationsForActiveSemesters(): List<Map<String, Any>>
    

    @Query(value = """
        SELECT DISTINCT cr.course_id
        FROM course_review cr
        WHERE cr.review_id = :reviewId
          AND (
            :role IN ('ADMIN', 'MANAGER')
            OR EXISTS (
                SELECT 1
                FROM course_instructor ci
                WHERE ci.course_id = cr.course_id
                  AND ci.instructor_id = :userId
            )
          )
    """, nativeQuery = true)
    fun getPublishableCourseIds(
        @Param("reviewId") reviewId: UUID,
        @Param("userId") userId: String,
        @Param("role") role: String
    ): List<UUID>
}
