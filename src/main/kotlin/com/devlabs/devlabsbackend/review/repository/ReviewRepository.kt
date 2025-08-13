package com.devlabs.devlabsbackend.review.repository

import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.project.domain.Project
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.user.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import java.time.LocalDate
import java.util.*

@RepositoryRestResource(path = "reviews")
interface ReviewRepository: JpaRepository<Review, UUID> {
    @Query("SELECT DISTINCT r FROM Review r JOIN r.projects p JOIN p.team t JOIN t.members m WHERE m = :student ORDER BY r.startDate DESC")
    fun findAllByProjectTeamMembersContaining(@Param("student") student: User): List<Review>

    @Query("SELECT DISTINCT r FROM Review r JOIN r.courses c JOIN c.instructors i WHERE i = :instructor ORDER BY r.startDate DESC")
    fun findAllByCourseInstructorsContaining(@Param("instructor") instructor: User): List<Review>
    
    @Query("SELECT DISTINCT r FROM Review r JOIN r.projects p JOIN p.team t JOIN t.members m WHERE m = :student")
    fun findByProjectsTeamMembersContaining(@Param("student") student: User, pageable: Pageable): Page<Review>
    
    @Query("SELECT DISTINCT r FROM Review r JOIN r.courses c JOIN c.instructors i WHERE i = :instructor")
    fun findByCoursesInstructorsContaining(@Param("instructor") instructor: User, pageable: Pageable): Page<Review>
    
    fun findByCoursesContaining(course: Course): List<Review>
    
    fun findByCourses(course: Course): List<Review> = findByCoursesContaining(course)
    
    fun findByCoursesContainingAndEndDateGreaterThanEqual(course: Course, date: LocalDate): List<Review>
    
    fun findByProjectsContaining(project: Project): List<Review>
    
    fun findByEndDateBefore(date: LocalDate, pageable: Pageable): Page<Review>
    
    fun findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
        currentDate: LocalDate, 
        currentDate2: LocalDate,
        pageable: Pageable
    ): Page<Review>
    fun findByStartDateAfter(date: LocalDate, pageable: Pageable): Page<Review>
    @Query("SELECT DISTINCT r FROM Review r " +
           "LEFT JOIN FETCH r.courses c " +
           "LEFT JOIN FETCH r.projects p " +
           "LEFT JOIN FETCH r.batches b " +
           "LEFT JOIN FETCH p.team t " +
           "LEFT JOIN FETCH t.members " +
           "LEFT JOIN FETCH c.instructors")
    fun findAllWithAssociations(): List<Review>
    
    @Query("""
        SELECT DISTINCT r FROM Review r 
        LEFT JOIN FETCH r.rubrics rb
        LEFT JOIN FETCH rb.criteria
        LEFT JOIN FETCH r.courses c
        LEFT JOIN FETCH r.projects p
        LEFT JOIN FETCH r.batches b
        WHERE r.id = :reviewId
    """)
    fun findByIdWithRelations(@Param("reviewId") reviewId: UUID): Review?
    
    fun findByCoursesInAndStartDateAfter(courses: List<Course>, date: LocalDate): List<Review>
    
    fun findByCoursesInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        courses: List<Course>, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): List<Review>
    
    fun findByCoursesInAndEndDateBefore(courses: List<Course>, date: LocalDate): List<Review>
    
    fun findByCoursesIn(courses: List<Course>): List<Review>
}
