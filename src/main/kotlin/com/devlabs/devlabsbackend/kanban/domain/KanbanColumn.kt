package com.devlabs.devlabsbackend.kanban.domain

import jakarta.persistence.*
import java.sql.Timestamp
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "kanban_column",
    indexes = [
        Index(name = "idx_kanban_column_board", columnList = "board_id"),
        Index(name = "idx_kanban_column_position", columnList = "position"),
        Index(name = "idx_kanban_column_board_position", columnList = "board_id, position")
    ]
)
class KanbanColumn(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    var name: String,
    var position: Int,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    var board: KanbanBoard,
    
    @OneToMany(mappedBy = "column", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var tasks: MutableSet<KanbanTask> = mutableSetOf(),
    
    val createdAt: Timestamp = Timestamp.from(Instant.now()),
    var updatedAt: Timestamp = Timestamp.from(Instant.now())
)
