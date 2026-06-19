package org.raimundo.ecommerce.interfaces.problem;

import org.raimundo.ecommerce.application.idempotency.IdempotencyConflictException;
import org.raimundo.ecommerce.domain.common.DomainException;
import org.raimundo.ecommerce.infrastructure.observability.MetricsService;
import org.raimundo.ecommerce.infrastructure.observability.CorrelationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class ProblemExceptionHandler {
    private final MetricsService metrics;

    public ProblemExceptionHandler(MetricsService metrics) {
        this.metrics = metrics;
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> domain(DomainException exception) {
        metrics.businessError(exception.code());
        HttpStatus status = exception.code().contains("not_found") ? HttpStatus.NOT_FOUND : HttpStatus.UNPROCESSABLE_ENTITY;
        return problem(status, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<ProblemDetail> idempotency(IdempotencyConflictException exception) {
        return problem(HttpStatus.CONFLICT, "idempotency_conflict", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException exception) {
        return problem(HttpStatus.BAD_REQUEST, "validation_error", "Request validation failed");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ProblemDetail> missingHeader(MissingRequestHeaderException exception) {
        return problem(HttpStatus.BAD_REQUEST, "missing_header", exception.getHeaderName() + " header is required");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> denied(AccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "forbidden", "Insufficient scope");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> generic(Exception exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Unexpected service error");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://ecommerce.raimundo.org/problems/" + code));
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        problem.setProperty("correlationId", CorrelationContext.get());
        return ResponseEntity.status(status).body(problem);
    }
}
