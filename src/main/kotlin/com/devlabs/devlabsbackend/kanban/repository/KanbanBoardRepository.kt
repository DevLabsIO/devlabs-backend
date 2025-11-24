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
        WHERE b.project = :project 
        ORDER BY c.position ASC
    """)
    fun findByProjectWithColumns(@Param("project") project: Project): KanbanBoard?
    
    @Query("""
        SELECT DISTINCT t FROM KanbanTask t 
        LEFT JOIN FETCH t.createdBy u 
        WHERE t.column.board = :board 
    """)
    fun findTasksByBoard(@Param("board") board: KanbanBoard): List<com.devlabs.devlabsbackend.kanban.domain.KanbanTask>
    
    fun findByProject(project: Project): KanbanBoard?
}
