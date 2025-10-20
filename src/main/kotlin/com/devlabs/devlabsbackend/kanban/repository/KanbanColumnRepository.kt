package com.devlabs.devlabsbackend.kanban.repository

import com.devlabs.devlabsbackend.kanban.domain.KanbanColumn
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KanbanColumnRepository : JpaRepository<KanbanColumn, UUID> {
    
    @Query("""
        SELECT DISTINCT c FROM KanbanColumn c 
        LEFT JOIN FETCH c.board b
        LEFT JOIN FETCH b.project p
        LEFT JOIN FETCH p.team t
        LEFT JOIN FETCH t.members
        WHERE c.id = :columnId
    """)
    fun findByIdWithProjectAndTeam(@Param("columnId") columnId: UUID): KanbanColumn?
}
