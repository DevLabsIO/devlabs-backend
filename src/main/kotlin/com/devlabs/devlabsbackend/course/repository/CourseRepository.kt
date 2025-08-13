package com.devlabs.devlabsbackend.course.repository

import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.semester.domain.Semester
import com.devlabs.devlabsbackend.user.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import java.util.*

@RepositoryRestResource(path = "courses")
interface CourseRepository : JpaRepository<Course, UUID> {
    
    @Query("SELECT c FROM Course c WHERE c.semester.isActive = true")
    fun findCoursesByActiveSemesters(pageable: Pageable): Page<Course>
    
    @Query("SELECT c FROM Course c WHERE c.semester.isActive = true AND :instructor MEMBER OF c.instructors")
    fun findCoursesByActiveSemestersAndInstructor(@Param("instructor") instructor: User, pageable: Pageable): Page<Course>

    @Query("SELECT c FROM Course c WHERE c.semester.isActive = true AND :instructor MEMBER OF c.instructors")
    @org.springframework.data.rest.core.annotation.RestResource(exported = false)
    fun findCoursesByActiveSemestersAndInstructor(@Param("instructor") instructor: User): List<Course>
      @Query("SELECT c FROM Course c WHERE c.semester.isActive = true AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    fun searchCoursesByActiveSemesters(@Param("query") query: String, pageable: Pageable): Page<Course>
    
    @Query("SELECT c FROM Course c WHERE c.semester.isActive = true AND :instructor MEMBER OF c.instructors AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    fun searchCoursesByActiveSemestersAndInstructor(@Param("instructor") instructor: User, @Param("query") query: String, pageable: Pageable): Page<Course>
    
    @Query("SELECT c FROM Course c WHERE c.semester.isActive = true AND :student MEMBER OF c.students")
    fun findCoursesByActiveSemestersAndStudent(@Param("student") student: User, pageable: Pageable): Page<Course>

    @Query("SELECT c FROM Course c WHERE c.semester.isActive = true AND :student MEMBER OF c.students")
    @org.springframework.data.rest.core.annotation.RestResource(exported = false)
    fun findCoursesByActiveSemestersAndStudent(@Param("student") student: User): List<Course>
        @Query("SELECT DISTINCT c FROM Course c JOIN c.batches b JOIN b.students s WHERE c.semester.isActive = true AND s = :student")
    @org.springframework.data.rest.core.annotation.RestResource(exported = false)
    fun findCoursesByActiveSemestersAndStudentThroughBatch(@Param("student") student: User): List<Course>

    @Query("SELECT DISTINCT c FROM Course c JOIN c.batches b JOIN b.students s WHERE c.semester.isActive = true AND s = :student")
    fun findCoursesByActiveSemestersAndStudentThroughBatch(@Param("student") student: User, pageable: Pageable): Page<Course>

    @Query("SELECT c FROM Course c WHERE c.semester.isActive = true")
    @org.springframework.data.rest.core.annotation.RestResource(exported = false)
    fun findCoursesByActiveSemesters(): List<Course>

    fun findBySemester(semester: com.devlabs.devlabsbackend.semester.domain.Semester): List<Course>
    
    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.semester WHERE c.id IN :ids")
    fun findAllByIdWithSemester(@Param("ids") ids: List<UUID>): List<Course>
    
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Course c WHERE c.semester = :semester")
    fun existsBySemester(@Param("semester") semester: com.devlabs.devlabsbackend.semester.domain.Semester): Boolean

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.batches WHERE c.id = :courseId")
    fun findByIdWithBatches(@Param("courseId") courseId: UUID): Course?
    
    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.students WHERE c.id = :courseId")
    fun findByIdWithStudents(@Param("courseId") courseId: UUID): Course?
    
    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.instructors WHERE c.id = :courseId")
    fun findByIdWithInstructors(@Param("courseId") courseId: UUID): Course?
    
    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.students LEFT JOIN FETCH c.instructors WHERE c.id = :courseId")
    fun findByIdWithStudentsAndInstructors(@Param("courseId") courseId: UUID): Course?
    
    @Query("SELECT COUNT(c) FROM Course c WHERE c.semester.isActive = true")
    fun countByActiveSemesters(): Long
    
    fun findBySemesterIn(semesters: List<Semester>): List<Course>
    
    fun findByInstructorsContaining(instructor: User): List<Course>
}