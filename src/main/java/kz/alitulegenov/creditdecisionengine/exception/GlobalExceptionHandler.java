package kz.alitulegenov.creditdecisionengine.exception;

import kz.alitulegenov.creditdecisionengine.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex
    ){
        Map<String, String> errors = new HashMap<>();

        for (FieldError fieldError
                : ex.getBindingResult().getFieldErrors()) {

            String message = fieldError.getDefaultMessage();

            errors.putIfAbsent(
                    fieldError.getField(),
                    message != null ? message : "Invalid value"
            );
        }
        ApiError apiError = new ApiError(
                400,
                "Validation Failed",
                errors
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(LoanDecisionNotFoundException.class)
    public ResponseEntity<ApiError> handleLoanDecisionNotFound(
            LoanDecisionNotFoundException ex
    ){
        ApiError apiError = new ApiError(
                404,
                ex.getMessage(),
                Map.of()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }
}
