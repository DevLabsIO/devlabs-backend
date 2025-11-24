package com.devlabs.devlabsbackend.review.service

import com.devlabs.devlabsbackend.batch.repository.BatchRepository
import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.ForbiddenException
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.core.pagination.PaginationInfo
import com.devlabs.devlabsbackend.course.repository.CourseRepository
import com.devlabs.devlabsbackend.individualscore.repository.IndividualScoreRepository
import com.devlabs.devlabsbackend.project.domain.Project
import com.devlabs.devlabsbackend.project.repository.ProjectRepository
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.review.domain.dto.*
import com.devlabs.devlabsbackend.review.repository.ReviewRepository
import com.devlabs.devlabsbackend.semester.domain.Semester
import com.devlabs.devlabsbackend.security.utils.SecurityUtils
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class ReviewQueryService(
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
    private val projectRepository: ProjectRepository,
    private val batchRepository: BatchRepository,
    private val reviewPublicationHelper: ReviewPublicationHelper,
    private val individualScoreRepository: IndividualScoreRepository,
) {
    
    private val logger = LoggerFactory.getLogger(ReviewQueryService::class.java)
    
    @Transactional(readOnly = true)
    @Cacheable(
        value = ["reviews"],
        key = "'user-' + #userId + '-' + #page + '-' + #size + '-' + #sortBy + '-' + #sortOrder",
        condition = "#page == 0 && #size == 10"
    )
    fun getReviewsForUser(
        userId: String,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "createdAt",
        sortOrder: String = "desc"
    ): PaginatedResponse<ReviewResponse> {
        val userRole = SecurityUtils.getCurrentUserRoleFromJwt() 
            ?: throw NotFoundException("User role not found in JWT")

        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val offset = page * size
        
        val reviewIds: List<UUID>
        val totalCount: Int

        when (user.role) {
            Role.ADMIN, Role.MANAGER -> {
                reviewIds = reviewRepository.findAllReviewIds(sortBy, sortOrder, offset, size)
                totalCount = reviewRepository.countAllReviews()
            }
            Role.FACULTY -> {
                reviewIds = reviewRepository.findReviewIdsForFaculty(userId, sortBy, sortOrder, offset, size)
                totalCount = reviewRepository.countReviewsForFaculty(userId)
            }
            Role.STUDENT -> {
                reviewIds = reviewRepository.findReviewIdsForStudent(userId, sortBy, sortOrder, offset, size)
                totalCount = reviewRepository.countReviewsForStudent(userId)
            }
        }

        if (reviewIds.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page + 1,
                    per_page = size,
                    total_pages = 0,
                    total_count = 0
                )
            )
        }
        
        val reviewData = reviewRepository.findReviewListData(reviewIds)
    
        val coursesData = reviewRepository.findCoursesByReviewIds(reviewIds)
        val coursesByReviewId = coursesData.groupBy { it["review_id"].toString() }
        
        val projectsData = reviewRepository.findProjectsByReviewIds(reviewIds)
        val projectsByReviewId = projectsData.groupBy { it["review_id"].toString() }
        
        val rubricsIds = reviewData.mapNotNull { it["rubrics_id"]?.toString() }.distinct()
            .map { UUID.fromString(it) }
        val criteriaData = if (rubricsIds.isNotEmpty()) {
            reviewRepository.findCriteriaByRubricsIds(rubricsIds)
        } else {
            emptyList()
        }
        val criteriaByRubricsId = criteriaData.groupBy { it["rubrics_id"].toString() }
        
        val publishData = reviewRepository.findPublishedCountsByReviewIds(reviewIds)
        val publishMap = publishData.associate { 
            it["review_id"].toString() to (it["count"] as Number).toInt()
        }
        
        val reviewResponses = reviewData.map { data ->
            mapBatchedDataToResponse(
                data, 
                coursesByReviewId, 
                projectsByReviewId,
                criteriaByRubricsId, 
                publishMap, 
                user.role
            )
        }

        val totalPages = (totalCount + size - 1) / size

        return PaginatedResponse(
            data = reviewResponses,
            pagination = PaginationInfo(
                current_page = page + 1,
                per_page = size,
                total_pages = totalPages,
                total_count = totalCount
            )
        )
    }

    
    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.REVIEW_DETAIL_CACHE], key = "'review_detail_' + #reviewId + '_' + (#userId ?: 'anonymous')")
    fun getReviewById(reviewId: UUID, userId: String? = null): ReviewResponse {
        
        val reviewData = reviewRepository.findReviewDataById(reviewId)
            ?: throw NotFoundException("Review with id $reviewId not found")
        
        
        val coursesData = reviewRepository.findCoursesByReviewId(reviewId)
        
        
        val projectsData = reviewRepository.findProjectsByReviewId(reviewId)
        
        val rubricsIdStr = reviewData["rubrics_id"]?.toString()
            ?: throw NotFoundException("Review does not have rubrics")
        val rubricsId = UUID.fromString(rubricsIdStr)
        val criteriaData = reviewRepository.findCriteriaByRubricsIds(listOf(rubricsId))

        val user = userId?.let { userRepository.findById(it).orElse(null) }
        val userRole = user?.role ?: Role.ADMIN
        
        val isPublished = if (userId != null && user != null) {
            reviewPublicationHelper.isReviewPublishedForUser(reviewId, userId, userRole.name)
        } else {
            reviewPublicationHelper.isReviewFullyPublished(reviewId)
        }
 
        return mapSingleReviewDataToResponse(
            reviewData,
            coursesData,
            projectsData,
            criteriaData,
            userRole,
            isPublished
        )
    }

    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.REVIEW_DETAIL_CACHE], key = "'review_user_' + #reviewId + '_' + #userId")
    fun getUserBasedReview(reviewId: UUID, userId: String): ReviewResponse {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val hasAccess = reviewRepository.hasUserAccessToReview(reviewId, userId, user.role.name)

        if (!hasAccess) {
            throw ForbiddenException("You don't have access to this review")
        }

        val reviewData = reviewRepository.findReviewDataById(reviewId)
            ?: throw NotFoundException("Review with id $reviewId not found")
        
        val coursesData = reviewRepository.findCoursesByReviewId(reviewId)
        val projectsData = reviewRepository.findProjectsByReviewId(reviewId)
        
        val rubricsIdStr = reviewData["rubrics_id"]?.toString()
            ?: throw NotFoundException("Review does not have rubrics")
        val rubricsId = UUID.fromString(rubricsIdStr)
        val criteriaData = reviewRepository.findCriteriaByRubricsIds(listOf(rubricsId))
        
        val isPublishedForUser = reviewPublicationHelper.isReviewPublishedForUser(reviewId, userId, user.role.name)
        
        return mapSingleReviewDataToResponse(
            reviewData,
            coursesData,
            projectsData,
            criteriaData,
            user.role,
            isPublishedForUser
        )
    }

    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.REVIEW_DETAIL_CACHE], key = "'review_criteria_' + #reviewId")
    fun getReviewCriteria(reviewId: UUID): ReviewCriteriaResponse {
        val criteriaData = reviewRepository.findReviewCriteriaData(reviewId)
        
        if (criteriaData.isEmpty()) {
            throw NotFoundException("Review with id $reviewId not found or has no rubrics")
        }
        
        val firstRow = criteriaData.first()
        val reviewName = firstRow["review_name"]?.toString()
            ?: throw NotFoundException("Review with id $reviewId not found")
        
        val criteria = criteriaData.mapNotNull { row ->
            val criterionId = row["criterion_id"]?.toString() ?: return@mapNotNull null
            ReviewCriterionDetail(
                id = UUID.fromString(criterionId),
                name = row["criterion_name"].toString(),
                description = row["criterion_description"].toString(),
                maxScore = (row["max_score"] as Number).toFloat(),
                isCommon = row["is_common"] as Boolean
            )
        }

        return ReviewCriteriaResponse(
            reviewId = reviewId,
            reviewName = reviewName,
            criteria = criteria
        )
    }

    @Transactional(readOnly = true)
    fun searchReviews(
        userId: String,
        name: String?,
        courseId: UUID?,
        status: String?,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "createdAt",
        sortOrder: String = "desc"
    ): PaginatedResponse<ReviewResponse> {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val currentDate = LocalDate.now().toString()
        val offset = page * size
        
        val reviewIds: List<UUID>
        val totalCount: Int
        
        when (user.role) {
            Role.ADMIN, Role.MANAGER -> {
                reviewIds = reviewRepository.searchReviewIdsForAdmin(
                    name, courseId, status, currentDate, sortBy, sortOrder, offset, size
                )
                totalCount = reviewRepository.countSearchReviewsForAdmin(
                    name, courseId, status, currentDate
                )
            }
            Role.FACULTY -> {
                reviewIds = reviewRepository.searchReviewIdsForFaculty(
                    userId, name, courseId, status, currentDate, sortBy, sortOrder, offset, size
                )
                totalCount = reviewRepository.countSearchReviewsForFaculty(
                    userId, name, courseId, status, currentDate
                )
            }
            Role.STUDENT -> {
                reviewIds = reviewRepository.searchReviewIdsForStudent(
                    userId, name, courseId, status, currentDate, sortBy, sortOrder, offset, size
                )
                totalCount = reviewRepository.countSearchReviewsForStudent(
                    userId, name, courseId, status, currentDate
                )
            }
        }
        
        if (reviewIds.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page + 1,
                    per_page = size,
                    total_pages = 0,
                    total_count = 0
                )
            )
        }
        
        val reviewData = reviewRepository.findReviewListData(reviewIds)
        val coursesData = reviewRepository.findCoursesByReviewIds(reviewIds)
        val coursesByReviewId = coursesData.groupBy { it["review_id"].toString() }
        
        val rubricsIds = reviewData.mapNotNull { it["rubrics_id"]?.toString() }.distinct()
            .map { UUID.fromString(it) }
        val criteriaData = if (rubricsIds.isNotEmpty()) {
            reviewRepository.findCriteriaByRubricsIds(rubricsIds)
        } else {
            emptyList()
        }
        val criteriaByRubricsId = criteriaData.groupBy { it["rubrics_id"].toString() }
        
        val publishData = reviewRepository.findPublishedCountsByReviewIds(reviewIds)
        val publishMap = publishData.associate { 
            it["review_id"].toString() to (it["count"] as Number).toInt()
        }

        val publicationStatusMap = reviewPublicationHelper.areReviewsPublishedForUser(reviewIds, userId, user.role.name)
        
        val reviewResponses = reviewData.map { data ->
            val reviewId = UUID.fromString(data["id"].toString())
            val isPublished = publicationStatusMap[reviewId] ?: false
            
            mapListDataToResponse(
                data, 
                coursesByReviewId, 
                criteriaByRubricsId, 
                publishMap, 
                user.role
            ).copy(isPublished = isPublished)
        }

        val totalPages = (totalCount + size - 1) / size

        return PaginatedResponse(
            data = reviewResponses,
            pagination = PaginationInfo(
                current_page = page + 1,
                per_page = size,
                total_pages = totalPages,
                total_count = totalCount
            )
        )
    }

    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.PROJECT_REVIEWS_CACHE], key = "'project_reviews_' + #projectId + '_' + (#userId ?: 'anonymous')")
    fun checkProjectReviewAssignment(projectId: UUID, userId: String? = null): ReviewAssignmentResponse {
        val today = LocalDate.now()
        
        val assignments = reviewRepository.findReviewAssignmentsForProject(projectId)
        
        if (assignments.isEmpty()) {
            return ReviewAssignmentResponse(
                hasReview = false,
                assignmentType = "NONE",
                liveReviews = emptyList(),
                upcomingReviews = emptyList(),
                completedReviews = emptyList()
            )
        }
        
        val reviewIdToType = mutableMapOf<UUID, String>()
        assignments.forEach { row ->
            val reviewId = UUID.fromString(row["id"].toString())
            val assignmentType = row["assignment_type"].toString()
            
            if (!reviewIdToType.containsKey(reviewId)) {
                reviewIdToType[reviewId] = assignmentType
            } else if (reviewIdToType[reviewId] == "SEMESTER" && assignmentType != "SEMESTER") {
                reviewIdToType[reviewId] = assignmentType
            } else if (reviewIdToType[reviewId] in listOf("BATCH", "COURSE") && assignmentType == "DIRECT") {
                reviewIdToType[reviewId] = assignmentType
            }
        }
        
        val reviewIds = reviewIdToType.keys.toList()
        
        val reviewData = reviewRepository.findReviewListData(reviewIds)
        val coursesData = reviewRepository.findCoursesByReviewIds(reviewIds)
        val coursesByReviewId = coursesData.groupBy { it["review_id"].toString() }
        
        val rubricsIds = reviewData.mapNotNull { it["rubrics_id"]?.toString() }.distinct()
            .map { UUID.fromString(it) }
        val criteriaData = if (rubricsIds.isNotEmpty()) {
            reviewRepository.findCriteriaByRubricsIds(rubricsIds)
        } else {
            emptyList()
        }
        val criteriaByRubricsId = criteriaData.groupBy { it["rubrics_id"].toString() }
        
        val publishData = reviewRepository.findPublishedCountsByReviewIds(reviewIds)
        val publishMap = publishData.associate { 
            it["review_id"].toString() to (it["count"] as Number).toInt()
        }
        
        val user = userId?.let { userRepository.findById(it).orElse(null) }
        
        val publishedStatusMap = if (userId != null && user != null) {
            reviewPublicationHelper.areReviewsPublishedForUser(reviewIds, userId, user.role.name)
        } else {
            emptyMap()
        }
        
        val allReviews = reviewData.map { data ->
            val review = mapListDataToResponse(
                data, 
                coursesByReviewId, 
                criteriaByRubricsId, 
                publishMap, 
                user?.role ?: Role.ADMIN
            )
            
            if (userId != null && user != null) {
                val reviewId = UUID.fromString(data["id"].toString())
                val isPublishedForUser = publishedStatusMap[reviewId] ?: false
                review.copy(isPublished = isPublishedForUser)
            } else {
                review
            }
        }
        
        val liveReviews = allReviews.filter {
            it.startDate <= today && it.endDate >= today
        }.sortedBy { it.name }

        val completedReviews = allReviews.filter {
            it.endDate < today
        }.sortedBy { it.name }

        val upcomingReviews = allReviews.filter {
            it.startDate > today
        }.sortedBy { it.name }
        
        val primaryAssignmentType = reviewIdToType.values.firstOrNull() ?: "NONE"

        return ReviewAssignmentResponse(
            hasReview = allReviews.isNotEmpty(),
            assignmentType = primaryAssignmentType,
            liveReviews = liveReviews,
            upcomingReviews = upcomingReviews,
            completedReviews = completedReviews
        )
    }

    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.INDIVIDUAL_SCORES_CACHE], key = "'review_results_' + #reviewId + '_' + #projectId + '_' + #userId")
    fun getReviewResults(reviewId: UUID, projectId: UUID, userId: String): ReviewResultsResponse {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        if (!isProjectAssociatedWithReview(reviewId, projectId)) {
            throw IllegalArgumentException("Project is not part of this review")
        }

        val project = projectRepository.findById(projectId).orElseThrow {
            NotFoundException("Project with id $projectId not found")
        }

        val canViewAllResults = when (user.role) {
            Role.ADMIN, Role.MANAGER -> true
            Role.FACULTY -> {
                projectRepository.isUserInstructorForProject(projectId, userId)
            }
            Role.STUDENT -> {
                if (!reviewPublicationHelper.isReviewPublishedForUser(reviewId, userId, user.role.name)) {
                    val reviewData = reviewRepository.findReviewDataById(reviewId)
                        ?: throw NotFoundException("Review with id $reviewId not found")
                    return ReviewResultsResponse(
                        id = project.id!!,
                        title = project.title,
                        projectTitle = project.title,
                        reviewName = reviewData["name"].toString(),
                        isPublished = false,
                        canViewAllResults = false,
                        results = emptyList()
                    )
                }
                if (!project.team.members.contains(user)) {
                    throw ForbiddenException("Students can only view results for their own projects")
                }
                false
            }
            else -> throw ForbiddenException("Invalid user role")
        }

        val scoresData = individualScoreRepository.findScoreDataByReviewAndProject(reviewId, projectId)
        val scoresByParticipantId = scoresData.groupBy { it["participant_id"].toString() }

        val filteredParticipantIds = if (canViewAllResults) {
            scoresByParticipantId.keys
        } else {
            scoresByParticipantId.keys.filter { it == userId }
        }

        val results = filteredParticipantIds.map { participantId ->
            val participantScores = scoresByParticipantId[participantId] ?: emptyList()
            val firstScore = participantScores.first()
            val participantName = firstScore["participant_name"].toString()

            val totalScore = participantScores.sumOf { (it["score"] as Number).toDouble() }
            val maxPossibleScore = participantScores.sumOf { (it["criterion_max_score"] as Number).toDouble() }
            val percentage = if (maxPossibleScore > 0.0) {
                (totalScore / maxPossibleScore) * 100.0
            } else {
                0.0
            }

            val criterionResults = participantScores.map { scoreData ->
                CriterionResult(
                    criterionId = UUID.fromString(scoreData["criterion_id"].toString()),
                    criterionName = scoreData["criterion_name"].toString(),
                    score = (scoreData["score"] as Number).toDouble(),
                    maxScore = (scoreData["criterion_max_score"] as Number).toDouble(),
                    comment = scoreData["comment"]?.toString()
                )
            }

            StudentResult(
                id = participantId,
                name = participantName,
                studentId = participantId,
                studentName = participantName,
                individualScore = totalScore,
                totalScore = totalScore,
                maxPossibleScore = maxPossibleScore,
                percentage = percentage,
                scores = criterionResults
            )
        }

        val reviewData = reviewRepository.findReviewDataById(reviewId)
            ?: throw NotFoundException("Review with id $reviewId not found")

        return ReviewResultsResponse(
            id = project.id!!,
            title = project.title,
            projectTitle = project.title,
            reviewName = reviewData["name"].toString(),
            isPublished = reviewPublicationHelper.isReviewPublishedForUser(reviewId, userId, user.role.name),
            canViewAllResults = canViewAllResults,
            results = results
        )
    }

    private fun isProjectAssociatedWithReview(reviewId: UUID, projectId: UUID): Boolean {
        return reviewRepository.isProjectAssociatedWithReview(reviewId, projectId)
    }

    private fun createSort(sortBy: String, sortOrder: String): Sort {
        val direction = if (sortOrder.lowercase() == "desc") Sort.Direction.DESC else Sort.Direction.ASC
        return Sort.by(direction, sortBy)
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun mapBatchedDataToResponse(
        data: Map<String, Any>,
        coursesByReviewId: Map<String, List<Map<String, Any>>>,
        projectsByReviewId: Map<String, List<Map<String, Any>>>,
        criteriaByRubricsId: Map<String, List<Map<String, Any>>>,
        publishMap: Map<String, Int>,
        userRole: Role
    ): ReviewResponse {
        val reviewIdStr = data["id"].toString()
        val rubricsIdStr = data["rubrics_id"].toString()
        val reviewId = UUID.fromString(reviewIdStr)
        val rubricsId = UUID.fromString(rubricsIdStr)
        
        val courses = coursesByReviewId[reviewIdStr]?.map { c ->
            CourseInfo(
                id = UUID.fromString(c["id"].toString()),
                name = c["name"].toString(),
                code = c["code"].toString(),
                semesterInfo = SemesterInfo(
                    id = UUID.fromString(c["semester_id"].toString()),
                    name = c["semester_name"].toString(),
                    year = (c["semester_year"] as Number).toInt(),
                    isActive = c["semester_is_active"] as Boolean
                )
            )
            } ?: emptyList()
        
        val projects = projectsByReviewId[reviewIdStr]
            ?.groupBy { it["id"].toString() }
            ?.map { (projectId, rows) ->
                val firstRow = rows.first()
                val memberIds = firstRow["member_ids"]?.toString()?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
                val memberNames = firstRow["member_names"]?.toString()?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
                
                val teamMembers = memberIds.zip(memberNames).map { (id, name) ->
                    TeamMemberInfo(
                        id = id,
                        name = name
                    )
                }
                
                ProjectInfo(
                    id = UUID.fromString(projectId),
                    title = firstRow["title"].toString(),
                    teamName = firstRow["team_name"].toString(),
                    teamMembers = teamMembers
                )
            } ?: emptyList()
        
        val criteria = criteriaByRubricsId[rubricsIdStr]?.map { cr ->
            CriteriaInfo(
                id = UUID.fromString(cr["id"].toString()),
                name = cr["name"].toString(),
                description = cr["description"].toString(),
                maxScore = (cr["max_score"] as Number).toFloat(),
                isCommon = cr["is_common"] as Boolean
            )
        } ?: emptyList()
        
        val publishedCount = publishMap[reviewIdStr] ?: 0
        val createdById = data["created_by_id"]?.toString()
        
        val isPublished = when (userRole) {
            Role.ADMIN, Role.MANAGER -> publishedCount == courses.size && courses.isNotEmpty()
            Role.FACULTY, Role.STUDENT -> publishedCount > 0
        }
        
        return ReviewResponse(
            id = reviewId,
            name = data["name"].toString(),
            startDate = java.time.LocalDate.parse(data["start_date"].toString()),
            endDate = java.time.LocalDate.parse(data["end_date"].toString()),
            publishedAt = null,
            createdBy = if (createdById != null) {
                CreatedByInfo(
                    id = createdById,
                    name = data["created_by_name"]?.toString() ?: "Unknown",
                    email = data["created_by_email"]?.toString() ?: "",
                    role = data["created_by_role"]?.toString() ?: "UNKNOWN"
                )
            } else {
                CreatedByInfo(id = "", name = "Unknown", email = "", role = "UNKNOWN")
            },
            courses = courses,
            projects = projects,
            sections = emptyList(),
            rubricsInfo = RubricInfo(
                id = rubricsId,
                name = data["rubrics_name"].toString(),
                criteria = criteria
            ),
            isPublished = isPublished
        )
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun mapListDataToResponse(
        data: Map<String, Any>,
        coursesByReviewId: Map<String, List<Map<String, Any>>>,
        criteriaByRubricsId: Map<String, List<Map<String, Any>>>,
        publishMap: Map<String, Int>,
        userRole: Role
    ): ReviewResponse {
        val reviewIdStr = data["id"].toString()
        val rubricsIdStr = data["rubrics_id"].toString()
        val reviewId = UUID.fromString(reviewIdStr)
        val rubricsId = UUID.fromString(rubricsIdStr)
        
        val courses = coursesByReviewId[reviewIdStr]?.map { c ->
            CourseInfo(
                id = UUID.fromString(c["id"].toString()),
                name = c["name"].toString(),
                code = c["code"].toString(),
                semesterInfo = SemesterInfo(
                    id = UUID.fromString(c["semester_id"].toString()),
                    name = c["semester_name"].toString(),
                    year = (c["semester_year"] as Number).toInt(),
                    isActive = c["semester_is_active"] as Boolean
                )
            )
        } ?: emptyList()
        
        val criteria = criteriaByRubricsId[rubricsIdStr]?.map { cr ->
            CriteriaInfo(
                id = UUID.fromString(cr["id"].toString()),
                name = cr["name"].toString(),
                description = cr["description"].toString(),
                maxScore = (cr["max_score"] as Number).toFloat(),
                isCommon = cr["is_common"] as Boolean
            )
        } ?: emptyList()
        
        val publishedCount = publishMap[reviewIdStr] ?: 0
        val createdById = data["created_by_id"]?.toString()
        
        val isPublished = when (userRole) {
            Role.ADMIN, Role.MANAGER -> publishedCount == courses.size && courses.isNotEmpty()
            Role.FACULTY, Role.STUDENT -> publishedCount > 0
        }
        
        return ReviewResponse(
            id = reviewId,
            name = data["name"].toString(),
            startDate = java.time.LocalDate.parse(data["start_date"].toString()),
            endDate = java.time.LocalDate.parse(data["end_date"].toString()),
            publishedAt = null,
            createdBy = if (createdById != null) {
                CreatedByInfo(
                    id = createdById,
                    name = data["created_by_name"]?.toString() ?: "Unknown",
                    email = data["created_by_email"]?.toString() ?: "",
                    role = data["created_by_role"]?.toString() ?: "UNKNOWN"
                )
            } else {
                CreatedByInfo(id = "", name = "Unknown", email = "", role = "UNKNOWN")
            },
            courses = courses,
            projects = emptyList(),
            sections = emptyList(),
            rubricsInfo = RubricInfo(
                id = rubricsId,
                name = data["rubrics_name"].toString(),
                criteria = criteria
            ),
            isPublished = isPublished
        )
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun mapSingleReviewDataToResponse(
        data: Map<String, Any>,
        coursesData: List<Map<String, Any>>,
        projectsData: List<Map<String, Any>>,
        criteriaData: List<Map<String, Any>>,
        @Suppress("UNUSED_PARAMETER") userRole: Role,
        isPublished: Boolean
    ): ReviewResponse {
        val reviewId = UUID.fromString(data["id"].toString())
        val rubricsId = UUID.fromString(data["rubrics_id"].toString())
        
        val courses = coursesData.map { c ->
            CourseInfo(
                id = UUID.fromString(c["id"].toString()),
                name = c["name"].toString(),
                code = c["code"].toString(),
                semesterInfo = SemesterInfo(
                    id = UUID.fromString(c["semester_id"].toString()),
                    name = c["semester_name"].toString(),
                    year = (c["semester_year"] as Number).toInt(),
                    isActive = c["semester_is_active"] as Boolean
                )
            )
        }
        
        val projects = projectsData
            .groupBy { it["id"].toString() }
            .map { (projectId, rows) ->
                val firstRow = rows.first()
                val memberIds = firstRow["member_ids"]?.toString()?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
                val memberNames = firstRow["member_names"]?.toString()?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
                
                val teamMembers = memberIds.zip(memberNames).map { (id, name) ->
                    TeamMemberInfo(
                        id = id,
                        name = name
                    )
                }
                
                ProjectInfo(
                    id = UUID.fromString(projectId),
                    title = firstRow["title"].toString(),
                    teamName = firstRow["team_name"].toString(),
                    teamMembers = teamMembers
                )
            }
        
        val criteria = criteriaData.map { cr ->
            CriteriaInfo(
                id = UUID.fromString(cr["id"].toString()),
                name = cr["name"].toString(),
                description = cr["description"].toString(),
                maxScore = (cr["max_score"] as Number).toFloat(),
                isCommon = cr["is_common"] as Boolean
            )
        }
        
        val createdById = data["created_by_id"]?.toString()
        
        return ReviewResponse(
            id = reviewId,
            name = data["name"].toString(),
            startDate = java.time.LocalDate.parse(data["start_date"].toString()),
            endDate = java.time.LocalDate.parse(data["end_date"].toString()),
            publishedAt = null,
            createdBy = if (createdById != null) {
                CreatedByInfo(
                    id = createdById,
                    name = data["created_by_name"]?.toString() ?: "Unknown",
                    email = data["created_by_email"]?.toString() ?: "",
                    role = data["created_by_role"]?.toString() ?: "UNKNOWN"
                )
            } else {
                CreatedByInfo(id = "", name = "Unknown", email = "", role = "UNKNOWN")
            },
            courses = courses,
            projects = projects,
            sections = emptyList(),
            rubricsInfo = RubricInfo(
                id = rubricsId,
                name = data["rubrics_name"].toString(),
                criteria = criteria
            ),
            isPublished = isPublished
        )
    }
    
    @Transactional(readOnly = true)
    fun getReviewProjectsWithFilters(
        reviewId: UUID,
        userId: String,
        teamId: UUID?,
        batchId: UUID?,
        courseId: UUID?
    ): ReviewProjectsResponse {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }
        
        val hasAccess = reviewRepository.hasUserAccessToReview(reviewId, userId, user.role.name)
        if (!hasAccess) {
            throw ForbiddenException("You don't have access to this review")
        }
        
        val review = reviewRepository.findById(reviewId).orElseThrow {
            NotFoundException("Review with id $reviewId not found")
        }
        
        val teamIdStr = teamId?.toString()
        val batchIdStr = batchId?.toString()
        val courseIdStr = courseId?.toString()
        
        val projectsData = reviewRepository.findFilteredProjectsByReviewId(
            reviewId, userId, user.role.name, teamIdStr, batchIdStr, courseIdStr
        )
        
        val projects = projectsData.map { row ->
            val memberIds = row["member_ids"]?.toString()?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
            val memberNames = row["member_names"]?.toString()?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
            val batchIdsStr = row["batch_ids"]?.toString()?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
            val courseIdsStr = row["course_ids"]?.toString()?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
            
            val teamMembers = memberIds.zip(memberNames).map { (id, name) ->
                TeamMemberInfo(id = id, name = name)
            }
            
            ReviewProjectInfo(
                projectId = UUID.fromString(row["project_id"].toString()),
                projectTitle = row["project_title"].toString(),
                teamId = UUID.fromString(row["team_id"].toString()),
                teamName = row["team_name"].toString(),
                teamMembers = teamMembers,
                batchIds = batchIdsStr.map { UUID.fromString(it) },
                courseIds = courseIdsStr.map { UUID.fromString(it) }
            )
        }
        
        val allTeams = projects.map { 
            TeamFilterInfo(teamId = it.teamId, teamName = it.teamName) 
        }.distinctBy { it.teamId }
        
        val allBatchIds = projects.flatMap { it.batchIds }.distinct()
        val batches = if (allBatchIds.isNotEmpty()) {
            batchRepository.findAllById(allBatchIds).map {
                BatchFilterInfo(batchId = it.id!!, batchName = it.name)
            }
        } else {
            emptyList()
        }
        
        val allCourseIds = projects.flatMap { it.courseIds }.distinct()
        val courses = if (allCourseIds.isNotEmpty()) {
            courseRepository.findAllById(allCourseIds).map {
                CourseFilterInfo(courseId = it.id!!, courseName = it.name, courseCode = it.code)
            }
        } else {
            emptyList()
        }
        
        return ReviewProjectsResponse(
            reviewId = reviewId,
            reviewName = review.name,
            projects = projects,
            teams = allTeams,
            batches = batches,
            courses = courses
        )
    }
    
    @Transactional(readOnly = true)
    fun getReviewExportData(
        reviewId: UUID,
        userId: String,
        batchIds: List<UUID>,
        courseIds: List<UUID>
    ): ReviewExportResponse {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }
        
        val hasAccess = reviewRepository.hasUserAccessToReview(reviewId, userId, user.role.name)
        if (!hasAccess) {
            throw ForbiddenException("You don't have access to this review")
        }
        
        val review = reviewRepository.findById(reviewId).orElseThrow {
            NotFoundException("Review with id $reviewId not found")
        }
        
        val criteria = review.rubrics?.criteria?.map {
            CriteriaInfo(
                id = it.id!!,
                name = it.name,
                description = it.description,
                maxScore = it.maxScore,
                isCommon = it.isCommon
            )
        } ?: emptyList()
        
        val projectsData = reviewRepository.findFilteredProjectsByReviewId(
            reviewId, userId, user.role.name, null, null, null
        )
        
        var filteredProjects = projectsData
        
        if (batchIds.isNotEmpty()) {
            filteredProjects = filteredProjects.filter { row ->
                val projectBatchIds = row["batch_ids"]?.toString()
                    ?.split("|")
                    ?.filter { it.isNotEmpty() }
                    ?.map { UUID.fromString(it) }
                    ?: emptyList()
                projectBatchIds.any { batchIds.contains(it) }
            }
        }
        
        if (courseIds.isNotEmpty()) {
            filteredProjects = filteredProjects.filter { row ->
                val projectCourseIds = row["course_ids"]?.toString()
                    ?.split("|")
                    ?.filter { it.isNotEmpty() }
                    ?.map { UUID.fromString(it) }
                    ?: emptyList()
                projectCourseIds.any { courseIds.contains(it) }
            }
        }
        
        val allBatchIds = filteredProjects
            .flatMap { row ->
                row["batch_ids"]?.toString()
                    ?.split("|")
                    ?.filter { it.isNotEmpty() }
                    ?.map { UUID.fromString(it) }
                    ?: emptyList()
            }
            .distinct()
        
        val batchMap = if (allBatchIds.isNotEmpty()) {
            batchRepository.findAllById(allBatchIds).associate { it.id!! to it.name }
        } else {
            emptyMap()
        }
        
        val allCourseIds = filteredProjects
            .flatMap { row ->
                row["course_ids"]?.toString()
                    ?.split("|")
                    ?.filter { it.isNotEmpty() }
                    ?.map { UUID.fromString(it) }
                    ?: emptyList()
            }
            .distinct()
        
        val courseMap = if (allCourseIds.isNotEmpty()) {
            courseRepository.findAllById(allCourseIds).associate { 
                it.id!! to "${it.name} (${it.code})" 
            }
        } else {
            emptyMap()
        }
        
        val projectIds = filteredProjects.map { UUID.fromString(it["project_id"].toString()) }
        
        val scoresData = if (projectIds.isNotEmpty()) {
            individualScoreRepository.findScoresByReviewAndProjects(reviewId, projectIds)
        } else {
            emptyList()
        }
        
        val scoresByParticipantAndProject = scoresData.groupBy { 
            "${it["participant_id"]}_${it["project_id"]}" 
        }
        
        val students = filteredProjects.flatMap { projectRow ->
            val projectId = UUID.fromString(projectRow["project_id"].toString())
            val projectTitle = projectRow["project_title"].toString()
            val teamId = UUID.fromString(projectRow["team_id"].toString())
            val teamName = projectRow["team_name"].toString()
            
            val memberIds = projectRow["member_ids"]?.toString()
                ?.split("|")
                ?.filter { it.isNotEmpty() } 
                ?: emptyList()
            val memberProfileIds = projectRow["member_profile_ids"]?.toString()
                ?.split("|")
                ?.filter { it.isNotEmpty() } 
                ?: emptyList()
            val memberNames = projectRow["member_names"]?.toString()
                ?.split("|")
                ?.filter { it.isNotEmpty() } 
                ?: emptyList()
            val memberEmails = projectRow["member_emails"]?.toString()
                ?.split("|")
                ?.filter { it.isNotEmpty() } 
                ?: emptyList()
            
            val projectBatchIds = projectRow["batch_ids"]?.toString()
                ?.split("|")
                ?.filter { it.isNotEmpty() }
                ?.map { UUID.fromString(it) }
                ?: emptyList()
            
            val projectCourseIds = projectRow["course_ids"]?.toString()
                ?.split("|")
                ?.filter { it.isNotEmpty() }
                ?.map { UUID.fromString(it) }
                ?: emptyList()
            
            val batchNames = projectBatchIds.mapNotNull { batchMap[it] }
            val courseNames = projectCourseIds.mapNotNull { courseMap[it] }
            
            memberIds.indices.map { index ->
                val memberId = memberIds[index]
                val memberProfileId = memberProfileIds.getOrNull(index) ?: ""
                val memberName = memberNames.getOrNull(index) ?: "Unknown"
                val memberEmail = memberEmails.getOrNull(index) ?: ""
                
                val key = "${memberId}_${projectId}"
                val participantScores = scoresByParticipantAndProject[key] ?: emptyList()
                
                val criteriaScores = mutableMapOf<UUID, CriteriaScoreData>()
                var totalScore: Double? = null
                var maxScore: Double? = null
                var percentage: Double? = null
                
                if (participantScores.isNotEmpty()) {
                    totalScore = participantScores.sumOf { (it["score"] as Number).toDouble() }
                    maxScore = participantScores.sumOf { (it["criterion_max_score"] as Number).toDouble() }
                    percentage = if (maxScore!! > 0.0) (totalScore!! / maxScore) * 100.0 else 0.0
                    
                    participantScores.forEach { scoreData ->
                        val criterionId = UUID.fromString(scoreData["criterion_id"].toString())
                        criteriaScores[criterionId] = CriteriaScoreData(
                            criterionId = criterionId,
                            criterionName = scoreData["criterion_name"].toString(),
                            score = (scoreData["score"] as Number).toDouble(),
                            maxScore = (scoreData["criterion_max_score"] as Number).toDouble(),
                            comment = scoreData["comment"]?.toString()
                        )
                    }
                }
                
                StudentExportData(
                    profileId = memberProfileId,
                    studentName = memberName,
                    email = memberEmail,
                    teamId = teamId,
                    teamName = teamName,
                    projectId = projectId,
                    projectTitle = projectTitle,
                    batchIds = projectBatchIds,
                    batchNames = batchNames,
                    courseIds = projectCourseIds,
                    courseNames = courseNames,
                    totalScore = totalScore,
                    maxScore = maxScore,
                    percentage = percentage,
                    criteriaScores = criteriaScores
                )
            }
        }
        
        return ReviewExportResponse(
            reviewId = reviewId,
            reviewName = review.name,
            students = students,
            criteria = criteria
        )
    }
}
