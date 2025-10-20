package com.devlabs.devlabsbackend.kanban.repository

import com.devlabs.devlabsbackend.kanban.domain.KanbanBoard
import com.devlabs.devlabsbackend.project.domain.Project
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KanbanBoardRepository : JpaRepository<KanbanBoard, UUID> {
    
    @Query("""
        SELECT DISTINCT b FROM KanbanBoard b 
        LEFT JOIN FETCH b.project p
        LEFT JOIN FETCH b.columns c 
        LEFT JOIN FETCH c.tasks t 
        LEFT JOIN FETCH t.createdBy u 
        WHERE b.project = :project 
        ORDER BY c.position ASC, t.position ASC
    """)
    fun findByProjectWithRelations(@Param("project") project: Project): KanbanBoard?
}
