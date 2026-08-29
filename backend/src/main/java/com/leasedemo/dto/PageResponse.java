package com.leasedemo.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Application-owned generic paged-response envelope.
 *
 * <p>Deliberately small and NOT a general-purpose pagination framework —
 * it exposes only the metadata Angular's data table/paginator actually
 * needs, without coupling the external API contract to Spring Data's own
 * {@link Page} JSON serialization (which exposes persistence-oriented
 * details such as {@code pageable}/{@code sort} object shapes that may
 * change between Spring Data versions).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    /**
     * Builds a {@link PageResponse} from a Spring Data {@link Page}, mapping
     * each entity/projection to its outbound DTO via {@code mapper}.
     */
    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
