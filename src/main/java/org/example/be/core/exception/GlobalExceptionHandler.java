package org.example.be.core.exception;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.MismatchedInputException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorRes> handleBusinessLogicException(BusinessLogicException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        var response = ResponseEntity.status(errorCode.getHttpStatus());
        if (ex.getDetails() instanceof Map<?, ?> details && details.get("retryAfter") instanceof Number seconds) {
            response.header(HttpHeaders.RETRY_AFTER, seconds.toString());
        }
        return response.body(ErrorRes.of(errorCode.name(), errorCode.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRes> handleValidationException(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "issue", issue(error.getCode()),
                        "message", error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage()))
                .toList();
        return respond(ErrorCode.VALIDATION_ERROR, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRes> handleUnreadableBody(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof MismatchedInputException mismatch) {
            StringBuilder path = new StringBuilder();
            for (var step : mismatch.getPath()) {
                if (step.getPropertyName() != null) {
                    if (!path.isEmpty()) path.append('.');
                    path.append(step.getPropertyName());
                } else if (step.getIndex() >= 0) {
                    path.append('[').append(step.getIndex()).append(']');
                }
            }
            return respond(ErrorCode.VALIDATION_ERROR, Map.of("field", path.toString(), "issue", "INVALID_FORMAT",
                    "message", "Invalid value."));
        }
        return respond(ErrorCode.MALFORMED_REQUEST, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorRes> handleMalformedRequest() {
        return respond(ErrorCode.MALFORMED_REQUEST, null);
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorRes> handleInvalidInput() {
        return respond(ErrorCode.VALIDATION_ERROR, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRes> handleMissingRoute() {
        return respond(ErrorCode.NOT_FOUND, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRes> handleUnexpected(Exception failure) {
        if (failure instanceof ErrorResponse http && http.getStatusCode().is4xxClientError()) {
            return ResponseEntity.status(http.getStatusCode()).headers(http.getHeaders())
                    .body(ErrorRes.of(ErrorCode.MALFORMED_REQUEST.name(), ErrorCode.MALFORMED_REQUEST.getMessage()));
        }
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        ErrorRes body = ErrorRes.of(code.name(), code.getMessage());
        log.error("API request failed ({})", body.getError().getRequestId(), failure);
        return ResponseEntity.status(code.getHttpStatus()).body(body);
    }

    private static String issue(String code) {
        if (code == null) return "INVALID_FORMAT";
        return switch (code) {
            case "NotNull", "NotBlank", "NotEmpty" -> "REQUIRED";
            case "Size" -> "INVALID_LENGTH";
            case "Positive", "DecimalMin" -> "MUST_BE_POSITIVE";
            default -> "INVALID_FORMAT";
        };
    }

    private static ResponseEntity<ErrorRes> respond(ErrorCode code, Object details) {
        return ResponseEntity.status(code.getHttpStatus()).body(ErrorRes.of(code.name(), code.getMessage(), details));
    }
}
