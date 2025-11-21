package com.devlabs.devlabsbackend.individualscore.repository

import com.devlabs.devlabsbackend.criterion.domain.Criterion
import com.devlabs.devlabsbackend.individualscore.domain.IndividualScore
import com.devlabs.devlabsbackend.project.domain.Project
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.user.domain.User
import com.devlabs.devlabsbackend.course.domain.Course
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@RepositoryRestResource(path = "individualScore")
interface IndividualScoreRepository : JpaRepository<IndividualScore, UUID> {
    
    fun findByParticipantAndCriterionAndReviewAndProject(
        participant: User,
        criterion: Criterion,
        review: Review,
        project: Project
    ): IndividualScore?
    
    @Query("""
        SELECT s FROM IndividualScore s 
        LEFT JOIN FETCH s.criterion
        WHERE s.participant = :participant 
          AND s.review = :review 
          AND s.project = :project
    """)
    fun findByParticipantAndReviewAndProject(
        @Param("participant") participant: User,
        @Param("review") review: Review,
        @Param("project") project: Project
    ): List<IndividualScore>
    
    @Query("""
        SELECT s FROM IndividualScore s 
        LEFT JOIN FETCH s.participant
        LEFT JOIN FETCH s.criterion
        WHERE s.review = :review 
          AND s.project = :project
    """)
    fun findByReviewAndProject(
        @Param("review") review: Review,
        @Param("project") project: Project
    ): List<IndividualScore>
    
    @Query(value = """
        SELECT 
            i_s.id,
            i_s.review_id,
            i_s.project_id,
            i_s.participant_id,
            u.name as participant_name,
            i_s.criterion_id,
            c.name as criterion_name,
            c.max_score as criterion_max_score,
            i_s.score,
            i_s.comment
        FROM individual_score i_s
        JOIN "user" u ON u.id = i_s.participant_id
        JOIN criterion c ON c.id = i_s.criterion_id
        WHERE i_s.review_id = :reviewId
          AND i_s.project_id = :projectId
        ORDER BY i_s.participant_id, i_s.criterion_id
    """, nativeQuery = true)
    fun findScoreDataByReviewAndProject(
        @Param("reviewId") reviewId: UUID,
        @Param("projectId") projectId: UUID
    ): List<Map<String, Any>>
    
    @Query(value = """
        SELECT 
            i_s.id,
            i_s.review_id,
            i_s.project_id,
            i_s.participant_id,
            u.name as participant_name,
            u.email as participant_email,
            i_s.criterion_id,
            c.name as criterion_name,
            c.max_score as criterion_max_score,
            i_s.score,
            i_s.comment
        FROM individual_score i_s
        JOIN "user" u ON u.id = i_s.participant_id
        JOIN criterion c ON c.id = i_s.criterion_id
        WHERE i_s.review_id = :reviewId
          AND i_s.project_id IN (:projectIds)
        ORDER BY i_s.participant_id, i_s.project_id, i_s.criterion_id
    """, nativeQuery = true)
    fun findScoresByReviewAndProjects(
        @Param("reviewId") reviewId: UUID,
        @Param("projectIds") projectIds: List<UUID>
    ): List<Map<String, Any>>
    
    @Query("""
        SELECT s FROM IndividualScore s 
        LEFT JOIN FETCH s.participant
        LEFT JOIN FETCH s.criterion
        WHERE s.review = :review 
          AND s.project = :project 
          AND s.course = :course
    """)
    fun findByReviewAndProjectAndCourse(
        @Param("review") review: Review,
        @Param("project") project: Project,
        @Param("course") course: Course
    ): List<IndividualScore>
    
    @Query("""
        SELECT s FROM IndividualScore s 
        LEFT JOIN FETCH s.criterion
        WHERE s.participant = :participant 
          AND s.review = :review 
          AND s.project = :project 
          AND s.course = :course
    """)
    fun findByParticipantAndReviewAndProjectAndCourse(
        @Param("participant") participant: User,
        @Param("review") review: Review,
        @Param("project") project: Project,
        @Param("course") course: Course
    ): List<IndividualScore>
    
    fun findByParticipantAndCriterionAndReviewAndProjectAndCourse(
        participant: User,
        criterion: Criterion,
        review: Review,
        project: Project,
        course: Course
    ): IndividualScore?
    
    @Query("SELECT s FROM IndividualScore s WHERE s.participant = :student AND :course MEMBER OF s.project.courses")
    fun findByParticipantAndProjectInCourse(
        @Param("student") student: User,
        @Param("course") course: Course
    ): List<IndividualScore>
    
    @Query("SELECT COALESCE(AVG(s.score / s.criterion.maxScore * 100), 0.0) FROM IndividualScore s WHERE s.participant = :student AND :course MEMBER OF s.project.courses")
    fun getAverageScorePercentageForStudentAndCourse(
        @Param("student") student: User,
        @Param("course") course: Course
    ): Double
    
    @Query("SELECT COUNT(DISTINCT s.review) FROM IndividualScore s WHERE s.participant = :student AND :course MEMBER OF s.project.courses")
    fun countDistinctReviewsForStudentAndCourse(
        @Param("student") student: User,
        @Param("course") course: Course
    ): Int
    
    @Query("SELECT DISTINCT is.participant FROM IndividualScore is WHERE is.review = :review AND is.project = :project")
    fun findDistinctParticipantsByReviewAndProject(
        @Param("review") review: Review,
        @Param("project") project: Project
    ): List<User>
    
    @Query("SELECT DISTINCT is.participant FROM IndividualScore is WHERE is.review = :review AND is.project = :project AND is.course = :course")
    fun findDistinctParticipantsByReviewAndProjectAndCourse(
        @Param("review") review: Review,
        @Param("project") project: Project,
        @Param("course") course: Course
    ): List<User>
    
    @Transactional
    fun deleteByParticipantAndReviewAndProject(
        participant: User,
        review: Review,
        project: Project
    )
    
    @Transactional
    fun deleteByReviewAndProject(
        review: Review,
        project: Project
    )
    
    @Transactional
    fun deleteByParticipantAndReviewAndProjectAndCourse(
        participant: User,
        review: Review,
        project: Project,
        course: Course
    )
    
    @Query("SELECT DISTINCT s.review FROM IndividualScore s WHERE s.participant = :participant AND s.course = :course ORDER BY s.review.startDate")
    fun findDistinctReviewsByParticipantAndCourse(
        @Param("participant") participant: User,
        @Param("course") course: Course
    ): List<Review>
    
    @Query("""
        SELECT s FROM IndividualScore s 
        LEFT JOIN FETCH s.criterion
        WHERE s.participant = :participant 
          AND s.review = :review 
          AND s.course = :course
    """)
    fun findByParticipantAndReviewAndCourse(
        @Param("participant") participant: User,
        @Param("review") review: Review,
        @Param("course") course: Course
    ): List<IndividualScore>

    @Query("""
        SELECT s FROM IndividualScore s 
        LEFT JOIN FETCH s.criterion
        LEFT JOIN FETCH s.review
        WHERE s.participant = :participant 
          AND s.review IN :reviews
          AND s.course = :course
    """)
    fun findByParticipantAndReviewsAndCourse(
        @Param("participant") participant: User,
        @Param("reviews") reviews: List<Review>,
        @Param("course") course: Course
    ): List<IndividualScore>
}
