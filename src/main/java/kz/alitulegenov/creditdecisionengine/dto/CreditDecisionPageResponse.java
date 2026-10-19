package kz.alitulegenov.creditdecisionengine.dto;

import java.util.List;
import java.util.Objects;

/**
 * Paginated response wrapper for credit decision audit history queries.
 *
 * @param items         decisions on the requested page
 * @param page          zero-based page index
 * @param size          maximum number of items per page
 * @param totalElements total number of decision records across all pages
 * @param totalPages    total number of available pages
 */
public record CreditDecisionPageResponse(
    List<CreditDecisionResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public CreditDecisionPageResponse {
        Objects.requireNonNull(items, "items must not be null");
        items = List.copyOf(items);
    }
}
