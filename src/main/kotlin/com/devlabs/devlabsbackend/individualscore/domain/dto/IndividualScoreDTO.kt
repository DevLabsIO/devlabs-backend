package com.devlabs.devlabsbackend.individualscore.domain.dto

import java.time.LocalDate
import java.util.*

data class SubmitScoreRequest(
    val userId: String,
    val reviewId: UUID,
    val projectId: UUID,
    val scores: List<ParticipantScore>
)

data class ParticipantScore(
    val participantId: String,
    val criterionScores: List<CriterionScore>
)

data class CriterionScore(
    val criterionId: UUID,
    val score: Double,
    val comment: String? = null
)

data class SubmitCourseScoreRequest(
    val reviewId: UUID,
    val projectId: UUID,
    val courseId: UUID,
    val scores: List<ParticipantScore>
)

data class IndividualScoreResponse(
    val id: UUID,
    val participantId: String,
    val participantName: String,
    val criterionId: UUID,
    val criterionName: String,
    val score: Double,
    val comment: String?,
    val reviewId: UUID,
    val projectId: UUID
)

data class ParticipantScoresSummary(
    val participantId: String,
    val participantName: String,
    val criterionScores: List<CriterionScoreDetail>,
    val totalScore: Double,
    val maxPossibleScore: Double,
    val percentage: Double
)

data class CriterionScoreDetail(
    val criterionId: UUID,
    val criterionName: String,
    val maxScore: Double,
    val score: Double,
    val comment: String?
)

data class CourseEvaluationInfo(
    val reviewId: UUID,
    val reviewName: String,
    val projectId: UUID,
    val projectTitle: String,
    val courseId: UUID,
    val courseName: String,
    val teamName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val hasExistingEvaluation: Boolean
)

data class CourseEvaluationData(
    val courseId: UUID,
    val courseName: String,
    val projectId: UUID,
    val reviewId: UUID,
    val teamMembers: List<TeamMemberInfo>,
    val criteria: List<CriterionInfo>,
    val existingScores: List<ParticipantScoreData>,
    val isPublished: Boolean
)

data class TeamMemberInfo(
    val id: String,
    val name: String,
    val email: String,
    val role: String
)

data class CriterionInfo(
    val id: UUID,
    val name: String,
    val description: String,
    val maxScore: Double,
    val courseSpecific: Boolean
)

data class ParticipantScoreData(
    val participantId: String,
    val criterionScores: List<CriterionScoreData>
)

data class CriterionScoreData(
    val criterionId: UUID,
    val score: Double,
    val comment: String?
)

data class InstructorInfo(
    val id: String,
    val name: String
)

data class CourseEvaluationSummary(
    val courseId: UUID,
    val courseName: String,
    val instructors: List<InstructorInfo>,
    val hasEvaluation: Boolean,
    val evaluationCount: Int
)

data class ProjectEvaluationSummary(
    val reviewId: UUID,
    val reviewName: String,
    val projectId: UUID,
    val projectTitle: String,
    val teamName: String,
    val courseEvaluations: List<CourseEvaluationSummary>
)

data class UserIdRequest(
    val userId: String
)

data class AvailableEvaluationRequest(
    val userId: String,
    val semester: UUID? = null
)

data class AvailableEvaluationResponse(
    val evaluations: List<CourseEvaluationInfo>,
    val totalCount: Int
)

data class ReviewEvaluationInfo(
    val reviewId: UUID,
    val reviewName: String,
    val dueDate: Date?,
    val projects: List<ProjectEvaluationInfo>
)

data class ProjectEvaluationInfo(
    val projectId: UUID,
    val projectTitle: String,
    val teamName: String,
    val courses: List<CourseEvaluationInfo>
)
