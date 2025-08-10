package com.devlabs.devlabsbackend.individualscore.service

import com.devlabs.devlabsbackend.batch.repository.BatchRepository
import com.devlabs.devlabsbackend.core.exception.ForbiddenException
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.course.repository.CourseRepository
import com.devlabs.devlabsbackend.criterion.repository.CriterionRepository
import com.devlabs.devlabsbackend.individualscore.domain.DTO.*
import com.devlabs.devlabsbackend.individualscore.domain.IndividualScore
import com.devlabs.devlabsbackend.individualscore.repository.IndividualScoreRepository
import com.devlabs.devlabsbackend.project.domain.Project
import com.devlabs.devlabsbackend.project.repository.ProjectRepository
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.review.repository.ReviewRepository
import com.devlabs.devlabsbackend.review.service.ReviewPublicationHelper
import com.devlabs.devlabsbackend.rubrics.domain.Rubrics
import com.devlabs.devlabsbackend.semester.repository.SemesterRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.User
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class IndividualScoreService(
    private val individualScoreRepository: IndividualScoreRepository,
    private val reviewRepository: ReviewRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
    private val criterionRepository: CriterionRepository,
    private val courseRepository: CourseRepository,
    private val batchRepository: BatchRepository,
    private val semesterRepository: SemesterRepository,
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

            if (participantId != null && participantId !=  user.id) {
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
    fun getCourseEvaluationData(reviewId: UUID, projectId: UUID, courseId: UUID, userId: String): CourseEvaluationData {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val review = reviewRepository.findById(reviewId).orElseThrow {
            NotFoundException("Review with id $reviewId not found")
        }

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

        val existingScores = project.team.members.mapNotNull { member ->
            val scores = individualScoreRepository.findByParticipantAndReviewAndProjectAndCourse(
                member, review, project, course
            )

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
    fun submitCourseScores(request: SubmitCourseScoreRequest, submitterId: String): List<IndividualScore> {
        val submitter = userRepository.findById(submitterId).orElseThrow {
            NotFoundException("User with id $submitterId not found")
        }

        val review = reviewRepository.findById(request.reviewId).orElseThrow {
            NotFoundException("Review with id ${request.reviewId} not found")
        }

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
        if (review.projects.any { it.id == project.id }) {
            return true
        }

        val projectCourses = project.courses
        if (projectCourses.isNotEmpty()) {
            if (review.courses.any { reviewCourse ->
                    projectCourses.any { projectCourse -> projectCourse.id == reviewCourse.id }
                }) {
                return true
            }
        }

        val teamMembers = project.team.members
        if (teamMembers.isNotEmpty()) {

            val memberBatches = mutableSetOf<com.devlabs.devlabsbackend.batch.domain.Batch>()
            teamMembers.forEach { member ->
                val batches = batchRepository.findByStudentsContaining(member)
                memberBatches.addAll(batches)
            }

            if (memberBatches.isNotEmpty()) {

                if (review.batches.any { batch -> memberBatches.any { it.id == batch.id } }) {
                    return true
                }

                val allCourses = courseRepository.findAll()
                val batchCourses = allCourses.filter { course ->
                    course.batches.any { batch -> memberBatches.any { it.id == batch.id } }
                }

                if (batchCourses.isNotEmpty()) {
                    if (review.courses.any { course -> batchCourses.any { it.id == course.id } }) {
                        return true
                    }
                }
            }
        }

        val semesters = mutableSetOf<UUID>()

        projectCourses.forEach { course ->
            course.semester.id?.let { semesters.add(it) }
        }

        teamMembers.forEach { member ->
            val batches = batchRepository.findByStudentsContaining(member)
            batches.forEach { batch ->
                batch.semester.forEach { semester ->
                    semester.id?.let { semesters.add(it) }
                }
            }
        }

        if (semesters.isNotEmpty()) {
            val semesterEntities = semesterRepository.findAllByIdWithCourses(semesters.toList())
            semesterEntities.forEach { semester ->
                val semesterCourses = semester.courses

                if (review.courses.any { course -> semesterCourses.any { it.id == course.id } }) {
                    return true
                }

                val semesterBatches = semester.batches
                if (review.batches.any { batch -> semesterBatches.any { it.id == batch.id } }) {
                    return true
                }
            }
        }
        return false
    }


    private fun checkCourseEvaluationAccess(user: User, review: Review, project: Project, course: com.devlabs.devlabsbackend.course.domain.Course) {
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

