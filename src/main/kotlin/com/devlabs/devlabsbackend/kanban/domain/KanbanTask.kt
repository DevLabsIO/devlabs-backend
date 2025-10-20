package com.devlabs.devlabsbackend.kanban.domain

import com.devlabs.devlabsbackend.user.domain.User
import jakarta.persistence.*
import java.sql.Timestamp
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "kanban_task",
    indexes = [
        Index(name = "idx_kanban_task_column", columnList = "column_id"),
        Index(name = "idx_kanban_task_created_by", columnList = "created_by"),
        Index(name = "idx_kanban_task_position", columnList = "position"),
        Index(name = "idx_kanban_task_column_position", columnList = "column_id, position")
    ]
)
class KanbanTask(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    var title: String,
    
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    
    var position: Int,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id")
    var column: KanbanColumn,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    var createdBy: User,
    
    val createdAt: Timestamp = Timestamp.from(Instant.now()),
    var updatedAt: Timestamp = Timestamp.from(Instant.now())
)
