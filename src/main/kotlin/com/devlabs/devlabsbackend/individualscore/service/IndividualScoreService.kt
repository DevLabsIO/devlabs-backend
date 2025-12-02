package com.devlabs.devlabsbackend.individualscore.service

import com.devlabs.devlabsbackend.batch.domain.Batch
import com.devlabs.devlabsbackend.batch.repository.BatchRepository
import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.ForbiddenException
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.course.repository.CourseRepository
import com.devlabs.devlabsbackend.criterion.repository.CriterionRepository
import com.devlabs.devlabsbackend.individualscore.domain.dto.*
import com.devlabs.devlabsbackend.individualscore.domain.IndividualScore
import com.devlabs.devlabsbackend.individualscore.repository.IndividualScoreRepository
import com.devlabs.devlabsbackend.project.domain.Project
import com.devlabs.devlabsbackend.project.repository.ProjectRepository
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.review.repository.ReviewRepository
import com.devlabs.devlabsbackend.review.service.ReviewPublicationHelper
import com.devlabs.devlabsbackend.semester.repository.SemesterRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.User
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class IndividualScoreService(
    private val individualScoreRepository: IndividualScoreRepository,
    private val reviewRepository: ReviewRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
    private val reviewPublicationHelper: ReviewPublicationHelper
) {

    fun checkScoreAccessRights(userId: String, review: Review, project: Project, participantId: String? = null) {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        if (user.role == Role.ADMIN || user.role == Role.MANAGER) {
            return
        }

        if (user.role == Role.FACULTY) {
            val isInstructorOfProjectCourse = project.courses.any { course ->
                course.instructors.contains(user)
            }

            if (!isInstructorOfProjectCourse) {
                throw ForbiddenException("Faculty can only view scores for projects in their courses")
            }
            return
        }

        if (user.role == Role.STUDENT) {
            if (!reviewPublicationHelper.isReviewPublishedForUser(review, user)) {
                throw ForbiddenException("Students can only view scores for published reviews")
            }

            if (participantId != null && participantId != user.id) {
                throw ForbiddenException("Students can only view their own scores")
            }

            if (!project.team.members.contains(user)) {
                throw ForbiddenException("Students can only view scores for projects they are part of")
            }

            return
        }
        throw ForbiddenException("Unauthorized access to scores")
    }

    @Transactional(readOnly = true)
    @Cacheable(
        value = ["individual-scores"],
        key = "'course-eval-' + #reviewId + '-' + #projectId + '-' + #courseId + '-' + #userId"
    )
    fun getCourseEvaluationData(reviewId: UUID, projectId: UUID, courseId: UUID, userId: String): CourseEvaluationData {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val review = reviewRepository.findByIdWithRubrics(reviewId)
            ?: throw NotFoundException("Review with id $reviewId not found")

        val project = projectRepository.findByIdWithRelations(projectId) ?: throw NotFoundException("Project with id $projectId not found")

        val course = courseRepository.findById(courseId).orElseThrow {
            NotFoundException("Course with id $courseId not found")
        }

        if (!isProjectAssociatedWithReview(review, project)) {
            throw IllegalArgumentException("Project is not part of this review")
        }

        if (!project.courses.contains(course)) {
            throw IllegalArgumentException("Course is not associated with this project")
        }

        checkCourseEvaluationAccess(user, review, project, course)

        val teamMembers = project.team.members.map { member ->
            TeamMemberInfo(
                id = member.id!!,
                name = member.name,
                email = member.email,
                role = member.role.name
            )
        }

        val criteria = review.rubrics?.criteria?.map { criterion ->
            CriterionInfo(
                id = criterion.id!!,
                name = criterion.name,
                description = criterion.description,
                maxScore = criterion.maxScore.toDouble(),
                courseSpecific = !criterion.isCommon
            )
        } ?: emptyList()

        val allScores = individualScoreRepository.findByReviewAndProjectAndCourse(review, project, course)
        val scoresByParticipant = allScores.groupBy { it.participant.id }

        val existingScores = project.team.members.mapNotNull { member ->
            val scores = scoresByParticipant[member.id] ?: emptyList()

            if (scores.isNotEmpty()) {
                ParticipantScoreData(
                    participantId = member.id!!,
                    criterionScores = scores.map { score ->
                        CriterionScoreData(
                            criterionId = score.criterion.id!!,
                            score = score.score,
                            comment = score.comment
                        )
                    }
                )
            } else {
                null
            }
        }

        return CourseEvaluationData(
            courseId = course.id!!,
            courseName = course.name,
            projectId = project.id!!,
            reviewId = review.id!!,
            teamMembers = teamMembers,
            criteria = criteria,
            existingScores = existingScores,
            isPublished = reviewPublicationHelper.isReviewPublishedForCourse(review, course)
        )
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(value = ["individual-scores"], allEntries = true),
            CacheEvict(value = [CacheConfig.INDIVIDUAL_SCORES_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.COURSE_DETAIL_CACHE, CacheConfig.COURSE_PERFORMANCE_CACHE], allEntries = true),
            CacheEvict(value = ["review-detail"], allEntries = true),
            CacheEvict(value = [CacheConfig.REVIEW_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_STUDENT], allEntries = true),
            CacheEvict(value = [CacheConfig.COURSES_USER_CACHE], allEntries = true)
        ]
    )
    fun submitCourseScores(request: SubmitCourseScoreRequest, submitterId: String): List<IndividualScore> {
        val submitter = userRepository.findById(submitterId).orElseThrow {
            NotFoundException("User with id $submitterId not found")
        }

        val review = reviewRepository.findByIdWithRubrics(request.reviewId)
            ?: throw NotFoundException("Review with id ${request.reviewId} not found")

        val project = projectRepository.findByIdWithRelations(request.projectId) ?: throw NotFoundException("Project with id ${request.projectId} not found")
        val course = courseRepository.findById(request.courseId).orElseThrow {
            NotFoundException("Course with id ${request.courseId} not found")
        }

        if (!isProjectAssociatedWithReview(review, project)) {
            throw IllegalArgumentException("Project is not part of this review")
        }

        if (!project.courses.contains(course)) {
            throw IllegalArgumentException("Course is not associated with this project")
        }

        if (submitter.role == Role.FACULTY) {
            if (!course.instructors.contains(submitter)) {
                throw ForbiddenException("Faculty can only submit scores for courses they teach")
            }
        } else if (submitter.role != Role.ADMIN && submitter.role != Role.MANAGER) {
            throw ForbiddenException("Only faculty, admin, or manager can submit scores")
        }

        val criteria = review.rubrics?.criteria ?: throw IllegalArgumentException("Review has no rubrics with criteria")

        val criteriaMap = criteria.associateBy { it.id }

        val savedScores = mutableListOf<IndividualScore>()

        request.scores.forEach { participantScores ->
            val participant = userRepository.findById(participantScores.participantId).orElseThrow {
                NotFoundException("Participant with id ${participantScores.participantId} not found")
            }

            if (!project.team.members.contains(participant)) {
                throw IllegalArgumentException("Participant ${participant.name} is not a member of this project team")
            }

            participantScores.criterionScores.forEach { criterionScore ->
                val criterion = criteriaMap[criterionScore.criterionId] ?: throw NotFoundException(
                    "Criterion with id ${criterionScore.criterionId} not found in review's rubrics"
                )

                if (criterionScore.score < 0 || criterionScore.score > criterion.maxScore) {
                    throw IllegalArgumentException(
                        "Score for criterion ${criterion.name} must be between 0 and ${criterion.maxScore}"
                    )
                }

                val existingScore = individualScoreRepository.findByParticipantAndCriterionAndReviewAndProjectAndCourse(
                    participant,
                    criterion,
                    review,
                    project,
                    course
                )

                val score = if (existingScore != null) {
                    existingScore.score = criterionScore.score
                    existingScore.comment = criterionScore.comment
                    existingScore
                } else {
                    IndividualScore(
                        participant = participant,
                        criterion = criterion,
                        score = criterionScore.score,
                        comment = criterionScore.comment,
                        review = review,
                        project = project,
                        course = course
                    )
                }

                savedScores.add(individualScoreRepository.save(score))
            }
        }

        return savedScores
    }

    @Transactional(readOnly = true)
    @Cacheable(
        value = ["individual-scores"],
        key = "'project-eval-summary-' + #reviewId + '-' + #projectId"
    )
    fun getProjectEvaluationSummary(reviewId: UUID, projectId: UUID): ProjectEvaluationSummary {
        val review = reviewRepository.findById(reviewId).orElseThrow {
            NotFoundException("Review with id $reviewId not found")
        }
        val project = projectRepository.findByIdWithRelations(projectId) ?: throw NotFoundException("Project with id $projectId not found")

        if (!isProjectAssociatedWithReview(review, project)) {
            throw IllegalArgumentException("Project is not part of this review")
        }

        val courses = project.courses.toList()
        val courseEvaluations = courses.map { course ->
            val hasScores = individualScoreRepository.findByReviewAndProjectAndCourse(
                review, project, course
            ).isNotEmpty()

            CourseEvaluationSummary(
                courseId = course.id!!,
                courseName = course.name,
                instructors = course.instructors.map { instructor ->
                    InstructorInfo(
                        id = instructor.id!!,
                        name = instructor.name
                    )
                },
                hasEvaluation = hasScores,
                evaluationCount = if (hasScores) {
                    individualScoreRepository.findDistinctParticipantsByReviewAndProjectAndCourse(
                        review, project, course
                    ).size
                } else 0
            )
        }

        return ProjectEvaluationSummary(
            reviewId = review.id!!,
            reviewName = review.name,
            projectId = project.id!!,
            projectTitle = project.title,
            teamName = project.team.name,
            courseEvaluations = courseEvaluations
        )
    }

    private fun isProjectAssociatedWithReview(review: Review, project: Project): Boolean {
        return reviewRepository.isProjectAssociatedWithReview(
            review.id ?: throw IllegalStateException("Review must have ID"),
            project.id ?: throw IllegalStateException("Project must have ID")
        )
    }

    private fun checkCourseEvaluationAccess(user: User, review: Review, project: Project, course: Course) {
        when (user.role) {
            Role.ADMIN, Role.MANAGER -> {
                return
            }
            Role.FACULTY -> {
                if (!course.instructors.contains(user)) {
                    throw ForbiddenException("Faculty can only access evaluations for courses they teach")
                }
            }
            Role.STUDENT -> {
                if (!reviewPublicationHelper.isReviewPublishedForUser(review, user)) {
                    throw ForbiddenException("Students can only access published reviews")
                }

                if (!project.team.members.contains(user)) {
                    throw ForbiddenException("Students can only access their own project evaluations")
                }
            }
        }
    }
}
