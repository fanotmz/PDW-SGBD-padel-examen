package be.ephec.padel.backend.error;

import be.ephec.padel.backend.dto.response.ApiErrorDto;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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

    // ✅ 403 (auth ok, mais pas les droits)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDto> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        ApiErrorDto error = buildError(
                HttpStatus.FORBIDDEN,
                "Access denied",
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // ✅ 401 (pas authentifié / auth invalide)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorDto> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {

        ApiErrorDto error = buildError(
                HttpStatus.UNAUTHORIZED,
                "Authentication required",
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDto> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        // ✅ ne pas écraser : on garde le premier message et on concatène si plusieurs erreurs
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> {
            details.merge(
                    err.getField(),
                    err.getDefaultMessage(),
                    (oldMsg, newMsg) -> oldMsg + "; " + newMsg
            );
        });

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

        Map<String, String> details = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> details.merge(
                v.getPropertyPath().toString(),
                v.getMessage(),
                (oldMsg, newMsg) -> oldMsg + "; " + newMsg
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