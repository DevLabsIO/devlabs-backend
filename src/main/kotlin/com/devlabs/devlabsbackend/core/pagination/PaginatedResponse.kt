package com.devlabs.devlabsbackend.core.pagination

import java.io.Serializable

data class PaginatedResponse<T>(
    val data: List<T>,
    val pagination: PaginationInfo
) : Serializable

data class PaginationInfo(
    val current_page: Int,
    val per_page: Int,
    val total_pages: Int,
    val total_count: Int
) : Serializable
