package be.ephec.padel.backend.error;

import be.ephec.padel.backend.dto.response.ApiErrorDto;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request) {

        ApiErrorDto error = buildError(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorDto> handleBusiness(
            BusinessException ex,
            HttpServletRequest request) {

        ApiErrorDto error = buildError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDto> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> details.put(err.getField(), err.getDefaultMessage()));

        ApiErrorDto error = buildError(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                details
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorDto> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        Map<String, String> details = new HashMap<>();
        ex.getConstraintViolations().forEach(v -> details.put(
                v.getPropertyPath().toString(),
                v.getMessage()
        ));

        ApiErrorDto error = buildError(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                details
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDto> handleJsonError(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        ApiErrorDto error = buildError(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request",
                request.getRequestURI(),
                null
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorDto> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String msg = "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue();

        ApiErrorDto error = buildError(
                HttpStatus.BAD_REQUEST,
                msg,
                request.getRequestURI(),
                null
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        // Ne pas exposer les détails internes en production.
        ApiErrorDto error = buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error",
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ApiErrorDto buildError(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> details) {

        ApiErrorDto dto = new ApiErrorDto();
        dto.setTimestamp(LocalDateTime.now());
        dto.setStatus(status.value());
        dto.setError(status.getReasonPhrase());
        dto.setMessage(message);
        dto.setPath(path);
        dto.setDetails(details);

        return dto;
    }
}