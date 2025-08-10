package com.devlabs.devlabsbackend.review.repository

import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.review.domain.ReviewCoursePublication
import com.devlabs.devlabsbackend.user.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import java.util.*

@RepositoryRestResource(path = "review-course-publications")
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
}
