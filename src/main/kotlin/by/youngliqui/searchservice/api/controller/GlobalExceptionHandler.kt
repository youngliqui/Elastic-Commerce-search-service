package by.youngliqui.searchservice.api.controller

import by.youngliqui.searchservice.api.dto.ErrorResponse
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Internal server error", ex)
        val errorResponse = ErrorResponse(
            code = "INTERNAL_ERROR",
            message = "Внутренняя ошибка сервера",
            timestamp = Instant.now()
        )
        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.warn("Validation error: {}", ex.message)
        val errorResponse = ErrorResponse(
            code = "VALIDATION_ERROR",
            message = ex.message ?: "Некорректные параметры запроса",
            timestamp = Instant.now()
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }
}