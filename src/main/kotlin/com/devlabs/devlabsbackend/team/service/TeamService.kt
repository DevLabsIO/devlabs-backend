package com.devlabs.devlabsbackend.team.service

import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.core.pagination.PaginationInfo
import com.devlabs.devlabsbackend.team.domain.Team
import com.devlabs.devlabsbackend.team.domain.dto.CreateTeamRequest
import com.devlabs.devlabsbackend.team.domain.dto.TeamResponse
import com.devlabs.devlabsbackend.team.domain.dto.UpdateTeamRequest
import com.devlabs.devlabsbackend.team.repository.TeamRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.dto.UserResponse
import com.devlabs.devlabsbackend.user.repository.UserRepository
import com.devlabs.devlabsbackend.user.service.toUserResponse
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.util.*

@Service
@Transactional
class TeamService(
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository
) {
    
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.TEAMS_LIST], allEntries = true)
        ]
    )
    fun createTeam(teamData: CreateTeamRequest, creatorId: String): TeamResponse {
        val creator = userRepository.findById(creatorId).orElseThrow {
            NotFoundException("User with id $creatorId not found")
        }

        val team = Team(
            name = teamData.name,
            description = teamData.description
        )

        team.members.add(creator)

        if (teamData.memberIds.isNotEmpty()) {
            val members = userRepository.findAllById(teamData.memberIds)

            if (members.size != teamData.memberIds.size) {
                throw NotFoundException("Some members could not be found")
            }

            val nonStudents = members.filter { it.role != Role.STUDENT }
            if (nonStudents.isNotEmpty()) {
                throw IllegalArgumentException("Only students can join teams")
            }
            members.forEach { member ->
                if (member.id != creatorId) {
                    team.members.add(member)
                }
            }
        }
        val savedTeam = teamRepository.save(team)
        return savedTeam.toTeamResponse()
    }

    @Cacheable(
        value = [CacheConfig.TEAMS_LIST],
        key = "'teams_all_' + #page + '_' + #size + '_' + #sortBy + '_' + #sortOrder"
    )
    fun getAllTeams(page: Int = 0, size: Int = 10, sortBy: String = "name", sortOrder: String = "asc"): PaginatedResponse<TeamResponse> {
        val offset = page * size
        val sortByNormalized = if (sortBy == "createdAt") "created_at" else sortBy
        val sortOrderNormalized = sortOrder.uppercase()
        
        val teamIdsRaw = teamRepository.findAllIds(sortByNormalized, sortOrderNormalized, offset, size)
        val teamIds = teamIdsRaw.map { (it[0] as UUID) }
        val totalCount = teamRepository.countAllTeams()
        
        if (teamIds.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page,
                    per_page = size,
                    total_pages = 0,
                    total_count = 0
                )
            )
        }
        
        val teams = teamRepository.findAllByIdWithRelations(teamIds)
        val teamMap = teams.associateBy { it.id }
        val orderedTeams = teamIds.mapNotNull { teamMap[it] }

        return PaginatedResponse(
            data = orderedTeams.map { team -> team.toTeamResponse() },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt()
            )
        )
    }    
    
    @Cacheable(
        value = [CacheConfig.TEAMS_LIST],
        key = "'teams_user_' + #userId + '_' + #page + '_' + #size + '_' + #sortBy + '_' + #sortOrder"
    )
    fun getTeamsByUser(userId: String, page: Int = 0, size: Int = 10, sortBy: String = "name", sortOrder: String = "asc"): PaginatedResponse<TeamResponse> {
        if (!userRepository.existsById(userId)) {
            throw NotFoundException("User with id $userId not found")
        }
        
        val offset = page * size
        val sortByNormalized = if (sortBy == "createdAt") "created_at" else sortBy
        val sortOrderNormalized = sortOrder.uppercase()
        
        val teamIdsRaw = teamRepository.findIdsByMember(userId, sortByNormalized, sortOrderNormalized, offset, size)
        val teamIds = teamIdsRaw.map { (it[0] as UUID) }
        val totalCount = teamRepository.countTeamsByMember(userId)
        
        if (teamIds.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page,
                    per_page = size,
                    total_pages = 0,
                    total_count = 0
                )
            )
        }
        
        val teams = teamRepository.findAllByIdWithRelations(teamIds)
        val teamMap = teams.associateBy { it.id }
        val orderedTeams = teamIds.mapNotNull { teamMap[it] }

        return PaginatedResponse(
            data = orderedTeams.map { team -> team.toTeamResponse() },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt()
            )
        )
    }

    @Cacheable(value = [CacheConfig.TEAM_DETAIL_CACHE], key = "'team_' + #teamId")
    fun getTeamById(teamId: UUID): TeamResponse {
        val team = teamRepository.findByIdWithRelations(teamId) 
            ?: throw NotFoundException("Team with id $teamId not found")
        return team.toTeamResponse()
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.TEAM_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.TEAMS_LIST], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_LIST, CacheConfig.PROJECT_DETAIL], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
    fun updateTeam(teamId: UUID, updateData: UpdateTeamRequest): TeamResponse {
        val team = teamRepository.findByIdWithRelations(teamId) 
            ?: throw NotFoundException("Team with id $teamId not found")

        updateData.name?.let { team.name = it }
        updateData.description?.let { team.description = it }

        if (updateData.memberIds != null) {
            val newMembers = userRepository.findAllById(updateData.memberIds)

            if (newMembers.size != updateData.memberIds.size) {
                throw NotFoundException("Some member IDs could not be found")
            }

            val nonStudents = newMembers.filter { it.role != Role.STUDENT }
            if (nonStudents.isNotEmpty()) {
                throw IllegalArgumentException("Only students can be team members")
            }

            team.members.clear()
            team.members.addAll(newMembers)
        }

        team.updatedAt = Timestamp.from(Instant.now())

        val savedTeam = teamRepository.save(team)
        return savedTeam.toTeamResponse()
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.TEAM_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.TEAMS_LIST], allEntries = true),
            CacheEvict(value = [CacheConfig.PROJECTS_LIST, CacheConfig.PROJECT_DETAIL], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
    fun deleteTeam(teamId: UUID) {
        if (!teamRepository.existsById(teamId)) {
            throw NotFoundException("Team with id $teamId not found")
        }
        teamRepository.deleteById(teamId)
    }

    @Cacheable(
        value = [CacheConfig.TEAMS_LIST],
        key = "'teams_search_' + #userId + '_' + #query + '_' + #page + '_' + #size + '_' + #sortBy + '_' + #sortOrder"
    )
    fun searchTeamsByUser(userId: String, query: String, page: Int = 0, size: Int = 10, sortBy: String = "name", sortOrder: String = "asc"): PaginatedResponse<TeamResponse> {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val offset = page * size
        val sortByNormalized = if (sortBy == "createdAt") "created_at" else sortBy
        val sortOrderNormalized = sortOrder.uppercase()

        val (teamIdsRaw, totalCount) = when (user.role) {
            Role.ADMIN, Role.MANAGER -> {
                val ids = teamRepository.findIdsByNameContaining(query, sortByNormalized, sortOrderNormalized, offset, size)
                val count = teamRepository.countTeamsByNameContaining(query)
                Pair(ids, count)
            }
            Role.FACULTY -> {
                val ids = teamRepository.findIdsByNameAndCourseInstructor(query, userId, sortByNormalized, sortOrderNormalized, offset, size)
                val count = teamRepository.countTeamsByNameAndCourseInstructor(query, userId)
                Pair(ids, count)
            }
            Role.STUDENT -> {
                val ids = teamRepository.findIdsByNameAndMember(query, userId, sortByNormalized, sortOrderNormalized, offset, size)
                val count = teamRepository.countTeamsByNameAndMember(query, userId)
                Pair(ids, count)
            }
        }
        
        val teamIds = teamIdsRaw.map { (it[0] as UUID) }

        if (teamIds.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page,
                    per_page = size,
                    total_pages = 0,
                    total_count = 0
                )
            )
        }
        
        val teams = teamRepository.findAllByIdWithRelations(teamIds)
        val teamMap = teams.associateBy { it.id }
        val orderedTeams = teamIds.mapNotNull { teamMap[it] }

        return PaginatedResponse(
            data = orderedTeams.map { team -> team.toTeamResponse() },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt()
            )
        )
    }

    @Cacheable(
        value = [CacheConfig.USERS_CACHE],
        key = "'students_search_' + #query"
    )
    fun searchStudents(query: String): List<UserResponse> {
        val students = userRepository.findByNameOrEmailContainingIgnoreCase(query)
            .filter { it.role == Role.STUDENT }
        return students.map { it.toUserResponse() }
    }
}

fun Team.toTeamResponse(): TeamResponse {
    return TeamResponse(
        id = this.id,
        name = this.name,
        description = this.description,
        members = this.members.map { it.toUserResponse() },
        projectCount = this.projects.size,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
