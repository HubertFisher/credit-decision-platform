package kz.alitulegenov.creditdecisionengine.exception;

import java.util.HashMap;
import java.util.Map;
import kz.alitulegenov.creditdecisionengine.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Centralized REST exception handler translating validation and domain exceptions
 * into structured {@link ApiError} responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String message = fieldError.getDefaultMessage();
            errors.putIfAbsent(
                    fieldError.getField(),
                    message != null ? message : "Invalid value"
            );
        }

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                errors
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException ex) {
        Map<String, String> errors = new HashMap<>();

        for (var result : ex.getParameterValidationResults()) {
            String parameterName = result.getMethodParameter().getParameterName();
            if (parameterName == null) {
                parameterName = "parameter" + result.getMethodParameter().getParameterIndex();
            }
            for (var error : result.getResolvableErrors()) {
                String message = error.getDefaultMessage();
                errors.putIfAbsent(
                        parameterName,
                        message != null ? message : "Invalid value"
                );
            }
        }

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                errors
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid parameter",
                Map.of(ex.getName(), "Invalid value type")
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedJson(HttpMessageNotReadableException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Malformed JSON request body",
                Map.of()
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(CreditDecisionNotFoundException.class)
    public ResponseEntity<ApiError> handleCreditDecisionNotFound(CreditDecisionNotFoundException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                Map.of()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }
}
