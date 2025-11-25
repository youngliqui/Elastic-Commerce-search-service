package by.youngliqui.searchservice.api.dto

import java.time.Instant

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Instant
)