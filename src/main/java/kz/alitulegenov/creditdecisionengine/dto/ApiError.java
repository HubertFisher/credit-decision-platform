package kz.alitulegenov.creditdecisionengine.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Standardized API error response body returned on validation failures, malformed parameters,
 * or missing resources.
 *
 * @param status  HTTP status code (e.g. 400, 404)
 * @param message human-readable summary of the error
 * @param errors  map of field or parameter names to specific validation messages
 */
public record ApiError(
        int status,
        String message,
        Map<String, String> errors
) {
    public ApiError {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(errors, "errors must not be null");
        errors = Map.copyOf(errors);
    }
}
